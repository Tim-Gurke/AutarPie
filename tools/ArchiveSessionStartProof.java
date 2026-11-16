package de.wattpit.app;

import android.os.SystemClock;
import de.wattpit.app.core.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Full extracted connect/setup, with asynchronous Android adapters. No radio test.
 * Cold/warm behaviour and control-read bytes are documented by the capture author:
 * https://github.com/edilAraujo/victron-trends/blob/main/PROTOCOL.md#11-keeping-a-session-alive */
public final class ArchiveSessionStartProof {
    static int checks;
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    abstract static class Environment implements AutoCloseable {
        static UUID uuid(int n){return UUID.fromString(String.format(Locale.ROOT,"306b%04x-b081-4037-83dc-e59fcc3cdfd0",n));}
        static final UUID SERVICE=uuid(1),CONTROL=uuid(2),DATA=uuid(3),BULK=uuid(4),CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        static final class Build {static final class VERSION {static int SDK_INT;}}
        static final class BluetoothProfile {static final int STATE_CONNECTED=2,STATE_DISCONNECTED=0;}
        static final class BluetoothStatusCodes {static final int SUCCESS=0;}
        static class BluetoothGattCallback {
            public void onConnectionStateChange(BluetoothGatt g,int status,int state){}
            public void onServicesDiscovered(BluetoothGatt g,int status){}
            public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){}
            public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){}
            public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,int status){}
            public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value,int status){}
            public void onMtuChanged(BluetoothGatt g,int mtu,int status){}
            public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){}
            public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){}
        }
        static final class BluetoothGattDescriptor {static final byte[] ENABLE_NOTIFICATION_VALUE={1,0},ENABLE_INDICATION_VALUE={2,0};void setValue(byte[] value){}}
        static final class BluetoothGattCharacteristic {
            static final int PROPERTY_READ=2,PROPERTY_WRITE_NO_RESPONSE=4,PROPERTY_NOTIFY=16,WRITE_TYPE_NO_RESPONSE=1,WRITE_TYPE_DEFAULT=2;
            final UUID id;byte[] value;boolean readable=true;BluetoothGattCharacteristic(UUID id){this.id=id;}
            UUID getUuid(){return id;}int getProperties(){return PROPERTY_WRITE_NO_RESPONSE|PROPERTY_NOTIFY|(readable?PROPERTY_READ:0);}
            BluetoothGattDescriptor getDescriptor(UUID uuid){return new BluetoothGattDescriptor();}
            void setWriteType(int type){}void setValue(byte[] value){this.value=value.clone();}byte[] getValue(){return value;}
        }
        final class Handler {
            final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor();
            boolean post(Runnable r){executor.execute(r);return true;}
            void later(Runnable r){executor.schedule(r,8,TimeUnit.MILLISECONDS);}
        }
        final class BluetoothGattService {
            BluetoothGattCharacteristic getCharacteristic(UUID id){return id.equals(CONTROL)?serverControl:id.equals(DATA)?serverData:id.equals(BULK)?serverBulk:null;}
        }
        final class BluetoothGatt {
            static final int GATT_SUCCESS=0;Handler handler;BluetoothGattCallback callback;boolean closed;
            boolean discoverServices(){handler.later(()->callback.onServicesDiscovered(this,0));return true;}
            boolean requestMtu(int mtu){handler.later(()->callback.onMtuChanged(this,77,0));return true;}
            BluetoothGattService getService(UUID id){return id.equals(SERVICE)?new BluetoothGattService():null;}
            boolean setCharacteristicNotification(BluetoothGattCharacteristic c,boolean enabled){events.add("subscribe");return true;}
            boolean writeDescriptor(BluetoothGattDescriptor d){handler.later(()->callback.onDescriptorWrite(this,d,0));return true;}
            int writeDescriptor(BluetoothGattDescriptor d,byte[] value){return writeDescriptor(d)?0:1;}
            boolean readCharacteristic(BluetoothGattCharacteristic c){
                events.add("read-control");reads++;readEntered.countDown();
                if(!blockRead)handler.later(()->{initialized=true;byte[] value=emptyRead?new byte[0]:new byte[]{0,4,0,1,(byte)0xde,0x4a,0};c.setValue(value);if(Build.VERSION.SDK_INT>=33)callback.onCharacteristicRead(this,c,value,readStatus);else callback.onCharacteristicRead(this,c,readStatus);});
                return true;
            }
            boolean writeCharacteristic(BluetoothGattCharacteristic c){return submit(c,c.getValue());}
            int writeCharacteristic(BluetoothGattCharacteristic c,byte[] value,int type){return submit(c,value)?0:1;}
            boolean submit(BluetoothGattCharacteristic c,byte[] value){
                if(closed)throw new AssertionError("Write after close");byte[] copy=value.clone();writes++;
                events.add((c==serverControl?"control:":"data:")+HexFormat.of().formatHex(copy));
                handler.later(()->{notify(serverControl,new byte[]{(byte)0xf9,1});
                    if(c!=serverControl&&(!cold||initialized)){
                        if(Arrays.equals(copy,new byte[]{1}))notify(serverData,new byte[]{2,0,0,0,0,0,0,0,0});
                        else if(copy.length==2&&copy[0]==3)notify(serverData,new byte[]{7,0,3,0});
                    }
                    callback.onCharacteristicWrite(this,c,0);
                });return true;
            }
            void notify(BluetoothGattCharacteristic c,byte[] value){c.setValue(value);if(Build.VERSION.SDK_INT>=33)callback.onCharacteristicChanged(this,c,value);else callback.onCharacteristicChanged(this,c);}
            void disconnect(){closed=true;}void close(){closed=true;}
        }
        final class BluetoothDevice {
            static final int BOND_NONE=10,BOND_BONDING=11,BOND_BONDED=12,TRANSPORT_LE=2,PHY_LE_1M_MASK=1;
            int getBondState(){return BOND_BONDED;}boolean createBond(){throw new AssertionError("No new pairing needed");}
            BluetoothGatt connectGatt(Object context,boolean auto,BluetoothGattCallback callback,int transport,int phy,Handler handler){BluetoothGatt g=new BluetoothGatt();g.handler=handler;g.callback=callback;handler.later(()->callback.onConnectionStateChange(g,0,BluetoothProfile.STATE_CONNECTED));return g;}
        }
        final Handler main=new Handler(),bluetooth=new Handler();
        final BlockingQueue<byte[]> received=new LinkedBlockingQueue<>(512);
        final ArrayDeque<ArchiveCodec.Message> messages=new ArrayDeque<>();
        final List<String> events=new CopyOnWriteArrayList<>();
        final CountDownLatch readEntered=new CountDownLatch(1);
        final BluetoothGattCharacteristic serverControl=new BluetoothGattCharacteristic(CONTROL),serverData=new BluetoothGattCharacteristic(DATA),serverBulk=new BluetoothGattCharacteristic(BULK);
        final ArchiveIo io=new ArchiveIo(()->{},e->{throw new AssertionError(e);});
        ArchiveCodec.Stream stream=new ArchiveCodec.Stream();volatile ArchiveLink link=new ArchiveLink();
        volatile boolean connected,cancelled,sessionReady;boolean automatic,cold,initialized,emptyRead,blockRead;
        volatile IOException receiveFailure;volatile BluetoothGatt gatt;BluetoothGattCharacteristic control,data;
        volatile CompletableFuture<Integer> operation;volatile String operationType="",cancellationReason="";
        volatile byte[] controlResponse;volatile boolean lastOperationSubmitted,lastOperationCallback;volatile int disconnectStatus,negotiatedMtu=23,receiveQueuePeak;
        volatile long longestAndroidQueueDelay,longestGattOperation;static volatile long lastGattClosedAt=-1;
        int connectionAttempts,readStatus,reads,writes;
        Environment(int api,boolean cold){Build.VERSION.SDK_INT=api;this.cold=cold;lastGattClosedAt=-1;}
        void stage(String text){events.add("stage:"+text);}void trace(String text){events.add("trace:"+text);}
        abstract void start()throws Exception;abstract void abort();
        public void close(){abort();io.close();main.executor.shutdownNow();bluetooth.executor.shutdownNow();}
    }
    static Environment create(boolean baseline,int api,boolean cold){return baseline?new ArchiveSessionStartBaseline(api,cold):new ArchiveSessionStartCurrent(api,cold);}
    static void connected(boolean baseline,int api,boolean cold)throws Exception{
        try(Environment e=create(baseline,api,cold)){
            e.start();check(e.sessionReady&&e.connected,"full production session setup completes: baseline="+baseline+", API="+api+", cold="+cold);
            List<String> commands=e.events.stream().filter(x->x.startsWith("data:")).toList();
            check(commands.equals(List.of("data:01","data:0300","data:0600821893421027","data:0301","data:0303")),"observed handshake bytes/order are retained");
            if(!baseline){int read=e.events.indexOf("read-control"),subscribe=e.events.indexOf("subscribe"),write=e.events.indexOf("control:fa80ff");check(e.reads==1&&read>=0&&read<subscribe&&read<write,"transport control is read before subscriptions and handshake");}
        }
    }
    public static void main(String[] args)throws Exception{
        connected(true,30,false);
        try(Environment e=create(true,30,true)){boolean timedOut=false;try{e.start();}catch(TimeoutException expected){timedOut=true;}check(timedOut&&!e.sessionReady&&e.reads==0,"released source omits cold-device initialization and cannot complete this captured-protocol model");}
        if(args.length>0&&args[0].equals("baseline-only")){System.out.println("RESULT "+checks+" historical baseline checks");return;}
        connected(false,30,true);connected(false,35,true);connected(false,30,false);
        try(Environment e=create(false,30,false)){e.serverControl.readable=false;e.start();check(e.sessionReady&&e.reads==0,"devices without a readable control characteristic retain their previous warm handshake");}
        try(Environment e=create(false,30,true)){e.readStatus=133;boolean rejected=false;try{e.start();}catch(IOException expected){rejected=expected.getMessage().contains("133");}check(rejected&&e.writes==0,"failed control read retains status and sends no handshake");}
        try(Environment e=create(false,35,true)){e.emptyRead=true;boolean rejected=false;try{e.start();}catch(IOException expected){rejected=expected.getMessage().contains("ohne Antwort");}check(rejected&&e.writes==0,"empty control response cannot start a session");}
        try(Environment e=create(false,30,true)){
            e.blockRead=true;ExecutorService caller=Executors.newSingleThreadExecutor();try{
                Future<?> f=caller.submit(()->{try{e.start();}catch(Exception ex){throw new CompletionException(ex);}});check(e.readEntered.await(2,TimeUnit.SECONDS),"control read actually becomes pending");e.abort();boolean stopped=false;try{f.get(2,TimeUnit.SECONDS);}catch(ExecutionException expected){stopped=true;}check(stopped&&e.writes==0,"closing pending initialization releases its caller without sending commands");check(e.lastOperationSubmitted&&!e.lastOperationCallback,"an accepted but interrupted Android read is not reported as an acknowledgement");
            }finally{caller.shutdownNow();}
        }
        System.out.println("RESULT "+checks+" transport initialization checks; Android/GATT adapters, no physical-device claim");
    }
}
