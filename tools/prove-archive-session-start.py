#!/usr/bin/env python3
"""Exercise actual old/current connect setup with cold/warm asynchronous peers; no APK."""
import argparse,hashlib,json,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--dependencies',required=True,type=Path);p.add_argument('--output',required=True,type=Path);p.add_argument('--baseline-ref',default='5fac3e7bf06a463590b866e6e40c76cb2ebba94e');p.add_argument('--baseline-only',action='store_true');a=p.parse_args()
root=Path(__file__).resolve().parents[1];out=a.output.resolve();out.mkdir(parents=True,exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True)
path='app/src/main/java/de/wattpit/app/ArchiveService.java';current=(root/path).read_text();baseline=subprocess.check_output(['git','show',a.baseline_ref+':'+path],cwd=root,text=True)
def block(s,signature):
 start=s.index(signature);brace=s.index('{',start);depth=1;end=brace+1
 while depth:depth+=(s[end]=='{')-(s[end]=='}');end+=1
 return s[start:end]
signatures=['private void incoming(','private void complete(','private void failOperation(','private void check()','private void op(','private void write(','private byte[] receivePacket(','private void replenish()','private void maintain()','private void subscribe(','private void awaitGattRelease(','private void openGatt(','private void connect(','private void awaitSession(','private void closeGatt()','private static final class GattFailure','private static final class GattRejected','private static final class LinkClosed']
generated=[]
for name,source in [('ArchiveSessionStartCurrent',current),('ArchiveSessionStartBaseline',baseline)]:
 methods=[block(source,s) for s in signatures if s in source];methods.append(block(source,'private final BluetoothGattCallback callback=')+';')
 for optional in ['private void readTransportControl()','private void initializeGatt()']:
  if optional in source:methods.append(block(source,optional))
 wrapper='package de.wattpit.app;\nimport android.os.SystemClock;import de.wattpit.app.core.*;import java.util.*;import java.util.concurrent.*;import java.io.*;\nfinal class '+name+' extends ArchiveSessionStartProof.Environment {\n'+name+'(int api,boolean cold){super(api,cold);}\nprivate interface GattAction{boolean run();}\n@Override void start()throws Exception{connect(new BluetoothDevice());sessionReady=true;}\n@Override void abort(){cancelled=true;closeGatt();}\n'+'\n'.join(methods)+'\n}\n'
 file=out/(name+'.java');file.write_text(wrapper);generated.append(file)
clock=out/'SystemClock.java';clock.write_text('package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}}\n');generated.append(clock)
names=['ArchiveCodec','ArchiveTimeReference','VictronSettings','ArchiveSession','ArchiveLink','ArchiveIo']
sources=[root/'app/src/main/java/de/wattpit/app/core'/(name+'.java') for name in names]+[root/'tools/ArchiveSessionStartProof.java']
jar=a.dependencies.resolve()/'android-api35.jar';subprocess.run(['java','com.sun.tools.javac.Main','-cp',str(jar),'-d',str(classes),*map(str,sources+generated)],check=True)
r=subprocess.run(['java','-cp',str(classes)+':'+str(jar),'de.wattpit.app.ArchiveSessionStartProof',*(['baseline-only'] if a.baseline_only else [])],capture_output=True,text=True,timeout=120);print(r.stdout+r.stderr,end='');(out/'results.log').write_text(r.stdout+r.stderr)
(out/'report.json').write_text(json.dumps({'baseline_ref':a.baseline_ref,'exit_code':r.returncode,'source_sha256':hashlib.sha256(current.encode()).hexdigest(),'checks':[line[5:] for line in r.stdout.splitlines() if line.startswith('PASS ')],'scope':'Full extracted production connect/setup; asynchronous Android/GATT adapters with documented cold/warm behaviour. No physical radio test.'},indent=2)+'\n');r.check_returncode()
