/* Actual browser rendering, neutral data and a delayed native-bridge fixture. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const fixture=require('./autarpie-fixture.cjs'),C=require('../app/src/main/assets/dashboard-core.js'),wait=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.WATTPIT_CHROMIUM,args:['--no-sandbox','--disable-gpu']});
 const page=await browser.newPage({viewport:{width:412,height:915},locale:'de-DE',timezoneId:'Europe/Berlin'}),errors=[],out=process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/quick-start';fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 const soc=C.tile('soc','fixture-soc'),chart=C.tile('solar','fixture-chart'),weather=C.tile('weather','fixture-weather');soc.type='gauge';chart.type='chart';chart.period.mode='today';chart.period.aggregate='last';chart.background='#cc6600';chart.opacity=100;
 soc.layouts.compact={x:0,y:0,w:5,h:6};chart.layouts.compact={x:5,y:0,w:7,h:6};weather.layouts.compact={x:0,y:6,w:12,h:5};
 await page.addInitScript(fixture);await page.addInitScript(board=>{
  apFixture.board=board;apFixture.chartStale=true;apFixture.cachedRequests=[];apFixture.freshRequests=0;const original=WattPit.snapshot,now=Date.now(),start=new Date().setHours(0,0,0,0),H=3600000;
  const equipment={version:1,groups:[{id:'roof',label:'Beispielanlage',count:2,watts:100,tilt:30,placement:'fixed',measurement:'solar',controller:'MPPT',defaultActive:true}]};
  const setup={groups:{roof:{active:true,azimuth:180,shade:[0,0,0]}}},place={name:'Beispielort',latitude:52.52,longitude:13.41},days=[],hours=[],values=[],windows=[];
  for(let i=0;i<3;i++){const from=start+i*24*H,day=new Date(from+12*H).toLocaleDateString('sv-SE');days.push({day,code:2,hours:4,sunrise:from+7*H,sunset:from+17*H});for(let h=1;h<=24;h++){const t=from+h*H,v=h>7&&h<=17?250:0;hours.push({t,ghi:v});values.push({t,v});}for(let slot=0;slot<3;slot++)windows.push({day,slot,from:from+[0,11,16][slot]*H,to:from+[11,16,24][slot]*H});}
  WattPit.appVersion=()=>'3.0.14';WattPit.solarSetup=()=>JSON.stringify(setup);WattPit.snapshot=()=>{const value=JSON.parse(original());Object.assign(value,{equipment,weather:{version:4,updated:now,timezone:'Europe/Berlin',place,days,hours,windows,solar:[{geometry:'30:0',values}]}});value.settings.weatherPlace=place;return JSON.stringify(value);};
  WattPit.dashboardHistory=()=>JSON.stringify({points:[{t:now-60000,last:now-60000,v:10,min:10,max:10,n:1},{t:now,last:now,v:148,min:148,max:148,n:1}],summary:{last:148,avg:79,min:10,max:148},stale:apFixture.chartStale,pending:false,dataAt:now-120000});
  WattPit.requestCachedSolarData=id=>apFixture.cachedRequests.push(id);WattPit.requestSolarData=()=>apFixture.freshRequests++;
 },{version:1,lanTileVersion:1,energyFlowVersion:1,dischargeSignVersion:1,tiles:[soc,chart,weather],blockOrder:['tiles','history','record']});
 const failures=[],check=new Proxy(assert,{get:(target,key)=>(...args)=>{try{return target[key](...args);}catch(error){failures.push(error.message);console.error('CHECK '+key+': '+error.message);}}});
 const repaint=()=>page.evaluate(()=>{AutarPieApp.dashboard.cache.clear();AutarPieApp.poll();AutarPieApp.solar.renderKey='';AutarPieApp.solar.render();AutarPieApp.dashboard.update();});
 const filter=locator=>locator.evaluate(e=>getComputedStyle(e).filter),card=page.locator('[data-tile="fixture-chart"]'),battery=page.locator('[data-tile="fixture-soc"]');
 try{
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await wait(page,()=>!!window.AutarPieApp?.solar&&apFixture.cachedRequests.length>0);
  check.match(await filter(card),/grayscale\(1\)/);check.equal(await filter(battery),'none');check.match(await card.locator('.tile-hint').textContent(),/Letzter Stand/);check.equal(await card.locator('.tile-data-status').textContent(),'Letzter Stand');check.equal(await card.locator('.tile-number').textContent(),'148');
  check.equal(await page.evaluate(()=>AutarPieApp.dashboard.config.tiles.find(t=>t.id==='fixture-chart').background),'#cc6600','grayscale must not overwrite the stored background');
  await page.evaluate(()=>onSolarCachedData({id:apFixture.cachedRequests.at(-1),data:{source:'synthetic-source',observations:[{day:new Date(Date.now()-86400000).toLocaleDateString('sv-SE'),slot:0,from:Date.now()-90000000,to:Date.now()-86400000,mobileFree:true,learningVersion:2,usage:{wh:20,duration:3600000,estimatedDuration:0,latest:Date.now()-86400000}}],forecasts:[],updated:Date.now()-86400000}}));
  check.equal(await page.evaluate(()=>AutarPieApp.solar.knowledgeState),'cached');check.match(await page.locator('#solarLearningNote').textContent(),/Bisheriger Lernstand/);await repaint();check.match(await filter(page.locator('[data-tile="fixture-weather"]')),/grayscale\(1\)/);
  await page.evaluate(()=>{apFixture.chartStale=false;onSolarPlanData({source:'synthetic-source',observations:[],forecasts:[],updated:Date.now()});});await repaint();
  check.equal(await filter(card),'none');check.equal(await card.locator('.tile-data-status').count(),0);check.equal(await filter(page.locator('[data-tile="fixture-weather"]')),'none');
  await page.evaluate(()=>onSolarCachedData({id:apFixture.cachedRequests.at(-1),data:{source:'synthetic-source',observations:[{old:true}],forecasts:[],updated:Date.now()-86400000}}));check.equal(await page.evaluate(()=>AutarPieApp.solar.knowledge.observations.length),0,'a delayed prior cannot replace a successful empty refresh');
  await page.screenshot({path:path.join(out,'autarpie-3.0.14-dark.png'),animations:'disabled'});
  await page.evaluate(()=>{apFixture.chartStale=true;AutarPieUI.appearance.theme='light';AutarPieUI.applyAppearance();});await repaint();
  check.match(await filter(card),/grayscale\(1\)/);check.equal(await filter(battery),'none');
  await page.screenshot({path:path.join(out,'autarpie-3.0.14-light.png'),animations:'disabled'});
  // Validate actual screenshot pixels, including the colored tile background.
  const {PNG}=require('pngjs');
  const png=PNG.sync.read(await card.screenshot({animations:'disabled'}));let opaque=0,gray=0;
  for(let i=0;i<png.data.length;i+=4)if(png.data[i+3]>250){opaque++;if(Math.max(...png.data.slice(i,i+3))-Math.min(...png.data.slice(i,i+3))<=2)gray++;}
  check.ok(opaque>1000&&gray/opaque>.98,'the whole stale tile, including its background, must render in grayscale');
  check.deepEqual(errors,[]);assert.deepEqual(failures,[],'All pixel/knowledge checks must pass');console.log('PASS independent current/stale tiles, unchanged stored background, pixel-verified grayscale, immediate cached knowledge and rejection of delayed priors.');
 }catch(error){await page.screenshot({path:path.join(out,'quick-start-failure.png'),fullPage:true}).catch(()=>{});console.error(errors);throw error;}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
