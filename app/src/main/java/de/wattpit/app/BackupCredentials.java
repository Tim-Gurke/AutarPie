package de.wattpit.app;
import org.json.*;
import java.util.*;

/** Portable device credentials only inside a password-authenticated full backup. */
final class BackupCredentials {
    static void transform(JSONObject preferences,boolean exporting)throws Exception{
        for(Iterator<String> stores=preferences.keys();stores.hasNext();){JSONObject store=preferences.getJSONObject(stores.next());for(Iterator<String> keys=store.keys();keys.hasNext();){JSONObject e=store.getJSONObject(keys.next());if(!"string".equals(e.optString("type")))continue;String text=e.getString("value");Object json;if(text.startsWith("{"))json=new JSONObject(text);else if(text.startsWith("["))json=new JSONArray(text);else continue;walk(json,exporting);e.put("value",json.toString());}}
    }
    private static void walk(Object value,boolean exporting)throws Exception{
        if(value instanceof JSONArray){JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++)walk(a.get(i),exporting);return;}if(!(value instanceof JSONObject))return;JSONObject o=(JSONObject)value;
        if(exporting&&o.has("backupPortableSecret"))throw new IllegalArgumentException("Ungültiger Sicherungs-Schlüsselstand.");
        if(!exporting&&o.has("secret")&&!o.has("backupPortableSecret"))throw new IllegalArgumentException("Geräteschlüssel ist noch an die Quellinstallation gebunden.");
        if(exporting&&o.has("secret")){byte[] b=KeyVault.decrypt(o.getString("secret"));try{o.remove("secret");o.put("backupPortableSecret",Base64.getEncoder().encodeToString(b));}finally{Arrays.fill(b,(byte)0);}}
        else if(!exporting&&o.has("backupPortableSecret")){if(o.has("secret"))throw new IllegalArgumentException("Mehrdeutiger Geräteschlüssel.");byte[] b=Base64.getDecoder().decode(o.getString("backupPortableSecret"));try{if(b.length!=16)throw new IllegalArgumentException("Ungültiger Geräteschlüssel.");o.put("secret",KeyVault.encrypt(b));o.remove("backupPortableSecret");}finally{Arrays.fill(b,(byte)0);}}
        for(Iterator<String> it=o.keys();it.hasNext();)walk(o.get(it.next()),exporting);
    }
    private BackupCredentials(){}
}
