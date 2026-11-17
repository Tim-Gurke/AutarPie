#!/usr/bin/env python3
"""Approved APKs only: verify real SAF and restore across two disposable emulator users/UIDs."""
import argparse,json,re,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--serial');p.add_argument('--output',type=Path,required=True);a=p.parse_args();serial=a.serial or subprocess.check_output(['adb','get-serialno'],text=True).strip();adb=['adb','-s',serial];users=[];a.output.mkdir(parents=True,exist_ok=True)
def run(*args,**kw):
 result=subprocess.run(adb+list(args),stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=180,**kw)
 # Android 8's Pm.runCreateUser returns 1 even after printing a successful ID.
 # Accept only that exact response; the caller independently verifies the user exists.
 legacy_created=args[:3]==('shell','pm','create-user') and result.returncode==1 and re.fullmatch(rb'Success: created user id [0-9]+\s*',result.stdout)
 if result.returncode and not legacy_created:
  detail=(result.stdout+result.stderr).decode(errors='replace');(a.output/'adb-error.log').write_text(detail)
  raise RuntimeError('ADB '+repr(args)+' failed: '+detail)
 return result
def phase(user,name):
 run('shell','am','force-stop','--user',str(user),'de.wattpit.app')
 s=run('shell','am','instrument','--user',str(user),'-w','-r','-e','class','de.wattpit.app.BackupIsolationTest','-e','backupPhase',name,'de.wattpit.app.test/androidx.test.runner.AndroidJUnitRunner').stdout.decode();(a.output/(name+'.log')).write_text(s)
 if 'OK (1 test)' not in s or 'AUTARPIE_BACKUP_PHASE_'+name+'_UID_' not in s:raise RuntimeError('Android backup phase failed: '+name)
if run('shell','getprop','ro.kernel.qemu').stdout.strip()!=b'1':raise RuntimeError('Use a disposable multi-user emulator.')
s=run('shell','am','instrument','-w','-r','-e','class','de.wattpit.app.BackupStorageTest','de.wattpit.app.test/androidx.test.runner.AndroidJUnitRunner').stdout.decode();(a.output/'storage.log').write_text(s)
if 'OK (1 test)' not in s or 'AUTARPIE_SAF_BACKUP_VERIFIED' not in s:raise RuntimeError('Android SAF backup failed.')
run('root');run('wait-for-device');previous=run('shell','getprop','fw.max_users').stdout.decode().strip();run('shell','setprop','fw.max_users','4')
try:
 for name in ['AutarPieBackupSource','AutarPieBackupTarget']:
  # Both test users are removed explicitly in finally on disposable emulators.
  result=run('shell','pm','create-user',name).stdout.decode();match=re.search(r'created user id (\d+)',result)
  if not match:raise RuntimeError('Cannot create disposable proof user.')
  user=int(match[1]);users.append(user)
  if not re.search(r'UserInfo\{'+str(user)+r':',run('shell','pm','list','users').stdout.decode()):raise RuntimeError('Created proof user is missing.')
  for package in ['de.wattpit.app','de.wattpit.app.test']:run('shell','cmd','package','install-existing','--user',str(user),package)
  run('shell','am','start-user','-w',str(user))
 phase(users[0],'source');run('shell','run-as','de.wattpit.app','--user',str(users[1]),'mkdir','-p','files')
 for filename in ['isolation.wohnpit','isolation-source.json']:
  data=run('exec-out','run-as','de.wattpit.app','--user',str(users[0]),'cat','files/'+filename).stdout
  run('shell','run-as','de.wattpit.app','--user',str(users[1]),'sh','-c',"'cat > files/"+filename+"'",input=data)
 phase(users[1],'prepare');phase(users[1],'restore')
 (a.output/'report.json').write_text(json.dumps({'passed':True,'source_user':users[0],'target_user':users[1],'scope':'Real Android SAF grants, provider write/read verification, retention, offline/read/rename failures; distinct app UIDs and real AndroidKeyStore, staged authenticated full restore after process restart. No physical Bluetooth or remote cloud upload guarantee.'},indent=2))
finally:
 for user in reversed(users):subprocess.run(adb+['shell','pm','remove-user',str(user)],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 subprocess.run(adb+['shell','setprop','fw.max_users',previous],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
