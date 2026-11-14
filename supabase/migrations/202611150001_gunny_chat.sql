begin;

create table if not exists public.gunny_chat (
    id bigint generated always as identity primary key,
    sender uuid not null references public.profiles(id) on delete cascade,
    receiver uuid references public.profiles(id) on delete cascade,
    sender_name text not null,
    message text not null check (char_length(message) <= 160),
    emoji integer not null check (emoji between -1 and 5),
    sent_at timestamptz not null default now(),
    read_at timestamptz,
    check (sender <> receiver),
    check (char_length(message) > 0 or emoji >= 0)
);
create index if not exists gunny_chat_public_idx on public.gunny_chat(id desc) where receiver is null;
create index if not exists gunny_chat_thread_idx on public.gunny_chat(sender, receiver, id desc) where receiver is not null;
create index if not exists gunny_chat_unread_idx on public.gunny_chat(receiver, sender, id) where read_at is null and receiver is not null;
create index if not exists gunny_chat_expiry_idx on public.gunny_chat(sent_at);
alter table public.gunny_chat enable row level security;
revoke all on public.gunny_chat from public, anon, authenticated;

create or replace function public.gunny_chat_api(
    p_user uuid, p_action text, p_peer integer, p_cursor bigint, p_message text, p_emoji integer
) returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    current_time_utc timestamptz := now();
    private_cutoff timestamptz := current_time_utc - interval '720 hours';
    public_cutoff timestamptz := date_trunc('day', current_time_utc at time zone 'Asia/Ho_Chi_Minh') at time zone 'Asia/Ho_Chi_Minh';
    peer_id uuid;
    display text;
    preview jsonb;
    history jsonb := '[]'::jsonb;
begin
    if p_action = 'purge' then
        delete from public.gunny_chat where sent_at < private_cutoff or (receiver is null and sent_at < public_cutoff);
        return '{}'::jsonb;
    end if;

    if p_peer > 0 then
        select id into peer_id from public.profiles where public_id = p_peer;
        if peer_id is null or peer_id = p_user or not exists (
            select 1 from public.gunny_friends f where f.accepted and
              ((f.sender = p_user and f.receiver = peer_id) or (f.receiver = p_user and f.sender = peer_id))
        ) then
            raise exception 'Chat requires an accepted friendship' using errcode = '42501';
        end if;
    end if;

    if p_action = 'send' then
        select display_name into strict display from public.profiles where id = p_user;
        if peer_id is not null then
            -- Allocate message IDs in commit order within this conversation.
            perform pg_advisory_xact_lock(hashtextextended(
                least(p_user, peer_id)::text || greatest(p_user, peer_id)::text, 0));
        end if;
        insert into public.gunny_chat(sender, receiver, sender_name, message, emoji)
        values (p_user, peer_id, display, p_message, p_emoji);
        return '{}'::jsonb;
    elsif p_action = 'read' then
        update public.gunny_chat set read_at = current_time_utc
        where sender = peer_id and receiver = p_user and id <= p_cursor and read_at is null and sent_at >= private_cutoff;
        return '{}'::jsonb;
    elsif p_action = 'friends' then
        return coalesce((
            select jsonb_agg(to_jsonb(contact) order by contact.unread desc, contact.name, contact.public_id)
            from (
                select p.public_id, p.display_name as name, exists (
                    select 1 from public.gunny_chat m where m.sender = p.id and m.receiver = p_user
                        and m.read_at is null and m.sent_at >= private_cutoff
                ) as unread
                from public.gunny_friends f
                join public.profiles p on p.id = case when f.sender = p_user then f.receiver else f.sender end
                where f.accepted and (f.sender = p_user or f.receiver = p_user)
            ) contact
        ), '[]'::jsonb);
    end if;

    select coalesce(jsonb_agg(to_jsonb(m) order by m.id desc), '[]'::jsonb) into preview from (
        select id, sender_name, message, emoji from public.gunny_chat
        where receiver is null and sent_at >= public_cutoff order by id desc limit 5
    ) m;

    if p_peer = 0 then
        select coalesce(jsonb_agg(to_jsonb(m) order by m.id desc), '[]'::jsonb) into history from (
            select id, sender_name, message, emoji from public.gunny_chat
            where receiver is null and sent_at >= public_cutoff and (p_cursor = 0 or id < p_cursor)
            order by id desc limit 17
        ) m;
    elsif p_peer > 0 then
        select coalesce(jsonb_agg(to_jsonb(m) order by m.id desc), '[]'::jsonb) into history from (
            select id, sender_name, message, emoji from public.gunny_chat
            where sent_at >= private_cutoff and (p_cursor = 0 or id < p_cursor)
                and ((sender = p_user and receiver = peer_id) or (receiver = p_user and sender = peer_id))
            order by id desc limit 17
        ) m;
    end if;

    return jsonb_build_object('preview', preview, 'history', history, 'unread', exists (
        select 1 from public.gunny_chat where receiver = p_user and sent_at >= private_cutoff and read_at is null
    ));
end;
$$;
revoke all on function public.gunny_chat_api(uuid,text,integer,bigint,text,integer) from public, anon, authenticated;
grant execute on function public.gunny_chat_api(uuid,text,integer,bigint,text,integer) to service_role;
notify pgrst,'reload schema';
commit;
