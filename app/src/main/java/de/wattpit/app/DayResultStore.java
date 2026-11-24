package de.wattpit.app;

import android.content.Context;
import org.json.*;
import java.time.*;

/** Small last complete day views, available before any LAN or raw-input check. */
final class DayResultStore {
    private static String parent(JSONObject snapshot,JSONArray args)throws Exception{
        long from=args.getLong(0),to=args.getLong(1),now=System.currentTimeMillis();
        if(from<0||to<=from||to-from>26L*3600000)return null;
        ZoneId zone=ZoneId.systemDefault();JSONObject weather=snapshot.optJSONObject("weather");try{if(weather!=null)zone=ZoneId.of(weather.optString("timezone",zone.getId()));}catch(Exception ignored){}
        long today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli();if(from!=today)return null;
        JSONObject lan=snapshot.optJSONObject("lan");String epoch=lan==null?snapshot.optString("historyEpoch"):lan.optString("sourceEpoch");if(epoch.isEmpty())return null;
        return AnalysisResultCache.hash("day-view-v1|"+from+"|"+epoch+"|"+ViewerAnalysis.learningSignature(snapshot,new JSONObject()));
    }
    private static String part(String method,JSONArray args){return method+":"+args.optString(2)+":"+args.optLong(3)+":"+(method.equals("energyHistoryWindow")?(args.optLong(4)==args.optLong(1)?"end":args.optLong(4)):"end");}
    static synchronized void save(Context context,String namespace,JSONObject snapshot,String method,JSONArray args,JSONObject data)throws Exception{
        if(!method.equals("energyHistory")&&!method.equals("energyHistoryWindow")||data.has("error")||data.optBoolean("pending"))return;
        JSONObject dataAnalysis=data.optJSONObject("analysis"),plant=snapshot.optJSONObject("lan");String currentEpoch=plant==null?snapshot.optString("historyEpoch"):plant.optString("sourceEpoch");if(dataAnalysis!=null&&!currentEpoch.equals(dataAnalysis.optString("epoch")))return;
        JSONObject analysis=data.optJSONObject("analysis"),lan=snapshot.optJSONObject("lan");String epoch=lan==null?snapshot.optString("historyEpoch"):lan.optString("sourceEpoch");if(analysis!=null&&!epoch.equals(analysis.optString("epoch"))||data.has("sourceEpoch")&&!epoch.equals(data.optString("sourceEpoch")))return;
        String key=parent(snapshot,args);if(key==null)return;long now=System.currentTimeMillis();JSONObject saved=LearnedModelStore.read(context,namespace,"energy",key,now);
        if(saved==null)saved=new JSONObject().put("version",1).put("key",key).put("entries",new JSONObject());
        JSONObject entry=new JSONObject(data.toString());entry.put("dataAt",data.optLong("calculatedAt",now));saved.getJSONObject("entries").put(part(method,args),entry);
        while(saved.getJSONObject("entries").length()>4){String first=saved.getJSONObject("entries").keys().next();saved.getJSONObject("entries").remove(first);}
        saved.put("at",now);LearnedModelStore.save(context,namespace,"energy",key,saved,now);
    }
    static JSONObject read(Context context,String namespace,JSONObject snapshot,String method,JSONArray args)throws Exception{
        if(!method.equals("energyHistory")&&!method.equals("energyHistoryWindow"))return null;
        String key=parent(snapshot,args);if(key==null)return null;JSONObject saved=LearnedModelStore.read(context,namespace,"energy",key,System.currentTimeMillis());if(saved==null)return null;
        JSONObject data=saved.getJSONObject("entries").optJSONObject(part(method,args));if(data==null)return null;
        data.put("stale",true).put("pending",true);for(String kind:new String[]{"solar","consumption","balance","mobile"})if(data.optJSONObject(kind)!=null)data.getJSONObject(kind).put("stale",true).put("pending",true).put("dataAt",data.optLong("dataAt"));return data;
    }
    private DayResultStore(){}
}
