-- Run AFTER shop 7.1.6 migration. Add four native weapons; preserve all balances and inventory.
begin;
alter table public.profiles drop constraint if exists profiles_equipped_check;
alter table public.profiles add constraint profiles_equipped_check check(equipped in(0,1,2,3,4,5,7,9));
create or replace function public.gunny_buy_weapon(p_user uuid,p_weapon integer) returns void language plpgsql security definer set search_path='' as $$
declare v_coins integer;v_owned integer;cost integer;
begin
 if p_weapon is null or p_weapon not in(0,1,2,3,4,5,7,9) then raise exception 'Invalid weapon';end if;
 select coins,owned into strict v_coins,v_owned from public.profiles where id=p_user for update;
 cost:=(array[0,650,950,450,550,800,1100,700,600,1200])[p_weapon+1];
 if (v_owned & (1<<p_weapon))=0 then
  if v_coins<cost then raise exception 'Insufficient coins';end if;
  v_coins:=v_coins-cost;v_owned:=v_owned | (1<<p_weapon);
 end if;
 update public.profiles set coins=v_coins,owned=v_owned,equipped=p_weapon where id=p_user;
end;$$;
revoke all on function public.gunny_buy_weapon(uuid,integer) from public,anon,authenticated;
grant execute on function public.gunny_buy_weapon(uuid,integer) to service_role;
create or replace function public.gunny_checkout(p_user uuid,p_sex integer,p_cart integer[]) returns void language plpgsql security definer set search_path='' as $$
declare v_coins integer;v_gender integer;v_owned integer;v_wardrobe integer[]; cost integer:=0;k integer;i integer;m integer;owned_mask integer;price integer;
begin
 if p_sex is null or p_sex not between 0 and 1 or array_ndims(p_cart) is distinct from 1 or array_length(p_cart,1) is distinct from 7 or array_lower(p_cart,1) is distinct from 1 then raise exception 'Invalid cart';end if;
 select coins,gender,owned,wardrobe into strict v_coins,v_gender,v_owned,v_wardrobe from public.profiles where id=p_user for update;
 if v_gender<>p_sex then raise exception 'Invalid character';end if;
 for k in 0..6 loop
  m:=p_cart[k+1];if m is null or m<0 or m>1023 then raise exception 'Invalid cart';end if;
  owned_mask:=case when k=0 then v_owned else v_wardrobe[p_sex*6+k] end;
  for i in 0..9 loop
   if (m & (1<<i))<>0 then
    if k=0 and i not in(0,1,2,3,4,5,7,9) then raise exception 'Invalid weapon';end if;
    price:=case when k=0 then (array[0,650,950,450,550,800,1100,700,600,1200])[i+1] when k=1 then 300 when k in(5,6) then 100 else 150 end;
    if (owned_mask & (1<<i))=0 then cost:=cost+price;end if;
   end if;
  end loop;
 end loop;
 if v_coins<cost then raise exception 'Insufficient coins';end if;
 for k in 1..6 loop v_wardrobe[p_sex*6+k]:=v_wardrobe[p_sex*6+k] | p_cart[k+1];end loop;
 update public.profiles set coins=v_coins-cost,owned=v_owned | p_cart[1],wardrobe=v_wardrobe where id=p_user;
end;$$;
revoke all on function public.gunny_checkout(uuid,integer,integer[]) from public,anon,authenticated;
grant execute on function public.gunny_checkout(uuid,integer,integer[]) to service_role;
commit;
