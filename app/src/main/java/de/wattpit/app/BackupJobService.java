package de.wattpit.app;
import android.app.job.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class BackupJobService extends JobService {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private Future<?> task;private final AtomicInteger generation=new AtomicInteger();
    @Override public boolean onStartJob(JobParameters params){int ticket=generation.incrementAndGet();task=worker.submit(()->{boolean ok=false;try{DailyBackup.run(this);ok=true;}catch(Exception ignored){}if(generation.get()==ticket){jobFinished(params,false);BackupScheduler.finished(this,ok);}});return true;}
    @Override public boolean onStopJob(JobParameters params){generation.incrementAndGet();if(task!=null)task.cancel(true);return BackupScheduler.prefs(this).getBoolean("enabled",false)||params.getExtras().getBoolean("manual");}
    @Override public void onDestroy(){generation.incrementAndGet();worker.shutdownNow();super.onDestroy();}
}
