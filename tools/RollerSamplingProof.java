package de.wattpit.app;

import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Real SQLite with sampled partial charges; no physical Bluetooth or Android lifecycle claim. */
public final class RollerSamplingProof {
    static final long H=3600000L,D=24*H;
    static final String MAC="AA:00:00:00:00:41",REG="AA:00:00:00:00:42",SOURCE="sampled-charge-test";
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);System.out.println("PASS "+text);}
    static JSONObject entry(JSONObject profile,long from,long to)throws Exception{return new JSONObject().put("profile",profile).put("started",from).put("ended",to).put("active",false).put("startSoc",40).put("endSoc",41).put("endReason","Ladeende erkannt").put("calculationSource","display");}
    static JSONObject meta(long at)throws Exception{return new JSONObject().put("at",at).put("chargeAssigned",true).put("chargePath","relay").put("soc",40);}
    static de.wattpit.app.core.BmsPacket packet(boolean charging){byte[] b=new byte[30];b[0]=(byte)0xdd;b[1]=3;b[3]=23;b[4]=0x13;b[5]=(byte)0x88;b[7]=charging?(byte)240:0;b[8]=7;b[9]=(byte)0xd0;b[10]=0x0a;b[11]=0x28;b[23]=40;b[24]=3;int sum=0;for(int i=2;i<27;i++)sum+=b[i]&255;int checksum=(-sum)&65535;b[27]=(byte)(checksum>>8);b[28]=(byte)checksum;b[29]=0x77;return de.wattpit.app.core.BmsPacket.decode(b);}
    static void automaticTopup(Path root)throws Exception{
        LanSyncProof.TestContext ctx=new LanSyncProof.TestContext(root);Repository repo=LanSyncProof.repository(ctx);
        ctx.getSharedPreferences("wohnpit-role",0).edit().putString("role","central").commit();RollerObservationProof.reset(RollerLedger.class,"instance",null);
        var ctor=RollerStore.class.getDeclaredConstructor(android.content.Context.class);ctor.setAccessible(true);RollerStore store=ctor.newInstance(ctx);
        JSONObject profile=RollerStore.checked(new JSONObject()).put("mac",MAC).put("chargePath","relay").put("autoSpecialUse",true).put("idleWatts",0).put("conversionEfficiency",.8).put("targetPercent",90);store.configure(profile.toString());
        RollerLedger ledger=RollerLedger.get(ctx);String source=RollerLedger.source(repo.prefs);long now=System.currentTimeMillis(),a=now/D*D-D+10*H;
        for(long t=a-2000;t<=a+124000;t+=2000){boolean charging=t>=a&&t<a+60000;repo.prefs.edit().putLong("outputMeasuredAt",t).putInt("outputMeasuredState",charging?1:0).commit();ledger.loadRaw(source,REG,new JSONObject().put("at",t).put("state",charging?1:0),false,MAC,true,"relay");store.accept(packet(charging),t);}
        JSONObject roller=store.snapshot(),data=ledger.usageLearning(source,roller,now,"UTC");
        check(roller.getJSONArray("journal").length()==1,"the real automatic detector records one minute of charging after its normal start/idle confirmation");
        check(data.getJSONArray("charges").length()==1,"confirmed OFF time during end detection does not discard the automatically captured short charge");
        double wh=data.getJSONArray("charges").getJSONObject(0).getDouble("inputWh");check(wh>2.2&&wh<2.6,"automatic top-up learns approximately its 2.5 Wh and never extrapolates to a full battery");ledger.close();
    }
    public static void main(String[] args){try{
        Class.forName("org.sqlite.JDBC");System.setProperty("wattpit.test.namedDatabases","true");TimeZone.setDefault(TimeZone.getTimeZone("UTC"));Path root=Path.of(args[0]);Files.createDirectories(root);
        LanSyncProof.TestContext context=new LanSyncProof.TestContext(root.resolve("device"));RollerLedger ledger=new RollerLedger(context,"sampled.db");
        long now=System.currentTimeMillis(),start=now/D*D-8*D;
        JSONObject profile=RollerStore.checked(new JSONObject()).put("mac",MAC).put("chargePath","relay").put("idleWatts",0).put("conversionEfficiency",.8).put("targetPercent",90);
        JSONArray journal=new JSONArray();
        ledger.getWritableDatabase().beginTransaction();try{
            for(int day=0;day<6;day++){
                long a=start+day*D,charge=a+10*H,duration=day==4?H:day==5?60000:H/2,end=charge+duration;
                // Polls do not coincide with midnight. Day four really has a two-hour outage.
                for(long t=a+15000;t<a+D;t+=30000){if(day==3&&t>=a+12*H&&t<a+14*H)continue;int state=day!=3&&t>=charge&&t<=end?1:0;ledger.loadRaw(SOURCE,REG,new JSONObject().put("at",t).put("state",state),false,MAC,true,"relay");}
                if(day==3)continue;
                boolean fragmented=day<3;
                for(long t=charge;t<end;t+=10000){
                    if(fragmented&&((t-charge)/10000%30==0||t>=charge+H/4&&t<charge+H/4+30000))continue;
                    if(day==4&&t>=charge+20*60000&&t<charge+24*60000)continue;
                    long session=fragmented&&t>=charge+H/4+30000?charge+H/4+30000:charge;
                    ledger.rawAdd(SOURCE,MAC,session,t,t+10000,120,120,0,.8,meta(t),meta(t+10000));
                }
                if(fragmented){journal.put(entry(profile,charge,charge+H/4));journal.put(entry(profile,charge+H/4+30000,end));}
                else journal.put(entry(profile,charge,end));
            }
            ledger.getWritableDatabase().setTransactionSuccessful();
        }finally{ledger.getWritableDatabase().endTransaction();}
        JSONObject roller=new JSONObject().put("profile",profile).put("journal",journal),data=ledger.usageLearning(SOURCE,roller,now,"UTC");
        JSONArray charges=data.getJSONArray("charges");
        check(charges.length()==5,"sampled charges, a one-minute top-up and the valid energy around a long hole all teach");
        for(int i=0;i<3;i++){
            JSONObject e=charges.getJSONObject(i);check(e.getInt("fragments")==2,"sampling fragments "+i+" count as one charging occasion");
            check(Math.abs(e.getDouble("inputWh")-75)<1e-8&&e.getDouble("observedInputWh")<75&&e.getDouble("estimatedGapWh")>0,"bounded gaps "+i+" are estimated separately from the observed energy");
        }
        JSONObject partial=charges.getJSONObject(3);
        check(partial.getBoolean("partsOnly")&&!partial.getBoolean("amountComplete")&&Math.abs(partial.getDouble("inputWh")-140)<1e-8&&partial.getDouble("estimatedGapWh")==0,"a four-minute hole retains exactly 140 observed Wh without inventing the missing ten Wh");
        check(partial.getString("missingReason").contains("Messlücke"),"the retained partial charge explains why its total is unknown");
        JSONObject topup=charges.getJSONObject(4);
        check(topup.getLong("to")-topup.getLong("from")==60000&&Math.abs(topup.getDouble("inputWh")-2.5)<1e-8,"a one-minute partial charge contributes its own 2.5 Wh with no battery-full or target requirement");
        check(data.getInt("excludedCharges")==0,"a high-coverage charge with a long hole no longer disappears from learning");
        int monitored=0;JSONObject days=data.getJSONObject("pathDays");
        for(Iterator<String> it=days.keys();it.hasNext();){JSONObject d=days.getJSONObject(it.next());if(d.getBoolean("complete")){monitored++;check(d.getLong("maxGapMillis")<=120000,"accepted frequency days have only bounded sampling gaps");}}
        check(monitored==4,"midnight sampling boundaries and brief top-ups teach frequency; the two-hour outage and incomplete charge stay unknown");
        check(data.toString().equals(ledger.usageLearning(SOURCE,roller,now,"UTC").toString()),"repeating sampling recovery never duplicates charge energy or events");
        check(roller.getJSONArray("journal").length()==8,"learning does not rewrite or combine the stored journal entries");
        Files.writeString(root.resolve("result.json"),new JSONObject().put("rollerLearning",data).put("roller",roller).put("now",now).toString());
        ledger.close();automaticTopup(root.resolve("automatic-topup"));System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
