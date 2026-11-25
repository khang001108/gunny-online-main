-- HF17: current uninterrupted inactivity, not the date the policy was enabled.
-- Review HF17-Preview-Inactivity.sql first. Existing manual schedules and extensions are preserved.
begin;
-- Only change to 'yes' AFTER reviewing overdue accounts in the preview.
set local gunny.hf17_confirm_overdue = 'no';

create or replace function public.gunny_inactive_deadline(p_user uuid) returns timestamptz
language sql stable security definer set search_path='' as $$
 select case when policy.enabled then greatest(
  greatest(a.last_seen_at,a.last_login_at,u.created_at)
    + make_interval(secs=>86400::double precision*coalesce(a.offline_days,policy.offline_days)),
  a.protected_until) else null end
 from auth.users u left join public.gunny_account_admin a on a.user_id=u.id
 cross join public.gunny_retention_policy policy where u.id=p_user and policy.singleton;
$$;
revoke all on function public.gunny_inactive_deadline(uuid) from public,anon,authenticated;
grant execute on function public.gunny_inactive_deadline(uuid) to service_role;

-- Do not silently expose previously protected-by-policy-date accounts to automatic deletion.
do $$
declare overdue bigint;
begin
 select count(*) into overdue from auth.users u
 where public.gunny_inactive_deadline(u.id)<=clock_timestamp();
 if overdue>0 and current_setting('gunny.hf17_confirm_overdue',true) is distinct from 'yes' then
  raise exception 'HF17: % accounts already past the new inactivity deadline. Review preview before confirming.', overdue;
 end if;
end $$;

-- Keep the RPC name used by deployed HF15/HF16. No bulk reset on enabling.
create or replace function public.gunny_retention_save_hf15(p_days integer,p_enabled boolean)
returns jsonb language plpgsql security definer set search_path='' as $$
declare policy public.gunny_retention_policy%rowtype; t timestamptz;
begin
 if p_days is null or p_days<1 or p_days>3650 or p_enabled is null then raise exception 'Invalid retention policy' using errcode='22023';end if;
 select * into strict policy from public.gunny_retention_policy where gunny_retention_policy.singleton for update;
 t:=clock_timestamp();
 update public.gunny_retention_policy set offline_days=p_days,enabled=p_enabled,updated_at=t where gunny_retention_policy.singleton;
 if p_enabled and not policy.enabled then
  -- Queued explicit grants only; never grant a new window to every account.
  update public.gunny_account_admin account set
   protected_until=greatest(public.gunny_inactive_deadline(account.user_id),t)+make_interval(days=>account.pending_extension_days),
   pending_extension_days=0
  where account.pending_extension_days>0;
 end if;
 insert into public.gunny_admin_audit(action,target,detail) values('retention-save','all',jsonb_build_object('days',p_days,'enabled',p_enabled));
 return (select to_jsonb(p) from public.gunny_retention_policy p where p.singleton);
end;
$$;
revoke all on function public.gunny_retention_save_hf15(integer,boolean) from public,anon,authenticated;
grant execute on function public.gunny_retention_save_hf15(integer,boolean) to service_role;
notify pgrst,'reload schema';
commit;
