import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
const base=process.argv[2]
if(!base||!process.argv.includes('--allow-write')||new URL(base).port!=='18081')throw Error('Isolated test instance required')
function client(){let cookie='';const studentNo='T'+crypto.randomUUID().replaceAll('-','').slice(0,25);return async(method,path,body)=>{if(path==='/portal/reserve')body={studentNo,realName:'测试学生',...body};const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',Cookie:cookie},...(body?{body:JSON.stringify(body)}:{})});if(r.headers.getSetCookie().length)cookie=r.headers.getSetCookie().map(x=>x.split(';')[0]).join('; ');return{status:r.status,...await r.json()}}}
const ok=r=>{assert.equal(r.status,200,JSON.stringify(r));return r.data}
const admin=client();ok(await admin('POST','/auth/login',{username:'admin001',password:'admin123'}))
const club=ok(await admin('POST','/club',{name:'Concurrent'+Date.now(),category:'学术',leader:'测试',maxMembers:100,intro:''})).id
const iso=o=>new Date(Date.now()+o+8*3600000).toISOString().slice(0,19)
const act=ok(await admin('POST','/portal/publish',{clubId:club,title:'并发参观测试',content:'测试',location:'测试馆',capacity:10,opensAt:iso(-3600000),closesAt:iso(3600000),cancelUntil:iso(7200000),startsAt:iso(10800000),endsAt:iso(14400000)})).id
const users=[];const stamp=Date.now()
for(let i=0;i<40;i++){
 const c=client(),username=`load${stamp}${i}`
 const id=ok(await c('POST','/auth/register',{username,password:'test12345',realName:'测试',phone:'13800138000'})).id
 ok(await c('POST','/auth/login',{username,password:'test12345'}))
 users.push(c)
}
const start=Date.now()
const results=await Promise.all(users.map((c,i)=>c('POST','/portal/reserve',{activityId:act,requestId:`load-${stamp}-${i}`})))
assert.equal(results.filter(r=>r.status===200).length,10,JSON.stringify(results))
assert.ok(results.every(r=>[200,400].includes(r.status)),JSON.stringify(results))
const row=ok(await admin('GET','/portal/roster?activityId='+act));assert.equal(row.filter(r=>r.status==='CONFIRMED').length,10)
console.log(`PASS 40 concurrent students / 10 places: exactly 10 confirmations (${Date.now()-start} ms, correctness test only)`)
const winner=users[results.findIndex(r=>r.status===200)]
let notices=[]
for(let i=0;i<30;i++){notices=ok(await winner('GET','/portal/notifications'));if(notices.some(x=>x.message.includes('预约成功')))break;await new Promise(r=>setTimeout(r,1000))}
assert.ok(notices.some(x=>x.message.includes('预约成功')),'Kafka notification not delivered')
assert.equal(notices.filter(x=>x.message.includes('预约成功')).length,1)
console.log('PASS transactional outbox -> Kafka -> idempotent inbox delivery')
const namespace=readFileSync('.local/test-namespace.txt','utf8').trim()
assert.match(namespace,/^qingju_test_\d+$/)
function sql(query){return execFileSync('mysql.exe',['--defaults-extra-file=mysql-client.cnf','--batch','--skip-column-names',namespace,'--execute='+query],{cwd:'.local',encoding:'utf8'}).trim()}
const event=sql("SELECT id FROM outbox_event WHERE event_type='RESERVED' ORDER BY id DESC LIMIT 1")
sql(`UPDATE outbox_event SET status='PENDING',next_attempt=NOW() WHERE id=${Number(event)}`)
await new Promise(r=>setTimeout(r,5000))
assert.equal(sql(`SELECT COUNT(*) FROM inbox_notification WHERE event_id=${Number(event)}`),'1')
console.log('PASS duplicate Kafka delivery produces exactly one inbox entry')
const token='recovery-'+Date.now()
sql(`INSERT INTO reservation_attempt(token,user_id,activity_id,state,expires_at) VALUES('${token}',1,${act},'PENDING',DATE_SUB(NOW(),INTERVAL 1 MINUTE))`)
const redis='D:\\Redis-x64-5.0.10\\redis-cli.exe'
const holds=namespace+':{visit:'+act+'}:holds'
execFileSync(redis,['-p','16379','HSET',holds,token,'1'])
for(let i=0;i<15;i++){if(sql(`SELECT state FROM reservation_attempt WHERE token='${token}'`)==='CLEANED')break;await new Promise(r=>setTimeout(r,1000))}
assert.equal(sql(`SELECT state FROM reservation_attempt WHERE token='${token}'`),'CLEANED')
assert.equal(execFileSync(redis,['-p','16379','HEXISTS',holds,token],{encoding:'utf8'}).trim(),'0')
console.log('PASS expired preclaim recovers without releasing a confirmed booking')
// 模拟 AOF 恢复旧预占：数据库已清理，Redis 又出现旧 token。
execFileSync(redis,['-p','16379','HSET',holds,token,'1'])
for(let i=0;i<15;i++){if(execFileSync(redis,['-p','16379','HEXISTS',holds,token],{encoding:'utf8'}).trim()==='0')break;await new Promise(r=>setTimeout(r,1000))}
assert.equal(execFileSync(redis,['-p','16379','HEXISTS',holds,token],{encoding:'utf8'}).trim(),'0')
console.log('PASS reverse reconciliation removes resurrected stale Redis holds')
const limited=await Promise.all(Array.from({length:80},()=>winner('POST','/portal/reserve',{activityId:act,requestId:'rate-limit-test'})))
assert.ok(limited.some(r=>r.status===429),'Lua rate limiting did not activate')
assert.ok(limited.every(r=>[200,429].includes(r.status)),JSON.stringify(limited))
console.log('PASS Redis Lua sliding window returns HTTP 429')
ok(await admin('POST','/portal/cancel-activity',{activityId:act,reason:'场馆临时维护'}))
assert.equal(ok(await admin('GET','/portal/roster?activityId='+act)).filter(r=>r.status==='CONFIRMED').length,0)
for(let i=0;i<20;i++){notices=ok(await winner('GET','/portal/notifications'));if(notices.some(x=>x.message.includes('活动取消')))break;await new Promise(r=>setTimeout(r,1000))}
assert.ok(notices.some(x=>x.message.includes('活动取消')))
console.log('PASS activity cancellation cancels bookings and delivers notification')
const operations=ok(await admin('GET','/portal/operations'))
console.log('OPERATIONS '+JSON.stringify(operations))
