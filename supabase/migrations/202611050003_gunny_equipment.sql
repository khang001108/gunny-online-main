-- Run AFTER shop 7.1.6 and arsenal 7.1.7 migrations. Earned coins are preserved.
begin;
alter table public.profiles add column if not exists shopping_money integer not null default 0 check(shopping_money between 0 and 999999);
alter table public.profiles add column if not exists worn_mask smallint;
update public.profiles set worn_mask=case when custom_look then 125 else 3 end where worn_mask is null;
alter table public.profiles alter column worn_mask set default 3;
alter table public.profiles alter column worn_mask set not null;
do $$ begin if not exists(select 1 from pg_constraint where conname='gunny_worn_mask_valid' and conrelid='public.profiles'::regclass) then alter table public.profiles add constraint gunny_worn_mask_valid check(worn_mask between 0 and 127 and ((worn_mask&2)=0 or (worn_mask&124)=0) and custom_look=((worn_mask&2)=0));end if;end $$;
create table if not exists public.gunny_exchanges(user_id uuid not null references public.profiles(id) on delete cascade,request_key text not null check(char_length(request_key) between 1 and 64),amount integer not null check(amount between 1 and 9999),created_at timestamptz not null default now(),primary key(user_id,request_key));
alter table public.gunny_exchanges enable row level security;
revoke all on public.gunny_exchanges from public,anon,authenticated;
grant all on public.gunny_exchanges to service_role;
create or replace function public.gunny_exchange(p_user uuid,p_amount integer,p_key text) returns void language plpgsql security definer set search_path='' as $$
declare earned integer;money integer;prior integer;
begin
 if p_amount is null or p_amount not between 1 and 9999 or p_key is null or p_key !~ '^[A-Za-z0-9-]{1,64}$' then raise exception 'Invalid exchange';end if;
 select coins,shopping_money into strict earned,money from public.profiles where id=p_user for update;
 select amount into prior from public.gunny_exchanges where user_id=p_user and request_key=p_key;
 if found then if prior<>p_amount then raise exception 'Invalid exchange key';end if;return;end if;
 if earned<p_amount*100 then raise exception 'Insufficient earned coins';end if;if money>999999-p_amount then raise exception 'Shopping wallet limit';end if;
 update public.profiles set coins=earned-p_amount*100,shopping_money=money+p_amount where id=p_user;
 insert into public.gunny_exchanges(user_id,request_key,amount) values(p_user,p_key,p_amount);
end;$$;
revoke all on function public.gunny_exchange(uuid,integer,text) from public,anon,authenticated;
grant execute on function public.gunny_exchange(uuid,integer,text) to service_role;
create or replace function public.gunny_equip(p_user uuid,p_kind integer,p_item integer) returns void language plpgsql security definer set search_path='' as $$
declare sex integer;owned_mask integer;ward integer[];parts integer[];mask integer;weapon integer;skin_id integer;
begin
 if p_kind is null or p_kind not between 0 and 6 or p_item is null or p_item not between -1 and 9 then raise exception 'Invalid equipment';end if;
 select gender,owned,wardrobe,look,worn_mask,equipped,skin into strict sex,owned_mask,ward,parts,mask,weapon,skin_id from public.profiles where id=p_user for update;
 if p_item>=0 then
  if p_kind=0 then if p_item not in(0,1,2,3,4,5,7,9) or (owned_mask&(1<<p_item))=0 then raise exception 'Item not owned';end if;weapon:=p_item;
  else if (ward[sex*6+p_kind]&(1<<p_item))=0 then raise exception 'Item not owned';end if;if p_kind=1 then skin_id:=p_item;else parts[p_kind-1]:=p_item;end if;end if;
 end if;
 if p_item<0 then mask:=mask & ~(1<<p_kind);
 elsif p_kind=1 then mask:=(mask&1)|2;
 elsif p_kind>=2 then mask:=(mask & (~2))|(1<<p_kind);
 else mask:=mask|1;end if;
 update public.profiles set equipped=weapon,skin=skin_id,look=parts,worn_mask=mask,custom_look=((mask&2)=0) where id=p_user;
end;$$;
revoke all on function public.gunny_equip(uuid,integer,integer) from public,anon,authenticated;
grant execute on function public.gunny_equip(uuid,integer,integer) to service_role;
create or replace function public.gunny_buy_weapon(p_user uuid,p_weapon integer) returns void language plpgsql security definer set search_path='' as $$
declare v_money integer;v_owned integer;cost integer;
begin
 if p_weapon is null or p_weapon not in(0,1,2,3,4,5,7,9) then raise exception 'Invalid weapon';end if;
 select shopping_money,owned into strict v_money,v_owned from public.profiles where id=p_user for update;
 cost:=(array[0,650,950,450,550,800,1100,700,600,1200])[p_weapon+1];
 if (v_owned & (1<<p_weapon))=0 then
  if v_money<cost then raise exception 'Insufficient coins';end if;
  v_money:=v_money-cost;v_owned:=v_owned | (1<<p_weapon);
 end if;
 update public.profiles set shopping_money=v_money,owned=v_owned,equipped=p_weapon where id=p_user;
end;$$;
revoke all on function public.gunny_buy_weapon(uuid,integer) from public,anon,authenticated;
grant execute on function public.gunny_buy_weapon(uuid,integer) to service_role;
create or replace function public.gunny_checkout(p_user uuid,p_sex integer,p_cart integer[]) returns void language plpgsql security definer set search_path='' as $$
declare v_money integer;v_gender integer;v_owned integer;v_wardrobe integer[]; cost integer:=0;k integer;i integer;m integer;owned_mask integer;price integer;
begin
 if p_sex is null or p_sex not between 0 and 1 or array_ndims(p_cart) is distinct from 1 or array_length(p_cart,1) is distinct from 7 or array_lower(p_cart,1) is distinct from 1 then raise exception 'Invalid cart';end if;
 select shopping_money,gender,owned,wardrobe into strict v_money,v_gender,v_owned,v_wardrobe from public.profiles where id=p_user for update;
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
 if v_money<cost then raise exception 'Insufficient coins';end if;
 for k in 1..6 loop v_wardrobe[p_sex*6+k]:=v_wardrobe[p_sex*6+k] | p_cart[k+1];end loop;
 update public.profiles set shopping_money=v_money-cost,owned=v_owned | p_cart[1],wardrobe=v_wardrobe where id=p_user;
end;$$;
revoke all on function public.gunny_checkout(uuid,integer,integer[]) from public,anon,authenticated;
grant execute on function public.gunny_checkout(uuid,integer,integer[]) to service_role;
commit;
