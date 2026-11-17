package de.wattpit.app;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.platform.app.InstrumentationRegistry;
import junit.framework.TestCase;
import java.io.*;
import org.json.JSONObject;

/** Real Android SAF grants/provider I/O, retention/failure paths and encrypted archive validation. */
public final class BackupStorageTest extends TestCase {
    private int files(ContentResolver r,Uri tree)throws Exception{int n=0;try(Cursor c=r.query(DocumentsContract.buildChildDocumentsUriUsingTree(tree,"root"),new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){while(c.moveToNext()){assertTrue(c.getString(0).endsWith(".wohnpit"));n++;}}return n;}
    public void testDailyBackupThroughDocumentsProvider()throws Exception{
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue((c.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0);ContentResolver r=c.getContentResolver();Uri control=Uri.parse("content://"+BackupDocumentsProvider.AUTH);r.call(control,"faultProof","",null);Uri tree=Uri.parse(r.call(control,"grantProof",c.getPackageName(),null).getString("uri"));r.takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        c.stopService(new Intent(c,MonitorService.class));c.stopService(new Intent(c,ArchiveService.class));Repository repo=Repository.get(c);repo.running=false;repo.archiveBusy=false;repo.db.getWritableDatabase();repo.prefs.edit().putBoolean("recording",true).putString("backup-proof-marker","source-data").commit();repo.running=true;
        char[] key="Android-neutral-backup-2026!".toCharArray();SharedPreferences p=BackupScheduler.prefs(c);p.edit().clear().putString("tree",tree.toString()).putString("owner","0123456789abcdef0123456789abcdef").putString("password",KeyVault.encrypt(new String(key).getBytes(java.nio.charset.StandardCharsets.UTF_8))).putInt("keep",2).commit();
        for(int i=0;i<3;i++)DailyBackup.run(c);assertEquals(2,files(r,tree));assertTrue(repo.running);assertTrue(repo.prefs.getBoolean("recording",false));String prior=p.getString("lastFile","");assertTrue(p.getLong("success",0)>0);
        r.call(control,"faultProof","no-rename",null);DailyBackup.run(c);assertEquals(2,files(r,tree));assertFalse(prior.equals(p.getString("lastFile","")));String verified=p.getString("lastFile","");long at=p.getLong("success",0);
        for(String fault:new String[]{"offline","read-failure"}){r.call(control,"faultProof",fault,null);boolean failed=false;try{DailyBackup.run(c);}catch(Exception expected){failed=true;}assertTrue(failed);assertEquals(verified,p.getString("lastFile",""));assertEquals(at,p.getLong("success",0));assertFalse(p.getString("error","").isEmpty());r.call(control,"faultProof","",null);assertEquals(2,files(r,tree));}
        try(InputStream in=r.openInputStream(Uri.parse(verified))){JSONObject manifest=MigrationBackup.prepare(c,in,key);assertEquals(2,manifest.getInt("version"));assertTrue(manifest.getBoolean("portableCredentials"));}assertEquals("source-data",repo.prefs.getString("backup-proof-marker",""));assertFalse(MigrationBackup.pending(c));MigrationBackup.discard(c);
        java.util.Arrays.fill(key,'\0');repo.running=false;repo.prefs.edit().putBoolean("recording",false).commit();p.edit().clear().commit();
        android.os.Bundle report=new android.os.Bundle();report.putString("stream","AUTARPIE_SAF_BACKUP_VERIFIED\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,report);
    }
}
