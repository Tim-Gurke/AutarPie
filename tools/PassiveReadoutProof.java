package de.wattpit.app;

import org.json.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

/** Synthetic encrypted advertisements, with real production decoding and snapshot selection. */
public final class PassiveReadoutProof {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    private static JSONArray devices(byte[] key,String kind)throws Exception{return new JSONArray().put(new JSONObject().put("mac","AA:BB:CC:DD:EE:01").put("kind",kind).put("readoutKey",Base64.getEncoder().encodeToString(key)));}
    private static byte[] packet(byte[] key)throws Exception{
        byte[] payload=new byte[12];payload[0]=3;payload[2]=0x28;payload[3]=5;payload[4]=20;payload[8]=123;
        byte[] iv=new byte[16];iv[0]=17;Cipher cipher=Cipher.getInstance("AES/CTR/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new IvParameterSpec(iv));
        byte[] encrypted=cipher.doFinal(payload),out=new byte[8+encrypted.length];out[0]=0x10;out[4]=1;out[5]=17;out[7]=key[0];System.arraycopy(encrypted,0,out,8,encrypted.length);return out;
    }
    private static JSONObject snapshot(long now)throws Exception{return new JSONObject().put("running",false).put("historyRevision",73).put("weather",new JSONObject().put("place","central-place")).put("devices",new JSONArray().put(new JSONObject().put("mac","AA:BB:CC:DD:EE:01").put("kind","solar").put("error","central error"))).put("readings",new JSONArray().put(new JSONObject().put("device","AA:BB:CC:DD:EE:01").put("kind","solar").put("t",now-2000).put("solar",55))).put("roller",new JSONObject().put("owner","central"));}
    public static void main(String[] args)throws Exception{
        long now=1760000000000L;byte[] key=new byte[16];Arrays.fill(key,(byte)0x31);byte[] packet=packet(key);PassiveReadout readout=new PassiveReadout();readout.configure(devices(key,"solar"));
        check(!readout.receive("AA:BB:CC:DD:EE:02",packet,now,now),"an unconfigured neighbour never enters the selected plant");
        check(!readout.receive("AA:BB:CC:DD:EE:01",packet,now-10001,now),"an old Android scan result cannot make a value look current");
        check(!readout.receive("AA:BB:CC:DD:EE:01",packet,now+1,now),"future receive timestamps are rejected");
        check(readout.receive("AA:BB:CC:DD:EE:01",packet,now,now),"a matching encrypted solar advertisement is decoded");
        JSONObject value=snapshot(now);check(readout.overlay(value,now)==1&&value.getJSONArray("readings").getJSONObject(0).getDouble("solar")==123,"fresh passive values take precedence over an older central snapshot");
        check(value.getJSONArray("readings").getJSONObject(0).getString("liveSource").equals("direct")&&!value.getBoolean("running"),"direct reception is labelled without claiming the central recording is running");
        check(value.getLong("historyRevision")==73&&value.getJSONObject("weather").getString("place").equals("central-place")&&value.getJSONObject("roller").getString("owner").equals("central"),"history identity, weather location and roller ownership stay with the central");
        readout.configure(devices(key,"solar"));check(readout.overlay(snapshot(now),now)==1,"refreshing unchanged credentials keeps an already received live value");
        JSONObject fallback=snapshot(now);check(readout.overlay(fallback,now+PassiveReadout.FRESH_MILLIS)==0&&fallback.getJSONArray("readings").getJSONObject(0).getDouble("solar")==55,"leaving radio range automatically falls back to the central value");
        byte[] replacement=key.clone();replacement[0]++;readout.configure(devices(replacement,"solar"));check(readout.overlay(snapshot(now),now)==0&&!readout.receive("AA:BB:CC:DD:EE:01",packet,now,now),"rotating a device key removes its previous values and rejects old-key broadcasts");
        readout.configure(devices(key,"shunt"));check(!readout.receive("AA:BB:CC:DD:EE:01",packet,now,now),"a solar packet cannot impersonate the configured battery monitor");
        readout.clear();check(readout.size()==0&&!readout.receive("AA:BB:CC:DD:EE:01",packet,now,now),"closing passive reception clears its in-memory credentials and readings");
        System.out.println("Passive readout checks: "+checks);
    }
}
