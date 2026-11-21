begin;
create or replace function public.gunny_stone_grade(p_difficulty integer,p_roll integer)
returns integer language plpgsql immutable set search_path='' as $$
declare weights integer[];i integer;total integer:=0;
begin
 if p_difficulty not between 0 and 2 or p_roll not between 0 and 99 then raise exception 'Invalid stone roll';end if;
 weights:=case p_difficulty when 0 then array[65,25,8,2,0] when 1 then array[25,35,25,12,3] else array[5,15,30,35,15] end;
 for i in 1..5 loop total:=total+weights[i];if p_roll<total then return i;end if;end loop;
 raise exception 'Invalid weights';
end;$$;
alter table public.profiles
 add column if not exists wings integer not null default 0 check(wings between 0 and 1),
 add column if not exists wing_equipped boolean not null default false,
 add column if not exists vip_spears integer not null default 0 check(vip_spears between 0 and 999999);
create table if not exists public.gunny_tribal_runs(
 id uuid primary key,stage integer not null check(stage between 1 and 4),
 difficulty integer not null check(difficulty between 0 and 2),turn integer not null,
 members jsonb not null,state jsonb not null,updated_at timestamptz not null default now());
create table if not exists public.gunny_tribal_matches(
 id uuid primary key,run_id uuid not null,stage integer not null check(stage between 1 and 4),
 difficulty integer not null check(difficulty between 0 and 2),winner integer not null check(winner between 0 and 2),
 players jsonb not null,deck integer[] not null,stone_min integer not null,stone_max integer not null,
 created_at timestamptz not null default now(),check(cardinality(deck)=18),check(stone_min between 1 and 300 and stone_max between stone_min and 300));
create table if not exists public.gunny_tribal_rewards(
 match_id uuid not null references public.gunny_tribal_matches(id),
 user_id uuid not null references public.profiles(id),pick integer not null check(pick between 0 and 1),
 card integer not null check(card between 0 and 17),result jsonb not null,
 primary key(match_id,user_id,pick),unique(match_id,card));
alter table public.gunny_tribal_runs enable row level security;
alter table public.gunny_tribal_matches enable row level security;
alter table public.gunny_tribal_rewards enable row level security;
revoke all on public.gunny_tribal_runs,public.gunny_tribal_matches,public.gunny_tribal_rewards from public,anon,authenticated;
create or replace function public.gunny_tribal(p_action text,p_payload jsonb)
returns jsonb language plpgsql security definer set search_path='' as $$
declare
 m public.gunny_tribal_matches%rowtype;r public.gunny_tribal_rewards%rowtype;
 mid uuid;uid uuid;stage_no integer;diff integer;win integer;lo integer;hi integer;
 slot integer;pick_no integer;kind integer;amount integer;position integer;i integer;
 deck integer[]:=array_fill(0,array[18]);positions integer[];member jsonb;result jsonb;
begin
 if p_action is null or p_payload is null or jsonb_typeof(p_payload)<>'object' then raise exception 'Invalid tribal request';end if;
 if p_action='status' then return '{}'::jsonb;end if;
 if p_action='checkpoint' then
  insert into public.gunny_tribal_runs(id,stage,difficulty,turn,members,state)
   values((p_payload->>'run')::uuid,(p_payload->>'stage')::integer,(p_payload->>'difficulty')::integer,
    (p_payload->>'turn')::integer,p_payload->'members',p_payload->'state')
   on conflict(id) do update set stage=excluded.stage,difficulty=excluded.difficulty,turn=excluded.turn,
    members=excluded.members,state=excluded.state,updated_at=now()
   where (excluded.stage,excluded.turn)>=(gunny_tribal_runs.stage,gunny_tribal_runs.turn);
  return '{}'::jsonb;
 end if;
 if p_action='wing' then
  uid:=(p_payload->>'user')::uuid;
  perform 1 from public.profiles where id=uid and (not (p_payload->>'equip')::boolean or wings>0) for update;
  if not found then raise exception 'Item not owned';end if;
  update public.profiles set wing_equipped=(p_payload->>'equip')::boolean where id=uid;
  return '{}'::jsonb;
 end if;
 mid:=(p_payload->>'match')::uuid;
 perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('tribal:'||mid::text,0));
 if p_action='finish' then
  if exists(select 1 from public.gunny_tribal_matches where id=mid) then return '{}'::jsonb;end if;
  stage_no:=(p_payload->>'stage')::integer;diff:=(p_payload->>'difficulty')::integer;win:=(p_payload->>'winner')::integer;
  lo:=(p_payload->>'min')::integer;hi:=(p_payload->>'max')::integer;
  if stage_no not between 1 and 4 or diff not between 0 and 2 or win not between 0 and 2
   or jsonb_array_length(p_payload->'players') not between 1 and 3 then raise exception 'Invalid tribal match';end if;
  if (select count(distinct x->>'id') from jsonb_array_elements(p_payload->'players') x)<>jsonb_array_length(p_payload->'players') then raise exception 'Duplicate player';end if;
  for member in select value from jsonb_array_elements(p_payload->'players') loop
   perform 1 from public.profiles where id=(member->>'id')::uuid for update;
   if not found or (member->>'team')::integer<>0 then raise exception 'Invalid tribal player';end if;
  end loop;
  if stage_no=4 then
   select array_agg(n order by random()) into positions from generate_series(1,18) n;
   position:=1;
   if diff=2 then deck[positions[position]]:=2;position:=position+1;end if;
   for i in 1..(case diff when 1 then 3 when 2 then 6 else 0 end) loop
    deck[positions[position]]:=3;position:=position+1;
   end loop;
  end if;
  insert into public.gunny_tribal_matches values(mid,(p_payload->>'run')::uuid,stage_no,diff,win,p_payload->'players',deck,lo,hi,now());
  for member in select value from jsonb_array_elements(p_payload->'players') loop
   update public.profiles set experience=least(2147483647::bigint,experience::bigint+case when win=0 then 150*(diff+1) else 50 end),
    coins=least(999999,coins+case when win=0 then 200*(diff+1) else 0 end),wins=wins+case when win=0 and stage_no=4 then 1 else 0 end
    where id=(member->>'id')::uuid;
  end loop;
  return '{}'::jsonb;
 end if;
 if p_action<>'claim' then raise exception 'Invalid tribal action';end if;
 uid:=(p_payload->>'user')::uuid;pick_no:=(p_payload->>'pick')::integer;slot:=(p_payload->>'card')::integer;
 select * into strict m from public.gunny_tribal_matches where id=mid for update;
 if m.winner<>0 or pick_no not between 0 and 1 or slot not between 0 and 17
  or not exists(select 1 from jsonb_array_elements(m.players) x where (x->>'id')::uuid=uid) then raise exception 'Invalid tribal reward';end if;
 select * into r from public.gunny_tribal_rewards where match_id=mid and user_id=uid and pick=pick_no;
 if found then if r.card<>slot then raise exception 'Reward payload mismatch';end if;return r.result;end if;
 if pick_no=1 and not exists(select 1 from public.gunny_tribal_rewards where match_id=mid and user_id=uid and pick=0) then raise exception 'Pick first card';end if;
 if exists(select 1 from public.gunny_tribal_rewards where match_id=mid and card=slot) then raise exception 'Card already taken';end if;
 kind:=m.deck[slot+1];amount:=1;
 if kind=0 then kind:=public.gunny_stone_grade(m.difficulty,floor(random()*100)::integer);kind:=case when kind=1 then 1 else kind+2 end;amount:=m.stone_min+floor(random()*(m.stone_max-m.stone_min+1))::integer;end if;
 update public.profiles set stones=least(999999,stones+case when kind=1 then amount else 0 end),
  craft_stones=case when kind>=4 then array[least(999999,craft_stones[1]+case when kind=4 then amount else 0 end),least(999999,craft_stones[2]+case when kind=5 then amount else 0 end),least(999999,craft_stones[3]+case when kind=6 then amount else 0 end),least(999999,craft_stones[4]+case when kind=7 then amount else 0 end)] else craft_stones end,
  lucky_charms=least(999999,lucky_charms+1),wings=wings|case when kind=2 then 1 else 0 end,
  wing_equipped=wing_equipped or kind=2,owned=owned|case when kind=3 then 256 else 0 end,
  vip_spears=least(999999,vip_spears+case when kind=3 then 1 else 0 end) where id=uid;
 result:=jsonb_build_object('card',slot,'type',kind,'amount',amount,'pick',pick_no);
 insert into public.gunny_tribal_rewards values(mid,uid,pick_no,slot,result);
 return result;
end $$;
revoke all on function public.gunny_tribal(text,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_tribal(text,jsonb) to service_role;
create or replace function public.gunny_equip(p_user uuid,p_kind integer,p_item integer) returns void language plpgsql security definer set search_path='' as $$
declare sex integer;owned_mask integer;ward integer[];parts integer[];mask integer;weapon integer;skin_id integer;
begin
 if p_kind is null or p_kind not between 0 and 6 or p_item is null or p_item not between -1 and 9 then raise exception 'Invalid equipment';end if;
 select gender,owned,wardrobe,look,worn_mask,equipped,skin into strict sex,owned_mask,ward,parts,mask,weapon,skin_id from public.profiles where id=p_user for update;
 if p_item>=0 then
  if p_kind=0 then if p_item not in(0,1,2,3,4,5,7,8,9) or (owned_mask&(1<<p_item))=0 then raise exception 'Item not owned';end if;weapon:=p_item;
  else if (ward[sex*6+p_kind]&(1<<p_item))=0 then raise exception 'Item not owned';end if;if p_kind=1 then skin_id:=p_item;else parts[p_kind-1]:=p_item;end if;end if;
 end if;
 if p_item<0 then mask:=mask & ~(1<<p_kind);
 elsif p_kind=1 then mask:=(mask&1)|2;
 elsif p_kind>=2 then mask:=(mask & (~2))|(1<<p_kind);
 else mask:=mask|1;end if;
 update public.profiles set equipped=weapon,skin=skin_id,look=parts,worn_mask=mask,custom_look=((mask&2)=0) where id=p_user;
end;$$;

notify pgrst,'reload schema';
commit;
