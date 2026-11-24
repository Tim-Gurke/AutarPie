/* Real Chromium UI with neutral bridge inputs; not a physical handset/LAN test. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs'),http=require('node:http'),wait=require('./browser-wait.cjs'),C=require('../app/src/main/assets/dashboard-core.js');
(async()=>{
 const root=path.resolve(__dirname,'../app/src/main/assets'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://localhost').pathname);if(!file.startsWith(root+path.sep)){res.writeHead(403).end();return;}try{res.setHeader('Content-Type',file.endsWith('.html')?'text/html':file.endsWith('.js')?'application/javascript':file.endsWith('.css')?'text/css':file.endsWith('.svg')?'image/svg+xml':'application/octet-stream');res.end(fs.readFileSync(file));}catch{res.writeHead(404).end();}});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;const errors=[],out=process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/learned-model';fs.mkdirSync(out,{recursive:true});
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.WATTPIT_CHROMIUM,args:['--no-sandbox','--disable-gpu']});const page=await browser.newPage({viewport:{width:412,height:915},locale:'de-DE',timezoneId:'UTC'});page.on('pageerror',e=>errors.push(e.message));
  const tile=C.tile('weather','fixture-model-weather');tile.weatherStyle={version:1,colors:{battery:'#36c280'},icons:{}};tile.layouts.compact={x:0,y:0,w:12,h:11};
  await page.addInitScript(require('./autarpie-fixture.cjs'));await page.addInitScript(board=>{
   apFixture.board=board;apFixture.factorCalls=0;apFixture.calibrationCalls=0;apFixture.gasLearningCalls=0;apFixture.weatherScale=1;apFixture.heat=5;apFixture.pendingModels=[];apFixture.shuntAge=0;
   const now=Number(localStorage.getItem('model-fixture-time')||Date.now());localStorage.setItem('model-fixture-time',String(now));const H=3600000,D=24*H,start=new Date(now).setUTCHours(0,0,0,0),source='neutral-source';
   let solarCore,suppliesCore;Object.defineProperty(window,'WattSolarCore',{configurable:true,get:()=>solarCore,set:value=>{solarCore=value;const consumption=value.consumption,calibration=value.calibration;value.consumption=(...a)=>{apFixture.factorCalls++;return consumption(...a);};value.calibration=(...a)=>{apFixture.calibrationCalls++;return calibration(...a);};}});
   Object.defineProperty(window,'WattSuppliesCore',{configurable:true,get:()=>suppliesCore,set:value=>{suppliesCore=value;const learn=value.learn;value.learn=(...a)=>{apFixture.gasLearningCalls++;return learn(...a);};}});
   const equipment={version:1,groups:[{id:'roof',label:'Beispielanlage',count:2,watts:100,tilt:30,placement:'fixed',measurement:'solar',controller:'MPPT',defaultActive:true}]},setup={groups:{roof:{active:true,azimuth:180,shade:[0,0,0]}}},place={name:'Beispielort',latitude:52,longitude:13},days=[],hours=[],values=[],windows=[],observations=[];
   for(let i=0;i<16;i++){const from=start+i*D,day=new Date(from).toISOString().slice(0,10);days.push({day,code:2,hours:7,sunrise:from+7*H,sunset:from+17*H});for(let j=1;j<=24;j++){hours.push({t:from+j*H});values.push({t:from+j*H,v:j>7&&j<=17?300:0});}for(let slot=0;slot<3;slot++)windows.push({day,slot,from:from+[0,11,16][slot]*H,to:from+[11,16,24][slot]*H});}
   for(let i=1;i<=30;i++){const from=start-i*D,day=new Date(from).toISOString().slice(0,10);for(let slot=0;slot<3;slot++)observations.push({day,slot,from:from+[0,11,16][slot]*H,to:from+[11,16,24][slot]*H,mobileFree:true,coverage:1,solarCoverage:1,bulkCoverage:1,consumption:[110,50,80][slot],solar:100});}
   const knowledge={source,observations,forecasts:[],updated:now-60000},original=WattPit.snapshot;apFixture.knowledge=knowledge;WattPit.appVersion=()=>'3.1.5';WattPit.equipment=()=>JSON.stringify(equipment);WattPit.solarSetup=()=>JSON.stringify(setup);
   WattPit.snapshot=()=>{const s=JSON.parse(original());s.equipment=equipment;s.settings.weatherPlace=place;s.readings.find(r=>r.kind==='shunt').t-=apFixture.shuntAge;s.weather={version:5,updated:apFixture.oldWeather?now-7*H:now,timezone:'UTC',place,days,hours,windows,solar:[{geometry:'30:0',values:values.map(v=>({...v,v:v.v*apFixture.weatherScale}))}]};if(apFixture.otherPlant)s.lan={id:'different-neutral-plant',sourceEpoch:'different-neutral-epoch'};return JSON.stringify(s);};
   const holdKnowledge=()=>localStorage.getItem('fixture-hold-knowledge')==='yes';
   WattPit.requestCachedSolarData=id=>{if(!holdKnowledge())setTimeout(()=>window.onSolarCachedData?.({id,data:knowledge}),0);};WattPit.requestSolarData=()=>{if(!holdKnowledge())setTimeout(()=>window.onSolarPlanData?.(knowledge),0);};
   const events=[],history=[];for(let i=0;i<6;i++){const at=start-(25-i*5)*D;events.push({id:'example-swap-'+i,kind:'gas',type:'swap',at,amount:10,site:{key:'example-site'}});if(i<5)history.push({site:'example-site',source:'archive',from:at,to:at+5*D,heat:3+i*3,windHeat:2,issuedAt:now});}
   WattPit.suppliesSnapshot=()=>JSON.stringify({version:1,protocol:1,events,weather:{history,forecastUpdated:now,siteKey:'example-site',forecast:Array.from({length:16},(_,i)=>({site:'example-site',source:'forecast',from:start+i*D,to:start+(i+1)*D,heat:apFixture.heat,windHeat:2}))}});
   WattPit.requestLearnedModel=json=>{const r=JSON.parse(json),data=JSON.parse(localStorage.getItem('fixture-native-'+r.kind)||'null');setTimeout(()=>onLearnedModelData({...r,action:'read',data:data?.key===r.key?data:null}),0);};
   apFixture.commitModel=r=>{localStorage.setItem('fixture-native-'+r.kind,JSON.stringify(r.data));onLearnedModelData({...r,action:'save',ok:true});};
   WattPit.saveLearnedModel=json=>{const r=JSON.parse(json);if(apFixture.holdModelSave)apFixture.pendingModels.push(r);else setTimeout(()=>apFixture.commitModel(r),0);};
  },{version:1,lanTileVersion:1,energyFlowVersion:1,dischargeSignVersion:1,tiles:[tile],showMiniHistory:false,blockOrder:['tiles','history','record']});
  await page.goto('http://127.0.0.1:'+server.address().port+'/index.html');await wait(page,()=>!!AutarPieApp.solar.modelStore?.value&&!!AutarPieApp.supplies.modelStore?.value);await wait(page,()=>AutarPieApp.solar.days?.[0]?.usage.wh>0);
  assert.ok(await page.evaluate(()=>apFixture.factorCalls>0&&apFixture.calibrationCalls>0&&apFixture.gasLearningCalls>0));
  await page.evaluate(()=>localStorage.setItem('fixture-hold-knowledge','yes'));
  await page.reload();await wait(page,()=>!!AutarPieApp.solar.modelStore?.value&&!!AutarPieApp.supplies.modelStore?.value&&AutarPieApp.solar.days?.[0]?.usage.wh>0);await page.waitForTimeout(300);
  assert.deepEqual(await page.evaluate(()=>[apFixture.factorCalls,apFixture.calibrationCalls,apFixture.gasLearningCalls]),[0,0,0],'a new WebView must use saved compiled factors');
  const bar=page.locator('[data-solar-day="0"] .solar-scale-battery'),fill=bar.locator('.solar-track i');
  assert.match(await bar.locator('em').textContent(),/≈ .*%/);
  assert.equal(await page.evaluate(()=>AutarPieApp.solar.knowledgeState),'loading','the historical journal is deliberately withheld');
  assert.equal(await bar.evaluate(e=>getComputedStyle(e).filter),'none','a valid saved model must release the battery bar while its journal is still loading');
  assert.equal(await fill.evaluate(e=>getComputedStyle(e).backgroundColor),'rgb(54, 194, 128)','the configured green battery color is preserved');
  await page.evaluate(()=>onSolarCachedData({id:AutarPieApp.solar.cachedRead.id,data:apFixture.knowledge}));
  assert.equal(await page.evaluate(()=>AutarPieApp.solar.knowledgeState),'cached');
  assert.equal(await bar.evaluate(e=>getComputedStyle(e).filter),'none','cached observations must not gray an already usable compiled model');
  await page.evaluate(()=>onSolarPlanData({...apFixture.knowledge,stale:true}));
  assert.equal(await bar.evaluate(e=>getComputedStyle(e).filter),'none','unchanged retained observations do not invalidate the saved factors');
  const before=await page.evaluate(()=>({solar:AutarPieApp.solar.days[0].total,usage:AutarPieApp.solar.days[0].usage.wh,gas:AutarPieApp.supplies.analysis.gas.needs[3]}));
  await page.evaluate(()=>{apFixture.weatherScale=.5;apFixture.heat=15;AutarPieApp.poll();AutarPieApp.solar.renderKey='';AutarPieApp.solar.render();AutarPieApp.supplies.render();});
  await wait(page,()=>AutarPieApp.solar.days?.[0]?.total>0);const after=await page.evaluate(()=>({solar:AutarPieApp.solar.days[0].total,usage:AutarPieApp.solar.days[0].usage.wh,gas:AutarPieApp.supplies.analysis.gas.needs[3]}));
  assert.equal(after.solar,before.solar*.5);assert.equal(after.usage,before.usage);assert.notEqual(after.gas,before.gas);assert.deepEqual(await page.evaluate(()=>[apFixture.factorCalls,apFixture.calibrationCalls,apFixture.gasLearningCalls]),[0,0,0]);
  await page.evaluate(()=>{apFixture.holdModelSave=true;onSolarPlanData({...apFixture.knowledge,stale:true,factorInputToken:'corrected-neutral-input',observations:apFixture.knowledge.observations.map(o=>({...o,consumption:o.consumption*1.2}))});});
  await wait(page,()=>!!AutarPieApp.solar.modelStore.pending);
  assert.match(await bar.evaluate(e=>getComputedStyle(e).filter),/grayscale\(1\)/,'a known changed factor input stays gray until its replacement is saved');
  await page.evaluate(()=>apFixture.commitModel(apFixture.pendingModels.find(r=>r.kind==='solar')));
  await wait(page,()=>AutarPieApp.solar.modelStore.value?.revision===AutarPieApp.solar.factorInputRevision);
  assert.equal(await bar.evaluate(e=>getComputedStyle(e).filter),'none','the native save acknowledgement releases the replacement model');
  assert.equal(await fill.evaluate(e=>getComputedStyle(e).backgroundColor),'rgb(54, 194, 128)');
  await page.screenshot({path:path.join(out,'autarpie-3.1.5-model-dark.png'),animations:'disabled'});await page.evaluate(()=>{AutarPieUI.appearance.theme='light';AutarPieUI.applyAppearance();});await page.screenshot({path:path.join(out,'autarpie-3.1.5-model-light.png'),animations:'disabled'});
  await page.evaluate(()=>{apFixture.oldWeather=true;AutarPieApp.poll();});assert.equal(await page.locator('#weatherStrip').getAttribute('data-outdated'),'true','old weather retains its independent stale marker');
  await page.evaluate(()=>{apFixture.oldWeather=false;apFixture.shuntAge=60000;AutarPieApp.poll();});assert.equal(await bar.locator('em').textContent(),'—','an old Shunt sample cannot become a green numeric forecast');assert.ok(await bar.evaluate(e=>e.classList.contains('solar-no-data')));
  await page.evaluate(()=>{apFixture.shuntAge=0;apFixture.otherPlant=true;AutarPieApp.poll();});assert.equal(await bar.locator('em').textContent(),'—','another plant cannot reuse the previous battery prediction');assert.equal(await page.evaluate(()=>AutarPieApp.solar.modelStore.value),null);assert.equal(await page.evaluate(()=>AutarPieApp.solar.factorInputRevision),'','plant changes clear pending factor revisions');
  assert.deepEqual(errors,[]);console.log('PASS actual UI restart with blocked/cached journal, green saved-model bars, unchanged learning counts, atomic replacement, old weather/Shunt and other-plant rejection.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exit(1);});
