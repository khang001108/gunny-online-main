begin;

alter table public.profiles
 add column if not exists craft_stones integer[] not null default array_fill(0,array[4]),
 add column if not exists lucky_charms integer not null default 0,
 add column if not exists protection_charms integer not null default 0;

do $$begin
 if not exists(select 1 from pg_constraint where conrelid='public.profiles'::regclass and conname='gunny_craft_materials') then
  alter table public.profiles add constraint gunny_craft_materials check(
   array_ndims(craft_stones)=1 and array_lower(craft_stones,1)=1 and cardinality(craft_stones)=4
   and array_position(craft_stones,null) is null and 0<=all(craft_stones) and 999999>=all(craft_stones)
   and lucky_charms between 0 and 999999 and protection_charms between 0 and 999999);
 end if;
end$$;

create table if not exists public.gunny_craft_receipts(
 request_key text primary key,
 user_id uuid not null references public.profiles(id) on delete cascade,
 action text not null,
 payload jsonb not null,
 roll integer not null check(roll between 0 and 9999),
 created_at timestamptz not null default now()
);
alter table public.gunny_craft_receipts enable row level security;
revoke all on public.gunny_craft_receipts from public,anon,authenticated;

create or replace function public.gunny_crafting(
 p_user uuid,p_action text,p_payload jsonb,p_key text,p_roll integer)
returns void language plpgsql security definer set search_path='' as $$
declare
 receipt public.gunny_craft_receipts%rowtype;
 p public.profiles%rowtype;
 qty integer;material integer;price integer;owned integer;
 sex integer;kind integer;item integer;rank integer;grade integer;idx integer;
 lucky boolean;protect boolean;chance integer;
 rates integer[]:=array[8000,6000,4000,2500,1500,900,500,250,120,60,30,15,8,4,2];
begin
 if p_user is null or p_action is null or p_action not in('buy','forge')
  or p_payload is null or jsonb_typeof(p_payload)<>'object'
  or p_key is null or p_key!~'^[A-Za-z0-9_-]{1,64}$'
  or p_roll is null or p_roll not between 0 and 9999 then raise exception 'Invalid crafting request';end if;
 perform pg_advisory_xact_lock(hashtextextended(p_key,784));
 select * into receipt from public.gunny_craft_receipts where request_key=p_key;
 if found then
  if receipt.user_id<>p_user or receipt.action<>p_action or receipt.payload<>p_payload
   then raise exception 'Request key payload mismatch';end if;
  return;
 end if;
 select * into strict p from public.profiles where id=p_user for update;
 qty:=(p_payload->>'quantity')::integer;
 if p_action='buy' then
  material:=(p_payload->>'material')::integer;
  if material is null or material not between 0 and 6 or qty is null or qty not between 1 and 1000
   or p_payload<>jsonb_build_object('material',material,'quantity',qty) then raise exception 'Invalid material';end if;
  price:=case when material<5 then 80*(1<<material) when material=5 then 300 else 500 end;
  owned:=case when material=0 then p.stones when material<5 then p.craft_stones[material] when material=5 then p.lucky_charms else p.protection_charms end;
  if p.shopping_money<price*qty or owned>999999-qty then raise exception 'Insufficient money or material limit';end if;
  update public.profiles set shopping_money=shopping_money-price*qty where id=p_user;
  if material=0 then update public.profiles set stones=stones+qty where id=p_user;
  elsif material<5 then update public.profiles set craft_stones[material]=craft_stones[material]+qty where id=p_user;
  elsif material=5 then update public.profiles set lucky_charms=lucky_charms+qty where id=p_user;
  else update public.profiles set protection_charms=protection_charms+qty where id=p_user;end if;
 else
  sex:=(p_payload->>'sex')::integer;kind:=(p_payload->>'kind')::integer;
  item:=(p_payload->>'item')::integer;rank:=(p_payload->>'rank')::integer;
  grade:=(p_payload->>'grade')::integer;lucky:=(p_payload->>'lucky')::boolean;
  protect:=(p_payload->>'protect')::boolean;
  if sex is null or sex<>p.gender or kind is null or kind not in(0,2,3,6)
   or item is null or item not between 0 and 9 or rank is null or rank not between 0 and 14
   or grade is null or grade not between 1 and 5 or qty is null or qty not between 1 and 3
   or lucky is null or protect is null
   or p_payload<>jsonb_build_object('sex',sex,'kind',kind,'item',item,'rank',rank,
          'grade',grade,'quantity',qty,'lucky',lucky,'protect',protect) then raise exception 'Invalid forge request';end if;
  if kind=0 then
   if item not in(0,1,2,3,4,5,7,9) or (p.owned&(1<<item))=0 then raise exception 'Item not owned';end if;
   idx:=item+1;
  else
   if (p.wardrobe[sex*6+kind]&(1<<item))=0 then raise exception 'Item not owned';end if;
   idx:=11+sex*60+(kind-1)*10+item;
  end if;
  if p.forge_levels[idx]<>rank then raise exception 'Forge level changed';end if;
  owned:=case when grade=1 then p.stones else p.craft_stones[grade-1] end;
  if owned<qty or lucky and p.lucky_charms<1 or protect and p.protection_charms<1 then raise exception 'Insufficient stones or charms';end if;
  chance:=least(10000,rates[rank+1]*(1<<(grade-1))*qty*case when lucky then 125 else 100 end/100);
  if grade=1 then update public.profiles set stones=stones-qty where id=p_user;
  else update public.profiles set craft_stones[grade-1]=craft_stones[grade-1]-qty where id=p_user;end if;
  update public.profiles set
   lucky_charms=lucky_charms-case when lucky then 1 else 0 end,
   protection_charms=protection_charms-case when protect then 1 else 0 end,
   forge_levels[idx]=(case when p_roll<chance then rank+1 when rank>=5 and not protect then rank-1 else rank end)::smallint,
   forge_progress[idx]=0 where id=p_user;
 end if;
 insert into public.gunny_craft_receipts(request_key,user_id,action,payload,roll) values(p_key,p_user,p_action,p_payload,p_roll);
end;$$;

revoke all on function public.gunny_crafting(uuid,text,jsonb,text,integer) from public,anon,authenticated;
grant execute on function public.gunny_crafting(uuid,text,jsonb,text,integer) to service_role;

create or replace function public.gunny_dungeon_charms()
returns trigger language plpgsql security definer set search_path='' as $$
declare kind integer;
begin
 select room_kind into kind from public.match_results where id=new.match_id;
 if kind in(3,4) then
  update public.profiles set lucky_charms=least(999999,lucky_charms+1),
   protection_charms=least(999999,protection_charms+case when kind=3 then 1 else 0 end)
   where id=new.user_id;
 end if;
 return new;
end;$$;
revoke all on function public.gunny_dungeon_charms() from public,anon,authenticated;
drop trigger if exists gunny_dungeon_charms on public.gunny780_card_rewards;
create trigger gunny_dungeon_charms after insert on public.gunny780_card_rewards
 for each row execute function public.gunny_dungeon_charms();
notify pgrst,'reload schema';
commit;
