package de.wattpit.app;

import android.content.ContentValues;
import org.json.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Derived knowledge persistence and SQL batching; no physical handset latency claim. */
public final class QuickStartProof {
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS "+label);}
    private static JSONObject data(String source,long now)throws Exception{return new JSONObject().put("source",source).put("updated",now-60000).put("observations",new JSONArray().put(new JSONObject().put("from",now-3600000).put("to",now-60000).put("key","fixture-context").put("wh",250))).put("forecasts",new JSONArray());}
    private static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path root=Path.of(args[0]);long now=System.currentTimeMillis();
        LanSyncProof.TestContext context=new LanSyncProof.TestContext(root);Repository repo=LanSyncProof.repository(context);
        JSONArray devices=new JSONArray("[{\"kind\":\"solar\",\"mac\":\"fixture-solar\"},{\"kind\":\"shunt\",\"mac\":\"fixture-shunt\"}]");repo.prefs.edit().putString("devices",devices.toString()).commit();
        String source=SolarPlanning.selectedSource(devices),epoch=HistoryExchange.epoch(repo.db),name="fixture-planning.db";JSONObject original=data(source,now);
        AnalysisResultCache derived=new AnalysisResultCache(context,"fixture-plant");JSONObject curve=new JSONObject().put("points",new JSONArray().put(new JSONObject().put("v",42)));
        derived.save("solar-today","source-one",curve);
        check(new AnalysisResultCache(new LanSyncProof.TestContext(root),"fixture-plant").read("solar-today","source-one").getJSONArray("points").getJSONObject(0).getInt("v")==42,"calculated curves survive a new Context without recalculation");
        check(derived.read("solar-today","source-two")==null&&new AnalysisResultCache(context,"another-plant").read("solar-today","source-one")==null,"changed inputs and another plant cannot reuse the result");
        derived.save("solar-today","source-one",new JSONObject().put("error","offline"));check(derived.read("solar-today","source-one")!=null,"a failed recalculation preserves the previous completed result");
        SolarPreviewCache.save(context,name,epoch,original);
        JSONObject read=SolarPreviewCache.read(new LanSyncProof.TestContext(root),name,source,epoch,now);
        check(read!=null&&read.getBoolean("cached")&&read.getBoolean("stale")&&read.getJSONArray("observations").toString().equals(original.getJSONArray("observations").toString()),"derived knowledge survives a new Context without network or a history rebuild");
        check(!original.has("cached")&&SolarPreviewCache.read(context,"other-plant.db",source,epoch,now)==null,"cache reads cannot mutate the fresh result or cross plant filenames");
        check(SolarPreviewCache.read(context,name,"other-source",epoch,now)==null&&SolarPreviewCache.read(context,name,source,"other-epoch",now)==null,"changed device sources and replaced source databases reject incompatible priors");
        SolarPreviewCache.save(context,name,epoch,new JSONObject().put("error","fixture-error"));check(SolarPreviewCache.read(context,name,source,epoch,now)!=null,"a failed refresh preserves the last valid derived knowledge");
        SolarPreviewCache.save(context,name,epoch,new JSONObject().put("observations",new JSONArray()));check(SolarPreviewCache.read(context,name,source,epoch,now)==null,"a successful empty result withdraws the previous cached knowledge");
        SolarPreviewCache.save(context,name,epoch,data(source,now+3600000));check(SolarPreviewCache.read(context,name,source,epoch,now)==null,"future-dated cached knowledge is not used");
        Path file=root.resolve("solar-preview").resolve(name+".json");Files.writeString(file,"{broken");check(SolarPreviewCache.read(context,name,source,epoch,now)==null,"a damaged disposable cache is treated as a cache miss");
        Files.write(file,new byte[SolarPreviewCache.LIMIT+1]);check(SolarPreviewCache.read(context,name,source,epoch,now)==null,"oversized cached data are rejected before parsing");
        boolean invalid=false;try{SolarPreviewCache.save(context,"../outside",epoch,original);}catch(java.io.IOException expected){invalid=true;}check(invalid,"cache paths are restricted to app-private filenames");
        SolarPreviewCache.clear(context,name);
        SolarPlanning plan=new SolarPlanning(context,repo,x->{},name,null);plan.getWritableDatabase();
        ContentValues observation=new ContentValues();observation.put("source",source);observation.put("start",now-3600000);observation.put("end",now-60000);observation.put("payload",original.getJSONArray("observations").getJSONObject(0).toString());plan.getWritableDatabase().insertOrThrow("observations",null,observation);
        repo.prefs.edit().putInt("planningEnergyVersion."+source,SolarPlanning.ALGORITHM).commit();
        check(plan.cachedKnowledge(source,epoch).getJSONArray("observations").length()==1,"an existing installation reads its derived SQL ledger before its first new cache exists");
        plan.clearPreview();repo.prefs.edit().putInt("planningEnergyVersion."+source,SolarPlanning.ALGORITHM-1).commit();check(plan.cachedKnowledge(source,epoch)==null,"legacy SQL fallback requires the current learning algorithm");
        LanSync lan=LanSyncProof.sync(context);String plant="ABCDEFGHIJKLMNOPQRSTUV";set(lan,"config",new JSONObject().put("role","viewer").put("id",plant));
        set(lan,"remote",new JSONObject().put("analysisEpoch",epoch).put("snapshot",new JSONObject().put("devices",devices)));
        Method analysis=LanSync.class.getDeclaredMethod("analysis");analysis.setAccessible(true);ViewerAnalysis replica=(ViewerAnalysis)analysis.invoke(lan);
        replica.repo.prefs.edit().putString("devices",devices.toString()).putString("analysis.epoch",epoch).commit();SolarPreviewCache.save(context,"wattpit-view-"+plant+"-planning.db",epoch,original);
        set(replica,"operationFocus","previous-view");lan.displayPriority("detail-view");Method priority=ViewerAnalysis.class.getDeclaredMethod("priority");priority.setAccessible(true);
        boolean yielded=false;try{priority.invoke(replica);}catch(InvocationTargetException failure){yielded=failure.getCause() instanceof java.io.InterruptedIOException;}
        check(yielded,"a changed view cancels old analysis before another data packet is fetched");
        set(replica,"operationFocus",lan.displayFocus());set(replica,"learningOperation",true);set(replica,"operationDemand",lan.displayDemand());lan.displayHistoryRequested();yielded=false;
        try{priority.invoke(replica);}catch(InvocationTargetException failure){yielded=failure.getCause() instanceof java.io.InterruptedIOException;}
        check(yielded,"a newly requested foreground curve interrupts the background learning transfer");
        set(replica,"learningOperation",false);
        ExecutorService reads=Executors.newSingleThreadExecutor();try{
            synchronized(replica){Future<JSONObject> fast=reads.submit(lan::cachedKnowledge);JSONObject cached=fast.get(2,TimeUnit.SECONDS);check(cached!=null&&cached.getJSONObject("analysis").getString("plant").equals(plant),"a cached preview does not wait behind the viewer analysis monitor used by network histories");}
            ((JSONObject)get(lan,"remote")).put("analysisEpoch","replacement");check(lan.cachedKnowledge()==null,"a live source epoch change immediately suppresses an old local prior");
        }finally{reads.shutdownNow();LanSyncProof.close(lan);}
        long from=now-86400000,to=now;RollerLedger ledger=RollerLedger.get(context);int count=2301;plan.getWritableDatabase().beginTransaction();try{
            for(int i=0;i<count;i++){ContentValues value=new ContentValues();value.put("place","fixture");value.put("geometry","fixture");value.put("end",from+(i+1)*1000);value.put("value",0.25);value.put("slot",0);plan.getWritableDatabase().insertOrThrow("model_weather",null,value);}plan.getWritableDatabase().setTransactionSuccessful();
        }finally{plan.getWritableDatabase().endTransaction();}
        String version=AnalysisExchange.version("model_weather",repo,plan,ledger,from,to);JSONArray legacy=new JSONArray(),batched=new JSONArray();long cursor=0;int oldRequests=0,newRequests=0;
        while(true){JSONObject page=AnalysisExchange.page(context,repo,plan,"model_weather",from,to,version,cursor);oldRequests++;append(legacy,page.getJSONArray("rows"));cursor=page.getLong("cursor");if(page.getBoolean("complete"))break;}
        cursor=0;boolean done=false;while(!done){JSONObject reply=AnalysisExchange.batch(context,repo,plan,"model_weather",from,to,version,cursor);newRequests++;JSONArray pages=reply.getJSONArray("pages");check(pages.length()<=AnalysisExchange.BATCH_PAGES&&reply.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=AnalysisExchange.BATCH_BYTES,"model responses remain bounded by pages and bytes");for(int i=0;i<pages.length();i++){JSONObject page=pages.getJSONObject(i);append(batched,page.getJSONArray("rows"));cursor=page.getLong("cursor");done=page.getBoolean("complete");}}
        check(legacy.toString().equals(batched.toString())&&batched.length()==count&&newRequests<oldRequests,"bounded model batches preserve all typed values with fewer requests");
        plan.getWritableDatabase().delete("model_weather","end=?",new String[]{""+(from+1000)});check(AnalysisExchange.batch(context,repo,plan,"model_weather",from,to,version,0).getJSONArray("pages").getJSONObject(0).getBoolean("changed"),"a changed model version invalidates an unfinished batch instead of installing mixed data");
        System.out.println("INFO model_page_requests="+oldRequests+" model_batch_requests="+newRequests+" rows="+count);
        plan.shutdown();repo.db.close();
    }
    private static Object get(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void append(JSONArray target,JSONArray values)throws Exception{for(int i=0;i<values.length();i++)target.put(values.get(i));}
}
