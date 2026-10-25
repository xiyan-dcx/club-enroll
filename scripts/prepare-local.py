"""Read local DB config without exposing credentials; back up before additive upgrades."""
from pathlib import Path
import subprocess, re, urllib.parse, datetime, shutil
root=Path(__file__).resolve().parents[1]
local=root/'.local'; local.mkdir(exist_ok=True)
source=root/'backend/src/main/resources/db.properties'
config=source.read_text(encoding='utf-8-sig')
props={}
for line in config.splitlines():
 if '=' in line and not line.lstrip().startswith('#'):
  k,v=line.split('=',1);props[k.strip()]=v.strip()
url=urllib.parse.urlparse(props['jdbc.url'][5:])
if url.hostname not in ('127.0.0.1','localhost'):raise SystemExit('Only local database is supported')
database=url.path.strip('/')
if not re.fullmatch(r'[A-Za-z0-9_]+',database):raise SystemExit('Invalid database name')
shutil.copyfile(source,local/'database.properties')
def quote(v):return '"'+v.replace('\\','\\\\').replace('"','\\"')+'"'
client=local/'mysql-client.cnf'
client.write_text('[client]\nhost=127.0.0.1\nport='+str(url.port or 3306)+'\nuser='+quote(props['jdbc.username'])+'\npassword='+quote(props['jdbc.password'])+'\ndefault-character-set=utf8mb4\n',encoding='utf-8')
mysql=shutil.which('mysql.exe');dump=shutil.which('mysqldump.exe')
def sql(text):return subprocess.run([mysql,'--defaults-extra-file=mysql-client.cnf','--batch','--skip-column-names'],cwd=str(local),input=text.encode('utf-8'),stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True).stdout
exists=sql("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='"+database+"';").strip()
if exists:
 backup=local/('backup-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'.sql')
 with backup.open('wb') as f:
  subprocess.run([dump,'--defaults-extra-file=mysql-client.cnf','--single-transaction','--routines','--triggers','--no-tablespaces',database],cwd=str(local),stdout=f,check=True)
 print('BACKUP='+str(backup))
else:
 sql('CREATE DATABASE `'+database+'` DEFAULT CHARACTER SET utf8mb4;')
init=(root/'sql/init.sql').read_text(encoding='utf-8')
# Initial schema only when no user table exists. Never execute DROP DATABASE.
tables=sql("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='"+database+"' AND TABLE_NAME='user';").strip()
if not tables:
 init=re.sub(r'DROP DATABASE[^;]*;|CREATE DATABASE[^;]*;|USE club_enroll;', '', init, flags=re.S)
 sql('USE `'+database+'`;\n'+init)
 print('INITIAL_SCHEMA_CREATED')
print('DATABASE_READY='+database)
