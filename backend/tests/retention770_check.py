"""Run only against an isolated local PostgreSQL database called gunny_test."""
import argparse
import datetime
import json
from pathlib import Path
import uuid
import psycopg


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, required=True)
    args = parser.parse_args()
    db = psycopg.connect(host="127.0.0.1", port=args.port, dbname="gunny_test", user="postgres", autocommit=True)
    root = Path(__file__).resolve().parents[2]
    with db, db.cursor() as cursor:
        cursor.execute("select current_database()")
        assert cursor.fetchone()[0] == "gunny_test"
        cursor.execute("""create role anon; create role authenticated; create role service_role;
          create schema auth;
          create table auth.users(id uuid primary key,email text,encrypted_password text,
           created_at timestamptz default now(),updated_at timestamptz default now(),last_sign_in_at timestamptz,
           email_confirmed_at timestamptz,raw_app_meta_data jsonb default '{}',raw_user_meta_data jsonb default '{}');
          create function auth.uid() returns uuid language sql as $$ select null::uuid $$;""")
        baseline = (root / "supabase/GUNNY-ALL.sql").read_text().split("-- Gunny 7.7.0: virtual accounts")[0]
        cursor.execute(baseline)
        one, two = uuid.uuid4(), uuid.uuid4()
        cursor.execute("insert into auth.users(id,email,encrypted_password,created_at) values(%s,'old@verified.test','unchanged-password-hash',now()-interval '300 days'),(%s,'fake@no-mail.invalid','second-password-hash',now()-interval '300 days')", (one, two))
        cursor.execute("update public.profiles set coins=321,shopping_money=987,experience=12345,wins=19 where id=%s", (one,))
        cursor.execute("update public.gunny_account_admin set last_seen_at=now()-interval '200 days'")
        cursor.execute("select jsonb_agg(to_jsonb(p) order by id) from public.profiles p")
        before = cursor.fetchone()[0]
        migration = (root / "supabase/migrations/202611120001_virtual_accounts_retention.sql").read_text()
        cursor.execute(migration)
        cursor.execute(migration)
        cursor.execute("select jsonb_agg(to_jsonb(p) order by id) from public.profiles p")
        assert cursor.fetchone()[0] == before
        cursor.execute("select encrypted_password,email,raw_app_meta_data->>'account_type',email_confirmed_at is not null from auth.users where id=%s", (one,))
        assert cursor.fetchone() == ("unchanged-password-hash", "old@verified.test", "virtual", True)
        print("PASS: Repeatable migration preserves UUIDs, passwords, login emails and every profile field")

        def admin(op, payload):
            cursor.execute("select public.gunny_admin(%s,%s::jsonb)", (op, json.dumps(payload)))
            return cursor.fetchone()[0]

        policy = admin("retention-policy", {})
        assert policy["offline_days"] == 90 and policy["enabled"] is False
        assert admin("maintenance", {})["deleted"] == 0
        print("PASS: Default is 90 days with automatic inactivity deletion disabled")
        admin("retention-save", {"days": 90, "enabled": True})
        cursor.execute("select public.gunny_inactive_deadline(%s)>now()+interval '89 days'", (one,))
        assert cursor.fetchone()[0]
        print("PASS: Enabling cleanup grants a full new window to long-offline existing accounts")
        admin("offline-days", {"id": str(one), "days": 30})
        cursor.execute("select public.gunny_inactive_deadline(%s)-now()", (one,))
        assert datetime.timedelta(days=29) < cursor.fetchone()[0] < datetime.timedelta(days=31)
        request = {"id": str(one), "days": 30, "request_key": str(uuid.uuid4())}
        admin("extend-offline", request)
        cursor.execute("select protected_until from public.gunny_account_admin where user_id=%s", (one,))
        extension = cursor.fetchone()[0]
        admin("extend-offline", request)
        cursor.execute("select protected_until from public.gunny_account_admin where user_id=%s", (one,))
        assert cursor.fetchone()[0] == extension
        print("PASS: Per-account limits and idempotent extensions are applied")
        admin("login", {"id": str(two)})
        cursor.execute("select public.gunny_inactive_deadline(%s)>now()+interval '89 days'", (two,))
        assert cursor.fetchone()[0]
        print("PASS: Logging in renews the inactivity deadline")
        cursor.execute("update public.gunny_account_admin set retention_since=now()-interval '100 days',last_seen_at=now()-interval '100 days',protected_until=null where user_id=%s", (one,))
        assert admin("maintenance", {})["deleted"] == 1
        cursor.execute("select id from auth.users")
        assert cursor.fetchall() == [(two,)]
        print("PASS: Only an expired disposable test account is deleted after cleanup is explicitly enabled")
        cursor.execute("select has_table_privilege('anon','public.gunny_retention_policy','SELECT'),has_function_privilege('authenticated','public.gunny_admin(text,jsonb)','EXECUTE')")
        assert cursor.fetchone() == (False, False)
        print("PASS: Policy tables and privileged RPCs remain inaccessible to player roles")


if __name__ == "__main__":
    main()
