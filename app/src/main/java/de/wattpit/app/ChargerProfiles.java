package de.wattpit.app;

import org.json.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

/** Three physical chargers; secret material never leaves the collecting installation. */
final class ChargerProfiles {
    static final String KEY="autarpie-chargers-v1";
    private static JSONObject state(Repository repo)throws JSONException{
        String stored=repo.prefs.getString(KEY,"");
        if(!stored.isEmpty())return new JSONObject(stored);
        JSONObject old=new JSONObject(repo.prefs.getString(EnergyStore.CHARGER,"{}"));
        JSONArray profiles=new JSONArray();if(!old.optString("mac").isEmpty())profiles.put(old);
        return new JSONObject().put("version",1).put("selected",old.optString("mac")).put("profiles",profiles);
    }
    static synchronized JSONArray all(Repository repo)throws JSONException{return state(repo).getJSONArray("profiles");}
    static synchronized JSONObject selected(Repository repo)throws JSONException{
        JSONObject state=state(repo);JSONArray profiles=state.getJSONArray("profiles");String mac=state.optString("selected");
        for(int i=0;i<profiles.length();i++)if(mac.equals(profiles.getJSONObject(i).optString("mac")))return profiles.getJSONObject(i);
        return profiles.length()>0?profiles.getJSONObject(0):new JSONObject();
    }
    static synchronized JSONObject find(Repository repo,String mac)throws JSONException{
        JSONArray profiles=all(repo);for(int i=0;i<profiles.length();i++)if(mac.equals(profiles.getJSONObject(i).optString("mac")))return profiles.getJSONObject(i);
        return new JSONObject();
    }
    private static void commit(Repository repo,JSONObject state)throws Exception{
        JSONObject selected=new JSONObject();JSONArray profiles=state.getJSONArray("profiles");
        for(int i=0;i<profiles.length();i++)if(profiles.getJSONObject(i).optString("mac").equals(state.optString("selected")))selected=profiles.getJSONObject(i);
        if(selected.length()==0&&profiles.length()>0){selected=profiles.getJSONObject(0);state.put("selected",selected.getString("mac"));}
        if(!repo.prefs.edit().putString(KEY,state.toString()).putString(EnergyStore.CHARGER,selected.toString()).commit())throw new IOException("Laderprofile konnten nicht gespeichert werden.");
    }
    static synchronized void save(Repository repo,String json)throws Exception{
        if(json==null||json.length()>5000)throw new IllegalArgumentException("Ungültiges Laderprofil.");
        JSONObject in=new JSONObject(json),state=state(repo);JSONArray list=state.getJSONArray("profiles");
        String mac=in.optString("mac").trim().toUpperCase(Locale.ROOT),role=in.optString("role","model"),name=in.optString("name","608PD").trim();
        if(!mac.matches("(?:[0-9A-F]{2}:){5}[0-9A-F]{2}"))throw new IllegalArgumentException("Bitte einen gefundenen 608PD wählen oder seine Bluetooth-Adresse eingeben.");
        if(!Arrays.asList("model","house-source","other").contains(role))throw new IllegalArgumentException("Ungültiger Energiepfad.");
        JSONObject old=find(repo,mac);int index=-1;for(int i=0;i<list.length();i++)if(list.getJSONObject(i).optString("mac").equals(mac))index=i;
        if(index<0&&list.length()>=3)throw new IllegalArgumentException("Drei Lader sind bereits gespeichert. Zum Ersetzen zuerst das entsprechende Profil entfernen; sein Journal bleibt erhalten.");
        JSONObject clean=new JSONObject().put("mac",mac).put("name",name.isEmpty()?"608PD":name.substring(0,Math.min(60,name.length()))).put("role",role).put("enabled",in.optBoolean("enabled"));
        for(String key:new String[]{"secret","confirmed","pairingPending"})if(old.has(key))clean.put(key,old.get(key));
        String auth=in.optString("auth");
        if(!auth.isEmpty()){
            if(!auth.matches("[0-9a-fA-F]{32}"))throw new IllegalArgumentException("Die private Verbindungskennung muss 32 Hexzeichen haben.");
            byte[] bytes=new byte[16];try{for(int i=0;i<16;i++)bytes[i]=(byte)Integer.parseInt(auth.substring(i*2,i*2+2),16);clean.put("secret",KeyVault.encrypt(bytes)).put("confirmed",true).put("pairingPending",false);}finally{Arrays.fill(bytes,(byte)0);}
        }
        if(clean.optBoolean("enabled")&&(!clean.has("secret")||!clean.optBoolean("confirmed",true)))throw new IllegalArgumentException("Bitte zuerst Neue Verbindung am Lader bestätigen wählen.");
        if(index<0)list.put(clean);else list.put(index,clean);state.put("selected",mac);commit(repo,state);
    }
    static synchronized void select(Repository repo,String mac)throws Exception{
        JSONObject state=state(repo);if(find(repo,mac).length()==0)throw new IllegalArgumentException("Laderprofil nicht gefunden.");state.put("selected",mac);commit(repo,state);
    }
    static synchronized void remove(Repository repo,String mac)throws Exception{
        JSONObject state=state(repo);JSONArray previous=state.getJSONArray("profiles"),next=new JSONArray();for(int i=0;i<previous.length();i++)if(!mac.equals(previous.getJSONObject(i).optString("mac")))next.put(previous.getJSONObject(i));
        state.put("profiles",next);commit(repo,state);
    }
    static synchronized void beginPairing(Repository repo,String json)throws Exception{
        JSONObject in=new JSONObject(json);in.put("enabled",false);save(repo,in.toString());
        JSONObject state=state(repo);JSONArray list=state.getJSONArray("profiles");String mac=state.optString("selected");
        for(int i=0;i<list.length();i++){
            JSONObject value=list.getJSONObject(i);if(!mac.equals(value.optString("mac")))continue;
            // Reuse a valid imported identity; never replace it with a new random one.
            if(!value.has("secret")){byte[] key=new byte[16];new SecureRandom().nextBytes(key);try{value.put("secret",KeyVault.encrypt(key)).put("confirmed",false);}finally{Arrays.fill(key,(byte)0);}}
            value.put("enabled",true).put("pairingPending",!value.optBoolean("confirmed",true));
        }commit(repo,state);
    }
    static synchronized void confirmed(Repository repo,String mac)throws Exception{updatePairing(repo,mac,true);}
    static synchronized void pairingFailed(Repository repo,String mac)throws Exception{updatePairing(repo,mac,false);}
    private static void updatePairing(Repository repo,String mac,boolean success)throws Exception{
        JSONObject state=state(repo);JSONArray profiles=state.getJSONArray("profiles");boolean changed=false;
        for(int i=0;i<profiles.length();i++){JSONObject p=profiles.getJSONObject(i);if(mac.equals(p.optString("mac"))&&p.optBoolean("pairingPending")){p.put("pairingPending",false).put("confirmed",success).put("enabled",success);changed=true;}}
        if(changed)commit(repo,state);
    }
    static JSONObject publicValue(JSONObject privateValue)throws JSONException{
        JSONObject value=new JSONObject(privateValue.toString());value.put("hasAuth",value.has("secret")&&value.optBoolean("confirmed",true));value.remove("secret");return value;
    }
    static synchronized JSONArray publicList(Repository repo)throws JSONException{JSONArray list=all(repo),out=new JSONArray();for(int i=0;i<list.length();i++)out.put(publicValue(list.getJSONObject(i)));return out;}
    private ChargerProfiles(){}
}
