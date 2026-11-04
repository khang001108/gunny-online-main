-- Additive upgrade: run after 202611030001_gunny.sql (safe to run again).
begin;
create or replace function public.gunny_finish_match(p_match uuid,p_winner integer,p_players jsonb) returns void language plpgsql security definer set search_path='' as $$
declare n integer; player jsonb;
begin
 if p_winner not between 0 and 2 or jsonb_typeof(p_players)<>'array' or jsonb_array_length(p_players) not between 1 and 6 then raise exception 'Invalid match';end if;
 insert into public.match_results(id,winner,players) values(p_match,p_winner,p_players) on conflict do nothing;
 get diagnostics n=row_count;if n=0 then return;end if;
 if (select count(distinct x->>'id') from jsonb_array_elements(p_players) x)<>jsonb_array_length(p_players) then raise exception 'Duplicate participant';end if;
 for player in select value from jsonb_array_elements(p_players) loop
  if (player->>'team')::integer not in(0,1) then raise exception 'Invalid team';end if;
  update public.profiles set coins=least(999999,coins+case when (player->>'team')::integer=p_winner then 300 else 80 end),wins=wins+case when (player->>'team')::integer=p_winner then 1 else 0 end where id=(player->>'id')::uuid;
 end loop;
end; $$;
revoke all on function public.gunny_finish_match(uuid,integer,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_finish_match(uuid,integer,jsonb) to service_role;
create table if not exists public.card_rewards (
 match_id uuid not null references public.match_results(id),
 user_id uuid not null references public.profiles(id) on delete cascade,
 card smallint not null check(card between 0 and 8),
 coins integer not null check(coins between 50 and 200),
 claimed_at timestamptz not null default now(),
 primary key(match_id,user_id)
);
alter table public.card_rewards enable row level security;
revoke all on public.card_rewards from public,anon,authenticated;
grant all on public.card_rewards to service_role;
create or replace function public.gunny_claim_card(p_match uuid,p_user uuid,p_card integer,p_coins integer)
returns jsonb language plpgsql security definer set search_path='' as $$
declare r public.card_rewards; n integer;
begin
 if p_card not between 0 and 8 or p_coins not between 50 and 200 then raise exception 'Invalid reward';end if;
 if not exists(select 1 from public.match_results where id=p_match and players @> jsonb_build_array(jsonb_build_object('id',p_user::text))) then raise exception 'Not a participant';end if;
 -- Serialize claims for this wallet and return the first committed choice on retries.
 perform 1 from public.profiles where id=p_user for update;
 select * into r from public.card_rewards where match_id=p_match and user_id=p_user;
 if found then return jsonb_build_object('card',r.card,'coins',r.coins);end if;
 insert into public.card_rewards(match_id,user_id,card,coins) values(p_match,p_user,p_card,p_coins);
 update public.profiles set coins=least(999999,coins+p_coins) where id=p_user;
 return jsonb_build_object('card',p_card,'coins',p_coins);
end;$$;
revoke all on function public.gunny_claim_card(uuid,uuid,integer,integer) from public,anon,authenticated;
grant execute on function public.gunny_claim_card(uuid,uuid,integer,integer) to service_role;
commit;
