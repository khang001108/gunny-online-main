-- VIP progression/items and permanent wings. Additive; run manually before server/client update.
begin;
alter table public.profiles add column if not exists vip_exp integer not null default 0 check(vip_exp between 0 and 21200),
 add column if not exists vip_cards integer[] not null default array_fill(0,array[13]) check(cardinality(vip_cards)=13 and 0<=all(vip_cards) and 999999>=all(vip_cards)),
 add column if not exists vip_wings integer not null default 0 check(vip_wings between 0 and 31),
 add column if not exists vip_wing integer not null default -1 check(vip_wing between -1 and 5);
update public.profiles set vip_wing=5 where wing_equipped and wings>0 and vip_wing=-1;
create table if not exists public.gunny_vip_receipts(request_key text primary key,user_id uuid not null references public.profiles(id),signature jsonb not null,created_at timestamptz not null default now());
alter table public.gunny_vip_receipts enable row level security;
revoke all on public.gunny_vip_receipts from public,anon,authenticated;
create or replace function public.gunny_vip(p_user uuid,p_op text,p_item integer,p_quantity integer,p_key text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare p public.profiles%rowtype;r public.gunny_vip_receipts%rowtype;sig jsonb;costs integer[]:=array[500,600,700,800,1000,1200,1400,1600,2000,2800,3800,4800];prices integer[]:=array[200,500,1000,2500,5000];need integer[]:=array[1,3,5,8,12];lv integer:=0;xp integer;i integer;
begin
 if p_op is null or p_item is null or p_quantity is null or p_key is null or p_key!~'^[A-Za-z0-9_-]{1,64}$' then raise exception 'Invalid VIP request';end if;
 sig:=jsonb_build_object('op',p_op,'item',p_item,'quantity',p_quantity);
 perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('vip:'||p_key,0));
 select * into r from public.gunny_vip_receipts where request_key=p_key;
 if found then if r.user_id<>p_user or r.signature<>sig then raise exception 'Reward payload mismatch';end if;return '{}'::jsonb;end if;
 select * into strict p from public.profiles where id=p_user for update;
 xp:=p.vip_exp;while lv<12 and xp>=costs[lv+1] loop xp:=xp-costs[lv+1];lv:=lv+1;end loop;
 if p_op='use' then
  if p_item not between 0 and 12 or p_quantity<>1 or p.vip_cards[p_item+1]<1 or p.vip_exp>=21200 then raise exception 'No VIP card or VIP MAX';end if;
  p.vip_cards[p_item+1]:=p.vip_cards[p_item+1]-1;p.vip_exp:=case when p_item=12 then 21200 else least(21200,p.vip_exp+(p_item+1)*100) end;
 elsif p_op='buy' then
  if p_item not between 0 and 4 or p_quantity<>1 then raise exception 'Invalid wing';end if;
  if lv<need[p_item+1] or (p.vip_wings&(1<<p_item))<>0 or p.shopping_money<prices[p_item+1] then raise exception 'Wing VIP/ownership/money requirement';end if;
  p.shopping_money:=p.shopping_money-prices[p_item+1];p.vip_wings:=p.vip_wings|(1<<p_item);
 elsif p_op='equip' then
  if p_quantity<>1 or p_item not between -1 and 5 or (p_item=5 and p.wings<1) or (p_item between 0 and 4 and (p.vip_wings&(1<<p_item))=0) then raise exception 'Item not owned';end if;
  p.vip_wing:=p_item;p.wing_equipped:=p_item=5;
 elsif p_op='grant' then
  if p_item not between 0 and 12 or p_quantity not between 1 and 999999 or p.vip_cards[p_item+1]>999999-p_quantity then raise exception 'Invalid VIP quantity';end if;
  p.vip_cards[p_item+1]:=p.vip_cards[p_item+1]+p_quantity;
 elsif p_op='cheats' then
  if p_item not between 0 and 1 then raise exception 'Invalid cheat status';end if;
  if p_item=1 and not p.test_cheats then for i in 1..13 loop p.vip_cards[i]:=least(999999,p.vip_cards[i]+999);end loop;end if;
  p.test_cheats:=p_item=1;if not p.test_cheats then p.test_god:=false;end if;
 else raise exception 'Invalid VIP operation';end if;
 update public.profiles set vip_exp=p.vip_exp,vip_cards=p.vip_cards,vip_wings=p.vip_wings,vip_wing=p.vip_wing,wing_equipped=p.wing_equipped,shopping_money=p.shopping_money,test_cheats=p.test_cheats,test_god=p.test_god where id=p_user;
 insert into public.gunny_vip_receipts(request_key,user_id,signature) values(p_key,p_user,sig);
 return '{}'::jsonb;
end;$$;
revoke all on function public.gunny_vip(uuid,text,integer,integer,text) from public,anon,authenticated;
grant execute on function public.gunny_vip(uuid,text,integer,integer,text) to service_role;
notify pgrst,'reload schema';
commit;
