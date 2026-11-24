const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const solarCore=require('../app/src/main/assets/solar-core.js');
const source=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/update-ui.js'),'utf8');
const decorate=source.slice(source.indexOf(' function decorateWeather(offset){'),source.indexOf('\n let pastDay='));
const pastDetail=source.slice(source.indexOf(' function showPastDay(d){'),source.indexOf('\n function refreshPast(){'));
const extras=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/extras.js'),'utf8');
const H=3600000,today='2026-11-24';
const day=(date,hours,code)=>{const from=Date.parse(date+'T08:00:00Z');return {day:date,hours,code,sunrise:from,sunset:from+9*H};};

// Execute the productive retrospective builder and SVG renderer. DOM adapters
// collect markup; this is not a browser layout or a physical weather measurement.
function render(days){
 const children=[],symbols=[],window={WattSolarCore:solarCore},container={dataset:{},prepend:b=>children.unshift(b),querySelector:()=>null};
 const elements={weatherDays:container,apWeatherToday:{}};
 const context={window,document:{createElement:()=>({dataset:{}})},app:{snapshot:{weather:{days}}},solar:{days:[{day:today}],a:{symbol:code=>{symbols.push(code);return window.WattExtras.prototype.symbol.call({},code);}},layoutPreview(){}},C:{finite:Number.isFinite},$:id=>elements[id],esc:x=>String(x),refreshPast(){},requestAnimationFrame(){},renderTrendButton(){}};
 vm.createContext(context);vm.runInContext(extras,context);vm.runInContext(decorate+'\ndecorateWeather();',context);
 return {children,symbols,container};
}

test('retrospective sunshine agrees with its sunshine duration instead of a rain or fog severity code',()=>{
 const days=[day('2026-11-21',7.5,61),day('2026-11-22',1.2,45),day('2026-11-23',6.0,45)],before=JSON.stringify(days),result=render(days);
 assert.deepEqual(result.children.map(b=>b.dataset.weatherHistory),days.map(d=>d.day));
 assert.match(result.children[0].innerHTML,/aria-label="Sonnig"/);
 assert.match(result.children[1].innerHTML,/aria-label="Bewölkt"/);
 assert.match(result.children[2].innerHTML,/aria-label="Heiter bis wolkig"/);
 for(let i=0;i<days.length;i++)assert.match(result.children[i].innerHTML,new RegExp(days[i].hours.toLocaleString('de-DE')+' h'));
 assert.equal(JSON.stringify(days),before,'the original provider code and weather rows remain intact');
});
test('missing sunshine inputs retain the provider symbol and missing weather stays unknown',()=>{
 const days=[{day:'2026-11-21',code:45},{...day('2026-11-22',8,61),sunset:null},{day:'2026-11-23',hours:null,code:null}],result=render(days);
 assert.match(result.children[0].innerHTML,/aria-label="Nebel"/);
 assert.match(result.children[1].innerHTML,/aria-label="Regen"/);
 assert.match(result.children[2].innerHTML,/aria-label="Keine Vorhersage"/);
});
test('retrospective keeps its three local calendar dates and labels zero sunshine as cloudy',()=>{
 const result=render([day('2026-11-20',9,0),day('2026-11-21',0,0),day('2026-11-22',8,48),day('2026-11-23',8,61),day(today,8,61)]);
 assert.deepEqual(result.children.map(b=>b.dataset.weatherHistory),['2026-11-21','2026-11-22','2026-11-23']);
 assert.match(result.children[0].innerHTML,/aria-label="Bewölkt"/);
 assert.match(result.children[0].innerHTML,/>0 h</);
 assert.equal(result.container.dataset.backlook,'true');
});
test('retrospective details explain the day symbol without changing regulator yield or stored battery data',()=>{
 let modal;const context={app:{openModal:(title,html)=>{modal={title,html};}},pastValues:()=>({yieldWh:600,point:{v:70,t:Date.parse('2026-11-23T17:00:00Z')}}),pastValue:(v,unit)=>v+' '+unit,esc:String,window:{},pastContext:null};
 vm.createContext(context);vm.runInContext(pastDetail,context);context.showPastDay(day('2026-11-23',6.0,45));
 assert.equal(modal.title,'Wetterrückschau');
 assert.match(modal.html,/Tagessymbol beschreibt den Sonnenanteil des Tages/);
 assert.match(modal.html,/Fehlen passende Sonnenzeiten/);
 assert.match(modal.html,/600 Wh/);assert.match(modal.html,/70 %/);
});
