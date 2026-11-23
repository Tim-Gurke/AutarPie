package de.wattpit.app;

import android.net.nsd.*;
import android.os.SystemClock;
import de.wattpit.app.core.LanWire;
import org.json.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Failed first discovery and a changed central port, without recreating the viewer. */
public final class ViewerReconnectProof {
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("PASS "+text);}
    private static Field field(String name)throws Exception{Field f=LanSync.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void drain(LanSync viewer)throws Exception{((ScheduledExecutorService)field("client").get(viewer)).submit(()->{}).get(8,TimeUnit.SECONDS);}
    private static void due(LanSync viewer)throws Exception{
        // Advance only the retry deadline, not sockets, SQLite or authentication.
        try{field("discoveryAttemptElapsed").setLong(viewer,SystemClock.elapsedRealtime()-31000);}catch(NoSuchFieldException released){}
    }
    private static NsdServiceInfo service(JSONObject config,int port)throws Exception{
        NsdServiceInfo info=new NsdServiceInfo();info.setServiceName("WattPit-"+config.getString("id").substring(0,8));info.setServiceType("_wattpit._tcp.");info.setHost(InetAddress.getByName("127.0.0.1"));info.setPort(port);info.setAttribute("group",config.getString("id"));return info;
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path root=Path.of(args[0]);
        long now=System.currentTimeMillis();JSONObject cached=new JSONObject().put("version",5).put("forecastDays",16).put("pastDays",7).put("signature","same").put("updated",now-60000);
        check(WeatherClient.current(cached,"same",7,"",now),"valid weather remains reusable during normal display polling");
        check(!WeatherClient.current(cached,"same",7,"previous request failed",now),"a failed weather refresh is retried even while the stored forecast is younger than six hours");
        check(!WeatherClient.current(cached,"same",7,"",now+6*3600000L)&&!WeatherClient.current(cached,"changed",7,"",now),"old weather and a changed solar setup cannot suppress a new weather request");
        LanSyncProof.TestContext central=new LanSyncProof.TestContext(root.resolve("central"));Repository repo=LanSyncProof.repository(central);repo.running=true;
        repo.prefs.edit().putString("weatherCache",new JSONObject().put("version",5).put("updated",System.currentTimeMillis()).put("proof","new central data").toString()).commit();
        LanSync host=LanSyncProof.sync(central),paired=null,viewer=null;boolean passed=false;
        try{
            host.create("Morning reconnect fixture");host.startHost();JSONObject config=(JSONObject)field("config").get(host);
            int oldPort=((ServerSocket)field("server").get(host)).getLocalPort();
            String code=LanWire.encodeInvitation(new JSONObject(config.toString()).put("port",oldPort).put("addresses",new JSONArray().put("127.0.0.1")).toString());
            LanSyncProof.TestContext display=new LanSyncProof.TestContext(root.resolve("display"));Repository local=LanSyncProof.repository(display);
            local.prefs.edit().putString("dashboard-v1","local-layout-must-stay").commit();
            paired=LanSyncProof.sync(display);paired.join(code);LanSyncProof.close(paired);paired=null;
            host.stopHost();
            // A competing local listener makes the real host use its existing
            // ephemeral-port fallback. Release the old port before reconnecting.
            try(ServerSocket occupied=new ServerSocket()){occupied.setReuseAddress(true);occupied.bind(new InetSocketAddress(oldPort));host.startHost();}
            int nextPort=((ServerSocket)field("server").get(host)).getLocalPort();
            check(nextPort!=oldPort,"central restart changes its TCP port while the saved pairing remains unchanged");
            viewer=LanSyncProof.sync(display);Object identity=viewer.identity();
            NsdManager nsd=new NsdManager();nsd.answer=service(config,nextPort);nsd.failures=1;field("nsd").set(viewer,nsd);
            viewer.foreground(bundle->{});drain(viewer);
            check(nsd.searches.size()==1&&!viewer.state().optBoolean("connected"),"a fresh morning launch really starts with a failed NSD search and an unreachable saved port");
            due(viewer);viewer.refreshNow();drain(viewer);drain(viewer);
            check(nsd.searches.size()==2&&viewer.state().optBoolean("connected"),"an open viewer retries the failed first search and reconnects without an app restart");
            check(viewer.identity()==identity&&new JSONObject(viewer.snapshot()).getJSONObject("weather").getString("proof").equals("new central data"),"the same authenticated pairing receives the current weather snapshot");
            ((ScheduledFuture<?>)field("refreshTask").get(viewer)).cancel(false);
            ScheduledExecutorService client=(ScheduledExecutorService)field("client").get(viewer);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
            client.execute(()->{entered.countDown();try{release.await();}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
            if(!entered.await(8,TimeUnit.SECONDS))throw new AssertionError("client barrier");
            java.util.Map<?,?> hostRequests=(java.util.Map<?,?>)field("requests").get(host);int before; synchronized(hostRequests){before=hostRequests.size();}
            try{for(int i=0;i<200;i++)viewer.refreshNow();}finally{release.countDown();}drain(viewer);
            synchronized(hostRequests){check(hostRequests.size()==before+1,"many simultaneous gray-field refresh requests coalesce into one queued central snapshot");}
            NsdManager.DiscoveryListener old=nsd.searches.get(0),current=(NsdManager.DiscoveryListener)field("discovery").get(viewer);
            old.onStartDiscoveryFailed("_wattpit._tcp.",0);
            check(field("discovery").get(viewer)==current,"a delayed failure from the first search cannot stop the replacement search");
            Method stop=LanSync.class.getDeclaredMethod("stopDiscovery"),discover=LanSync.class.getDeclaredMethod("discover");stop.setAccessible(true);discover.setAccessible(true);
            stop.invoke(viewer);field("resolvedPort").setInt(viewer,oldPort);nsd.silent=1;discover.invoke(viewer);
            due(viewer);viewer.refreshNow();drain(viewer);drain(viewer);
            check(viewer.state().getBoolean("connected")&&nsd.stops>=2,"a search that starts but delivers no services is also retried after the bounded delay");
            stop.invoke(viewer);nsd.deferResolution=true;discover.invoke(viewer);NsdManager.ResolveListener late=nsd.resolutions.get(nsd.resolutions.size()-1);
            stop.invoke(viewer);discover.invoke(viewer);
            check(((AtomicBoolean)field("resolving").get(viewer)).get(),"the replacement search can start its own resolution after the old search is stopped");
            late.onResolveFailed(nsd.answer,0);
            check(((AtomicBoolean)field("resolving").get(viewer)).get(),"an old resolution failure cannot release the new search's in-flight resolution");
            late.onServiceResolved(service(config,oldPort));
            check(field("resolvedPort").getInt(viewer)==nextPort,"an old resolution success cannot replace the newly discovered central port");
            nsd.resolutions.get(nsd.resolutions.size()-1).onServiceResolved(nsd.answer);drain(viewer);
            viewer.background();int searches=nsd.searches.size();due(viewer);viewer.refreshNow();drain(viewer);
            check(nsd.searches.size()==searches,"a closed display does not keep restarting discovery");
            check(repo.running&&local.prefs.getString("dashboard-v1","").equals("local-layout-must-stay"),"recovery preserves central recording, the saved pairing and the display layout");
            passed=true;
        }catch(Throwable failure){failure.printStackTrace();}finally{if(paired!=null)LanSyncProof.close(paired);if(viewer!=null)LanSyncProof.close(viewer);LanSyncProof.close(host);System.exit(passed?0:1);}
    }
}
