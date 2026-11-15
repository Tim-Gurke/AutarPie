package de.wattpit.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Real journal SQLite, fresh-process resume, and the production typed replica transport. */
public final class ChargerJournalProof {
    private static int checks;
    private static final String SOURCE="synthetic-plant",A="00:00:00:00:00:11",B="00:00:00:00:00:12",C="00:00:00:00:00:13";
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);checks++;System.out.println("PASS "+text);}
    private static JSONObject point(long at,int mode,long elapsed,long capacity)throws Exception{return new JSONObject().put("at",at).put("type","sample").put("mode",mode).put("elapsed",elapsed).put("capacity",capacity).put("voltage",12).put("current",2).put("cells",new JSONArray().put(4).put(4).put(4)).put("name","Neutraler Testlader").put("role","model");}
    private static void sample(SQLiteDatabase db,String mac,long at,int mode,long elapsed,long capacity)throws Exception{ChargerJournal.record(db,SOURCE,mac,point(at,mode,elapsed,capacity));}
    private static JSONObject session(JSONObject report,String mac,int index)throws Exception{int found=0;JSONArray sessions=report.getJSONArray("sessions");for(int i=0;i<sessions.length();i++){JSONObject s=sessions.getJSONObject(i);if(mac.equals(s.optString("device"))&&found++==index)return s;}throw new AssertionError("Missing session "+mac+" "+index);}
    private static void fixture(SQLiteDatabase db,long base)throws Exception{
        sample(db,A,base-5000,0,0,0);sample(db,A,base,1,0,0);sample(db,A,base+5000,1,5,1);sample(db,A,base+10000,1,10,2);sample(db,A,base+15000,0,0,0);
        sample(db,B,base-5000,0,0,0);sample(db,B,base,3,0,0);sample(db,B,base+5000,3,5,1);ChargerJournal.record(db,SOURCE,B,new JSONObject().put("at",base+7000).put("type","gap"));sample(db,B,base+20000,3,20,8);sample(db,B,base+25000,3,25,9);sample(db,B,base+30000,0,0,0);
        sample(db,C,base+1000,1,120,50);sample(db,C,base+6000,1,125,51);sample(db,C,base+11000,1,0,0);sample(db,C,base+16000,1,5,1);
        // Requests precede their observations; record() preserves chronological arrival order.
    }
    private static void commands(SQLiteDatabase db,long base)throws Exception{
        ChargerJournal.record(db,SOURCE,A,new JSONObject().put("at",base-1000).put("type","command").put("action","start"));
        ChargerJournal.record(db,SOURCE,A,new JSONObject().put("at",base+12000).put("type","command").put("action","stop"));
        ChargerJournal.record(db,SOURCE,B,new JSONObject().put("at",base+10000).put("type","command").put("action","stop"));
    }
    private static void verify(JSONObject report)throws Exception{
        check(report.getInt("version")==1,"the standalone journal export declares its format version");
        check(report.getJSONArray("sessions").length()==4,"three chargers retain four distinct observed sessions after one counter reset");
        JSONObject a=session(report,A,0),b=session(report,B,0),c=session(report,C,0),old=session(report,C,1);
        check(a.getString("state").equals("complete")&&a.getBoolean("startObserved")&&a.getBoolean("endObserved"),"fresh idle/start/end observations produce a complete charge");
        check(a.getLong("observedMillis")==10000&&Math.abs(a.getDouble("batteryWh")-24*10/3600.0)<1e-10,"battery-side Wh integrate only the two observed five-second intervals");
        check(a.getLong("counterMah")==2,"the final idle counter does not overwrite the last active capacity");
        check(b.getInt("mode")==3&&b.getBoolean("hasGap")&&b.getString("state").equals("incomplete"),"powerbank use is retained with its actual connection gap");
        check(b.getLong("observedMillis")==10000&&b.getLong("gapMillis")==15000,"the gap contributes no invented energy or observed duration");
        check(Math.abs(b.getDouble("batteryWh")-a.getDouble("batteryWh"))<1e-10,"longer unobserved operation cannot inflate the energy integral");
        check(c.getString("state").equals("unknown")&&!c.getBoolean("startObserved"),"an unresolved active sample remains an unknown end after silence");
        check(old.getString("reason").contains("zurückgesetzt")&&!old.getBoolean("endObserved"),"a counter reset preserves the earlier incomplete fragment");
        check(report.getString("energySide").equals("608PD-Akkuseite"),"the energy reference cannot be confused with a second house-side total");
        JSONArray commands=report.getJSONArray("commands");int confirmed=0;for(int i=0;i<commands.length();i++)if(commands.getJSONObject(i).optBoolean("confirmed"))confirmed++;
        check(commands.length()==3&&confirmed==2,"requests require a matching later device state within the confirmation window");
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);LanSyncProof.TestContext context=new LanSyncProof.TestContext(root.resolve("host"));Repository repo=LanSyncProof.repository(context);
        RollerLedger ledger=new RollerLedger(context,"journal.db");SQLiteDatabase db=ledger.getWritableDatabase();Field singleton=RollerLedger.class.getDeclaredField("instance");singleton.setAccessible(true);singleton.set(null,ledger);
        long base;if(args[1].equals("write")){
            String journalSource=ChargerJournal.source(repo);repo.prefs.edit().putString("devices","[{\"kind\":\"solar\",\"mac\":\"00:00:00:00:00:30\"}]").commit();
            check(journalSource.equals(ChargerJournal.source(repo)),"changing Victron devices cannot change the journal's persistent plant identity");
            db.execSQL("INSERT INTO input_intervals(source,start,end,wh,mac,session) VALUES('existing-input',1,2,3,'00:00:00:00:00:20',1)");
            db.execSQL("DROP TABLE charger_raw");db.execSQL("PRAGMA user_version=3");ledger.close();
            ledger=new RollerLedger(context,"journal.db");db=ledger.getWritableDatabase();singleton.set(null,ledger);
            try(Cursor c=db.rawQuery("SELECT wh FROM input_intervals WHERE source='existing-input'",null)){check(c.moveToFirst()&&c.getDouble(0)==3,"the version-three ledger upgrades without losing existing roller records");}
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM charger_raw",null)){check(c.moveToFirst()&&c.getLong(0)==0,"the journal table is added by the real SQLite upgrade");}
            base=System.currentTimeMillis()/60000*60000-86400000;Files.writeString(root.resolve("time.txt"),""+base);
            // Interleave commands in their real arrival order.
            ChargerJournal.record(db,SOURCE,A,new JSONObject().put("at",base-1000).put("type","command").put("action","start"));
            fixture(db,base);commands(db,base);
            JSONObject secret=point(base+100000,0,0,0).put("secret","NEVER-IN-JOURNAL").put("auth","NEVER-IN-JOURNAL");ChargerJournal.record(db,SOURCE,A,secret);
            sample(db,A,base+10000,1,10,2);
            try(Cursor c=db.rawQuery("SELECT payload FROM charger_raw",null)){while(c.moveToNext())check(!c.getString(0).contains("NEVER-IN-JOURNAL"),"private fields are excluded before persistent journal storage");}
        }else base=Long.parseLong(Files.readString(root.resolve("time.txt")));
        JSONObject result=ChargerJournal.query(ledger,SOURCE,base-60000,base+120000,"",base+120000);verify(result);
        check(ChargerJournal.query(ledger,"different-plant",base-60000,base+120000,"",base+120000).getJSONArray("sessions").length()==0,"another plant's observations are isolated");
        check(ChargerJournal.query(ledger,SOURCE,base-60000,base+120000,B,base+120000).getJSONArray("sessions").length()==1,"device filtering does not merge the three chargers");
        JSONObject exported=new JSONObject(result.toString());check(exported.getJSONArray("sessions").toString().equals(result.getJSONArray("sessions").toString()),"the JSON journal export preserves all derived session fields");
        if(args[1].equals("write")){Files.writeString(root.resolve("sessions.json"),result.getJSONArray("sessions").toString());}
        else check(result.getJSONArray("sessions").toString().equals(Files.readString(root.resolve("sessions.json"))),"a fresh process reconstructs exactly the same sessions from persisted observations");
        String version=AnalysisExchange.version("charger_raw",repo,null,ledger,base-60000,base+120000);JSONObject page=AnalysisExchange.page(context,repo,null,"charger_raw",base-60000,base+120000,version,0);JSONArray rows=page.getJSONArray("rows");
        check(rows.length()>0&&!page.optBoolean("changed"),"the actual typed analysis protocol exports charger observations");
        LanSyncProof.TestContext target=new LanSyncProof.TestContext(root.resolve("replica-"+args[1]));Repository replica=LanSyncProof.repository(target);RollerLedger replicaLedger=new RollerLedger(target,"journal-replica.db");replicaLedger.getWritableDatabase();
        AnalysisExchange.install("charger_raw",replica,null,replicaLedger,base-60000,base+120000,rows);AnalysisExchange.install("charger_raw",replica,null,replicaLedger,base-60000,base+120000,rows);
        JSONObject copied=ChargerJournal.query(replicaLedger,SOURCE,base-60000,base+120000,"",base+120000);check(copied.getJSONArray("sessions").toString().equals(result.getJSONArray("sessions").toString()),"repeated viewer replication cannot duplicate a session or its Wh");
        ledger.close();replicaLedger.close();repo.db.close();replica.db.close();System.out.println("RESULT "+checks+" charger journal "+args[1]+" checks");
    }
}
