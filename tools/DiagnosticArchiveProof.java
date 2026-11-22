package de.wattpit.app;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Read every generated ZIP entry after real supplier, storage and partial-write failures. */
public final class DiagnosticArchiveProof {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    private static Map<String,String> entries(byte[] bytes)throws Exception{
        Map<String,String> result=new LinkedHashMap<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;while((entry=zip.getNextEntry())!=null){result.put(entry.getName(),new String(zip.readAllBytes(),StandardCharsets.UTF_8));zip.closeEntry();}}return result;
    }
    public static void main(String[] args)throws Exception{
        ByteArrayOutputStream complete=new ByteArrayOutputStream();boolean ok=DiagnosticArchive.write(complete,new JSONObject(),()->new JSONObject().put("fixture",true),out->out.write("timestamp,value\n1,42\n".getBytes(StandardCharsets.UTF_8)));
        Map<String,String> values=entries(complete.toByteArray());check(ok&&new JSONObject(values.get("export-result.json")).getString("status").equals("complete"),"a complete diagnostic ZIP reports successful details and history");
        check(new JSONObject(values.get("diagnose.json")).getBoolean("fixture")&&values.get("verlauf.csv").contains("1,42"),"the diagnostic JSON and actual CSV remain readable");
        ByteArrayOutputStream failed=new ByteArrayOutputStream();boolean partial=DiagnosticArchive.write(failed,new JSONObject().put("viewerTiming",new JSONObject().put("networkMs",123)),()->{check(failed.size()>0,"the export writes a header before attempting the failing analysis");throw new JSONException("synthetic invalid stored data");},out->out.write("timestamp,value\n1,42\n".getBytes(StandardCharsets.UTF_8)));
        values=entries(failed.toByteArray());check(!partial&&failed.size()>0&&new JSONObject(values.get("export-result.json")).getString("status").equals("partial"),"failed analysis still produces a nonempty, readable partial archive");
        JSONObject details=new JSONObject(values.get("diagnose.json"));check(details.getBoolean("partial")&&details.getJSONObject("viewerTiming").getInt("networkMs")==123&&details.getJSONArray("exportErrors").length()==1,"timings and an explicit failure report survive an unreadable analysis");
        check(values.get("verlauf.csv").contains("1,42"),"raw measurements can be exported even when learning data is broken");
        ByteArrayOutputStream storage=new ByteArrayOutputStream();ok=DiagnosticArchive.write(storage,new JSONObject(),()->new JSONObject().put("fixture",true),out->{out.write("timestamp,value\n".getBytes(StandardCharsets.UTF_8));throw new IOException("synthetic read failure");});
        values=entries(storage.toByteArray());check(!ok&&new JSONObject(values.get("export-result.json")).getJSONArray("errors").getJSONObject(0).getString("part").equals("verlauf"),"a partial CSV read is labelled and the following ZIP entry is intact");
        check(new JSONObject(values.get("diagnose.json")).getBoolean("fixture"),"a history read failure does not discard the diagnostic JSON");
        boolean rejected=false;try{DiagnosticArchive.write(new OutputStream(){public void write(int b)throws IOException{throw new IOException("synthetic provider failure");}},new JSONObject(),JSONObject::new,out->{});}catch(IOException expected){rejected=true;}
        check(rejected,"a failing output provider is reported instead of claiming a saved diagnosis");
        System.out.println("Diagnostic export checks: "+checks);
    }
}
