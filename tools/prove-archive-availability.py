#!/usr/bin/env python3
"""Compare released and prepared coverage decisions; compile Java only, never an APK."""
import argparse,hashlib,json,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);p.add_argument('--baseline-ref',default='9cf8d9db51a87141f44abcd63a852f0a833523d6');a=p.parse_args()
root=Path(__file__).resolve().parents[1];out=a.output.resolve();out.mkdir(parents=True,exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True)
path='app/src/main/java/de/wattpit/app/core/ArchiveSyncPlan.java';baseline=subprocess.check_output(['git','show',a.baseline_ref+':'+path],cwd=root,text=True)
reference=out/'ArchiveSyncPlanReference.java';reference.write_text(baseline.replace('ArchiveSyncPlan','ArchiveSyncPlanReference'))
names=['ArchiveCodec','ArchiveTimeReference','VictronSettings','ArchiveMerge','ArchiveSyncPlan','ArchiveTransferPlan','ArchiveSchedule','HistorySchema','Reading','SampleTimeline','EnergyIntegral']
sources=[root/'app/src/main/java/de/wattpit/app/core'/(name+'.java') for name in names]+[root/'tools/ArchiveAvailabilityProof.java']
subprocess.run(['java','com.sun.tools.javac.Main','-d',str(classes),*map(str,sources),str(reference)],check=True)
r=subprocess.run(['java','-Xmx384m','-cp',str(classes),'de.wattpit.app.core.ArchiveAvailabilityProof'],capture_output=True,text=True,timeout=60);print(r.stdout+r.stderr,end='');(out/'results.log').write_text(r.stdout+r.stderr)
(out/'report.json').write_text(json.dumps({'exit_code':r.returncode,'baseline_ref':a.baseline_ref,'source_sha256':{str(s.relative_to(root)):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources},'checks':[v[5:] for v in r.stdout.splitlines() if v.startswith('PASS ')],'measurements':[json.loads(v[8:]) for v in r.stdout.splitlines() if v.startswith('METRICS ')]},indent=2)+'\n');r.check_returncode()
