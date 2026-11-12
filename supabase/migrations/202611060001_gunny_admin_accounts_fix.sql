-- Fix account-list PL/pgSQL name ambiguity. Does not delete or reset accounts.
begin;
create or replace function public.gunny_admin(p_op text,p_data jsonb default '{}') returns jsonb
language plpgsql security definer set search_path='' as $$
declare uid uuid; a public.gunny_account_admin%rowtype; result jsonb; notices jsonb:='[]';
 t timestamptz:=clock_timestamp(); start_t timestamptz;end_t timestamptz; n integer; item jsonb;
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
