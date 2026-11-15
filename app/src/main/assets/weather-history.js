(function(root,factory){const api=factory(typeof module==='object'&&module.exports?require('./supplies-travel.js'):root.WattSupplyTravel);if(typeof module==='object'&&module.exports)module.exports=api;else root.WattWeatherHistory=api;})(typeof globalThis==='object'?globalThis:this,function(T){
  'use strict';
  const DAY=86400000,finite=n=>typeof n==='number'&&Number.isFinite(n);
  function coverage(config={},now=Date.now()){
    if(config.weather?.version>=2)return travelCoverage(config,now);
    const weather=config.weather||{};let timezone=weather.timezone,verifiedZone=true,format;
    try{if(!timezone)throw Error();format=new Intl.DateTimeFormat('en-CA',{timeZone:timezone,year:'numeric',month:'2-digit',day:'2-digit'});}catch(e){timezone='UTC';verifiedZone=false;format=new Intl.DateTimeFormat('en-CA',{timeZone:timezone,year:'numeric',month:'2-digit',day:'2-digit'});}
    const date=t=>{const p=Object.fromEntries(format.formatToParts(t).map(p=>[p.type,p.value]));return `${p.year}-${p.month}-${p.day}`;},today=date(now),limit=date(now-1096*DAY),sites=new Map(),stored=new Map();
    const ensure=(key,name)=>{if(!sites.has(key))sites.set(key,{key,name:name||'Eintragsort',first:null,rows:[]});return sites.get(key);};
    if(weather.siteKey)ensure(weather.siteKey,weather.place?.name);
    for(const e of config.events||[])if(e.site?.key)ensure(e.site.key,e.site.name);
    let unknown=0,truncated=false;
    for(const e of config.events||[]){
      if(!finite(e.at)||date(e.at)>=today)continue;
      if(!e.site?.key){unknown++;continue;}
      const s=ensure(e.site.key,e.site.name),first=date(e.at);truncated=truncated||first<limit;
      if(s.first===null||first<s.first)s.first=first;
    }
    // Keep only usable, completed historical daily records. Forecasts never prove a backfill.
    for(const row of weather.history||[]){
      if(!['archive','recent'].includes(row.source)||!row.site||!finite(row.from)||!finite(row.to)||row.to>now||row.to-row.from<21*3600000||row.to-row.from>27*3600000||!['temperature','wind','heat','windHeat'].every(k=>finite(row[k])))continue;
      const day=date(row.from);if(day>=today||date(row.to-1)!==day)continue;
      const key=row.site+'/'+day,prior=stored.get(key);if(!prior||row.source==='archive')stored.set(key,{...row,date:day});
    }
    for(const row of stored.values())ensure(row.site,row.site===weather.siteKey?weather.place?.name:null).rows.push(row);
    let required=0,available=0;
    const summaries=[...sites.values()].map(s=>{
      const missing=[],present=[],start=s.first===null?null:s.first<limit?limit:s.first;
      if(start)for(let t=Date.parse(start+'T12:00:00Z');new Date(t).toISOString().slice(0,10)<today;t+=DAY){const day=new Date(t).toISOString().slice(0,10);(stored.has(s.key+'/'+day)?present:missing).push(day);}
      required+=present.length+missing.length;available+=present.length;s.rows.sort((a,b)=>a.from-b.from);
      return {...s,start,end:start?new Date(Date.parse(today+'T12:00:00Z')-DAY).toISOString().slice(0,10):null,present,missing,stored:s.rows.length,from:s.rows[0]?.date||null,to:s.rows.at(-1)?.date||null};
    }).filter(s=>s.first!==null||s.stored);
    const missing=required-available,state=!verifiedZone?'unverified':unknown?'unassigned':!required?'idle':missing?'incomplete':'complete';
    return {state,timezone,required,available,missing,unknown,truncated,sites:summaries,stored:stored.size,rows:[...stored.values()].sort((a,b)=>b.from-a.from),pending:!!weather.pending,error:weather.error||'',updated:weather.updated||null};
  }
  function travelCoverage(config,now){
    const weather=config.weather||{},events=(config.events||[]).filter(e=>finite(e.at)&&e.at<=now).sort((a,b)=>a.at-b.at),first=events[0],end=Math.floor(now/3600000)*3600000,start=first?Math.max(first.at,now-1096*DAY):end;
    const route=T.route(events,config.sites||[],start,end,first?.site),rows=T.canonical(weather.history||[]),basis=T.across(rows,route,start,end),names=new Map();
    for(const point of [...events,...config.sites||[]])if(point.site?.key)names.set(point.site.key,point.site.name||'Aufenthaltsort');
    const sites=[...new Set(route.map(s=>s.site).filter(Boolean))].map(key=>{
      const spans=route.filter(s=>s.site===key),required=spans.reduce((n,s)=>n+s.to-s.from,0)/3600000,coverage=T.across(rows,spans,start,end),available=(coverage.features?.coverage||coverage.coverage)*(end-start)/3600000;
      return {key,name:names.get(key)||'Aufenthaltsort',required,available,missing:Math.max(0,required-available),from:spans[0].from,to:spans.at(-1).to};
    });
    const required=Math.max(0,end-start)/3600000,available=basis.coverage*required,unknown=basis.unknownLocationMillis/3600000;
    return {travel:true,state:!required?'idle':unknown>0?'unassigned':required-available>1e-6?'incomplete':'complete',required,available,missing:Math.max(0,required-available),unknown,sites,pending:!!weather.pending,error:weather.error||'',updated:weather.updated||null,truncated:!!first&&first.at<now-1096*DAY};
  }
  return {coverage};
});
