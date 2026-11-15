package de.wattpit.app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.io.File;
import java.util.*;

/** Raw locations of the collecting phone, independent of solar mounting geometry. */
final class SupplyLocations {
    static final String SITES="supplies-sites-v1",FIX="supplies-location-v2";
    static final long VALID=30*60000L;
    static boolean automatic(Repository repo){
        try{JSONObject journal=new JSONObject(repo.prefs.getString(SuppliesStore.KEY,"{}"));return journal.has("followGps")?journal.getBoolean("followGps"):new JSONObject(repo.prefs.getString("weatherPlace","{}")).optString("name").startsWith("GPS");}catch(Exception e){return false;}
    }
    static JSONObject current(Repository repo)throws Exception{
        if(automatic(repo)){JSONObject fix=new JSONObject(repo.prefs.getString(FIX,"{}"));if(fix.optLong("until")>=System.currentTimeMillis())return fix.getJSONObject("site");return new JSONObject();}
        return SuppliesStore.site(new JSONObject(repo.prefs.getString("weatherPlace","{}")));
    }
    static synchronized void configured(Repository repo)throws Exception{
        JSONObject current=SuppliesStore.site(new JSONObject(repo.prefs.getString("weatherPlace","{}")));
        String signature=current.optString("key")+"|"+automatic(repo);
        if(signature.equals(repo.prefs.getString("supplies-configured-site-v2","")))return;
        JSONArray sites=new JSONArray(repo.prefs.getString(SITES,"[]"));long now=System.currentTimeMillis();
        // Old configured GPS coordinates are not a new fix after a reboot or update.
        sites.put(new JSONObject().put("at",now).put("site",automatic(repo)?new JSONObject():current).put("source",automatic(repo)?"gps-waiting":"configured"));
        repo.prefs.edit().remove(FIX).commit();
        if(!repo.prefs.edit().putString(SITES,sites.toString()).putString("supplies-configured-site-v2",signature).putLong("supplies-locations-revision",now).putLong("supplies-location-started",now).commit())throw new java.io.IOException();
    }
    static synchronized void fix(Repository repo,JSONObject input,long at)throws Exception{
        JSONObject location=SuppliesStore.site(input),prior=new JSONObject(repo.prefs.getString(FIX,"{}"));
        if(at<prior.optLong("at")||at<repo.prefs.getLong("supplies-location-started",0))return;
        JSONObject previous=prior.optJSONObject("site"),chosen=SuppliesStore.site(new JSONObject(repo.prefs.getString("weatherPlace","{}")));
        // Weather grids do not benefit from GPS jitter creating a new site every few metres.
        if(previous!=null&&distance(previous,location)<2000)location=previous;
        else if(chosen.length()>0&&distance(chosen,location)<2000)location=chosen;
        JSONArray sites=new JSONArray(repo.prefs.getString(SITES,"[]"));JSONObject tail=sites.length()>0?sites.getJSONObject(sites.length()-1):null;
        if(tail!=null&&tail.optString("source").startsWith("gps")&&tail.optLong("until")>=at&&tail.optJSONObject("site")!=null&&tail.getJSONObject("site").optString("key").equals(location.optString("key")))tail.put("until",at+VALID);
        else sites.put(new JSONObject().put("at",at).put("until",at+VALID).put("site",location).put("source","gps"));
        if(!repo.prefs.edit().putString(SITES,sites.toString()).putString(FIX,new JSONObject().put("at",at).put("until",at+VALID).put("site",location).toString()).putLong("supplies-locations-revision",System.currentTimeMillis()).commit())throw new java.io.IOException();
    }
    static double distance(JSONObject a,JSONObject b){double la=Math.toRadians(a.optDouble("latitude")),lb=Math.toRadians(b.optDouble("latitude")),dlat=lb-la,dlon=Math.toRadians(b.optDouble("longitude")-a.optDouble("longitude"));double h=Math.sin(dlat/2)*Math.sin(dlat/2)+Math.cos(la)*Math.cos(lb)*Math.sin(dlon/2)*Math.sin(dlon/2);return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)));}
    static JSONObject status(Repository repo)throws JSONException{
        JSONObject fix=new JSONObject(repo.prefs.getString(FIX,"{}"));boolean auto=automatic(repo);
        String state=repo.prefs.getString("supplies-location-status","GPS-Nachführung wartet auf die Aufzeichnung.");
        if(auto&&!repo.running)state="GPS-Nachführung pausiert; auf dem erfassenden Handy die Aufzeichnung starten.";
        else if(auto&&fix.optLong("until")<System.currentTimeMillis()&&state.startsWith("GPS-Nachführung aktiv"))state="Letzter Standort ist veraltet; noch kein neuer GPS-Fix verfügbar.";
        return new JSONObject().put("supported",true).put("automatic",auto).put("lastAt",fix.optLong("at")).put("place",fix.optJSONObject("site")).put("status",auto?state:"Fester Wetterort; bei einem Umzug den Wetterort ändern.");
    }
    static void recover(Repository repo){
        if(repo.prefs.getBoolean("supplies-locations-recovered-v2",false))return;
        try{File file=repo.applicationContext().getDatabasePath("wattpit-planning.db");if(file.isFile())recover(repo,file);repo.prefs.edit().putBoolean("supplies-locations-recovered-v2",true).commit();}catch(Exception ignored){}
    }
    static synchronized void recover(Repository repo,File file)throws Exception{
        TreeMap<Long,JSONObject> points=new TreeMap<>();String last="";
        // Existing local setup records can date earlier moves. Never invent a route from endpoints.
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY);Cursor c=db.rawQuery("SELECT at,payload FROM contexts ORDER BY at",null)){
            while(c.moveToNext()){JSONObject place=SuppliesStore.site(new JSONObject(c.getString(1)).optJSONObject("place"));String key=place.optString("key");if(key.isEmpty()||key.equals(last))continue;last=key;long at=c.getLong(0);points.put(at,new JSONObject().put("at",at).put("site",place).put("source","recorded-setup"));}
        }
        JSONArray saved=new JSONArray(repo.prefs.getString(SITES,"[]"));for(int i=0;i<saved.length();i++){JSONObject row=saved.getJSONObject(i);points.put(row.getLong("at"),row);}
        JSONArray merged=new JSONArray();for(JSONObject row:points.values())merged.put(row);
        if(!repo.prefs.edit().putString(SITES,merged.toString()).putLong("supplies-locations-revision",System.currentTimeMillis()).commit())throw new java.io.IOException();
    }
    private SupplyLocations(){}
}
