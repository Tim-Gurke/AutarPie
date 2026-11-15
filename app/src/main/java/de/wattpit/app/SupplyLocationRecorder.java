package de.wattpit.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import org.json.*;

/** Short fixes during an active recording; no sensors run on display phones. */
final class SupplyLocationRecorder implements LocationListener {
    private final Repository repo;
    private final Context context;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final LocationManager manager;
    private long attempted;
    private boolean waiting;
    private final Runnable timeout=()->{stop();status("Kein frischer Standort; Reisezeiten mit fehlendem GPS bleiben unbekannt.");};
    SupplyLocationRecorder(Repository repo){this.repo=repo;context=repo.applicationContext();manager=context.getSystemService(LocationManager.class);}
    static boolean permitted(Context context){return (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)&&(Build.VERSION.SDK_INT<29||context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED);}
    private void status(String value){if(!value.equals(repo.prefs.getString("supplies-location-status","")))repo.prefs.edit().putString("supplies-location-status",value).apply();}
    @SuppressLint("MissingPermission") void tick(){
        if(!repo.running||!SupplyLocations.automatic(repo)||AppRole.viewerOnly(context)){stop();return;}
        if(!permitted(context)){stop();status("GPS-Nachführung wartet: auf dem erfassenden Handy Standort → Immer zulassen aktivieren.");return;}
        long now=SystemClock.elapsedRealtime();if(waiting||now-attempted<15*60000L&&attempted!=0)return;attempted=now;
        if(manager==null){status("Kein Standortdienst verfügbar.");return;}
        try{
            Location best=null;for(String provider:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER})if(manager.isProviderEnabled(provider)){Location last=manager.getLastKnownLocation(provider);if(usable(last)&&last.getTime()>=repo.prefs.getLong("supplies-location-started",0)&&(best==null||last.getElapsedRealtimeNanos()>best.getElapsedRealtimeNanos()))best=last;}
            if(best!=null){onLocationChanged(best);return;}
            waiting=true;boolean requested=false;for(String provider:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER})if(manager.isProviderEnabled(provider)){manager.requestLocationUpdates(provider,1000,0,this,Looper.getMainLooper());requested=true;}
            if(requested){status("GPS-Nachführung sucht den aktuellen Wohnwagenstandort …");handler.postDelayed(timeout,25000);}else{stop();status("GPS-Nachführung wartet: Android-Standortdienst einschalten.");}
        }catch(SecurityException|IllegalArgumentException e){stop();status("GPS-Nachführung wartet auf Standortzugriff; die Energieaufzeichnung läuft weiter.");}
    }
    static boolean usable(Location location){if(location==null||!location.hasAccuracy()||location.getAccuracy()>2000||location.isFromMockProvider())return false;long age=SystemClock.elapsedRealtimeNanos()-location.getElapsedRealtimeNanos();return age>=0&&age<=5*60*1000000000L&&Double.isFinite(location.getLatitude())&&Double.isFinite(location.getLongitude());}
    @Override public void onLocationChanged(Location location){
        if(!usable(location)||location.getTime()<repo.prefs.getLong("supplies-location-started",0)||!repo.running||!SupplyLocations.automatic(repo))return;
        try{long age=(SystemClock.elapsedRealtimeNanos()-location.getElapsedRealtimeNanos())/1000000L;SupplyLocations.fix(repo,new JSONObject().put("latitude",location.getLatitude()).put("longitude",location.getLongitude()).put("name","GPS-Standort"),System.currentTimeMillis()-age);status("GPS-Nachführung aktiv · Standort des erfassenden Handys.");stop();SuppliesWeather.refresh(repo,false);}catch(Exception e){stop();status("Standort konnte noch nicht gespeichert werden.");}
    }
    @SuppressLint("MissingPermission") void stop(){handler.removeCallbacks(timeout);waiting=false;if(manager!=null)try{manager.removeUpdates(this);}catch(SecurityException ignored){}}
    @Override public void onStatusChanged(String provider,int status,Bundle extras){}
    @Override public void onProviderEnabled(String provider){}
    @Override public void onProviderDisabled(String provider){}
}
