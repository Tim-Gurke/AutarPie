const {test}=require('node:test'),assert=require('node:assert/strict'),C=require('../app/src/main/assets/solar-core.js'),S=require('../app/src/main/assets/supplies-core.js'),L=require('../app/src/main/assets/learned-model.js');
const H=3600000,D=24*H,now=Date.parse('2026-11-24T06:00:00Z'),start=Date.parse('2026-11-24T00:00:00Z');
const pause=ms=>new Promise(r=>setTimeout(r,ms));
function fixture(){
 const equipment={version:1,groups:[{id:'roof',label:'Example',count:2,watts:100,tilt:30,placement:'fixed',measurement:'solar',controller:'MPPT'}]},plan=C.normalize({groups:{roof:{azimuth:180,shade:[0,0,0]}}},equipment),weather={place:{latitude:52,longitude:13},timezone:'UTC',days:[],windows:[],hours:[],solar:[{geometry:'30:0',values:[]}]},observations=[];
 for(let i=0;i<14;i++){const t=start+i*D,day=new Date(t).toISOString().slice(0,10);weather.days.push({day,hours:7,sunrise:t+7*H,sunset:t+17*H});for(let slot=0;slot<3;slot++)weather.windows.push({day,slot,from:t+[0,11,16][slot]*H,to:t+[11,16,24][slot]*H});for(let j=1;j<=24;j++){weather.hours.push({t:t+j*H});weather.solar[0].values.push({t:t+j*H,v:j>7&&j<=17?300:0});}}
 for(let i=1;i<=30;i++){const t=start-i*D,day=new Date(t).toISOString().slice(0,10);for(let slot=0;slot<3;slot++){const from=t+[0,11,16][slot]*H,to=t+[11,16,24][slot]*H;observations.push({day,slot,from,to,mobileFree:true,consumption:(to-from)/H*10,coverage:1,solarCoverage:1,bulkCoverage:1,solar:100});}}
 return {equipment,plan,weather,knowledge:{source:'neutral-source',updated:now-60000,observations,forecasts:[],presence:{state:'home',history:[],plans:[]}}};
}
async function compiled(f){const model=await new Promise(done=>L.solarBuilder(C,f.knowledge,f.weather,f.equipment,f.plan,now)(()=>false,done));return {...model,version:1};}
const forbidden=()=>{throw Error('historical learning was accessed by a forecast');};
test('compiled solar profiles preserve the existing 14-day forecast exactly at their learning instant',async()=>{
 const f=fixture(),model=await compiled(f),original=C.forecast(f.weather,f.equipment,f.plan,f.knowledge,now);
 assert.deepEqual(C.forecast(f.weather,f.equipment,f.plan,{...f.knowledge,learnedModel:model,requireLearnedModel:true},now),original);
 assert.equal(Object.keys(model.profiles).length,32);
});
test('new SOC/weather and presence forecasts use persisted factors with no observation or comparison reads',async()=>{
 const f=fixture(),model=JSON.parse(JSON.stringify(await compiled(f))),knowledge={source:f.knowledge.source,learnedModel:model,requireLearnedModel:true,presence:f.knowledge.presence};
 Object.defineProperties(knowledge,{observations:{get:forbidden},forecasts:{get:forbidden}});
 const first=C.forecast(f.weather,f.equipment,f.plan,knowledge,now),weather=JSON.parse(JSON.stringify(f.weather));weather.solar[0].values.forEach(r=>r.v*=.5);
 const second=C.forecast(weather,f.equipment,f.plan,{...knowledge,batteryVoltage:13.4},now+60000);assert.equal(second[0].total,first[0].total*.5);assert.deepEqual(second[0].usage.parts,first[0].usage.parts);
 const reserve=C.batteryForecast(second,weather,{soc:65,voltage:13.4,capacity:100,t:now+60000},now+60000);assert.ok(reserve.length);
});
test('incompatible or absent compiled factors leave consumption unknown without launching learning',async()=>{
 const f=fixture(),model=await compiled(f),knowledge={source:'different-source',learnedModel:model,requireLearnedModel:true};Object.defineProperties(knowledge,{observations:{get:forbidden},forecasts:{get:forbidden}});
 const days=C.forecast(f.weather,f.equipment,f.plan,knowledge,now);assert.equal(days[0].usage.wh,null);
});
test('source-less preview knowledge and manual consumption remain usable without measurement history',async()=>{
 const f=fixture();f.knowledge.source='';assert.ok(L.solarRevision(f.knowledge,'2026-11-24'));const known=await compiled(f);assert.ok(C.forecast(f.weather,f.equipment,f.plan,{...f.knowledge,learnedModel:known,requireLearnedModel:true},now)[0].usage.wh>0);
 f.plan.usageMode='manual';f.plan.manual=[240,240,240,240];f.knowledge.observations=[];const manual=await compiled(f),day=C.forecast(f.weather,f.equipment,f.plan,{...f.knowledge,learnedModel:manual,requireLearnedModel:true},now)[0];assert.equal(day.usage.wh,240);assert.equal(day.usage.manual,true);
});
test('saved models survive a new store and an unchanged revision never invokes its builder',async()=>{
 const data=new Map(),storage={getItem:k=>data.get(k),setItem:(k,v)=>data.set(k,v)},store=new L.Store('solar',null,()=>{},storage);
 store.select('plant-and-config');store.update('input-v1',(_,done)=>done({at:Date.now(),profiles:{},source:'neutral'}));await pause(50);
 const again=new L.Store('solar',null,()=>{},storage);assert.ok(again.select('plant-and-config'));again.update('input-v1',forbidden);await pause(30);assert.equal(again.value.revision,'input-v1');assert.equal(again.select('other-plant'),null);
});
test('native acknowledgement precedes installing a model and late saves cannot cross a source change',async()=>{
 const sent=[],store=new L.Store('supplies',{saveLearnedModel:json=>sent.push(JSON.parse(json))},()=>{},{getItem(){},setItem(){}});
 store.select('one');store.update('r1',(_,done)=>done({at:Date.now(),profiles:{}}));await pause(30);assert.equal(store.value,null);
 const request=sent[0];store.receive({...request,action:'save',ok:true});assert.equal(store.value.revision,'r1');
 store.update('r2',(_,done)=>done({at:Date.now(),profiles:{}}));await pause(30);const late=sent[1];store.select('two');store.receive({...late,action:'save',ok:true});assert.equal(store.value,null);
});
function supply(){const events=[],history=[],forecast=[];for(let i=0;i<6;i++){const at=start-(25-i*5)*D;events.push({id:'supply-fixture-'+i,kind:'gas',type:'swap',at,amount:10,site:{key:'example-site'}});if(i<5)history.push({site:'example-site',source:'archive',from:at,to:at+5*D,heat:3+i*3,windHeat:2,issuedAt:now});}
 for(let i=0;i<16;i++)forecast.push({site:'example-site',source:'forecast',from:start+i*D,to:start+(i+1)*D,heat:5,windHeat:2});
 return {version:1,events,weather:{history,forecast,forecastUpdated:now,siteKey:'example-site'}};
}
test('gas coefficients and water statistics round-trip and retain the existing result',()=>{
 const input=supply(),model=JSON.parse(JSON.stringify(S.learn(input,now)));assert.deepEqual(S.build(input,now,model,{requireLearned:true}),S.build(input,now));
});
test('a new gas weather forecast changes need while the stored learning identity and coefficients stay fixed',()=>{
 const input=supply(),model=S.learn(input,now),before=JSON.stringify(model),first=S.build(input,now,model,{requireLearned:true});
 input.weather.forecast=input.weather.forecast.map(r=>({...r,heat:15,windHeat:4}));input.weather.forecastUpdated=now+1000;
 assert.equal(S.learningKey(input,now),model.inputKey);const second=S.build(input,now,model,{requireLearned:true});assert.notEqual(second.gas.needs[3],first.gas.needs[3]);assert.equal(second.gas.modelPending,false);assert.equal(JSON.stringify(model),before);
});
test('retrieving the same historical weather again does not change gas learning factors',()=>{
 const input=supply(),key=S.learningKey(input,now);input.weather.history=input.weather.history.map(r=>({...r,issuedAt:now+3600000}));input.weather.updated=now+3600000;assert.equal(S.learningKey(input,now),key);
});
test('a corrected historical gas input withdraws incompatible coefficients until background learning finishes',()=>{
 const input=supply(),model=S.learn(input,now);input.events[1].amount=7;
 const next=S.build(input,now,model,{requireLearned:true});assert.equal(next.gas.modelPending,true);assert.equal(next.gas.daily,null);
});
