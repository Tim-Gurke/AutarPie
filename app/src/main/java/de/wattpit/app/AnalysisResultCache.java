package de.wattpit.app;

import android.content.Context;
import android.database.Cursor;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Bounded private results. The token covers actual inputs, not the last display refresh. */
final class AnalysisResultCache {
    static final int LIMIT=2*1024*1024, TOTAL=8*1024*1024;
    private final File directory;
    AnalysisResultCache(Context context,String plant){
        if(!plant.matches("[A-Za-z0-9_-]{1,80}"))throw new IllegalArgumentException();
        directory=new File(new File(context.getFilesDir(),"derived-results-v1"),plant);
    }
    static String hash(String text)throws Exception{
        return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    synchronized JSONObject read(String key,String token){
        try{File file=new File(directory,hash(key)+".json");if(!file.isFile()||file.length()>LIMIT)return null;
            JSONObject value=new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
            return token.equals(value.optString("token"))?value.getJSONObject("data"):null;
        }catch(Exception ignored){return null;}
    }
    synchronized void save(String key,String token,JSONObject data){
        if(data.has("error")||data.optBoolean("pending")||data.optBoolean("stale"))return;
        try{byte[] bytes=new JSONObject().put("token",token).put("data",data).toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>LIMIT)return;
            if(!directory.isDirectory()&&!directory.mkdirs())return;
            File target=new File(directory,hash(key)+".json"),temporary=new File(directory,"pending.tmp");
            Files.write(temporary.toPath(),bytes);Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
            File[] files=directory.listFiles((d,n)->n.endsWith(".json"));if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));
            long total=0;for(File file:files)total+=file.length();int count=files.length;
            for(File file:files){if(total<=TOTAL&&count<=64)break;long length=file.length();if(file.delete()){total-=length;count--;}}
        }catch(Exception ignored){}
    }
    static String token(Repository repo,SolarPlanning plan,RollerLedger ledger,long from,long to)throws Exception{
        StringBuilder value=new StringBuilder("v1:").append(SolarPlanning.ALGORITHM).append(':').append(repo.prefs.getString("analysis.epoch",""));
        value.append(HistoryExchange.cachedVersions(repo.db,from,to));
        for(String key:new String[]{"devices","equipment-v1","solar-setup-v1","weatherPlace","rollerChargeWindows",SpecialUses.KEY,"analysisTimezone"})value.append('|').append(repo.prefs.getString(key,""));
        value.append('|').append(repo.prefs.getFloat("capacity",100));
        // Hash rows only in the supported interval. A changed hour outside it cannot
        // invalidate a completed result; the preceding contexts remain dependencies.
        for(String table:new String[]{"contexts","display_contexts","model_weather","device_settings","input_intervals","roller_raw","load_raw","charger_raw","archive_clock_state"}){
            String where=AnalysisExchange.where(table,from,to);
            if(table.equals("display_contexts")||table.equals("contexts"))where="at<="+to+" AND (at>="+from+" OR at IN (SELECT MAX(at) FROM "+table+" WHERE at<"+from+" GROUP BY source))";
            value.append('|').append(table);
            try(Cursor c=AnalysisExchange.database(table,repo,plan,ledger,false).rawQuery("SELECT * FROM "+table+" WHERE "+where+" ORDER BY rowid",null)){
                MessageDigest digest=MessageDigest.getInstance("SHA-256");
                while(c.moveToNext())for(int i=0;i<c.getColumnCount();i++){String part=c.isNull(i)?"null":c.getString(i);byte[] bytes=part.getBytes(StandardCharsets.UTF_8);digest.update((bytes.length+":").getBytes(StandardCharsets.UTF_8));digest.update(bytes);}
                value.append(Base64.getEncoder().encodeToString(digest.digest()));
            }
        }
        return hash(value.toString());
    }
}
