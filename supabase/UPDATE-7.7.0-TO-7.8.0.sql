-- Gunny Java: nâng cấp Supabase từ 7.7.0 lên 7.8.0.
-- Yêu cầu migration tài khoản 7.7.0 đã chạy. Chạy toàn bộ file trong SQL Editor.
begin;

-- 202611130001_forge_dungeon.sql
-- Gunny 7.8.0: additive economy and dungeon progression. Run MANUALLY after review.
-- No account reset/deletion, no change to authentication, retention or existing rewards.

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

-- 202611130002_forge15_progress.sql
alter table public.profiles
  add column if not exists forge_progress integer[] not null default array_fill(0,array[130]);

update public.profiles
set forge_levels=(select array_agg(least(v,15)::smallint order by n)
                  from unnest(forge_levels) with ordinality as x(v,n));

alter table public.profiles drop constraint if exists gunny780_forge_levels;
alter table public.profiles drop constraint if exists gunny781_forge_levels;
alter table public.profiles drop constraint if exists gunny781_forge_progress;
alter table public.profiles add constraint gunny781_forge_levels
 check(array_ndims(forge_levels)=1 and array_lower(forge_levels,1)=1 and cardinality(forge_levels)=130
       and 0<=all(forge_levels) and 15>=all(forge_levels) and array_position(forge_levels,null) is null);
alter table public.profiles add constraint gunny781_forge_progress
 check(array_ndims(forge_progress)=1 and array_lower(forge_progress,1)=1 and cardinality(forge_progress)=130
       and 0<=all(forge_progress) and 3750>all(forge_progress) and array_position(forge_progress,null) is null);

create or replace function public.gunny_economy780(p_user uuid,p_operation text,p_payload jsonb,p_key text)
returns void language plpgsql security definer set search_path='' as $$
declare receipt public.gunny780_requests%rowtype;p public.profiles%rowtype;qty integer;sex integer;kind integer;item integer;rank integer;progress integer;idx integer;cost integer;spend integer;
begin
 if p_user is null or p_key is null or p_key !~ '^[A-Za-z0-9_-]{1,64}$' or p_payload is null or jsonb_typeof(p_payload)<>'object' or p_operation not in('buy','forge') then raise exception 'Invalid economy request';end if;
 perform pg_advisory_xact_lock(hashtextextended(p_key,780));
 select * into receipt from public.gunny780_requests where request_key=p_key;
 if found then
  if receipt.user_id<>p_user or receipt.operation<>p_operation or receipt.payload<>p_payload then raise exception 'Request key payload mismatch';end if;
  return;
 end if;
 select * into strict p from public.profiles where id=p_user for update;
 if p_operation='buy' then
  qty:=(p_payload->>'quantity')::integer;
  if qty is null or qty not between 1 and 1000 or p_payload<>jsonb_build_object('quantity',qty) then raise exception 'Invalid quantity';end if;
  cost:=qty*80;
  if p.shopping_money<cost or p.stones>999999-qty then raise exception 'Insufficient shopping money or stone limit';end if;
  update public.profiles set shopping_money=shopping_money-cost,stones=stones+qty where id=p_user;
 else
  sex:=(p_payload->>'sex')::integer;kind:=(p_payload->>'kind')::integer;item:=(p_payload->>'item')::integer;rank:=(p_payload->>'rank')::integer;progress:=(p_payload->>'progress')::integer;
  if sex is null or kind is null or item is null or rank is null or progress is null or sex<>p.gender or kind not in(0,2,3,6) or item not between 0 and 9 or rank not between 0 and 14 or progress not between 0 and 3749 or p_payload<>jsonb_build_object('sex',sex,'kind',kind,'item',item,'rank',rank,'progress',progress) then raise exception 'Invalid forge request';end if;
  if kind=0 then
   if item not in(0,1,2,3,4,5,7,9) or (p.owned&(1<<item))=0 then raise exception 'Item not owned';end if;idx:=item+1;
  else
   if (p.wardrobe[sex*6+kind]&(1<<item))=0 then raise exception 'Item not owned';end if;idx:=11+sex*60+(kind-1)*10+item;
  end if;
  if p.forge_levels[idx]<>rank or p.forge_progress[idx]<>progress then raise exception 'Forge level changed';end if;
  if p.stones<=0 then raise exception 'Insufficient stones';end if;
  cost:=250*(rank+1);spend:=least(p.stones,cost-progress);
  if progress+spend>=cost then
   update public.profiles set stones=stones-spend,forge_levels[idx]=(rank+1)::smallint,forge_progress[idx]=0 where id=p_user;
  else
   update public.profiles set stones=stones-spend,forge_progress[idx]=progress+spend where id=p_user;
  end if;
 end if;
 insert into public.gunny780_requests(request_key,user_id,operation,payload) values(p_key,p_user,p_operation,p_payload);
end;$$;

revoke all on function public.gunny_economy780(uuid,text,jsonb,text) from public,anon,authenticated;
grant execute on function public.gunny_economy780(uuid,text,jsonb,text) to service_role;

-- 202611140001_dungeon_difficulties.sql
create or replace function public.gunny_finish_dungeon780(p_match uuid,p_winner integer,p_players jsonb,p_difficulty integer)
returns void language plpgsql security definer set search_path='' as $$
declare old public.match_results%rowtype;member jsonb;
begin
 if p_match is null or p_winner is null or p_winner not between 0 and 2 or p_difficulty is null or p_difficulty not in(0,1,2) or p_players is null or jsonb_typeof(p_players)<>'array' or jsonb_array_length(p_players) not between 1 and 3 then raise exception 'Invalid dungeon match';end if;
 if (select count(distinct value->>'id') from jsonb_array_elements(p_players))<>jsonb_array_length(p_players) then raise exception 'Duplicate participant';end if;
 perform pg_advisory_xact_lock(hashtextextended(p_match::text,780));
 select * into old from public.match_results where id=p_match;
 if found then
  if old.winner<>p_winner or old.players<>p_players or old.room_kind<>3 or old.dungeon_difficulty is distinct from p_difficulty then raise exception 'Match payload mismatch';end if;
  return;
 end if;
 perform 1 from public.profiles where id in(select (value->>'id')::uuid from jsonb_array_elements(p_players)) order by id for update;
 for member in select value from jsonb_array_elements(p_players) loop
  if (member->>'team')::integer is distinct from 0 then raise exception 'Invalid dungeon team';end if;
  perform 1 from public.profiles where id=(member->>'id')::uuid and experience>=5250 and (p_difficulty<>2 or dungeon_normal_clear);
  if not found then raise exception 'Dungeon level or difficulty locked';end if;
 end loop;
 insert into public.match_results(id,winner,players,room_kind,dungeon_difficulty) values(p_match,p_winner,p_players,3,p_difficulty);
 for member in select value from jsonb_array_elements(p_players) loop
  update public.profiles set experience=least(2147483647::bigint,experience::bigint+case when p_winner=0 then 300 else 100 end),wins=wins+case when p_winner=0 then 1 else 0 end,dungeon_normal_clear=dungeon_normal_clear or (p_winner=0 and p_difficulty=1) where id=(member->>'id')::uuid;
 end loop;
end;$$;

revoke all on function public.gunny_finish_dungeon780(uuid,integer,jsonb,integer) from public,anon,authenticated;
grant execute on function public.gunny_finish_dungeon780(uuid,integer,jsonb,integer) to service_role;

-- 202611140002_ant_cave_stages.sql
alter table public.match_results drop constraint if exists gunny780_room_kind;
alter table public.match_results add constraint gunny780_room_kind check(room_kind between 0 and 4);

create or replace function public.gunny_finish_ant_stage(p_match uuid,p_winner integer,p_players jsonb,p_difficulty integer)
returns void language plpgsql security definer set search_path='' as $$
declare old public.match_results%rowtype;member jsonb;
begin
 if p_match is null or p_winner is null or p_winner not between 0 and 2 or p_difficulty is null or p_difficulty not in(0,1,2) or p_players is null or jsonb_typeof(p_players)<>'array' or jsonb_array_length(p_players) not between 1 and 3 then raise exception 'Invalid dungeon match';end if;
 if (select count(distinct value->>'id') from jsonb_array_elements(p_players))<>jsonb_array_length(p_players) then raise exception 'Duplicate participant';end if;
 perform pg_advisory_xact_lock(hashtextextended(p_match::text,780));
 select * into old from public.match_results where id=p_match;
 if found then
  if old.winner<>p_winner or old.players<>p_players or old.room_kind<>4 or old.dungeon_difficulty is distinct from p_difficulty then raise exception 'Match payload mismatch';end if;
  return;
 end if;
 perform 1 from public.profiles where id in(select (value->>'id')::uuid from jsonb_array_elements(p_players)) order by id for update;
 for member in select value from jsonb_array_elements(p_players) loop
  if (member->>'team')::integer is distinct from 0 then raise exception 'Invalid dungeon team';end if;
  perform 1 from public.profiles where id=(member->>'id')::uuid and experience>=5250 and (p_difficulty<>2 or dungeon_normal_clear);
  if not found then raise exception 'Dungeon level or difficulty locked';end if;
 end loop;
 insert into public.match_results(id,winner,players,room_kind,dungeon_difficulty) values(p_match,p_winner,p_players,4,p_difficulty);
 for member in select value from jsonb_array_elements(p_players) loop
  update public.profiles set experience=least(2147483647::bigint,experience::bigint+case when p_winner=0 then 300 else 100 end),wins=wins+case when p_winner=0 then 1 else 0 end where id=(member->>'id')::uuid;
 end loop;
end;$$;

create or replace function public.gunny_claim780(p_match uuid,p_user uuid,p_pick integer,p_card integer)
returns jsonb language plpgsql security definer set search_path='' as $$
declare result public.match_results%rowtype;prior public.gunny780_card_rewards%rowtype;eligible boolean;coin integer:=0;stone integer:=0;
begin
 if p_match is null or p_user is null or p_pick is null or p_pick not between 0 and 1 or p_card is null or p_card not between 0 and 17 then raise exception 'Invalid reward request';end if;
 select * into strict result from public.match_results where id=p_match for update;
 if result.room_kind not in(2,3,4) or (result.room_kind=4 and (p_pick<>0 or p_card>8)) then raise exception 'Invalid reward mode';end if;
 select * into prior from public.gunny780_card_rewards where match_id=p_match and user_id=p_user and pick=p_pick;
 if found then
  if prior.card<>p_card then raise exception 'Reward payload mismatch';end if;
  return jsonb_build_object('card',prior.card,'pick',prior.pick,'coins',prior.coins,'stones',prior.stones);
 end if;
 eligible:=exists(select 1 from jsonb_array_elements(result.players) x where x->>'id'=p_user::text and result.winner<2 and ((x->>'team')::integer=result.winner or result.room_kind=2 and coalesce((x->>'damage')::integer,0)>0));
 if not eligible then raise exception 'Not eligible for card';end if;
 if p_pick=1 and not exists(select 1 from public.gunny780_card_rewards where match_id=p_match and user_id=p_user and pick=0) then raise exception 'Missing first pick';end if;
 if exists(select 1 from public.gunny780_card_rewards where match_id=p_match and card=p_card) then raise exception 'Card already taken';end if;
 if result.room_kind in(3,4) then stone:=10+floor(random()*91)::integer;
 elsif random()<0.5 then stone:=1+floor(random()*10)::integer;
 else coin:=500+floor(random()*1501)::integer;end if;
 perform 1 from public.profiles where id=p_user for update;
 insert into public.gunny780_card_rewards(match_id,user_id,pick,card,coins,stones) values(p_match,p_user,p_pick,p_card,coin,stone);
 update public.profiles set coins=least(999999,coins+coin),stones=least(999999,stones+stone) where id=p_user;
 return jsonb_build_object('card',p_card,'pick',p_pick,'coins',coin,'stones',stone);
end;$$;
revoke all on function public.gunny_finish_ant_stage(uuid,integer,jsonb,integer) from public,anon,authenticated;
grant execute on function public.gunny_finish_ant_stage(uuid,integer,jsonb,integer) to service_role;
revoke all on function public.gunny_claim780(uuid,uuid,integer,integer) from public,anon,authenticated;
grant execute on function public.gunny_claim780(uuid,uuid,integer,integer) to service_role;

notify pgrst,'reload schema';
commit;

-- Chat công khai và tin nhắn bạn bè
begin;

create table if not exists public.gunny_chat (
    id bigint generated always as identity primary key,
    sender uuid not null references public.profiles(id) on delete cascade,
    receiver uuid references public.profiles(id) on delete cascade,
    sender_name text not null,
    message text not null check (char_length(message) <= 160),
    emoji integer not null check (emoji between -1 and 5),
    sent_at timestamptz not null default now(),
    read_at timestamptz,
    check (sender <> receiver),
    check (char_length(message) > 0 or emoji >= 0)
);
create index if not exists gunny_chat_public_idx on public.gunny_chat(id desc) where receiver is null;
create index if not exists gunny_chat_thread_idx on public.gunny_chat(sender, receiver, id desc) where receiver is not null;
create index if not exists gunny_chat_unread_idx on public.gunny_chat(receiver, sender, id) where read_at is null and receiver is not null;
create index if not exists gunny_chat_expiry_idx on public.gunny_chat(sent_at);
alter table public.gunny_chat enable row level security;
revoke all on public.gunny_chat from public, anon, authenticated;

create or replace function public.gunny_chat_api(
    p_user uuid, p_action text, p_peer integer, p_cursor bigint, p_message text, p_emoji integer
) returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    current_time_utc timestamptz := now();
    private_cutoff timestamptz := current_time_utc - interval '720 hours';
    public_cutoff timestamptz := date_trunc('day', current_time_utc at time zone 'Asia/Ho_Chi_Minh') at time zone 'Asia/Ho_Chi_Minh';
    peer_id uuid;
    display text;
    preview jsonb;
    history jsonb := '[]'::jsonb;
begin
    if p_action = 'purge' then
        delete from public.gunny_chat where sent_at < private_cutoff or (receiver is null and sent_at < public_cutoff);
        return '{}'::jsonb;
    end if;

    if p_peer > 0 then
        select id into peer_id from public.profiles where public_id = p_peer;
        if peer_id is null or peer_id = p_user or not exists (
            select 1 from public.gunny_friends f where f.accepted and
              ((f.sender = p_user and f.receiver = peer_id) or (f.receiver = p_user and f.sender = peer_id))
        ) then
            raise exception 'Chat requires an accepted friendship' using errcode = '42501';
        end if;
    end if;

    if p_action = 'send' then
        select display_name into strict display from public.profiles where id = p_user;
        if peer_id is not null then
            -- Allocate message IDs in commit order within this conversation.
            perform pg_advisory_xact_lock(hashtextextended(
                least(p_user, peer_id)::text || greatest(p_user, peer_id)::text, 0));
        end if;
        insert into public.gunny_chat(sender, receiver, sender_name, message, emoji)
        values (p_user, peer_id, display, p_message, p_emoji);
        return '{}'::jsonb;
    elsif p_action = 'read' then
        update public.gunny_chat set read_at = current_time_utc
        where sender = peer_id and receiver = p_user and id <= p_cursor and read_at is null and sent_at >= private_cutoff;
        return '{}'::jsonb;
    elsif p_action = 'friends' then
        return coalesce((
            select jsonb_agg(to_jsonb(contact) order by contact.unread desc, contact.name, contact.public_id)
            from (
                select p.public_id, p.display_name as name, exists (
                    select 1 from public.gunny_chat m where m.sender = p.id and m.receiver = p_user
                        and m.read_at is null and m.sent_at >= private_cutoff
                ) as unread
                from public.gunny_friends f
                join public.profiles p on p.id = case when f.sender = p_user then f.receiver else f.sender end
                where f.accepted and (f.sender = p_user or f.receiver = p_user)
            ) contact
        ), '[]'::jsonb);
    end if;

    select coalesce(jsonb_agg(to_jsonb(m) order by m.id desc), '[]'::jsonb) into preview from (
        select id, sender_name, message, emoji from public.gunny_chat
        where receiver is null and sent_at >= public_cutoff order by id desc limit 5
    ) m;

    if p_peer = 0 then
        select coalesce(jsonb_agg(to_jsonb(m) order by m.id desc), '[]'::jsonb) into history from (
            select id, sender_name, message, emoji from public.gunny_chat
            where receiver is null and sent_at >= public_cutoff and (p_cursor = 0 or id < p_cursor)
            order by id desc limit 17
        ) m;
    elsif p_peer > 0 then
        select coalesce(jsonb_agg(to_jsonb(m) order by m.id desc), '[]'::jsonb) into history from (
            select id, sender_name, message, emoji from public.gunny_chat
            where sent_at >= private_cutoff and (p_cursor = 0 or id < p_cursor)
                and ((sender = p_user and receiver = peer_id) or (receiver = p_user and sender = peer_id))
            order by id desc limit 17
        ) m;
    end if;

    return jsonb_build_object('preview', preview, 'history', history, 'unread', exists (
        select 1 from public.gunny_chat where receiver = p_user and sent_at >= private_cutoff and read_at is null
    ));
end;
$$;
revoke all on function public.gunny_chat_api(uuid,text,integer,bigint,text,integer) from public, anon, authenticated;
grant execute on function public.gunny_chat_api(uuid,text,integer,bigint,text,integer) to service_role;
notify pgrst,'reload schema';
commit;

begin;
alter table public.gunny_chat drop constraint if exists gunny_chat_emoji_check;
alter table public.gunny_chat add constraint gunny_chat_emoji_check check (emoji between -1 and 66);
commit;
