#!/usr/bin/env python3
"""Production SQLite, journal derivation and typed replication; no Android APK or radio."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

p=argparse.ArgumentParser()
p.add_argument('--dependencies',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[1];out=a.output.resolve();out.mkdir(parents=True,exist_ok=True)
generated=out/'generated';generated.mkdir(exist_ok=True)
(generated/'R.java').write_text('package de.wattpit.app; final class R {static final class drawable {static final int ic_bolt=1,ic_launcher_foreground=2;}}')
version=re.search(r"versionName '([^']+)'",(root/'app/build.gradle').read_text()).group(1)
(generated/'BuildConfig.java').write_text('package de.wattpit.app; final class BuildConfig {static final String VERSION_NAME='+json.dumps(version)+';}')
(generated/'SystemClock.java').write_text('package android.os; public final class SystemClock {public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long elapsedRealtimeNanos(){return System.nanoTime();}}')
classes=out/'classes';classes.mkdir(exist_ok=True);deps=a.dependencies.resolve()
sources=list((root/'app/src/main/java').rglob('*.java'))+list((root/'tools/testing').rglob('*.java'))+list(generated.glob('*.java'))+[root/'tools/ChargerJournalProof.java',root/'tools/ChargerCaptureProof.java',root/'tools/LanSyncProof.java']
cp=os.pathsep.join(map(str,[deps/'android-api35.jar',*deps.glob('zxing-*.jar')]))
subprocess.run(['java','com.sun.tools.javac.Main','-source','17','-target','17','-cp',cp,'-d',str(classes),*map(str,sources)],check=True)
cp=os.pathsep.join(map(str,[classes,deps/'android-api35.jar',deps/'sqlite-jdbc-3.46.1.0.jar',deps/'slf4j-api-1.7.36.jar',*deps.glob('zxing-*.jar')]))
database=Path(tempfile.mkdtemp(prefix='journal-',dir=out));results=[]
for phase in ['write','resume']:
    run=subprocess.run(['java','-cp',cp,'de.wattpit.app.ChargerJournalProof',str(database),phase],capture_output=True,text=True,timeout=60)
    results.append({'phase':phase,'exit_code':run.returncode,'checks':[line for line in run.stdout.splitlines() if line.startswith(('PASS ','RESULT '))]})
    (out/'report.json').write_text(json.dumps({'scope':__doc__,'phases':results},indent=2)+'\n')
    (out/(phase+'.log')).write_text(run.stdout+run.stderr)
    print(run.stdout+run.stderr,end='');run.check_returncode()

run=subprocess.run(['java','-cp',cp,'de.wattpit.app.ChargerCaptureProof'],capture_output=True,text=True,timeout=30)
(out/'capture.log').write_text(run.stdout+run.stderr)
results.append({'phase':'capture','exit_code':run.returncode,'checks':[line for line in run.stdout.splitlines() if line.startswith(('PASS ','RESULT '))]})
(out/'report.json').write_text(json.dumps({'scope':__doc__,'phases':results},indent=2)+'\n')
print(run.stdout+run.stderr,end='');run.check_returncode()
