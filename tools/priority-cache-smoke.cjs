/* Actual app UI, synthetic private readings. No handset latency or radio claim. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const fixture=require('./autarpie-fixture.cjs'),wait=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.WATTPIT_CHROMIUM,args:['--no-sandbox','--disable-gpu']});
 const page=await browser.newPage({viewport:{width:390,height:844},timezoneId:'Europe/Berlin'}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));page.setDefaultTimeout(10000);
 await page.clock.setFixedTime(new Date('2026-11-23T12:00:00Z'));await page.addInitScript(fixture);
 await page.addInitScript(()=>{
  apFixture.board.showMiniHistory=false;apFixture.board.tiles=apFixture.board.tiles.slice(0,3).filter(t=>t.metric!=='soc');
  for(const tile of apFixture.board.tiles){tile.type='chart';tile.period={mode:'today',aggregate:'last'};}
  apFixture.requests=[];apFixture.focus=[];WattPit.appVersion=()=>'3.1.3';WattPit.isLanViewer=()=>true;
  WattPit.lanState=()=>JSON.stringify({id:'fixture-priority-plant',sourceEpoch:'fixture-epoch',connected:true});
  WattPit.requestHistory=json=>apFixture.requests.push(JSON.parse(json));WattPit.setReadFocus=(focus,detail)=>apFixture.focus.push({focus,detail});
  WattPit.saveRoller=json=>{apFixture.roller=JSON.parse(json);return '';};
  WattPit.requestPlantCommand=json=>{const q=JSON.parse(json);if(q.action==='saveRoller')WattPit.saveRoller(q.args[0]);setTimeout(()=>onPlantResult({id:q.id,ok:true,revision:1}),1);};
  const roller=WattPit.rollerSnapshot;WattPit.rollerSnapshot=()=>{const value=JSON.parse(roller());if(apFixture.roller)value.profile=apFixture.roller;return JSON.stringify(value);};
 });
 const out=process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/priority-cache';fs.mkdirSync(out,{recursive:true});
 async function answer(){await page.evaluate(()=>{const q=apFixture.requests.at(-1),now=Date.now(),voltage=q.args[2]==='voltage',last=voltage?13.1:42,points=Array.from({length:13},(_,i)=>{const v=voltage?13.1:42*i/12;return {t:q.args[0]+(now-1000-q.args[0])*i/12,v,min:v,max:v,n:1};});onHistoryResult({id:q.id,data:{points,summary:{last,avg:voltage?13.1:21,min:voltage?13.1:0,max:last,coverage:1,from:q.args[0],to:q.args[1]}}});});}
 try{
  await page.goto('file://'+path.resolve('app/src/main/assets/index.html'));await wait(page,()=>apFixture.requests.length>0);
  await answer();await page.waitForTimeout(200);await answer();await page.waitForTimeout(200);
  await page.locator('[data-tile="sample-1"]').click();await page.locator('#tileActionDetail').click();
  await wait(page,()=>apFixture.focus.some(f=>f.detail));
  const before=await page.evaluate(()=>apFixture.requests.length);await answer();await page.waitForTimeout(250);
  await page.evaluate(()=>{AutarPieApp.poll();AutarPieApp.poll();AutarPieApp.poll();});await page.waitForTimeout(250);
  assert.equal(await page.evaluate(()=>apFixture.requests.length),before,'a detail must not start other dashboard or learning reads');
  assert.equal(await page.locator('#detailReading .detail-number').textContent(),'42');
  await page.screenshot({path:path.join(out,'autarpie-3.1.3-dark.png'),animations:'disabled'});
  await page.locator('#detailClose').click();await page.waitForTimeout(200);
  await page.reload();await wait(page,()=>apFixture.requests.length>0);
  const solar=page.locator('[data-tile="sample-1"]');await wait(page,()=>document.querySelector('[data-tile="sample-1"] .tile-number')?.textContent.includes('42'));
  assert.ok((await solar.getAttribute('class')).includes('data-outdated'),'restored values keep the pending-data marker');
  await page.evaluate(()=>{AutarPieUI.appearance.theme='light';AutarPieUI.applyAppearance();});
  await page.screenshot({path:path.join(out,'autarpie-3.1.3-light.png'),animations:'disabled'});
  await page.evaluate(()=>AutarPieApp.roller.configure());await page.locator('#roller-displayFreshSeconds').fill('25');await page.locator('#rollerSave').click();
  await wait(page,()=>apFixture.roller?.displayFreshSeconds===25);
  await page.evaluate(()=>AutarPieApp.roller.configure());assert.equal(await page.locator('#roller-displayFreshSeconds').inputValue(),'25');
  await page.locator('#roller-displayFreshSeconds').scrollIntoViewIfNeeded();await page.evaluate(()=>{document.getElementById('toast').hidden=true;const status=document.getElementById('plantActionStatus');if(status)status.hidden=true;});await page.screenshot({path:path.join(out,'autarpie-3.1.3-settings.png'),animations:'disabled'});
  assert.deepEqual(errors,[]);console.log('PASS actual detail priority, persisted curve after reload, gray data status, and saved BMS display tolerance.');
 }catch(error){await page.screenshot({path:path.join(out,'priority-failure.png'),fullPage:true}).catch(()=>{});console.error(errors);throw error;}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
