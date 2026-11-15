const {test}=require('node:test'),assert=require('node:assert/strict'),C=require('../app/src/main/assets/roller-core.js');
test('calibration includes idle exactly once',()=>{assert.equal(C.input(96,C.defaults),138);assert.equal(C.input(0,C.defaults),0);});
test('planning uses Wh across battery voltages',()=>{const r=C.plan({...C.defaults,capacityAh:26,nominalVoltage:48.1,targetPercent:90},{soc:70});assert.ok(Math.abs(r.missing-250.12)<1e-8);assert.ok(Math.abs(r.draw-r.missing*138/96)<1e-8);});
test('no BMS means unknown demand',()=>{const r=C.plan(C.defaults,{});assert.equal(r.draw,null);assert.equal(r.available,null);});
test('ride reserve, absence and infeasible trip',()=>{const p={...C.defaults,rideKm:100,whPerKm:15,departure:1000,returnAt:9000};const r=C.plan(p,{soc:70},2000);assert.equal(r.away,true);assert.equal(r.impossible,true);assert.equal(r.rideWh,1800);});
test('planned charging cannot consume energy twice when target reached',()=>{assert.equal(C.plan(C.defaults,{soc:95}).draw,0);});
test('departure deadline considers charge duration',()=>{assert.equal(C.plan({...C.defaults,departure:10000},{soc:20},1000).late,true);});
test('forecast load is remaining demand and disappears without live BMS or on a ride',()=>{const s={fresh:true,profile:{...C.defaults,mac:'AA:BB:CC:DD:EE:FF',planCharge:true},live:{soc:70},session:{}};let loads=C.loads(s,1000);assert.equal(loads.length,1);assert.equal(loads[0].watts,138);assert.ok(Math.abs((loads[0].to-loads[0].from)*138/3600000-C.plan(s.profile,s.live).draw)<1e-8);assert.deepEqual(C.loads({...s,fresh:false},1000),[]);assert.deepEqual(C.loads({...s,profile:{...s.profile,departure:500,returnAt:9000}},1000),[]);});

test('stored energy prefers the BMS remaining capacity at nominal voltage',()=>{
 assert.equal(C.storedEnergy(C.defaults,{remainingAh:18,fullAh:26,soc:10}),18*48.1);
 assert.equal(C.storedEnergy(C.defaults,{remainingAh:0,fullAh:26,soc:0}),0);
});
test('stored energy falls back to valid SOC and keeps missing or impossible values unknown',()=>{
 assert.equal(C.storedEnergy(C.defaults,{soc:50}),26*48.1/2);
 for(const live of [{},{soc:NaN},{soc:-1},{soc:101},{remainingAh:15,fullAh:0}])assert.equal(C.storedEnergy(C.defaults,live),null);
 assert.equal(C.storedEnergy(C.defaults,{remainingAh:30,fullAh:26,soc:50}),26*48.1/2);
});
test('six roller fields retain valid choices and migrate missing or invalid slots independently',()=>{
 const values=['voltage','current','temperature:1',null,'__proto__','temperature:00'],copy=structuredClone(values);
 assert.deepEqual(C.normalizeDisplay(values),['voltage','current','temperature:1','charged','input','temperature']);assert.deepEqual(values,copy);
 assert.deepEqual(C.normalizeDisplay(),C.displayDefaults);assert.deepEqual(C.normalizeDisplay(['soc','soc']).slice(0,2),['soc','soc']);
});
test('selected roller readings preserve units, signed current, measured zero and missing values',()=>{
 const live={voltage:50.78,current:-1.89,soc:0,remainingAh:0,fullAh:26};
 for(const [key,value,unit] of [['voltage',50.78,'V'],['current',-1.89,'A'],['soc',0,'%'],['remainingAh',0,'Ah'],['fullAh',26,'Ah']]){const r=C.displayReading(key,C.defaults,live);assert.equal(r.value,value);assert.equal(r.unit,unit);}
 assert.equal(C.displayReading('current',C.defaults,{current:null}).value,null);assert.equal(C.displayReading('charged',C.defaults,{}, {batteryWh:12}).value,12);
});
test('roller temperature maximum and explicit sensors do not substitute a missing channel',()=>{
 const live={temperatures:[-5.5,0,22.3,null,'30']};assert.equal(C.displayReading('temperature',C.defaults,live).value,22.3);
 assert.equal(C.displayReading('temperature:0',C.defaults,live).value,-5.5);assert.equal(C.displayReading('temperature:1',C.defaults,live).value,0);
 assert.equal(C.displayReading('temperature:3',C.defaults,live).value,null);assert.equal(C.displayReading('temperature:1',C.defaults,{temperatures:[22.3]}).value,null);
});
test('sensor choices remain available before connection and retain a selected additional channel',()=>{
 const options=C.displayOptions({},['temperature:4']);assert.ok(options.some(([key])=>key==='temperature:0'));assert.ok(options.some(([key])=>key==='temperature:1'));assert.ok(options.some(([key])=>key==='temperature:4'));
 assert.equal(C.normalizeDisplay(['temperature:116'])[0],'stored');
});

test('BMS freshness expires from the measurement timestamp even while a cached snapshot still says fresh',()=>{
 const now=1700000000000,live={at:now-10001,soc:88,voltage:53},state={fresh:true,live,lastKnown:{at:now-60000,soc:91}};
 const old=C.readingState(state,now);assert.equal(old.fresh,false);assert.deepEqual(old.live,{});assert.equal(old.lastKnown.soc,88);assert.equal(old.lastKnown.at,live.at);assert.equal(state.fresh,true);assert.equal(state.live.soc,88);
});
test('returning with a lower current SOC replaces the visibly stale high SOC and its timestamp',()=>{
 const now=1700000000000,saved={at:now-3600000,soc:87};
 const parked=C.readingState({fresh:false,live:{},lastKnown:saved},now);assert.equal(parked.fresh,false);assert.equal(parked.lastKnown.soc,87);
 const returned=C.readingState({fresh:true,live:{at:now,soc:36},lastKnown:saved},now);assert.equal(returned.fresh,true);assert.equal(returned.live.soc,36);assert.equal(returned.lastKnown.soc,36);assert.equal(saved.soc,87);
 assert.equal(C.readingState(returned,now+10001).fresh,false);
});
test('missing and future BMS timestamps cannot gain freshness from opening the app or polling again',()=>{
 const now=1700000000000;for(const at of [undefined,0,NaN,now+1])assert.equal(C.readingState({fresh:true,live:{at,soc:80}},now).fresh,false);
 const current={fresh:true,live:{at:now-10000,soc:50}};assert.equal(C.readingState(current,now).fresh,true);assert.equal(C.readingState(current,now+1).fresh,false);
 const lost={fresh:false,live:{at:now,soc:50}};assert.equal(C.readingState(lost,now).fresh,false);
});
