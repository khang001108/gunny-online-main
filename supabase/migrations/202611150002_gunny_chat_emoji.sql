begin;
alter table public.gunny_chat drop constraint if exists gunny_chat_emoji_check;
alter table public.gunny_chat add constraint gunny_chat_emoji_check check (emoji between -1 and 66);
notify pgrst,'reload schema';
commit;
