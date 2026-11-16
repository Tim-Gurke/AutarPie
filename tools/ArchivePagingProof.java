package de.wattpit.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.wattpit.app.core.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual HistoryDb differential proof. No measurements or private profiles from a user. */
public final class ArchivePagingProof {
    static final long EPOCH=1700000000000L,END=2500000L,NOW=EPOCH+END*1000L;
    static final String DEVICE="paging-fixture";
    static final String[] FIELDS={"solar","voltage","current","load","pvVoltage","temperature"};
    static int checks;
    static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError();return c.getLong(0);}}
    static ArchiveCodec.Clock clock(){ArchiveCodec.Clock c=new ArchiveCodec.Clock();byte[] b=new byte[8];System.arraycopy(ArchiveCodec.le(EPOCH/1000,4),0,b,4,4);c.anchors(b);return c;}
    static List<ArchiveCodec.Level> levels(){List<ArchiveCodec.Level> r=new ArrayList<>();int[] periods={1,30,300,1800},counts={121,241,289,2160};for(int i=0;i<periods.length;i++)r.add(new ArchiveCodec.Level(counts[i],periods[i],END-END%periods[i]));return r;}
    static String fingerprint(SQLiteDatabase db){
        StringBuilder b=new StringBuilder();
        try(Cursor c=db.rawQuery("SELECT COUNT(*),SUM(ts),SUM(period),COUNT(solar),SUM(solar),COUNT(voltage),SUM(voltage),COUNT(current),SUM(current),COUNT(load),SUM(load),COUNT(pvVoltage),SUM(pvVoltage),COUNT(temperature),SUM(temperature) FROM readings",null)){
            c.moveToFirst();for(int i=0;i<15;i++)b.append(c.getString(i)).append('|');
        }
        b.append(scalar(db,"SELECT COUNT(*) FROM archive_coverage"));return b.toString();
    }
    static void seed(HistoryDb history){
        SQLiteDatabase db=history.getWritableDatabase();db.beginTransaction();
        try{
            db.execSQL("WITH RECURSIVE seq(n) AS (VALUES(0) UNION ALL SELECT n+1 FROM seq WHERE n<249999) INSERT INTO readings(ts,device,kind,origin,period,solar,voltage,current,load,pvVoltage,temperature) SELECT "+EPOCH+"+n*10000,'"+DEVICE+"','solar',CASE WHEN n%6<3 THEN 'live' WHEN n%6<5 THEN 'archive' ELSE 'csv' END,CASE WHEN n%6<3 THEN CASE WHEN n%17=0 THEN 30 ELSE 10 END WHEN n%4=0 THEN 30 WHEN n%4=1 THEN 300 ELSE 1800 END,CASE WHEN n%41!=0 THEN n%250 END,CASE WHEN n%43!=0 THEN 13.2 END,CASE WHEN n%47!=0 THEN n%11 END,CASE WHEN n%53!=0 THEN n%3 END,CASE WHEN n%59!=0 THEN 26.4 END,CASE WHEN n%61!=0 THEN 24.0 END FROM seq WHERE n%449>=29");
            // Thousands of equal timestamps cross page boundaries. Row id must
            // preserve the previous ordering of duplicate live resolutions.
            db.execSQL("WITH RECURSIVE seq(n) AS (VALUES(0) UNION ALL SELECT n+1 FROM seq WHERE n<4096) INSERT INTO readings(ts,device,kind,origin,period,solar,voltage,current,load,pvVoltage,temperature) SELECT "+(EPOCH+700000000L)+",'"+DEVICE+"','solar',CASE WHEN n%2=0 THEN 'live' ELSE 'archive' END,5000-n,10,13.1,2.0,1.0,26.0,22.0 FROM seq");
            db.execSQL("INSERT INTO readings(ts,device,kind,origin,period,solar,voltage,current,load,pvVoltage,temperature) SELECT ts,'other-device',kind,origin,period,solar,voltage,current,load,pvVoltage,temperature FROM readings WHERE device='"+DEVICE+"'");
            // Same device but another kind, missing fields, and unsupported
            // origins must never supply coverage for this solar channel.
            db.execSQL("INSERT INTO readings(ts,device,kind,origin,period,solar) VALUES("+(EPOCH+5000)+",'"+DEVICE+"','shunt','live',10,99)");
            db.execSQL("INSERT INTO readings(ts,device,kind,origin,period,solar) VALUES("+(EPOCH+7000)+",'"+DEVICE+"','solar','estimated',10,99)");
            for(int channel=0;channel<FIELDS.length;channel++)db.execSQL("INSERT INTO archive_coverage VALUES(?,?,?,?,?)",new Object[]{DEVICE,channel,30,EPOCH+500000,EPOCH+560000});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        check(scalar(db,"SELECT COUNT(*) FROM readings")>470000,"large neutral history is persisted with independent devices and origins");
        check(scalar(db,"PRAGMA user_version")==9,"existing database version remains unchanged");
    }
    static void compare(HistoryDb history,HistoryDbReference reference)throws Exception{
        SQLiteDatabase db=history.getWritableDatabase();String before=fingerprint(db);var clock=clock();var levels=levels();
        long totalRows=0;int totalPages=0;
        for(int channel=0;channel<FIELDS.length;channel++){
            final String field=FIELDS[channel];List<String> queries=new ArrayList<>();
            SQLiteDatabase.beforeQuery=sql->{if(sql.startsWith("SELECT ts,origin,period"))queries.add(sql);};
            var old=reference.archiveAvailability(DEVICE,"solar",channel,field,levels,clock,NOW,()->{});
            check(queries.size()==1&&!queries.get(0).contains(" LIMIT "),field+": released HistoryDb issues an unbounded history result");
            queries.clear();
            var now=history.archiveAvailability(DEVICE,"solar",channel,field,levels,clock,NOW,()->{});
            SQLiteDatabase.beforeQuery=null;
            long rows=scalar(db,"SELECT COUNT(*) FROM readings WHERE device='"+DEVICE+"' AND kind='solar' AND "+field+" IS NOT NULL AND origin IN ('live','archive','csv')");
            check(rows>200000&&queries.size()==rows/ArchiveSyncPlan.READING_PAGE_SIZE+1,field+": every stored row is covered by bounded forward pages");
            check(queries.stream().allMatch(q->q.endsWith(" LIMIT 2048")&&!q.contains("OFFSET")),field+": no oversized result and no growing OFFSET rescan");
            for(int i=1;i<queries.size();i++)if(!queries.get(i).contains("ts>? OR (ts=? AND rowid>?)"))throw new AssertionError("Missing stable continuation");
            Random random=new Random(400+channel);
            for(int period:new int[]{1,30,300,1800})for(int n=0;n<12000;n++){
                long at=n<4100?EPOCH+700000000L+(n-2050)*1000L:EPOCH+Math.floorMod(random.nextLong(),END*1000L);
                if(old.known(at,period)!=now.known(at,period))throw new AssertionError(field+": changed gap/coverage at "+at+" period "+period);
            }
            check(true,field+": 48000 coverage decisions preserve real holes, field nulls and equal-time origins/resolutions");
            for(var level:levels){
                var a=ArchiveSyncPlan.plan(level,clock,old);var b=ArchiveSyncPlan.plan(level,clock,now);
                if(a.known!=b.known||a.needed!=b.needed||a.deferred!=b.deferred||a.windows.size()!=b.windows.size())throw new AssertionError("Changed device request counts");
                for(int i=0;i<a.windows.size();i++)if(a.windows.get(i).cursor!=b.windows.get(i).cursor||a.windows.get(i).count!=b.windows.get(i).count)throw new AssertionError("Changed missing window");
            }
            check(true,field+": all four archive request plans and unavailable acknowledgements match the released reader");
            totalRows+=rows;totalPages+=queries.size();
        }
        var dbPlan=new ArrayList<String>();
        String[] args={DEVICE,"solar",""+(EPOCH+1000000000L),""+NOW,""+(EPOCH+1000000000L),""+(EPOCH+1000000000L),"0"};
        try(Cursor c=db.rawQuery("EXPLAIN QUERY PLAN "+ArchiveSyncPlan.readingsPageSql("voltage",true),args)){while(c.moveToNext())dbPlan.add(c.getString(3));}
        check(dbPlan.stream().anyMatch(x->x.contains("history_device"))&&dbPlan.stream().noneMatch(x->x.contains("TEMP B-TREE")),"real SQLite seeks the existing device/time index without a temporary sort or schema migration");
        AtomicInteger pages=new AtomicInteger(),callbacks=new AtomicInteger();
        SQLiteDatabase.beforeQuery=sql->{if(sql.startsWith("SELECT ts,origin,period"))pages.incrementAndGet();};
        boolean cancelled=false;
        try{history.archiveAvailability(DEVICE,"solar",0,"voltage",levels,clock,NOW,()->{if(callbacks.incrementAndGet()==6)throw new IOException("proof cancel");});}
        catch(IOException expected){cancelled=expected.getMessage().equals("proof cancel");}
        finally{SQLiteDatabase.beforeQuery=null;}
        check(cancelled&&pages.get()==1,"cancel stops before opening another page and preserves stored data");
        boolean rejected=false;try{history.archiveAvailability(DEVICE,"solar",0,"voltage;DROP TABLE readings",levels,clock,NOW,()->{});}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"field whitelist also protects the paged query");
        check(before.equals(fingerprint(db))&&scalar(db,"PRAGMA user_version")==9,"full differential read, cancellation and rejected input change no measurements or database version");
        System.out.println("METRICS {\"rowsReadAcrossSixFields\":"+totalRows+",\"preparedSqlPages\":"+totalPages+",\"maxRowsPerPreparedResult\":2048,\"exactPointComparisons\":288000,\"scope\":\"Real SQLite coverage equivalence and bounded results; JDBC does not model Android CursorWindow latency\"}");
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("wattpit.test.database",args[0]);
        try(HistoryDb history=new HistoryDb(null);HistoryDbReference reference=new HistoryDbReference(null)){
            if(args[1].equals("seed"))seed(history);else compare(history,reference);
        }
        System.out.println("RESULT "+checks+" "+args[1]+" checks; no APK or physical radio run");
    }
}
