#!/usr/bin/env python3
"""Compare actual released/current HistoryDb reads with large real SQLite data.

Compile Java only. JDBC streams results, so its timings are not Android
CursorWindow benchmarks and must not be advertised as handset speedups.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--dependencies', required=True, type=Path)
p.add_argument('--output', required=True, type=Path)
p.add_argument('--baseline-ref', default='5fac3e7bf06a463590b866e6e40c76cb2ebba94e')
a = p.parse_args()
root = Path(__file__).resolve().parents[1]
out = a.output.resolve()
generated = out / 'generated'
classes = out / 'classes'
generated.mkdir(parents=True, exist_ok=True)
classes.mkdir(exist_ok=True)
path = 'app/src/main/java/de/wattpit/app/HistoryDb.java'
baseline = subprocess.check_output(['git', 'show', a.baseline_ref+':'+path], cwd=root, text=True)
(generated/'HistoryDbReference.java').write_text(baseline.replace('HistoryDb', 'HistoryDbReference'))
(generated/'SystemClock.java').write_text('package android.os; public final class SystemClock { public static long elapsedRealtime(){return System.nanoTime()/1000000;} public static long uptimeMillis(){return elapsedRealtime();} public static long elapsedRealtimeNanos(){return System.nanoTime();} }')
(generated/'R.java').write_text('package de.wattpit.app; final class R {static final class drawable {static final int ic_bolt=1,ic_launcher_foreground=2;}}')
version = re.search(r"versionName '([^']+)'", (root/'app/build.gradle').read_text()).group(1)
(generated/'BuildConfig.java').write_text('package de.wattpit.app; final class BuildConfig {static final String VERSION_NAME='+json.dumps(version)+';}')
deps = a.dependencies.resolve()
sources = list((root/'app/src/main/java').rglob('*.java')) + list((root/'tools/testing').rglob('*.java'))
sources += list(generated.glob('*.java')) + [root/'tools/ArchivePagingProof.java']
compile_cp = os.pathsep.join(map(str, [deps/'android-api35.jar', *deps.glob('zxing-*.jar')]))
subprocess.run(['java', 'com.sun.tools.javac.Main', '-source', '17', '-target', '17', '-cp', compile_cp, '-d', str(classes), *map(str, sources)], check=True)
cp = os.pathsep.join(map(str, [classes, deps/'android-api35.jar', deps/'sqlite-jdbc-3.46.1.0.jar', deps/'slf4j-api-1.7.36.jar', *deps.glob('zxing-*.jar')]))
phases = {}
logs = []
for phase in ['seed', 'compare', 'resume']:
    run = subprocess.run(['java', '-Xmx512m', '-cp', cp, 'de.wattpit.app.ArchivePagingProof', str(out/'paging.db'), phase], capture_output=True, text=True, timeout=90)
    print(run.stdout+run.stderr, end='', flush=True)
    logs.append(run.stdout+run.stderr)
    (out/'results.log').write_text(''.join(logs))
    phases[phase] = {'exit_code': run.returncode, 'checks': [l[5:] for l in run.stdout.splitlines() if l.startswith('PASS ')], 'metrics': [json.loads(l[8:]) for l in run.stdout.splitlines() if l.startswith('METRICS ')]}
    (out/'report.json').write_text(json.dumps({'baseline_ref': a.baseline_ref, 'scope': 'Actual released and current HistoryDb with real SQLite and large neutral history. Identical coverage, gaps, duplicate timestamps, cancellation and fresh-process reuse. Result-size/index proof; no Android CursorWindow or physical handset timing claim.', 'source_sha256': {str(s.relative_to(root)): hashlib.sha256(s.read_bytes()).hexdigest() for s in sources if s.is_relative_to(root)}, 'phases': phases}, indent=2)+'\n')
    run.check_returncode()
