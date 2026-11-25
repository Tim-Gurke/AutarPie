const test=require('node:test'),assert=require('node:assert/strict'),C=require('../app/src/main/assets/supplies-core.js'),T=require('../app/src/main/assets/supplies-travel.js'),W=require('../app/src/main/assets/weather-history.js');
const H=C.HOUR,D=C.DAY,now=Date.parse('2026-11-20T06:00:00Z'),A={key:'50000:8000',latitude:50,longitude:8,name:'Beispielort A'},B={key:'51000:9000',latitude:51,longitude:9,name:'Beispielort B'};
const swap=(i,at)=>({id:'neutral-stay-swap-'+i,kind:'gas',type:'swap',at,amount:11,empty:true,site:A});
function fixture(){const events=[swap(0,now-50*D),swap(1,now-30*D),swap(2,now-10*D),swap(3,now)],from=now-8*D,to=now-6*D;return {version:1,events,sites:[{at:now-50*D,until:from,site:A},{at:to,until:now+H,site:A}],confirmedStays:[],weather:{version:2,siteKey:A.key,currentPlace:A,place:A,forecastUpdated:now,updated:now,history:[{source:'archive',site:A.key,from:now-50*D,to:now-30*D,heat:3,windHeat:2},{source:'archive',site:A.key,from:now-30*D,to:now-10*D,heat:5,windHeat:2},{source:'archive',site:A.key,from:now-10*D,to:now,heat:12,windHeat:2}],forecast:[{source:'forecast',site:A.key,from:now,to:now+16*D,heat:18,windHeat:2}]}};}
const confirmation=(f,site=A)=>({id:'neutral-confirmation',from:f.sites[0].until,to:f.sites[1].at,site,updated:now});
test('a known place before and after a GPS gap produces a direct yes/no suggestion',()=>{
 const f=fixture(),q=T.questions(f,now);assert.equal(q.length,1);assert.equal(q[0].site.key,A.key);assert.equal(q[0].from,now-8*D);assert.equal(q[0].to,now-6*D);assert.equal(q[0].unknownMillis,2*D);
});
test('yes fills only missing times, reuses existing weather and makes the last bottle eligible',()=>{
 const f=fixture(),before=structuredClone(f),old=C.build(f,now).gas;assert.equal(old.intervals,3);assert.equal(old.weatherStatus.weatherIntervals,2);
 f.confirmedStays=[confirmation(f)];const after=C.build(f,now).gas;
 assert.equal(after.weatherStatus.weatherIntervals,3);assert.equal(after.daily,old.daily);assert.equal(after.lastDaily,1.1);assert.deepEqual(f.events,before.events);assert.deepEqual(f.sites,before.sites);assert.equal(T.questions(f,now).length,0);assert.equal(W.coverage(f,now).unknown,0);
});
test('a corrected place supplies its own weather without rewriting real GPS stays',()=>{
 const f=fixture();f.confirmedStays=[confirmation(f,B)];f.weather.history.push({source:'archive',site:B.key,from:now-8*D,to:now-6*D,heat:22,windHeat:2});const r=T.route(f.events,f.sites,now-10*D,now,A,f.confirmedStays);
 assert.deepEqual(r.map(s=>[s.site,(s.to-s.from)/D]),[[A.key,2],[B.key,2],[A.key,6]]);assert.equal(C.build(f,now).gas.weatherStatus.weatherIntervals,3);assert.deepEqual(f.sites[0].site,A);
});
test('a manual location does not invent missing weather or overwrite new observed evidence',()=>{
 const f=fixture();f.confirmedStays=[confirmation(f,B)];assert.equal(C.build(f,now).gas.weatherStatus.weatherIntervals,2);
 f.sites.push({at:now-8*D,until:now-6*D,site:A});const r=T.route(f.events,f.sites,now-8*D,now-6*D,A,f.confirmedStays);assert.ok(r.every(s=>s.site===A.key));
});
test('removing or correcting a confirmation changes factors once and restores unresolved questions',()=>{
 const f=fixture(),key=C.learningKey(f,now);f.confirmedStays=[confirmation(f)];const confirmed=C.learningKey(f,now);assert.notEqual(confirmed,key);assert.equal(T.questions(f,now).length,0);
 f.weather.forecastUpdated=now+H;assert.equal(C.learningKey(f,now),confirmed);f.confirmedStays=[];assert.equal(C.learningKey(f,now),key);assert.equal(T.questions(f,now).length,1);
});
test('separate unknown spans at the same place are grouped but actual moves stay separate',()=>{
 const f=fixture();f.sites=[{at:now-50*D,until:now-8*D,site:A},{at:now-7*D,until:now-6*D,site:A},{at:now-5*D,until:now+H,site:A}];const q=T.questions(f,now);assert.equal(q.length,1);assert.equal(q[0].unknownMillis,2*D);
 f.sites[1].site=B;assert.equal(T.questions(f,now).length,2);
});
test('last accepted edit wins on overlapping gap confirmations while preserving known observations',()=>{
 const f=fixture();f.confirmedStays=[confirmation(f),{...confirmation(f,B),id:'other-confirmation',updated:now+1}];const r=T.route(f.events,f.sites,now-8*D,now-6*D,A,f.confirmedStays);assert.equal(r[0].site,B.key);
 const key=C.learningKey(f,now);f.confirmedStays[0].updated=now+2;assert.notEqual(C.learningKey(f,now),key);assert.equal(T.route(f.events,f.sites,now-8*D,now-6*D,A,f.confirmedStays)[0].site,A.key);
});
test('colder forecasts cannot undercut the latest complete bottle merely because older cycles were mild',()=>{
 const f=fixture(),p=C.build(f,now).gas;assert.ok(p.daily<1.1);assert.equal(p.lastDaily,1.1);assert.ok(p.expectedDaily>=1.1-1e-10);assert.ok(p.needs[14]>=14*1.1-1e-10);assert.ok(p.remainingHours<=240+1e-5);assert.equal(p.expectedDailyFloorReason,'last-weather-missing');
});
test('a known equally cold period uses the last rate and warmer weather may reduce the forecast',()=>{
 const f=fixture(),profile={kind:'gas',daily:.6,lastDaily:1.1,lastFeatures:{heat:12},model:{provisional:true,coefficients:[.3,.02]}},weather=f.weather;
 const cold=C.consumption(profile,weather,now,now+D,A.key,true);assert.equal(cold.amount,1.1);assert.equal(cold.floorReason,'not-warmer');
 weather.forecast[0].heat=2;const warm=C.consumption(profile,weather,now,now+D,A.key,true);assert.ok(warm.amount<1.1);assert.equal(warm.floorReason,'');
});
test('missing forecast weather uses the last complete rate as an explicit cautious assumption',()=>{
 const f=fixture();f.weather.forecast=[];const p=C.build(f,now).gas;assert.equal(p.expectedDaily,1.1);assert.equal(p.expectedDailyWeatherCorrected,false);assert.equal(p.expectedDailyFloorReason,'forecast-weather-missing');
});
test('a new weather forecast applies saved factors without changing the learned input key',()=>{
 const f=fixture();f.confirmedStays=[confirmation(f)];const model=C.learn(f,now),key=C.learningKey(f,now),first=C.build(f,now,model,{requireLearned:true}).gas;
 f.weather.forecast[0].heat=1;const next=C.build(f,now,model,{requireLearned:true}).gas;assert.equal(C.learningKey(f,now),key);assert.equal(next.modelPending,false);assert.ok(next.expectedDaily<first.expectedDaily);assert.equal(next.daily,first.daily);
});
test('forecasts preserve measured journals, observed locations and learned coefficients',()=>{
 const f=fixture();f.confirmedStays=[confirmation(f)];const original=structuredClone(f),model=C.learn(f,now),saved=JSON.stringify(model);C.build(f,now,model,{requireLearned:true});assert.deepEqual(f,original);assert.equal(JSON.stringify(model),saved);
});
