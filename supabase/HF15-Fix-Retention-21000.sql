-- Fix HF15 retention save: explicit WHERE for account grace-window update.
-- Replaces only the RPC; does not change policy or player data when installed.
begin;
create or replace function public.gunny_retention_save_hf15(p_days integer,p_enabled boolean)
returns jsonb language plpgsql security definer set search_path='' as $$
declare n integer; policy public.gunny_retention_policy%rowtype; t timestamptz;
begin
  n:=p_days;if n is null or n<1 or n>3650 or p_enabled is null then raise exception 'Invalid retention policy' using errcode='22023';end if;
  -- Same lock order as extend-offline: policy, then account rows.
  select * into strict policy from public.gunny_retention_policy where gunny_retention_policy.singleton for update;
  t:=clock_timestamp();
  update public.gunny_retention_policy set offline_days=n,enabled=p_enabled,updated_at=t where gunny_retention_policy.singleton;
  if p_enabled and not policy.enabled then
   update public.gunny_account_admin
   set retention_since=t
   where retention_since<t;
   -- A disabled policy has no running deadline: apply queued days to the newly granted window.
   -- greatest() also preserves any longer absolute protection already granted previously.
   update public.gunny_account_admin account set
    protected_until=greatest(coalesce(public.gunny_inactive_deadline(account.user_id),t),t)+make_interval(days=>account.pending_extension_days),
    pending_extension_days=0
   where account.pending_extension_days>0;
  end if;
  insert into public.gunny_admin_audit(action,target,detail) values('retention-save','all',jsonb_build_object('days',n,'enabled',p_enabled));
  return (select to_jsonb(p) from public.gunny_retention_policy p where p.singleton);
end;
$$;
revoke all on function public.gunny_retention_save_hf15(integer,boolean) from public,anon,authenticated;
grant execute on function public.gunny_retention_save_hf15(integer,boolean) to service_role;
notify pgrst,'reload schema';

commit;
