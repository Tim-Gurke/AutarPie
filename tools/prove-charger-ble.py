#!/usr/bin/env python3
"""Exercise production ChargerBle with an asynchronous, MTU-limited Android BLE peer.

Only synthetic addresses, identities and measurements are used. This checks the
Android callback orchestration, not a physical radio or the real Android stack.
"""
import argparse
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--android-jar', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--source', type=Path, help='Alternative production ChargerBle.java, for the regression baseline')
parser.add_argument('--legacy', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
output = args.output.resolve()
generated = output / 'generated'
classes = output / 'classes'
generated.mkdir(parents=True, exist_ok=True)
classes.mkdir(parents=True, exist_ok=True)

files = {
'android/content/Context.java': '''package android.content;
public class Context {
 public Context getApplicationContext(){return this;}
 public <T> T getSystemService(Class<T> type){return type.cast(new android.bluetooth.BluetoothManager());}
}
''',
'android/os/Looper.java': '''package android.os;
public final class Looper {public static Looper getMainLooper(){return new Looper();}}
''',
'android/os/Build.java': '''package android.os;
public final class Build {public static final class VERSION {public static int SDK_INT=26;}}
''',
'android/os/Handler.java': '''package android.os;
import java.util.*;
public final class Handler {
 public static final Queue<Runnable> immediate=new ArrayDeque<>();
 public static final List<Runnable> delayed=new ArrayList<>();
 public Handler(Looper looper){}
 public boolean post(Runnable task){immediate.add(task);return true;}
 public boolean postDelayed(Runnable task,long millis){delayed.add(task);return true;}
 public void removeCallbacks(Runnable task){immediate.removeIf(r->r==task);delayed.removeIf(r->r==task);}
 public static void clear(){immediate.clear();delayed.clear();}
 public static void drain(){for(int n=0;!immediate.isEmpty();n++){if(n>200)throw new AssertionError("Unbounded callbacks");immediate.remove().run();}}
}
''',
'android/bluetooth/BluetoothProfile.java': '''package android.bluetooth;
public interface BluetoothProfile {int STATE_CONNECTED=2,STATE_DISCONNECTED=0;}
''',
'android/bluetooth/BluetoothStatusCodes.java': '''package android.bluetooth;
public final class BluetoothStatusCodes {public static final int SUCCESS=0;}
''',
'android/bluetooth/BluetoothGattCallback.java': '''package android.bluetooth;
public abstract class BluetoothGattCallback {
 public void onConnectionStateChange(BluetoothGatt g,int status,int state){}
 public void onMtuChanged(BluetoothGatt g,int mtu,int status){}
 public void onServicesDiscovered(BluetoothGatt g,int status){}
 public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){}
 public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){}
 public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){}
 public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){}
}
''',
'android/bluetooth/BluetoothGattDescriptor.java': '''package android.bluetooth;
public final class BluetoothGattDescriptor {
 public static final byte[] ENABLE_NOTIFICATION_VALUE={1,0};
 public final BluetoothGattCharacteristic owner;
 public BluetoothGattDescriptor(BluetoothGattCharacteristic owner){this.owner=owner;}
 public boolean setValue(byte[] value){return true;}
}
''',
'android/bluetooth/BluetoothGattCharacteristic.java': '''package android.bluetooth;
import java.util.UUID;
public final class BluetoothGattCharacteristic {
 public static final int PROPERTY_WRITE_NO_RESPONSE=4,WRITE_TYPE_NO_RESPONSE=1,WRITE_TYPE_DEFAULT=2;
 private final UUID id;private byte[] value=new byte[0];
 public BluetoothGattCharacteristic(UUID id){this.id=id;}
 public UUID getUuid(){return id;}
 public int getProperties(){return 0x1a;}
 public BluetoothGattDescriptor getDescriptor(UUID id){return new BluetoothGattDescriptor(this);}
 public boolean setValue(byte[] value){this.value=value.clone();return true;}
 public byte[] getValue(){return value;}
 public void setWriteType(int type){}
}
''',
'android/bluetooth/BluetoothGattService.java': '''package android.bluetooth;
import java.util.*;
public final class BluetoothGattService {
 private final Map<UUID,BluetoothGattCharacteristic> values=new HashMap<>();
 public BluetoothGattCharacteristic getCharacteristic(UUID id){return values.computeIfAbsent(id,BluetoothGattCharacteristic::new);}
}
''',
'android/bluetooth/BluetoothManager.java': '''package android.bluetooth;
public final class BluetoothManager {public BluetoothAdapter getAdapter(){return new BluetoothAdapter();}}
''',
'android/bluetooth/BluetoothAdapter.java': '''package android.bluetooth;
public final class BluetoothAdapter {
 public boolean isEnabled(){return true;}
 public static boolean checkBluetoothAddress(String mac){return mac.matches("(?:[0-9A-F]{2}:){5}[0-9A-F]{2}");}
 public BluetoothDevice getRemoteDevice(String mac){return new BluetoothDevice();}
}
''',
'android/bluetooth/BluetoothDevice.java': '''package android.bluetooth;
import android.content.Context;import android.os.Handler;
public final class BluetoothDevice {
 public static final int TRANSPORT_LE=2,PHY_LE_1M_MASK=1;
 public BluetoothGatt connectGatt(Context context,boolean auto,BluetoothGattCallback cb,int transport,int phy,Handler handler){
  BluetoothGatt g=new BluetoothGatt(cb,handler);BluetoothGatt.last=g;
  handler.post(()->cb.onConnectionStateChange(g,0,BluetoothProfile.STATE_CONNECTED));return g;
 }
}
''',
'android/bluetooth/BluetoothGatt.java': '''package android.bluetooth;
import android.os.*;import java.util.*;
public final class BluetoothGatt {
 public static final int GATT_SUCCESS=0;
 public static BluetoothGatt last;
 public static int negotiated=247,mtuStatus=0,serviceStatus=0,cells=0;
 public static boolean requestAccepted=true,mtuCallback=true,missingService=false;
 public static final List<String> calls=new ArrayList<>();
 public final BluetoothGattCallback callback;private final Handler handler;
 private final BluetoothGattService service=new BluetoothGattService();private final Set<UUID> subscriptions=new HashSet<>();
 public int mtu=23;public boolean closed;
 public BluetoothGatt(BluetoothGattCallback callback,Handler handler){this.callback=callback;this.handler=handler;}
 public static UUID uuid(int id){return UUID.fromString(String.format(Locale.ROOT,"%08x-0000-1000-8000-00805f9b34fb",id));}
 public boolean requestMtu(int requested){calls.add("mtu:"+requested);if(!requestAccepted)return false;
  if(mtuCallback)handler.post(()->{if(mtuStatus==0)mtu=negotiated;callback.onMtuChanged(this,mtu,mtuStatus);});return true;}
 public boolean discoverServices(){calls.add("services");handler.post(()->callback.onServicesDiscovered(this,serviceStatus));return true;}
 public BluetoothGattService getService(UUID id){return !missingService&&id.equals(uuid(0xaf00))?service:null;}
 public boolean setCharacteristicNotification(BluetoothGattCharacteristic c,boolean enabled){subscriptions.add(c.getUuid());return true;}
 public int writeDescriptor(BluetoothGattDescriptor d,byte[] value){calls.add("subscribe");handler.post(()->callback.onDescriptorWrite(this,d,0));return 0;}
 public boolean writeDescriptor(BluetoothGattDescriptor d){return writeDescriptor(d,new byte[]{1,0})==0;}
 public boolean writeCharacteristic(BluetoothGattCharacteristic c){return writeCharacteristic(c,c.getValue(),2)==0;}
 public int writeCharacteristic(BluetoothGattCharacteristic c,byte[] value,int type){
  calls.add(c.getUuid().equals(uuid(0xaf02))?"auth:"+(value[0]&255):"poll");
  handler.post(()->callback.onCharacteristicWrite(this,c,0));
  if(c.getUuid().equals(uuid(0xaf02))){
   if(value[0]==0x18)notifyValue(c,new byte[]{0x19,0});
   else if((value[0]&255)==0xe0){byte[] info=new byte[13];info[0]=(byte)0xe1;notifyValue(c,info);}
  }else if(Arrays.equals(value,new byte[]{0x12,(byte)0xe4})){
   byte[] sample=new byte[23+4*cells];sample[0]=0x31;sample[1]=(byte)0xe5;sample[6]=(byte)(cells==0?0:1);sample[22]=(byte)cells;
   int voltage=cells*3700;sample[10]=(byte)voltage;sample[11]=(byte)(voltage>>8);
   for(int i=0;i<cells;i++){sample[23+4*i]=0x74;sample[24+4*i]=0x0e;}
   // A real ATT notification cannot carry more than the negotiated MTU minus its header.
   if(sample.length<=mtu-3)notifyValue(c,sample);
  }
  return 0;
 }
 private void notifyValue(BluetoothGattCharacteristic c,byte[] value){if(!subscriptions.contains(c.getUuid()))return;
  handler.post(()->{c.setValue(value);if(Build.VERSION.SDK_INT>=33)callback.onCharacteristicChanged(this,c,value.clone());else callback.onCharacteristicChanged(this,c);});}
 public void disconnect(){calls.add("disconnect");}
 public void close(){closed=true;}
}
''',
'de/wattpit/app/Repository.java': '''package de.wattpit.app;
import android.content.Context;import java.util.*;
final class Repository {
 static Repository instance=new Repository();boolean running=true;final Prefs prefs=new Prefs();
 static Repository get(Context context){return instance;}
 static final class Prefs {
  final Map<String,String> values=new HashMap<>();
  String getString(String key,String fallback){return values.getOrDefault(key,fallback);}
  Prefs edit(){return this;}Prefs putString(String key,String value){values.put(key,value);return this;}boolean commit(){return true;}
 }
}
''',
'de/wattpit/app/KeyVault.java': '''package de.wattpit.app;
final class KeyVault {
 static String encrypt(byte[] value){return java.util.Base64.getEncoder().encodeToString(value);}
 static byte[] decrypt(String value){return java.util.Base64.getDecoder().decode(value);}
}
''',
'de/wattpit/app/EnergyStore.java': '''package de.wattpit.app;
import org.json.JSONObject;
final class EnergyStore {
 static final String CHARGER="synthetic-charger";static String error="";static JSONObject sample;static int samples;
 static void error(boolean charger,String message){error=message;}
 static void charger(JSONObject value){sample=value;samples++;error="";}
}
''',
'de/wattpit/app/ChargerBleProof.java': '''package de.wattpit.app;
import android.bluetooth.*;import android.content.Context;import android.os.*;import java.lang.reflect.*;import java.util.*;import org.json.*;
public final class ChargerBleProof {
 static int checks;
 static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 static ChargerBle fresh(int api)throws Exception{
  Handler.clear();Repository.instance=new Repository();EnergyStore.error="";EnergyStore.samples=0;EnergyStore.sample=null;
  BluetoothGatt.calls.clear();BluetoothGatt.negotiated=247;BluetoothGatt.mtuStatus=0;BluetoothGatt.serviceStatus=0;BluetoothGatt.cells=0;
  BluetoothGatt.requestAccepted=true;BluetoothGatt.mtuCallback=true;BluetoothGatt.missingService=false;
  Build.VERSION.SDK_INT=api;Field singleton=ChargerBle.class.getDeclaredField("instance");singleton.setAccessible(true);singleton.set(null,null);
  return ChargerBle.get(new Context());
 }
 static void start()throws Exception{ChargerBle.configure(new Context(),new JSONObject().put("mac","00:00:00:00:00:04").put("enabled",true).put("role","model").put("auth","000102030405060708090a0b0c0d0e0f").toString());Handler.drain();}
 static void tick(ChargerBle ble)throws Exception{Method m=ChargerBle.class.getDeclaredMethod("tick");m.setAccessible(true);m.invoke(ble);Handler.drain();}
 static void field(ChargerBle ble,String name,long value)throws Exception{Field f=ChargerBle.class.getDeclaredField(name);f.setAccessible(true);f.setLong(ble,value);}
 static void success(int api,int cells,int mtu)throws Exception{
  ChargerBle ble=fresh(api);BluetoothGatt.cells=cells;BluetoothGatt.negotiated=mtu;start();
  check(!BluetoothGatt.last.closed,"Successful setup keeps the GATT link");
  check(BluetoothGatt.calls.get(0).equals("mtu:247"),"Negotiate before service discovery or writes");
  check(BluetoothGatt.calls.indexOf("services")<BluetoothGatt.calls.indexOf("auth:24"),"Discover before authenticating");
  check(BluetoothGatt.calls.stream().filter(s->s.equals("subscribe")).count()==2,"Subscribe to both characteristics");
  tick(ble);check(EnergyStore.samples==1,"The production callback receives one complete status");
  check(EnergyStore.sample.getInt("mode")== (cells==0?0:1),"Idle and charging modes both arrive");
  check(EnergyStore.sample.getDouble("current")==0,"Zero current is a valid live value");
  check(EnergyStore.sample.getJSONArray("cells").length()==cells,"Every transmitted cell arrives");
  check(EnergyStore.error.isEmpty(),"A valid sample clears an earlier error");
  BluetoothGatt.last.callback.onMtuChanged(BluetoothGatt.last,23,0);Handler.drain();
  check(!BluetoothGatt.last.closed,"Ignore late duplicate MTU callbacks after setup");
  ble.stop();Handler.drain();check(BluetoothGatt.last.closed,"Stopping closes the link");
 }
 public static void main(String[] args)throws Exception{
  if(args.length>0&&args[0].equals("legacy")){
   ChargerBle ble=fresh(26);start();tick(ble);
   check(BluetoothGatt.last.mtu==23,"Legacy link retains the default MTU");
   check(EnergyStore.samples==0,"Even the 23-byte idle status cannot fit a 20-byte value");
   field(ble,"lastReceive",System.currentTimeMillis()-11000);tick(ble);
   check(EnergyStore.error.equals("608PD antwortet nicht mehr."),"Legacy code reproduces the reported error");
   System.out.println("Legacy ChargerBle reproduces idle timeout: "+checks+" checks");return;
  }
  success(26,0,247);success(26,3,247);success(35,6,247);success(26,6,50);
  ChargerBle ble=fresh(26);BluetoothGatt.negotiated=23;start();
  check(BluetoothGatt.last.closed&&EnergyStore.error.contains("zu klein"),"Reject a confirmed undersized MTU explicitly");
  check(!BluetoothGatt.calls.contains("services")&&EnergyStore.samples==0,"Do not authenticate or show data on an undersized link");
  fresh(35);BluetoothGatt.mtuStatus=133;start();
  check(BluetoothGatt.last.closed&&EnergyStore.error.contains("133"),"Report MTU error status");
  check(!BluetoothGatt.calls.contains("services"),"An unsuccessful exchange cannot advance setup");
  fresh(26);BluetoothGatt.requestAccepted=false;start();
  check(BluetoothGatt.last.closed&&EnergyStore.error.contains("angefordert"),"A refused MTU request fails explicitly");
  ble=fresh(26);BluetoothGatt.mtuCallback=false;start();
  check(BluetoothGatt.calls.equals(List.of("mtu:247")),"Wait for the asynchronous MTU callback");
  field(ble,"lastConnect",System.currentTimeMillis()-21000);tick(ble);
  check(BluetoothGatt.last.closed&&EnergyStore.error.contains("Paketgröße")&&EnergyStore.error.contains("rechtzeitig"),"A lost callback times out with the correct stage");
  fresh(26);BluetoothGatt.serviceStatus=129;start();
  check(BluetoothGatt.last.closed&&EnergyStore.error.contains("Bluetooth-Status 129"),"Service discovery failure keeps the real status");
  check(!EnergyStore.error.contains("nicht gefunden"),"Failed discovery is distinct from a missing service");
  fresh(26);BluetoothGatt.missingService=true;start();
  check(BluetoothGatt.last.closed&&EnergyStore.error.contains("AF00"),"An actually missing service is named separately");
  ble=fresh(26);start();BluetoothGatt previous=BluetoothGatt.last;ble.stop();Handler.drain();
  field(ble,"lastConnect",0);ble.start();Handler.drain();
  check(BluetoothGatt.last!=previous&&Collections.frequency(BluetoothGatt.calls,"mtu:247")==2,"Every new connection renegotiates its own MTU");
  previous.callback.onMtuChanged(previous,23,133);previous.callback.onServicesDiscovered(previous,133);Handler.drain();
  check(!BluetoothGatt.last.closed,"Callbacks from a closed connection cannot fail its replacement");
  tick(ble);check(EnergyStore.samples==1,"A reconnected charger delivers fresh idle values");
  System.out.println("Charger BLE transport: "+checks+" checks passed");
 }
}
''',
}
sources = []
for name, content in files.items():
    path = generated / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    sources.append(str(path))
production = root / 'app/src/main/java'
source = args.source.resolve() if args.source else production / 'de/wattpit/app/ChargerBle.java'
sources += [str(source), *[str(production / ('de/wattpit/app/core/' + name + '.java')) for name in ('EnergyProtocol', 'ArchiveCodec', 'ArchiveTimeReference', 'VictronSettings')]]
subprocess.run(['java', 'com.sun.tools.javac.Main', '-source', '17', '-target', '17', '-cp', str(args.android_jar.resolve()), '-d', str(classes), *sources], check=True)
result = subprocess.run(['java', '-cp', str(classes) + ':' + str(args.android_jar.resolve()), 'de.wattpit.app.ChargerBleProof', *(['legacy'] if args.legacy else [])], text=True, capture_output=True)
print(result.stdout, end='')
if result.returncode:
    print(result.stderr)
    raise SystemExit(result.returncode)
(output / 'results.log').write_text(result.stdout)
(output / 'report.json').write_text(json.dumps({'production_adapter': str(source.relative_to(root)) if source.is_relative_to(root) else source.name, 'legacy_baseline': args.legacy, 'result': result.stdout.strip(), 'physical_bluetooth_test': False}, indent=2) + '\n')
