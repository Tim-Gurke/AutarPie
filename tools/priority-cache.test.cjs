const {test}=require('node:test'),assert=require('node:assert/strict');
const Bridge=require('../app/src/main/assets/read-bridge.js'),R=require('../app/src/main/assets/roller-core.js');
test('opening a detail cancels the previous queue and submits only its dependencies',()=>{
 const requests=[],focus=[],b=new Bridge({requestHistory:q=>requests.push(JSON.parse(q)),setReadFocus:q=>focus.push(q)},()=>{});
 b.setFocus('dashboard','overview');b.read('history',[1,1000,'power','all'],'solar');b.read('history',[1,1000,'voltage','all'],'battery');
 b.setFocus('detail','solar-today');b.read('history',[1,2000,'power','all'],JSON.stringify(['detail','solar']));
 b.read('history',[1,2000,'voltage','all'],'battery');b.result({id:requests[0].id,data:{points:[{v:900}]}});
 assert.equal(requests.length,2);assert.equal(b.queue.size,0);assert.equal(b.inflight.id,requests[1].id);
 b.result({id:requests[1].id,data:{points:[{v:42}]}});assert.equal(requests.length,2);assert.equal(focus.length,2);b.setActive(false);
});
test('energy detail scopes nested inside an energy family retain priority',()=>{
 const requests=[],b=new Bridge({requestHistory:q=>requests.push(JSON.parse(q))},()=>{});b.setFocus('detail','flow');
 b.read('energyHistoryWindow',[1,2000,'all',1,2000],JSON.stringify([JSON.stringify(['detail','flow']),'selected-range']));
 assert.equal(requests.length,1);b.setActive(false);
});
test('completed results survive a new bridge while a changed plant cannot read them',()=>{
 const values=new Map();global.localStorage={getItem:k=>values.get(k),setItem:(k,v)=>values.set(k,v)};
 const args=[1,1000,'power','all'];let plant='first',requests=[];const native={isLanViewer:()=>true,lanState:()=>JSON.stringify({id:plant,sourceEpoch:'source-v1'}),requestHistory:q=>requests.push(JSON.parse(q))};
 const a=new Bridge(native,()=>{});a.read('history',args,'solar');a.result({id:requests[0].id,data:{points:[{v:17}],summary:{last:17}}});a.setActive(false);
 const b=new Bridge(native,()=>{}),warm=JSON.parse(b.read('history',args,'solar'));assert.equal(warm.points[0].v,17);assert.equal(warm.stale,true);assert.equal(warm.pending,true);b.setActive(false);
 plant='other';const c=new Bridge(native,()=>{}),foreign=JSON.parse(c.read('history',args,'solar'));assert.equal(foreign.points.length,0);c.setActive(false);delete global.localStorage;
});
test('display tolerance preserves BMS values without extending control or planning freshness',()=>{
 const now=100000,state={fresh:false,live:{},lastKnown:{at:85000,soc:70},profile:{displayFreshSeconds:20}};
 assert.equal(R.displayState(state,now).fresh,true);assert.equal(R.readingState(state,now).fresh,false);
 assert.equal(R.displayState(state,106000).fresh,false);assert.equal(R.displayState({...state,lastKnown:{at:110000}},now).fresh,false);
 assert.equal(R.displayState({...state,lastKnown:{at:99000,soc:30}},now).live.soc,30);
});
