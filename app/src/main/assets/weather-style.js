(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.WattWeatherStyle=api;})(typeof globalThis==='object'?globalThis:this,function(){
 'use strict';
 const TYPES={sun:{label:'Sonne',code:0,parts:{sun:'Sonne'}},partly:{label:'Sonne und Wolke',code:2,parts:{sun:'Sonne',cloud:'Wolke'}},cloud:{label:'Wolken',code:3,parts:{cloud:'Wolke'}},fog:{label:'Nebel',code:45,parts:{cloud:'Wolke',fog:'Nebel'}},rain:{label:'Regen',code:61,parts:{cloud:'Wolke',rain:'Tropfen'}},snow:{label:'Schnee',code:71,parts:{cloud:'Wolke',snow:'Schnee'}},storm:{label:'Gewitter',code:95,parts:{cloud:'Wolke',lightning:'Blitz'}},unknown:{label:'Unbekannt',code:-1,parts:{unknown:'Symbol'}}};
 const FIELDS={text:'Schrift und Zahlen',muted:'Nebenbeschriftungen',symbol:'Symbol-Vorgabe',line:'Trennlinien',control:'Bedienelemente',selected:'Auswahlmarkierung',solar:'Solarbalken',battery:'Akkubalken',extras:'Extras-Balken',track:'Balkenhintergrund'};
 const color=v=>typeof v==='string'&&/^#[0-9a-f]{6}$/i.test(v)?v.toLowerCase():null;
 function normalize(input){const out={version:1,colors:{},icons:{}};if(!input||typeof input!=='object'||input.version!=null&&input.version!==1)return out;
  for(const key of Object.keys(FIELDS)){const c=color(input.colors?.[key]);if(c)out.colors[key]=c;}
  for(const [type,meta]of Object.entries(TYPES)){const parts={};for(const part of Object.keys(meta.parts)){const c=color(input.icons?.[type]?.[part]);if(c)parts[part]=c;}if(Object.keys(parts).length)out.icons[type]=parts;}
  return out;
 }
 function type(code){if(code===0)return 'sun';if(code===1||code===2)return 'partly';if(code===3)return 'cloud';if(code===45||code===48)return 'fog';if([51,53,55,56,57,61,63,65,66,67,80,81,82].includes(code))return 'rain';if([71,73,75,77,85,86].includes(code))return 'snow';if([95,96,99].includes(code))return 'storm';return 'unknown';}
 return {TYPES,FIELDS,color,normalize,type};
});
