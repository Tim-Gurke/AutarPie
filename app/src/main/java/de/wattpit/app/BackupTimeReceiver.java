package de.wattpit.app;
import android.content.*;
public final class BackupTimeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){android.os.UserManager u=c.getSystemService(android.os.UserManager.class);if(u!=null&&!u.isUserUnlocked())return;android.content.SharedPreferences p=BackupScheduler.prefs(c);if(p.getLong("due",0)>System.currentTimeMillis())p.edit().putLong("due",0).commit();BackupScheduler.reconcile(c);}
}
