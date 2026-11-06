-- Optional but recommended: runs deletions even while Render sleeps.
-- Enable pg_cron in Supabase Database > Extensions first, then run this file.
select cron.unschedule(jobid) from cron.job where jobname='gunny-account-cleanup';
select cron.schedule('gunny-account-cleanup','* * * * *',
 $$select public.gunny_admin('maintenance','{}'::jsonb);$$);
