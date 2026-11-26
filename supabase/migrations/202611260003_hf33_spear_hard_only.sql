-- HF33: direct spear and duplicate compensation only for Hard Sulan.
-- Existing ownership and normal final gift trigger are unchanged.
BEGIN;
create or replace function public.gunny_hf19_spear_award()
returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.stage=4 and new.difficulty=2 and new.winner=0 then
  -- SET expressions read the pre-update row: only existing owners get the extra bag.
  -- The original gunny_tribal_final_gift trigger still grants the normal bag.
  update public.profiles p
   set gift_bags=least(999999,p.gift_bags+case when (p.owned & 256)<>0 then 1 else 0 end),
       owned=p.owned | 256,
       vip_spears=greatest(p.vip_spears,1)
   where p.id in (select (x->>'id')::uuid from pg_catalog.jsonb_array_elements(new.players) x
    where (x->>'team')::integer=0);
 end if;
 return new;
end;$$;
revoke all on function public.gunny_hf19_spear_award() from public,anon,authenticated;
drop trigger if exists gunny_hf19_spear_award on public.gunny_tribal_matches;
create trigger gunny_hf19_spear_award after insert on public.gunny_tribal_matches
for each row execute function public.gunny_hf19_spear_award();
NOTIFY pgrst,'reload schema';
COMMIT;
