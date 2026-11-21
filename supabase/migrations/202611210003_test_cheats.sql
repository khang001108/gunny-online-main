begin;
alter table public.profiles add column if not exists test_cheats boolean not null default false,
 add column if not exists test_god boolean not null default false;
create or replace function public.gunny_test_cheat(p_user uuid,p_action integer,p_enabled boolean default false)
returns void language plpgsql security definer set search_path='' as $$
declare p public.profiles%rowtype;k integer;i integer;idx integer;
begin
 select * into strict p from public.profiles where id=p_user for update;
 if p_action=6 then update public.profiles set test_cheats=p_enabled,test_god=test_god and p_enabled where id=p_user;return;end if;
 if not p.test_cheats then raise exception 'Test cheats disabled';end if;
 case p_action
 when 0 then update public.profiles set stones=999999,craft_stones=array_fill(999999,array[4]),lucky_charms=999999,protection_charms=999999 where id=p_user;
 when 1 then
  for k in 0..6 loop for i in 0..9 loop
   if k in(0,2,3,6) and ((k=0 and (p.owned&(1<<i))<>0) or (k>0 and (p.wardrobe[p.gender*6+k]&(1<<i))<>0)) then
    idx:=case when k=0 then i+1 else 11+p.gender*60+(k-1)*10+i end;
    update public.profiles set forge_levels[idx]=15,forge_progress[idx]=0 where id=p_user;
   end if;
  end loop;end loop;
 when 2 then update public.profiles set experience=120750 where id=p_user;
 when 3 then update public.profiles set shopping_money=999999,coins=999999 where id=p_user;
 when 4 then update public.profiles set test_god=p_enabled where id=p_user;
 else raise exception 'Invalid test cheat';end case;
end;$$;
revoke all on function public.gunny_test_cheat(uuid,integer,boolean) from public,anon,authenticated;
grant execute on function public.gunny_test_cheat(uuid,integer,boolean) to service_role;
notify pgrst,'reload schema';
commit;
