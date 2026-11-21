begin;
alter table public.profiles
 add column if not exists gift_level integer not null default 1 check(gift_level between 1 and 71),
 add column if not exists gift_bags integer not null default 0 check(gift_bags between 0 and 999999),
 add column if not exists vip_tickets integer not null default 0 check(vip_tickets between 0 and 999999),
 add column if not exists vip_until bigint not null default 0;
create table if not exists public.gunny_gift_receipts(
 user_id uuid not null references public.profiles(id) on delete cascade,
 request_key text not null,item_type integer not null,result jsonb not null,
 created_at timestamptz not null default now(),primary key(user_id,request_key));
alter table public.gunny_gift_receipts enable row level security;
revoke all on public.gunny_gift_receipts from public,anon,authenticated;
create or replace function public.gunny_gift_open(p_user uuid,p_type integer,p_key text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare p public.profiles%rowtype;prior public.gunny_gift_receipts%rowtype;r jsonb;lv integer;stone integer;coin integer;vip boolean;
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
  stone:=20+floor(random()*51)::integer;vip:=random()<0.1;
  update public.profiles set gift_bags=gift_bags-1,craft_stones[1]=least(999999,craft_stones[1]+stone),vip_tickets=least(999999,vip_tickets+case when vip then 1 else 0 end) where id=p_user;
  r:=jsonb_build_array(jsonb_build_array(3,stone));if vip then r:=r||jsonb_build_array(jsonb_build_array(4,1));end if;
 else
  if p.vip_tickets<1 then raise exception 'No VIP ticket';end if;
  update public.profiles set vip_tickets=vip_tickets-1,vip_until=greatest(vip_until,floor(extract(epoch from now()))::bigint)+86400 where id=p_user;
  r:=jsonb_build_array(jsonb_build_array(5,1));
 end if;
 r:=jsonb_build_object('rewards',r);insert into public.gunny_gift_receipts(user_id,request_key,item_type,result) values(p_user,p_key,p_type,r);return r;
end;$$;
revoke all on function public.gunny_gift_open(uuid,integer,text) from public,anon,authenticated;
grant execute on function public.gunny_gift_open(uuid,integer,text) to service_role;
-- Final-stage inserts occur once inside the existing settlement transaction.
create or replace function public.gunny_award_final_gift()
returns trigger language plpgsql security definer set search_path='' as $$
declare member jsonb;
begin
 if new.winner<>0 then return new;end if;
 if tg_table_name='match_results' then if new.room_kind<>3 then return new;end if;
 else if new.stage<>4 then return new;end if;end if;
 for member in select jsonb_build_object('id',id,'team',0) from (select distinct value->>'id' as id from jsonb_array_elements(new.players) where (value->>'team')::integer=0) players loop
  if (member->>'team')::integer=0 then update public.profiles set gift_bags=least(999999,gift_bags+1) where id=(member->>'id')::uuid;end if;
 end loop;return new;
end;$$;
drop trigger if exists gunny_final_gift on public.match_results;
create trigger gunny_final_gift after insert on public.match_results for each row execute function public.gunny_award_final_gift();
drop trigger if exists gunny_tribal_final_gift on public.gunny_tribal_matches;
create trigger gunny_tribal_final_gift after insert on public.gunny_tribal_matches for each row execute function public.gunny_award_final_gift();
notify pgrst,'reload schema';
commit;
