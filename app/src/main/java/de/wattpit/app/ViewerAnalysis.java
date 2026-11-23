package de.wattpit.app;

import android.content.*;
import org.json.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import de.wattpit.app.core.EnergyIntegral;

/** One plant replica per display handset. It never imports into that handset's own plant. */
final class ViewerAnalysis {
    private final LanSync lan;
    private final AnalysisResultCache results;
    private JSONArray operationArgs;private String operationFocus;private long operationDemand;private boolean learningOperation,comparisonsOnly;
    final Repository repo;
    final SolarPlanning planning;
    private final RollerLedger ledger;
    private final Object identity;
    private final String plant;
    private Consumer<String> progress;
    private volatile JSONObject metadata=new JSONObject();
    private volatile JSONObject rollerObservations=new JSONObject();
    private String usageRevision="";private long usageAt;
    JSONObject rollerObservations(){return rollerObservations;}
    private long progressAt;
    private volatile String epoch="";
    private long modelAt,modelFrom,modelThrough;private String lastModelOperation="";
    private long rawRevision,downloadedRows,downloadedBlocks;
    private long networkNanos,storageNanos,operationNanos,computeNanos,connectNanos;private int requests;private String operation="";private boolean completed;
    private volatile JSONObject lastTiming=new JSONObject();
    ViewerAnalysis(Context context,LanSync lan,String id){
        if(!id.matches("[A-Za-z0-9_-]{22}"))throw new IllegalArgumentException("Ungültige Anlage.");
        this.lan=lan;results=new AnalysisResultCache(context,id);identity=lan.identity();plant=id;String prefix="wattpit-view-"+id;
        repo=Repository.analysis(context,prefix);ledger=new RollerLedger(context,prefix+"-roller.db");
        planning=new SolarPlanning(context,repo,value->{},prefix+"-planning.db",ledger);
        // Open with this Context, before any request can switch the selected plant.
        repo.db.getWritableDatabase();planning.getWritableDatabase();ledger.getWritableDatabase();
        try{rollerObservations=new JSONObject(repo.prefs.getString("analysis.rollerUsage","{}"));}catch(JSONException ignored){}
    }
    private void priority()throws IOException{
        if(operationFocus!=null&&!operationFocus.equals(lan.displayFocus())||learningOperation&&operationDemand!=lan.displayDemand())throw new java.io.InterruptedIOException("Die geöffnete Ansicht erhält Vorrang. Der bisherige Lernstand bleibt erhalten.");
    }
    private JSONObject get(String method,JSONArray args)throws Exception{
        priority();
        if(identity!=lan.identity()||!lan.viewer())throw new IOException("Anlage wurde gewechselt.");
        long started=System.nanoTime();JSONObject value;try{value=lan.queryInput(method,args);}finally{networkNanos+=System.nanoTime()-started;connectNanos+=lan.lastConnectNanos();requests++;}
        if(identity!=lan.identity()||!lan.viewer())throw new IOException("Anlage wurde gewechselt.");return value;
    }
    private void note(String value){long now=System.currentTimeMillis();if(progress!=null&&(now-progressAt>=300||value.equals("Auswertung auf diesem Handy …"))){progressAt=now;progress.accept(value);}}
    private void reset(String next)throws Exception{
        modelAt=0;
        planning.clearPreview();
        HistoryExchange.reset(repo.db);rollerObservations=new JSONObject();usageRevision="";usageAt=0;
        for(String table:AnalysisExchange.ALL)AnalysisExchange.database(table,repo,planning,ledger,true).delete(table,"1=1",null);
        SharedPreferences.Editor edit=repo.prefs.edit();for(String key:repo.prefs.getAll().keySet())edit.remove(key);edit.putString("analysis.epoch",next).commit();epoch=next;
    }
    private void raw(long from,long to)throws Exception{
        for(long start=from;start<to;start+=90L*86400000){
            JSONObject manifest=get("analysisManifest",new JSONArray().put(start).put(Math.min(to,start+90L*86400000)));
            String next=manifest.getString("epoch");if(!next.equals(repo.prefs.getString("analysis.epoch","")))reset(next);epoch=next;rawRevision=manifest.getLong("revision");
            JSONArray blocks=manifest.getJSONArray("blocks");
            if(lan.analysisBatch()){rawBatch(blocks);continue;}
            for(int i=0;i<blocks.length();i++){
                JSONArray b=blocks.getJSONArray(i);long block=b.getLong(0),revision=b.getLong(1),count=b.getLong(2);
                rawRevision=Math.max(rawRevision,revision);if(HistoryExchange.cachedVersion(repo.db,block)==revision)continue;
                note("Messdaten vom Hauptgerät · Abschnitt "+(i+1)+" von "+blocks.length());
                boolean installed=false;
                for(int attempt=0;attempt<3&&!installed;attempt++){
                    JSONArray rows=new JSONArray();long cursor=0;boolean changed=false;
                    if(count>100000)throw new IOException("Datenabschnitt ist zu groß.");
                    if(count>0)while(true){
                        JSONObject page=get("analysisPage",new JSONArray().put(block).put(revision).put(cursor));
                        if(page.optBoolean("changed")){changed=true;break;}
                        JSONArray values=page.getJSONArray("rows");for(int r=0;r<values.length();r++)rows.put(values.get(r));
                        long nextCursor=page.getLong("cursor");downloadedRows+=values.length();
                        if(page.getBoolean("complete"))break;
                        if(nextCursor<=cursor||rows.length()>count)throw new IOException("Unvollständige Messdatenübertragung.");cursor=nextCursor;note("Messdaten werden abgeglichen …");
                    }
                    if(!changed){long saving=System.nanoTime();HistoryExchange.replace(repo.db,block,revision,count,rows);storageNanos+=System.nanoTime()-saving;downloadedBlocks++;installed=true;}
                    else{
                        JSONObject fresh=get("analysisManifest",new JSONArray().put(block*HistoryExchange.BLOCK).put((block+1)*HistoryExchange.BLOCK));
                        if(!epoch.equals(fresh.getString("epoch")))throw new IOException("Der Datenbestand der Zentrale wurde gewechselt. Bitte erneut laden.");
                        JSONArray changedBlocks=fresh.getJSONArray("blocks");if(changedBlocks.length()==0){revision=0;count=0;}else{revision=changedBlocks.getJSONArray(0).getLong(1);count=changedBlocks.getJSONArray(0).getLong(2);}
                    }
                }
                if(!installed)throw new IOException("Messdaten ändern sich gerade. Die letzten Werte bleiben erhalten.");
            }
        }
    }
    private static final class PendingBlock{
        long block,revision,count,cursor;int attempts;JSONArray rows=new JSONArray();
        PendingBlock(JSONArray b)throws Exception{block=b.getLong(0);revision=b.getLong(1);count=b.getLong(2);if(count<0||count>100000)throw new IOException("Datenabschnitt ist zu groß.");}
    }
    private void rawBatch(JSONArray blocks)throws Exception{
        Map<Long,Long> saved=HistoryExchange.cachedVersions(repo.db,blocks.length()==0?0:blocks.getJSONArray(0).getLong(0)*HistoryExchange.BLOCK,blocks.length()==0?1:(blocks.getJSONArray(blocks.length()-1).getLong(0)+1)*HistoryExchange.BLOCK);
        ArrayDeque<PendingBlock> pending=new ArrayDeque<>();for(int i=0;i<blocks.length();i++){JSONArray b=blocks.getJSONArray(i);rawRevision=Math.max(rawRevision,b.getLong(1));if(!Objects.equals(saved.get(b.getLong(0)),b.getLong(1)))pending.add(new PendingBlock(b));}
        while(!pending.isEmpty()){
            List<PendingBlock> active=new ArrayList<>();JSONArray ask=new JSONArray();
            while(active.size()<8&&!pending.isEmpty()){if(!active.isEmpty()&&(active.get(0).count>4096||pending.peekFirst().count>4096))break;PendingBlock b=pending.removeFirst();active.add(b);ask.put(new JSONArray().put(b.block).put(b.revision).put(b.cursor));}
            JSONArray pages=get("analysisBatch",new JSONArray().put(ask)).getJSONArray("pages");if(pages.length()!=active.size())throw new IOException("Unvollständige Messdatenübertragung.");
            for(int i=0;i<active.size();i++){
                PendingBlock b=active.get(i);JSONObject page=pages.getJSONObject(i);
                if(page.optBoolean("changed")){
                    if(++b.attempts>3)throw new IOException("Messdaten ändern sich gerade. Die letzten Werte bleiben erhalten.");
                    JSONObject fresh=get("analysisManifest",new JSONArray().put(b.block*HistoryExchange.BLOCK).put((b.block+1)*HistoryExchange.BLOCK-1));if(!epoch.equals(fresh.getString("epoch")))throw new IOException("Der Datenbestand der Zentrale wurde gewechselt. Bitte erneut laden.");
                    JSONArray current=fresh.getJSONArray("blocks");b.revision=0;b.count=0;for(int k=0;k<current.length();k++){JSONArray v=current.getJSONArray(k);if(v.getLong(0)==b.block){b.revision=v.getLong(1);b.count=v.getLong(2);}}
                    if(b.count<0||b.count>100000)throw new IOException("Datenabschnitt ist zu groß.");b.cursor=0;b.rows=new JSONArray();pending.addFirst(b);continue;
                }
                if(page.getLong("block")!=b.block||page.getLong("revision")!=b.revision||page.getLong("n")!=b.count)throw new IOException("Falscher Messdatenabschnitt.");JSONArray rows=page.getJSONArray("rows");for(int k=0;k<rows.length();k++)b.rows.put(rows.get(k));downloadedRows+=rows.length();
                long next=page.getLong("cursor");if(b.rows.length()>b.count||(!page.getBoolean("complete")&&next<=b.cursor))throw new IOException("Unvollständige Messdatenübertragung.");b.cursor=next;
                if(page.getBoolean("complete")){long start=System.nanoTime();HistoryExchange.replace(repo.db,b.block,b.revision,b.count,b.rows);storageNanos+=System.nanoTime()-start;downloadedBlocks++;}else pending.addFirst(b);
            }note("Messdaten werden abgeglichen · "+downloadedBlocks+" Abschnitte gespeichert");
        }
    }
    private void beginTiming(String name){operation=name;completed=false;networkNanos=storageNanos=computeNanos=connectNanos=0;requests=0;operationNanos=System.nanoTime();}
    private void endTiming(){try{long total=System.nanoTime()-operationNanos;lastTiming=new JSONObject().put("method",operation).put("modelFrom",modelFrom).put("modelThrough",modelThrough).put("at",System.currentTimeMillis()).put("completed",completed).put("totalMs",total/1000000).put("networkMs",networkNanos/1000000).put("connectionMs",connectNanos/1000000).put("storageMs",storageNanos/1000000).put("calculationMs",computeNanos/1000000).put("localMs",Math.max(0,total-networkNanos-storageNanos)/1000000).put("requests",requests).put("rows",downloadedRows).put("blocks",downloadedBlocks).put("batch",lan.analysisBatch()).put("modelBatch",lan.analysisModelBatch());repo.prefs.edit().putString("analysis.timing",lastTiming.toString()).apply();}catch(Exception ignored){}}
    private void model(long from,long to)throws Exception{model(from,to,true);}
    private void model(long from,long to,boolean learning)throws Exception{
        long now=System.currentTimeMillis();
        // Ninety-day BMS learning is independent of a visible five-day chart's inputs.
        from=(learning?Math.min(from,now-90L*86400000):from)/86400000L*86400000L;to=Math.min((to+86399999)/86400000L*86400000L,(now/86400000L+1)*86400000L);
        if(!learning&&lastModelOperation.equals(operation)&&modelAt>0&&now>=modelAt&&now-modelAt<5000&&from>=modelFrom&&to<=modelThrough){
            JSONObject current=get("analysisInputState",new JSONArray());
            if(current.optString("epoch").equals(epoch)&&current.getString("token").equals(metadata.optString("inputToken")))return;
            modelAt=0;
        }
        for(int retry=0;retry<3;retry++){
            JSONObject m=get("analysisMetadata",new JSONArray().put(from).put(to));
            if(m.optBoolean("changed"))continue;
            if(m.getInt("algorithm")!=SolarPlanning.ALGORITHM)throw new IOException("Bitte Hauptgerät und Anzeige auf dieselbe AutarPie-Version aktualisieren.");
            JSONObject versions=m.getJSONObject("tables");boolean changed=false;long dirtyRollerFrom=Long.MAX_VALUE,dirtyRollerTo=0;
            for(String table:AnalysisExchange.ALL){
                if(comparisonsOnly&&!table.equals("forecasts"))continue;
                if(!learning&&!comparisonsOnly&&(table.equals("observations")||table.equals("forecasts")||table.equals("confirmed_setups")))continue;
                if(!learning&&table.equals("roller_raw")&&!operation.equals("rollerEnergy"))continue;
                if(!learning&&table.equals("load_raw")&&!operation.equals("loadHistory"))continue;
                if(!learning&&table.equals("charger_raw")&&!operation.equals("chargerJournal"))continue;
                if(!versions.has(table)&&table.equals("charger_raw"))continue;
                if(!versions.has(table))throw new IOException("Bitte Zentrale und Anzeigen auf dieselbe AutarPie-Version aktualisieren.");
                String version=versions.getString(table),scope=(table.equals("model_weather")||table.equals("device_settings")||table.equals("input_intervals")||table.equals("roller_raw")||(table.equals("load_raw")||table.equals("charger_raw")))?":"+from+":"+to:"";
                String key="analysis.table."+table+scope;boolean incremental=AnalysisExchange.incremental(table);
                if(version.equals(repo.prefs.getString(key,"")))continue;
                String cursorKey="analysis.cursor."+table+scope;long cursor=incremental?repo.prefs.getLong(cursorKey,0):0;JSONArray rows=new JSONArray();
                note("Berechnungsgrundlagen werden abgeglichen …");
                while(true){
                    boolean batch=lan.analysisModelBatch(),complete=false;JSONObject response=get(batch?"analysisModelBatch":"analysisModelPage",new JSONArray().put(table).put(from).put(to).put(version).put(cursor));
                    JSONArray pages=batch?response.getJSONArray("pages"):new JSONArray().put(response);
                    if(pages.length()==0||pages.length()>AnalysisExchange.BATCH_PAGES)throw new IOException("Unvollständige Berechnungsgrundlagen.");
                    for(int p=0;p<pages.length();p++){
                        JSONObject page=pages.getJSONObject(p);if(page.optBoolean("changed")){changed=true;break;}
                        JSONArray values=page.getJSONArray("rows");for(int i=0;i<values.length();i++)rows.put(values.get(i));
                        long next=page.getLong("cursor");complete=page.getBoolean("complete");
                        if(next<cursor||!complete&&next==cursor||rows.length()>250000)throw new IOException("Unvollständige Berechnungsgrundlagen.");cursor=next;
                        if(complete)break;
                    }
                    if(changed||complete)break;note("Berechnungsgrundlagen werden abgeglichen …");
                }
                if(changed)break;
                long saving=System.nanoTime();AnalysisExchange.install(table,repo,planning,ledger,from,to,rows);storageNanos+=System.nanoTime()-saving;if(table.equals("roller_raw"))for(int i=0;i<rows.length();i++){JSONArray row=rows.getJSONArray(i);dirtyRollerFrom=Math.min(dirtyRollerFrom,row.getLong(1));dirtyRollerTo=Math.max(dirtyRollerTo,row.getLong(2));}
                SharedPreferences.Editor edit=repo.prefs.edit().putString(key,version);if(incremental)edit.putLong(cursorKey,cursor);edit.commit();
            }
            if(changed)continue;
            JSONObject settings=m.getJSONObject("settings");SharedPreferences.Editor edit=repo.prefs.edit();
            for(Iterator<String> it=settings.keys();it.hasNext();){String key=it.next();if(key.equals("capacity"))edit.putFloat(key,(float)settings.getDouble(key));else if(key.equals("sampleSeconds"))edit.putInt(key,settings.getInt(key));else edit.putString(key,settings.getString(key));}
            if(!repo.prefs.getBoolean("analysis.seeded",false)){
                JSONObject markers=m.getJSONObject("markers");for(Iterator<String> it=markers.keys();it.hasNext();){String key=it.next();Object value=markers.get(key);if(value instanceof Number){if(key.startsWith("planningEnergyVersion."))edit.putInt(key,((Number)value).intValue());else edit.putLong(key,((Number)value).longValue());}else edit.putString(key,value.toString());}
                edit.putBoolean("analysis.seeded",true);
            }
            edit.putString("analysisTimezone",m.getString("timezone")).commit();if(dirtyRollerTo>dirtyRollerFrom)ledger.derive(dirtyRollerFrom,dirtyRollerTo);metadata=m;
            // At most 90 days; the central remains a raw-data collector.
            JSONObject roller=new JSONObject(lan.part("roller")),profile=roller.optJSONObject("profile");
            String usageKey="partial-energy-v2|"+(profile==null?"":profile.optString("mac"))+"|"+roller.optLong("sessionRevision")+"|"+versions.optString("roller_raw")+"|"+versions.optString("load_raw");long usageNow=System.currentTimeMillis();
            if(learning&&profile!=null&&!profile.optString("mac").isEmpty()&&(!usageKey.equals(usageRevision)||usageNow-usageAt>300000||rollerObservations.length()==0)){
                rollerObservations=ledger.observedDays(RollerLedger.source(repo.prefs),profile.optString("mac"),Math.max(0,usageNow-90L*86400000),usageNow,m.getString("timezone"));
                rollerObservations.put("learning",ledger.usageLearning(RollerLedger.source(repo.prefs),roller,usageNow,m.getString("timezone")));
                repo.prefs.edit().putString("analysis.rollerUsage",rollerObservations.toString()).apply();usageRevision=usageKey;usageAt=usageNow;
            }
            modelAt=System.currentTimeMillis();lastModelOperation=operation;modelFrom=from;modelThrough=to;return;
        }
        throw new IOException("Die Berechnungsgrundlagen ändern sich gerade. Bitte erneut versuchen.");
    }
    private void requireProtocol()throws Exception{
        if(lan.analysisProtocol()!=1)throw new IOException("Bitte zuerst das Hauptgerät auf AutarPie 1.7.0 oder neuer aktualisieren.");
    }
    private static long support(long from){return Math.max(0,from/86400000L*86400000L-4L*86400000-EnergyIntegral.LOOKAROUND);}
    private void prepare(long from,long to)throws Exception{
        requireProtocol();boolean modelled=operation.startsWith("energy")||operation.equals("dailyEnergy")||operation.equals("dashboardHistory")&&operationArgs!=null&&operationArgs.optString(2).equals("mobileSolarPower")||operation.equals("history")&&operationArgs!=null&&Arrays.asList("mobileSolarPower","mobileSolarEnergy","consumptionEnergy","flowSolarEnergy","flowBalanceEnergy").contains(operationArgs.optString(2));
        long a=modelled?support(from):Math.max(0,from-EnergyIntegral.LOOKAROUND),b=Math.min(System.currentTimeMillis()+86400000,to+EnergyIntegral.LOOKAROUND);
        // Establish the database identity before installing model inputs.
        JSONObject head=get("analysisManifest",new JSONArray().put(a).put(Math.min(b,a+86400000)));
        if(!head.getString("epoch").equals(repo.prefs.getString("analysis.epoch","")))reset(head.getString("epoch"));
        model(a,b,false);raw(a,b);
    }
    private JSONObject stamp(JSONObject result)throws Exception{
        JSONObject current=get("analysisInputState",new JSONArray());
        if(current.has("epoch")&&(!current.getString("epoch").equals(epoch)||metadata.has("epoch")&&!metadata.getString("epoch").equals(epoch)))throw new IOException("Der Datenbestand der Zentrale wurde gewechselt. Bitte erneut laden.");
        // Each transferred table and raw block is already checked before installation.
        // A later weather/configuration update makes that complete snapshot older;
        // it must not erase the first usable result while recording continues.
        boolean changed=!current.getString("token").equals(metadata.optString("inputToken"));
        if(changed)modelAt=0;
        JSONObject state=new JSONObject().put("plant",plant).put("at",metadata.optLong("at")).put("measurementAt",metadata.optLong("measurementAt")).put("revision",rawRevision).put("tables",metadata.getJSONObject("tables")).put("controlRevision",metadata.optLong("controlRevision")).put("algorithm",SolarPlanning.ALGORITHM).put("epoch",epoch).put("local",true).put("inputsChanged",changed);
        result.put("analysis",state);if(changed)result.put("stale",true);else result.remove("stale");
        for(String key:new String[]{"solar","consumption","balance","mobile"})if(result.optJSONObject(key)!=null){JSONObject part=result.getJSONObject(key).put("analysis",state);if(changed)part.put("stale",true);else part.remove("stale");}return result;
    }
    synchronized JSONObject query(String method,JSONArray args,Consumer<String> progress)throws Exception{
        this.progress=progress;operationArgs=args;learningOperation=false;operationFocus=lan.displayFocus();downloadedRows=downloadedBlocks=0;beginTiming(method);
        try{
            long from=args.getLong(0),to=args.getLong(1),now=System.currentTimeMillis();
            if(from<0||to<=from||to-from>(method.equals("dailyEnergy")?32L:366L)*86400000||(!method.equals("dailyEnergy")&&to>now+86400000))throw new IOException("Ungültiger Zeitraum.");
            prepare(from,Math.max(from+1,Math.min(to,now)));priority();
            String cacheKey=method+":"+args,token=AnalysisResultCache.token(repo,planning,ledger,support(from),to);JSONObject retained=results.read(cacheKey,token);
            if(retained!=null){retained.put("calculationReused",true);JSONObject stamped=stamp(retained);completed=true;return stamped;}
            note("Auswertung auf diesem Handy …");long calculating=System.nanoTime();JSONObject result;
            switch(method){
                case "chargerJournal":if(!metadata.getJSONObject("tables").has("charger_raw"))throw new IOException("Für das Laderjournal zuerst die Zentrale auf 3.0.8 aktualisieren.");result=ChargerJournal.query(ledger,ChargerJournal.source(repo),from,to,args.optString(2),now);break;
                case "loadHistory":result=ledger.loadHistory(RollerLedger.source(repo.prefs),from,to,args.getString(2),args.optString(3));break;
                            case "rollerEnergy":result=ledger.summary(RollerLedger.source(repo.prefs),from,to);break;
                case "history":result=new JSONObject().put("points",planning.history(from,to,args.getString(2),args.getString(3)));break;
                case "dashboardHistory":result=planning.dashboardData(from,to,args.getString(2));break;
                case "energyHistoryWindow":result=planning.energyFlow(from,to,args.getLong(3),args.getLong(4),args.getString(2));break;
                case "energyHistory":result=planning.energyFlow(from,to,args.getLong(3),args.getString(2));break;
                case "dailyEnergy":result=dailyResults(from,to);break;
                default:throw new IllegalArgumentException("Unbekannte Auswertung.");
            }computeNanos+=System.nanoTime()-calculating;result.put("calculatedAt",System.currentTimeMillis());results.save(cacheKey,token,result);JSONObject stamped=stamp(result);completed=true;return stamped;
        }finally{endTiming();this.progress=null;}
    }
    synchronized JSONObject knowledge(Consumer<String> progress)throws Exception{
        this.progress=progress;learningOperation=true;operationDemand=lan.displayDemand();operationFocus=lan.displayFocus();downloadedRows=downloadedBlocks=0;beginTiming("knowledge");try{
            requireProtocol();long now=System.currentTimeMillis(),since=now-7L*86400000,previous=repo.prefs.getLong("analysis.learningRevision",0);
            JSONObject changes=get("analysisChanges",new JSONArray().put(previous));
            if(!changes.getString("epoch").equals(repo.prefs.getString("analysis.epoch","")))reset(changes.getString("epoch"));
            JSONObject head=get("analysisMetadata",new JSONArray().put(Math.max(0,now-86400000)).put(now));
            String modelVersions=learningVersions(head.getJSONObject("tables"));
            String signature=learningSignature();long learnedAt=repo.prefs.getLong("analysis.learnedAt",0);
            long today=java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            boolean corrected=changes.optLong("changedFrom")>0&&changes.optLong("changedFrom")<Math.min(today,learnedAt-86400000L);
            if(learnedAt>0&&now>=learnedAt&&now-learnedAt<6L*3600000&&!corrected&&signature.equals(repo.prefs.getString("analysis.learningSignature",""))&&modelVersions.equals(repo.prefs.getString("analysis.learningModelVersions",""))){
                JSONObject saved=lan.cachedKnowledge();if(saved!=null){
                    if(!head.getJSONObject("tables").optString("forecasts").equals(repo.prefs.getString("analysis.table.forecasts",""))){
                        comparisonsOnly=true;try{model(Math.max(0,now-86400000),now,false);}finally{comparisonsOnly=false;}
                        saved=planning.refreshComparisons(saved);
                    }
                    saved.put("stale",false).put("learningReused",true);completed=true;return saved;}
            }
            model(support(since),now);if(metadata.optLong("measurementAt")>0)now=metadata.getLong("measurementAt");since=planning.learningSince(now);
            long correction=changes.optLong("changedFrom");if(previous>0&&correction>0)since=Math.min(since,correction);
            // An imported partial learning ledger is not proof that its historical raw inputs arrived.
            long first=metadata.optLong("firstMeasurement");boolean recovering=planning.needsHistoricalRecovery();
            if((recovering||previous==0&&!planning.hasObservations())&&first>0){since=Math.min(since,first);note("Vorhandenen Messverlauf vollständig für das Lernen abgleichen …");}
            since=Math.max(LearningWindow.since(now),since);
            // Completed local days and model priors need the preceding day across DST boundaries.
            since=Math.max(LearningWindow.since(now),since-86400000);
            java.time.ZoneId zone;try{zone=java.time.ZoneId.of(metadata.getString("timezone"));}catch(Exception e){zone=java.time.ZoneId.of("Europe/Berlin");}
            since=java.time.Instant.ofEpochMilli(since).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli();
            JSONArray presence=null;if(now-support(since)>376L*86400000){for(long a=support(since);a<now;a+=360L*86400000){model(a,Math.min(now,a+360L*86400000));if(presence==null)presence=metadata.getJSONArray("presenceHistory");}}
            else model(support(since),now);
            raw(support(since),now);
            planning.analysisPresence(presence==null?metadata.getJSONArray("presenceHistory"):presence,metadata.getJSONObject("presence"),since,now);if(previous>0){if(changes.has("ranges"))planning.invalidateLearning(changes.getJSONArray("ranges"));else planning.invalidateLearning(correction);}
            note("Verbrauch und Solarvorschau werden auf diesem Handy berechnet …");priority();long calculating=System.nanoTime();planning.analysisCheckpoint=()->{priority();return null;};JSONObject result=planning.calculateKnowledge();priority();computeNanos+=System.nanoTime()-calculating;
            if(result.has("error"))throw new IOException(result.getString("error"));
            stamp(result);if(recovering&&!result.optBoolean("historyRecoveryPending"))planning.completeHistoricalRecovery(since,now);
            repo.prefs.edit().putLong("analysis.learningRevision",changes.getLong("revision")).putLong("analysis.learnedAt",System.currentTimeMillis()).putString("analysis.learningSignature",signature).putString("analysis.learningModelVersions",learningVersions(metadata.getJSONObject("tables"))).commit();completed=true;return result;
        }finally{planning.analysisCheckpoint=null;endTiming();this.progress=null;}
    }
    private JSONObject dailyResults(long from,long to)throws Exception{
        JSONObject weather=new JSONObject(repo.prefs.getString("weatherCache","{}")).put("analysisTimezone",repo.prefs.getString("analysisTimezone",TimeZone.getDefault().getID()));
        java.time.ZoneId zone;try{zone=java.time.ZoneId.of(weather.getString("analysisTimezone"));}catch(Exception ignored){zone=java.time.ZoneId.systemDefault();}
        JSONArray days=new JSONArray();long until=Math.min(to,System.currentTimeMillis());int reused=0,calculated=0;
        for(long start=from;start<until;){priority();long end=Math.min(until,java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());
            String key="daily-part:"+start+":"+end,token=AnalysisResultCache.token(repo,planning,ledger,support(start),end);JSONObject part=results.read(key,token);
            if(part==null){part=planning.dailyEnergy(start,end,weather);results.save(key,token,part);calculated++;}else reused++;
            JSONArray values=part.getJSONArray("days");for(int i=0;i<values.length();i++)days.put(values.get(i));start=end;
        }
        return new JSONObject().put("days",days).put("accountingVersion",2).put("reusedDays",reused).put("calculatedDays",calculated);
    }
    private static String learningVersions(JSONObject tables){return tables.optString("observations")+"|"+tables.optString("confirmed_setups")+"|"+tables.optString("archive_clock_state");}
    private String learningSignature()throws Exception{return learningSignature(new JSONObject(lan.part("snapshot")),new JSONObject(lan.part("presence")));}
    static String learningSignature(JSONObject snapshot,JSONObject presence)throws Exception{
        JSONObject settings=snapshot.optJSONObject("settings");if(settings==null)settings=new JSONObject();
        JSONArray devices=snapshot.optJSONArray("devices");if(devices==null)devices=new JSONArray();
        JSONObject place=settings.optJSONObject("weatherPlace"),site=new JSONObject();
        if(place!=null)for(String key:new String[]{"latitude","longitude"})if(place.has(key))site.put(key,place.get(key));
        // Live timestamps, RSSI, connection errors and labels are observations,
        // not changes to the learning configuration. Keep only the selected source.
        return AnalysisResultCache.hash(new JSONArray().put(SolarPlanning.ALGORITHM).put(SolarPlanning.selectedSource(devices)).put(snapshot.opt("equipment")).put(settings.opt("solarSetup")).put(site).put(settings.opt("capacity")).put(settings.opt("specialUses")).put(presence.optString("state")).toString());
    }
    synchronized JSONObject diagnostics()throws Exception{return new JSONObject().put("downloadedRows",downloadedRows).put("downloadedBlocks",downloadedBlocks).put("epoch",epoch).put("revision",rawRevision).put("timing",lastTiming);}
    synchronized void rebuildHistory()throws Exception{planning.rebuildHistory();repo.prefs.edit().remove("analysis.learnedAt").commit();}
    boolean exportDiagnostics(java.io.OutputStream output)throws Exception{
        JSONObject base=new JSONObject().put("appVersion",BuildConfig.VERSION_NAME).put("viewerTiming",lastTiming).put("analysis",new JSONObject().put("plant",plant).put("epoch",epoch).put("revision",rawRevision).put("inputMetadata",metadata).put("cached",true));
        return DiagnosticArchive.write(output,base,()->{
            JSONObject diagnostic=planning.diagnosticSnapshotNow();diagnostic.put("viewerTiming",base.getJSONObject("viewerTiming")).put("analysis",base.getJSONObject("analysis"));
            diagnostic.remove("rollerHandover");return diagnostic.put("roller",new JSONObject(lan.part("roller")));
        },repo.db::exportTo);
    }
    void close(){planning.shutdown();}
}
