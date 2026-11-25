package de.wattpit.app;

import org.json.*;
import java.util.*;

/** Compact raw hourly weather. Consumption fitting remains in the display client. */
final class SupplyWeatherData {
    static final long HOUR=3600000L,DAY=24*HOUR;
    static long day(long at){return Math.floorDiv(at,DAY)*DAY;}
    static String key(JSONObject row){return row.optString("site")+"/"+row.optLong("from")+"/"+row.optString("source")+"/"+(row.has("hours")?"hour":"day");}
    static JSONArray hours(JSONObject response,String site,String source,long now,boolean future)throws Exception{
        JSONObject data=response.getJSONObject("hourly");JSONArray time=data.getJSONArray("time"),temp=data.getJSONArray("temperature_2m"),wind=data.optJSONArray("wind_speed_10m");Map<Long,JSONArray> slots=new TreeMap<>();
        for(int i=0;i<time.length();i++){
            long at=time.getLong(i)*1000L;if(future?at+HOUR<=now:at+HOUR>now)continue;
            double t=temp.optDouble(i,Double.NaN),w=wind==null?Double.NaN:wind.optDouble(i,Double.NaN);if(!Double.isFinite(t)||t< -90||t>65)continue;if(!Double.isFinite(w)||w<0||w>500)w=Double.NaN;
            long start=day(at);JSONArray row=slots.get(start);if(row==null){row=new JSONArray();for(int h=0;h<24;h++)row.put(JSONObject.NULL);slots.put(start,row);}row.put((int)((at-start)/HOUR),new JSONArray().put(t).put(Double.isFinite(w)?w:JSONObject.NULL));
        }
        JSONArray out=new JSONArray();for(Map.Entry<Long,JSONArray> entry:slots.entrySet())out.put(summarize(new JSONObject().put("site",site).put("from",entry.getKey()).put("to",entry.getKey()+DAY).put("hours",entry.getValue()).put("source",source).put("issuedAt",now).put("resolution","hour")));return out;
    }
    static JSONObject summarize(JSONObject row)throws Exception{
        JSONArray slots=row.getJSONArray("hours");double temp=0,wind=0,heat=0,windHeat=0;int count=0,windCount=0;
        for(int i=0;i<slots.length();i++){JSONArray slot=slots.optJSONArray(i);if(slot==null)continue;double t=slot.optDouble(0,Double.NaN),w=slot.optDouble(1,Double.NaN);if(!Double.isFinite(t))continue;count++;temp+=t;heat+=Math.max(0,18-t);if(Double.isFinite(w)){windCount++;wind+=w;windHeat+=Math.max(0,18-t)*w/20;}}
        return row.put("temperature",count>0?temp/count:JSONObject.NULL).put("heat",count>0?heat/count:JSONObject.NULL).put("wind",windCount>0?wind/windCount:JSONObject.NULL).put("windHeat",windCount>0?windHeat/windCount:JSONObject.NULL).put("observedHours",count);
    }
    static void merge(Map<String,JSONObject> history,JSONArray rows)throws Exception{
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i),old=history.get(key(row));if(old!=null&&row.has("hours")&&old.has("hours")){JSONArray a=old.getJSONArray("hours"),b=row.getJSONArray("hours");for(int j=0;j<Math.min(24,a.length());j++)if(b.isNull(j)&&!a.isNull(j))b.put(j,a.get(j));summarize(row);}history.put(key(row),row);}
    }
    static boolean covered(Map<String,JSONObject> history,String site,long from,long to,String source){
        for(long t=from;t<to;t+=HOUR){JSONObject row=history.get(site+"/"+day(t)+"/"+source+"/hour");if(row==null||row.optJSONArray("hours")==null)return false;JSONArray slot=row.optJSONArray("hours").optJSONArray((int)((t-day(t))/HOUR));if(slot==null||!Double.isFinite(slot.optDouble(0,Double.NaN)))return false;}return true;
    }
    static final class Need {final JSONObject site;final TreeSet<Long> days=new TreeSet<>();Need(JSONObject site){this.site=site;}}
    static LinkedHashMap<String,Need> needs(JSONObject journal,JSONArray visits,long now)throws Exception{
        LinkedHashMap<String,Need> out=new LinkedHashMap<>();long limit=now-1096*DAY;JSONArray events=journal.getJSONArray("events");
        // Journal places remain useful for old stationary cycles and for backfilling uncertain moves.
        for(int i=0;i<events.length();i++){JSONObject event=events.getJSONObject(i);long end=i+1<events.length()?events.getJSONObject(i+1).getLong("at"):now;add(out,event.optJSONObject("site"),Math.max(limit,event.getLong("at")),Math.min(now,end));}
        for(int i=0;i<visits.length();i++){JSONObject visit=visits.getJSONObject(i);long end=i+1<visits.length()?visits.getJSONObject(i+1).getLong("at"):now;if(visit.has("until"))end=Math.min(end,visit.getLong("until"));add(out,visit.optJSONObject("site"),Math.max(limit,visit.getLong("at")),Math.min(now,end));}
        JSONArray stays=journal.optJSONArray("confirmedStays");if(stays!=null)for(int i=0;i<stays.length();i++){JSONObject stay=stays.getJSONObject(i);add(out,stay.optJSONObject("site"),Math.max(limit,stay.getLong("from")),Math.min(now,stay.getLong("to")));}
        return out;
    }
    private static void add(LinkedHashMap<String,Need> out,JSONObject site,long from,long to)throws Exception{
        if(site==null||!site.has("key")||to<=from)return;JSONObject checked=SuppliesStore.site(site);String key=checked.getString("key");Need need=out.computeIfAbsent(key,k->new Need(checked));for(long d=day(from);d<=day(to-1);d+=DAY)need.days.add(d);
    }
    private SupplyWeatherData(){}
}
