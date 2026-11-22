package de.wattpit.app;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/** A failed analysis still produces a readable diagnostic archive with an explicit partial status. */
final class DiagnosticArchive {
    interface Details {JSONObject read()throws Exception;}
    interface History {void write(OutputStream out)throws Exception;}
    private static void json(ZipOutputStream zip,String name,JSONObject value)throws Exception{
        zip.putNextEntry(new ZipEntry(name));zip.write(value.toString(2).getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    }
    static boolean write(OutputStream destination,JSONObject base,Details details,History history)throws Exception{
        JSONArray errors=new JSONArray();
        try(ZipOutputStream zip=new ZipOutputStream(destination)){
            json(zip,"export-start.json",new JSONObject().put("appVersion",BuildConfig.VERSION_NAME).put("at",System.currentTimeMillis()).put("status","started"));zip.flush();
            JSONObject diagnostic;
            try{diagnostic=details.read();}catch(Exception e){errors.put(failure("diagnose",e));diagnostic=new JSONObject(base.toString()).put("partial",true).put("exportErrors",errors);}
            json(zip,"diagnose.json",diagnostic);
            zip.putNextEntry(new ZipEntry("verlauf.csv"));try{history.write(zip);}catch(Exception e){errors.put(failure("verlauf",e));}finally{zip.closeEntry();}
            json(zip,"export-result.json",new JSONObject().put("status",errors.length()==0?"complete":"partial").put("errors",errors).put("at",System.currentTimeMillis()));
        }
        return errors.length()==0;
    }
    private static JSONObject failure(String part,Exception e)throws JSONException{
        JSONArray stack=new JSONArray();for(StackTraceElement line:e.getStackTrace()){if(stack.length()>=10)break;stack.put(line.toString());}
        return new JSONObject().put("part",part).put("exception",e.getClass().getName()).put("stack",stack);
    }
    private DiagnosticArchive(){}
}
