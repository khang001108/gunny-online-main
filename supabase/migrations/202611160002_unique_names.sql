-- Preserve the first owner of a name. Older duplicate names receive a short suffix.
begin;
lock table public.profiles in share row exclusive mode;
do $$
declare player record; candidate text; suffix text; attempt integer;
begin
 for player in
  select id,display_name,public_id from (
   select id,display_name,public_id,row_number() over (
    partition by lower(normalize(btrim(display_name),NFC)) order by public_id,id
   ) as ordinal from public.profiles
  ) names where ordinal>1 order by public_id,id
 loop
  attempt:=0;
  loop
   suffix:=case when attempt=0 then '#'||player.public_id::text else '#'||substr(md5(player.id::text||attempt::text),1,8) end;
   candidate:=left(btrim(player.display_name),greatest(0,13-char_length(suffix)))||suffix;
   exit when not exists(select 1 from public.profiles where id<>player.id
    and lower(normalize(btrim(display_name),NFC))=lower(normalize(candidate,NFC)));
   attempt:=attempt+1;
  end loop;
  update public.profiles set display_name=candidate where id=player.id;
  if to_regclass('public.gunny_admin_audit') is not null then
   insert into public.gunny_admin_audit(action,target,detail) values('unique-name-rename',player.id::text,
    jsonb_build_object('old_name',player.display_name,'new_name',candidate));
  end if;
 end loop;
end $$;
create unique index if not exists gunny_profiles_name_unique
 on public.profiles(lower(normalize(btrim(display_name),NFC)));
create or replace function public.gunny_name_available(p_name text,p_except uuid default null)
returns boolean language sql stable security definer set search_path='' as $$
 select not exists(select 1 from public.profiles where id is distinct from p_except
  and lower(normalize(btrim(display_name),NFC))=lower(normalize(btrim(p_name),NFC)));
$$;
revoke all on function public.gunny_name_available(text,uuid) from public,anon,authenticated;
grant execute on function public.gunny_name_available(text,uuid) to service_role;
notify pgrst,'reload schema';
commit;
