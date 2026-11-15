package de.wattpit.app;

import android.database.Cursor;
import org.json.*;
import java.time.*;
import java.util.*;

/** Derived on displays only. A missing radio sample never certifies a zero-use day. */
final class RollerUsageLearning {
    private static final long H=3600000L,D=24*H;
    private static final double MIN_CHARGE_COVERAGE=.90,MIN_DAY_COVERAGE=.98;
    private static final long MAX_CHARGE_GAP=180000,MAX_CHARGE_MISSING=600000,MAX_DAY_GAP=120000;
    private final RollerLedger ledger;
    private final Map<String,Day> days=new TreeMap<>();
    private final Map<Long,Charge> samples=new HashMap<>();
    private final Map<String,Day> offKnown=new HashMap<>();
    private ZoneId zone;
    private static final class Day {
        final TreeMap<Long,Long> known=new TreeMap<>(); double wh; boolean invalid; int starts;
        void add(long a,long b){Map.Entry<Long,Long> p=known.floorEntry(a);if(p!=null&&p.getValue()>=a){a=p.getKey();b=Math.max(b,p.getValue());known.remove(p.getKey());}while((p=known.ceilingEntry(a))!=null&&p.getKey()<=b){b=Math.max(b,p.getValue());known.remove(p.getKey());}known.put(a,b);}
        long covered(){long n=0;for(var p:known.entrySet())n+=p.getValue()-p.getKey();return n;}
        long maxGap(long from,long to){long gap=0,end=from;for(var p:known.entrySet()){gap=Math.max(gap,p.getKey()-end);end=Math.max(end,p.getValue());}return Math.max(gap,to-end);}
        void addClipped(Day other,long from,long to){Map.Entry<Long,Long> p=other.known.floorEntry(from);if(p==null||p.getValue()<=from)p=other.known.ceilingEntry(from);while(p!=null&&p.getKey()<to){long a=Math.max(from,p.getKey()),b=Math.min(to,p.getValue());if(b>a)add(a,b);p=other.known.higherEntry(p.getKey());}}
    }
    private static final class Charge {long from,to,millis,maxGap;double wh,battery;final Day known=new Day();}
    RollerUsageLearning(RollerLedger ledger){this.ledger=ledger;}
    private interface Visit {void apply(Day day,long a,long b);}
    private void split(long from,long to,Visit visit){long a=from;while(a<to){LocalDate date=Instant.ofEpochMilli(a).atZone(zone).toLocalDate();long b=Math.min(to,date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());visit.apply(days.computeIfAbsent(date.toString(),k->new Day()),a,b);a=b;}}
    JSONObject calculate(String source,JSONObject roller,long now,String timezone)throws Exception{
        try{zone=ZoneId.of(timezone);}catch(Exception e){zone=ZoneId.of("UTC");}
        JSONObject profile=roller.optJSONObject("profile");String mac=profile==null?"":profile.optString("mac");
        JSONObject result=new JSONObject().put("version",1).put("samplingTolerance",1).put("mac",mac).put("updated",now).put("timezone",zone.getId());
        if(mac.isEmpty())return result.put("charges",new JSONArray()).put("pathDays",new JSONObject());
        long since=now-90*D,previous=0;
        try(Cursor c=ledger.getReadableDatabase().rawQuery("SELECT payload FROM roller_raw WHERE source=? AND mac=? AND end>? AND start<? ORDER BY start",new String[]{source,mac,""+since,""+now})){
            while(c.moveToNext()){JSONArray rows=new JSONArray(c.getString(0));for(int i=0;i<rows.length();i++){
                JSONArray r=rows.getJSONArray(i);long a=Math.max(Math.max(since,previous),r.getLong(0)),b=Math.min(now,r.getLong(1));
                if(b<=a||r.getLong(1)-r.getLong(0)>10000)continue;previous=b;
                JSONObject meta=r.optJSONObject(8);if(meta!=null&&!meta.optBoolean("chargeAssigned",true))continue;
                long session=r.optLong(6);if(session<=0)continue;
                double p=r.optDouble(2),q=r.optDouble(3),idle=r.optDouble(4),eff=r.optDouble(5);
                if(!Double.isFinite(p)||!Double.isFinite(q)||!Double.isFinite(idle)||!Double.isFinite(eff)||eff<=0||eff>1||idle<0)continue;
                p=Math.max(0,p);q=Math.max(0,q);double battery=(p+q)*.5*(b-a)/H,input=(de.wattpit.app.core.RollerEnergy.inputWatts(p,idle,eff)+de.wattpit.app.core.RollerEnergy.inputWatts(q,idle,eff))*.5*(b-a)/H;
                Charge s=samples.computeIfAbsent(session,k->new Charge());if(s.from==0)s.from=a;else s.maxGap=Math.max(s.maxGap,a-s.to);s.to=b;s.millis+=b-a;s.wh+=input;s.battery+=battery;s.known.add(a,b);
                final long from=a,to=b;final double wh=input;split(a,b,(d,x,y)->{d.add(x,y);d.wh+=wh*(y-x)/(to-from);});
            }}
        }
        Map<String,JSONObject> last=new HashMap<>();
        try(Cursor c=ledger.getReadableDatabase().rawQuery("SELECT mac,payload FROM load_raw WHERE source=? AND end>=? AND start<=? ORDER BY start",new String[]{source,""+(since-45000),""+now})){
            while(c.moveToNext()){String regulator=c.getString(0);JSONArray rows=new JSONArray(c.getString(1));for(int i=0;i<rows.length();i++){
                JSONObject q=rows.getJSONObject(i),p=last.get(regulator);long at=q.optLong("at");if(p!=null&&at<=p.optLong("at"))continue;last.put(regulator,q);
                if(p==null||at-p.optLong("at")>45000||!mac.equals(p.optString("scooter"))||!mac.equals(q.optString("scooter")))continue;
                boolean configured=p.optBoolean("learningPath",p.optBoolean("exclusive"))&&q.optBoolean("learningPath",q.optBoolean("exclusive"));
                if(!configured||!p.optString("chargePath","load-output").equals(q.optString("chargePath","load-output")))continue;
                // State is an actual regulator reply. Relay-coil watts never supply charge energy.
                if(p.optInt("state",-1)==0&&q.optInt("state",-1)==0){long a=Math.max(since,p.optLong("at")),b=Math.min(now,at);if(b>a){offKnown.computeIfAbsent(q.optString("chargePath","load-output"),k->new Day()).add(a,b);split(a,b,(d,x,y)->d.add(x,y));}}
            }}
        }
        Map<Long,JSONObject> unique=new TreeMap<>();JSONArray journal=roller.optJSONArray("journal");if(journal==null)journal=new JSONArray();
        for(int i=0;i<journal.length();i++){
            JSONObject e=journal.getJSONObject(i),old=unique.get(e.optLong("started"));JSONObject identity=e.optJSONObject("profile");
            if(identity==null||!mac.equals(identity.optString("mac"))||e.optBoolean("active")||e.optLong("started")<=0||e.optLong("ended")>now||e.optLong("ended")<=since||e.optLong("ended")<=e.optLong("started"))continue;
            if(old==null||e.optLong("ended")>=old.optLong("ended"))unique.put(e.getLong("started"),e);
        }
        List<List<JSONObject>> groups=new ArrayList<>();long end=0;JSONObject previousEntry=null;
        for(JSONObject e:unique.values()){
            boolean same=false;if(previousEntry!=null){JSONObject a=previousEntry.getJSONObject("profile"),b=e.getJSONObject("profile");double before=previousEntry.optDouble("endSoc",Double.NaN),after=e.optDouble("startSoc",Double.NaN);long gap=e.getLong("started")-end;
                same=gap<=MAX_CHARGE_GAP&&a.optString("chargePath").equals(b.optString("chargePath"))&&(gap<=0||Double.isFinite(before)&&Double.isFinite(after)&&after>=before-2&&after<=before+3);
            }
            if(!same)groups.add(new ArrayList<>());groups.get(groups.size()-1).add(e);end=Math.max(end,e.getLong("ended"));previousEntry=e;
        }
        JSONArray charges=new JSONArray(),exclusions=new JSONArray();int excluded=0;long lastCompleteEnd=0,latestRecorded=0;
        for(List<JSONObject> group:groups){JSONObject first=group.get(0),tail=group.stream().max(Comparator.comparingLong(e->e.optLong("ended"))).orElse(first);long a=first.getLong("started"),b=group.stream().mapToLong(e->e.optLong("ended")).max().orElse(0),millis=0,maxGap=0,entryEnd=a;double wh=0,battery=0,rawWh=0;boolean trustworthy=true,legacy=false;Day covered=new Day();
            latestRecorded=Math.max(latestRecorded,b);
            for(JSONObject e:group){long x=e.getLong("started"),y=e.getLong("ended");maxGap=Math.max(maxGap,x-entryEnd);entryEnd=Math.max(entryEnd,y);Charge r=samples.get(x);if(r!=null){millis+=r.millis;wh+=r.wh;rawWh+=r.wh;battery+=r.battery;maxGap=Math.max(maxGap,Math.max(r.maxGap,Math.max(r.from-x,y-r.to)));covered.addClipped(r.known,x,y);}else if(!e.optString("calculationSource").equals("display")&&Double.isFinite(e.optDouble("inputWh",Double.NaN))){legacy=true;long observed=e.optLong("observedMillis");millis+=observed;wh+=e.optDouble("inputWh");battery+=e.optDouble("batteryWh",0);maxGap=Math.max(maxGap,y-x-observed);}else trustworthy=false;}
            // The detector waits for confirmed idle before ending a short charge. An
            // observed OFF load path is known non-charging time, not missing BMS energy.
            if(!legacy){Day off=offKnown.get(first.getJSONObject("profile").optString("chargePath"));if(off!=null)covered.addClipped(off,a,b);millis=covered.covered();maxGap=covered.maxGap(a,b);}
            String reason=tail.optString("endReason");double coverage=b>a?Math.min(1,millis/(double)(b-a)):0;
            long missing=Math.max(0,b-a-millis);String rejection=!trustworthy?"Berechnungsgrundlagen fehlen":coverage<MIN_CHARGE_COVERAGE?"Zu wenig Messabdeckung":maxGap>MAX_CHARGE_GAP||missing>MAX_CHARGE_MISSING?"Zu große Messlücke":wh<=0||!Double.isFinite(wh)?"Ladeenergie unbekannt":reason.contains("pausiert")||reason.contains("verloren")?"Ladeaufzeichnung unterbrochen":a<lastCompleteEnd?"Überlappende Ladeaufzeichnung":"";
            if(!rejection.isEmpty()){excluded++;exclusions.put(new JSONObject().put("from",a).put("to",b).put("coverage",coverage).put("maxGapMillis",maxGap).put("reason",rejection));split(a,b,(d,x,y)->d.invalid=true);continue;}
            if(a<since||b>now)continue;lastCompleteEnd=b;
            // Fill only bounded sampling gaps, using this charge's observed mean power.
            // These are planning estimates; journal measurements and plant totals stay untouched.
            double estimatedWh=wh/coverage,estimatedBattery=battery/coverage,extraWh=estimatedWh-rawWh;
            charges.put(new JSONObject().put("id",mac+":"+a).put("mac",mac).put("from",a).put("to",b).put("inputWh",estimatedWh).put("batteryWh",estimatedBattery).put("observedInputWh",wh).put("estimatedGapWh",estimatedWh-wh).put("gapMillis",missing).put("maxGapMillis",maxGap).put("samplingTolerance",1).put("coverage",coverage).put("estimatedInput",true).put("fragments",group.size()));
            split(a,b,(d,x,y)->{d.add(x,y);d.wh+=extraWh*(y-x)/(b-a);});
            String day=Instant.ofEpochMilli(a).atZone(zone).toLocalDate().toString();Day d=days.get(day);if(d!=null)d.starts++;
        }
        JSONObject pathDays=new JSONObject();LocalDate today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        for(var pair:days.entrySet()){
            LocalDate date=LocalDate.parse(pair.getKey());long a=date.atStartOfDay(zone).toInstant().toEpochMilli(),b=date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();Day d=pair.getValue();long covered=d.covered();
            long maxGap=d.maxGap(a,b);double coverage=covered/(double)(b-a);
            pathDays.put(pair.getKey(),new JSONObject().put("mac",mac).put("millis",covered).put("dayMillis",b-a).put("wh",d.wh).put("charges",d.starts).put("coverage",coverage).put("maxGapMillis",maxGap).put("samplingTolerance",1).put("complete",date.isBefore(today)&&coverage>=MIN_DAY_COVERAGE&&maxGap<=MAX_DAY_GAP&&!d.invalid));
        }
        return result.put("charges",charges).put("pathDays",pathDays).put("excludedCharges",excluded).put("exclusions",exclusions).put("latestRecorded",latestRecorded);
    }
}
