package de.wattpit.app;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Disposable, device-local derived knowledge. Never substitutes a cached battery reading. */
final class SolarPreviewCache {
    static final int LIMIT=8*1024*1024;
    private static File file(Context c,String name)throws IOException{
        if(!name.matches("[A-Za-z0-9_.-]{1,120}")||name.contains(".."))throw new IOException("Ungültiger Vorschau-Speicher.");
        return new File(new File(c.getFilesDir(),"solar-preview"),name+".json");
    }
    static synchronized JSONObject read(Context c,String name,String source,String epoch,long now){
        try{
            File f=file(c,name);if(!f.isFile()||f.length()>LIMIT)return null;
            JSONObject saved=new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));
            if(saved.optInt("algorithm")!=SolarPlanning.ALGORITHM||!source.equals(saved.optString("source"))||!epoch.equals(saved.optString("epoch")))return null;
            JSONObject data=saved.getJSONObject("data");long at=data.optLong("updated");
            if(at<=0||at>now+5000||data.has("error")||!source.equals(data.optString("source"))||data.optJSONArray("observations")==null||data.getJSONArray("observations").length()==0)return null;
            return data.put("cached",true).put("stale",true);
        }catch(Exception ignored){return null;}
    }
    static synchronized void save(Context c,String name,String epoch,JSONObject data)throws Exception{
        if(data.has("error"))return;
        File target=file(c,name);JSONArray rows=data.optJSONArray("observations");
        if(rows==null)return;if(rows.length()==0){Files.deleteIfExists(target.toPath());return;}
        JSONObject value=new JSONObject(data.toString());value.remove("cached");value.remove("stale");value.remove("analysis");
        byte[] bytes=new JSONObject().put("algorithm",SolarPlanning.ALGORITHM).put("source",value.getString("source")).put("epoch",epoch).put("data",value).toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>LIMIT)throw new IOException("Vorschau-Zwischenstand ist zu groß.");
        Files.createDirectories(target.getParentFile().toPath());Path temp=new File(target.getPath()+".tmp").toPath();
        try{Files.write(temp,bytes);Files.move(temp,target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temp);}
    }
    static synchronized void clear(Context c,String name){try{Files.deleteIfExists(file(c,name).toPath());}catch(Exception ignored){}}
    private SolarPreviewCache(){}
}
