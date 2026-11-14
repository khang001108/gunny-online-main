begin;

alter table public.profiles
  add column if not exists forge_progress integer[] not null default array_fill(0,array[130]);

update public.profiles
set forge_levels=(select array_agg(least(v,15)::smallint order by n)
                  from unnest(forge_levels) with ordinality as x(v,n));

alter table public.profiles drop constraint if exists gunny780_forge_levels;
alter table public.profiles drop constraint if exists gunny781_forge_levels;
alter table public.profiles drop constraint if exists gunny781_forge_progress;
alter table public.profiles add constraint gunny781_forge_levels
 check(array_ndims(forge_levels)=1 and array_lower(forge_levels,1)=1 and cardinality(forge_levels)=130
       and 0<=all(forge_levels) and 15>=all(forge_levels) and array_position(forge_levels,null) is null);
alter table public.profiles add constraint gunny781_forge_progress
 check(array_ndims(forge_progress)=1 and array_lower(forge_progress,1)=1 and cardinality(forge_progress)=130
       and 0<=all(forge_progress) and 3750>all(forge_progress) and array_position(forge_progress,null) is null);

create or replace function public.gunny_economy780(p_user uuid,p_operation text,p_payload jsonb,p_key text)
returns void language plpgsql security definer set search_path='' as $$
declare receipt public.gunny780_requests%rowtype;p public.profiles%rowtype;qty integer;sex integer;kind integer;item integer;rank integer;progress integer;idx integer;cost integer;spend integer;
begin
 if p_user is null or p_key is null or p_key !~ '^[A-Za-z0-9_-]{1,64}$' or p_payload is null or jsonb_typeof(p_payload)<>'object' or p_operation not in('buy','forge') then raise exception 'Invalid economy request';end if;
 perform pg_advisory_xact_lock(hashtextextended(p_key,780));
 select * into receipt from public.gunny780_requests where request_key=p_key;
 if found then
  if receipt.user_id<>p_user or receipt.operation<>p_operation or receipt.payload<>p_payload then raise exception 'Request key payload mismatch';end if;
  return;
 end if;
 select * into strict p from public.profiles where id=p_user for update;
 if p_operation='buy' then
  qty:=(p_payload->>'quantity')::integer;
  if qty is null or qty not between 1 and 1000 or p_payload<>jsonb_build_object('quantity',qty) then raise exception 'Invalid quantity';end if;
  cost:=qty*80;
  if p.shopping_money<cost or p.stones>999999-qty then raise exception 'Insufficient shopping money or stone limit';end if;
  update public.profiles set shopping_money=shopping_money-cost,stones=stones+qty where id=p_user;
 else
  sex:=(p_payload->>'sex')::integer;kind:=(p_payload->>'kind')::integer;item:=(p_payload->>'item')::integer;rank:=(p_payload->>'rank')::integer;progress:=(p_payload->>'progress')::integer;
  if sex is null or kind is null or item is null or rank is null or progress is null or sex<>p.gender or kind not in(0,2,3,6) or item not between 0 and 9 or rank not between 0 and 14 or progress not between 0 and 3749 or p_payload<>jsonb_build_object('sex',sex,'kind',kind,'item',item,'rank',rank,'progress',progress) then raise exception 'Invalid forge request';end if;
  if kind=0 then
   if item not in(0,1,2,3,4,5,7,9) or (p.owned&(1<<item))=0 then raise exception 'Item not owned';end if;idx:=item+1;
  else
   if (p.wardrobe[sex*6+kind]&(1<<item))=0 then raise exception 'Item not owned';end if;idx:=11+sex*60+(kind-1)*10+item;
  end if;
  if p.forge_levels[idx]<>rank or p.forge_progress[idx]<>progress then raise exception 'Forge level changed';end if;
  if p.stones<=0 then raise exception 'Insufficient stones';end if;
  cost:=250*(rank+1);spend:=least(p.stones,cost-progress);
  if progress+spend>=cost then
   update public.profiles set stones=stones-spend,forge_levels[idx]=(rank+1)::smallint,forge_progress[idx]=0 where id=p_user;
  else
   update public.profiles set stones=stones-spend,forge_progress[idx]=progress+spend where id=p_user;
  end if;
 end if;
 insert into public.gunny780_requests(request_key,user_id,operation,payload) values(p_key,p_user,p_operation,p_payload);
end;$$;

revoke all on function public.gunny_economy780(uuid,text,jsonb,text) from public,anon,authenticated;
grant execute on function public.gunny_economy780(uuid,text,jsonb,text) to service_role;
commit;
