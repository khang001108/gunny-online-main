const fs=require('node:fs'),vm=require('node:vm'),crypto=require('node:crypto');
const line=fs.readFileSync('public/admin/app.js','utf8').split('\n').find(s=>s.startsWith('function requestId()'));
const context={crypto:{getRandomValues:b=>crypto.randomFillSync(b)}};vm.createContext(context);vm.runInContext(line,context);
const seen=new Set();for(let i=0;i<1000;i++){const id=vm.runInContext('requestId()',context);if(!/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/.test(id)||seen.has(id))throw Error('Invalid request ID');seen.add(id);}
console.log('PASS 1000 unique UUIDs with LAN HTTP crypto fallback');
