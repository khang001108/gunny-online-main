-- Gunny 7.7.0: virtual accounts and inactivity retention. Keep existing user UUIDs.
-- Existing accounts receive a full retention window from this migration, not immediate deletion.
begin;
-- Transaction guard: only login policy/admin metadata may change in this upgrade.
create temporary table gunny_770_preserve on commit drop as
 select u.id,u.email,u.encrypted_password,to_jsonb(p) profile
 from auth.users u join public.profiles p on p.id=u.id;
create table if not exists public.gunny_retention_policy(
 singleton boolean primary key default true check(singleton),
 offline_days integer not null default 90 check(offline_days between 1 and 3650),
 enabled boolean not null default false,updated_at timestamptz not null default now()
);
insert into public.gunny_retention_policy(singleton) values(true) on conflict do nothing;
alter table public.gunny_retention_policy enable row level security;
revoke all on public.gunny_retention_policy from public,anon,authenticated;
grant all on public.gunny_retention_policy to service_role;
alter table public.gunny_account_admin add column if not exists offline_days integer check(offline_days between 1 and 3650);
alter table public.gunny_account_admin add column if not exists protected_until timestamptz;
alter table public.gunny_account_admin add column if not exists retention_since timestamptz not null default now();
insert into public.gunny_account_admin(user_id) select id from auth.users on conflict do nothing;

-- Convert login policy in place. Never replace UUIDs, passwords, emails or profile rows.
-- This is executed only when the owner runs this migration manually.
update auth.users set raw_app_meta_data=coalesce(raw_app_meta_data,'{}'::jsonb)||'{"account_type":"virtual"}'::jsonb,
 email_confirmed_at=coalesce(email_confirmed_at,now()),updated_at=now()
 where id in (select id from public.profiles)
 and (coalesce(raw_app_meta_data->>'account_type','')<>'virtual' or email_confirmed_at is null);

create or replace function public.gunny_inactive_deadline(p_user uuid) returns timestamptz
language sql stable security definer set search_path='' as $$
 select case when policy.enabled then greatest(
  greatest(coalesce(a.last_seen_at,a.last_login_at,u.created_at),a.retention_since)
    + make_interval(days=>coalesce(a.offline_days,policy.offline_days)),
  coalesce(a.protected_until,'-infinity'::timestamptz)) else null end
 from public.gunny_account_admin a join auth.users u on u.id=a.user_id
 cross join public.gunny_retention_policy policy where a.user_id=p_user and policy.singleton;
$$;
revoke all on function public.gunny_inactive_deadline(uuid) from public,anon,authenticated;
grant execute on function public.gunny_inactive_deadline(uuid) to service_role;

create or replace function public.gunny_admin(p_op text,p_data jsonb default '{}') returns jsonb
language plpgsql security definer set search_path='' as $$
declare uid uuid; a public.gunny_account_admin%rowtype; result jsonb; notices jsonb:='[]';
 t timestamptz:=clock_timestamp(); start_t timestamptz;end_t timestamptz; n integer; item jsonb; grant_row public.gunny_wallet_grants%rowtype;add_coins integer;add_money integer;request_id uuid;balance jsonb;
begin
 if p_op='retention-policy' then
  return (select to_jsonb(p) from public.gunny_retention_policy p where singleton);
 elsif p_op='retention-save' then
  n:=(p_data->>'days')::integer;if n is null or n<1 or n>3650 then raise exception 'Days must be 1..3650';end if;
  if coalesce((p_data->>'enabled')::boolean,true) and exists(select 1 from public.gunny_retention_policy where singleton and not enabled) then
   update public.gunny_account_admin set retention_since=t;
  end if;
  update public.gunny_retention_policy set offline_days=n,enabled=coalesce((p_data->>'enabled')::boolean,true),updated_at=t where singleton;
  insert into public.gunny_admin_audit(action,target,detail) values(p_op,'all',p_data);
  return (select to_jsonb(p) from public.gunny_retention_policy p where singleton);
 elsif p_op='accounts' then
  select coalesce(jsonb_agg(r),'[]') into result from (
   select u.id,u.email,coalesce(p.display_name,'Chưa tạo nhân vật') display_name,p.public_id,
    u.created_at,ac.last_login_at,ac.last_seen_at,ac.max_offline_seconds,ac.delete_at,ac.delete_reason,ac.appeal,ac.cancel_on_login,
    public.gunny_inactive_deadline(u.id) inactive_delete_at,ac.offline_days,ac.protected_until,'virtual'::text account_type,
    coalesce(p.coins,0) coins,coalesce(p.shopping_money,0) shopping_money
   from auth.users u left join public.profiles p on p.id=u.id left join public.gunny_account_admin ac on ac.user_id=u.id
   where coalesce(p.display_name,'') ilike '%'||left(coalesce(p_data->>'search',''),100)||'%'
     or u.email ilike '%'||left(coalesce(p_data->>'search',''),100)||'%'
     or p.public_id::text=coalesce(p_data->>'search','')
   order by u.created_at desc,u.id limit 100 offset greatest(0,least(1000000,coalesce((p_data->>'offset')::integer,0)))
  ) r;
  return jsonb_build_object('accounts',result,'total',(select count(*) from auth.users),'now',t);
 elsif p_op='announcements' then
  return coalesce((select jsonb_agg(r) from (select * from public.gunny_announcements order by starts_at desc limit 200) r),'[]');
 elsif p_op='announcement-save' then
  uid:=nullif(p_data->>'target','')::uuid;start_t:=(p_data->>'starts_at')::timestamptz;end_t:=(p_data->>'ends_at')::timestamptz;
  if start_t is null or end_t is null or end_t<=start_t then raise exception 'Invalid dates';end if;
  perform pg_advisory_xact_lock(750001);
  if (select count(*) from public.gunny_announcements where starts_at<end_t and ends_at>start_t)>=20 then raise exception 'Too many overlapping announcements';end if;
  insert into public.gunny_announcements(target,title,body,starts_at,ends_at)
   values(uid,trim(p_data->>'title'),trim(p_data->>'body'),start_t,end_t) returning to_jsonb(gunny_announcements.*) into result;
  insert into public.gunny_admin_audit(action,target,detail) values(p_op,coalesce(uid::text,'all'),result);return result;
 elsif p_op='announcement-delete' then
  delete from public.gunny_announcements where id=(p_data->>'id')::uuid;
  insert into public.gunny_admin_audit(action,target) values(p_op,p_data->>'id');return '{}';
 elsif p_op='maintenance' then
  n:=0;
  insert into public.gunny_account_admin(user_id) select id from auth.users on conflict do nothing;
  for a in select * from public.gunny_account_admin where delete_at<=t or public.gunny_inactive_deadline(user_id)<=t order by user_id for update skip locked loop
   insert into public.gunny_admin_audit(action,target,detail) values(case when a.delete_at<=t then 'scheduled-delete' else 'inactive-delete' end,a.user_id::text,to_jsonb(a));
   delete from auth.users where id=a.user_id;n:=n+1;
  end loop;
  return jsonb_build_object('deleted',n);
 elsif p_op='presence' then
  for item in select value from jsonb_array_elements(p_data->'users') loop
   update public.gunny_account_admin set last_seen_at=greatest(last_seen_at,(item->>'at')::timestamptz) where user_id=(item->>'id')::uuid;
  end loop;return coalesce((select jsonb_agg(user_id) from public.gunny_account_admin where user_id in(select (value->>'id')::uuid from jsonb_array_elements(p_data->'users'))),'[]');
 end if;
 uid:=(p_data->>'id')::uuid;
 if p_op='wallet-add' then
  add_coins:=coalesce((p_data->>'coins')::integer,0);add_money:=coalesce((p_data->>'money')::integer,0);request_id:=(p_data->>'request_key')::uuid;
  if request_id is null or add_coins<0 or add_money<0 or add_coins>999999 or add_money>999999 or add_coins+add_money=0 or char_length(trim(coalesce(p_data->>'reason','')))=0 or char_length(p_data->>'reason')>300 then raise exception 'Invalid wallet grant';end if;
  perform 1 from public.profiles where id=uid for update;if not found then raise exception 'Account not found';end if;
  select * into grant_row from public.gunny_wallet_grants where request_key=request_id;
  if found then
   if grant_row.user_id<>uid::text or grant_row.coins<>add_coins or grant_row.money<>add_money or grant_row.reason<>p_data->>'reason' then raise exception 'Invalid request key';end if;
  else
   if exists(select 1 from public.profiles where id=uid and (coins+add_coins>999999 or shopping_money+add_money>999999)) then raise exception 'Wallet limit';end if;
   insert into public.gunny_wallet_grants(request_key,user_id,coins,money,reason) values(request_id,uid::text,add_coins,add_money,p_data->>'reason');
   update public.profiles set coins=coins+add_coins,shopping_money=shopping_money+add_money where id=uid;
   insert into public.gunny_admin_audit(action,target,detail) values(p_op,uid::text,p_data);
  end if;
  select jsonb_build_object('coins',coins,'money',shopping_money) into balance from public.profiles where id=uid;return balance;
 end if;
 if p_op='delete-now' then
  perform 1 from public.gunny_account_admin where user_id=uid for update;
  perform 1 from auth.users where id=uid for update;if not found then raise exception 'Account not found';end if;
  insert into public.gunny_admin_audit(action,target,detail) values(p_op,uid::text,p_data);
  delete from auth.users where id=uid;return '{}';
 end if;
 insert into public.gunny_account_admin(user_id) values(uid) on conflict do nothing;
 select * into strict a from public.gunny_account_admin where user_id=uid for update;
 if p_op='offline-days' then
  n:=(p_data->>'days')::integer;
  if n is not null and (n<1 or n>3650) then raise exception 'Days must be 1..3650';end if;
  update public.gunny_account_admin set offline_days=n where user_id=uid;
 elsif p_op='extend-offline' then
  perform (p_data->>'request_key')::uuid;
  if exists(select 1 from public.gunny_admin_audit where action=p_op and target=uid::text and detail->>'request_key'=p_data->>'request_key') then return '{}';end if;
  n:=(p_data->>'days')::integer;
  if n is null or n<1 or n>3650 then raise exception 'Days must be 1..3650';end if;
  update public.gunny_account_admin set protected_until=greatest(coalesce(public.gunny_inactive_deadline(uid),t),t)+make_interval(days=>n) where user_id=uid;
 elsif p_op='schedule' then
  start_t:=(p_data->>'delete_at')::timestamptz;if start_t is null or start_t<=t then raise exception 'Choose a future date';end if;
  if char_length(coalesce(p_data->>'appeal',''))>250 or char_length(coalesce(p_data->>'reason',''))>300 then raise exception 'Text too long';end if;
  update public.gunny_account_admin set delete_at=start_t,delete_reason=coalesce(p_data->>'reason',''),appeal=coalesce(p_data->>'appeal',''),cancel_on_login=coalesce((p_data->>'cancel_on_login')::boolean,true) where user_id=uid;
 elsif p_op='cancel-delete' then
  update public.gunny_account_admin set delete_at=null where user_id=uid;
 elsif p_op='login' then
  if a.delete_at<=t or public.gunny_inactive_deadline(uid)<=t then
   insert into public.gunny_admin_audit(action,target,detail) values('scheduled-delete',uid::text,to_jsonb(a));
   delete from auth.users where id=uid;return jsonb_build_object('blocked',true,'notices','[]'::jsonb);
  end if;
  if a.delete_at is not null then
   notices:=jsonb_build_array(jsonb_build_object('title','Lịch xoá tài khoản','appeal',a.appeal,'body',
    'Tài khoản dự kiến xoá vào '||to_char(a.delete_at at time zone 'Asia/Bangkok','DD/MM/YYYY HH24:MI')||' (giờ Việt Nam). Còn '||greatest(1,ceil(extract(epoch from (a.delete_at-t))/86400))::text||' ngày. '||a.delete_reason||
    case when a.cancel_on_login then ' Lịch xoá đã tự huỷ vì bạn đăng nhập trước hạn.' else ' Lịch xoá vẫn giữ nguyên; liên hệ quản trị viên để gia hạn.' end));
  end if;
  update public.gunny_account_admin set last_login_at=t,last_seen_at=t,
   max_offline_seconds=case when a.last_seen_at is null then a.max_offline_seconds else greatest(coalesce(a.max_offline_seconds,0),greatest(0,extract(epoch from(t-a.last_seen_at)))::bigint) end,
   delete_at=case when a.cancel_on_login then null else a.delete_at end where user_id=uid;
  select notices||coalesce(jsonb_agg(jsonb_build_object('title',title,'body',body) order by starts_at,id),'[]') into result
   from (select * from public.gunny_announcements where (target is null or target=uid) and starts_at<=t and ends_at>t order by starts_at,id limit 20) q;
  return jsonb_build_object('blocked',false,'notices',result);
 else raise exception 'Unknown operation';end if;
 insert into public.gunny_admin_audit(action,target,detail) values(p_op,uid::text,p_data);return '{}';
end;$$;
revoke all on function public.gunny_admin(text,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_admin(text,jsonb) to service_role;
do $preserve$
begin
 if exists(select 1 from gunny_770_preserve old
  left join auth.users u on u.id=old.id left join public.profiles p on p.id=old.id
  where u.id is null or u.email is distinct from old.email
   or u.encrypted_password is distinct from old.encrypted_password
   or to_jsonb(p) is distinct from old.profile) then
  raise exception 'Upgrade cancelled: existing login credentials or game profile changed';
 end if;
end;
$preserve$;
commit;
notify pgrst,'reload schema';
