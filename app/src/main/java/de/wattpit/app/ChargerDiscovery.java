package de.wattpit.app;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.*;
import org.json.*;
import java.util.*;

/** A bounded scan runs on the collecting phone, including requests from a display. */
@SuppressLint("MissingPermission")
final class ChargerDiscovery {
    private static ChargerDiscovery instance;
    private static final Map<String,JSONObject> found=new LinkedHashMap<>();
    private static String message="Noch keine Suche gestartet";private static boolean active;
    private final Context context;private final Handler main=new Handler(Looper.getMainLooper());private BluetoothLeScanner scanner;
    private ChargerDiscovery(Context context){this.context=context.getApplicationContext();}
    static synchronized JSONObject snapshot()throws JSONException{return new JSONObject().put("active",active).put("message",message).put("devices",new JSONArray(found.values()));}
    static synchronized void search(Context context)throws Exception{
        if(!Repository.get(context).running)throw new IllegalArgumentException("Bitte zuerst die Aufzeichnung auf dem erfassenden Handy starten.");
        if(instance==null)instance=new ChargerDiscovery(context);instance.main.post(instance::begin);
    }
    private final ScanCallback callback=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult result){
            ScanRecord record=result.getScanRecord();if(record==null)return;String name=record.getDeviceName();byte[] maker=record.getManufacturerSpecificData(0xabba);
            boolean model=maker!=null&&maker.length>=6&&((maker[2]&255)==1||(maker[2]&255)==0x81)&&(maker[3]&255)==0xc0&&maker[4]==0&&maker[5]==0;
            if(!model&&(name==null||!name.toUpperCase(Locale.ROOT).replace(" ","").contains("608PD")))return;
            try{synchronized(ChargerDiscovery.class){String mac=result.getDevice().getAddress();if(found.size()<20||found.containsKey(mac))found.put(mac,new JSONObject().put("mac",mac).put("name",name==null||name.isEmpty()?"608PD":name.substring(0,Math.min(60,name.length()))).put("rssi",result.getRssi()));message=found.size()+" 608PD gefunden · gewünschtes Gerät auswählen";}}catch(Exception ignored){}
        }
        @Override public void onScanFailed(int error){synchronized(ChargerDiscovery.class){active=false;message="Bluetooth-Suche fehlgeschlagen ("+error+"). Berechtigungen prüfen und erneut suchen.";}}
    };
    private final Runnable finish=()->end();
    private void begin(){
        end();try{BluetoothManager manager=context.getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();if(adapter==null||!adapter.isEnabled())throw new IllegalStateException("Bitte Bluetooth einschalten.");scanner=adapter.getBluetoothLeScanner();if(scanner==null)throw new IllegalStateException("Bluetooth ist noch nicht bereit.");
            synchronized(ChargerDiscovery.class){found.clear();active=true;message="Suche am erfassenden Handy · 15 Sekunden";}
            scanner.startScan(Collections.emptyList(),new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),callback);main.postDelayed(finish,15000);
        }catch(Exception e){synchronized(ChargerDiscovery.class){active=false;message=e instanceof SecurityException?"Bluetooth-Berechtigung fehlt. Berechtigungen am erfassenden Handy prüfen.":e.getMessage();}}
    }
    private void end(){main.removeCallbacks(finish);if(scanner!=null)try{scanner.stopScan(callback);}catch(Exception ignored){}scanner=null;synchronized(ChargerDiscovery.class){if(active)message=found.isEmpty()?"Kein 608PD gefunden. Einschalten und in Reichweite des erfassenden Handys bringen.":found.size()+" 608PD gefunden · gewünschtes Gerät auswählen";active=false;}}
}
