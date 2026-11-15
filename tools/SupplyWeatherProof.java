package de.wattpit.app;

import org.json.*;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;

/** Actual weather packing, planning and location persistence with neutral locations. */
public final class SupplyWeatherProof {
    static int checks;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static JSONObject place(double lat,double lon,String name)throws Exception{return new JSONObject().put("latitude",lat).put("longitude",lon).put("name",name);}
    static JSONObject response(long at,int count)throws Exception{JSONArray time=new JSONArray(),temp=new JSONArray(),wind=new JSONArray();for(int i=0;i<count;i++){time.put((at+i*3600000L)/1000L);temp.put(i==3?JSONObject.NULL:i);wind.put(i==2?JSONObject.NULL:10);}return new JSONObject().put("hourly",new JSONObject().put("time",time).put("temperature_2m",temp).put("wind_speed_10m",wind));}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path root=Path.of(args[0]);LanSyncProof.TestContext context=new LanSyncProof.TestContext(root);Repository repo=LanSyncProof.repository(context);long h=SupplyWeatherData.HOUR,d=SupplyWeatherData.DAY,now=System.currentTimeMillis(),base=SupplyWeatherData.day(now)-3*d;
        JSONObject a=SuppliesStore.site(place(50,8,"Ort A")),b=SuppliesStore.site(place(50.15,8.15,"Ort B")),c=SuppliesStore.site(place(51,9,"Zwischenhalt"));String key=a.getString("key");
        JSONArray rows=SupplyWeatherData.hours(response(base,26),key,"archive",now,false);
        check(rows.length()==2,"26 absolute hours remain ordered across UTC day boundaries");
        check(rows.getJSONObject(0).getJSONArray("hours").length()==24,"raw hourly slots are packed into one compact day record");
        check(rows.getJSONObject(0).getJSONArray("hours").isNull(3),"missing temperature remains a gap, not zero degrees");
        check(rows.getJSONObject(0).getJSONArray("hours").getJSONArray(2).isNull(1),"temperature survives a missing wind reading");
        check(rows.getJSONObject(0).getInt("observedHours")==23,"daily compatibility fields disclose incomplete hourly data");
        JSONArray past=SupplyWeatherData.hours(response(base,5),key,"recent",base+2*h+h/2,false),future=SupplyWeatherData.hours(response(base,5),key,"forecast",base+2*h+h/2,true);
        check(past.getJSONObject(0).getInt("observedHours")==2,"only completed past hours are eligible as historical weather");
        check(future.getJSONObject(0).getJSONArray("hours").isNull(1)&&!future.getJSONObject(0).getJSONArray("hours").isNull(2),"current and future forecast hours stay separate from training");
        Map<String,JSONObject> history=new TreeMap<>();SupplyWeatherData.merge(history,rows);
        check(SupplyWeatherData.covered(history,key,base,base+2*h,"archive"),"available hourly archive coverage is recognized");
        check(!SupplyWeatherData.covered(history,key,base,base+4*h,"archive"),"a missing hour cannot be hidden by a daily average");
        JSONObject partial=response(base,26);partial.getJSONObject("hourly").getJSONArray("temperature_2m").put(0,JSONObject.NULL);SupplyWeatherData.merge(history,SupplyWeatherData.hours(partial,key,"archive",now+1,false));
        check(history.get(key+"/"+base+"/archive/hour").getJSONArray("hours").getJSONArray(0).getDouble(0)==0,"a later incomplete download preserves known hours");
        JSONArray events=new JSONArray().put(new JSONObject().put("id","neutral-first").put("kind","gas").put("type","swap").put("amount",11).put("at",base).put("site",a)).put(new JSONObject().put("id","neutral-second").put("kind","gas").put("type","swap").put("amount",11).put("at",base+2*d).put("site",b));
        JSONObject journal=new JSONObject().put("events",events);JSONArray visits=new JSONArray().put(new JSONObject().put("at",base).put("site",a)).put(new JSONObject().put("at",base+5*h).put("until",base+6*h).put("site",c)).put(new JSONObject().put("at",base+d).put("site",b));
        Map<String,SupplyWeatherData.Need> needs=SupplyWeatherData.needs(journal,visits,now);
        check(needs.containsKey(c.getString("key")),"a stop without a gas or water event is scheduled for weather backfill");
        check(needs.get(c.getString("key")).days.size()==1,"a GPS expiry limits the requested stop instead of inventing a long stay");
        check(needs.containsKey(a.getString("key"))&&needs.containsKey(b.getString("key")),"both nearby towns keep their own weather requests");
        String original=events.toString();repo.prefs.edit().putString(SuppliesStore.KEY,new JSONObject().put("version",1).put("events",events).put("followGps",true).toString()).putString("weatherPlace",place(50,8,"GPS · Ort A").toString()).putString("solar-setup-v1","UNCHANGED-SOLAR-GEOMETRY").commit();
        SupplyLocations.configured(repo);check(SupplyLocations.current(repo).length()==0,"an old configured GPS coordinate is not silently refreshed on startup");
        long start=System.currentTimeMillis()+1;SupplyLocations.fix(repo,a,start);int count=new JSONArray(repo.prefs.getString(SupplyLocations.SITES,"[]")).length();SupplyLocations.fix(repo,place(50.001,8.001,"GPS-Standort"),start+15*60000L);
        JSONArray gps=new JSONArray(repo.prefs.getString(SupplyLocations.SITES,"[]"));
        check(gps.length()==count,"nearby GPS jitter and timely heartbeats extend a stay without extra sites");
        check(gps.getJSONObject(gps.length()-1).getLong("until")==start+45*60000L,"the GPS validity boundary follows the last observed fix");
        SupplyLocations.configured(repo);check(new JSONArray(repo.prefs.getString(SupplyLocations.SITES,"[]")).length()==count,"a maintenance tick does not reset a travelling phone to its old configured town");
        SupplyLocations.fix(repo,b,start+20*60000L);check(new JSONObject(repo.prefs.getString(SupplyLocations.FIX,"{}")).getJSONObject("site").getString("key").equals(b.getString("key")),"a real move changes the gas weather site");
        check(repo.prefs.getString("weatherPlace","").contains("Ort A")&&repo.prefs.getString("solar-setup-v1","").equals("UNCHANGED-SOLAR-GEOMETRY"),"following travel does not alter the solar place or mounting geometry");
        count=new JSONArray(repo.prefs.getString(SupplyLocations.SITES,"[]")).length();SupplyLocations.fix(repo,b,start+90*60000L);check(new JSONArray(repo.prefs.getString(SupplyLocations.SITES,"[]")).length()==count+1,"a long GPS outage creates a separate stay instead of bridging an unknown gap");
        check(SuppliesStore.read(repo.prefs).getJSONArray("events").toString().equals(original),"GPS collection preserves every original gas journal entry");
        JSONObject manual=SuppliesStore.read(repo.prefs).put("followGps",false);repo.prefs.edit().putString(SuppliesStore.KEY,manual.toString()).commit();SupplyLocations.configured(repo);check(SupplyLocations.current(repo).getString("key").equals(a.getString("key")),"manual weather mode continues to use the selected fixed place");
        Path database=root.resolve("old-planning.db");try(SQLiteDatabase db=SQLiteDatabase.openDatabase(database.toString(),null,0)){db.execSQL("CREATE TABLE contexts(at INTEGER PRIMARY KEY,payload TEXT)");for(int i=0;i<3;i++)db.execSQL("INSERT INTO contexts VALUES(?,?)",new Object[]{base+i*h,new JSONObject().put("place",i==2?b:a).toString()});}
        repo.prefs.edit().putString(SupplyLocations.SITES,new JSONArray().put(new JSONObject().put("at",base+2*h).put("site",c).put("source","existing")).toString()).commit();SupplyLocations.recover(repo,database.toFile());JSONArray recovered=new JSONArray(repo.prefs.getString(SupplyLocations.SITES,"[]"));
        check(recovered.length()==2,"existing setup records restore actual past location changes without duplicate unchanged sites");
        check(recovered.getJSONObject(1).getJSONObject("site").getString("key").equals(c.getString("key")),"an existing explicit record wins over imported setup history at the same instant");
        check(recovered.getJSONObject(0).getLong("at")==base,"recovered travel times retain their original timestamps");
        check(SuppliesStore.read(repo.prefs).getJSONArray("events").toString().equals(original),"historical location recovery leaves bottle counts and quantities unchanged");
        Files.writeString(root.resolve("packed-weather.json"),new JSONObject().put("version",2).put("history",rows).toString());
        repo.db.close();System.out.println("RESULT "+checks+" supply weather checks passed; no APK or radio test");
    }
}
