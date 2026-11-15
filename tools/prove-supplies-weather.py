#!/usr/bin/env python3
"""Compile production sources and check raw travel/weather persistence without an APK."""
import argparse
import os
from pathlib import Path
import subprocess
import sys
import tempfile

p=argparse.ArgumentParser()
p.add_argument('--dependencies',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[1];out=a.output.resolve();out.mkdir(parents=True,exist_ok=True)
subprocess.run([sys.executable,str(root/'tools/prove-charger-journal.py'),'--dependencies',str(a.dependencies.resolve()),'--output',str(out),'--compile-only'],check=True)
deps=a.dependencies.resolve();cp=os.pathsep.join(map(str,[out/'classes',deps/'android-api35.jar',deps/'sqlite-jdbc-3.46.1.0.jar',deps/'slf4j-api-1.7.36.jar',*deps.glob('zxing-*.jar')]))
data=Path(tempfile.mkdtemp(prefix='supplies-',dir=out));run=subprocess.run(['java','-cp',cp,'de.wattpit.app.SupplyWeatherProof',str(data)],capture_output=True,text=True,timeout=60)
(out/'weather.log').write_text(run.stdout+run.stderr);print(run.stdout+run.stderr,end='');run.check_returncode()
subprocess.run(['node',str(root/'tools/supplies-weather-roundtrip.cjs'),str(data/'packed-weather.json')],check=True)
