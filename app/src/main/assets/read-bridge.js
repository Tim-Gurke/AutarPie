(function(root,factory){
  const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.WattReadBridge=api;
})(typeof globalThis==='object'?globalThis:this,function(){
  'use strict';
  const methods=new Set(['chargerJournal','rollerEnergy','loadHistory','history','dashboardHistory','energyHistory','energyHistoryWindow','dailyEnergy']);
  const missing=()=>({points:[],summary:{},pending:true});
  const pending=method=>method==='dailyEnergy'?{days:[],pending:true}:method.startsWith('energy')?{solar:missing(),consumption:missing(),balance:missing(),mobile:missing(),pending:true}:missing();
  return class {
    constructor(raw,ready,storage=globalThis.localStorage){
      this.raw=raw;this.ready=ready;this.cache=new Map();this.queue=new Map();this.nextId=0;this.generation=0;this.epoch=0;this.busy=false;this.active=true;
      this.storage=storage;this.focusSerial=0;
      this.bridge=new Proxy(raw,{get:(target,name)=>{const value=target[name];if(typeof value!=='function')return value;return methods.has(name)?(...args)=>this.read(name,args):(...args)=>target[name](...args);}});
    }
    source(){
      let source=this.raw.isLanViewer?.()?'viewer':'local';
      if(source==='viewer'&&this.raw.lanState)try{const lan=JSON.parse(this.raw.lanState());source+=':'+(lan.id||lan.name||'')+':'+(lan.sourceEpoch||'');}catch(_){}
      if(this.raw.historyIdentity)try{source+=':'+this.raw.historyIdentity();}catch(_){}
      if(this.owner!==undefined&&source!==this.owner)this.invalidate(true);
      if(this.owner!==source){this.owner=source;this.restore();}
    }
    restore(){
      try{const saved=JSON.parse(this.storage?.getItem('autarpie-derived-history-v1')||'{}');if(saved.owner!==this.owner||saved.version!==1||!Array.isArray(saved.entries))return;
        for(const entry of saved.entries.slice(-48))if(methods.has(entry.method)&&Array.isArray(entry.args)&&entry.data&&entry.at>0&&entry.at<=Date.now()+5000)this.remember(entry.key,{...entry,generation:-1,epoch:this.epoch});
      }catch(_){}
    }
    persist(){
      try{let entries=[...this.cache.values()].filter(e=>e.data).slice(-48).map(e=>({key:e.key,method:e.method,args:e.args,scope:e.scope,at:e.at,startedAt:e.startedAt,data:e.data})),value;
        do{value=JSON.stringify({version:1,owner:this.owner,entries});if(value.length<=1800000)break;entries.shift();}while(entries.length);
        this.storage?.setItem('autarpie-derived-history-v1',value);
      }catch(_){}
    }
    setFocus(mode,key=''){
      const focus=mode+':'+key;if(this.focus===focus)return;this.focus=focus;this.focusMode=mode;this.focusToken=String(++this.focusSerial)+':'+focus;
      this.queue.clear();clearTimeout(this.timer);this.inflight=null;
      this.raw.setReadFocus?.(this.focusToken,mode==='detail');
    }
    allowed(scope){if(this.focusMode!=='detail')return true;try{const parts=JSON.parse(scope);return Array.isArray(parts)&&(parts[0]==='detail'||this.allowed(parts[0]));}catch(_){return false;}}
    state(state){
      this.source();const changed=this.busy!==!!state.busy||this.runId!==state.runId;
      if(changed){this.invalidate();this.epoch++;this.queue.clear();this.runId=state.runId;}
      this.busy=!!state.busy;
    }
    key(method,args,scope){
      // Solar bounds that follow the live window must share its time bucket.
      // Otherwise every repaint creates a fresh energy query a few ms later.
      const energy=method.startsWith('energy');
      return JSON.stringify([scope??null,method,args.map((v,i)=>i<2||energy&&i>=3&&(v===args[0]||v===args[1])?Math.floor(v/30000):v)]);
    }
    remember(key,value){
      if(value.scope!=null)for(const [oldKey,old] of this.cache)if(old.method===value.method&&old.scope===value.scope)this.cache.delete(oldKey);
      this.cache.delete(key);if(this.cache.size>=128)this.cache.delete(this.cache.keys().next().value);this.cache.set(key,value);
    }
    // Routine revisions schedule new data; explicit rebuilds invalidate freshness.
    invalidate(drop=false,throttled=false){
      this.throttled=throttled;if(!throttled)this.forcedGeneration=this.generation+1;if(!throttled){clearTimeout(this.deferTimer);this.deferTimer=null;}
      this.generation++;
      // New live readings make results older, but must not restart the first
      // transfer or remove the other visible charts from the waiting queue.
      if(drop){this.epoch++;this.cache.clear();this.queue.clear();}
    }
    previous(method,args,scope){
      return [...this.cache.values()].reverse().find(x=>{
        if(!x.data||x.method!==method||x.scope!==scope)return false;
        const semantic=method==='dailyEnergy'?[]:method.startsWith('energy')?args.slice(2,3):args.slice(2);
        if(!semantic.every((v,i)=>v===x.args[i+2]))return false;
        // A caller-supplied scope describes the same tile/period as its live window moves.
        // Unscoped callers retain only a nearby window, with all query parameters intact.
        return scope!=null||x.args.slice(2).every((v,i)=>method==='dailyEnergy'||v===args[i+2])&&Math.abs(x.args[0]-args[0])<30000&&Math.abs(x.args[1]-args[1])<30000;
      });
    }
    recent(entry,query){
      if(!entry||!query||query.scope==null||entry.error||entry.recheck||entry.epoch!==this.epoch||entry.generation<(this.forcedGeneration||0))return false;
      const age=Date.now()-(entry.startedAt??entry.at);
      if(age<0||age>=45000||[0,1].some(i=>Math.abs(entry.args[i]-query.args[i])>=45000))return false;
      const parts=[entry.data];
      if(query.method.startsWith('energy')){
        for(let i=3;i<query.args.length;i++)if(query.args[i]!==entry.args[i]&&![0,1].some(j=>query.args[i]===query.args[j]&&entry.args[i]===entry.args[j]))return false;
        for(const key of ['solar','consumption','balance','mobile'])if(entry.data?.[key])parts.push(entry.data[key]);
      }
      return parts.every(part=>part&&!part.stale&&!part.pending&&!part.error&&!part.refreshError&&!part.syncing);
    }
    value(entry,method,refreshing=false,query){
      if(!entry?.data){const data=entry?.error?{error:entry.error}:pending(method);if(refreshing&&this.inflight?.progress)data.progress=this.inflight.progress;return JSON.stringify(data);}
      if(!refreshing&&!entry.error&&entry.generation===this.generation)return JSON.stringify(entry.data);
      // A successful recent result stays current during routine background
      // refresh. Missing, failed, explicitly invalidated or delayed data stays gray.
      const current=this.recent(entry,query),data=JSON.parse(JSON.stringify(entry.data)),mark=part=>{
        part.pending=refreshing&&!current;part.stale=!current;part.refreshing=current;part.dataAt=entry.startedAt??entry.at;
        if(refreshing&&this.inflight?.progress)part.progress=this.inflight.progress;
        if(entry.error)part.refreshError=entry.error;else delete part.refreshError;
      };
      mark(data);if(method.startsWith('energy'))for(const key of ['solar','consumption','balance','mobile'])if(data[key])mark(data[key]);
      return JSON.stringify(data);
    }
    accept(query,data){
      const error=data?.error||(!data||typeof data!=='object'?'Zeitraum konnte nicht geladen werden.':'');
      const old=error?this.previous(query.method,query.args,query.scope):null;
      this.remember(query.key,{...query,data:error?old?.data:data,at:error?old?.at:Date.now(),error,generation:query.generation-(!error&&data.stale?1:0),retryAt:error?Date.now()+5000:0});
      if(!error&&!data.pending&&!data.syncing)this.persist();
    }
    initialPending(){return [this.inflight,...this.queue.values()].some(query=>query&&!this.previous(query.method,query.args,query.scope)?.data);}
    retry(queries){
      if(!this.active)return;this.source();
      const seen=new Set();
      for(const query of queries){
        const {method,args,scope}=query,key=this.key(method,args,scope);if(seen.has(key))continue;seen.add(key);
        const same=entry=>entry&&entry.method===method&&entry.scope===scope&&(scope!=null||entry.key===key);
        // A transfer that is already progressing keeps its place. Never add
        // duplicate native work merely because several tiles share its result.
        if(same(this.inflight)&&this.inflight.epoch===this.epoch)continue;
        for(const entry of this.cache.values())if(same(entry)){entry.recheck=true;entry.generation=Math.min(entry.generation,this.generation-1);entry.retryAt=0;}
        this.read(method,args,scope);
      }
    }
    read(method,args,scope){
      this.source();const key=this.key(method,args,scope),query={key,method,args,scope,generation:this.generation,epoch:this.epoch,focus:this.focusToken};
      if(!this.allowed(scope))return this.value(this.previous(method,args,scope),method,false,query);
      // Local model/history reads can be just as expensive as LAN queries. Keep
      // them off the WebView's JavaScript thread whenever the worker is available.
      if(!this.raw.requestHistory){
        let data;try{data=JSON.parse(this.raw[method](...args));}catch(_){data={error:'Zeitraum konnte nicht geladen werden.'};}
        if(data.syncing){try{this.state(JSON.parse(this.raw.syncState()));}catch(_){}return this.busy?this.read(method,args,scope):this.value(this.previous(method,args,scope),method,true);}
        this.accept(query,data);return this.value(this.cache.get(key),method);
      }
      const cached=this.cache.get(key);
      if(cached?.generation===this.generation&&(!cached.error||cached.retryAt>Date.now()))return this.value(cached,method);
      if(cached?.data&&!cached.error&&!cached.recheck&&cached.generation>=(this.forcedGeneration||0)&&cached.generation!==this.generation&&this.throttled&&Date.now()-cached.at<30000){
        if(!this.deferTimer){this.deferTimer=setTimeout(()=>{this.deferTimer=null;if(this.active)this.ready('refresh');},Math.max(1,30000-(Date.now()-cached.at)));this.deferTimer?.unref?.();}
        return this.value(cached,method,false,query);
      }
      const running=this.inflight?.key===key&&(this.inflight.generation===this.generation||scope!=null&&this.inflight.epoch===this.epoch);
      if(!running){
        // Updating a tile's window keeps its position behind the other tiles.
        const slot=scope==null?key:JSON.stringify([method,scope]);
        if(!this.queue.has(slot)&&this.queue.size>=24)this.queue.delete(this.queue.keys().next().value);
        this.queue.set(slot,query);this.pump();
      }
      return this.value(this.previous(method,args,scope),method,true,query);
    }
    pump(){
      if(!this.active||this.inflight||!this.queue.size)return;
      const [key,query]=this.queue.entries().next().value;this.queue.delete(key);
      const request={...query,generation:this.generation,startedAt:Date.now(),id:++this.nextId};this.inflight=request;
      this.armTimeout(request.id);
      try{this.raw.requestHistory(JSON.stringify(request));}
      catch(_){this.result({id:request.id,data:{error:'Zeitraum konnte nicht geladen werden.'}});}
    }
    armTimeout(id){clearTimeout(this.timer);this.timer=setTimeout(()=>this.result({id,data:{error:'Der Abruf dauert zu lange. Die letzten Werte bleiben erhalten.'}}),45000);this.timer?.unref?.();}
    progress(response){if(this.inflight?.id!==response.id)return;this.inflight.progress=String(response.message||'Daten werden abgeglichen …');this.armTimeout(response.id);if(this.active)this.ready(this.inflight.method);}
    setActive(active){this.active=!!active;if(!this.active){clearTimeout(this.timer);return;}if(this.inflight)this.armTimeout(this.inflight.id);this.ready('resume');this.pump();}
    result(response){
      this.source();
      const query=this.inflight;if(!query||query.id!==response.id)return;
      if(!response.interim){clearTimeout(this.timer);this.inflight=null;}
      const data=response.data||{error:response.error||'Zeitraum konnte nicht geladen werden.'};
      const completed=data&&typeof data==='object'&&!data.error&&!data.pending&&!data.syncing;
      // A complete first result from this same plant is useful while recording
      // continues. Keep its original revision so it is visibly stale and a new
      // read follows. Plant changes and import transitions still reject it.
      if(query.epoch===this.epoch&&(query.generation===this.generation||query.scope!=null&&(completed||response.interim&&!data.error))){
        this.accept(query,data);if(this.active)this.ready(query.method);
      }
      if(!response.interim)this.pump();
    }
  };
});
