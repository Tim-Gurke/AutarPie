package de.wattpit.app;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.*;
import android.os.*;
import de.wattpit.app.core.EnergyProtocol;
import org.json.*;
import java.util.*;

/** Dedicated 608PD link. The private handshake is provisioned locally, never shipped. */
@SuppressLint("MissingPermission")
final class ChargerBle {
    private static ChargerBle instance;
    static synchronized ChargerBle get(Context context){if(instance==null)instance=new ChargerBle(context.getApplicationContext());return instance;}
    private static final UUID SERVICE=uuid(0xaf00),DATA=uuid(0xaf01),AUTH=uuid(0xaf02),CCCD=uuid(0x2902);
    // A six-cell status needs 47 value bytes plus the three-byte ATT notification header.
    private static final int REQUESTED_MTU=247,MINIMUM_MTU=50;
    private enum Phase { CONNECTING, MTU, SERVICES, AUTH, READY }
    private static UUID uuid(int value){return UUID.fromString(String.format(Locale.ROOT,"%08x-0000-1000-8000-00805f9b34fb",value));}
    private final Context context;private final Repository repo;private final Handler main=new Handler(Looper.getMainLooper());
    private volatile BluetoothGatt gatt;private BluetoothGattCharacteristic data,auth;private volatile boolean enabled,ready,busy;private volatile long lastReceive,lastConnect,lastSampleAt;private volatile int lastMode=-1;private String expected="";private long expectedAt;
    private final ArrayDeque<Runnable> operations=new ArrayDeque<>();private Phase phase;
    private final boolean owner;private volatile String profileMac;
    private final Map<String,ChargerBle> peers=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ExecutorService journal=java.util.concurrent.Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"608pd-journal");thread.setDaemon(true);return thread;});
    private ChargerBle(Context context){this(context,"",true);}
    private ChargerBle(Context context,String mac,boolean owner){this.context=context;repo=Repository.get(context);this.profileMac=mac;this.owner=owner;}
    private void record(JSONObject value){String mac=profileMac;if(mac.isEmpty())return;try{JSONObject config=profile();value.put("device",mac).put("name",config.optString("name","608PD")).put("role",config.optString("role","model"));journal.execute(()->{try{ChargerJournal.record(repo,mac,value);}catch(Exception e){EnergyStore.chargerError(mac,"Laderdaten empfangen; Journal konnte nicht gespeichert werden.");}});}catch(JSONException ignored){}}

    static void configure(Context context,String json)throws Exception{ChargerProfiles.save(Repository.get(context),json);get(context).restart();}
    static JSONObject publicConfig(Repository repo)throws JSONException{return ChargerProfiles.publicValue(ChargerProfiles.selected(repo));}
    static void select(Context context,String mac)throws Exception{ChargerProfiles.select(Repository.get(context),mac);}
    static void pair(Context context,String json)throws Exception{if(!Repository.get(context).running)throw new IllegalArgumentException("Bitte zuerst die Aufzeichnung starten.");ChargerProfiles.beginPairing(Repository.get(context),json);get(context).restart();}
    static void remove(Context context,String mac)throws Exception{ChargerProfiles.remove(Repository.get(context),mac);get(context).restart();}
    private JSONObject profile()throws JSONException{return profileMac.isEmpty()?ChargerProfiles.selected(repo):ChargerProfiles.find(repo,profileMac);}
    private void preparePeers()throws JSONException{
        if(!owner)return;for(ChargerBle peer:peers.values())peer.stop();peers.clear();profileMac=ChargerProfiles.selected(repo).optString("mac");
        JSONArray profiles=ChargerProfiles.all(repo);for(int i=0;i<profiles.length();i++){JSONObject p=profiles.getJSONObject(i);String mac=p.optString("mac");if(p.optBoolean("enabled")&&!mac.equals(profileMac)){ChargerBle peer=new ChargerBle(context,mac,false);peers.put(mac,peer);peer.start();}}
    }
    private final BluetoothGattCallback callback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state){main.post(()->{
            if(g!=gatt)return;
            if(status==BluetoothGatt.GATT_SUCCESS&&state==BluetoothProfile.STATE_CONNECTED){
                if(phase!=Phase.CONNECTING)return;
                phase=Phase.MTU;
                if(!g.requestMtu(REQUESTED_MTU))fail("Die Bluetooth-Paketgröße für den 608PD konnte nicht angefordert werden.");
            }else if(state==BluetoothProfile.STATE_DISCONNECTED||status!=BluetoothGatt.GATT_SUCCESS)fail("608PD getrennt (Bluetooth-Status "+status+"); erneuter Versuch folgt.");
        });}
        @Override public void onMtuChanged(BluetoothGatt g,int mtu,int status){main.post(()->{
            if(g!=gatt||phase!=Phase.MTU)return;
            if(status!=BluetoothGatt.GATT_SUCCESS){fail("Die Bluetooth-Paketgröße für den 608PD konnte nicht vereinbart werden (Status "+status+").");return;}
            if(mtu<MINIMUM_MTU){fail("Die Bluetooth-Paketgröße für den 608PD ist zu klein ("+mtu+"; mindestens "+MINIMUM_MTU+" erforderlich).");return;}
            phase=Phase.SERVICES;
            if(!g.discoverServices())fail("Die Laderdienste konnten nicht angefordert werden.");
        });}
        @Override public void onServicesDiscovered(BluetoothGatt g,int status){main.post(()->{
            if(g!=gatt||phase!=Phase.SERVICES)return;
            if(status!=BluetoothGatt.GATT_SUCCESS){fail("Die 608PD-Dienste konnten nicht gelesen werden (Bluetooth-Status "+status+").");return;}
            BluetoothGattService service=g.getService(SERVICE);
            if(service==null){fail("Der erwartete 608PD-Dienst AF00 wurde nicht gefunden.");return;}
            data=service.getCharacteristic(DATA);auth=service.getCharacteristic(AUTH);if(data==null||auth==null){fail("608PD-Schnittstelle unvollständig.");return;}
            phase=Phase.AUTH;subscribe(data);subscribe(auth);enqueue(()->authenticate());
        });}
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){main.post(()->complete(g,status));}
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){main.post(()->complete(g,status));}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){main.post(()->{if(g==gatt)receive(c.getUuid(),value);});}
        @SuppressWarnings("deprecation") @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){if(Build.VERSION.SDK_INT<33){byte[] value=c.getValue();main.post(()->{if(g==gatt)receive(c.getUuid(),value);});}}
    };
    void start(){main.post(()->{try{if(enabled){tick();return;}preparePeers();enabled=true;tick();}catch(JSONException e){fail("Laderprofile konnten nicht gelesen werden.");}});}
    void stop(){main.post(()->{enabled=false;for(ChargerBle peer:peers.values())peer.stop();peers.clear();close();});}
    private void restart(){main.post(()->{close();try{preparePeers();if(repo.running){enabled=true;lastConnect=0;tick();}}catch(JSONException e){fail("Laderprofile konnten nicht gelesen werden.");}});}
    private final Runnable poll=()->tick();
    private void tick(){
        main.removeCallbacks(poll);if(!enabled||!repo.running)return;try{
            JSONObject c=profile();if(!c.optBoolean("enabled"))return;
            long now=System.currentTimeMillis();if(gatt==null&&now-lastConnect>=15000){lastConnect=now;BluetoothManager manager=context.getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();if(adapter==null||!adapter.isEnabled()){EnergyStore.chargerError(profileMac,"Bluetooth ist ausgeschaltet.");}else{phase=Phase.CONNECTING;gatt=adapter.getRemoteDevice(c.getString("mac")).connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE,BluetoothDevice.PHY_LE_1M_MASK,main);}}
            else if(gatt!=null&&!ready&&now-lastConnect>(phase==Phase.AUTH&&c.optBoolean("pairingPending")?45000:20000)){fail(phase==Phase.MTU?"Die Bluetooth-Paketgröße für den 608PD wurde nicht rechtzeitig bestätigt.":phase==Phase.SERVICES?"Die 608PD-Dienste wurden nicht rechtzeitig gelesen.":phase==Phase.CONNECTING?"Der Bluetooth-Verbindungsaufbau zum 608PD wurde nicht rechtzeitig bestätigt.":c.optBoolean("pairingPending")?"Neue Verbindung wurde am 608PD nicht rechtzeitig bestätigt. Erneut verbinden und am Lader bestätigen.":"608PD-Anmeldung wurde nicht rechtzeitig bestätigt.");}
            else if(ready){if(lastReceive>0&&now-lastReceive>10000){fail("608PD antwortet nicht mehr.");}else enqueue(()->write(data,EnergyProtocol.chargerPoll()));}
            if(!expected.isEmpty()&&now-expectedAt>8000){EnergyStore.chargerError(profileMac,"Der Lader hat den angeforderten Betriebszustand noch nicht bestätigt.");expected="";}
        }catch(Exception e){fail("608PD-Verbindung nicht möglich. Einrichtung und Berechtigungen prüfen.");}
        main.postDelayed(poll,1000);
    }
    private void enqueue(Runnable action){if(operations.size()>8)return;operations.add(action);advance();}
    private void advance(){if(busy||gatt==null)return;Runnable action=operations.poll();if(action!=null)try{action.run();}catch(SecurityException|IllegalStateException e){fail("Bluetooth-Auftrag nicht möglich. Berechtigungen am erfassenden Handy prüfen.");}}
    private void complete(BluetoothGatt g,int status){if(g!=gatt)return;busy=false;if(status!=0){fail("Bluetooth-Auftrag vom 608PD abgelehnt.");return;}advance();}
    @SuppressWarnings("deprecation") private void subscribe(BluetoothGattCharacteristic c){enqueue(()->{BluetoothGattDescriptor d=c.getDescriptor(CCCD);if(d==null||!gatt.setCharacteristicNotification(c,true)){fail("608PD-Benachrichtigungen fehlen.");return;}busy=true;byte[] v=BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;boolean accepted;if(Build.VERSION.SDK_INT>=33)accepted=gatt.writeDescriptor(d,v)==BluetoothStatusCodes.SUCCESS;else{d.setValue(v);accepted=gatt.writeDescriptor(d);}if(!accepted)fail("608PD-Benachrichtigungen konnten nicht aktiviert werden.");});}
    @SuppressWarnings("deprecation") private void write(BluetoothGattCharacteristic c,byte[] value){if(gatt==null||c==null)return;busy=true;int type=(c.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)!=0?BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE:BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;boolean accepted;if(Build.VERSION.SDK_INT>=33)accepted=gatt.writeCharacteristic(c,value,type)==BluetoothStatusCodes.SUCCESS;else{c.setWriteType(type);c.setValue(value);accepted=gatt.writeCharacteristic(c);}if(!accepted)fail("Bluetooth-Schreibauftrag nicht angenommen.");}
    private void authenticate(){try{JSONObject c=profile();byte[] key=KeyVault.decrypt(c.getString("secret"));if(key.length!=16)throw new IllegalArgumentException();byte[] packet=new byte[19];packet[0]=0x18;System.arraycopy(key,0,packet,1,16);if(c.optBoolean("pairingPending"))packet[18]=1;write(auth,packet);Arrays.fill(key,(byte)0);}catch(Exception e){fail("Private 608PD-Verbindung erneut aus eigener Aufzeichnung übernehmen.");}}
    private void receive(UUID characteristic,byte[] value){try{
        if(AUTH.equals(characteristic)){if(value.length==2&&(value[0]&255)==0x19){if(value[1]!=0){if(value[1]==1&&profile().optBoolean("pairingPending")){EnergyStore.chargerError(profileMac,"Neue Verbindung jetzt mit einer Taste am 608PD bestätigen.");return;}fail("608PD hat die private Verbindung abgelehnt.");return;}enqueue(()->write(auth,new byte[]{(byte)0xe0}));}
            else if(value.length>=2&&(value[0]&255)==0xe1){ready=true;phase=Phase.READY;lastReceive=System.currentTimeMillis();}return;}
        if(!ready)return;EnergyProtocol.ChargerSample sample=EnergyProtocol.charger(value);if(sample==null)return;long now=System.currentTimeMillis();lastReceive=now;lastSampleAt=now;lastMode=sample.mode;JSONArray cells=new JSONArray();for(double v:sample.cellVoltages)cells.put(v);
        JSONObject config=profile(),point=new JSONObject().put("at",now).put("device",profileMac).put("name",config.optString("name","608PD")).put("role",config.optString("role","model")).put("mode",sample.mode).put("current",sample.current).put("voltage",sample.voltage).put("elapsed",sample.elapsed).put("capacity",sample.capacity).put("cells",cells);
        EnergyStore.charger(point);record(new JSONObject(point.toString()).put("type","sample"));
        if(config.optBoolean("pairingPending"))ChargerProfiles.confirmed(repo,profileMac);
        if(expected.equals("stop")&&sample.mode==0||expected.equals("start")&&sample.mode==1)expected="";
    }catch(Exception e){EnergyStore.chargerError(profileMac,"608PD-Daten konnten nicht gelesen werden.");}}
    void command(String action)throws Exception{commandFor(context,ChargerProfiles.selected(repo).optString("mac"),action);}
    static void commandFor(Context context,String mac,String action)throws Exception{
        ChargerBle manager=get(context),target=mac.equals(manager.profileMac)?manager:manager.peers.get(mac);
        if(target==null)throw new IllegalArgumentException("Den gewählten 608PD zuerst verbinden und aktuelle Werte abwarten.");target.commandHere(action);
    }
    private void commandHere(String action)throws Exception{
        if(!action.equals("stop")&&!action.equals("start"))throw new IllegalArgumentException("Die Powerbank-Funktion bitte am 608PD starten.");
        if(!repo.running||!ready||lastSampleAt==0||System.currentTimeMillis()<lastSampleAt||System.currentTimeMillis()-lastSampleAt>10000)throw new IllegalArgumentException("Zuerst den 608PD verbinden und aktuelle Werte abwarten.");
        if(action.equals("start")){JSONObject c=profile();if(!c.optString("role").equals("model"))throw new IllegalArgumentException("Wohnwagenladung bitte am 608PD mit dem passenden Batterieprofil starten.");}
        if(action.equals("start")&&lastMode!=0||action.equals("stop")&&lastMode==0)throw new IllegalArgumentException("Der Lader ist bereits in einem anderen Zustand. Aktuelle Werte prüfen.");
        final BluetoothGatt commandGatt=gatt;final String commandMac=profileMac;
        main.post(()->{try{record(new JSONObject().put("at",System.currentTimeMillis()).put("type","command").put("action",action));}catch(JSONException ignored){}enqueue(()->{long now=System.currentTimeMillis();if(gatt!=commandGatt||!profileMac.equals(commandMac)||!ready||lastSampleAt==0||now<lastSampleAt||now-lastSampleAt>10000||action.equals("start")&&lastMode!=0||action.equals("stop")&&lastMode==0){EnergyStore.chargerError(commandMac,"Laderverbindung oder Betriebszustand wurde geändert; Auftrag nicht gesendet.");advance();return;}expected=action;expectedAt=now;write(data,action.equals("stop")?EnergyProtocol.chargerStop():EnergyProtocol.chargerStart(3,4210,500));});});
    }
    private void fail(String text){EnergyStore.chargerError(profileMac,text);try{ChargerProfiles.pairingFailed(repo,profileMac);}catch(Exception ignored){}close();}
    private void close(){if(gatt!=null)try{record(new JSONObject().put("at",System.currentTimeMillis()).put("type","gap"));}catch(JSONException ignored){}main.removeCallbacks(poll);lastSampleAt=0;ready=false;busy=false;phase=null;operations.clear();BluetoothGatt old=gatt;gatt=null;data=null;auth=null;if(old!=null){try{old.disconnect();old.close();}catch(Exception ignored){}}if(enabled&&repo.running)main.postDelayed(poll,15000);}
}
