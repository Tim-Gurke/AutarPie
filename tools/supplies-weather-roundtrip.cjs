const fs=require('node:fs'),assert=require('node:assert/strict'),T=require('../app/src/main/assets/supplies-travel.js');
const weather=JSON.parse(fs.readFileSync(process.argv[2],'utf8')),rows=T.canonical(weather.history),first=weather.history[0];
assert.equal(rows.length,25);assert.equal(T.features(rows,first.from,first.from+2*3600000,first.site).heat,17.5);assert.equal(T.features(rows,first.from+2*3600000,first.from+3*3600000,first.site).windHeat,null);
assert.equal(T.features(rows,first.from,first.from+4*3600000,first.site),null);
console.log('PASS native packed weather survives JSON transfer into the display model, including absent wind and uncovered temperature');
