const test=require('node:test'),assert=require('node:assert/strict'),C=require('../app/src/main/assets/supplies-core.js'),T=require('../app/src/main/assets/supplies-travel.js');
const D=C.DAY,H=C.HOUR,now=Date.parse('2026-11-15T12:00:00Z'),A={key:'place-a',name:'Ort A'},B={key:'place-b',name:'Ort B'};
const swap=(id,at,site,amount=11)=>({id:'travel-swap-'+id,kind:'gas',type:'swap',at,site,amount,empty:true});
const weather=(site,from,to,heat,windHeat=2)=>({site:site.key,from,to,heat,windHeat,source:'archive'});
const fixture=()=>({events:[swap('first',now-2*D,A),swap('second',now,A)],sites:[{at:now-2*D,site:A},{at:now-D,site:B},{at:now-H,site:A}],weather:{history:[weather(A,now-2*D,now,10),weather(B,now-2*D,now,20)]}});
test('expired GPS fixes leave an explicit unknown span until a later observation',()=>{
 const route=T.route([], [{at:now-4*H,until:now-3*H,site:A},{at:now-H,until:now+H,site:B}],now-4*H,now,A);
 assert.deepEqual(route.map(r=>[r.site,(r.to-r.from)/H]),[[A.key,1],[null,2],[B.key,1]]);
});
test('a GPS expiry cannot override a newer site at the same instant',()=>{
 const route=T.route([], [{at:now-2*H,until:now-H,site:A},{at:now-H,until:now+H,site:B}],now-2*H,now,A);
 assert.deepEqual(route.map(r=>r.site),[A.key,B.key]);
});
test('new hourly coverage counts only actual stop durations and preserves unknown time',()=>{
 const W=require('../app/src/main/assets/weather-history.js'),f=fixture();f.weather.version=2;
 const coverage=W.coverage(f,now);assert.equal(coverage.travel,true);assert.equal(coverage.required,48);assert.equal(coverage.available,48);assert.equal(coverage.state,'complete');assert.equal(coverage.sites.find(s=>s.key===B.key).required,23);
 f.sites[0].until=now-2*D+H;const gap=W.coverage(f,now);assert.equal(gap.unknown,23);assert.equal(gap.available,25);assert.equal(gap.state,'unassigned');
});
test('issued forecast rows cannot make trip weather coverage complete',()=>{
 const W=require('../app/src/main/assets/weather-history.js'),f=fixture();f.weather.version=2;f.weather.history=f.weather.history.map(r=>({...r,source:'forecast'}));
 const coverage=W.coverage(f,now);assert.equal(coverage.available,0);assert.equal(coverage.state,'incomplete');
});
test('a recent backfill cannot make a stale reused forecast fresh',()=>{
 const f=fixture();f.weather={...f.weather,siteKey:A.key,updated:now,forecastUpdated:now-40*H,forecast:[{...weather(A,now,now+20*D,20),source:'forecast'}]};
 const p=C.build(f,now).gas;assert.ok(p.model);assert.equal(p.forecastWeatherCorrected,false);assert.equal(p.weatherStatus.state,'stale');
});
test('one bottle travelling between two locations remains one measured cycle with duration-weighted weather',()=>{
 const input={events:[swap('first',now-2*D,A),swap('second',now,B)],sites:[{at:now-2*D,site:A},{at:now-D,site:B}],weather:{history:[weather(A,now-2*D,now,10),weather(B,now-2*D,now,20)]}};
 const p=C.build(input,now).gas,row=C.intervals(input.events,'gas',now,input.weather,input.sites).rows[0];
 assert.equal(p.intervals,1);assert.equal(p.weatherStatus.weatherIntervals,1);assert.equal(p.daily,5.5);assert.equal(row.amount,11);assert.equal(row.features.heat,15);assert.equal(row.features.sites,2);assert.equal(row.features.coverage,1);
});
test('returning to the first location inside a bottle cycle includes the intervening stop',()=>{
 const f=fixture(),rows=C.intervals(f.events,'gas',now,f.weather,f.sites).rows;
 assert.equal(rows.length,1);assert.equal(rows[0].weather.route.length,3);assert.equal(rows[0].features.sites,2);assert.ok(Math.abs(rows[0].features.heat-(10*(D+H)+20*(D-H))/(2*D))<1e-10);
});
test('a half-hour location change uses that hour at each actual location rather than their full-day means',()=>{
 const from=Date.parse('2026-11-14T10:00:00Z'),to=from+H,midnight=from-10*H;
 const rows=T.canonical([{site:A.key,source:'archive',from:midnight,to:midnight+D,hours:Array.from({length:24},(_,i)=>[i===10?5:18,10])},{site:B.key,source:'archive',from:midnight,to:midnight+D,hours:Array.from({length:24},(_,i)=>[i===10?15:0,10])}]);
 const route=T.route([], [{at:from,site:A},{at:from+H/2,site:B}],from,to,A),result=T.across(rows,route,from,to);
 assert.equal(result.features.heat,8);assert.equal(result.features.windHeat,4);assert.equal(result.coarseMillis,0);
});
test('nearby towns and very distant destinations use their own weather with the same rule',()=>{
 const f=fixture(),copy=structuredClone(f);copy.sites[1].site={...B,latitude:-30,longitude:150};assert.deepEqual(C.build(copy,now).gas,C.build(f,now).gas);
});
test('a recorded stop without any gas or water transaction is still a weather location',()=>{
 const f=fixture(),p=C.build(f,now).gas;assert.equal(f.events.length,2);assert.equal(p.weatherStatus.weatherIntervals,1);assert.equal(p.weatherStatus.details[0].sites,2);
});
test('journal entries at different places alone do not invent the time of an unrecorded move',()=>{
 const f=fixture();f.sites=[];f.events[1].site=B;const p=C.build(f,now).gas;
 assert.equal(p.intervals,1);assert.equal(p.daily,5.5);assert.equal(p.weatherStatus.weatherIntervals,0);assert.equal(p.weatherStatus.details[0].reason,'unknown-location');assert.equal(p.weatherStatus.details[0].locationCoverage,0);
});
test('known intermediate observations retain their known spans around an undated location change',()=>{
 const events=[swap('first',now-2*D,A),{id:'travel-water-stop',kind:'water',type:'supply',at:now-D,amount:20,site:B},swap('second',now,B)];
 const route=T.route(events,[],now-2*D,now,A);assert.equal(route[0].site,null);assert.equal(route[0].to,now-D);assert.equal(route[1].site,B.key);assert.equal(route[1].from,now-D);
});
test('missing weather at one stop leaves that time unfilled and keeps both bottle events',()=>{
 const f=fixture();f.weather.history=f.weather.history.filter(r=>r.site===A.key);const p=C.build(f,now).gas;
 assert.equal(p.intervals,1);assert.equal(p.daily,5.5);assert.equal(p.weatherStatus.weatherIntervals,0);assert.equal(p.weatherStatus.details[0].reason,'missing-weather');assert.equal(p.weatherStatus.details[0].missingWeatherMillis,D-H);assert.equal(f.events.length,2);
});
test('hourly archives and old daily weather cannot double-count time or prefer a coarse mean over the actual hour',()=>{
 const from=now-D,rows=C.historyRows({history:[weather(A,from,now,0),{...weather(A,from+H,from+2*H,20),source:'recent'},{...weather(A,from+H,from+2*H,30),issuedAt:10}]});
 const features=C.weatherFeatures(rows,from,now,A.key);assert.equal(features.coverage,1);assert.equal(features.heat,30/24);
});
test('temperature learning remains possible when wind observations are unavailable',()=>{
 const f=fixture();f.weather.history.forEach(row=>row.windHeat=null);const p=C.build(f,now).gas;
 assert.equal(p.weatherStatus.weatherIntervals,1);assert.equal(p.model.wind,false);assert.equal(p.model.provisional,true);
});
test('issued future forecasts never become historical training rows, including the packed hourly form',()=>{
 assert.deepEqual(C.historyRows({history:[{site:A.key,from:now-D,to:now,source:'forecast',hours:Array.from({length:24},()=>[5,10])}]}),[]);
});
test('stock since the last refill follows each travelled location instead of disabling weather correction for the entire period',()=>{
 const f={events:[swap('first',now-5*D,A,3),swap('second',now-2*D,A)],sites:[{at:now-5*D,site:A},{at:now-D,site:B}],weather:{siteKey:B.key,updated:now,history:[weather(A,now-5*D,now,10),weather(B,now-D,now,20)],forecast:[{...weather(B,now,now+15*D,20),source:'forecast'}]}};
 const p=C.build(f,now).gas;assert.equal(p.daily,1);assert.equal(p.historyWeatherCorrected,true);assert.ok(Math.abs(p.stock-(11-1-4/3))<1e-9);
});
test('travelling weather evaluation preserves original journals, locations, cached weather and water learning',()=>{
 const f=fixture();f.events.push({id:'travel-water-first',kind:'water',type:'supply',at:now-2*D,amount:20,site:A},{id:'travel-water-second',kind:'water',type:'supply',at:now,amount:20,site:A});const original=structuredClone(f);const p=C.build(f,now);
 assert.deepEqual(f,original);assert.equal(p.water.daily,10);assert.equal(p.gas.intervals,1);assert.deepEqual(C.build(f,now),p);
});
