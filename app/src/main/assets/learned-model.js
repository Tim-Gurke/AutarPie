(function(root,factory){const api=factory(root);if(typeof module==='object'&&module.exports)module.exports=api;else root.WattLearnedModel=api;})(typeof window==='object'?window:globalThis,function(root){
 'use strict';
 const stores=new Set(),copy=x=>JSON.parse(JSON.stringify(x));let nextId=0;
 const finite=x=>typeof x==='number'&&Number.isFinite(x);
 function hash(text){let a=2166136261,b=3339675911;for(let i=0;i<text.length;i++){a=Math.imul(a^text.charCodeAt(i),16777619);b=Math.imul(b^text.charCodeAt(i),2246822519);}return (a>>>0).toString(16)+':'+(b>>>0).toString(16)+':'+text.length;}
 function owner(snapshot){return JSON.stringify([snapshot.lan?.id||'local',snapshot.lan?.sourceEpoch||snapshot.historyEpoch||'']);}
 function valid(value,key,now=Date.now()){return value?.version===1&&value.key===key&&finite(value.at)&&value.at>0&&value.at<=now+5000;}
 const previous=root.onLearnedModelData;
 root.onLearnedModelData=result=>{for(const store of stores)store.receive(result);previous?.(result);};
 class Store{
  constructor(kind,native,changed=()=>{},storage){this.kind=kind;this.native=native;this.changed=changed;this.storage=storage??root.localStorage;this.key=null;this.value=null;this.serial=0;stores.add(this);}
  select(key){
   if(this.key===key)return this.value;this.key=key;this.value=null;this.error='';this.serial++;this.requested='';this.pending=null;
   try{const value=JSON.parse(this.storage?.getItem('autarpie-learned-'+this.kind)||'null');if(valid(value,key))this.value=value;}catch(_){}
   this.readId=++nextId;if(this.native?.requestLearnedModel)this.native.requestLearnedModel(JSON.stringify({id:this.readId,kind:this.kind,key}));
   return this.value;
  }
  receive(result){
   if(result.kind!==this.kind||result.key!==this.key)return;
   if(result.action==='read'&&result.id===this.readId){if(valid(result.data,this.key)&&(!this.value||result.data.at>this.value.at)){this.value=result.data;this.retain();this.changed();}return;}
   if(result.action!=='save'||result.id!==this.pending?.id)return;
   const pending=this.pending;this.pending=null;if(!result.ok){this.error=result.error||'Lernmodell konnte nicht gespeichert werden.';this.retryAt=Date.now()+30000;this.requested='';this.changed();return;}
   if(pending.serial!==this.serial)return;this.value=pending.value;this.error='';this.retain();this.changed();
  }
  retain(){try{this.storage?.setItem('autarpie-learned-'+this.kind,JSON.stringify(this.value));}catch(_){} }
  update(revision,build){
   if(!revision||Date.now()<(this.retryAt||0)||this.value?.revision===revision||this.requested===revision)return;
   this.requested=revision;const serial=++this.serial,key=this.key,cancelled=()=>serial!==this.serial||key!==this.key;
   setTimeout(()=>{if(cancelled())return;try{build(cancelled,value=>{
    if(cancelled())return;if(value?.error){this.error=value.error;this.retryAt=Date.now()+30000;this.requested='';this.changed();return;}value={...value,version:1,key,revision};if(!valid(value,key))throw Error('Ungültiges Lernmodell.');
    if(this.native?.saveLearnedModel){const id=++nextId;this.pending={id,serial,value};this.native.saveLearnedModel(JSON.stringify({id,kind:this.kind,key,data:value}));}
    else{this.value=value;this.retain();this.changed();}
   });}catch(e){if(!cancelled()){this.error=e.message;this.retryAt=Date.now()+30000;this.requested='';this.changed();}}},20);
  }
 }
 function solarKey(C,snapshot,equipment,plan){const place=snapshot.settings?.weatherPlace||snapshot.weather?.place||{},devices=(snapshot.devices||[]).filter(d=>d.kind==='solar'||d.kind==='shunt').map(d=>[d.kind,d.mac]);return JSON.stringify([1,owner(snapshot),devices,C.setupKey(equipment,plan,place,'model'),plan.usageMode,plan.manual]);}
 function solarRevision(knowledge,today,contextKey=''){return knowledge.source&&finite(knowledge.updated)&&knowledge.updated>0?JSON.stringify([knowledge.source,knowledge.factorInputToken||hash(JSON.stringify([knowledge.observations,knowledge.forecasts])),today,contextKey]):'';}
 function solarBuilder(C,knowledge,weather,equipment,plan,now){
  return (cancelled,done)=>{
   const today=C.stamp(now,weather.timezone)?.day;if(!today)return;
   const key=C.calibrationKey(equipment,plan,weather.place,knowledge.source),system=C.systemKey(equipment,knowledge.source),model={at:now,source:knowledge.source,inputAt:knowledge.updated,calibrationKey:key,systemKey:system,profiles:{}};
   const tasks=[()=>{model.calibration=C.calibration(knowledge.observations,knowledge.forecasts,key,system,now);}];
   for(let i=0;i<32;i++){const day=new Date(Date.parse(today+'T12:00:00Z')+i*C.DAY).toISOString().slice(0,10);model.profiles[day]={};for(const mode of ['home','away'])tasks.push(()=>{model.profiles[day][mode]=C.consumption(plan,knowledge.observations,day,now,mode);});}
   let index=0;const step=()=>{if(cancelled())return;try{tasks[index++]();if(index<tasks.length)setTimeout(step,0);else done(model);}catch(e){done({error:e.message});}};step();
  };
 }
 return {Store,hash,owner,valid,solarKey,solarRevision,solarBuilder,copy};
});
