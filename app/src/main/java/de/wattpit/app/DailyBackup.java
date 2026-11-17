package de.wattpit.app;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class DailyBackup {
    private static final AtomicBoolean ACTIVE=new AtomicBoolean();static boolean running(){return ACTIVE.get();}
    private static final long LIMIT=MigrationBackup.MAX_BYTES;
    static final class Deferred extends IOException{Deferred(String message){super(message);}}
    static void run(Context context)throws Exception{
        Context c=context.getApplicationContext();SharedPreferences p=BackupScheduler.prefs(c);if(MigrationBackup.pending(c))throw new IOException("Wiederherstellung wartet auf Neustart.");if(!ACTIVE.compareAndSet(false,true))throw new IOException("Eine Sicherung läuft bereits.");
        File staged=new File(c.getCacheDir(),"daily-backup-encrypted.tmp");Uri partial=null,completed=null;char[] password=null;
        try{
            if(Repository.get(c).archiveBusy)throw new Deferred("Archivabruf läuft; die Sicherung später erneut versuchen.");
            Uri tree=Uri.parse(p.getString("tree",""));if(tree.toString().isEmpty())throw new IOException("Bitte einen Sicherungsordner wählen.");byte[] b=KeyVault.decrypt(p.getString("password",""));try{password=new String(b,StandardCharsets.UTF_8).toCharArray();}finally{Arrays.fill(b,(byte)0);}
            p.edit().putString("progress","Datenstand wird gesichert und verschlüsselt …").remove("error").commit();try(OutputStream out=new FileOutputStream(staged)){MigrationBackup.exportLive(c,out,password);}check();if(staged.length()>LIMIT)throw new IOException("Sicherung ist zu groß.");
            byte[] expected;try(InputStream in=new FileInputStream(staged)){expected=digest(in);}ContentResolver r=c.getContentResolver();Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));String owner=p.getString("owner","");if(!owner.matches("[a-f0-9]{32}"))throw new IOException("Sicherung erneut einrichten.");String name="AutarPie-"+owner+"-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+"-"+UUID.randomUUID().toString().substring(0,8)+".wohnpit";
            p.edit().putString("progress","Verschlüsselte Datei wird gespeichert und geprüft …").apply();partial=DocumentsContract.createDocument(r,parent,"application/octet-stream",name+".partial");if(partial==null)throw new IOException("Datei konnte nicht erstellt werden.");try(InputStream in=new FileInputStream(staged);OutputStream out=r.openOutputStream(partial,"wt")){if(out==null)throw new IOException("Ziel ist nicht beschreibbar.");copy(in,out);}verify(r,partial,expected);check();
            try{completed=DocumentsContract.renameDocument(r,partial,name);}catch(Exception ignored){}if(completed!=null)partial=null;else{completed=DocumentsContract.createDocument(r,parent,"application/octet-stream",name);if(completed==null)throw new IOException("Sicherung nicht abgeschlossen.");try(InputStream in=new FileInputStream(staged);OutputStream out=r.openOutputStream(completed,"wt")){if(out==null)throw new IOException("Ziel ist nicht beschreibbar.");copy(in,out);}}
            verify(r,completed,expected);check();if(partial!=null){try{DocumentsContract.deleteDocument(r,partial);}catch(Exception ignored){}partial=null;}
            Uri newest=completed;if(!p.edit().putLong("success",System.currentTimeMillis()).putString("lastFile",newest.toString()).putLong("bytes",staged.length()).putString("progress","").remove("error").commit())throw new IOException("Sicherungsstatus nicht gespeichert.");completed=null;
            try{retain(r,tree,owner,p.getInt("keep",7),newest);}catch(Exception e){p.edit().putString("error","Sicherung gespeichert; ältere Stände konnten nicht alle entfernt werden.").apply();}
        }catch(Exception e){p.edit().putString("progress","").putString("error",e instanceof Deferred?e.getMessage():Thread.currentThread().isInterrupted()?"Sicherung durch Android unterbrochen; wird später erneut versucht.":"Sicherung fehlgeschlagen. Zielzugriff, Speicherplatz und gegebenenfalls Internet prüfen.").commit();throw e;}
        finally{if(password!=null)Arrays.fill(password,'\0');if(partial!=null)try{DocumentsContract.deleteDocument(c.getContentResolver(),partial);}catch(Exception ignored){}if(completed!=null)try{DocumentsContract.deleteDocument(c.getContentResolver(),completed);}catch(Exception ignored){}staged.delete();ACTIVE.set(false);}
    }
    private static void check()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Sicherung unterbrochen.");}
    private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];long total=0;int n;while((n=in.read(b))!=-1){check();total+=n;if(total>LIMIT)throw new IOException("Sicherung ist zu groß.");out.write(b,0,n);}}
    private static byte[] digest(InputStream in)throws Exception{if(in==null)throw new IOException("Sicherung nicht lesbar.");MessageDigest sha=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];long total=0;int n;while((n=in.read(b))!=-1){check();total+=n;if(total>LIMIT)throw new IOException("Sicherung ist zu groß.");sha.update(b,0,n);}return sha.digest();}
    private static void verify(ContentResolver r,Uri uri,byte[] expected)throws Exception{try(InputStream in=r.openInputStream(uri)){if(!MessageDigest.isEqual(expected,digest(in)))throw new IOException("Rückleseprüfung fehlgeschlagen.");}}
    private static void retain(ContentResolver r,Uri tree,String owner,int keep,Uri newest)throws Exception{
        keep=Math.max(2,Math.min(30,keep));List<String[]> files=new ArrayList<>();Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        try(Cursor c=r.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(c==null)return;while(c.moveToNext()){String name=c.getString(1);if(name!=null&&name.matches("AutarPie-"+owner+"-[0-9]{8}-[0-9]{6}-[a-f0-9]{8}\\.wohnpit")&&!c.getString(0).equals(DocumentsContract.getDocumentId(newest)))files.add(new String[]{c.getString(0),name});}}
        files.sort((a,b)->b[1].compareTo(a[1]));for(int i=keep-1;i<files.size();i++)DocumentsContract.deleteDocument(r,DocumentsContract.buildDocumentUriUsingTree(tree,files.get(i)[0]));
    }
    private DailyBackup(){}
}
