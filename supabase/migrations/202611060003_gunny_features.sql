-- After all 7.5.2 migrations. Existing players keep all possessions and balances.
begin;
alter table public.profiles drop constraint if exists profiles_owned_check;
alter table public.profiles add constraint profiles_owned_check check(owned between 0 and 1023);
alter table public.profiles alter column owned set default 0;
alter table public.profiles alter column worn_mask set default 0;
alter table public.profiles alter column custom_look set default true;
alter table public.profiles alter column wardrobe set default '{0,0,0,0,0,0,0,0,0,0,0,0}';
alter table public.profiles add column if not exists bag_layout integer[] not null default array_fill(-1,array[175]);
alter table public.profiles drop constraint if exists gunny_bag_layout_valid;
alter table public.profiles add constraint gunny_bag_layout_valid check(array_ndims(bag_layout)=1 and cardinality(bag_layout)=175 and array_lower(bag_layout,1)=1 and array_position(bag_layout,null) is null and -1<=all(bag_layout) and 69>=all(bag_layout));
create or replace function public.gunny_new_user() returns trigger language plpgsql security definer set search_path='' as $$
begin insert into public.profiles(id,display_name,owned,worn_mask,custom_look,wardrobe,character_chosen) values(new.id,left(coalesce(nullif(trim(new.raw_user_meta_data->>'display_name'),''),'Gunner'),13),0,0,true,array_fill(0,array[12]),false) on conflict do nothing;return new;end;$$;
create or replace function public.gunny_checkout(p_user uuid,p_sex integer,p_cart integer[]) returns void language plpgsql security definer set search_path='' as $$
declare v_money integer;v_gender integer;v_owned integer;v_wardrobe integer[]; cost integer:=0;k integer;i integer;m integer;owned_mask integer;price integer;
begin
 if p_sex is null or p_sex not between 0 and 1 or array_ndims(p_cart) is distinct from 1 or array_length(p_cart,1) is distinct from 7 or array_lower(p_cart,1) is distinct from 1 then raise exception 'Invalid cart';end if;
 select shopping_money,gender,owned,wardrobe into strict v_money,v_gender,v_owned,v_wardrobe from public.profiles where id=p_user for update;
 if v_gender<>p_sex then raise exception 'Invalid character';end if;
 for k in 0..6 loop
  m:=p_cart[k+1];if m is null or m<0 or m>1023 then raise exception 'Invalid cart';end if;
  owned_mask:=case when k=0 then v_owned else v_wardrobe[p_sex*6+k] end;
  for i in 0..9 loop
   if (m & (1<<i))<>0 then
    if k=0 and i not in(0,1,2,3,4,5,7,9) then raise exception 'Invalid weapon';end if;
    price:=case when i=0 then 0 when k=0 then 180 when k=1 then 350 when k=2 then 200 when k=3 then 130 when k=5 then 35 else 100 end;
    if (owned_mask & (1<<i))=0 then cost:=cost+price;end if;
   end if;
  end loop;
 end loop;
 if v_money<cost then raise exception 'Insufficient coins';end if;
 for k in 1..6 loop v_wardrobe[p_sex*6+k]:=v_wardrobe[p_sex*6+k] | p_cart[k+1];end loop;
 update public.profiles set shopping_money=v_money-cost,owned=v_owned | p_cart[1],wardrobe=v_wardrobe where id=p_user;
end;$$;
revoke all on function public.gunny_checkout(uuid,integer,integer[]) from public,anon,authenticated;
grant execute on function public.gunny_checkout(uuid,integer,integer[]) to service_role;

create or replace function public.gunny_bag_layout(p_user uuid,p_layout integer[]) returns void language plpgsql security definer set search_path='' as $$
declare p public.profiles%rowtype;idx integer;v integer;k integer;item integer;sex integer;seen text[]:='{}';key text;
begin
 if array_ndims(p_layout) is distinct from 1 or array_lower(p_layout,1) is distinct from 1 or cardinality(p_layout) is distinct from 175 or array_position(p_layout,null) is not null then raise exception 'Invalid bag layout';end if;
 select * into strict p from public.profiles where id=p_user for update;
 for idx in 1..175 loop
  v:=p_layout[idx];if v=-1 then continue;end if;
  if v<0 or v>69 then raise exception 'Invalid item';end if;k:=v/10;item:=v%10;sex:=case when idx<=100 then 0 else 1 end;
  if idx<=25 then
   if k<>0 or item not in(0,1,2,3,4,5,7,9) or (p.owned&(1<<item))=0 then raise exception 'Item not owned';end if;key:='w:'||v;
  else
   if k<1 or (p.wardrobe[sex*6+k]&(1<<item))=0 then raise exception 'Item not owned';end if;key:=sex||':'||v;
  end if;
  if key=any(seen) then raise exception 'Duplicate item';end if;seen:=array_append(seen,key);
 end loop;
 update public.profiles set bag_layout=p_layout where id=p_user;
end;$$;
revoke all on function public.gunny_bag_layout(uuid,integer[]) from public,anon,authenticated;
grant execute on function public.gunny_bag_layout(uuid,integer[]) to service_role;
create table if not exists public.gunny_wallet_grants(request_key uuid primary key,user_id text not null,coins integer not null,money integer not null,reason text not null,created_at timestamptz not null default now());
alter table public.gunny_wallet_grants enable row level security;
revoke all on public.gunny_wallet_grants from public,anon,authenticated;
grant all on public.gunny_wallet_grants to service_role;
create or replace function public.gunny_admin(p_op text,p_data jsonb default '{}') returns jsonb
language plpgsql security definer set search_path='' as $$
declare uid uuid; a public.gunny_account_admin%rowtype; result jsonb; notices jsonb:='[]';
 t timestamptz:=clock_timestamp(); start_t timestamptz;end_t timestamptz; n integer; item jsonb; grant_row public.gunny_wallet_grants%rowtype;add_coins integer;add_money integer;request_id uuid;balance jsonb;
begin
 if p_op='accounts' then
  select coalesce(jsonb_agg(r),'[]') into result from (
   select u.id,u.email,coalesce(p.display_name,'Chưa tạo nhân vật') display_name,p.public_id,
    u.created_at,ac.last_login_at,ac.last_seen_at,ac.max_offline_seconds,ac.delete_at,ac.delete_reason,ac.appeal,ac.cancel_on_login,
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
  for a in select * from public.gunny_account_admin where delete_at<=t order by user_id for update skip locked loop
   insert into public.gunny_admin_audit(action,target,detail) values('scheduled-delete',a.user_id::text,to_jsonb(a));
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
 if p_op='schedule' then
  start_t:=(p_data->>'delete_at')::timestamptz;if start_t is null or start_t<=t then raise exception 'Choose a future date';end if;
  if char_length(coalesce(p_data->>'appeal',''))>250 or char_length(coalesce(p_data->>'reason',''))>300 then raise exception 'Text too long';end if;
  update public.gunny_account_admin set delete_at=start_t,delete_reason=coalesce(p_data->>'reason',''),appeal=coalesce(p_data->>'appeal',''),cancel_on_login=coalesce((p_data->>'cancel_on_login')::boolean,true) where user_id=uid;
 elsif p_op='cancel-delete' then
  update public.gunny_account_admin set delete_at=null where user_id=uid;
 elsif p_op='login' then
  if a.delete_at<=t then
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

notify pgrst,'reload schema';
commit;
