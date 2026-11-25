GUNNY 7.9.18 HF17 - ADMIN / CURRENT INACTIVITY

Patch based on repo (9) plus HF15 admin RPC and HF16 compatibility.
No client, gameplay, VIP, asset, network or balance files are replaced.

DEPLOY
1. Back up Supabase. Run HF17-Preview-Inactivity.sql (read-only) in SQL Editor.
   If rows are returned, review them first: those accounts may become eligible for deletion when the policy is enabled.
2. Run supabase/migrations/202611250001_hf17_current_inactivity.sql.
   It defaults to refusing installation when an enabled policy already has overdue accounts.
   Only after reviewing and explicitly accepting their eligibility, change the indicated
   gunny.hf17_confirm_overdue value to yes and rerun the migration.
   No account DELETE is executed by the migration itself, but the existing maintenance/login
   logic can delete eligible accounts after commit. Existing manual schedules and extensions remain.
3. Copy patch files into repo root, commit/push and rebuild backend on Pi 5.
4. Refresh /admin/ and verify P1/P2 days. No JAR build required.

RULES
Last activity = greatest(last_seen_at, last_login_at, created_at). Current offline interval
resets on successful login or ongoing activity; no reset on policy toggle. Default 90 days
is overridden by account-specific days and explicit protected_until. Manual deletion schedule
remains independent and can expire earlier. Historical max_offline_seconds stays in DB but
is not used in the dashboard or to calculate expiration.

TESTS
Backend javac, 614 deterministic assertions, admin HTTP tests incl activity sorting,
retention validation and session/schedule flows, JS syntax, HTML control/id checks.
Browser rendering test blocked by the runtime browser administrator; not tested visually.
Production SQL/RPC not executed; verify after deploying. Earlier 401 API issue not addressed.
