begin;
alter table public.profiles add column if not exists max_damage integer not null default 0,
 add column if not exists weapon_uses integer[] not null default array_fill(0,array[10]);
create table if not exists public.gunny_stats_receipts(match_id uuid not null,user_id uuid not null references public.profiles(id),primary key(match_id,user_id));
alter table public.gunny_stats_receipts enable row level security;
revoke all on public.gunny_stats_receipts from public,anon,authenticated;
create or replace function public.gunny_stats793(p_match uuid,p_players jsonb) returns void language plpgsql security definer set search_path='' as $$
declare entry jsonb; uid uuid; w integer;
begin
 for entry in select * from jsonb_array_elements(p_players) loop
 uid:=(entry->>'id')::uuid;w:=greatest(0,least(9,coalesce((entry->>'weapon')::integer,0)));
 insert into public.gunny_stats_receipts values(p_match,uid) on conflict do nothing;
 if found then update public.profiles set max_damage=greatest(max_damage,coalesce((entry->>'damage')::integer,0)),weapon_uses[w+1]=case when w between 0 and 9 then least(2147483647::bigint,weapon_uses[w+1]::bigint+1)::integer else 0 end where id=uid;end if;
 end loop;
end;$$;
revoke all on function public.gunny_stats793(uuid,jsonb) from public,anon,authenticated;
grant execute on function public.gunny_stats793(uuid,jsonb) to service_role;
notify pgrst,'reload schema';
commit;
