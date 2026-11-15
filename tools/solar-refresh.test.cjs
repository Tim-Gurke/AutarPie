const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/solar-ui.js'),'utf8');

// Execute the productive constructor, async callback, savedSunset and scale methods.
// DOM entry points are adapters; no browser layout or native history query is simulated as proof.
function fixture(){
 const requests=[],elements=new Map(),context={window:{WattSolarCore:{},WattSpecialUses:class{}},document:{getElementById:id=>{if(!elements.has(id))elements.set(id,{setAttribute(){}});return elements.get(id);},addEventListener(){},querySelector(){return null;}},console};
 vm.createContext(context);vm.runInContext(source,context);
 const at=Date.parse('2026-11-15T15:40:00Z'),snapshot={lan:{id:'plant-one'},historyRevision:1,archiveBusy:false,devices:[{kind:'shunt',mac:'TEST-SHUNT'}]};let now=at+600000;
 const plan=new context.window.WattSolarPlan({getSnapshot:()=>snapshot,native:{requestSunsetSoc:(time,id)=>requests.push({time,id})}});
 plan.render=()=>{plan.days=[{reserve:{evening:{at}}}];plan.savedSunset(snapshot,now);};
 const point=()=>plan.days[0].reserve.evening;
 const reply=(value,error)=>context.window.onSolarSunsetSoc({id:requests.at(-1).id,point:value,error});
 return {plan,snapshot,requests,point,reply,at,receive:value=>context.window.onSolarSunsetSoc(value),render:()=>plan.render(),advance:ms=>{now+=ms;},scale:()=>plan.scale('battery','Akku',point().soc,String(point().soc),'Gespeicherter Stand')};
}

test('today keeps a measured sunset bar during a revision refresh and then applies its new value',()=>{
 const f=fixture();f.render();f.reply({soc:69,t:f.at});assert.equal(f.point().soc,69);
 f.advance(31000);f.snapshot.historyRevision++;f.render();assert.equal(f.requests.length,2);
 assert.equal(f.point().soc,69,'starting a background refresh must not erase the measured bar');assert.equal(f.point().pending,true);assert.equal(f.point().stale,true);assert.match(f.scale(),/width:69%/);assert.doesNotMatch(f.scale(),/solar-no-data/);
 f.reply({soc:67,t:f.at});assert.equal(f.point().soc,67);assert.equal(f.point().pending,false);assert.equal(f.point().stale,false);
});
test('zero percent remains a measured value while refreshing instead of becoming unknown',()=>{
 const f=fixture();f.render();f.reply({soc:0,t:f.at});f.advance(31000);f.snapshot.historyRevision++;f.render();assert.equal(f.point().soc,0);assert.equal(f.point().mode,'measured');assert.doesNotMatch(f.scale(),/solar-no-data/);
});
test('a transient history read error retains and labels the last measured sunset point',()=>{
 const f=fixture();f.render();f.reply({soc:42,t:f.at});f.advance(31000);f.snapshot.historyRevision++;f.render();f.reply(undefined,'History unavailable');assert.equal(f.point().soc,42);assert.equal(f.point().stale,true);assert.equal(f.point().readError,'History unavailable');
});
test('a successful empty reply withdraws a previous point instead of preserving a false measurement',()=>{
 const f=fixture();f.render();f.reply({soc:42,t:f.at});f.advance(31000);f.snapshot.historyRevision++;f.render();f.reply(null);assert.equal(f.point().soc,undefined);assert.match(f.scale(),/solar-no-data/);
});
test('a source change rejects the previous plant value and its late response',()=>{
 const f=fixture();f.render();f.reply({soc:42,t:f.at});f.advance(31000);f.snapshot.historyRevision++;f.render();const old=f.requests.at(-1).id;
 f.snapshot.lan.id='plant-two';f.render();assert.equal(f.point().soc,undefined);assert.notEqual(f.requests.at(-1).id,old);
 f.reply({soc:12,t:f.at});assert.equal(f.point().soc,12);f.receive({id:old,point:{soc:99,t:f.at}});assert.equal(f.point().soc,12);
});
test('a changed shunt does not inherit the previously measured sunset value',()=>{
 const f=fixture();f.render();f.reply({soc:42,t:f.at});f.snapshot.devices[0].mac='OTHER-TEST-SHUNT';f.render();assert.equal(f.point().soc,undefined);f.reply({soc:8,t:f.at});assert.equal(f.point().soc,8);
});
test('a first pending query and an invalid successful point remain unknown',()=>{
 const f=fixture();f.render();assert.equal(f.point().soc,undefined);f.reply({soc:42,t:f.at-16*60000});assert.equal(f.point().soc,undefined);
});
