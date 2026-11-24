package de.wattpit.app;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Durable compiled factors, independent of raw/history/network queues and cache eviction. */
final class LearnedModelStore {
    static final int LIMIT=2*1024*1024;
    private static File file(Context context,String plant,String kind)throws IOException{
        if(!plant.matches("[A-Za-z0-9_-]{1,80}")||!kind.matches("solar|supplies|energy"))throw new IOException("Ungültiger Lernspeicher.");
        return new File(new File(context.getFilesDir(),"learned-models-v1"),plant+"-"+kind+".json");
    }
    static synchronized JSONObject read(Context context,String plant,String kind,String key,long now){
        try{File f=file(context,plant,kind);if(!f.isFile()||f.length()>LIMIT)return null;
            JSONObject data=new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));
            long at=data.optLong("at");return data.optInt("version")==1&&key.equals(data.optString("key"))&&at>0&&at<=now+5000?data:null;
        }catch(Exception ignored){return null;}
    }
    static synchronized void save(Context context,String plant,String kind,String key,JSONObject data,long now)throws Exception{
        long at=data.optLong("at");if(data.optInt("version")!=1||!key.equals(data.optString("key"))||key.length()>64000||at<=0||at>now+5000||data.has("error"))throw new IOException("Ungültiges Lernmodell.");
        byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>LIMIT)throw new IOException("Lernmodell ist zu groß.");
        File target=file(context,plant,kind);Files.createDirectories(target.getParentFile().toPath());Path temporary=new File(target.getPath()+".tmp").toPath();
        try{Files.write(temporary,bytes);Files.move(temporary,target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}
    }
    private LearnedModelStore(){}
}
