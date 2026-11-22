/* Real browser event flow; synthetic bridge values, no physical Bluetooth claim. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const fixture=require('./autarpie-fixture.cjs'),wait=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.WATTPIT_CHROMIUM,args:['--no-sandbox','--disable-gpu']});
 const page=await browser.newPage({viewport:{width:412,height:915},locale:'de-DE',timezoneId:'Europe/Berlin'}),errors=[],out=process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/viewer-readout';fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(fixture);await page.addInitScript(()=>{
  apFixture.direct=false;apFixture.radio=false;apFixture.central=false;apFixture.controls=[];const original=WattPit.snapshot,place={name:'Standort der Zentrale',latitude:52.52,longitude:13.41};
  WattPit.appVersion=()=>'3.0.16';WattPit.appRole=()=>'viewer';WattPit.isLanViewer=()=>true;
  WattPit.lanState=()=>JSON.stringify({role:'viewer',id:'abcdefghijklmnopqrstuv',name:'Beispielanlage',sourceEpoch:'fixture-epoch',connected:apFixture.central,receivedAt:Date.now()});
  WattPit.viewerReadoutState=()=>JSON.stringify({enabled:apFixture.direct,direct:apFixture.radio?2:0,status:apFixture.direct?'Direkter Liveempfang aktiv · außerhalb der Reichweite Werte von der Zentrale.':'Direkter Liveempfang ausgeschaltet.'});
  WattPit.setViewerReadout=value=>{apFixture.direct=value;return '';};
  WattPit.start=()=>{apFixture.controls.push('start');return '';};WattPit.stop=()=>{apFixture.controls.push('stop');return '';};
  WattPit.snapshot=()=>{const value=JSON.parse(original()),direct=apFixture.direct&&apFixture.radio;value.running=apFixture.central;value.lan=JSON.parse(WattPit.lanState());value.settings.weatherPlace=place;value.viewerReadout={direct:direct?2:0};for(const r of value.readings){r.t=Date.now()-(direct||apFixture.central?0:60000);if(direct)r.liveSource='direct';if(r.kind==='solar')r.solar=direct?148:55;}return JSON.stringify(value);};
 });
 try{
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await wait(page,()=>!!window.AutarPieApp&&!!window.WattLan);
  assert.equal(await page.evaluate(()=>AutarPieApp.reading('solar')),null,'stale central data is not promoted to live');
  await page.evaluate(()=>document.getElementById('lanManage').click());await page.locator('#lanDirectReadout').check();
  assert.equal(await page.evaluate(()=>apFixture.direct),true);assert.equal(await page.evaluate(()=>WattPit.appRole()),'viewer');assert.deepEqual(await page.evaluate(()=>apFixture.controls),[],'enabling passive reception never starts or stops central recording');
  await page.screenshot({path:path.join(out,'autarpie-3.0.16-settings.png'),animations:'disabled'});
  await page.evaluate(()=>{AutarPieApp.closeModal();apFixture.radio=true;AutarPieApp.poll();});
  assert.equal(await page.evaluate(()=>AutarPieApp.reading('solar').solar),148,'direct live data works while the central snapshot is offline');
  assert.equal(await page.evaluate(()=>AutarPieApp.snapshot.running),false,'local reception does not claim the central recorder is running');
  assert.match(await page.locator('#liveStatus').getAttribute('title'),/2 direkt per Bluetooth/);
  assert.deepEqual(await page.evaluate(()=>AutarPieApp.snapshot.settings.weatherPlace),{name:'Standort der Zentrale',latitude:52.52,longitude:13.41});
  await page.evaluate(()=>{apFixture.radio=false;apFixture.central=true;AutarPieApp.poll();});
  assert.equal(await page.evaluate(()=>AutarPieApp.reading('solar').solar),55,'central live values take over when passive reception ends');
  assert.deepEqual(await page.evaluate(()=>apFixture.controls),[]);assert.deepEqual(errors,[]);
  console.log('PASS viewer option, direct values with an offline central, fallback, unchanged GPS and recording ownership.');
 }catch(error){await page.screenshot({path:path.join(out,'viewer-readout-failure.png'),fullPage:true}).catch(()=>{});console.error(errors);throw error;}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
