#!/usr/bin/env python3
"""Block real SQLite while unchanged ArchiveService IO methods use asynchronous GATT adapters.

Compiles all production Java, but creates no APK. Android Handler/GATT, the peer
and a 4x monotonic clock are simulated. SQL, codec, actor, request/write/ACK and
notification methods are real. This is not a physical radio or boot test.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--dependencies', type=Path, required=True)
p.add_argument('--output', type=Path, required=True)
p.add_argument('--baseline-ref', default='c15f75c6b4be0adea1e543b029484a7353d14473')
a = p.parse_args()
root = Path(__file__).resolve().parents[1]
out = a.output.resolve()
generated = out / 'generated'
classes = out / 'classes'
generated.mkdir(parents=True, exist_ok=True)
classes.mkdir(exist_ok=True)
path = 'app/src/main/java/de/wattpit/app/ArchiveService.java'
current = (root / path).read_text()
baseline = subprocess.run(['git', 'show', a.baseline_ref + ':' + path], cwd=root,
                          check=True, capture_output=True, text=True).stdout


def block(text, signature):
    start = text.index(signature)
    brace = text.index('{', start)
    depth, end = 1, brace + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return text[start:end]


for name, source, fixed in [('ArchiveIoHarness', current, True), ('ArchiveLegacyHarness', baseline, False)]:
    methods = [block(source, signature) for signature in [
        'private void incoming(', 'private void complete(', 'private void failOperation(',
        'private void check()', 'private void trace(', 'private void op(',
        'private void write(', 'private byte[] receivePacket(',
        'private void replenish()', 'private void maintain()',
        'private byte[] exchange(int reg,byte[] command,java.util.function.Predicate<byte[]> accept,long timeout)',
        'private void closeGatt()', 'private void traceSummary()', 'private void saveDiagnostics(',
        'private static final class GattFailure', 'private static final class GattRejected',
        'private static final class LinkClosed',
    ]]
    callback = block(source, 'private final BluetoothGattCallback callback=') + ';'
    methods.append(callback)
    if fixed:
        methods.extend(block(source, signature) for signature in [
            'private void transportFailed(', 'private void idleTransport()', 'private <T>T databaseCall(',
        ])
    for action, wrapper in [('CANCEL', 'cancel'), ('CONTROL_PREEMPT', 'preempt')]:
        action_block = block(source, 'if("' + action + '".equals(intent.getAction()))')
        methods.append('public int ' + wrapper + '()' + action_block[action_block.index('{'):])
    availability = ('databaseCall("Blockierte Bestandsprüfung",()->' if fixed else '')
    availability += 'history.archiveAvailability("io-test","shunt",0xed8d,"voltage",levels,clock,at,this::' + ('check' if fixed else 'maintain') + ')'
    if fixed:
        availability += ')'
    wrapper = '''package de.wattpit.app;
import de.wattpit.app.core.*;
import android.os.SystemClock;
import java.util.*;import java.util.concurrent.*;import java.io.*;import org.json.*;
final class __NAME__ extends ArchiveIoProof.Environment {
 __NAME__(Repository repo,int api){super(repo,api);}
 private interface GattAction {boolean run();}
 @Override void idleTick()throws Exception{__IDLE__}
 @Override void ioFailure(Exception e){__FAIL__}
 @Override void notifyPacket(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] bytes){if(Build.VERSION.SDK_INT>=33)callback.onCharacteristicChanged(g,c,bytes);else{c.setValue(bytes);callback.onCharacteristicChanged(g,c);}}
 @Override void writeComplete(BluetoothGatt g,BluetoothGattCharacteristic c,int status){callback.onCharacteristicWrite(g,c,status);}
 @Override void peerDisconnect(BluetoothGatt g,int status){callback.onConnectionStateChange(g,status,BluetoothProfile.STATE_DISCONNECTED);}
 @Override byte[] request()throws Exception{return exchange(0xec5b,ArchiveCodec.trendQuery(0,1260,3),null,12000);}
 @Override void closeTransport(){closeGatt();}
 @Override void maintenance()throws Exception{maintain();}
 @Override void snapshot(){traceSummary();saveDiagnostics("IO proof");}
 @Override ArchiveSyncPlan.Availability availability(HistoryDb history,List<ArchiveCodec.Level> levels,ArchiveCodec.Clock clock,long at)throws Exception{return __AVAILABILITY__;}
 __METHODS__
}
'''.replace('__NAME__', name).replace('__IDLE__', 'idleTransport();' if fixed else '')
    wrapper = wrapper.replace('__FAIL__', 'transportFailed(e);' if fixed else 'receiveFailure=new IOException(e);')
    wrapper = wrapper.replace('__AVAILABILITY__', availability).replace('__METHODS__', '\n'.join(methods))
    (generated / (name + '.java')).write_text(wrapper)

(generated / 'SystemClock.java').write_text('''package android.os;
public final class SystemClock {
 private static final long START=System.nanoTime();
 public static long elapsedRealtime(){return 1+(System.nanoTime()-START)/250000;}
 public static long uptimeMillis(){return elapsedRealtime();}
 public static long elapsedRealtimeNanos(){return elapsedRealtime()*1000000;}
}''')
version = re.search(r"versionName '([^']+)'", (root / 'app/build.gradle').read_text()).group(1)
(generated / 'BuildConfig.java').write_text('package de.wattpit.app; final class BuildConfig {static final String VERSION_NAME=' + json.dumps(version) + ';}')
(generated / 'R.java').write_text('package de.wattpit.app; final class R {static final class drawable {static final int ic_bolt=1,ic_launcher_foreground=2;}}')
deps = a.dependencies.resolve()
sources = list((root / 'app/src/main/java').rglob('*.java')) + list((root / 'tools/testing').rglob('*.java'))
sources += list(generated.glob('*.java')) + [root / 'tools/ArchiveIoProof.java', root / 'tools/LanSyncProof.java']
cp = os.pathsep.join(map(str, [deps / 'android-api35.jar', *deps.glob('zxing-*.jar')]))
subprocess.run(['java', 'com.sun.tools.javac.Main', '-source', '17', '-target', '17', '-cp', cp,
                '-d', str(classes), *map(str, sources)], check=True)
cp = os.pathsep.join(map(str, [classes, deps / 'android-api35.jar', deps / 'sqlite-jdbc-3.46.1.0.jar',
                              deps / 'slf4j-api-1.7.36.jar', *deps.glob('zxing-*.jar')]))
results = []
for phase in ['legacy', 'fixed30', 'fixed35', 'cancel', 'preempt', 'disconnect', 'actor', 'ack-failure', 'ack-timeout']:
    run = subprocess.run(['java', '-cp', cp, 'de.wattpit.app.ArchiveIoProof', str(out / phase), phase],
                         capture_output=True, text=True, timeout=60)
    print(run.stdout + run.stderr, end='', flush=True)
    (out / (phase + '.log')).write_text(run.stdout + run.stderr)
    results.append({'phase': phase, 'exit_code': run.returncode, 'checks': [s[5:] for s in run.stdout.splitlines() if s.startswith('PASS ')]})
    (out / 'report.json').write_text(json.dumps({
        'scope': __doc__, 'baseline_ref': a.baseline_ref,
        'current_service_sha256': hashlib.sha256(current.encode()).hexdigest(),
        'baseline_service_sha256': hashlib.sha256(baseline.encode()).hexdigest(),
        'source_sha256': {str(s.relative_to(root)): hashlib.sha256(s.read_bytes()).hexdigest() for s in sources if s.is_relative_to(root)},
        'phases': results,
    }, indent=2) + '\n')
    run.check_returncode()
