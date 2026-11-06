-- Fix 42883 when equipping individual clothes: separate & from unary ~.
-- Replaces only gunny_equip; does not reset accounts, possessions or balances.
begin;
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

notify pgrst,'reload schema';
commit;
