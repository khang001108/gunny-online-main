-- Run once after the existing Gunny migrations. Only the server service_role may checkout.
begin;
alter table public.profiles add column if not exists wardrobe integer[];
-- Preserve the outfit already worn before the shop update; every category also starts with item 0.
update public.profiles p set wardrobe=(select array_agg(case when (i-1)/6=p.gender then 1 | (1 << case when (i-1)%6=0 then p.skin else p.look[(i-1)%6] end) else 1 end order by i) from generate_series(1,12) i) where wardrobe is null;
alter table public.profiles alter column wardrobe set default '{1,1,1,1,1,1,1,1,1,1,1,1}';
alter table public.profiles alter column wardrobe set not null;
do $$ begin if not exists(select 1 from pg_constraint where conname='gunny_wardrobe_valid' and conrelid='public.profiles'::regclass) then alter table public.profiles add constraint gunny_wardrobe_valid check(array_ndims(wardrobe)=1 and array_lower(wardrobe,1)=1 and cardinality(wardrobe)=12 and array_position(wardrobe,null) is null and 0<=all(wardrobe) and 1023>=all(wardrobe));end if;end $$;
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
    if k=0 and i not in(0,3,5,9) then raise exception 'Invalid weapon';end if;
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
