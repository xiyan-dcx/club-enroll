import assert from 'node:assert/strict'
const base=process.argv[2]
if(new URL(base).port!=='18081'||!process.argv.includes('--allow-write'))throw Error('Requires isolated test instance')
let passed=0
const stamp=Date.now(),reason='希望加入社团共同参加学习交流活动'
const ok=(r,status=200)=>{assert.equal(r.status,status,JSON.stringify(r));return r.data}
async function check(name,fn){await fn();console.log('PASS '+name);passed++}
function client(){let cookie='';return async(method,path,body)=>{const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',Cookie:cookie},...(body?{body:JSON.stringify(body)}:{})});if(r.headers.getSetCookie().length)cookie=r.headers.getSetCookie().map(x=>x.split(';')[0]).join('; ');return{status:r.status,...await r.json()}}}
const admin=client(),a=client(),b=client(),anon=client()
ok(await admin('POST','/auth/login',{username:'admin001',password:'admin123'}))
for(const [c,s] of [[a,'a'],[b,'b']]){const username='flow'+stamp+s;ok(await c('POST','/auth/register',{username,password:'test12345',realName:'注册姓名',phone:'13800138000'}));ok(await c('POST','/auth/login',{username,password:'test12345'}))}
const details={studentNo:'S'+stamp,realName:'提交姓名'}
const club=ok(await admin('POST','/club',{name:'Flow'+stamp,category:'学术',leader:'负责人',maxMembers:20,intro:''})).id
let enrollment
await check('缺少学号姓名不能报名',async()=>ok(await a('POST','/enrollment/apply',{clubId:club,reason}),400))
await check('无需独立身份审核即可报名',async()=>{enrollment=ok(await a('POST','/enrollment/apply',{clubId:club,reason,...details})).id})
await check('管理员待审列表展示提交姓名学号',async()=>{const row=ok(await admin('GET','/enrollment/pending')).records.find(x=>x.id===enrollment);assert.equal(row.studentNo,details.studentNo);assert.equal(row.studentName,details.realName)})
await check('同学号换账号不能重复报名',async()=>ok(await b('POST','/enrollment/apply',{clubId:club,reason,...details}),400))
await check('旧身份审核接口不可用',async()=>{ok(await a('GET','/portal/identity'),400);ok(await admin('POST','/portal/identity-review',{userId:1,version:1,action:'APPROVED',remark:'测试'}),400)})
await check('普通学生不能授权',async()=>ok(await a('POST','/portal/grant',{clubId:club,username:'flow'+stamp+'a'}),403))
ok(await admin('POST','/portal/grant',{clubId:club,username:'flow'+stamp+'a'}))
await check('授权后能查询负责社团',async()=>assert.ok(ok(await a('GET','/portal/clubs')).some(x=>x.id===club)))
const iso=n=>new Date(Date.now()+n*3600000+8*3600000).toISOString().slice(0,19)
const payload={clubId:club,title:'场馆参观',content:'参观信息',location:'场馆',capacity:2,opensAt:iso(-1),closesAt:iso(1),cancelUntil:iso(2),startsAt:iso(3),endsAt:iso(4)}
await check('非负责人不能发布',async()=>ok(await b('POST','/portal/publish',payload),403))
const activity=ok(await a('POST','/portal/publish',payload)).id
await check('匿名不能预约',async()=>ok(await anon('POST','/portal/reserve',{activityId:activity,...details,requestId:'anonymous-1'}),401))
await check('缺少学号姓名不能预约',async()=>ok(await a('POST','/portal/reserve',{activityId:activity,requestId:'missing-1'}),400))
let reservation
await check('直接填写信息可预约且重复重试幂等',async()=>{reservation=ok(await a('POST','/portal/reserve',{activityId:activity,...details,requestId:'reserve-1'}));assert.equal(ok(await a('POST','/portal/reserve',{activityId:activity,...details,requestId:'reserve-1'})).id,reservation.id)})
await check('同账号改学号不能重复占位',async()=>ok(await a('POST','/portal/reserve',{activityId:activity,studentNo:'OTHER',realName:'其他',requestId:'changed-1'}),400))
await check('同学号换账号不能预约',async()=>ok(await b('POST','/portal/reserve',{activityId:activity,...details,requestId:'other-user'}),400))
await check('名单展示提交信息且限制访问',async()=>{const row=ok(await a('GET','/portal/roster?activityId='+activity))[0];assert.equal(row.real_name,details.realName);assert.equal(row.student_no,details.studentNo);ok(await b('GET','/portal/roster?activityId='+activity),403)})
await check('不能取消他人预约',async()=>ok(await b('POST','/portal/cancel',{id:reservation.id,version:reservation.version}),400))
await check('取消回补及旧请求重放不重新占位',async()=>{ok(await a('POST','/portal/cancel',{id:reservation.id,version:reservation.version}));assert.equal(ok(await a('POST','/portal/reserve',{activityId:activity,...details,requestId:'reserve-1'})).status,'CANCELLED')})
await check('新请求重新预约且旧取消版本失效',async()=>{ok(await a('POST','/portal/reserve',{activityId:activity,...details,requestId:'reserve-2'}));ok(await a('POST','/portal/cancel',{id:reservation.id,version:reservation.version}),400)})
await check('负责人可审批本人社团报名',async()=>ok(await a('POST','/portal/review-enrollment',{id:enrollment,action:'pass',remark:'已核对学号姓名通过'})))
await check('授权通知异步送达',async()=>{let rows=[];for(let i=0;i<30;i++){rows=ok(await a('GET','/portal/notifications'));if(rows.some(x=>x.message.includes('授权管理')))break;await new Promise(r=>setTimeout(r,1000))}assert.ok(rows.some(x=>x.message.includes('授权管理')))})
await check('撤销授权立即限制管理权限',async()=>{ok(await admin('POST','/portal/grant',{clubId:club,username:'flow'+stamp+'a',action:'remove'}));ok(await a('POST','/portal/publish',payload),403)})
console.log(`All ${passed} submission checks passed`)
