const {test}=require('node:test'),assert=require('node:assert/strict');
const Control=require('../app/src/main/assets/control-bridge');
test('remote settings stay pending until an authenticated native result, and duplicate taps do not dispatch twice',async()=>{
 const requests=[],events=[];let local=0;const b=new Control({isLanViewer:()=>true,controlRevision:()=>4,requestPlantCommand:q=>requests.push(JSON.parse(q)),saveEquipment:()=>local++,settings:()=>''},s=>events.push(s));
 const promise=b.bridge.saveEquipment('{}');assert.equal(requests.length,1);assert.equal(local,0);let resolved=false;promise.then(()=>resolved=true);await Promise.resolve();assert.equal(resolved,false);
 assert.match(await b.bridge.saveEquipment('{}'),/abwarten/);assert.equal(requests.length,1);
 b.result({id:999,ok:true});assert.equal(resolved,false);b.result({id:requests[0].id,ok:true});assert.equal(await promise,'');assert.match(events.at(-1),/bestätigt/);
});
test('remote failure remains an error and presentation settings stay local',async()=>{
 const requests=[],local=[];const b=new Control({isLanViewer:()=>true,controlRevision:()=>8,requestPlantCommand:q=>requests.push(JSON.parse(q)),settings:q=>{local.push(q);return '';}},()=>{});
 assert.equal(b.bridge.settings('{"theme":"light"}'),'');assert.equal(requests.length,0);assert.equal(local.length,1);
 const pending=b.bridge.settings('{"capacity":100}');b.result({id:requests[0].id,error:'Hauptgerät nicht erreichbar'});assert.equal(await pending,'Hauptgerät nicht erreichbar');assert.equal(local.length,1);
});
test('standalone methods preserve their synchronous native contract',()=>{
 const b=new Control({isLanViewer:()=>false,saveSolarSetup:q=>'validated:'+q},()=>{});assert.equal(b.bridge.saveSolarSetup('{}'),'validated:{}');
});

test('charger discovery, pairing, profile selection and addressed commands use the shared confirmation bridge',async()=>{
 for(const action of ['searchChargers','pairCharger','selectCharger','removeCharger','chargerCommandFor']){
  const requests=[],raw={isLanViewer:()=>true,controlRevision:()=>7,requestPlantCommand:json=>requests.push(JSON.parse(json)),[action]:()=>{throw Error('Local viewer action must not execute');}};
  const bridge=new Control(raw,()=>{},()=>{},()=>7),pending=bridge.bridge[action]('00:00:00:00:00:11','stop');
  assert.equal(requests[0].action,action);assert.equal(requests[0].args[0],'00:00:00:00:00:11');bridge.result({id:requests[0].id,ok:true,revision:8});assert.equal(await pending,'');
 }
});
test('a confirmed search advances its open setup form to the returned revision without accepting later unseen edits',async()=>{
 global.document={getElementById:id=>id==='modal'?{hidden:false}:null,body:{classList:{toggle(){}}},querySelectorAll:()=>[]};
 const requests=[],raw={isLanViewer:()=>true,controlRevision:()=>7,requestPlantCommand:json=>requests.push(JSON.parse(json)),searchChargers(){},pairCharger(){}};
 const bridge=new Control(raw,()=>{},()=>{},()=>7);bridge.capture();const search=bridge.bridge.searchChargers();bridge.result({id:requests[0].id,ok:true,revision:8});await search;
 const pair=bridge.bridge.pairCharger('{}');assert.equal(requests[1].revision,8);bridge.result({id:requests[1].id,error:'inzwischen geändert'});assert.equal(await pair,'inzwischen geändert');delete global.document;
});
