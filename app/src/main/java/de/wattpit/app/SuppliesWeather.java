package de.wattpit.app;

import org.json.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Infrequent weather collection on the data host; supply data never leaves the device. */
final class SuppliesWeather {
    private static final ExecutorService NETWORK=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private static final AtomicLong ATTEMPT=new AtomicLong();
    static boolean busy(){return BUSY.get();}
    private static final long DAY=SupplyWeatherData.DAY,HOUR=SupplyWeatherData.HOUR;
    static void refresh(Repository repo,boolean force){
        try{SuppliesStore.recordSite(repo);}catch(Exception ignored){}
        long now=System.currentTimeMillis();if(!force&&now-ATTEMPT.get()<900000L)return;
        if(!SupplyLocations.automatic(repo)&&repo.prefs.getString("weatherPlace","").isEmpty()||!BUSY.compareAndSet(false,true))return;ATTEMPT.set(now);
        NETWORK.execute(()->{
            try{
                JSONObject journal=SuppliesStore.read(repo.prefs),saved=new JSONObject(repo.prefs.getString(SuppliesStore.WEATHER,"{}")),current=SupplyLocations.current(repo);
                JSONArray visits=new JSONArray(repo.prefs.getString(SupplyLocations.SITES,"[]"));
                LinkedHashMap<String,SupplyWeatherData.Need> needs=SupplyWeatherData.needs(journal,visits,now);
                StringBuilder signature=new StringBuilder();for(Map.Entry<String,SupplyWeatherData.Need> e:needs.entrySet())signature.append(e.getKey()).append(e.getValue().days);
                String basis=signature.toString(),siteKey=current.optString("key");
                if(!force&&saved.optInt("version")==2&&!saved.optBoolean("incomplete",true)&&saved.optLong("journalRevision",-1)==journal.optLong("revision")&&saved.optString("needSignature").equals(basis)&&saved.optString("siteKey").equals(siteKey)&&now-saved.optLong("updated")<HOUR)return;
                Map<String,JSONObject> history=new TreeMap<>();JSONArray prior=saved.optJSONArray("history");if(prior!=null)SupplyWeatherData.merge(history,prior);
                JSONArray forecast=new JSONArray();long forecastAt=saved.optLong("forecastUpdated",saved.optLong("updated"));boolean incomplete=false;int calls=0;long today=SupplyWeatherData.day(now),recentStart=today-14*DAY,archiveEnd=today-7*DAY;
                if(!siteKey.isEmpty())try{
                    JSONObject recent=WeatherClient.get(url(current,15,14));
                    SupplyWeatherData.merge(history,SupplyWeatherData.hours(recent,siteKey,"recent",now,false));
                    forecast=SupplyWeatherData.hours(recent,siteKey,"forecast",now,true);forecastAt=now;
                }catch(Exception missing){incomplete=true;if(siteKey.equals(saved.optString("siteKey")))forecast=saved.optJSONArray("forecast")==null?new JSONArray():saved.getJSONArray("forecast");}
                // A bounded round-robin queue also covers stops with no gas or water transaction.
                List<String> jobs=new ArrayList<>();Map<String,JSONObject> places=new HashMap<>();
                for(Map.Entry<String,SupplyWeatherData.Need> entry:needs.entrySet()){
                    String key=entry.getKey();SupplyWeatherData.Need need=entry.getValue();places.put(key,need.site);boolean recentMissing=false;long chunk=-1,last=-1;
                    for(long day:need.days){
                        if(day>=recentStart&&day<=today&&!key.equals(siteKey)&&!covered(history,key,day,Math.min(day+DAY,Math.floorDiv(now,HOUR)*HOUR)))recentMissing=true;
                        if(day>archiveEnd)continue;
                        if(SupplyWeatherData.covered(history,key,day,day+DAY,"archive")){if(chunk>=0){jobs.add(key+"|"+chunk+"|"+last);chunk=-1;}continue;}
                        if(chunk<0)chunk=day;else if(day>last+DAY||day-chunk>=90*DAY){jobs.add(key+"|"+chunk+"|"+last);chunk=day;}last=day;
                    }
                    if(recentMissing)jobs.add(key+"|recent");if(chunk>=0)jobs.add(key+"|"+chunk+"|"+last);
                }
                int start=jobs.isEmpty()?0:Math.floorMod(saved.optInt("backfillCursor"),jobs.size());
                for(int n=0;n<Math.min(5,jobs.size());n++){
                    String[] job=jobs.get((start+n)%jobs.size()).split("\\|");JSONObject place=places.get(job[0]);calls++;
                    try{JSONObject result=job[1].equals("recent")?WeatherClient.get(url(place,1,14)):WeatherClient.get("https://archive-api.open-meteo.com/v1/archive?latitude="+place.getDouble("latitude")+"&longitude="+place.getDouble("longitude")+"&start_date="+Instant.ofEpochMilli(Long.parseLong(job[1])).atZone(ZoneOffset.UTC).toLocalDate()+"&end_date="+Instant.ofEpochMilli(Long.parseLong(job[2])).atZone(ZoneOffset.UTC).toLocalDate()+"&timeformat=unixtime&timezone=UTC&wind_speed_unit=kmh&hourly=temperature_2m,wind_speed_10m");SupplyWeatherData.merge(history,SupplyWeatherData.hours(result,job[0],job[1].equals("recent")?"recent":"archive",now,false));}
                    catch(Exception unavailable){incomplete=true;}
                }
                if(jobs.size()>calls)incomplete=true;
                for(Map.Entry<String,SupplyWeatherData.Need> e:needs.entrySet())for(long d:e.getValue().days)if(!covered(history,e.getKey(),d,Math.min(d+DAY,Math.floorDiv(now,HOUR)*HOUR)))incomplete=true;
                JSONArray historic=new JSONArray();for(JSONObject row:history.values())historic.put(row);
                JSONArray issued=saved.optJSONArray("forecastArchive"),kept=new JSONArray();String issuedDay=Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate().toString();if(issued!=null)for(int i=Math.max(0,issued.length()-119);i<issued.length();i++){JSONObject row=issued.getJSONObject(i);if(!issuedDay.equals(row.optString("day"))||!siteKey.equals(row.optString("site")))kept.put(row);}
                if(!siteKey.isEmpty())kept.put(new JSONObject().put("day",issuedDay).put("site",siteKey).put("issuedAt",now).put("days",forecast));
                JSONObject next=new JSONObject().put("version",2).put("timezone","UTC").put("siteKey",siteKey).put("place",current).put("updated",now).put("forecastUpdated",forecastAt).put("journalRevision",journal.optLong("revision")).put("needSignature",basis).put("incomplete",incomplete).put("backfillCursor",start+calls).put("history",historic).put("forecast",forecast).put("forecastArchive",kept);
                // Raw historic rows are valid even if the phone moved during the request.
                if(!siteKey.equals(SupplyLocations.current(repo).optString("key")))next.put("forecast",new JSONArray()).put("siteKey","").put("incomplete",true);
                android.content.SharedPreferences.Editor edit=repo.prefs.edit().putString(SuppliesStore.WEATHER,next.toString());if(incomplete)edit.putString("suppliesWeatherError","Historisches Wetter für einzelne Aufenthalte fehlt noch; der Rückblick wird schrittweise ergänzt.");else edit.remove("suppliesWeatherError");edit.commit();
            }catch(Exception e){repo.prefs.edit().putString("suppliesWeatherError","Wetterabruf gerade nicht möglich. Gespeicherte Wetterwerte bleiben erhalten; fehlende Werte werden später ergänzt.").apply();}
            finally{BUSY.set(false);}
        });
    }
    private static String url(JSONObject site,int future,int past)throws Exception{return "https://api.open-meteo.com/v1/forecast?latitude="+site.getDouble("latitude")+"&longitude="+site.getDouble("longitude")+"&forecast_days="+future+"&past_days="+past+"&timeformat=unixtime&timezone=UTC&wind_speed_unit=kmh&hourly=temperature_2m,wind_speed_10m";}
    private static boolean covered(Map<String,JSONObject> history,String site,long from,long to){for(long h=from;h<to;h+=HOUR)if(!SupplyWeatherData.covered(history,site,h,h+HOUR,"archive")&&!SupplyWeatherData.covered(history,site,h,h+HOUR,"recent"))return false;return true;}
    static JSONArray days(JSONObject response,String site,String source,long issuedAt)throws Exception {
        JSONObject hourly=response.getJSONObject("hourly");JSONArray time=hourly.getJSONArray("time"),temp=hourly.getJSONArray("temperature_2m"),wind=hourly.getJSONArray("wind_speed_10m");
        ZoneId zone=ZoneId.of(response.getString("timezone"));Map<LocalDate,double[]> sums=new TreeMap<>();
        for(int i=0;i<time.length();i++){if(temp.isNull(i)||wind.isNull(i))continue;double t=temp.getDouble(i),w=wind.getDouble(i);if(!Double.isFinite(t)||!Double.isFinite(w)||t< -90||t>65||w<0||w>500)continue;LocalDate date=Instant.ofEpochMilli(time.getLong(i)*1000L).atZone(zone).toLocalDate();double[] s=sums.computeIfAbsent(date,d->new double[5]);double heat=Math.max(0,18-t);s[0]++;s[1]+=t;s[2]+=w;s[3]+=heat;s[4]+=heat*w/20;}
        JSONArray out=new JSONArray();for(Map.Entry<LocalDate,double[]> entry:sums.entrySet()){double[] s=entry.getValue();long from=entry.getKey().atStartOfDay(zone).toInstant().toEpochMilli(),to=entry.getKey().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();if(s[0]*3600000L<.9*(to-from))continue;out.put(new JSONObject().put("from",from).put("to",to).put("site",site).put("temperature",s[1]/s[0]).put("wind",s[2]/s[0]).put("heat",s[3]/s[0]).put("windHeat",s[4]/s[0]).put("source",source).put("issuedAt",issuedAt));}return out;
    }
    private SuppliesWeather(){}
}
