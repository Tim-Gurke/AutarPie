#!/usr/bin/env python3
"""Run unchanged production receive/exchange/session methods against a credit-limited peer.

The Android notification wrapper, clock and BLE writes are simulated. This is a
transport regression proof, not an Android lifecycle or physical radio test.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--source-root', type=Path)
parser.add_argument('--legacy', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
source = args.source_root.resolve() if args.source_root else root
out = args.output.resolve()
generated = out / 'generated'
classes = out / 'classes'
generated.mkdir(parents=True, exist_ok=True)
classes.mkdir(exist_ok=True)
service = source / 'app/src/main/java/de/wattpit/app/ArchiveService.java'
text = service.read_text()

def method(signature):
    start = text.index(signature)
    brace = text.index('{', start)
    depth, end = 1, brace + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return text[start:end]

methods = [method(s) for s in [
    'private void incoming(', 'private void check()', 'private void replenish()',
    'private void maintain()',
    'private byte[] exchange(int reg,byte[] command,java.util.function.Predicate<byte[]> accept,long timeout)',
    'private void awaitSession(',
]]
methods.append(method('private byte[] receivePacket(') if not args.legacy else
               'private byte[] receivePacket(long timeout) throws InterruptedException {return received.poll(timeout,TimeUnit.MILLISECONDS);}')
harness = r'''package de.wattpit.app;
import de.wattpit.app.core.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import android.os.SystemClock;
public final class ArchiveBackpressureProof {
 static int checks;
 static void require(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 static UUID uuid(int n){return UUID.fromString(String.format(Locale.ROOT,"306b%04x-b081-4037-83dc-e59fcc3cdfd0",n));}
 static final UUID CONTROL=uuid(2),DATA=uuid(3),BULK=uuid(4);
 final BlockingQueue<byte[]> received=new LinkedBlockingQueue<>(512);
 final ArrayDeque<ArchiveCodec.Message> messages=new ArrayDeque<>();
 final ArchiveCodec.Stream stream=new ArchiveCodec.Stream();
 final ArchiveLink link=new ArchiveLink();
 volatile boolean cancelled,connected=true;
 volatile IOException receiveFailure;
 int disconnectStatus,peak,notifications,heartbeats,receiveQueuePeak;
 final BluetoothGattCharacteristic control=new BluetoothGattCharacteristic(CONTROL),data=new BluetoothGattCharacteristic(DATA);
 final Peer peer=new Peer();
 static final class BluetoothGattCharacteristic {
  final UUID id;BluetoothGattCharacteristic(UUID id){this.id=id;}UUID getUuid(){return id;}
 }
 static final class LinkClosed extends IOException {LinkClosed(int status){super("link "+status);}}
 void failOperation(Exception e){}
 void write(BluetoothGattCharacteristic c,byte[] bytes)throws Exception{
  check();
  if(c==control){peer.window+=bytes[1]&255;if(peer.window>128)throw new AssertionError("Credits exceed the advertised receive window");peer.flush();}
  else if(Arrays.equals(bytes,ArchiveSession.keepAlive())){heartbeats++;peer.lastHeartbeat=SystemClock.now;}
  else {int reg=(bytes[4]&255)*256+(bytes[5]&255);peer.reply(reg,peer.replyPayload);}
 }
 final class Peer {
  int window=128;
  long lastHeartbeat;
  byte[] replyPayload={11,22,33,44};
  final ArrayDeque<byte[]> pending=new ArrayDeque<>();
  void packet(byte[] bytes){
   if(window<=0)throw new AssertionError("peer exceeded receive credits");window--;
   incoming(new BluetoothGattCharacteristic((notifications++%2)==0?DATA:BULK),bytes);
   peak=Math.max(peak,received.size());
  }
  void burst(){for(int n=0;n<32&&window>0;n++)packet(new byte[]{8,3,0x19,3,0,0x41,1});}
  void flush(){while(window>0&&!pending.isEmpty())packet(pending.removeFirst());}
  void reply(int reg,byte[] payload){
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();bytes.write(8);bytes.write(3);bytes.write(0x19);bytes.write(reg>>8);bytes.write(reg);
   if(payload.length<24)bytes.write(0x40+payload.length);else{bytes.write(0x58);bytes.write(payload.length);}
   bytes.write(payload,0,payload.length);byte[] frame=bytes.toByteArray();
   int cut=Math.min(9,frame.length);pending.add(Arrays.copyOfRange(frame,0,cut));if(cut<frame.length)pending.add(Arrays.copyOfRange(frame,cut,frame.length));flush();
  }
 }
 // Production methods are inserted below unchanged.
 __METHODS__
 public static void main(String[] argv)throws Exception{
  boolean legacy=Boolean.parseBoolean(argv[0]);
  ArchiveBackpressureProof slow=new ArchiveBackpressureProof();slow.link.heartbeatSent(0);
  // A small metadata phase has already consumed real notifications before local SQL work.
  for(int n=0;n<63;n++){slow.peer.packet(new byte[]{8,3,0x19,3,0,0x41,1});slow.stream.feed(slow.receivePacket(0));}
  for(SystemClock.now=250;SystemClock.now<=120000;SystemClock.now+=250){
   slow.peer.burst();
   try{slow.maintain();}catch(IOException e){if(!legacy)throw e;break;}
   if(SystemClock.now-slow.peer.lastHeartbeat>=10000)throw new AssertionError("Watchdog expired during local preflight");
  }
  if(legacy){
   require(slow.cancelled,"3.0.7 falsely marks an overflowing receive queue as cancellation");
   require(slow.peak==512,"3.0.7 fills all 512 queue slots during local preflight");
   require(slow.link.grants()>=7,"3.0.7 keeps granting receive credits without consuming packets");
   System.out.println("RESULT "+checks+" legacy reproduction checks");return;
  }
  require(!slow.cancelled&&slow.receiveFailure==null,"two-minute local preflight survives continuous notifications");
  require(slow.peak<=ArchiveLink.RECEIVE_WINDOW,"queued notifications stay within the advertised 128-packet window");
  require(slow.link.grants()==0,"unprocessed notifications do not release receive credits");
  require(slow.heartbeats==40,"local preflight renews the session every three seconds");
  require(slow.peer.window==0,"peer applies backpressure while the database reader is busy");
  byte[] first=slow.exchange(0xec5a,ArchiveCodec.read(0xec5a),null,12000);
  require(Arrays.equals(first,new byte[]{11,22,33,44}),"production exchange drains backlog and receives the deferred register reply");
  require(slow.link.grants()>0,"production exchange returns credits after freeing queue space");
  byte[] block=new byte[56];for(int n=0;n<block.length;n++)block[n]=(byte)(n*7);
  slow.peer.replyPayload=block;
  require(Arrays.equals(block,slow.exchange(0xec5b,ArchiveCodec.trendQuery(0,1000,24),null,12000)),"split DATA/BULK archive response survives the pause and resume");
  ArchiveBackpressureProof session=new ArchiveBackpressureProof();
  for(int n=0;n<64;n++)session.peer.packet(new byte[]{8,3,0x19,3,0,0x41,1});
  session.peer.packet(new byte[]{7,0,3,0});session.awaitSession(ArchiveSession.setup()[1]);session.replenish();
  require(session.received.isEmpty()&&session.link.grants()==1,"production session setup also acknowledges consumed packets");
  int before=session.link.grants();for(int n=0;n<20;n++){SystemClock.now+=3000;session.maintain();}
  require(session.link.grants()==before,"idle watchdog renewals do not invent receive credits");
  session.received.offer(new byte[0]);session.receivePacket(0);session.replenish();
  require(session.link.grants()==before,"cancellation wake-up is not counted as a data packet");
  ArchiveBackpressureProof broken=new ArchiveBackpressureProof();
  for(int n=0;n<513;n++)broken.incoming(broken.data,new byte[]{7,0,3,0});
  require(!broken.cancelled&&broken.receiveFailure!=null,"a peer exceeding credits produces a transport error instead of user cancellation");
  try{broken.check();throw new AssertionError("Overflow must fail the import");}catch(IOException e){require(e.getMessage().contains("Empfangspuffer voll"),"overflow retains the concrete diagnostic reason");}
  require(slow.link.received()>128,"receive and consumption counters continue after backpressure is released");
  System.out.println("RESULT "+checks+" archive backpressure checks");
 }
}
'''.replace('__METHODS__', '\n'.join(methods))
(generated / 'ArchiveBackpressureProof.java').write_text(harness)
(generated / 'SystemClock.java').write_text('package android.os; public final class SystemClock {public static long now;public static long elapsedRealtime(){return now;}}')
files = [generated / 'ArchiveBackpressureProof.java', generated / 'SystemClock.java']
for name in ['ArchiveLink', 'ArchiveCodec', 'ArchiveSession', 'ArchiveTimeReference', 'VictronSettings']:
    path = source / f'app/src/main/java/de/wattpit/app/core/{name}.java'
    files.append(path if path.exists() else root / f'app/src/main/java/de/wattpit/app/core/{name}.java')
subprocess.run(['java', 'com.sun.tools.javac.Main', '-source', '17', '-target', '17', '-d', str(classes), *map(str, files)], check=True)
run = subprocess.run(['java', '-cp', str(classes), 'de.wattpit.app.ArchiveBackpressureProof', str(args.legacy).lower()], text=True, capture_output=True, timeout=30)
(out / 'results.log').write_text(run.stdout + run.stderr)
(out / 'report.json').write_text(json.dumps({'legacy': args.legacy, 'scope': __doc__, 'exit_code': run.returncode, 'source_sha256': {str(p.relative_to(source) if p.is_relative_to(source) else p.name): hashlib.sha256(p.read_bytes()).hexdigest() for p in [service, *files[2:]]}, 'results': run.stdout.splitlines()}, indent=2) + '\n')
print(run.stdout + run.stderr, end='')
run.check_returncode()
