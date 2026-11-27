-- HF45 rewards: service-role-only RPC, no rewrite of historical receipts or inventories.
BEGIN;
alter table public.gunny_tribal_matches add column if not exists reward_config jsonb;
create or replace function public.gunny_reward_roll(cfg jsonb,k text) returns integer language plpgsql volatile set search_path='' as $$
declare lo integer:=(cfg->>(k||'Min'))::integer; hi integer:=(cfg->>(k||'Max'))::integer;
begin if lo is null or hi is null or lo<0 or hi<lo or hi>300 then raise exception 'Invalid reward range';end if; return lo+floor(random()*(hi-lo+1))::integer;end;$$;
create or replace function public.gunny_reward_grade(cfg jsonb,r integer) returns integer language plpgsql immutable set search_path='' as $$
declare total integer:=0;i integer;
begin for i in 1..5 loop total:=total+(cfg->>('grade'||i))::integer;if r<total then return i;end if;end loop;raise exception 'Invalid reward weights';end;$$;
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
 if result.room_kind in(3,4) then stone:=case when nullif(current_setting('gunny.reward_config',true),'') is null then (10+floor(random()*91)::integer)/2 else public.gunny_reward_roll(current_setting('gunny.reward_config',true)::jsonb,'stone') end;
 elsif random()<0.5 then stone:=1+floor(random()*10)::integer;
 else coin:=500+floor(random()*1501)::integer;end if;
 perform 1 from public.profiles where id=p_user for update;
 insert into public.gunny780_card_rewards(match_id,user_id,pick,card,coins,stones) values(p_match,p_user,p_pick,p_card,coin,stone);
 update public.profiles set coins=least(999999,coins+coin),stones=least(999999,stones+stone) where id=p_user;
 return jsonb_build_object('card',p_card,'pick',p_pick,'coins',coin,'stones',stone);
end;$$;
create or replace function public.gunny_dungeon_charms()
returns trigger language plpgsql security definer set search_path='' as $$
declare kind integer;cfg jsonb:=nullif(current_setting('gunny.reward_config',true),'')::jsonb;
begin
 select room_kind into kind from public.match_results where id=new.match_id;
 if kind in(3,4) then
  update public.profiles set lucky_charms=least(999999,lucky_charms+case when random()*100<coalesce((cfg->>'luckyChance')::integer,100) then coalesce((cfg->>'luckyBase')::integer,1) else 0 end),
   protection_charms=least(999999,protection_charms+case when cfg is not null then case when new.pick=0 and random()*100<coalesce((cfg->>'protectChance')::integer,100) then public.gunny_reward_roll(cfg,'protect') else 0 end when kind=3 then 1 else 0 end)
   where id=new.user_id;
 end if;
 return new;
end;$$;
create or replace function public.gunny_claim_dungeon_config(p_match uuid,p_user uuid,p_pick integer,p_card integer,p_difficulty integer,p_config jsonb)
returns jsonb language plpgsql security definer set search_path='' as $$
declare result jsonb;grade_no integer;amount integer;old_stones integer;already_claimed boolean;
begin
 if p_difficulty not between 0 and 3 then raise exception 'Invalid difficulty';end if;
 -- Match first, then profile: same lock order as the original claim function.
 perform 1 from public.match_results where id=p_match for update;
 select stones into strict old_stones from public.profiles where id=p_user for update;
 already_claimed:=exists(select 1 from public.gunny780_card_rewards where match_id=p_match and user_id=p_user and pick=p_pick);
 perform set_config('gunny.reward_config',p_config::text,true);
 result:=public.gunny_claim780(p_match,p_user,p_pick,p_card);
 select grade into grade_no from public.gunny_stone_receipts where match_id=p_match and user_id=p_user and pick=p_pick;
 if not found then
  -- Legacy claims keep their original Lv1 reward; never convert it a second time.
  grade_no:=case when already_claimed then 1 else public.gunny_reward_grade(p_config,floor(random()*100)::integer) end;amount:=(result->>'stones')::integer;
  if grade_no>1 then update public.profiles set stones=old_stones,craft_stones[grade_no-1]=least(999999,craft_stones[grade_no-1]+amount) where id=p_user;end if;
  insert into public.gunny_stone_receipts values(p_match,p_user,p_pick,grade_no);
 end if;
 perform set_config('gunny.reward_config','',true);
 return result||jsonb_build_object('grade',grade_no);
end;$$;
create or replace function public.gunny_tribal(p_action text,p_payload jsonb)
returns jsonb language plpgsql security definer set search_path='' as $$
declare
 m public.gunny_tribal_matches%rowtype;r public.gunny_tribal_rewards%rowtype;
 mid uuid;uid uuid;stage_no integer;diff integer;win integer;lo integer;hi integer;
 slot integer;pick_no integer;kind integer;amount integer;position integer;i integer;
 deck integer[]:=array_fill(0,array[18]);positions integer[];member jsonb;result jsonb;cfg jsonb;
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
  lo:=(p_payload->>'min')::integer;hi:=(p_payload->>'max')::integer;cfg:=p_payload->'config';
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
   for i in 1..coalesce((cfg->>'luckyCards')::integer,case when diff=2 then 1 else 0 end) loop
    deck[positions[i]]:=2;
   end loop;
  end if;
  insert into public.gunny_tribal_matches(id,run_id,stage,difficulty,winner,players,deck,stone_min,stone_max,created_at,reward_config) values(mid,(p_payload->>'run')::uuid,stage_no,diff,win,p_payload->'players',deck,lo,hi,now(),cfg);
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
 cfg:=m.reward_config;kind:=m.deck[slot+1];amount:=case when kind=2 then case when cfg is null then 5+floor(random()*16)::integer else public.gunny_reward_roll(cfg,'lucky') end else 1 end;
 if kind=0 then kind:=case when cfg is null then public.gunny_stone_grade(m.difficulty,floor(random()*100)::integer) else public.gunny_reward_grade(cfg,floor(random()*100)::integer) end;kind:=case when kind=1 then 1 else kind+2 end;amount:=m.stone_min+floor(random()*(m.stone_max-m.stone_min+1))::integer;end if;
 update public.profiles set stones=least(999999,stones+case when kind=1 then amount else 0 end),
  craft_stones=case when kind>=4 then array[least(999999,craft_stones[1]+case when kind=4 then amount else 0 end),least(999999,craft_stones[2]+case when kind=5 then amount else 0 end),least(999999,craft_stones[3]+case when kind=6 then amount else 0 end),least(999999,craft_stones[4]+case when kind=7 then amount else 0 end)] else craft_stones end,
  lucky_charms=least(999999,lucky_charms+case when kind=2 then amount else case when random()*100<coalesce((cfg->>'luckyChance')::integer,100) then coalesce((cfg->>'luckyBase')::integer,1) else 0 end end),
  protection_charms=least(999999,protection_charms+case when cfg is not null and pick_no=0 and random()*100<coalesce((cfg->>'protectChance')::integer,100) then public.gunny_reward_roll(cfg,'protect') else 0 end),owned=owned|case when kind=3 then 256 else 0 end,
  vip_spears=least(999999,vip_spears+case when kind=3 then 1 else 0 end) where id=uid;
 result:=jsonb_build_object('card',slot,'type',kind,'amount',amount,'pick',pick_no);
 insert into public.gunny_tribal_rewards values(mid,uid,pick_no,slot,result);
 return result;
end $$;
create or replace function public.gunny_gift_open_config(p_user uuid,p_type integer,p_key text,p_config jsonb)
returns jsonb language plpgsql security definer set search_path='' as $$
declare p public.profiles%rowtype;prior public.gunny_gift_receipts%rowtype;r jsonb;lv integer;stone integer;coin integer;card integer:=0;
begin
 if p_key is null or length(p_key) not between 1 and 120 or p_type is null or p_type not between 0 and 2 then raise exception 'Invalid gift request';end if;
 select * into strict p from public.profiles where id=p_user for update;
 select * into prior from public.gunny_gift_receipts where user_id=p_user and request_key=p_key;
 if found then if prior.item_type<>p_type then raise exception 'Gift payload mismatch';end if;return prior.result;end if;
 if p_type=0 then
  lv:=p.gift_level;if lv>70 or p.experience<25*(lv-1)*lv then raise exception 'Not enough level for chest %',lv;end if;
  stone:=50+floor(random()*51)::integer;coin:=100*lv;
  update public.profiles set gift_level=gift_level+1,stones=least(999999,stones+stone),coins=least(999999,coins+coin) where id=p_user;
  r:=jsonb_build_array(jsonb_build_array(1,coin),jsonb_build_array(2,stone));
  if lv<70 then r:=jsonb_build_array(jsonb_build_array(0,lv+1))||r;end if;
 elsif p_type=1 then
  if p.gift_bags<1 then raise exception 'No dungeon gift';end if;
  stone:=public.gunny_reward_roll(p_config,'stone');
  if random()<((p_config->>'vipChance')::integer/100.0) then card:=1+floor(random()*9)::integer;end if;
  if card>0 then
   if p.vip_cards[card]>=999999 then raise exception 'VIP card inventory full; gift not consumed';end if;
   p.vip_cards[card]:=p.vip_cards[card]+1;
  end if;
  update public.profiles set gift_bags=gift_bags-1,craft_stones[1]=least(999999,craft_stones[1]+stone),vip_cards=p.vip_cards where id=p_user;
  r:=jsonb_build_array(jsonb_build_array(3,stone));
  if card>0 then r:=r||jsonb_build_array(jsonb_build_array(9+card,1));end if;
 else
  -- Defensive conversion for residual legacy items, including imported profiles.
  if p.vip_tickets<1 then raise exception 'No legacy VIP ticket';end if;
  if p.vip_cards[1]>=999999 then raise exception 'VIP card inventory full; ticket not consumed';end if;
  update public.profiles set vip_tickets=vip_tickets-1,vip_cards[1]=vip_cards[1]+1 where id=p_user;
  r:=jsonb_build_array(jsonb_build_array(10,1));
 end if;
 r:=jsonb_build_object('rewards',r);insert into public.gunny_gift_receipts(user_id,request_key,item_type,result) values(p_user,p_key,p_type,r);return r;
end;$$;
revoke all on function public.gunny_reward_roll(jsonb,text) from public,anon,authenticated;
grant execute on function public.gunny_reward_roll(jsonb,text) to service_role;
revoke all on function public.gunny_reward_grade(jsonb,integer) from public,anon,authenticated;
grant execute on function public.gunny_reward_grade(jsonb,integer) to service_role;
revoke all on function public.gunny_claim_dungeon_config(uuid,uuid,integer,integer,integer,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_claim_dungeon_config(uuid,uuid,integer,integer,integer,jsonb) to service_role;
revoke all on function public.gunny_gift_open_config(uuid,integer,text,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_gift_open_config(uuid,integer,text,jsonb) to service_role;
revoke all on function public.gunny_tribal(text,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_tribal(text,jsonb) to service_role;
notify pgrst,'reload schema';
COMMIT;
