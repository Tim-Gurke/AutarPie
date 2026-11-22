package de.wattpit.app;

import de.wattpit.app.core.Reading;
import de.wattpit.app.core.VictronDecoder;
import org.json.*;
import java.util.*;

/** In-memory, read-only Instant Readout. It never writes history or opens a device connection. */
final class PassiveReadout {
    static final long FRESH_MILLIS=15000;
    private final Map<String,byte[]> keys=new HashMap<>();
    private final Map<String,String> kinds=new HashMap<>();
    private final Map<String,Reading> readings=new HashMap<>();
    synchronized void configure(JSONArray devices)throws Exception{
        Map<String,byte[]> next=new HashMap<>();Map<String,String> types=new HashMap<>();
        try{
            if(devices.length()>32)throw new IllegalArgumentException("Zu viele Empfangsgeräte.");
            for(int i=0;i<devices.length();i++){
                JSONObject d=devices.getJSONObject(i);String mac=VictronDecoder.normalizeMac(d.getString("mac")),kind=d.getString("kind");
                if(!kind.equals("solar")&&!kind.equals("shunt")||next.containsKey(mac))throw new IllegalArgumentException("Ungültiges Empfangsgerät.");
                byte[] key=Base64.getDecoder().decode(d.getString("readoutKey"));if(key.length!=16)throw new IllegalArgumentException("Ungültiger Empfangsschlüssel.");
                next.put(mac,key);types.put(mac,kind);
            }
        }catch(Exception e){for(byte[] key:next.values())Arrays.fill(key,(byte)0);throw e;}
        readings.entrySet().removeIf(e->!types.getOrDefault(e.getKey(),"").equals(kinds.get(e.getKey()))||!Arrays.equals(keys.get(e.getKey()),next.get(e.getKey())));
        for(byte[] key:keys.values())Arrays.fill(key,(byte)0);keys.clear();keys.putAll(next);kinds.clear();kinds.putAll(types);
    }
    synchronized void clear(){for(byte[] key:keys.values())Arrays.fill(key,(byte)0);keys.clear();kinds.clear();readings.clear();}
    synchronized int size(){return keys.size();}
    synchronized boolean receive(String address,byte[] packet,long at,long now){
        try{
            if(at>now||now-at>10000)return false;String mac=VictronDecoder.normalizeMac(address);byte[] key=keys.get(mac);if(key==null)return false;
            Reading r=VictronDecoder.decode(packet,key);if(!r.kind.equals(kinds.get(mac)))return false;
            Reading previous=readings.get(mac);if(previous!=null&&at<previous.timestamp)return false;
            r.device=mac;r.timestamp=at;readings.put(mac,r);return true;
        }catch(Exception ignored){return false;}
    }
    synchronized int overlay(JSONObject snapshot,long now)throws Exception{
        JSONArray devices=snapshot.optJSONArray("devices"),old=snapshot.optJSONArray("readings"),merged=new JSONArray();int direct=0;
        if(devices==null)return 0;
        for(int i=0;i<devices.length();i++){
            JSONObject device=devices.getJSONObject(i);String address=device.optString("mac");Reading r=readings.get(address.toUpperCase(Locale.ROOT));JSONObject central=null;
            if(old!=null)for(int j=0;j<old.length();j++)if(address.equalsIgnoreCase(old.getJSONObject(j).optString("device"))){central=old.getJSONObject(j);break;}
            if(r!=null&&r.kind.equals(device.optString("kind"))&&now>=r.timestamp&&now-r.timestamp<FRESH_MILLIS){
                JSONObject live=HistoryDb.json(r).put("liveSource","direct");live.put("device",address);merged.put(live);device.remove("error");device.put("seen",r.timestamp);direct++;
            }else if(central!=null)merged.put(central);
        }
        snapshot.put("readings",merged);return direct;
    }
}
