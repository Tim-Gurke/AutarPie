const {test}=require('node:test'),assert=require('node:assert/strict');
const Watchdog=require('../app/src/main/assets/freshness-watchdog.js');
const Bridge=require('../app/src/main/assets/read-bridge.js');
function fixture(){const calls=[],watch=new Watchdog(ids=>calls.push(ids));return {calls,watch};}

test('a continuously gray field retries after 45 seconds and then at most once per minute',()=>{
  const {calls,watch}=fixture();watch.check(['solar'],'plant',1000);watch.check(['solar'],'plant',45999);assert.deepEqual(calls,[]);
  watch.check(['solar'],'plant',46000);assert.deepEqual(calls,[['solar']]);
  for(let now=47000;now<106000;now+=1000)watch.check(['solar'],'plant',now);
  assert.equal(calls.length,1);watch.check(['solar'],'plant',106000);assert.equal(calls.length,2);
});
test('only overdue gray fields retry; current and newly gray fields keep their own state',()=>{
  const {calls,watch}=fixture();watch.check(['solar','battery'],'plant',1000);watch.check(['solar','weather'],'plant',30000);watch.check(['solar','weather'],'plant',46000);
  assert.deepEqual(calls,[['solar']]);watch.check(['weather'],'plant',75000);assert.deepEqual(calls,[['solar'],['weather']]);
});
test('a recovered field must become continuously gray again before another retry',()=>{
  const {calls,watch}=fixture();watch.check(['solar'],'plant',1000);watch.check([],'plant',40000);watch.check(['solar'],'plant',46000);watch.check(['solar'],'plant',90000);assert.equal(calls.length,0);watch.check(['solar'],'plant',91000);assert.equal(calls.length,1);
});
test('hidden fields and a paused display cannot accumulate retry deadlines',()=>{
  const {calls,watch}=fixture();watch.check(['solar'],'plant',1000);watch.check([],'plant',20000);watch.check(['solar'],'plant',46000);assert.equal(calls.length,0);
  watch.reset();watch.check(['solar'],'plant',3600000);assert.equal(calls.length,0);watch.check(['solar'],'plant',3645000);assert.equal(calls.length,1);
});
test('switching plant or source database discards the previous retry state',()=>{
  const {calls,watch}=fixture();watch.check(['solar'],'plant-one:epoch-one',1000);watch.check(['solar'],'plant-one:epoch-two',46000);watch.check(['solar'],'plant-two:epoch-one',91000);assert.equal(calls.length,0);
});
test('clock rollback restarts the delay and duplicated tile ids trigger only one retry',()=>{
  const {calls,watch}=fixture();watch.check(['solar','solar'],'plant',100000);watch.check(['solar','solar'],'plant',1000);watch.check(['solar','solar'],'plant',46000);assert.deepEqual(calls,[['solar']]);
});
test('watchdog retries preserve a gray curve until an actual successful response arrives',()=>{
  let now=1000;const requests=[],args=[0,100000,'power','all'],query={method:'history',args,scope:'solar'};
  const bridge=new Bridge({requestHistory:json=>requests.push(JSON.parse(json))},()=>{}),watch=new Watchdog(()=>bridge.retry([query]));
  bridge.read(query.method,args,query.scope);bridge.result({id:requests.at(-1).id,data:{points:[{v:42}],stale:true}});
  watch.check(['solar'],'plant',now);now+=45000;watch.check(['solar'],'plant',now);
  const old=JSON.parse(bridge.read(query.method,args,query.scope));assert.equal(old.points[0].v,42);assert.equal(old.stale,true);assert.equal(old.pending,true);
  bridge.result({id:requests.at(-1).id,data:{points:[{v:43}]}});const fresh=JSON.parse(bridge.read(query.method,args,query.scope));assert.equal(fresh.points[0].v,43);assert.equal(fresh.stale,undefined);assert.equal(fresh.pending,undefined);
});
