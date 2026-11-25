-- Read only. Uses the currently configured policy and the proposed HF17 rule.
-- If rows appear, review them before running the HF17 migration (which may enable deletion of these accounts).
with deadlines as (
 select u.id,u.email, greatest(a.last_seen_at,a.last_login_at,u.created_at) last_activity,
        coalesce(a.offline_days,p.offline_days) allowed_days,
        p.enabled,
        greatest(greatest(a.last_seen_at,a.last_login_at,u.created_at)
          + make_interval(secs=>86400::double precision*coalesce(a.offline_days,p.offline_days)),a.protected_until) proposed_deadline
 from auth.users u left join public.gunny_account_admin a on a.user_id=u.id
 cross join public.gunny_retention_policy p where p.singleton
)
select id,email,last_activity,allowed_days,proposed_deadline,enabled
from deadlines where proposed_deadline<=now() order by proposed_deadline;
