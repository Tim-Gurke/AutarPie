const {test}=require('node:test'),assert=require('node:assert/strict');
const W=require('../app/src/main/assets/weather-style.js'),D=require('../app/src/main/assets/dashboard-core.js'),B=require('../app/src/main/assets/settings-backup.js');
test('each weather type has its own components and all supported condition codes map to a configurable symbol',()=>{
 for(const [type,meta]of Object.entries(W.TYPES))assert.equal(W.type(meta.code),type);
 for(const [type,codes]of Object.entries({partly:[1,2],fog:[45,48],rain:[51,53,55,56,57,61,63,65,66,67,80,81,82],snow:[71,73,75,77,85,86],storm:[95,96,99]}))for(const code of codes)assert.equal(W.type(code),type);
 assert.equal(W.type(null),'unknown');assert.equal(W.type(123),'unknown');
});
test('weather colors round trip across settings and independent dashboard copies without changing old layouts',()=>{
 const board=D.defaults(),tile=board.tiles.find(t=>t.metric==='weather');tile.weatherStyle={version:1,colors:{text:'#123456',muted:'#654321'},icons:{sun:{sun:'#ffaa00'},partly:{sun:'#ffbb11',cloud:'#ffffff'},rain:{cloud:'#aaaaaa',rain:'#2244ff'}}};
 const backup=B.create({settings:{theme:'dark',palette:'classic',capacity:100,sampleSeconds:10,miniRange:'today'}},board,{version:1,groups:[]});
 const restored=B.normalize(JSON.parse(JSON.stringify(backup))).dashboard;
 assert.deepEqual(restored.tiles,board.tiles);const copy=D.normalize(JSON.parse(JSON.stringify(restored)));copy.tiles.find(t=>t.metric==='weather').weatherStyle.icons.partly.cloud='#bbccdd';assert.equal(tile.weatherStyle.icons.partly.cloud,'#ffffff');
 const old=D.defaults();assert.ok(!('weatherStyle'in old.tiles.find(t=>t.metric==='weather')));assert.deepEqual(D.normalize(old),old);
});
test('portable weather fields cannot introduce CSS or recolor a mask and reject unknown schemas',()=>{
 const v=W.normalize({colors:{text:'url(unsafe)',muted:'#ABCDEF',foreign:'#123456'},icons:{partly:{sun:'#FFAA00',mask:'#ffffff'},foreign:{sun:'#123456'}}});
 assert.deepEqual(v,{version:1,colors:{muted:'#abcdef'},icons:{partly:{sun:'#ffaa00'}}});assert.deepEqual(W.normalize({version:99,colors:{text:'#000000'}}),W.normalize());
});
