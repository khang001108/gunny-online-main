"""PostgreSQL integration tests. LOCALHOST + gunny.isolated_test=1 only; disposable DB per test."""
import argparse,json,threading,unittest,uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import psycopg
import retention770_review as base
PORT=None

class Expansion(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        base.PORT=PORT
        base.Review.setUpClass.__func__(cls)
        cls.expansion=(base.ROOT/'supabase/migrations/202611130001_forge_dungeon.sql').read_text()
    drop_database=base.Review.drop_database
    val=base.Review.val
    snapshot=base.Review.snapshot
    def setUp(self):
        base.Review.setUp(self)
        self.db.execute(self.migration)
        self.original=self.snapshot()
        self.db.execute(self.expansion)
        self.db.execute('update profiles set shopping_money=999999,stones=10000,owned=1023,wardrobe=array_fill(1023,array[12]),experience=10000')
    def action(self,operation,payload,key=None,uid=None,db=None):
        (db or self.db).execute('select public.gunny_economy780(%s,%s,%s::jsonb,%s)',(uid or self.one,operation,json.dumps(payload),key or uuid.uuid4().hex))
    def forge(self,kind=0,item=0,rank=0,sex=0,**kwargs):self.action('forge',dict(kind=kind,item=item,rank=rank,sex=sex),**kwargs)
    def state(self,uid=None):return self.val('select jsonb_build_array(stones,shopping_money,forge_levels) from profiles where id=%s',(uid or self.one,))
    def match(self,kind=2,winner=0,damage=10):
        match=uuid.uuid4();players=[dict(id=str(self.one),team=0,damage=damage),dict(id=str(self.two),team=1 if kind==2 else 0,damage=damage)]
        if kind==2:self.db.execute('select public.gunny_finish_match(%s,%s,%s::jsonb,%s)',(match,winner,json.dumps(players),kind))
        else:self.db.execute('select public.gunny_finish_dungeon780(%s,%s,%s::jsonb,0)',(match,winner,json.dumps(players)))
        return match,players
    def claim(self,match,pick,card,uid=None,db=None):return (db or self.db).execute('select public.gunny_claim780(%s,%s,%s,%s)',(match,uid or self.one,pick,card)).fetchone()[0]
    def race(self,worker,count=8):
        barrier=threading.Barrier(count)
        def work(i):
            with psycopg.connect(dbname=self.database,**self.params) as db:
                barrier.wait(timeout=5)
                try:worker(db,i);return 'ok'
                except psycopg.Error as e:return e.sqlstate
        with ThreadPoolExecutor(max_workers=count) as pool:return list(pool.map(work,range(count)))
    def test_repeat_migration_preserves_every_existing_field(self):
        snapshot=self.snapshot();self.db.execute(self.expansion);self.assertEqual(snapshot,self.snapshot())
        old_auth,old_profiles=self.original;self.assertEqual(old_auth,snapshot[0])
        for row in old_profiles:
            current=self.val('select to_jsonb(p) from profiles p where id=%s',(row['id'],))
            for field in ('id','display_name','equipped','look','wins','coins'):self.assertEqual(row[field],current[field])
    def test_rerun_preserves_spent_stones_forge_receipts_and_cards(self):
        self.action('buy',{'quantity':3},key='persistent-buy');self.forge(key='persistent-forge');match,_=self.match();reward=self.claim(match,0,3);before=self.snapshot();state=self.state()
        self.db.execute(self.expansion)
        self.assertEqual(before,self.snapshot());self.assertEqual(state,self.state());self.assertEqual(reward,self.claim(match,0,3));self.action('buy',{'quantity':3},key='persistent-buy');self.assertEqual(state,self.state())
    def test_new_fields_default_empty_without_resetting_users(self):
        uid=uuid.uuid4();self.db.execute("insert into auth.users(id,email,encrypted_password) values(%s,'new@fake.test','preserved')",(uid,))
        self.assertEqual(self.val('select stones=0 and not dungeon_normal_clear and forge_levels=array_fill(0::smallint,array[130]) from profiles where id=%s',(uid,)),True)
    def test_buy_exact_price(self):
        self.action('buy',{'quantity':10});self.assertEqual(self.state()[:2],[10010,999199])
    def test_buy_retry_charges_once(self):
        for _ in range(5):self.action('buy',{'quantity':3},key='same-buy')
        self.assertEqual(self.state()[:2],[10003,999759])
    def test_request_key_changed_quantity_rejected(self):
        self.action('buy',{'quantity':3},key='same-buy');before=self.state()
        with self.assertRaises(psycopg.Error):self.action('buy',{'quantity':4},key='same-buy')
        self.assertEqual(before,self.state())
    def test_request_key_cannot_change_user_or_operation(self):
        self.action('buy',{'quantity':3},key='global-key')
        with self.assertRaises(psycopg.Error):self.action('buy',{'quantity':3},key='global-key',uid=self.two)
        with self.assertRaises(psycopg.Error):self.forge(key='global-key')
    def test_invalid_buy_inputs_never_mutate_balances(self):
        for payload in ({'quantity':0},{'quantity':1000},{'quantity':-1},{'quantity':None},{'quantity':'1'},{'quantity':1,'extra':4}):
            before=self.state()
            with self.assertRaises(psycopg.Error):self.action('buy',payload)
            self.assertEqual(before,self.state())
    def test_insufficient_wallet_and_stone_cap(self):
        self.db.execute('update profiles set shopping_money=79 where id=%s',(self.one,))
        with self.assertRaises(psycopg.Error):self.action('buy',{'quantity':1})
        self.db.execute('update profiles set shopping_money=999999,stones=999999 where id=%s',(self.one,))
        with self.assertRaises(psycopg.Error):self.action('buy',{'quantity':1})
    def test_first_second_and_third_forge_costs(self):
        for rank in range(3):self.forge(rank=rank)
        stones,_,ranks=self.state();self.assertEqual(stones,10000-4-8-12);self.assertEqual(ranks[0],3)
    def test_forge_retry_once(self):
        for _ in range(3):self.forge(key='forge-one')
        self.assertEqual(self.state()[0],9996);self.assertEqual(self.state()[2][0],1)
    def test_stale_rank_rejected_without_spending(self):
        self.forge()
        with self.assertRaises(psycopg.Error):self.forge(rank=0)
        self.assertEqual(self.state()[0],9996)
    def test_gender_and_item_indexing(self):
        self.forge(kind=2,item=9);self.forge(kind=6,item=9)
        self.db.execute('update profiles set gender=1 where id=%s',(self.one,));self.forge(kind=2,item=9,sex=1);self.forge(kind=6,item=9,sex=1)
        ranks=self.state()[2];self.assertEqual([i for i,n in enumerate(ranks) if n],[29,69,89,129])
    def test_unowned_invalid_gender_and_rank_cap(self):
        self.db.execute('update profiles set owned=1,wardrobe=array_fill(1,array[12]) where id=%s',(self.one,))
        for args in [dict(item=1),dict(kind=2,item=1),dict(sex=1),dict(kind=7),dict(item=10),dict(rank=99)]:
            with self.assertRaises(psycopg.Error):self.forge(**args)
        self.assertEqual(self.state()[0],10000)
    def test_only_weapon_cloth_hat_and_glasses_can_be_forged(self):
        for kind in (1,4,5):
            before=self.state()
            with self.assertRaises(psycopg.Error):self.forge(kind=kind)
            self.assertEqual(before,self.state())
        for kind in (0,2,3,6):self.forge(kind=kind)
    def test_existing_other_category_ranks_are_preserved(self):
        self.db.execute('update profiles set forge_levels[11]=7,forge_levels[41]=8,forge_levels[51]=9 where id=%s',(self.one,))
        before=self.state();self.db.execute(self.expansion);self.assertEqual(before,self.state())
    def test_zero_stones_rejected(self):
        self.db.execute('update profiles set stones=3 where id=%s',(self.one,))
        with self.assertRaises(psycopg.Error):self.forge()
        self.assertEqual(self.state()[0],3)
    def test_forge_array_constraints(self):
        for value in ('array_fill(0,array[129])','array_fill(-1,array[130])','array_fill(100,array[130])','array_fill(null::smallint,array[130])'):
            with self.assertRaises(psycopg.Error):self.db.execute('update profiles set forge_levels='+value+' where id=%s',(self.one,))
    def test_concurrent_buy_retry(self):
        outcomes=self.race(lambda db,i:self.action('buy',{'quantity':7},key='concurrent-buy',db=db));self.assertEqual(outcomes,['ok']*8);self.assertEqual(self.state()[:2],[10007,999439])
    def test_concurrent_same_rank_spends_once(self):
        outcomes=self.race(lambda db,i:self.forge(key='rank-'+str(i),db=db));self.assertEqual(outcomes.count('ok'),1);self.assertEqual(self.state()[0],9996)
    def test_durable_key_survives_account_deletion(self):
        self.action('buy',{'quantity':1},key='durable');self.db.execute('delete from auth.users where id=%s',(self.one,))
        with self.assertRaises(psycopg.Error):self.action('buy',{'quantity':1},key='durable',uid=self.two)
    def test_two_team_picks_and_retry(self):
        match,_=self.match();before=self.state();a=self.claim(match,0,0);b=self.claim(match,1,1)
        self.assertEqual(a,self.claim(match,0,0));self.assertEqual(b,self.claim(match,1,1))
        self.assertEqual(self.val('select count(*) from gunny780_card_rewards where match_id=%s',(match,)),2)
        self.assertEqual(self.state()[0],before[0]+a['stones']+b['stones'])
    def test_third_pick_and_missing_first_rejected(self):
        match,_=self.match()
        for pick in (1,2,-1):
            with self.assertRaises(psycopg.Error):self.claim(match,pick,0)
    def test_card_conflicts_and_payload_change(self):
        match,_=self.match();self.claim(match,0,0)
        with self.assertRaises(psycopg.Error):self.claim(match,0,1)
        with self.assertRaises(psycopg.Error):self.claim(match,0,0,uid=self.two)
    def test_concurrent_claim_retry_once(self):
        match,_=self.match();outcomes=self.race(lambda db,i:self.claim(match,0,2,db=db));self.assertEqual(outcomes,['ok']*8);self.assertEqual(self.val('select count(*) from gunny780_card_rewards where match_id=%s',(match,)),1)
    def test_loser_without_damage_and_solo_mode_cannot_claim(self):
        match,_=self.match(winner=1,damage=0)
        with self.assertRaises(psycopg.Error):self.claim(match,0,0)
        match,players=self.match();self.db.execute('update match_results set room_kind=0 where id=%s',(match,))
        with self.assertRaises(psycopg.Error):self.claim(match,0,0)
    def test_dungeon_only_stones_in_range(self):
        match,_=self.match(kind=3)
        for pick in (0,1):
            reward=self.claim(match,pick,pick);self.assertEqual(reward['coins'],0);self.assertTrue(10<=reward['stones']<=100)
    def test_dungeon_unlock_and_no_coin_reward(self):
        before=self.val('select coins from profiles where id=%s',(self.one,));match,players=self.match(kind=3)
        self.assertTrue(self.val('select dungeon_normal_clear from profiles where id=%s',(self.one,)));self.assertEqual(before,self.val('select coins from profiles where id=%s',(self.one,)))
        self.db.execute('select gunny_finish_dungeon780(%s,0,%s::jsonb,0)',(match,json.dumps(players)))
        self.assertEqual(self.val('select wins from profiles where id=%s',(self.one,)),20)
    def test_dungeon_level_and_hard_gate(self):
        players=[dict(id=str(self.one),team=0,damage=1)]
        for level,hard in ((5249,0),(10000,1)):
            self.db.execute('update profiles set experience=%s where id=%s',(level,self.one))
            with self.assertRaises(psycopg.Error):self.db.execute('select gunny_finish_dungeon780(%s,0,%s::jsonb,%s)',(uuid.uuid4(),json.dumps(players),hard))
    def test_dungeon_failure_no_unlock_or_cards(self):
        match,_=self.match(kind=3,winner=1);self.assertFalse(self.val('select dungeon_normal_clear from profiles where id=%s',(self.one,)))
        with self.assertRaises(psycopg.Error):self.claim(match,0,0)
    def test_hard_success_after_normal_clear(self):
        self.match(kind=3);players=[dict(id=str(self.one),team=0,damage=1)];self.db.execute('select gunny_finish_dungeon780(%s,0,%s::jsonb,1)',(uuid.uuid4(),json.dumps(players)))
    def test_private_rpc_and_tables(self):
        for role in ('anon','authenticated'):
            self.db.execute('set role '+role)
            try:
                with self.assertRaises(psycopg.Error):self.action('buy',{'quantity':1})
                with self.assertRaises(psycopg.Error):self.val('select count(*) from gunny780_requests')
                with self.assertRaises(psycopg.Error):self.db.execute('update profiles set stones=999999')
            finally:self.db.execute('reset role')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--port',type=int,required=True);args=parser.parse_args();PORT=args.port
    unittest.main(argv=['expansion780_review'],verbosity=2)
