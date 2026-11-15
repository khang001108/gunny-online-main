const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {PGlite}=require(path.resolve(process.argv[2]));
(async()=>{
 const db=new PGlite();
 await db.exec(`create role anon;create role authenticated;create role service_role;
 create table profiles(id uuid primary key,public_id integer,display_name text);
 create table gunny_admin_audit(action text,target text,detail jsonb);
 insert into profiles values
 ('11111111-1111-4111-8111-111111111111',1001,'Gunner'),
 ('22222222-2222-4222-8222-222222222222',1002,' gunNER '),
 ('33333333-3333-4333-8333-333333333333',1003,'Trùng');`);
 const migration=fs.readFileSync('supabase/migrations/202611160002_unique_names.sql','utf8');
 await db.exec(migration);
 let rows=(await db.query('select * from profiles order by public_id')).rows;
 assert.equal(rows[0].display_name,'Gunner');assert.equal(rows[1].display_name,'gunNER#1002');
 assert.equal((await db.query('select count(*)::int as n from gunny_admin_audit')).rows[0].n,1);
 const available=async(name,except=null)=>(await db.query('select gunny_name_available($1,$2) as ok',[name,except])).rows[0].ok;
 assert.equal(await available(' GUNNER '),false);
 assert.equal(await available('gunner',rows[0].id),true);
 assert.equal(await available('Tru\u0300ng'),false);
 const concurrent=await Promise.allSettled([
  db.query("insert into profiles values('44444444-4444-4444-8444-444444444444',1004,'NewName')"),
  db.query("insert into profiles values('55555555-5555-4555-8555-555555555555',1005,'newname')")
 ]);
 assert.equal(concurrent.filter(x=>x.status==='fulfilled').length,1);
 await assert.rejects(db.query("update profiles set display_name='GUNNER' where public_id=1003"),/gunny_profiles_name_unique/);
 await db.exec(migration);
 assert.equal((await db.query('select count(*)::int as n from gunny_admin_audit')).rows[0].n,1);
 await db.exec('set role anon');await assert.rejects(available('secret'),/permission denied/);await db.exec('reset role');
 await db.close();console.log('PASS SQL unique names: case folding, Unicode, existing duplicates, concurrent creation, rename, permissions and repeat migration');
})().catch(error=>{console.error(error);process.exit(1)});
