package de.wattpit.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import de.wattpit.app.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;

/** Real blocked SQL plus extracted productive transport methods. No Android lifecycle or radio. */
public final class ArchiveIoProof {
    private static int checks;
    private static void require(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    private static final long EPOCH=1700000000000L;
    private static ArchiveCodec.Clock clock(){ArchiveCodec.Clock c=new ArchiveCodec.Clock();byte[] anchor=new byte[8];System.arraycopy(ArchiveCodec.le(EPOCH/1000,4),0,anchor,4,4);c.anchors(anchor);return c;}
    private static Throwable failure(Future<?> result)throws Exception{try{result.get(5,TimeUnit.SECONDS);throw new AssertionError("Expected failure");}catch(ExecutionException e){return e.getCause();}}
    private static ExecutorService caller(){return Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"proof-caller");t.setDaemon(true);return t;});}

    abstract static class Environment implements AutoCloseable {
        static UUID uuid(int n){return UUID.fromString(String.format(Locale.ROOT,"306b%04x-b081-4037-83dc-e59fcc3cdfd0",n));}
        static final UUID CONTROL=uuid(2),DATA=uuid(3),BULK=uuid(4);
        static final int START_NOT_STICKY=2;
        static final class Build {static final class VERSION {static int SDK_INT;}}
        static final class BluetoothStatusCodes {static final int SUCCESS=0;}
        static final class BluetoothProfile {static final int STATE_CONNECTED=2,STATE_DISCONNECTED=0;}
        static final class BluetoothGattDescriptor { }
        static class BluetoothGattCallback {
            public void onConnectionStateChange(BluetoothGatt g,int status,int state){}
            public void onServicesDiscovered(BluetoothGatt g,int status){}
            public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){}
            public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){}
            public void onMtuChanged(BluetoothGatt g,int mtu,int status){}
            public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){}
            public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){}
        }
        static final class BluetoothGattCharacteristic {
            static final int PROPERTY_WRITE_NO_RESPONSE=4,WRITE_TYPE_NO_RESPONSE=1,WRITE_TYPE_DEFAULT=2;
            final UUID id;byte[] value;BluetoothGattCharacteristic(UUID id){this.id=id;}
            UUID getUuid(){return id;}int getProperties(){return PROPERTY_WRITE_NO_RESPONSE;}
            void setWriteType(int type){}void setValue(byte[] bytes){value=bytes.clone();}byte[] getValue(){return value;}
        }
        final class Handler {
            final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"android-main-adapter");t.setDaemon(true);return t;});
            void post(Runnable r){executor.execute(r);}
            void later(Runnable r,long ms){executor.schedule(r,ms,TimeUnit.MILLISECONDS);}
        }
        final class BluetoothGatt {
            static final int GATT_SUCCESS=0;volatile boolean closed;
            boolean writeCharacteristic(BluetoothGattCharacteristic c){legacyWrites++;return submit(c,c.getValue());}
            int writeCharacteristic(BluetoothGattCharacteristic c,byte[] value,int type){modernWrites++;return submit(c,value)?0:1;}
            boolean submit(BluetoothGattCharacteristic c,byte[] value){
                if(closed){writesAfterClose++;return false;}
                if(peer.rejectNext){peer.rejectNext=false;return false;}
                writes++;int active=inFlight.incrementAndGet();maxInFlight.accumulateAndGet(active,Math::max);
                if(active>1)asynchronousFailure.compareAndSet(null,new AssertionError("Overlapping GATT operations"));
                byte[] copy=value.clone();int status=peer.failNext;peer.failNext=0;
                if(peer.silentNext){peer.silentNext=false;return true;}
                main.later(()->{inFlight.decrementAndGet();if(status==0&&!closed)peer.written(c,copy);writeComplete(this,c,status);},8);
                return true;
            }
            void disconnect(){closed=true;peer.alive=false;}void close(){closed=true;}
        }
        final class Peer implements AutoCloseable {
            final ScheduledExecutorService radio=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"victron-peer-adapter");t.setDaemon(true);return t;});
            final ArrayDeque<byte[]> pending=new ArrayDeque<>();
            int window=128,notifications,maximumWindow=128;long lastHeartbeat=SystemClock.elapsedRealtime();
            volatile boolean alive=true,unsolicited=true,watchdog=true,rejectNext,silentNext;
            volatile int failNext;
            Peer(){radio.scheduleWithFixedDelay(()->{try{tick();}catch(Throwable e){asynchronousFailure.compareAndSet(null,e);}},15,15,TimeUnit.MILLISECONDS);}
            synchronized void tick(){
                if(!alive)return;
                if(watchdog&&SystemClock.elapsedRealtime()-lastHeartbeat>=10000){alive=false;BluetoothGatt current=gatt;main.post(()->peerDisconnect(current,19));return;}
                flush();if(unsolicited)for(int n=0;n<16&&window>0;n++)packet(new byte[]{8,0,0x19,3,0,0x41,1});
            }
            synchronized void written(BluetoothGattCharacteristic c,byte[] bytes){
                if(c==control){window+=bytes[1]&255;maximumWindow=Math.max(maximumWindow,window);if(window>128)throw new AssertionError("Receive window exceeded");flush();}
                else{
                    main.post(()->notifyPacket(gatt,control,new byte[]{(byte)0xf9,1}));
                    if(Arrays.equals(bytes,ArchiveSession.keepAlive()))lastHeartbeat=SystemClock.elapsedRealtime();
                    else{
                        byte[] payload=new byte[14];payload[0]=0;System.arraycopy(ArchiveCodec.le(1260,4),0,payload,1,4);payload[5]=3;payload[6]=30;
                        for(int n=0;n<3;n++)System.arraycopy(ArchiveCodec.le(1340-10L*n,2),0,payload,8+n*2,2);
                        byte[] frame=new byte[20];frame[0]=8;frame[1]=3;frame[2]=0x19;frame[3]=(byte)0xec;frame[4]=0x5b;frame[5]=0x4e;System.arraycopy(payload,0,frame,6,14);
                        pending.add(Arrays.copyOfRange(frame,0,9));pending.add(Arrays.copyOfRange(frame,9,frame.length));flush();
                    }
                }
            }
            private void flush(){while(window>0&&!pending.isEmpty())packet(pending.removeFirst());}
            private void packet(byte[] bytes){
                if(window--<=0)throw new AssertionError("Peer ignored credits");
                BluetoothGatt current=gatt;BluetoothGattCharacteristic c=(notifications++%2==0)?data:bulk;
                main.post(()->notifyPacket(current,c,bytes));
            }
            public void close(){alive=false;radio.shutdownNow();}
        }
        final Repository repository;
        final Handler main=new Handler();
        final BlockingQueue<byte[]> received=new LinkedBlockingQueue<>(512);
        final ArrayDeque<ArchiveCodec.Message> messages=new ArrayDeque<>();
        ArchiveCodec.Stream stream=new ArchiveCodec.Stream();
        volatile ArchiveLink link=new ArchiveLink();
        final ArchiveIo io=new ArchiveIo(this::idleTick,this::ioFailure);
        volatile boolean connected=true,sessionReady=true,cancelled,userCancelled,started=true;
        volatile IOException receiveFailure;volatile int disconnectStatus,receiveQueuePeak,negotiatedMtu=77;
        volatile CompletableFuture<Integer> operation;volatile String operationType="",cancellationReason="";
        volatile long idlePackets,idleMessages,lastTransportTick,longestDatabaseCall,longestGattOperation,longestAndroidQueueDelay;
        final List<String> diagnostics=new ArrayList<>();long importStarted=SystemClock.elapsedRealtime();
        String kind="shunt";int changed,blocks,skipped,missing,unmapped;
        final BluetoothGattCharacteristic control=new BluetoothGattCharacteristic(CONTROL),data=new BluetoothGattCharacteristic(DATA),bulk=new BluetoothGattCharacteristic(BULK);
        volatile BluetoothGatt gatt=new BluetoothGatt();
        final AtomicReference<Throwable> asynchronousFailure=new AtomicReference<>();
        final AtomicInteger inFlight=new AtomicInteger(),maxInFlight=new AtomicInteger();
        volatile int writes,legacyWrites,modernWrites,writesAfterClose;
        final Peer peer=new Peer();
        Environment(Repository repository,int api){this.repository=repository;Build.VERSION.SDK_INT=api;link.control(new byte[]{(byte)0xf9,(byte)0xff});link.heartbeatSent(SystemClock.elapsedRealtime());}
        abstract void idleTick()throws Exception;abstract void ioFailure(Exception e);
        abstract void notifyPacket(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] bytes);
        abstract void writeComplete(BluetoothGatt g,BluetoothGattCharacteristic c,int status);
        abstract void peerDisconnect(BluetoothGatt g,int status);
        abstract byte[] request()throws Exception;abstract void closeTransport();abstract void maintenance()throws Exception;abstract void snapshot();
        abstract ArchiveSyncPlan.Availability availability(HistoryDb db,List<ArchiveCodec.Level> levels,ArchiveCodec.Clock c,long at)throws Exception;
        abstract int cancel();abstract int preempt();
        void stopSelf(){}
        public void close(){closeTransport();io.close();peer.close();main.executor.shutdownNow();}
    }

    private static final class Blocked implements AutoCloseable {
        final ExecutorService worker=caller();final SQLiteDatabase lock;final Future<ArchiveSyncPlan.Availability> result;
        final CountDownLatch queryEntered=new CountDownLatch(1);boolean released;
        Blocked(Environment e,HistoryDb history,Path database)throws Exception{
            SQLiteDatabase db=history.getWritableDatabase();db.execSQL("PRAGMA journal_mode=DELETE");db.execSQL("PRAGMA busy_timeout=12000");
            lock=SQLiteDatabase.openDatabase(database.toString(),null,0);lock.execSQL("BEGIN EXCLUSIVE");
            SQLiteDatabase.beforeQuery=sql->{if(sql.startsWith("SELECT ts,origin,period FROM readings"))queryEntered.countDown();};
            result=worker.submit(()->e.availability(history,Collections.singletonList(new ArchiveCodec.Level(41,30,1200)),clock(),EPOCH+2000000));
            require(queryEntered.await(2,TimeUnit.SECONDS),"the actual HistoryDb SELECT reaches real SQLite");
            Thread.sleep(150);require(!result.isDone(),"an exclusive SQLite lock blocks the real query before row callbacks");
        }
        void release(){if(!released){lock.execSQL("COMMIT");released=true;SQLiteDatabase.beforeQuery=null;}}
        public void close(){release();worker.shutdownNow();lock.close();}
    }
    private static void seed(HistoryDb history)throws Exception{
        for(int i=0;i<=40;i++){Reading r=new Reading();r.device="io-test";r.kind="shunt";r.origin="archive";r.timestamp=EPOCH+i*30000L;r.period=30;r.voltage=13.2;history.insert(r);}
    }
    private static long count(HistoryDb history)throws Exception{try(Cursor c=history.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM readings WHERE device='io-test'",null)){c.moveToFirst();return c.getLong(0);}}
    private static void resumedValues(Environment e,HistoryDb history)throws Exception{
        ArchiveCodec.Block b=new ArchiveCodec.Block(e.request(),ArchiveCodec.field(0xed8d,-1),0,3);
        require(b.time==1260&&b.period==30&&Arrays.equals(b.values,new Double[]{13.4,13.3,13.2}),"productive write/ACK/exchange decodes a split DATA/BULK block after blocked SQL");
        List<long[]> times=new ArrayList<>();List<Double> values=new ArrayList<>();for(int n=0;n<b.count;n++){times.add(new long[]{clock().millis(b.time-(long)n*b.period),b.period});values.add(b.values[n]);}
        for(int n=0;n<2;n++)history.archiveBlock("io-test","shunt","voltage",0xed8d,b.period,times,values,ArchiveSyncPlan.completed(b,clock(),EPOCH+2000000));
        require(count(history)==43,"resume preserves every old sample and deduplicates a repeated archive block");
        try(Cursor c=history.getReadableDatabase().rawQuery("SELECT voltage FROM readings WHERE device='io-test' AND ts="+(EPOCH+1260000),null)){c.moveToFirst();require(c.getDouble(0)==13.4,"the decoded measurement reaches the real SQLite history unchanged");}
    }
    private static void actor()throws Exception{
        AtomicInteger active=new AtomicInteger(),maximum=new AtomicInteger();AtomicReference<Exception> error=new AtomicReference<>();
        ArchiveIo io=new ArchiveIo(()->{},error::set);ExecutorService callers=caller();
        try{
            List<Future<Integer>> tasks=new ArrayList<>();for(int i=0;i<8;i++)tasks.add(callers.submit(()->io.call(()->{int n=active.incrementAndGet();maximum.accumulateAndGet(n,Math::max);try{Thread.sleep(5);return io.call(()->7);}finally{active.decrementAndGet();}})));
            for(Future<Integer> task:tasks)require(task.get(2,TimeUnit.SECONDS)==7,"nested requests complete on the same transport owner");
            require(maximum.get()==1&&error.get()==null,"concurrent callers never overlap their GATT tasks");
            CountDownLatch started=new CountDownLatch(1);Future<?> running=callers.submit(()->io.call(()->{started.countDown();new CountDownLatch(1).await();return null;}));
            require(started.await(1,TimeUnit.SECONDS),"a transport operation is really in flight before close");
            Future<?> queued=callers.submit(()->io.call(()->42));Thread.sleep(40);io.close();
            require(failure(running) instanceof InterruptedException,"closing the transport interrupts the active request");
            require(failure(queued) instanceof IOException,"closing the transport releases queued callers rather than stranding their Futures");
            try{io.call(()->42);throw new AssertionError("closed transport accepted work");}catch(IOException expected){require(true,"a closed transport rejects subsequent requests");}
        }finally{io.close();callers.shutdownNow();}
    }
    public static void main(String[] args)throws Exception{
        String phase=args[1];if(phase.equals("actor")){actor();System.out.println("RESULT "+checks+" actor checks");return;}
        Class.forName("org.sqlite.JDBC");Path directory=Path.of(args[0]);LanSyncProof.TestContext context=new LanSyncProof.TestContext(directory);
        Repository repo=LanSyncProof.repository(context);HistoryDb history=repo.db;seed(history);Path database=directory.resolve("wattpit-history.db");
        boolean legacy=phase.equals("legacy");int api=phase.equals("fixed30")?30:35;
        try(Environment e=legacy?new ArchiveLegacyHarness(repo,api):new ArchiveIoHarness(repo,api)){
            if(phase.startsWith("ack-")){
                e.peer.unsolicited=false;e.peer.watchdog=false;
                if(phase.equals("ack-timeout")){
                    e.peer.silentNext=true;ExecutorService calls=caller();
                    try{Future<?> pending=calls.submit(()->e.request());Throwable error;try{pending.get(23,TimeUnit.SECONDS);throw new AssertionError("Unacknowledged write succeeded");}catch(ExecutionException expected){error=expected.getCause();}
                        require(error instanceof IOException&&error.getMessage().contains("nicht bestätigt"),"a missing real GATT callback fails the write with its actual timeout reason");
                        int before=e.writes;e.writeComplete(e.gatt,e.data,0);try{e.request();throw new AssertionError("Late ACK reused");}catch(IOException expected){require(e.writes==before,"a late callback cannot acknowledge a following write after timeout");}
                    }finally{calls.shutdownNow();}
                }else{
                    e.peer.rejectNext=true;try{e.request();throw new AssertionError("Rejected Android write succeeded");}catch(Exception expected){require(e.writes==0,"a rejected Android write is not treated as an acknowledged request");}
                    e.peer.failNext=133;try{e.request();throw new AssertionError("GATT failure succeeded");}catch(IOException expected){require(expected.getMessage().contains("133"),"a failing real GATT callback retains status 133");}
                }
            }else{
                try(Blocked blocked=new Blocked(e,history,database)){
                    if(phase.equals("cancel")||phase.equals("preempt")||phase.equals("disconnect")){
                        if(phase.equals("cancel"))e.cancel();else if(phase.equals("preempt"))e.preempt();else{e.peer.alive=false;e.peerDisconnect(e.gatt,19);}
                        Thread.sleep(350);int before=e.writes;Thread.sleep(350);
                        require(e.writes==before,"transport writes stop while SQL is still blocked after "+phase);
                        require(!blocked.result.isDone(),"stopping Bluetooth does not pretend the blocked SQL query already finished");
                        blocked.release();Throwable error=failure(blocked.result);
                        require(error instanceof IOException,"the database worker reports the transport stop after the real SQL lock is released");
                        if(phase.equals("disconnect"))require(error.getMessage().contains("19")&&!e.userCancelled,"remote disconnect keeps status 19 and is not relabeled user cancellation");
                        else require(e.userCancelled==phase.equals("cancel"),"manual command preemption and explicit user cancellation remain distinct");
                        require(count(history)==41,"a stopped preflight retains all stored measurements");
                    }else{
                        Thread.sleep(4350);require(!blocked.result.isDone(),"SQL remains blocked for more than the peer's modeled watchdog interval");
                        if(legacy){
                            require(!e.connected&&e.disconnectStatus==19,"3.0.8 loses the modeled peer while its real SQL query is blocked");
                            require(e.link.heartbeats()==1&&e.link.consumed()==0&&e.receiveQueuePeak==128,"3.0.8 neither consumes the receive window nor renews the session during blocked SQL");
                            blocked.release();try{blocked.result.get(3,TimeUnit.SECONDS);}catch(ExecutionException expected){}
                        }else{
                            require(e.connected&&e.receiveFailure==null&&e.asynchronousFailure.get()==null,"independent transport survives the same real SQL block");
                            require(e.link.received()>512&&e.link.consumed()>512&&e.link.grants()>5,"productive callbacks consume and replenish repeated receive windows during blocked SQL");
                            require(e.link.heartbeats()>=5&&e.link.maxHeartbeatGap()<6000,"session renewals continue without database row callbacks");
                            require(e.receiveQueuePeak<=128&&e.peer.maximumWindow<=128,"combined DATA/BULK reception remains inside the advertised window");
                            blocked.release();ArchiveSyncPlan.Availability available=blocked.result.get(3,TimeUnit.SECONDS);
                            require(!ArchiveSyncPlan.needsData(Collections.singletonList(new ArchiveCodec.Level(41,30,1200)),clock(),available),"the resumed real HistoryDb query still recognizes its complete existing history");
                            resumedValues(e,history);
                            require(e.maxInFlight.get()==1&&e.writesAfterClose==0,"database and transport threads never overlap GATT operations");
                            require(api>=33?e.modernWrites>0&&e.legacyWrites==0:e.legacyWrites>0&&e.modernWrites==0,"productive writes exercise the Android "+api+" callback signature");
                            e.snapshot();String exported=repo.diagnosticText();
                            require(exported.contains("ms · Blockierte Bestandsprüfung")&&exported.contains("Sitzungserhalt")&&exported.contains("Bluetooth-Verarbeitung neben lokalen Abfragen"),"the actual diagnostic text exports SQL duration and independent transport progress");
                            System.out.println("METRICS "+new JSONObject().put("received",e.link.received()).put("consumed",e.link.consumed()).put("receiveQueuePeak",e.receiveQueuePeak).put("heartbeats",e.link.heartbeats()).put("maxHeartbeatGapMs",e.link.maxHeartbeatGap()).put("sqlMs",e.longestDatabaseCall).put("maxGattInFlight",e.maxInFlight.get()));
                        }
                    }
                }
            }
            require(e.asynchronousFailure.get()==null,"no asynchronous GATT adapter failure is hidden");
        }finally{history.close();SQLiteDatabase.beforeQuery=null;}
        System.out.println("RESULT "+checks+" "+phase+" checks");
    }
}
