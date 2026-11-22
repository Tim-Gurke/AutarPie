package de.wattpit.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.*;
import de.wattpit.app.core.VictronDecoder;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Foreground viewer-only scanner. No GATT, recording service, GPS fixes or BMS ownership. */
final class ViewerReadout {
    static final String ENABLED="viewer-passive-readout";
    private final Context context;private final Repository repo;private final LanSync lan;
    private final PassiveReadout values=new PassiveReadout();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService provisioning=Executors.newSingleThreadExecutor();
    private BluetoothLeScanner scanner;private volatile boolean scanning,foreground,closed;private boolean fetching;
    private volatile Object owner;private volatile String status="Direkter Liveempfang ausgeschaltet.";private long fetchedAt,attemptedAt;
    ViewerReadout(Context c,Repository repo,LanSync lan){context=c.getApplicationContext();this.repo=repo;this.lan=lan;}
    boolean enabled(){return repo.prefs.getBoolean(ENABLED,false);}
    void enabled(boolean enabled){repo.prefs.edit().putBoolean(ENABLED,enabled).apply();handler.post(()->active(foreground));}
    boolean permitted(){return Build.VERSION.SDK_INT>=31?context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED&&context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED:context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    void active(boolean active){
        handler.removeCallbacks(tick);foreground=active;
        if(!active||!enabled()||!lan.viewer()){stop();values.clear();owner=null;fetchedAt=attemptedAt=0;return;}
        tick.run();
    }
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(closed||!foreground||!enabled()||!lan.viewer()){stop();return;}
        Object current=lan.identity();String id=lan.state().optString("id");
        if(owner!=current){stop();values.clear();owner=current;fetchedAt=attemptedAt=0;}
        long now=SystemClock.elapsedRealtime();
        if(!fetching&&(attemptedAt==0||now-attemptedAt>=30000)&&(fetchedAt==0||now-fetchedAt>=300000))provision(current,id);
        if(!permitted()){stop();status="Direkter Liveempfang wartet auf die Bluetooth-Freigabe.";}
        else if(values.size()>0)start();
        else status="Empfangsschlüssel werden von der Zentrale geladen.";
        handler.postDelayed(this,5000);
    }};
    private void provision(Object identity,String id){
        if(!id.matches("[A-Za-z0-9_-]{22}"))return;fetching=true;attemptedAt=SystemClock.elapsedRealtime();
        provisioning.execute(()->{
            JSONArray credentials=null;boolean remote=false;
            try{credentials=restore(id);}catch(Exception ignored){}
            final JSONArray cached=credentials;
            if(cached!=null)handler.post(()->{if(!closed&&foreground&&enabled()&&lan.viewer()&&owner==identity&&identity==lan.identity())try{values.configure(cached);if(permitted())start();}catch(Exception ignored){}});
            try{
                JSONObject result=lan.queryInput("readoutCredentials",new JSONArray());
                if(!id.equals(result.getString("plant")))throw new IllegalArgumentException();
                credentials=result.getJSONArray("devices");remote=true;
            }catch(Exception ignored){/* Encrypted local credentials also support a temporarily offline central. */}
            final JSONArray received=credentials;final boolean refreshed=remote;
            handler.post(()->{
                fetching=false;if(closed||!foreground||!enabled()||!lan.viewer()||owner!=identity||identity!=lan.identity())return;
                try{
                    if(received==null){status="Empfangsschlüssel fehlen; Zentrale verbinden und gegebenenfalls aktualisieren.";return;}
                    values.configure(received);if(refreshed){save(id,received);fetchedAt=SystemClock.elapsedRealtime();}
                    if(values.size()==0)status="Keine unterstützten Victron-Geräte für direkten Empfang eingerichtet.";
                    else if(permitted())start();
                }catch(Exception ignored){values.clear();status="Empfangsschlüssel konnten nicht übernommen werden.";}
            });
        });
    }
    private String key(String id){return "viewer-readout-credentials."+id;}
    private JSONArray restore(String id)throws Exception{
        JSONArray saved=new JSONArray(repo.prefs.getString(key(id),"[]")),out=new JSONArray();
        for(int i=0;i<saved.length();i++){JSONObject d=saved.getJSONObject(i);byte[] bytes=KeyVault.decrypt(d.getString("secret"));try{out.put(new JSONObject().put("mac",d.getString("mac")).put("kind",d.getString("kind")).put("readoutKey",Base64.getEncoder().encodeToString(bytes)));}finally{Arrays.fill(bytes,(byte)0);}}
        return out.length()==0?null:out;
    }
    private void save(String id,JSONArray credentials)throws Exception{
        JSONArray out=new JSONArray();for(int i=0;i<credentials.length();i++){JSONObject d=credentials.getJSONObject(i);byte[] bytes=Base64.getDecoder().decode(d.getString("readoutKey"));try{out.put(new JSONObject().put("mac",VictronDecoder.normalizeMac(d.getString("mac"))).put("kind",d.getString("kind")).put("secret",KeyVault.encrypt(bytes)));}finally{Arrays.fill(bytes,(byte)0);}}
        if(!repo.prefs.edit().putString(key(id),out.toString()).commit())throw new java.io.IOException();
    }
    private final ScanCallback callback=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult result){receive(result);}
        @Override public void onBatchScanResults(List<ScanResult> results){for(ScanResult r:results)receive(r);}
        @Override public void onScanFailed(int error){scanning=false;status="Direkter Bluetooth-Empfang konnte nicht starten. Neuer Versuch folgt.";}
    };
    @SuppressLint("MissingPermission") private void receive(ScanResult result){
        if(!foreground||!enabled()||!lan.viewer()||owner!=lan.identity()||result.getScanRecord()==null)return;
        long age=(SystemClock.elapsedRealtimeNanos()-result.getTimestampNanos())/1000000;if(age<0||age>10000)return;
        long now=System.currentTimeMillis();values.receive(result.getDevice().getAddress(),result.getScanRecord().getManufacturerSpecificData(0x02e1),now-age,now);
    }
    @SuppressLint("MissingPermission") private void start(){
        if(scanning||!foreground||!permitted())return;
        try{
            BluetoothManager manager=context.getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
            if(adapter==null||!adapter.isEnabled()){status="Direkter Liveempfang wartet auf eingeschaltetes Bluetooth.";return;}
            if(Build.VERSION.SDK_INT<=30){LocationManager location=context.getSystemService(LocationManager.class);if(location!=null&&!location.isProviderEnabled(LocationManager.GPS_PROVIDER)&&!location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){status="Für Bluetooth-Empfang den Android-Standortdienst einschalten. Der Anlagenstandort bleibt der Zentrale zugeordnet.";return;}}
            scanner=adapter.getBluetoothLeScanner();if(scanner==null)return;
            ScanFilter filter=new ScanFilter.Builder().setManufacturerData(0x02e1,new byte[]{0x10},new byte[]{(byte)0xff}).build();
            scanner.startScan(Collections.singletonList(filter),new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),callback);scanning=true;status="Direkter Liveempfang aktiv · außerhalb der Reichweite Werte von der Zentrale.";
        }catch(SecurityException e){status="Direkter Liveempfang wartet auf die Bluetooth-Freigabe.";}
        catch(IllegalStateException e){status="Bluetooth ist noch nicht bereit.";}
    }
    @SuppressLint("MissingPermission") private void stop(){if(scanner!=null&&scanning)try{scanner.stopScan(callback);}catch(Exception ignored){}scanning=false;scanner=null;}
    JSONObject overlay(JSONObject snapshot){
        try{
            boolean allowed=foreground&&enabled()&&lan.viewer()&&owner==lan.identity();int direct=allowed?values.overlay(snapshot,System.currentTimeMillis()):0;
            snapshot.put("viewerReadout",new JSONObject().put("enabled",enabled()).put("scanning",allowed&&scanning).put("direct",direct).put("status",enabled()?status:"Direkter Liveempfang ausgeschaltet."));
        }catch(Exception ignored){}return snapshot;
    }
    void close(){closed=true;handler.removeCallbacks(tick);stop();values.clear();provisioning.shutdownNow();}
}
