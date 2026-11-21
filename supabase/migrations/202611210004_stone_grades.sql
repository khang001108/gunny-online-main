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
