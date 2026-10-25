from pathlib import Path
import subprocess,re,time
root=Path(__file__).resolve().parents[1];local=root/'.local'
database='qingju_test_'+str(int(time.time()))
text=(root/'sql/init.sql').read_text(encoding='utf-8').replace('club_enroll',database)
text=re.sub(r'DROP DATABASE[^;]*;','',text)
subprocess.run(['mysql.exe','--defaults-extra-file=mysql-client.cnf'],cwd=str(local),input=text.encode('utf-8'),check=True)
config=(local/'database.properties').read_text(encoding='utf-8-sig')
config=re.sub(r'(jdbc:mysql://[^/]+/)[^?\s]+',lambda m:m.group(1)+database,config)
(local/'test-db.properties').write_text(config,encoding='utf-8')
(local/'test-namespace.txt').write_text(database,encoding='ascii')
print('TEST_DATABASE='+database)
