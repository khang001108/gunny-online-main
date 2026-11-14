begin;

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
notify pgrst,'reload schema';
commit;
