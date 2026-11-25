-- HF19: restore the VIP EXP RPC contract and award weapon 8 on new final victories; existing owners get one extra gift bag.
-- No historical matches, cards, VIP balances or retention policies are rewritten.
begin;
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

-- Internal tribal stage 4 is the visible Tà Thần stage 3 (room stage 6).
create or replace function public.gunny_hf19_spear_deck()
returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.stage=4 then new.deck:=pg_catalog.array_replace(new.deck,3,0); end if;
 return new;
end;$$;
revoke all on function public.gunny_hf19_spear_deck() from public,anon,authenticated;
drop trigger if exists gunny_hf19_spear_deck on public.gunny_tribal_matches;
create trigger gunny_hf19_spear_deck before insert on public.gunny_tribal_matches
for each row execute function public.gunny_hf19_spear_deck();

create or replace function public.gunny_hf19_spear_award()
returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.stage=4 and new.winner=0 then
  -- SET expressions read the pre-update row: only existing owners get the extra bag.
  -- The original gunny_tribal_final_gift trigger still grants the normal bag.
  update public.profiles p
   set gift_bags=least(999999,p.gift_bags+case when (p.owned & 256)<>0 then 1 else 0 end),
       owned=p.owned | 256,
       vip_spears=greatest(p.vip_spears,1)
   where p.id in (select (x->>'id')::uuid from pg_catalog.jsonb_array_elements(new.players) x
    where (x->>'team')::integer=0);
 end if;
 return new;
end;$$;
revoke all on function public.gunny_hf19_spear_award() from public,anon,authenticated;
drop trigger if exists gunny_hf19_spear_award on public.gunny_tribal_matches;
create trigger gunny_hf19_spear_award after insert on public.gunny_tribal_matches
for each row execute function public.gunny_hf19_spear_award();
notify pgrst,'reload schema';
commit;
