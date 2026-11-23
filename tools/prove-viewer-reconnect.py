"""Real encrypted LAN/SQLite recovery with controlled Android NSD callbacks; no phone/radio timing."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--dependencies',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
p.add_argument('--baseline-ref',help='Compile the released LanSync for a negative comparison.')
a=p.parse_args();root=Path(__file__).resolve().parents[1];out=a.output.resolve();out.mkdir(parents=True,exist_ok=True)
generated=out/'generated';generated.mkdir(exist_ok=True)
(generated/'R.java').write_text('package de.wattpit.app; final class R {static final class drawable {static final int ic_bolt=1,ic_launcher_foreground=2;}}')
version=re.search(r"versionName '([^']+)'",(root/'app/build.gradle').read_text()).group(1)
(generated/'BuildConfig.java').write_text('package de.wattpit.app; final class BuildConfig {static final String VERSION_NAME='+json.dumps(version)+';}')
(generated/'SystemClock.java').write_text('package android.os; public final class SystemClock {public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long elapsedRealtimeNanos(){return System.nanoTime();}}')
(generated/'Build.java').write_text('package android.os; public final class Build {public static final String MODEL="Reconnect fixture";public static final class VERSION {public static final int SDK_INT=35;}}')
sources=list((root/'app/src/main/java').rglob('*.java'))+list((root/'tools/testing').rglob('*.java'))+list((root/'tools/testing-nsd').rglob('*.java'))+[root/'tools/LanSyncProof.java',root/'tools/ViewerReconnectProof.java']
if a.baseline_ref:
    path='app/src/main/java/de/wattpit/app/LanSync.java'
    sources.remove(root/path)
    (generated/'LanSync.java').write_text(subprocess.check_output(['git','show',a.baseline_ref+':'+path],cwd=root,text=True))
sources+=list(generated.glob('*.java'));deps=a.dependencies.resolve();classes=out/'classes';classes.mkdir(exist_ok=True)
compile_cp=os.pathsep.join(map(str,[deps/'android-api35.jar',*deps.glob('zxing-*.jar')]))
subprocess.run(['java','com.sun.tools.javac.Main','-source','17','-target','17','-cp',compile_cp,'-d',str(classes),*map(str,sources)],check=True)
cp=os.pathsep.join(map(str,[classes,deps/'android-api35.jar',deps/'sqlite-jdbc-3.46.1.0.jar',deps/'slf4j-api-1.7.36.jar',*deps.glob('zxing-*.jar')]))
run=subprocess.run(['java','-cp',cp,'de.wattpit.app.ViewerReconnectProof',str(out/'devices')],capture_output=True,text=True,timeout=45)
(out/'results.log').write_text(run.stdout+run.stderr)
(out/'report.json').write_text(json.dumps({'scope':__doc__,'baseline':a.baseline_ref,'exit_code':run.returncode,'checks':[line[5:] for line in run.stdout.splitlines() if line.startswith('PASS ')]},indent=2)+'\n')
print(run.stdout+run.stderr,end='');run.check_returncode()
