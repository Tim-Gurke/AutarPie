package de.wattpit.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/** Android 10/11 require a separate grant for BLE discovery after boot. */
final class BackgroundBleAccess {
    static boolean granted(Context context){return Build.VERSION.SDK_INT<29||Build.VERSION.SDK_INT>30||context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    static void openSettings(Activity activity){activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName())));}
    static org.json.JSONObject diagnostic(Context context)throws org.json.JSONException{
        SharedPreferences prefs=context.getSharedPreferences("wattpit",0);
        android.os.UserManager user=context.getSystemService(android.os.UserManager.class);
        android.os.PowerManager power=context.getSystemService(android.os.PowerManager.class);
        android.location.LocationManager location=context.getSystemService(android.location.LocationManager.class);
        Object locationEnabled=org.json.JSONObject.NULL;
        try{if(location!=null)locationEnabled=Build.VERSION.SDK_INT>=28?location.isLocationEnabled():location.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)||location.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);}catch(SecurityException|IllegalArgumentException ignored){}
        return new org.json.JSONObject().put("androidSdk",Build.VERSION.SDK_INT)
                .put("fineLocation",context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                .put("backgroundBleAllowed",granted(context)).put("locationEnabled",locationEnabled)
                .put("bluetoothScan",Build.VERSION.SDK_INT<31||context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED)
                .put("bluetoothConnect",Build.VERSION.SDK_INT<31||context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED)
                .put("userUnlocked",user==null||user.isUserUnlocked())
                .put("batteryOptimizationExempt",power!=null&&power.isIgnoringBatteryOptimizations(context.getPackageName()))
                .put("autostartEnabled",prefs.getBoolean("autostart",true)).put("recordingRequested",prefs.getBoolean("recording",false))
                .put("lastAutostartRequest",prefs.getLong("autostartAt",0)).put("lastStartFailureAt",prefs.getLong("lastStartFailureAt",0))
                .put("lastStartFailure",prefs.getString("lastStartFailure",""));
    }
    static void offer(Activity activity){
        if(granted(activity)||activity.isFinishing()||activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
        SharedPreferences prefs=activity.getSharedPreferences("wattpit",0);
        if(!prefs.getBoolean("autostart",true)||prefs.getBoolean("background-ble-explained-304",false))return;
        new AlertDialog.Builder(activity).setTitle("Bluetooth nach dem Neustart")
            .setMessage("Damit AutarPie auf Android 10/11 auch nach einem Neustart bei geschlossenem Bildschirm Geräte findet, erlaube unter Berechtigungen → Standort → Immer zulassen. Der Standortdienst muss eingeschaltet bleiben. Diese Freigabe wird für die Bluetooth-Suche benötigt.")
            .setPositiveButton("Berechtigung einstellen",(d,w)->{
                prefs.edit().putBoolean("background-ble-explained-304",true).apply();
                if(Build.VERSION.SDK_INT==29)activity.requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},304);
                else openSettings(activity);
            }).setNegativeButton("Später",(d,w)->prefs.edit().putBoolean("background-ble-explained-304",true).apply()).show();
    }
    private BackgroundBleAccess(){}
}
