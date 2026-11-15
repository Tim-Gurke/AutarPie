package de.wattpit.app.core;

/** Packet credits and the session watchdog are independent parts of the BLE transport. */
public final class ArchiveLink {
    public static final int RECEIVE_WINDOW=128,REFILL=65;
    public static final long HEARTBEAT_MS=3000;
    private int sendCredits,pendingReceive,received,consumed,sent,grants;
    private long heartbeat=-1,maxHeartbeatGap;
    private int heartbeats;

    public synchronized void control(byte[] packet){
        if(packet.length==2&&(packet[0]&255)==0xf9)sendCredits=Math.min(255,sendCredits+(packet[1]&255));
    }
    public synchronized boolean reserveSend(){
        if(sendCredits==0)return false;
        sendCredits--;sent++;return true;
    }
    /** Both register characteristics consume the shared notification window. */
    public synchronized void receivedData(){received++;}
    /** Release the peer's next credits only after the reader has freed queue space. */
    public synchronized void consumedData(){pendingReceive++;consumed++;}
    public synchronized int receiveGrant(){
        if(pendingReceive<REFILL)return 0;
        pendingReceive-=REFILL;grants++;return REFILL;
    }
    public synchronized void heartbeatSent(long now){if(heartbeat>=0)maxHeartbeatGap=Math.max(maxHeartbeatGap,Math.max(0,now-heartbeat));heartbeat=now;heartbeats++;}
    public synchronized boolean heartbeatDue(long now){return heartbeat>=0&&now-heartbeat>=HEARTBEAT_MS;}
    public synchronized int heartbeats(){return heartbeats;}
    public synchronized long maxHeartbeatGap(){return maxHeartbeatGap;}
    public synchronized long heartbeatAge(long now){return heartbeat<0?-1:Math.max(0,now-heartbeat);}
    public synchronized int received(){return received;}
    public synchronized int consumed(){return consumed;}
    public synchronized int sent(){return sent;}
    public synchronized int grants(){return grants;}
}
