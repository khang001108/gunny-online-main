begin;
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
