const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {PGlite} = require(path.resolve(process.argv[2]));

(async () => {
    const db = new PGlite();
    await db.exec(`
        create role anon; create role authenticated; create role service_role;
        create table profiles(id uuid primary key, display_name text, stones integer default 0,
            coins integer default 0, shopping_money integer default 0);
        create table gunny_admin_audit(action text,target text,detail jsonb);
        insert into profiles(id,display_name) values('11111111-1111-4111-8111-111111111111','Gunner');
    `);
    await db.exec(fs.readFileSync('supabase/migrations/202611160001_support.sql', 'utf8'));
    const call = async (op, data = {}) => (await db.query(
        'select gunny_support_admin($1,$2::jsonb) as result', [op, JSON.stringify(data)]
    )).rows[0].result;
    const payload = {id: '11111111-1111-4111-8111-111111111111',
        request_key: '22222222-2222-4222-8222-222222222222', amount: 50000,
        comment: 'Cảm ơn bạn', stones: 10, coins: 20, money: 30};
    await Promise.all([call('support-add', payload), call('support-add', payload)]);
    let wallet = (await db.query('select stones,coins,shopping_money from profiles')).rows[0];
    assert.deepEqual(wallet, {stones: 10, coins: 20, shopping_money: 30});
    assert.equal((await call('support-list')).length, 1);
    await assert.rejects(call('support-add', {...payload, coins: 21}), /Request key payload mismatch/);
    const publicData = await call('support-public');
    assert.deepEqual(publicData.entries, [{display_name: 'Gunner', amount: 50000, comment: 'Cảm ơn bạn'}]);
    await call('support-comment', {request_key: payload.request_key, comment: 'Cảm ơn lần nữa'});
    await call('support-add', payload);
    assert.equal((await call('support-public')).entries[0].comment, 'Cảm ơn lần nữa');
    const overflow = {...payload, request_key: '33333333-3333-4333-8333-333333333333', stones: 999999};
    await assert.rejects(call('support-add', overflow), /Gift exceeds wallet limit/);
    assert.equal((await call('support-list')).length, 1);
    wallet = (await db.query('select stones,coins,shopping_money from profiles')).rows[0];
    assert.deepEqual(wallet, {stones: 10, coins: 20, shopping_money: 30});
    await call('support-qr', {qr: 'test-base64', mime: 'image/jpeg'});
    assert.equal((await call('support-public')).qr, 'test-base64');
    await db.exec('set role anon');
    await assert.rejects(call('support-public'), /permission denied/);
    await db.exec('reset role; set role authenticated');
    await assert.rejects(db.query('select * from gunny_support_entries'), /permission denied/);
    await db.exec('reset role');
    await db.close();
    console.log('PASS support SQL: atomic gifts, retry deduplication, limits, private fields, QR and role permissions');
})().catch(error => { console.error(error); process.exit(1); });
