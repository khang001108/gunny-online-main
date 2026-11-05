-- Existing accounts keep their selected character. New accounts choose on first login.
begin;
alter table public.profiles add column if not exists character_chosen boolean not null default true;
alter table public.profiles alter column character_chosen set default false;
commit;
