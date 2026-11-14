"""SQL review tests: localhost only; requires server flag gunny.isolated_test=1.

Each test gets its own disposable database. Never accepts production credentials.
Requires psycopg 3. Start PostgreSQL with: postgres -c gunny.isolated_test=1
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import datetime as dt
import json
from pathlib import Path
import threading
import unittest
import uuid
import psycopg
from psycopg import sql

ROOT = Path(__file__).resolve().parents[2]
PORT = None
DAY = dt.timedelta(days=1)

class Review(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.params = dict(host='127.0.0.1', port=PORT, user='postgres', autocommit=True,
                          connect_timeout=5, options='-c statement_timeout=8000 -c lock_timeout=5000')
        cls.control = psycopg.connect(dbname='gunny_test', **cls.params)
        cls.addClassCleanup(cls.control.close)
        marker = cls.control.execute("select current_setting('gunny.isolated_test',true)").fetchone()[0]
        if marker != '1':
            raise RuntimeError('Refusing tests: server is not marked gunny.isolated_test=1')
        for role in ('anon', 'authenticated', 'service_role'):
            if not cls.control.execute('select 1 from pg_roles where rolname=%s', (role,)).fetchone():
                cls.control.execute(sql.SQL('create role {}').format(sql.Identifier(role)))
        cls.baseline = (ROOT/'supabase/GUNNY-ALL.sql').read_text().split('-- Gunny 7.7.0: virtual accounts')[0]
        cls.migration = (ROOT/'supabase/migrations/202611120001_virtual_accounts_retention.sql').read_text()
        print('Test PostgreSQL:', cls.control.execute('show server_version').fetchone()[0], flush=True)

    def setUp(self):
        self.database = 'gunny_case_' + uuid.uuid4().hex
        self.control.execute(sql.SQL('create database {}').format(sql.Identifier(self.database)))
        self.addCleanup(self.drop_database)
        self.db = psycopg.connect(dbname=self.database, **self.params)
        self.addCleanup(self.db.close)
        self.db.execute("""create schema auth;
          create table auth.users(id uuid primary key,email text,encrypted_password text,
           created_at timestamptz default now(),updated_at timestamptz default now(),last_sign_in_at timestamptz,
           email_confirmed_at timestamptz,raw_app_meta_data jsonb default '{}',raw_user_meta_data jsonb default '{}');
          create function auth.uid() returns uuid language sql as $$ select null::uuid $$;""")
        self.db.execute(self.baseline)
        self.one, self.two, self.three, self.orphan = [uuid.uuid4() for _ in range(4)]
        for i, uid in enumerate((self.one, self.two, self.three, self.orphan)):
            self.db.execute("""insert into auth.users(id,email,encrypted_password,created_at,raw_app_meta_data,raw_user_meta_data)
              values(%s,%s,%s,now()-interval '300 days',%s::jsonb,%s::jsonb)""",
                            (uid, f'old{i}@login.invalid', 'unchanged-hash-'+str(i),
                             json.dumps({'provider':'email','custom':'preserve'}), json.dumps({'display_name':f'Gunner {i}'})))
        self.db.execute("""update public.profiles set coins=321,shopping_money=987,experience=12345,
          wins=19,owned=9,equipped=3,look='{1,2,3,4,5}' where id=%s""", (self.one,))
        self.db.execute("insert into public.gunny_account_admin(user_id) select id from auth.users on conflict do nothing")
        self.db.execute("update public.gunny_account_admin set last_seen_at=now()-interval '200 days'")
        self.db.execute('delete from public.profiles where id=%s', (self.orphan,))
        self.db.execute('update auth.users set email=null,encrypted_password=null where id=%s', (self.orphan,))
        self.db.execute("update auth.users set email_confirmed_at='2025-10-10T00:00:00Z' where id=%s", (self.one,))

    def drop_database(self):
        self.control.execute(sql.SQL('drop database {} with (force)').format(sql.Identifier(self.database)))

    def val(self, query, params=()): return self.db.execute(query, params).fetchone()[0]
    def admin(self, op, data): return self.val('select public.gunny_admin(%s,%s::jsonb)', (op,json.dumps(data)))
    def migrate(self, text=None):
        try: self.db.execute(self.migration if text is None else text)
        except psycopg.Error:
            self.db.rollback()
            raise
    def enable(self, days=90): return self.admin('retention-save', {'days':days,'enabled':True})
    def disable(self, days=90): return self.admin('retention-save', {'days':days,'enabled':False})
    def extend(self, uid=None, days=30, key=None):
        data = {'id':str(uid or self.one),'days':days,'request_key':str(key or uuid.uuid4())}
        self.admin('extend-offline',data)
        return data
    def account(self, uid=None): return self.val('select to_jsonb(a) from public.gunny_account_admin a where user_id=%s', (uid or self.one,))
    def protected(self, uid=None): return self.val('select protected_until from public.gunny_account_admin where user_id=%s', (uid or self.one,))
    def deadline(self, uid=None): return self.val('select public.gunny_inactive_deadline(%s)', (uid or self.one,))
    def snapshot(self):
        return (self.val("select jsonb_agg(jsonb_build_object('id',id,'email',email,'hash',encrypted_password) order by id) from auth.users"),
                self.val('select jsonb_agg(to_jsonb(p) order by id) from public.profiles p'))
    def schedule(self, uid, days, cancel=False):
        changed = self.db.execute("""update public.gunny_account_admin set delete_at=clock_timestamp()+%s*interval '1 day',
          delete_reason='Giữ lịch thủ công cũ',appeal='https://example.invalid/appeal',cancel_on_login=%s where user_id=%s""", (days,cancel,uid)).rowcount
        self.assertEqual(changed, 1)
    def schedules(self):
        return self.val("""select jsonb_agg(jsonb_build_object('id',user_id,'at',delete_at,'reason',delete_reason,
          'appeal',appeal,'cancel',cancel_on_login) order by user_id) from public.gunny_account_admin""")
    def assert_days(self, timestamp, days):
        self.assertLess(abs(timestamp-self.val('select clock_timestamp()')-days*DAY), dt.timedelta(seconds=5))
    def race(self, requests):
        barrier = threading.Barrier(len(requests))
        def worker(request):
            with psycopg.connect(dbname=self.database, **self.params) as db:
                barrier.wait(timeout=5)
                try: return ('ok',db.execute('select public.gunny_admin(%s,%s::jsonb)',(request[0],json.dumps(request[1]))).fetchone()[0])
                except psycopg.Error as error: return ('error',error.sqlstate)
        with ThreadPoolExecutor(max_workers=len(requests)) as pool: return list(pool.map(worker,requests))

    def test_defaults_disabled_90_days(self):
        self.migrate(); p=self.admin('retention-policy',{})
        self.assertEqual((p['enabled'],p['offline_days']),(False,90)); self.assertIsNone(self.deadline())
        self.assertEqual(self.admin('maintenance',{})['deleted'],0)

    def test_rerun_preserves_all_logins_profiles_and_manual_schedules(self):
        self.schedule(self.one,15,True); self.schedule(self.two,-10,False)
        before,schedules=self.snapshot(),self.schedules()
        self.migrate(); self.migrate()
        self.assertEqual(self.snapshot(),before); self.assertEqual(self.schedules(),schedules)
        self.assertEqual(self.val("select raw_app_meta_data->>'custom' from auth.users where id=%s",(self.one,)),'preserve')
        self.assertEqual(self.val("select raw_app_meta_data->>'account_type' from auth.users where id=%s",(self.one,)),'virtual')
        confirmed = self.val('select email_confirmed_at from auth.users where id=%s',(self.one,))
        self.assertEqual((confirmed.year,confirmed.month,confirmed.day),(2025,10,10))

    def test_disabled_extensions_accumulate_without_a_deadline(self):
        self.migrate(); self.extend(days=30); self.extend(days=15)
        self.assertEqual(self.account()['pending_extension_days'],45)
        self.assertIsNone(self.protected()); self.assertIsNone(self.deadline())

    def test_disabled_extension_cannot_shorten_existing_protection(self):
        self.migrate(); self.db.execute("update public.gunny_account_admin set protected_until=now()+interval '200 days' where user_id=%s",(self.one,))
        before=self.protected(); self.extend(days=30); self.extend(days=15)
        self.assertEqual(self.protected(),before); self.assertEqual(self.account()['pending_extension_days'],45)

    def test_enable_applies_pending_days_to_a_fresh_window_once(self):
        self.migrate(); self.extend(days=30); self.extend(days=15)
        self.db.execute("update public.gunny_account_admin set retention_since=now()-interval '200 days',last_seen_at=now()-interval '200 days'")
        self.enable(); self.assert_days(self.deadline(),135); self.assertEqual(self.account()['pending_extension_days'],0)
        before=self.protected(); self.enable(); self.assertEqual(self.protected(),before)

    def test_enable_preserves_later_protection_and_account_override(self):
        self.migrate(); self.admin('offline-days',{'id':str(self.one),'days':15})
        self.db.execute("update public.gunny_account_admin set protected_until=now()+interval '200 days' where user_id=%s",(self.one,))
        before=self.protected(); self.extend(); self.enable(days=120)
        self.assertEqual(self.protected(),before+30*DAY); self.assertEqual(self.account()['offline_days'],15)

    def test_enable_uses_new_default_plus_pending_days(self):
        self.migrate(); self.extend(); self.enable(days=120); self.assert_days(self.deadline(),150)

    def test_pending_days_use_account_override_instead_of_global_default(self):
        self.migrate(); self.admin('offline-days',{'id':str(self.one),'days':15})
        self.extend(); self.enable(days=120); self.assert_days(self.deadline(),45)

    def test_disable_extend_enable_keeps_both_old_and_new_grants(self):
        self.migrate(); self.enable(); self.extend(days=200); before=self.protected()
        self.disable(); self.extend(days=20); self.assertEqual(self.protected(),before)
        self.enable(); self.assertEqual(self.protected(),before+20*DAY)

    def test_enabled_extension_adds_to_current_deadline(self):
        self.migrate(); self.enable(); before=self.deadline(); self.extend()
        self.assertEqual(self.protected(),before+30*DAY)

    def test_retry_before_and_after_enable_is_idempotent(self):
        self.migrate(); request=self.extend(); self.admin('extend-offline',request)
        self.assertEqual(self.account()['pending_extension_days'],30)
        self.enable(); before=self.protected(); self.admin('extend-offline',request); self.assertEqual(self.protected(),before)
        request2=self.extend(days=10); before=self.protected(); self.admin('extend-offline',request2)
        self.assertEqual(self.protected(),before)
        self.assertEqual(self.val('select count(*) from public.gunny_offline_extensions'),2)
        self.assertEqual(self.val("select count(*) from public.gunny_admin_audit where action='extend-offline'"),2)

    def test_changed_days_or_target_with_same_key_is_rejected(self):
        self.migrate(); request=self.extend()
        for changed in ({**request,'days':31},{**request,'id':str(self.two)}):
            with self.subTest(changed=changed), self.assertRaises(psycopg.Error): self.admin('extend-offline',changed)
        self.assertEqual(self.account()['pending_extension_days'],30); self.assertEqual(self.account(self.two)['pending_extension_days'],0)

    def test_uuid_case_cannot_duplicate_a_request(self):
        self.migrate(); request=self.extend(key=uuid.UUID('abcdefab-cdef-abcd-efab-cdefabcdefab'))
        self.admin('extend-offline',{**request,'request_key':request['request_key'].upper()})
        self.assertEqual(self.account()['pending_extension_days'],30)

    def test_missing_null_and_malformed_keys_rejected(self):
        self.migrate()
        for key in (None,'','invalid','0000'):
            with self.subTest(key=key), self.assertRaises(psycopg.Error): self.admin('extend-offline',{'id':str(self.one),'days':30,'request_key':key})
        with self.assertRaises(psycopg.Error): self.admin('extend-offline',{'id':str(self.one),'days':30})
        self.assertEqual(self.account()['pending_extension_days'],0); self.assertEqual(self.val('select count(*) from public.gunny_offline_extensions'),0)

    def test_invalid_days_or_absent_account_leave_no_receipt(self):
        self.migrate()
        for days in (None,0,-1,3651,1.5,'30',True):
            with self.subTest(days=days), self.assertRaises(psycopg.Error): self.admin('extend-offline',{'id':str(self.one),'days':days,'request_key':str(uuid.uuid4())})
        with self.assertRaises(psycopg.Error): self.extend(uid=uuid.uuid4())
        self.assertEqual(self.val('select count(*) from public.gunny_offline_extensions'),0)

    def test_receipts_survive_audit_cleanup(self):
        self.migrate(); request=self.extend(); self.db.execute("delete from public.gunny_admin_audit where action='extend-offline'")
        self.admin('extend-offline',request); self.assertEqual(self.account()['pending_extension_days'],30)

    def test_concurrent_duplicate_applies_once(self):
        self.migrate(); request={'id':str(self.one),'days':30,'request_key':str(uuid.uuid4())}
        self.assertEqual([r[0] for r in self.race([('extend-offline',request)]*2)],['ok','ok'])
        self.assertEqual(self.account()['pending_extension_days'],30)
        self.assertEqual(self.val('select count(*) from public.gunny_offline_extensions'),1)
        self.assertEqual(self.val("select count(*) from public.gunny_admin_audit where action='extend-offline'"),1)

    def test_concurrent_same_key_other_account_rejected(self):
        self.migrate(); key=str(uuid.uuid4())
        result=self.race([('extend-offline',{'id':str(uid),'days':30,'request_key':key}) for uid in (self.one,self.two)])
        self.assertEqual(sorted(r[0] for r in result),['error','ok'])
        self.assertEqual(self.account()['pending_extension_days']+self.account(self.two)['pending_extension_days'],30)

    def test_concurrent_duplicate_while_enabled_extends_once(self):
        self.migrate(); self.enable(); before=self.deadline()
        request={'id':str(self.one),'days':30,'request_key':str(uuid.uuid4())}
        self.assertEqual([r[0] for r in self.race([('extend-offline',request)]*2)],['ok','ok'])
        self.assertEqual(self.protected(),before+30*DAY)

    def test_concurrent_distinct_requests_accumulate(self):
        self.migrate(); result=self.race([('extend-offline',{'id':str(self.one),'days':n,'request_key':str(uuid.uuid4())}) for n in (30,15)])
        self.assertEqual([r[0] for r in result],['ok','ok']); self.assertEqual(self.account()['pending_extension_days'],45)

    def test_enable_racing_with_extension_does_not_lose_days(self):
        self.migrate(); result=self.race([('retention-save',{'days':90,'enabled':True}),('extend-offline',{'id':str(self.one),'days':30,'request_key':str(uuid.uuid4())})])
        self.assertEqual([r[0] for r in result],['ok','ok'])
        self.assertEqual(self.account()['pending_extension_days'],0); self.assert_days(self.deadline(),120)

    def test_manual_schedules_unchanged_by_extend_and_policy_toggle(self):
        self.schedule(self.one,10,True); self.schedule(self.two,15,False); before=self.schedules()
        self.migrate(); self.extend(); self.extend(uid=self.two); self.enable(); self.disable(); self.enable(days=60)
        self.assertEqual(self.schedules(),before)

    def test_due_manual_schedule_still_runs_when_retention_off(self):
        self.schedule(self.one,-1); self.schedule(self.two,10); self.migrate(); self.extend()
        self.assertEqual(self.admin('maintenance',{})['deleted'],1)
        self.assertEqual(self.val('select count(*) from auth.users where id=%s',(self.one,)),0)
        self.assertEqual(self.val('select count(*) from auth.users where id=%s',(self.two,)),1)
        self.assertEqual(self.val('select action from public.gunny_admin_audit where target=%s order by id desc limit 1',(str(self.one),)),'scheduled-delete')

    def test_cancel_on_login_flags_keep_existing_manual_behavior(self):
        self.schedule(self.one,10,True); self.schedule(self.two,10,False); self.migrate(); before=self.account(self.two)['delete_at']
        self.admin('login',{'id':str(self.one)}); self.admin('login',{'id':str(self.two)})
        self.assertIsNone(self.account()['delete_at']); self.assertEqual(self.account(self.two)['delete_at'],before)

    def test_receipt_cannot_be_reused_after_account_deletion(self):
        self.migrate(); request=self.extend(); self.schedule(self.one,-1); self.admin('maintenance',{})
        with self.assertRaises(psycopg.Error): self.admin('extend-offline',{**request,'id':str(self.two)})
        self.assertEqual(self.account(self.two)['pending_extension_days'],0)
        self.admin('extend-offline',request)
        self.assertEqual(self.val('select count(*) from auth.users where id=%s',(self.one,)),0)
        self.assertEqual(self.val('select count(*) from public.gunny_account_admin where user_id=%s',(self.one,)),0)

    def test_due_manual_schedule_blocks_login_even_if_auto_cancel_was_true(self):
        self.schedule(self.one,-1,True); self.migrate(); self.extend()
        result=self.admin('login',{'id':str(self.one)})
        self.assertTrue(result['blocked'])
        self.assertEqual(self.val('select count(*) from auth.users where id=%s',(self.one,)),0)

    def test_extension_and_enable_preserve_all_credentials_and_profiles(self):
        before=self.snapshot(); self.migrate(); self.extend(); self.enable(); self.extend(days=15)
        self.disable(); self.extend(days=10); self.enable(days=120)
        self.assertEqual(self.snapshot(),before)

    def test_rerun_keeps_active_policy_pending_credit_and_history(self):
        self.migrate(); self.enable(days=120); self.extend(); self.disable(days=120); self.extend(days=15)
        before=(self.admin('retention-policy',{}),self.account(),self.snapshot(),self.val('select count(*) from public.gunny_admin_audit'))
        self.migrate()
        self.assertEqual((self.admin('retention-policy',{}),self.account(),self.snapshot(),self.val('select count(*) from public.gunny_admin_audit')),before)

    def test_legacy_receipt_import_does_not_reapply_days(self):
        request={'id':str(self.one),'days':30,'request_key':str(uuid.uuid4())}
        self.db.execute("insert into public.gunny_admin_audit(action,target,detail) values('extend-offline',%s,%s::jsonb)",(str(self.one),json.dumps(request)))
        self.migrate(); self.admin('extend-offline',request)
        self.assertEqual(self.account()['pending_extension_days'],0); self.assertIsNone(self.protected())
        self.assertEqual(self.val('select count(*) from public.gunny_offline_extensions'),1)

    def test_identical_legacy_receipts_are_deduplicated_by_uuid(self):
        request={'days':30,'request_key':'abcdefab-cdef-abcd-efab-cdefabcdefab'}
        for key in (request['request_key'],request['request_key'].upper()):
            self.db.execute("insert into public.gunny_admin_audit(action,target,detail) values('extend-offline',%s,%s::jsonb)",(str(self.one),json.dumps({**request,'request_key':key})))
        self.migrate(); self.migrate()
        self.assertEqual(self.val('select count(*) from public.gunny_offline_extensions'),1)
        self.assertEqual(self.val("select count(*) from public.gunny_admin_audit where action='extend-offline'"),2)

    def test_legacy_compact_braced_uuid_and_integer_text_are_normalized(self):
        key=uuid.UUID('abcdefab-cdef-abcd-efab-cdefabcdefab')
        for text in (key.hex, '{'+str(key)+'}'):
            self.db.execute("insert into public.gunny_admin_audit(action,target,detail) values('extend-offline',%s,%s::jsonb)",(str(self.one),json.dumps({'days':' +030 ','request_key':text})))
        self.migrate()
        self.extend(key=key)
        self.assertEqual(self.val('select count(*) from public.gunny_offline_extensions'),1)
        self.assertEqual(self.account()['pending_extension_days'],0)

    def test_invalid_historical_receipts_do_not_break_the_upgrade(self):
        for data in ({'days':30},{'request_key':'broken','days':30},
                     {'request_key':str(uuid.uuid4()),'days':'999999999999999999999'},
                     {'request_key':str(uuid.uuid4()),'days':0}):
            self.db.execute("insert into public.gunny_admin_audit(action,target,detail) values('extend-offline',%s,%s::jsonb)",(str(self.one),json.dumps(data)))
        before=self.snapshot();self.migrate()
        self.assertEqual(self.snapshot(),before)
        self.assertEqual(self.val('select count(*) from public.gunny_offline_extensions'),0)
        self.assertEqual(self.val("select count(*) from public.gunny_admin_audit where action='extend-offline'"),4)

    def test_legacy_same_key_different_days_aborts(self):
        key=str(uuid.uuid4())
        for days in (30,31):
            self.db.execute("insert into public.gunny_admin_audit(action,target,detail) values('extend-offline',%s,%s::jsonb)",(str(self.one),json.dumps({'days':days,'request_key':key})))
        before=self.snapshot()
        with self.assertRaisesRegex(psycopg.Error,'Conflicting historical'): self.migrate()
        self.assertEqual(self.snapshot(),before)

    def test_conflicting_legacy_key_aborts_preserving_data(self):
        key=str(uuid.uuid4())
        for uid in (self.one,self.two): self.db.execute("insert into public.gunny_admin_audit(action,target,detail) values('extend-offline',%s,%s::jsonb)",(str(uid),json.dumps({'days':30,'request_key':key})))
        before=self.snapshot()
        with self.assertRaisesRegex(psycopg.Error,'Conflicting historical'): self.migrate()
        self.assertEqual(self.snapshot(),before); self.assertIsNone(self.val("select to_regclass('public.gunny_retention_policy')"))

    def test_guard_rolls_back_profile_hash_and_orphan_email_changes(self):
        before=self.snapshot()
        mutations=["update public.profiles set coins=999;","update auth.users set encrypted_password='tampered';","update auth.users set email='tampered@invalid.test' where id='"+str(self.orphan)+"';"]
        for mutation in mutations:
            with self.subTest(mutation=mutation), self.assertRaisesRegex(psycopg.Error,'Upgrade cancelled'): self.migrate(self.migration.replace('do $preserve$',mutation+'\ndo $preserve$',1))
            self.assertEqual(self.snapshot(),before); self.assertIsNone(self.val("select to_regclass('public.gunny_retention_policy')"))

    def test_guard_rolls_back_manual_schedule_changes(self):
        self.schedule(self.one,10,True); before=self.schedules()
        with self.assertRaisesRegex(psycopg.Error,'manual deletion schedules changed'): self.migrate(self.migration.replace('do $preserve$',"update public.gunny_account_admin set delete_at=now();\ndo $preserve$",1))
        self.assertEqual(self.schedules(),before)

    def test_latest_login_not_hidden_by_older_last_seen(self):
        self.migrate(); self.enable()
        self.db.execute("update public.gunny_account_admin set retention_since=now()-interval '100 days',last_seen_at=now()-interval '100 days',last_login_at=clock_timestamp() where user_id=%s",(self.one,))
        self.assert_days(self.deadline(),90)

    def test_inactive_cleanup_and_login_use_correct_deadline(self):
        self.migrate(); self.enable()
        self.db.execute("update public.gunny_account_admin set retention_since=now()-interval '100 days',last_seen_at=now()-interval '100 days' where user_id=%s",(self.one,))
        self.assertEqual(self.admin('maintenance',{})['deleted'],1)
        self.assertEqual(self.val('select action from public.gunny_admin_audit where target=%s order by id desc limit 1',(str(self.one),)),'inactive-delete')
        self.admin('login',{'id':str(self.two)}); self.assert_days(self.deadline(self.two),90)

    def test_rls_and_privileged_rpc_not_exposed_to_players(self):
        self.migrate()
        for role in ('anon','authenticated'):
            for privilege in ('SELECT','INSERT','UPDATE','DELETE'): self.assertFalse(self.val("select has_table_privilege(%s,'public.gunny_offline_extensions',%s)",(role,privilege)))
            self.assertFalse(self.val("select has_function_privilege(%s,'public.gunny_admin(text,jsonb)','EXECUTE')",(role,)))
        self.assertTrue(self.val("select relrowsecurity from pg_class where oid='public.gunny_offline_extensions'::regclass"))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__); parser.add_argument('--port',type=int,required=True)
    options,remaining=parser.parse_known_args(); PORT=options.port
    unittest.main(argv=[__file__,*remaining],verbosity=2)
