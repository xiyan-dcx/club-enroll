// 写入仅用于本地独立测试库：生成带 audit_ 前缀的用户、社团及报名。
// node scripts/smoke-test.mjs http://localhost:18080/club-enroll/api --allow-write
import assert from 'node:assert/strict'

const base = process.argv[2]
if (!base || !process.argv.includes('--allow-write') || !['localhost', '127.0.0.1'].includes(new URL(base).hostname)) {
  throw new Error('仅允许本地独立测试环境，需显式传入 --allow-write')
}
const prefix = `audit_${Date.now()}`
const results = []
function client() {
  let cookie = ''; const studentNo = 'S'+crypto.randomUUID().replaceAll('-','').slice(0,25)
  return async (method, path, body) => {
    if(path==='/enrollment/apply' && typeof body==='object')body={studentNo,realName:'测试学生',...body}
    const res = await fetch(base + path, {
      method,
      headers: { 'Content-Type': 'application/json', ...(cookie ? { Cookie: cookie } : {}) },
      ...(body !== undefined ? { body: typeof body === 'string' ? body : JSON.stringify(body) } : {}),
    })
    const cookies = res.headers.getSetCookie()
    if (cookies.length) cookie = cookies.map(value => value.split(';')[0]).join('; ')
    const json = await res.json()
    return { status: res.status, ...json }
  }
}
async function check(name, fn) {
  try { await fn(); results.push({ name, passed: true }); console.log(`PASS ${name}`) }
  catch (e) { results.push({ name, passed: false, error: e.message }); console.error(`FAIL ${name}: ${e.message}`) }
}
const anonymous = client(), student = client(), admin = client()
const expect = (res, status) => { assert.equal(res.status, status); assert.equal(res.code, status === 200 ? 0 : status); return res.data }
await check('未登录接口返回401', async () => expect(await anonymous('GET', '/club/list'), 401))
await check('公开注册管理员被拒绝', async () => expect(await anonymous('POST', '/auth/register', { username: prefix, password: 'test12345', role: 'admin', realName: '测试', phone: '13800138000' }), 400))
await check('畸形JSON返回400', async () => expect(await anonymous('POST', '/auth/login', '{'), 400))
await check('拒绝text/plain请求体', async () => {
  const res = await fetch(base + '/auth/login', { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: '{"username":"chen","password":"123456"}' })
  assert.equal(res.status, 400)
})
await check('错误密码返回401', async () => expect(await anonymous('POST', '/auth/login', { username: 'chen', password: 'wrong123' }), 401))
await check('示例学生登录且不返回密码', async () => {
  const user = expect(await student('POST', '/auth/login', { username: 'chen', password: '123456' }), 200)
  assert.equal(user.role, 'student'); assert.equal(user.password, undefined)
})
await check('示例管理员登录', async () => assert.equal(expect(await admin('POST', '/auth/login', { username: 'admin001', password: 'admin123' }), 200).role, 'admin'))
async function verifyStudent() {}
for (const path of ['/notice', '/notice/', '/notice/list', '/club/list', '/enrollment/review', '/enrollment/%72eview', '/enrollment/review;test']) {
  await check(`学生写接口拒绝 ${path}`, async () => expect(await student('POST', path, {}), 403))
}
await check('管理员不得提交学生报名', async () => expect(await admin('POST', '/enrollment/apply', { clubId: 1, reason: '我希望加入社团参加活动学习交流' }), 403))
await check('报名缺少ID返回400', async () => expect(await student('POST', '/enrollment/apply', {}), 400))
await check('小数ID返回400', async () => expect(await student('POST', '/enrollment/apply', { clubId: 1.5, reason: '我希望加入社团参加活动学习交流' }), 400))
await check('超大页码不会溢出', async () => expect(await admin('GET', '/enrollment/pending?page=2147483647&size=100'), 400))
let clubId, enrollmentId
const clubData = { name: prefix, category: '学术', leader: '测试负责人', maxMembers: 10, intro: '' }
await check('空简介社团创建成功', async () => { clubId = expect(await admin('POST', '/club', clubData), 200).id })
await check('小数人数上限被拒绝', async () => expect(await admin('POST', '/club', { ...clubData, maxMembers: 1.5 }), 400))
await check('重复社团名称返回400', async () => expect(await admin('POST', '/club', clubData), 400))
await check('报名理由不足10字被拒绝', async () => expect(await student('POST', '/enrollment/apply', { clubId, reason: '太短' }), 400))
await check('学生报名成功', async () => { enrollmentId = expect(await student('POST', '/enrollment/apply', { clubId, reason: '我希望加入社团参加活动学习交流' }), 200).id })
await check('重复报名被拒绝', async () => expect(await student('POST', '/enrollment/apply', { clubId, reason: '我希望加入社团参加活动学习交流' }), 400))
await check('待审接口含学生姓名且支持分页', async () => {
  const page = expect(await admin('GET', '/enrollment/pending?size=100'), 200)
  assert.equal(page.records.find(row => row.id === enrollmentId).studentName, '测试学生')
  assert.equal(page.size, 100)
})
await check('审批通过并递增人数', async () => {
  expect(await admin('POST', '/enrollment/review', { id: enrollmentId, action: 'pass', remark: '欢迎加入社团！' }), 200)
  const data = expect(await admin('GET', `/club/detail?id=${clubId}`), 200)
  assert.equal(data.club.currentMembers, 1)
})
await check('审批备注不足5字被拒绝', async () => expect(await admin('POST', '/enrollment/review', { id: enrollmentId, action: 'pass', remark: '短' }), 400))
await check('重复审批被拒绝', async () => expect(await admin('POST', '/enrollment/review', { id: enrollmentId, action: 'pass', remark: '欢迎加入社团！' }), 400))
await check('学生结果查询为已通过', async () => assert.equal(expect(await student('GET', '/enrollment/mine'), 200).find(row => row.id === enrollmentId).status, 2))
await check('发布公告并查询', async () => {
  expect(await admin('POST', '/notice', { clubId, title: '验证公告', content: '用于验证公告发布与展示。' }), 200)
  assert.equal(expect(await student('GET', `/notice/list?clubId=${clubId}`), 200)[0].title, '验证公告')
})
await check('不存在的公告社团返回400', async () => expect(await admin('POST', '/notice', { clubId: 9223372036854775, title: '测试', content: '测试' }), 400))
await check('下架社团编辑保持下架', async () => {
  expect(await admin('DELETE', `/club?id=${clubId}`), 200)
  expect(await admin('PUT', '/club', { ...clubData, id: clubId, name: prefix + '_edit' }), 200)
  assert.equal(expect(await admin('GET', `/club/detail?id=${clubId}`), 200).club.isActive, 0)
})
await check('不能向下架社团发布公告', async () => expect(await admin('POST', '/notice', { clubId, title: '测试', content: '测试' }), 400))

const clients = []
let capacityId
const applications = []
await check('准备8个独立学生及3名额社团', async () => {
  capacityId = expect(await admin('POST', '/club', { ...clubData, name: prefix + '_capacity', maxMembers: 3 }), 200).id
  for (let i = 0; i < 8; i++) {
    const c = client(), username = `${prefix}_${i}`
    expect(await c('POST', '/auth/register', { username, password: 'test12345', realName: '测试学生' + i, phone: '13800138000' }), 200)
    assert.equal(expect(await c('POST', '/auth/login', { username, password: 'test12345' }), 200).role, 'student')
    clients.push(c)
    await verifyStudent(c, `S${Date.now()}${i}`)
    applications.push(expect(await c('POST', '/enrollment/apply', { clubId: capacityId, reason: '我希望加入社团参加活动学习交流' }), 200).id)
  }
})
await check('8路并发审批仅3条通过且人数为3', async () => {
  const responses = await Promise.all(applications.map(id => admin('POST', '/enrollment/review', { id, action: 'pass', remark: '欢迎加入社团！' })))
  assert.equal(responses.filter(r => r.status === 200).length, 3)
  assert.equal(responses.filter(r => r.status === 400).length, 5)
  assert.equal(expect(await admin('GET', `/club/detail?id=${capacityId}`), 200).club.currentMembers, 3)
  const records = await Promise.all(clients.map(c => c('GET', '/enrollment/mine')))
  assert.equal(records.filter(r => r.data[0].status === 2).length, 3)
  assert.equal(records.filter(r => r.data[0].status === 1).length, 5)
})
await check('待审分页按size截断并提供总数', async () => {
  const result = expect(await admin('GET', '/enrollment/pending?page=1&size=2'), 200)
  assert.equal(result.records.length, 2); assert.ok(result.total > 2)
})
await check('人数上限不能降到已有人数以下', async () => expect(await admin('PUT', '/club', { ...clubData, id: capacityId, name: prefix + '_capacity', maxMembers: 2 }), 400))
await check('满员社团拒绝新报名', async () => expect(await student('POST', '/enrollment/apply', { clubId: capacityId, reason: '我希望加入社团参加活动学习交流' }), 400))
await check('拒绝审批不增加人数', async () => {
  const pending = expect(await admin('GET', '/enrollment/pending?size=100'), 200).records.find(row => applications.includes(row.id))
  expect(await admin('POST', '/enrollment/review', { id: pending.id, action: 'reject', remark: '当前名额已经用完' }), 200)
  assert.equal(expect(await admin('GET', `/club/detail?id=${capacityId}`), 200).club.currentMembers, 3)
})
await check('4路并发重复报名仅1条成功', async () => {
  const id = expect(await admin('POST', '/club', { ...clubData, name: prefix + '_duplicate' }), 200).id
  const responses = await Promise.all(Array.from({ length: 4 }, () => student('POST', '/enrollment/apply', { clubId: id, reason: '我希望加入社团参加活动学习交流' })))
  assert.equal(responses.filter(r => r.status === 200).length, 1)
  assert.equal(responses.filter(r => r.status === 400).length, 3)
})
await check('登出后会话失效', async () => {
  expect(await student('POST', '/auth/logout'), 200)
  expect(await student('GET', '/auth/me'), 401)
})
console.log(JSON.stringify({ total: results.length, passed: results.filter(r => r.passed).length, results }, null, 2))
if (results.some(r => !r.passed)) process.exitCode = 1
