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


-- 7.8.5: crafting, tribal dungeon, test-account permission and graded stones
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
create table if not exists public.gunny_stone_receipts(
 match_id uuid not null,user_id uuid not null,pick integer not null,grade integer not null check(grade between 1 and 5),
 primary key(match_id,user_id,pick),foreign key(match_id,user_id,pick) references public.gunny780_card_rewards(match_id,user_id,pick));
alter table public.gunny_stone_receipts enable row level security;
revoke all on public.gunny_stone_receipts from public,anon,authenticated;
create or replace function public.gunny_claim_dungeon_stones(p_match uuid,p_user uuid,p_pick integer,p_card integer,p_difficulty integer)
returns jsonb language plpgsql security definer set search_path='' as $$
declare result jsonb;grade_no integer;amount integer;old_stones integer;already_claimed boolean;
begin
 if p_difficulty not between 0 and 2 then raise exception 'Invalid difficulty';end if;
 -- Match first, then profile: same lock order as the original claim function.
 perform 1 from public.match_results where id=p_match for update;
 select stones into strict old_stones from public.profiles where id=p_user for update;
 already_claimed:=exists(select 1 from public.gunny780_card_rewards where match_id=p_match and user_id=p_user and pick=p_pick);
 result:=public.gunny_claim780(p_match,p_user,p_pick,p_card);
 select grade into grade_no from public.gunny_stone_receipts where match_id=p_match and user_id=p_user and pick=p_pick;
 if not found then
  -- Legacy claims keep their original Lv1 reward; never convert it a second time.
  grade_no:=case when already_claimed then 1 else public.gunny_stone_grade(p_difficulty,floor(random()*100)::integer) end;amount:=(result->>'stones')::integer;
  if grade_no>1 then update public.profiles set stones=old_stones,craft_stones[grade_no-1]=least(999999,craft_stones[grade_no-1]+amount) where id=p_user;end if;
  insert into public.gunny_stone_receipts values(p_match,p_user,p_pick,grade_no);
 end if;
 return result||jsonb_build_object('grade',grade_no);
end;$$;
revoke all on function public.gunny_claim_dungeon_stones(uuid,uuid,integer,integer,integer) from public,anon,authenticated;
grant execute on function public.gunny_claim_dungeon_stones(uuid,uuid,integer,integer,integer) to service_role;
notify pgrst,'reload schema';
commit;
