package android.net.nsd;

import java.util.ArrayList;
import java.util.List;

/** Controlled Android NSD callbacks only; production TCP, encryption and SQL stay real. */
public final class NsdManager {
    public static final int PROTOCOL_DNS_SD=1;
    public interface RegistrationListener {void onServiceRegistered(NsdServiceInfo value);void onRegistrationFailed(NsdServiceInfo value,int code);void onServiceUnregistered(NsdServiceInfo value);void onUnregistrationFailed(NsdServiceInfo value,int code);}
    public interface DiscoveryListener {void onDiscoveryStarted(String type);void onDiscoveryStopped(String type);void onStartDiscoveryFailed(String type,int code);void onStopDiscoveryFailed(String type,int code);void onServiceFound(NsdServiceInfo value);void onServiceLost(NsdServiceInfo value);}
    public interface ResolveListener {void onResolveFailed(NsdServiceInfo value,int code);void onServiceResolved(NsdServiceInfo value);}
    public final List<DiscoveryListener> searches=new ArrayList<>();
    public final List<ResolveListener> resolutions=new ArrayList<>();
    public volatile int failures,silent,stops;
    public volatile boolean deferResolution;
    public NsdServiceInfo answer;
    public void registerService(NsdServiceInfo info,int protocol,RegistrationListener listener){listener.onServiceRegistered(info);}
    public void unregisterService(RegistrationListener listener){}
    public void discoverServices(String type,int protocol,DiscoveryListener listener){
        searches.add(listener);
        if(failures>0){failures--;listener.onStartDiscoveryFailed(type,0);return;}
        listener.onDiscoveryStarted(type);
        if(silent>0){silent--;return;}
        listener.onServiceFound(answer);
    }
    public void stopServiceDiscovery(DiscoveryListener listener){stops++;listener.onDiscoveryStopped("_wattpit._tcp.");}
    public void resolveService(NsdServiceInfo info,ResolveListener listener){resolutions.add(listener);if(!deferResolution)listener.onServiceResolved(info);}
}
