-- Thắp sáng: private records and atomic thank-you gifts. Run after the 7.8.0 schema.
begin;
create table if not exists public.gunny_support_settings (
 singleton boolean primary key default true check (singleton),
 qr text not null default '', mime text not null default 'image/jpeg',
 updated_at timestamptz not null default now()
);
insert into public.gunny_support_settings(singleton) values(true) on conflict do nothing;
create table if not exists public.gunny_support_entries (
 request_key uuid primary key,
 user_id uuid references public.profiles(id) on delete set null,
 display_name text not null,
 amount bigint not null check(amount between 0 and 1000000000000),
 comment text not null default '' check(char_length(comment)<=1000),
 stones integer not null check(stones between 0 and 999999),
 coins integer not null check(coins between 0 and 999999),
 money integer not null check(money between 0 and 999999),
 created_at timestamptz not null default now()
);
alter table public.gunny_support_settings enable row level security;
alter table public.gunny_support_entries enable row level security;
revoke all on public.gunny_support_settings,public.gunny_support_entries from public,anon,authenticated;
grant all on public.gunny_support_settings,public.gunny_support_entries to service_role;

create or replace function public.gunny_support_admin(p_op text,p_data jsonb default '{}')
returns jsonb language plpgsql security definer set search_path=public as $$
declare
 entry public.gunny_support_entries%rowtype;
 uid uuid; key uuid; gift_stones integer; gift_coins integer; gift_money integer;
 paid bigint; note text; player_name text; result jsonb;
begin
 if p_op='support-public' then
  return jsonb_build_object('qr',(select qr from gunny_support_settings where singleton),
   'mime',(select mime from gunny_support_settings where singleton),
   'entries',coalesce((select jsonb_agg(x) from (
    select display_name,amount,comment from gunny_support_entries order by created_at desc limit 100
   ) x),'[]'::jsonb));
 elsif p_op='support-list' then
  return coalesce((select jsonb_agg(x) from (
   select * from gunny_support_entries order by created_at desc limit 100
  ) x),'[]'::jsonb);
 elsif p_op='support-qr' then
  if p_data->>'mime' not in ('image/jpeg','image/png') or length(p_data->>'qr') not between 1 and 2000000
   then raise exception 'Invalid QR image'; end if;
  update gunny_support_settings set qr=p_data->>'qr',mime=p_data->>'mime',updated_at=now() where singleton;
  return '{}'::jsonb;
 elsif p_op='support-comment' then
  if char_length(coalesce(p_data->>'comment',''))>1000 then raise exception 'Comment too long'; end if;
  update gunny_support_entries set comment=coalesce(p_data->>'comment','') where request_key=(p_data->>'request_key')::uuid;
  if not found then raise exception 'Support entry not found'; end if;
  return '{}'::jsonb;
 elsif p_op<>'support-add' then
  raise exception 'Unknown support operation';
 end if;
 uid:=(p_data->>'id')::uuid;key:=(p_data->>'request_key')::uuid;
 gift_stones:=(p_data->>'stones')::integer;gift_coins:=(p_data->>'coins')::integer;gift_money:=(p_data->>'money')::integer;
 paid:=(p_data->>'amount')::bigint;note:=coalesce(p_data->>'comment','');
 if uid is null or key is null or gift_stones is null or gift_coins is null or gift_money is null or paid is null
  or gift_stones not between 0 and 999999 or gift_coins not between 0 and 999999 or gift_money not between 0 and 999999
  or paid not between 0 and 1000000000000 or char_length(note)>1000 then raise exception 'Invalid support entry'; end if;
 -- Serialize both same-key retries and grants to a shared wallet.
 perform pg_advisory_xact_lock(hashtextextended(key::text,0));
 select * into entry from gunny_support_entries where request_key=key;
 if found then
  if entry.user_id is distinct from uid or entry.amount<>paid or entry.stones<>gift_stones
   or entry.coins<>gift_coins or entry.money<>gift_money then raise exception 'Request key payload mismatch'; end if;
  return to_jsonb(entry);
 end if;
 select display_name into player_name from profiles where id=uid for update;
 if not found then raise exception 'Account not found'; end if;
 if exists(select 1 from profiles where id=uid and
  (stones+gift_stones>999999 or coins+gift_coins>999999 or shopping_money+gift_money>999999))
  then raise exception 'Gift exceeds wallet limit'; end if;
 update profiles set stones=stones+gift_stones,coins=coins+gift_coins,shopping_money=shopping_money+gift_money where id=uid;
 insert into gunny_support_entries(request_key,user_id,display_name,amount,comment,stones,coins,money)
 values(key,uid,player_name,paid,note,gift_stones,gift_coins,gift_money) returning * into entry;
 insert into gunny_admin_audit(action,target,detail) values('support-add',uid::text,to_jsonb(entry));
 return to_jsonb(entry);
end $$;
revoke all on function public.gunny_support_admin(text,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_support_admin(text,jsonb) to service_role;
notify pgrst,'reload schema';
commit;
