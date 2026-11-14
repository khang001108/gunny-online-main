-- Gunny 7.8.0: additive economy and dungeon progression. Run MANUALLY after review.
-- No account reset/deletion, no change to authentication, retention or existing rewards.
begin;
lock table public.profiles in share row exclusive mode;
create temporary table gunny780_preserve on commit drop as
 select id,to_jsonb(p)-'stones'-'forge_levels'-'dungeon_normal_clear' as value from public.profiles p;
alter table public.profiles add column if not exists stones integer not null default 0;
alter table public.profiles add column if not exists forge_levels smallint[] not null default array_fill(0::smallint,array[130]);
alter table public.profiles add column if not exists dungeon_normal_clear boolean not null default false;
do $$begin
 if not exists(select 1 from pg_constraint where conrelid='public.profiles'::regclass and conname='gunny780_stones') then
  alter table public.profiles add constraint gunny780_stones check(stones between 0 and 999999);
 end if;
 if not exists(select 1 from pg_constraint where conrelid='public.profiles'::regclass and conname='gunny780_forge_levels') then
  alter table public.profiles add constraint gunny780_forge_levels check(array_ndims(forge_levels)=1 and array_lower(forge_levels,1)=1 and cardinality(forge_levels)=130 and 0<=all(forge_levels) and 99>=all(forge_levels) and array_position(forge_levels,null) is null);
 end if;
end$$;
create table if not exists public.gunny780_requests(
 request_key text primary key,user_id uuid not null,operation text not null,payload jsonb not null,created_at timestamptz not null default now()
);
alter table public.gunny780_requests enable row level security;
revoke all on public.gunny780_requests from public,anon,authenticated;
grant all on public.gunny780_requests to service_role;
-- Receipts deliberately survive account deletion: a key cannot later be reused for another account.
create table if not exists public.gunny780_card_rewards(
 match_id uuid not null references public.match_results(id),user_id uuid not null references public.profiles(id) on delete cascade,
 pick smallint not null check(pick between 0 and 1),card smallint not null check(card between 0 and 17),
 coins integer not null default 0 check(coins between 0 and 2000),stones integer not null default 0 check(stones between 0 and 100),
 primary key(match_id,user_id,pick),unique(match_id,card),claimed_at timestamptz not null default now(),
 check((coins>0 and stones=0) or (stones>0 and coins=0))
);
alter table public.gunny780_card_rewards enable row level security;
revoke all on public.gunny780_card_rewards from public,anon,authenticated;
grant all on public.gunny780_card_rewards to service_role;
-- Existing room kinds and historical match/card rows are preserved.
alter table public.match_results add column if not exists dungeon_difficulty smallint;
alter table public.match_results drop constraint if exists match_results_room_kind_check;
do $$begin
 if not exists(select 1 from pg_constraint where conrelid='public.match_results'::regclass and conname='gunny780_room_kind') then
  alter table public.match_results add constraint gunny780_room_kind check(room_kind between 0 and 3);
 end if;
end$$;
create or replace function public.gunny_economy780(p_user uuid,p_operation text,p_payload jsonb,p_key text)
returns void language plpgsql security definer set search_path='' as $$
declare receipt public.gunny780_requests%rowtype; p public.profiles%rowtype;qty integer;sex integer;kind integer;item integer;rank integer;idx integer;cost integer;
begin
 if p_user is null or p_key is null or p_key !~ '^[A-Za-z0-9_-]{1,64}$' or p_payload is null or jsonb_typeof(p_payload)<>'object' or p_operation not in('buy','forge') or p_operation is null then raise exception 'Invalid economy request';end if;
 perform pg_advisory_xact_lock(hashtextextended(p_key,780));
 select * into receipt from public.gunny780_requests where request_key=p_key;
 if found then
  if receipt.user_id<>p_user or receipt.operation<>p_operation or receipt.payload<>p_payload then raise exception 'Request key payload mismatch';end if;
  return;
 end if;
 select * into strict p from public.profiles where id=p_user for update;
 if p_operation='buy' then
  qty:=(p_payload->>'quantity')::integer;
  if qty is null or qty not between 1 and 999 or p_payload<>jsonb_build_object('quantity',qty) then raise exception 'Invalid quantity';end if;
  cost:=qty*80;
  if p.shopping_money<cost or p.stones>999999-qty then raise exception 'Insufficient shopping money or stone limit';end if;
  update public.profiles set shopping_money=shopping_money-cost,stones=stones+qty where id=p_user;
 else
  sex:=(p_payload->>'sex')::integer;kind:=(p_payload->>'kind')::integer;item:=(p_payload->>'item')::integer;rank:=(p_payload->>'rank')::integer;
  if sex is null or kind is null or item is null or rank is null or sex<>p.gender or kind not in(0,2,3,6) or item not between 0 and 9 or rank not between 0 and 98 or p_payload<>jsonb_build_object('sex',sex,'kind',kind,'item',item,'rank',rank) then raise exception 'Invalid forge request';end if;
  if kind=0 then
   if item not in(0,1,2,3,4,5,7,9) or (p.owned&(1<<item))=0 then raise exception 'Item not owned';end if;
   idx:=item+1;
  else
   if (p.wardrobe[sex*6+kind]&(1<<item))=0 then raise exception 'Item not owned';end if;
   idx:=11+sex*60+(kind-1)*10+item;
  end if;
  if p.forge_levels[idx]<>rank then raise exception 'Forge level changed';end if;
  cost:=4*(rank+1);
  if p.stones<cost then raise exception 'Insufficient stones';end if;
  update public.profiles set stones=stones-cost,forge_levels[idx]=(rank+1)::smallint where id=p_user;
 end if;
 insert into public.gunny780_requests(request_key,user_id,operation,payload) values(p_key,p_user,p_operation,p_payload);
end;$$;
create or replace function public.gunny_finish_dungeon780(p_match uuid,p_winner integer,p_players jsonb,p_difficulty integer)
returns void language plpgsql security definer set search_path='' as $$
declare old public.match_results%rowtype;member jsonb;n integer;
begin
 if p_match is null or p_winner is null or p_winner not between 0 and 2 or p_difficulty is null or p_difficulty not in(0,1) or p_players is null or jsonb_typeof(p_players)<>'array' or jsonb_array_length(p_players) not between 1 and 3 then raise exception 'Invalid dungeon match';end if;
 if (select count(distinct value->>'id') from jsonb_array_elements(p_players))<>jsonb_array_length(p_players) then raise exception 'Duplicate participant';end if;
 perform pg_advisory_xact_lock(hashtextextended(p_match::text,780));
 select * into old from public.match_results where id=p_match;
 if found then
  if old.winner<>p_winner or old.players<>p_players or old.room_kind<>3 or old.dungeon_difficulty is distinct from p_difficulty then raise exception 'Match payload mismatch';end if;
  return;
 end if;
 -- Lock profiles in a consistent order, including opposite party payload ordering.
 perform 1 from public.profiles where id in(select (value->>'id')::uuid from jsonb_array_elements(p_players)) order by id for update;
 for member in select value from jsonb_array_elements(p_players) loop
  if (member->>'team')::integer is distinct from 0 then raise exception 'Invalid dungeon team';end if;
  perform 1 from public.profiles where id=(member->>'id')::uuid and experience>=5250 and (p_difficulty=0 or dungeon_normal_clear);
  if not found then raise exception 'Dungeon level or difficulty locked';end if;
 end loop;
 insert into public.match_results(id,winner,players,room_kind,dungeon_difficulty) values(p_match,p_winner,p_players,3,p_difficulty);
 for member in select value from jsonb_array_elements(p_players) loop
  update public.profiles set experience=least(2147483647::bigint,experience::bigint+case when p_winner=0 then 300 else 100 end),wins=wins+case when p_winner=0 then 1 else 0 end,dungeon_normal_clear=dungeon_normal_clear or (p_winner=0 and p_difficulty=0) where id=(member->>'id')::uuid;
 end loop;
end;$$;
create or replace function public.gunny_claim780(p_match uuid,p_user uuid,p_pick integer,p_card integer)
returns jsonb language plpgsql security definer set search_path='' as $$
declare result public.match_results%rowtype;prior public.gunny780_card_rewards%rowtype;eligible boolean;coin integer:=0;stone integer:=0;
begin
 if p_match is null or p_user is null or p_pick is null or p_pick not between 0 and 1 or p_card is null or p_card not between 0 and 17 then raise exception 'Invalid reward request';end if;
 select * into strict result from public.match_results where id=p_match for update;
 if result.room_kind not in(2,3) then raise exception 'Invalid reward mode';end if;
 select * into prior from public.gunny780_card_rewards where match_id=p_match and user_id=p_user and pick=p_pick;
 if found then
  if prior.card<>p_card then raise exception 'Reward payload mismatch';end if;
  return jsonb_build_object('card',prior.card,'pick',prior.pick,'coins',prior.coins,'stones',prior.stones);
 end if;
 eligible:=exists(select 1 from jsonb_array_elements(result.players) x where x->>'id'=p_user::text and result.winner<2 and ((x->>'team')::integer=result.winner or result.room_kind=2 and coalesce((x->>'damage')::integer,0)>0));
 if not eligible then raise exception 'Not eligible for card';end if;
 if p_pick=1 and not exists(select 1 from public.gunny780_card_rewards where match_id=p_match and user_id=p_user and pick=0) then raise exception 'Missing first pick';end if;
 if exists(select 1 from public.gunny780_card_rewards where match_id=p_match and card=p_card) then raise exception 'Card already taken';end if;
 if result.room_kind=3 then stone:=10+floor(random()*91)::integer;
 elsif random()<0.5 then stone:=1+floor(random()*10)::integer;
 else coin:=500+floor(random()*1501)::integer;end if;
 perform 1 from public.profiles where id=p_user for update;
 insert into public.gunny780_card_rewards(match_id,user_id,pick,card,coins,stones) values(p_match,p_user,p_pick,p_card,coin,stone);
 update public.profiles set coins=least(999999,coins+coin),stones=least(999999,stones+stone) where id=p_user;
 return jsonb_build_object('card',p_card,'pick',p_pick,'coins',coin,'stones',stone);
end;$$;
revoke all on function public.gunny_economy780(uuid,text,jsonb,text),public.gunny_finish_dungeon780(uuid,integer,jsonb,integer),public.gunny_claim780(uuid,uuid,integer,integer) from public,anon,authenticated;
grant execute on function public.gunny_economy780(uuid,text,jsonb,text),public.gunny_finish_dungeon780(uuid,integer,jsonb,integer),public.gunny_claim780(uuid,uuid,integer,integer) to service_role;
do $$begin
 if exists(select 1 from gunny780_preserve b full join public.profiles p on p.id=b.id where b.id is null or p.id is null or b.value is distinct from (to_jsonb(p)-'stones'-'forge_levels'-'dungeon_normal_clear')) then raise exception 'Migration changed existing profiles';end if;
end$$;
notify pgrst,'reload schema';
commit;
