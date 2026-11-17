package de.wattpit.app;
import android.app.job.*;
import android.content.*;
import java.time.*;

final class BackupScheduler {
    static final int JOB=60913;
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("autarpie-backups",0);}
    static long next(long now,String time,String zone){LocalTime at=LocalTime.parse(time);ZoneId z=ZoneId.of(zone);ZonedDateTime current=Instant.ofEpochMilli(now).atZone(z),slot=current.toLocalDate().atTime(at).atZone(z);if(!slot.toInstant().isAfter(current.toInstant()))slot=current.toLocalDate().plusDays(1).atTime(at).atZone(z);return slot.toInstant().toEpochMilli();}
    static long nextTime(long now,int hour,int minute){return next(now,String.format(java.util.Locale.ROOT,"%02d:%02d",hour,minute),ZoneId.systemDefault().getId());}
    private static void schedule(Context c,long delay,boolean manual){JobScheduler jobs=c.getSystemService(JobScheduler.class);if(jobs==null)throw new IllegalStateException("Zeitplanung nicht verfügbar.");android.os.PersistableBundle extras=new android.os.PersistableBundle();extras.putBoolean("manual",manual);JobInfo job=new JobInfo.Builder(JOB,new ComponentName(c,BackupJobService.class)).setPersisted(true).setMinimumLatency(Math.max(1,delay)).setBackoffCriteria(15*60000,JobInfo.BACKOFF_POLICY_EXPONENTIAL).setExtras(extras).build();if(jobs.schedule(job)!=JobScheduler.RESULT_SUCCESS)throw new IllegalStateException("Zeitplanung nicht verfügbar.");}
    static synchronized void reconcile(Context c){SharedPreferences p=prefs(c);JobScheduler jobs=c.getSystemService(JobScheduler.class);if(jobs==null)return;if(!p.getBoolean("enabled",false)){JobInfo pending=jobs.getPendingJob(JOB);if(pending==null||!pending.getExtras().getBoolean("manual"))jobs.cancel(JOB);return;}if(DailyBackup.running())return;
        try{long now=System.currentTimeMillis(),due=p.getLong("due",0);String zone=ZoneId.systemDefault().getId();if(due<=0||due>now&&!zone.equals(p.getString("zone",""))){due=nextTime(now,p.getInt("hour",3),p.getInt("minute",0));p.edit().putLong("due",due).commit();}p.edit().putString("zone",zone).commit();JobInfo pending=jobs.getPendingJob(JOB);if(pending!=null&&pending.getExtras().getBoolean("manual"))return;schedule(c,Math.max(1,Math.max(due,p.getLong("retry",0))-now),false);}catch(Exception e){p.edit().putString("error","Sicherungszeit prüfen und erneut speichern.").apply();}}
    static synchronized void now(Context c){schedule(c,1,true);}
    static void finished(Context c,boolean success){SharedPreferences p=prefs(c);long now=System.currentTimeMillis(),due=nextTime(now,p.getInt("hour",3),p.getInt("minute",0));SharedPreferences.Editor e=p.edit();if(success)e.putLong("due",due).remove("retry");else e.putLong("retry",now+15*60000);e.commit();if(p.getBoolean("enabled",false))schedule(c,success?Math.max(1,due-now):15*60000,false);}
    private BackupScheduler(){}
}
