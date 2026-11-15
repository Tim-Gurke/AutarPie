package de.wattpit.app.core;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** One owner for GATT operations and idle receive/watchdog work, independent of SQL. */
public final class ArchiveIo implements AutoCloseable {
    @FunctionalInterface public interface Tick { void run() throws Exception; }
    private final ScheduledExecutorService executor;
    private volatile Thread owner;
    private volatile boolean closed;
    public ArchiveIo(Tick idle,Consumer<Exception> failure){
        executor=Executors.newSingleThreadScheduledExecutor(action->{Thread t=new Thread(action,"AutarPie-Archiv-Bluetooth");t.setDaemon(true);owner=t;return t;});
        executor.scheduleWithFixedDelay(()->{if(closed)return;try{idle.run();}catch(Exception e){if(!closed)failure.accept(e);}},250,250,TimeUnit.MILLISECONDS);
    }
    public boolean isWorkerThread(){return Thread.currentThread()==owner;}
    public <T>T call(Callable<T> action)throws Exception{
        if(closed)throw new IOException("Archivverbindung ist beendet");
        if(isWorkerThread())return action.call();
        Future<T> future;
        try{future=executor.submit(action);}catch(RejectedExecutionException e){throw new IOException("Archivverbindung ist beendet",e);}
        try{return future.get();}
        catch(InterruptedException e){future.cancel(true);Thread.currentThread().interrupt();throw e;}
        catch(CancellationException e){throw new IOException("Archivverbindung ist beendet",e);}
        catch(ExecutionException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw new IOException("Archivkommunikation fehlgeschlagen",e.getCause());}
    }
    @Override public void close(){closed=true;for(Runnable queued:executor.shutdownNow())if(queued instanceof Future<?>)((Future<?>)queued).cancel(false);}
}
