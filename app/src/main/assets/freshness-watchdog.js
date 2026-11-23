(function(root,factory){
  const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.WattFreshnessWatchdog=api;
})(typeof globalThis==='object'?globalThis:this,function(){
  'use strict';
  return class {
    constructor(retry){this.retry=retry;this.fields=new Map();}
    reset(){this.fields.clear();}
    check(grayFields,owner,now=Date.now()){
      if(owner!==this.owner){this.reset();this.owner=owner;}
      const visible=new Set(grayFields),due=[];
      for(const key of this.fields.keys())if(!visible.has(key))this.fields.delete(key);
      for(const key of visible){
        let state=this.fields.get(key);
        if(!state||now<state.since||state.lastRetry!==null&&now<state.lastRetry){state={since:now,lastRetry:null};this.fields.set(key,state);}
        if(now-state.since>=45000&&(state.lastRetry===null||now-state.lastRetry>=60000)){state.lastRetry=now;due.push(key);}
      }
      if(due.length)this.retry(due);
    }
  };
});
