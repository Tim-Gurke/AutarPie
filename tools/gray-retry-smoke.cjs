/* Actual dashboard and async bridge with a deliberately incomplete first reply. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const fixture=require('./autarpie-fixture.cjs'),C=require('../app/src/main/assets/dashboard-core.js'),wait=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.WATTPIT_CHROMIUM,args:['--no-sandbox','--disable-gpu']});
 const page=await browser.newPage({viewport:{width:412,height:915},locale:'de-DE',timezoneId:'Europe/Berlin'}),errors=[],out=process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/gray-retry';fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 const start=Date.parse('2026-11-23T08:00:00Z'),live=C.tile('soc','retry-live'),chart=C.tile('solar','retry-chart');
 live.type='gauge';chart.type='chart';chart.period={...chart.period,mode:'custom',from:start-3600000,to:start-60000,aggregate:'last'};
 live.layouts.compact={x:0,y:0,w:6,h:6};chart.layouts.compact={x:6,y:0,w:6,h:6};
 await page.clock.setFixedTime(new Date(start));
 await page.addInitScript(fixture);await page.addInitScript(board=>{
  apFixture.board=board;apFixture.requests=[];apFixture.refreshes=0;apFixture.hold=false;apFixture.waiting=null;
  const original=WattPit.snapshot;WattPit.appVersion=()=>'3.1.1';WattPit.isLanViewer=()=>true;
  WattPit.lanState=()=>JSON.stringify({role:'viewer',id:'abcdefghijklmnopqrstuv',sourceEpoch:'retry-fixture',connected:true});
  WattPit.snapshot=()=>{const value=JSON.parse(original());value.historyRevision=1;value.lan=JSON.parse(WattPit.lanState());return JSON.stringify(value);};
  WattPit.refreshLan=()=>apFixture.refreshes++;
  WattPit.dashboardHistory=()=>{throw Error('This scenario must use the asynchronous history bridge.');};
  apFixture.reply=(request,pending)=>{const [from,to]=request.args;window.onHistoryResult({id:request.id,data:{points:[{t:from,v:20},{t:to,v:148}],summary:{last:148,min:20,max:148},...(pending?{pending:true}:{})}});};
  WattPit.requestHistory=json=>{const request=JSON.parse(json);apFixture.requests.push(request);if(apFixture.hold&&request.method==='dashboardHistory'){apFixture.waiting=request;return;}setTimeout(()=>apFixture.reply(request,request.method==='dashboardHistory'),10);};
 },{version:1,lanTileVersion:1,energyFlowVersion:1,dischargeSignVersion:1,showMiniHistory:false,tiles:[live,chart],blockOrder:['tiles','history','record']});
 const card=page.locator('[data-tile="retry-chart"]'),current=page.locator('[data-tile="retry-live"]'),filter=locator=>locator.evaluate(el=>getComputedStyle(el).filter);
 const chartRequests=()=>page.evaluate(()=>apFixture.requests.filter(request=>request.method==='dashboardHistory').length);
 const poll=()=>page.evaluate(()=>AutarPieApp.poll());
 try{
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await wait(page,()=>!!window.AutarPieApp&&apFixture.requests.some(request=>request.method==='dashboardHistory'));
  await page.waitForTimeout(200);await poll();
  assert.match(await filter(card),/grayscale\(1\)/);assert.equal(await filter(current),'none');const layout=await page.evaluate(()=>JSON.stringify(AutarPieApp.dashboard.config)),initial=await chartRequests();
  await page.clock.setFixedTime(new Date(start+44000));await poll();assert.equal(await page.evaluate(()=>apFixture.refreshes),0);assert.equal(await chartRequests(),initial);
  await page.evaluate(()=>apFixture.hold=true);await page.clock.setFixedTime(new Date(start+46000));await poll();
  assert.equal(await page.evaluate(()=>apFixture.refreshes),1);assert.equal(await chartRequests(),initial+1);assert.match(await filter(card),/grayscale\(1\)/,'asking again does not make an incomplete curve current');assert.equal(await filter(current),'none');
  for(let i=0;i<5;i++)await poll();assert.equal(await chartRequests(),initial+1,'repeated repaints do not duplicate the running transfer');
  await page.clock.setFixedTime(new Date(start+106000));await poll();assert.equal(await page.evaluate(()=>apFixture.refreshes),2);assert.equal(await chartRequests(),initial+1,'another retry minute still shares the already running transfer');
  await page.evaluate(()=>{apFixture.reply(apFixture.waiting,false);AutarPieApp.poll();});await page.waitForTimeout(150);await poll();
  assert.equal(await filter(card),'none');assert.equal(await card.locator('.tile-number').textContent(),'148');assert.equal(await filter(current),'none');
  assert.equal(await page.evaluate(()=>JSON.stringify(AutarPieApp.dashboard.config)),layout,'retrying never changes the saved arrangement');
  const refreshed=await page.evaluate(()=>apFixture.refreshes);await page.evaluate(()=>window.onDisplayActive({active:false}));await page.clock.setFixedTime(new Date(start+600000));await poll();assert.equal(await page.evaluate(()=>apFixture.refreshes),refreshed);
  await page.evaluate(()=>window.onDisplayActive({active:true}));assert.equal(await page.evaluate(()=>apFixture.refreshes),refreshed);assert.deepEqual(errors,[]);
  await page.screenshot({path:path.join(out,'autarpie-3.1.1-retry.png'),animations:'disabled'});
  console.log('PASS gray dashboard fields retry after 45 seconds, keep honest colors, bound duplicate requests and preserve layout during a fresh open display.');
 }catch(error){await page.screenshot({path:path.join(out,'gray-retry-failure.png'),fullPage:true}).catch(()=>{});console.error(errors);throw error;}finally{await browser.close();}
})().catch(error=>{console.error(error);process.exit(1);});
