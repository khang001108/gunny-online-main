-- HF14: dungeon bags grant EXP cards, not time-based VIP. Run once (safe to rerun).
begin;
-- Prevent a concurrent legacy gift opening from racing with the conversion.
lock table public.profiles in share row exclusive mode;
-- Convert one unused legacy ticket / each remaining started 24h of VIP to one VIP 1 card.
-- Abort rather than truncate rewards if an account has no capacity.
do $$
begin
 if exists(select 1 from public.profiles p where p.vip_cards[1]::bigint + p.vip_tickets +
   ceil(greatest(0::bigint,p.vip_until-floor(extract(epoch from now()))::bigint)/86400.0)::bigint > 999999) then
  raise exception 'HF14: VIP 1 inventory capacity exceeded; no data was changed';
 end if;
end $$;
update public.profiles p
 set vip_cards[1] = (p.vip_cards[1]::bigint+p.vip_tickets+
   ceil(greatest(0::bigint,p.vip_until-floor(extract(epoch from now()))::bigint)/86400.0)::bigint)::integer,
     vip_tickets=0, vip_until=0
 where p.vip_tickets>0 or p.vip_until<>0;

-- New RPC name deliberately fails closed until this migration is installed.
-- Reward IDs 10..18 encode VIP card 1..9; amount is the number of cards.
create or replace function public.gunny_gift_open_exp(p_user uuid,p_type integer,p_key text)
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
  stone:=20+floor(random()*51)::integer;
  if random()<0.1 then card:=1+floor(random()*9)::integer;end if;
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
revoke all on function public.gunny_gift_open_exp(uuid,integer,text) from public,anon,authenticated;
grant execute on function public.gunny_gift_open_exp(uuid,integer,text) to service_role;
-- Block outdated backends from minting old tickets during a rolling deployment.
-- The level chest is unchanged and remains backward-compatible.
create or replace function public.gunny_gift_open(p_user uuid,p_type integer,p_key text)
returns jsonb language plpgsql security definer set search_path='' as $$
begin
 if p_type is distinct from 0 then raise exception 'Update server and client HF14 for VIP EXP gifts';end if;
 return public.gunny_gift_open_exp(p_user,p_type,p_key);
end;$$;
revoke all on function public.gunny_gift_open(uuid,integer,text) from public,anon,authenticated;
grant execute on function public.gunny_gift_open(uuid,integer,text) to service_role;
notify pgrst,'reload schema';
commit;
