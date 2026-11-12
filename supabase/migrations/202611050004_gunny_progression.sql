-- Run after equipment-718.sql. Existing balances and inventory are preserved.
begin;
alter table public.profiles alter column coins set default 0;
alter table public.match_results add column if not exists room_kind integer not null default 0 check(room_kind between 0 and 2);
alter table public.card_rewards drop constraint if exists card_rewards_coins_check;
alter table public.card_rewards add constraint card_rewards_coins_check check(coins between 50 and 2000);
create or replace function public.gunny_finish_match(p_match uuid,p_winner integer,p_players jsonb,p_kind integer)
returns void language plpgsql security definer set search_path='' as $$
declare n integer; player jsonb;
begin
 if p_kind not between 0 and 2 or p_winner not between 0 and 2 or jsonb_typeof(p_players)<>'array' or jsonb_array_length(p_players) not between 1 and 6 then raise exception 'Invalid match';end if;
 if (select count(distinct x->>'id') from jsonb_array_elements(p_players) x)<>jsonb_array_length(p_players) then raise exception 'Duplicate participant';end if;
 insert into public.match_results(id,winner,players,room_kind) values(p_match,p_winner,p_players,p_kind) on conflict do nothing;
 get diagnostics n=row_count;if n=0 then return;end if;
 for player in select value from jsonb_array_elements(p_players) loop
  if (player->>'team')::integer not in(0,1) then raise exception 'Invalid team';end if;
  if (player->>'team')::integer=p_winner then
   update public.profiles set coins=least(999999,coins+case when p_kind=2 then 1000 else 500 end),wins=wins+1 where id=(player->>'id')::uuid;
  end if;
 end loop;
end;$$;
create or replace function public.gunny_finish_match(p_match uuid,p_winner integer,p_players jsonb)
returns void language plpgsql security definer set search_path='' as $$
begin perform public.gunny_finish_match(p_match,p_winner,p_players,0);end;$$;
revoke all on function public.gunny_finish_match(uuid,integer,jsonb,integer) from public,anon,authenticated;
grant execute on function public.gunny_finish_match(uuid,integer,jsonb,integer) to service_role;
create or replace function public.gunny_claim_card(p_match uuid,p_user uuid,p_card integer,p_coins integer)
returns jsonb language plpgsql security definer set search_path='' as $$
declare old_card integer; old_coins integer; kind integer; won boolean;
begin
 if p_card not between 0 and 8 then raise exception 'Invalid card';end if;
 select room_kind,exists(select 1 from jsonb_array_elements(players) x where x->>'id'=p_user::text and (x->>'team')::integer=winner)
 into kind,won from public.match_results where id=p_match for update;
 if not found then raise exception 'Unknown match';end if;
 perform 1 from public.profiles where id=p_user for update;
 select card,coins into old_card,old_coins from public.card_rewards where match_id=p_match and user_id=p_user;
 if found then return jsonb_build_object('card',old_card,'coins',old_coins);end if;
 if not won then raise exception 'Only winners may claim';end if;
 if p_coins not between (case when kind=2 then 500 else 200 end) and (case when kind=2 then 2000 else 1000 end) then raise exception 'Invalid reward';end if;
 if exists(select 1 from public.card_rewards where match_id=p_match and card=p_card) then raise exception 'Card already taken';end if;
 insert into public.card_rewards(match_id,user_id,card,coins) values(p_match,p_user,p_card,p_coins);
 update public.profiles set coins=least(999999,coins+p_coins) where id=p_user;
 return jsonb_build_object('card',p_card,'coins',p_coins);
end;$$;
revoke all on function public.gunny_claim_card(uuid,uuid,integer,integer) from public,anon,authenticated;
grant execute on function public.gunny_claim_card(uuid,uuid,integer,integer) to service_role;
commit;
