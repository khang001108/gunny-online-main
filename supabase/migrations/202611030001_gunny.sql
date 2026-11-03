-- Run once in the SQL editor of a NEW Supabase project.
begin;
create table if not exists public.profiles (
 id uuid primary key references auth.users(id) on delete cascade,
 display_name text not null default 'Gunner' check(char_length(display_name) between 1 and 13),
 gender smallint not null default 0 check(gender between 0 and 1),
 skin smallint not null default 0 check(skin between 0 and 9),
 custom_look boolean not null default false,
 look integer[] not null default '{0,0,0,0,0}' check(array_length(look,1)=5 and 0<=all(look) and 9>=all(look)),
 coins integer not null default 1000 check(coins between 0 and 999999),
 wins integer not null default 0 check(wins>=0),
 owned integer not null default 1 check(owned>=1),
 equipped integer not null default 0 check(equipped in (0,3,5,9)),
 created_at timestamptz not null default now()
);
alter table public.profiles enable row level security;
revoke all on public.profiles from anon, authenticated;
grant select on public.profiles to authenticated;
grant all on public.profiles to service_role;
drop policy if exists own_profile_read on public.profiles;
create policy own_profile_read on public.profiles for select to authenticated using(auth.uid()=id);
create or replace function public.gunny_new_user() returns trigger language plpgsql security definer set search_path='' as $$
begin insert into public.profiles(id,display_name) values(new.id,left(coalesce(nullif(trim(new.raw_user_meta_data->>'display_name'),''),'Gunner'),13)) on conflict do nothing;return new;end; $$;
drop trigger if exists gunny_auth_created on auth.users;
create trigger gunny_auth_created after insert on auth.users for each row execute function public.gunny_new_user();
insert into public.profiles(id) select id from auth.users on conflict do nothing;
create table if not exists public.match_results(id uuid primary key,winner smallint not null check(winner between 0 and 2),players jsonb not null,finished_at timestamptz not null default now());
alter table public.match_results enable row level security;
revoke all on public.match_results from anon,authenticated;
grant all on public.match_results to service_role;
create or replace function public.gunny_buy_weapon(p_user uuid,p_weapon integer) returns void language plpgsql security definer set search_path='' as $$
declare v_coins integer; v_owned integer; cost integer;
begin
 if p_weapon not in(0,3,5,9) then raise exception 'Invalid weapon';end if;
 select coins,owned into strict v_coins,v_owned from public.profiles where id=p_user for update;
 cost:=case p_weapon when 0 then 0 when 3 then 450 when 5 then 800 else 1200 end;
 if (v_owned & (1<<p_weapon))=0 then if v_coins<cost then raise exception 'Insufficient coins';end if;v_coins:=v_coins-cost;v_owned:=v_owned | (1<<p_weapon);end if;
 update public.profiles set coins=v_coins,owned=v_owned,equipped=p_weapon where id=p_user;
end; $$;
create or replace function public.gunny_finish_match(p_match uuid,p_winner integer,p_players jsonb) returns void language plpgsql security definer set search_path='' as $$
declare n integer; player jsonb;
begin
 if p_winner not between 0 and 2 or jsonb_typeof(p_players)<>'array' or jsonb_array_length(p_players) not between 1 and 4 then raise exception 'Invalid match';end if;
 insert into public.match_results(id,winner,players) values(p_match,p_winner,p_players) on conflict do nothing;
 get diagnostics n=row_count;if n=0 then return;end if;
 if (select count(distinct x->>'id') from jsonb_array_elements(p_players) x)<>jsonb_array_length(p_players) then raise exception 'Duplicate participant';end if;
 for player in select value from jsonb_array_elements(p_players) loop
  if (player->>'team')::integer not in(0,1) then raise exception 'Invalid team';end if;
  update public.profiles set coins=least(999999,coins+case when (player->>'team')::integer=p_winner then 300 else 80 end),wins=wins+case when (player->>'team')::integer=p_winner then 1 else 0 end where id=(player->>'id')::uuid;
 end loop;
end; $$;
revoke all on function public.gunny_new_user() from public,anon,authenticated;
revoke all on function public.gunny_buy_weapon(uuid,integer) from public,anon,authenticated;
revoke all on function public.gunny_finish_match(uuid,integer,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_buy_weapon(uuid,integer) to service_role;
grant execute on function public.gunny_finish_match(uuid,integer,jsonb) to service_role;
commit;
