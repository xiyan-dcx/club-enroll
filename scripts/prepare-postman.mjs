import {readFileSync,writeFileSync,mkdirSync} from 'node:fs'
import {execFileSync} from 'node:child_process'
const base='http://127.0.0.1:18082/api'
const ns=readFileSync('.local/benchmark-namespace.txt','utf8').trim()
if(!/^qingju_test_\d+$/.test(ns))throw Error('Only isolated benchmark database allowed')
function sql(query){return execFileSync('mysql.exe',['--defaults-extra-file=mysql-client.cnf','--batch','--skip-column-names',ns,'--execute='+query],{cwd:'.local',encoding:'utf8'}).trim()}
async function call(path,body,cookie=''){const r=await fetch(base+path,{method:'POST',headers:{'Content-Type':'application/json',Cookie:cookie},body:JSON.stringify(body)});const j=await r.json();if(!r.ok||j.code!==0)throw Error(JSON.stringify(j));return{data:j.data,cookie:r.headers.getSetCookie().map(x=>x.split(';')[0]).join('; ')}}
// 固定测试数据；再次运行只刷新会话，不清空预约记录。
let club=sql("SELECT id FROM club WHERE name='BenchmarkClub' LIMIT 1")
if(!club){sql("INSERT INTO club(name,category,leader,max_members,intro) VALUES('BenchmarkClub','学术','负责人',1000,'用于相同数据集的缓存对照测试')");club=sql("SELECT id FROM club WHERE name='BenchmarkClub'")}
if(!/^\d+$/.test(club))throw Error('Invalid club')
if(sql(`SELECT COUNT(*) FROM visit_activity WHERE club_id=${club}`)==='0'){
 for(let i=0;i<100;i++)sql(`START TRANSACTION; INSERT INTO notice(club_id,title,content,publisher_id) VALUES(${club},'Benchmark${i}',REPEAT('activity ',50),1); INSERT INTO visit_activity(club_id,notice_id,title,content,location,capacity,starts_at,ends_at,opens_at,closes_at,cancel_until,publisher_id) VALUES(${club},LAST_INSERT_ID(),'Benchmark${i}',REPEAT('activity ',50),'Hall',10,DATE_ADD(NOW(),INTERVAL 7 DAY),DATE_ADD(NOW(),INTERVAL 8 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 5 DAY),DATE_ADD(NOW(),INTERVAL 6 DAY),1); COMMIT;`)
}
const users=[]
for(let i=0;i<40;i++){
 const username='benchuser'+i,password='BenchTest123'
 if(sql(`SELECT COUNT(*) FROM user WHERE username='${username}'`)==='0')await call('/auth/register',{username,password,realName:'压测学生'+i,phone:'13800138000'})
 const session=await call('/auth/login',{username,password})
 users.push({studentNo:'BENCH'+String(i).padStart(4,'0'),realName:'压测学生'+i,sessionCookie:session.cookie,requestId:'bench-request-'+i})
}
const admin=await call('/auth/login',{username:'admin001',password:'admin123'})
const activity=sql(`SELECT id FROM visit_activity WHERE club_id=${club} ORDER BY id LIMIT 1`)
mkdirSync('.local/postman',{recursive:true})
writeFileSync('.local/postman/users.json',JSON.stringify(users,null,2))
const values={baseUrl:base,adminCookie:admin.cookie,studentCookie:users[0].sessionCookie,activityId:activity,benchmarkClubId:club,usersJson:JSON.stringify(users)}
writeFileSync('.local/postman/qingju.environment.json',JSON.stringify({name:'青聚本地压测',values:Object.entries(values).map(([key,value])=>({key,value,enabled:true,type:'default'})),_postman_variable_scope:'environment'},null,2))
console.log('POSTMAN_FILES=.local/postman (40 independent sessions, isolated database)')
