/* A connected idle charger has zero current; missing replies stay unknown. Synthetic data only. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs'),wait=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})}),page=await browser.newPage({viewport:{width:412,height:1200},locale:'de-DE',timezoneId:'UTC'}),errors=[];
 const out=path.resolve(process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/autarpie-ui');fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 const preset=require('../app/src/main/assets/ui-preset.json');
 try{
  await page.addInitScript({content:'('+require('./autarpie-fixture.cjs').toString()+')();('+function(p){
   apFixture.board=p.dashboard;apFixture.workspace=p.workspace;apFixture.appearance=p.appearance;apFixture.scale=p.viewSettings.displayScale;
   apFixture.freshCharger=true;apFixture.chargerError='';WattPit.appVersion=()=>'3.0.7';
   WattPit.energySnapshot=()=>JSON.stringify({config:{assignment:'model',monitor:true,rules:{enabled:false}},output:{at:Date.now(),state:0,current:0,voltage:13},charger:{at:Date.now()-(apFixture.freshCharger?0:60000),mode:0,voltage:11.1,current:0,capacity:0,elapsed:0,cells:[3.7,3.7,3.7]},chargerConfig:{name:'608PD · Beispiel',role:'model',enabled:true,hasAuth:true},chargerError:apFixture.chargerError});
  }.toString()+')('+JSON.stringify(preset)+');'});
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await wait(page,()=>!!window.AutarPieUI&&!!window.AutarPieApp?.energy);
  await page.evaluate(()=>AutarPieApp.showPage('model'));
  const panel=page.locator('#apChargerPanel');await panel.getByText('Bereit',{exact:true}).waitFor();
  assert.equal(await panel.locator('.ap-energy-values strong').nth(1).textContent(),'0,00 A');
  assert.equal(await panel.locator('#apChargerStop').isDisabled(),true);assert.equal(await panel.locator('#apChargerStart').isDisabled(),false);
  for(const theme of ['dark','light']){
   await page.evaluate(theme=>{AutarPieUI.appearance.theme=theme;apFixture.appearance={...AutarPieUI.appearance};AutarPieUI.applyAppearance();document.getElementById('toast').hidden=true;},theme);
   assert.equal(await page.evaluate(()=>document.body.scrollWidth>innerWidth),false);
   await page.screenshot({path:path.join(out,'autarpie-3.0.7-'+theme+'.png'),animations:'disabled'});
  }
  await page.evaluate(()=>{apFixture.freshCharger=false;apFixture.chargerError='Die Bluetooth-Paketgröße für den 608PD ist zu klein (23; mindestens 50 erforderlich).';AutarPieApp.energy.poll();});
  await panel.getByText('Keine aktuellen Laderdaten',{exact:true}).waitFor();
  assert.equal(await panel.locator('.ap-energy-values strong').nth(1).textContent(),'— A');
  assert.match(await panel.locator('.form-error').textContent(),/Paketgröße.*23.*50/);
  assert.equal(await panel.locator('#apChargerStart').isDisabled(),true);assert.equal(await panel.locator('#apChargerStop').isDisabled(),true);
  assert.deepEqual(errors,[]);console.log('PASS 3 scenarios: idle zero-current status in dark/light mode, and unknown measurements with disabled commands after missing replies.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
