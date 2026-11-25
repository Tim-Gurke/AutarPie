(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.WattSupplyTravel=api;})(typeof globalThis==='object'?globalThis:this,function(){
 'use strict';
 const HOUR=3600000,finite=Number.isFinite,READY=Symbol('canonical-weather');
 function canonical(input=[],sources=['archive','recent']){
  if(input[READY])return input;
  const groups=new Map();let order=0;
  for(const row of input){
   if(!sources.includes(row.source)||!row.site||!finite(row.from)||!finite(row.to)||row.to<=row.from)continue;
   const rows=Array.isArray(row.hours)?row.hours.flatMap((slot,i)=>Array.isArray(slot)&&finite(slot[0])?[{...row,from:row.from+i*HOUR,to:Math.min(row.to,row.from+(i+1)*HOUR),heat:Math.max(0,18-slot[0]),windHeat:finite(slot[1])?Math.max(0,18-slot[0])*slot[1]/20:null,hours:undefined,resolution:'hour'}]:[]):[row];
   for(const r of rows){if(!finite(r.heat)||r.to<=r.from)continue;const candidate={...r,index:order++},list=groups.get(r.site)||[];list.push(candidate);groups.set(r.site,list);}
  }
  const result=[];
  for(const rows of groups.values()){
   const events=[];for(const row of rows){events.push({at:row.from,row,start:true},{at:row.to,row,start:false});}
   events.sort((a,b)=>a.at-b.at);const heap=[];
   const better=(a,b)=>(a.to-a.from)!==(b.to-b.from)?a.to-a.from<b.to-b.from:a.source!==b.source?({archive:3,recent:2,forecast:1}[a.source]||0)>({archive:3,recent:2,forecast:1}[b.source]||0):(a.issuedAt||0)!==(b.issuedAt||0)?(a.issuedAt||0)>(b.issuedAt||0):a.index>b.index;
   const push=row=>{heap.push(row);let i=heap.length-1;while(i>0){const parent=(i-1)>>1;if(!better(heap[i],heap[parent]))break;[heap[i],heap[parent]]=[heap[parent],heap[i]];i=parent;}};
   const pop=()=>{const tail=heap.pop();if(!heap.length)return;heap[0]=tail;let i=0;for(;;){let next=i,l=2*i+1,r=l+1;if(l<heap.length&&better(heap[l],heap[next]))next=l;if(r<heap.length&&better(heap[r],heap[next]))next=r;if(next===i)break;[heap[i],heap[next]]=[heap[next],heap[i]];i=next;}};
   for(let i=0;i<events.length;){const at=events[i].at;while(i<events.length&&events[i].at===at){if(events[i].start)push(events[i].row);i++;}while(heap.length&&heap[0].to<=at)pop();const to=events[i]?.at;if(heap.length&&to>at){const row=heap[0],tail=result.at(-1);if(tail?.index===row.index&&tail.to===at)tail.to=to;else result.push({...row,from:at,to});}}
  }
  result.sort((a,b)=>a.from-b.from||String(a.site).localeCompare(String(b.site)));Object.defineProperty(result,READY,{value:true});return result;
 }
 function totals(rows,from,to,site){
  let covered=0,heat=0,windCovered=0,windHeat=0,coarse=0;
  for(const row of rows){if(row.site!==site||!finite(row.heat))continue;const span=Math.max(0,Math.min(to,row.to)-Math.max(from,row.from));if(!span)continue;covered+=span;heat+=span*row.heat;if(finite(row.windHeat)){windCovered+=span;windHeat+=span*row.windHeat;}if(row.resolution!=='hour'&&row.to-row.from>HOUR)coarse+=span;}
  return {covered,heat,windCovered,windHeat,coarse};
 }
 function features(rows,from,to,site){
  if(!site||to<=from)return null;const t=totals(rows[READY]?rows:canonical(rows,['archive','recent','forecast',undefined]),from,to,site),duration=to-from;
  return t.covered/duration>=.9?{heat:t.heat/t.covered,windHeat:t.windCovered/duration>=.9?t.windHeat/t.windCovered:null,coverage:t.covered/duration,windCoverage:t.windCovered/duration,coarseMillis:t.coarse}:null;
 }
 /** Explicit location history supplies transitions. Journal snapshots alone cannot date a move. */
 function route(events,sites,from,to,initialSite,confirmedStays=[]){
  if(to<=from)return [];
  const raw=sites.filter(p=>finite(p.at)&&p.at<=to).sort((a,b)=>a.at-b.at),recorded=[];
  for(let i=0;i<raw.length;i++){const point=raw[i];recorded.push({...point,recorded:true});if(finite(point.until)&&point.until>=point.at&&point.until<=(to)&&(!raw[i+1]||point.until<raw[i+1].at))recorded.push({at:point.until,site:null,recorded:true});}
  const observations=events.filter(p=>finite(p.at)&&p.at>=from&&p.at<=to&&p.site?.key).map(p=>({...p,recorded:false}));
  const before=recorded.filter(p=>p.at<=from).sort((a,b)=>a.at-b.at).at(-1);
  let site=before?before.site:initialSite,cursor=from,evidence=from;const out=[];
  const append=(a,b,key,reason='')=>{if(b>a){const tail=out.at(-1);if(tail?.site===key&&tail.to===a&&tail.reason===reason)tail.to=b;else out.push({from:a,to:b,site:key||null,reason});}};
  const points=[...recorded.filter(p=>p.at>from),...observations].sort((a,b)=>a.at-b.at||Number(a.recorded)-Number(b.recorded));
  for(const point of points){
   const next=point.site?.key||null,current=site?.key||null;
   if(point.recorded){append(cursor,point.at,current,current?'':'unknown-location');cursor=point.at;site=point.site;evidence=point.at;}
   else if(next===current){evidence=point.at;}
   else {
    // Preserve a known prefix; the unrecorded transition itself stays uncertain.
    append(cursor,evidence,current,current?'':'unknown-location');append(evidence,point.at,null,'unknown-transition');cursor=point.at;site=point.site;evidence=point.at;
   }
  }
  append(cursor,to,site?.key,site?.key?'':'unknown-location');return applyStays(out,confirmedStays);
 }
 // User confirmations fill gaps; original observed locations always win.
 function applyStays(segments,stays=[]){
  const valid=stays.filter(s=>s.site?.key&&finite(s.from)&&finite(s.to)&&s.to>s.from);
  if(!valid.length)return segments;
  const out=[],append=s=>{const prior=out.at(-1);if(prior&&prior.to===s.from&&prior.site===s.site&&prior.reason===s.reason&&prior.confirmationId===s.confirmationId)prior.to=s.to;else out.push({...s});};
  for(const segment of segments){
   if(segment.site){append(segment);continue;}
   const matching=valid.filter(s=>s.from<segment.to&&s.to>segment.from),cuts=[...new Set([segment.from,segment.to,...matching.flatMap(s=>[Math.max(segment.from,s.from),Math.min(segment.to,s.to)])])].sort((a,b)=>a-b);
   for(let i=1;i<cuts.length;i++){const from=cuts[i-1],to=cuts[i],stay=matching.filter(s=>s.from<=from&&s.to>=to).sort((a,b)=>(a.updated||0)-(b.updated||0)||valid.indexOf(a)-valid.indexOf(b)).at(-1);append(stay?{from,to,site:stay.site.key,reason:'user-confirmed',confirmationId:stay.id}:{...segment,from,to});}
  }
  return out;
 }
 function places(config){
  const found=new Map();for(const s of [...(config.events||[]).map(e=>e.site),...(config.sites||[]).map(e=>e.site),...(config.confirmedStays||[]).map(e=>e.site),config.weather?.place,config.weather?.currentPlace])if(s?.key)found.set(s.key,s);return [...found.values()];
 }
 function questions(config,now=Date.now()){
  const events=(config.events||[]).filter(e=>e.kind==='gas'&&e.type==='swap'&&finite(e.at)&&e.at<=now).sort((a,b)=>a.at-b.at);if(!events.length)return [];
  const from=Math.max(events[0].at,now-1096*24*HOUR),to=Math.floor(now/60000)*60000,segments=route(config.events||[],config.sites||[],from,to,events[0].site,config.confirmedStays||[]),known=new Map(places(config).map(s=>[s.key,s])),out=[],following=[];let previous=events[0].site?.key,merge=true,next;
  for(let i=segments.length-1;i>=0;i--){following[i]=next;if(segments[i].site)next=segments[i].site;}
  for(let i=0;i<segments.length;i++){
   const segment=segments[i];if(segment.site){if(segment.site!==previous)merge=false;previous=segment.site;continue;}
   const key=previous||following[i]||config.weather?.currentPlace?.key||config.weather?.siteKey,site=known.get(key);if(!site)continue;
   const tail=out.at(-1),millis=segment.to-segment.from;
   if(merge&&tail?.site.key===site.key){tail.to=segment.to;tail.unknownMillis+=millis;}else out.push({id:'stay-'+segment.from+'-'+site.key,from:segment.from,to:segment.to,unknownMillis:millis,site});merge=true;
  }
  return out.filter(q=>q.unknownMillis>=15*60000).reverse();
 }
 function across(rows,segments,from,to){
  const canonicalRows=rows[READY]?rows:canonical(rows);let covered=0,heat=0,windCovered=0,windHeat=0,known=0,coarse=0;const places=new Set();
  for(const segment of segments){if(!segment.site)continue;known+=segment.to-segment.from;places.add(segment.site);const t=totals(canonicalRows,segment.from,segment.to,segment.site);covered+=t.covered;heat+=t.heat;windCovered+=t.windCovered;windHeat+=t.windHeat;coarse+=t.coarse;}
  const duration=to-from,coverage=duration>0?covered/duration:0;
  return {features:coverage>=.9?{heat:heat/covered,windHeat:windCovered/duration>=.9?windHeat/windCovered:null,coverage,windCoverage:windCovered/duration,coarseMillis:coarse,sites:places.size}:null,coverage,locationCoverage:duration>0?known/duration:0,unknownLocationMillis:Math.max(0,duration-known),missingWeatherMillis:Math.max(0,known-covered),coarseMillis:coarse,sites:places.size};
 }
 return {canonical,features,route,across,applyStays,places,questions};
});
