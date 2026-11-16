package de.wattpit.app.core;

import java.util.*;

/** Differential coverage and allocation/CPU comparison with the released planner. */
public final class ArchiveAvailabilityProof {
    static final long EPOCH=1700000000000L;
    static final List<Integer> PERIODS=Arrays.asList(30,60,300,3600);
    static int checks;
    static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    record Row(long at,String origin,int period){}
    static ArchiveCodec.Clock clock(){ArchiveCodec.Clock c=new ArchiveCodec.Clock();byte[] b=new byte[8];System.arraycopy(ArchiveCodec.le(EPOCH/1000,4),0,b,4,4);c.anchors(b);return c;}
    static void compare(List<Row> rows,String label){
        long end=EPOCH+400000000L;var old=new ArchiveSyncPlanReference.Availability(PERIODS,end);var now=new ArchiveSyncPlan.Availability(PERIODS,end);
        for(Row row:rows){old.reading(row.at,row.origin,row.period);now.reading(row.at,row.origin,row.period);}
        old.imported(60,EPOCH+123000,EPOCH+183000);now.imported(60,EPOCH+123000,EPOCH+183000);
        var clock=clock();for(int period:PERIODS){var level=new ArchiveCodec.Level(2500,period,200000);var a=ArchiveSyncPlanReference.plan(level,clock,old);var b=ArchiveSyncPlan.plan(level,clock,now);check(a.known==b.known&&a.needed==b.needed&&a.deferred==b.deferred,label+" preserves counts at "+period+" seconds");check(a.windows.size()==b.windows.size(),label+" preserves missing ranges at "+period+" seconds");for(int i=0;i<a.windows.size();i++)if(a.windows.get(i).cursor!=b.windows.get(i).cursor||a.windows.get(i).count!=b.windows.get(i).count)throw new AssertionError("Changed missing request");}
        Random random=new Random(41);for(int period:PERIODS)for(int n=0;n<12000;n++){long at=EPOCH+random.nextInt(250000000);if(old.known(at,period)!=now.known(at,period))throw new AssertionError(label+" invented/lost coverage at "+at);}
        check(true,label+" preserves 48000 exact point decisions, real holes and unavailable acknowledgements");
    }
    static long feed(boolean reference,int count){
        long begin=System.nanoTime();
        if(reference){var a=new ArchiveSyncPlanReference.Availability(PERIODS,EPOCH+count*10000L);for(int i=0;i<count;i++)a.reading(EPOCH+i*10000L,"live",10);if(!a.known(EPOCH+(count-1)*10000L,30))throw new AssertionError();}
        else{var a=new ArchiveSyncPlan.Availability(PERIODS,EPOCH+count*10000L);for(int i=0;i<count;i++)a.reading(EPOCH+i*10000L,"live",10);if(!a.known(EPOCH+(count-1)*10000L,30))throw new AssertionError();}
        return System.nanoTime()-begin;
    }
    public static void main(String[] args){
        List<Row> rows=new ArrayList<>();Random random=new Random(72);for(int i=0;i<7000;i++){long at=EPOCH+i*30000L+(i%11==0?500:0);if(i%13!=0)rows.add(new Row(at,"live",i%3==0?30:10));if(i%3==0)rows.add(new Row(at,"archive",i%2==0?30:60));if(i%7==0)rows.add(new Row(at,"csv",0));if(i%17==0)rows.add(new Row(at-500,"live",10));}
        compare(rows,"mixed observed order");rows.sort(Comparator.comparingLong(Row::at));compare(rows,"SQL timestamp order");Collections.reverse(rows);compare(rows,"descending imported blocks");Collections.shuffle(rows,random);compare(rows,"overlapping and late packets");
        List<Row> disconnected=Arrays.asList(new Row(EPOCH,"live",10),new Row(EPOCH+600000,"live",10),new Row(EPOCH+600000,"archive",30),new Row(EPOCH+1800000,"csv",60));compare(disconnected,"long recording holes");
        for(int i=0;i<4;i++){feed(true,12000);feed(false,12000);}long[] before=new long[3],after=new long[3];for(int i=0;i<3;i++){if(i%2==0){before[i]=feed(true,400000);after[i]=feed(false,400000);}else{after[i]=feed(false,400000);before[i]=feed(true,400000);}}Arrays.sort(before);Arrays.sort(after);
        System.out.println("METRICS {\"rows\":400000,\"tiers\":4,\"releasedMedianMs\":"+before[1]/1e6+",\"preparedMedianMs\":"+after[1]/1e6+",\"scope\":\"Actual Java coverage planner; no SQLite, ARM handset or radio timing\"}");System.out.println("RESULT "+checks+" coverage comparisons");
    }
}
