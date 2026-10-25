import {writeFileSync,mkdirSync} from 'node:fs'
const event=(listen,code)=>({listen,script:{type:'text/javascript',exec:code.split('\n')}})
function req(name,path,script='',body=null,cookie='{{adminCookie}}',pre=''){
 return{name,protocolProfileBehavior:{disableCookies:true},request:{method:body?'POST':'GET',header:[{key:'Cookie',value:cookie},{key:'Content-Type',value:'application/json'}],url:'{{baseUrl}}'+path,...(body?{body:{mode:'raw',raw:JSON.stringify(body,null,2)}}:{})},event:[...(pre?[event('prerequest',pre)]:[]),event('test',script)]}
}
const check="pm.test('HTTP 200 and business success',()=>{pm.response.to.have.status(200);pm.expect(pm.response.json().code).to.eql(0)});"
const metrics="const m=pm.response.json().data;"
const dataPre="const cookie=pm.iterationData.get('sessionCookie');if(!cookie)throw Error('Attach users.json as performance data file');pm.request.headers.upsert({key:'Cookie',value:cookie});"
const collection={info:{name:'青聚：并发与缓存对照',schema:'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'},item:[
 {name:'01 缓存对照（只运行此文件夹）',item:[req('活动列表','/portal/activities',check,null,'{{studentCookie}}')]},
 {name:'02 名额竞争（附 users.json）',item:[req('预约同一场次','/portal/reserve',"pm.test('No unexpected server/auth error',()=>pm.expect([200,400,429]).to.include(pm.response.code)); if(pm.response.code===200)pm.test('Confirmed',()=>pm.expect(pm.response.json().data.status).to.eql('CONFIRMED'));",{activityId:'{{activityId}}',studentNo:'{{studentNo}}',realName:'{{realName}}',requestId:'{{requestId}}'},'{{sessionCookie}}',dataPre)]},
 {name:'03 手动检查（不要参与压力循环）',item:[
 req('A 记录压测前计数','/portal/cache-metrics',check+metrics+"pm.environment.set('cacheBefore',JSON.stringify(m));"),
 req('B 查看本轮回源统计','/portal/cache-metrics',check+metrics+`const before=JSON.parse(pm.environment.get('cacheBefore')||'{}');if(before.enabled!==m.enabled)throw Error('Record start snapshot after restarting');const requests=m.requests-before.requests,loads=m.databaseLoads-before.databaseLoads;const result={mode:m.enabled?'开启缓存':'关闭缓存',requests,databaseLoads:loads,loadPercent:requests?(loads/requests*100).toFixed(2):'0'};console.log(result);pm.visualizer.set('<style>body{font:18px sans-serif}td,th{padding:16px;border:1px solid #ddd}</style><h2>{{mode}}</h2><table><tr><th>缓存入口请求</th><th>数据库回源</th><th>回源比例</th></tr><tr><td>{{requests}}</td><td>{{databaseLoads}}</td><td>{{loadPercent}}%</td></tr></table>',result);`),
 req('C 检查预约人数及学号唯一性','/portal/roster?activityId={{activityId}}',check+`const rows=pm.response.json().data.filter(x=>x.status==='CONFIRMED');pm.test('Exactly 10 confirmed',()=>pm.expect(rows.length).to.eql(10));pm.test('Unique student numbers',()=>pm.expect(new Set(rows.map(x=>x.student_no)).size).to.eql(rows.length));pm.visualizer.set('<h2>有效预约：{{count}} / 10</h2><table>{{#each rows}}<tr><td>{{student_no}}</td><td>{{real_name}}</td><td>{{status}}</td></tr>{{/each}}</table>',{count:rows.length,rows});`),
 req('D 查看 Outbox 与预占状态','/portal/operations',check),
 req('E 新建下一轮竞争场次','/portal/publish',check+"pm.environment.set('activityId',String(pm.response.json().data.id));",{clubId:'{{benchmarkClubId}}',title:'Postman名额竞争',content:'场馆参观',location:'Hall',capacity:10,opensAt:'{{opensAt}}',closesAt:'{{closesAt}}',cancelUntil:'{{cancelUntil}}',startsAt:'{{startsAt}}',endsAt:'{{endsAt}}'},'{{adminCookie}}',"for(const [key,h] of Object.entries({opensAt:-1,closesAt:24,cancelUntil:25,startsAt:26,endsAt:27}))pm.variables.set(key,new Date(Date.now()+(h+8)*3600000).toISOString().slice(0,19));"),
 ]}
]}
// 请求标识需按场次隔离，避免换场次复用旧键。
collection.item[1].item[0].event.find(e=>e.listen==='prerequest').script.exec.unshift("pm.variables.set('bookingKey','visit-'+pm.environment.get('activityId')+'-'+pm.iterationData.get('studentNo'));")
collection.item[1].item[0].request.body.raw=collection.item[1].item[0].request.body.raw.replace('{{requestId}}','{{bookingKey}}')
for(const folder of collection.item)for(const item of folder.item)if(item.request.body)item.request.body.raw=item.request.body.raw.replace(/"\{\{(activityId|benchmarkClubId)\}\}"/g,'{{$1}}')
mkdirSync('scripts/postman',{recursive:true})
collection.item.push({name:'04 单次40并发（无数据文件）',item:[req('发送一批并发预约','/auth/me',check+`
const users=JSON.parse(pm.environment.get('usersJson')||'[]');
pm.test('40 independent users supplied',()=>pm.expect(users.length).to.eql(40));
if(users.length!==40)throw Error('Refresh and import generated environment');
const url=pm.environment.get('baseUrl'),id=Number(pm.environment.get('activityId'));
Promise.all(users.map(u=>new Promise(resolve=>{
 const start=Date.now();pm.sendRequest({url:url+'/portal/reserve',method:'POST',header:{'Content-Type':'application/json',Cookie:u.sessionCookie},body:{mode:'raw',raw:JSON.stringify({activityId:id,studentNo:u.studentNo,realName:u.realName,requestId:'visit-'+id+'-'+u.studentNo})}},(err,res)=>resolve({studentNo:u.studentNo,status:err?0:res.code,ms:Date.now()-start,message:err?String(err):res.json().message}));
}))).then(rows=>{
 const times=rows.map(x=>x.ms).sort((a,b)=>a-b),success=rows.filter(x=>x.status===200).length;
 pm.test('Exactly 10 successful first submissions',()=>pm.expect(success).to.eql(10));
 pm.test('Remaining requests are business rejections',()=>pm.expect(rows.filter(x=>x.status===400).length).to.eql(30));
 const report={success,total:rows.length,p95:times[Math.ceil(times.length*.95)-1],rows};console.log(report);
 pm.visualizer.set('<h2>单批并发：{{total}}，成功：{{success}}</h2><p>客户端单批 P95：{{p95}} ms（非持续压测）</p><table>{{#each rows}}<tr><td>{{studentNo}}</td><td>{{status}}</td><td>{{ms}} ms</td><td>{{message}}</td></tr>{{/each}}</table>',report);
});
`)]})
writeFileSync('scripts/postman/qingju.collection.json',JSON.stringify(collection,null,2))
