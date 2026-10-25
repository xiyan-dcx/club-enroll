<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/utils/api'

const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const admin = user.role === 'admin'
const tab = ref(route.query.tab || 'activities')
const loading = ref(false), busy = ref(false), error = ref('')
const activities = ref([]), reservations = ref([])
const clubs = ref([]), managers = ref([])
const roster = ref([]), rosterOpen = ref(false), enrollments = ref([])
const selectedClub = ref(null)
const notifications = ref([])
const bookingForm = reactive({ studentNo: '', realName: user.realName || '' })
const bookingOpen = ref(false), bookingActivity = ref(null)
const grantForm = reactive({ clubId: null, username: '' })
const activityForm = reactive({ clubId: null, title: '', content: '', location: '', capacity: 30, opensAt: '', closesAt: '', cancelUntil: '', startsAt: '', endsAt: '' })
const managed = computed(() => clubs.value.length > 0 || admin)
const managedActivities = computed(() => activities.value.filter(a => clubs.value.some(c => c.id === a.club_id)))
const statusName = s => ({ UNVERIFIED:'未提交', PENDING:'待审核', APPROVED:'已通过', REJECTED:'未通过', REVOKED:'已撤销', CONFIRMED:'预约成功', CANCELLED:'已取消' }[s] || s)
const dateText = s => s?.replace('T', ' ') || '—'
const requestKeys = new Map()

async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await Promise.all([api.get('/portal/activities'), api.get('/portal/clubs')])
    activities.value = data[0].data; clubs.value = data[1].data
    if (!selectedClub.value && clubs.value.length) selectedClub.value = clubs.value[0].id
    if (admin) {
      managers.value = (await api.get('/portal/managers')).data
    } else {
      reservations.value = (await api.get('/portal/reservations')).data
      notifications.value = (await api.get('/portal/notifications')).data
    }
  } catch { error.value = '数据加载失败，请确认数据库升级已执行并重试。' }
  finally { loading.value = false }
}
async function run(action, success) {
  if (busy.value) return
  busy.value = true
  try { await action(); if (success) ElMessage.success(success); await load() }
  catch { /* API 拦截器已展示错误；保留表单供修正 */ }
  finally { busy.value = false }
}
function reserve(a) { bookingActivity.value = a; bookingOpen.value = true }
async function submitBooking() {
  const a = bookingActivity.value
  if (!bookingForm.studentNo.trim() || !bookingForm.realName.trim()) { ElMessage.warning('请填写学号和姓名'); return }
  let key = requestKeys.get(a.id) || sessionStorage.getItem(`visit:${user.id}:${a.id}`)
  if (!key) key = crypto.randomUUID()
  requestKeys.set(a.id, key); sessionStorage.setItem(`visit:${user.id}:${a.id}`, key)
  await run(async () => {
    const res = await api.post('/portal/reserve', { activityId: a.id, requestId: key, ...bookingForm })
    requestKeys.delete(a.id); sessionStorage.removeItem(`visit:${user.id}:${a.id}`)
    bookingOpen.value = false
    tab.value = 'reservations'
    ElMessage.success(res.data.status === 'CONFIRMED' ? '预约成功' : '该次请求对应的预约已取消，请重新预约')
  })
}
async function cancel(row) {
  try {
    await ElMessageBox.confirm('取消后名额将释放，重新预约需要再次竞争名额。', '取消预约')
    await run(async () => {
      await api.post('/portal/cancel', { id: row.id, version: row.version })
      requestKeys.delete(row.activity_id); sessionStorage.removeItem(`visit:${user.id}:${row.activity_id}`)
    }, '预约已取消')
  } catch {}
}
async function publish() {
  await run(async () => {
    await api.post('/portal/publish', activityForm)
    activityForm.title = ''; activityForm.content = ''
  }, '活动公告已发布，学生可在活动预约页面查看')
}
async function showRoster(a) {
  try { roster.value = (await api.get('/portal/roster', { params: { activityId: a.id } })).data; rosterOpen.value = true } catch {}
}
async function cancelActivity(a) {
  try {
    const { value } = await ElMessageBox.prompt('取消整场活动后，预约会取消并发送站内通知。请填写原因。', '取消活动', { inputValidator: v => v?.trim().length >= 2 && v.length <= 200 || '请输入 2—200 字原因' })
    await run(() => api.post('/portal/cancel-activity', { activityId: a.id, reason: value }), '活动已取消，通知将异步送达')
  } catch {}
}
async function loadEnrollments() {
  if (!selectedClub.value) return
  try { enrollments.value = (await api.get('/portal/enrollments', { params: { clubId: selectedClub.value } })).data } catch { enrollments.value = [] }
}
async function reviewEnrollment(row, action) {
  try {
    const { value } = await ElMessageBox.prompt('请填写 5—200 字审批备注', action === 'pass' ? '通过报名' : '拒绝报名', { inputValidator: v => v?.trim().length >= 5 && v.trim().length <= 200 || '请输入 5—200 字' })
    await run(() => api.post('/portal/review-enrollment', { id: row.id, action, remark: value }), '审批完成')
    await loadEnrollments()
  } catch {}
}
onMounted(load)
</script>

<template>
  <div class="portal-shell">
    <header class="portal-header">
      <div><strong>青聚</strong><span>校园社团招新与活动预约</span></div>
      <nav>
        <RouterLink :to="admin ? '/admin/dashboard' : '/student/home'">{{ admin ? '管理后台' : '热门社团' }}</RouterLink>
        <RouterLink v-if="!admin" to="/student/my-enrollment">我的报名</RouterLink>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </nav>
    </header>
    <main class="portal-main" v-loading="loading">
      <h1>{{ admin ? '社团与活动管理' : '探索社团，预约校园体验' }}</h1>
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-4" />
      <div v-if="!admin && clubs.length"><el-tag type="success">社团负责人</el-tag><span> 管理社团：{{ clubs.map(c => c.name).join("、") }}</span></div>
      <el-tabs v-model="tab">
        <el-tab-pane v-if="!admin" label="消息通知" name="notifications">
          <el-alert title="预约及取消结果会异步送达；预约状态以「我的预约」为准。" :closable="false" />
          <el-table :data="notifications"><el-table-column prop="message" label="通知" /><el-table-column prop="created_at" label="时间" /></el-table>
        </el-tab-pane>
        <el-tab-pane label="活动预约" name="activities">
          <div class="activity-grid">
            <article v-for="a in activities" :key="a.id" class="portal-card">
              <el-tag>{{ a.club_name }}</el-tag><h2>{{ a.title }}</h2>
              <p class="activity-content">{{ a.content }}</p>
              <dl><dt>地点</dt><dd>{{ a.location }}</dd><dt>参观时间</dt><dd>{{ dateText(a.starts_at) }} — {{ dateText(a.ends_at) }}</dd><dt>预约窗口</dt><dd>{{ dateText(a.opens_at) }} — {{ dateText(a.closes_at) }}</dd><dt>取消截止</dt><dd>{{ dateText(a.cancel_until) }}</dd></dl>
              <div class="activity-footer"><strong>剩余 {{ a.capacity - a.booked_count }} / {{ a.capacity }} 名额</strong><el-button v-if="!admin" type="primary" :loading="busy" @click="reserve(a)">预约参观</el-button></div>
              <small>无需先报名入社。页面余量仅供参考，以提交结果为准。</small>
            </article>
          </div>
          <el-empty v-if="!activities.length" description="暂无已发布的参观活动" />
        </el-tab-pane>
        <el-tab-pane v-if="!admin" label="我的预约" name="reservations">
          <el-table :data="reservations"><el-table-column prop="title" label="活动" /><el-table-column prop="location" label="地点" /><el-table-column label="时间"><template #default="{row}">{{ dateText(row.starts_at) }}</template></el-table-column><el-table-column label="状态"><template #default="{row}">{{ statusName(row.status) }}</template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button v-if="row.status === 'CONFIRMED'" :disabled="row.server_now >= row.cancel_until" :loading="busy" @click="cancel(row)">取消预约</el-button></template></el-table-column></el-table>
        </el-tab-pane>
        <el-tab-pane v-if="managed" label="发布活动公告" name="publish">
          <section class="portal-card narrow"><h2>发布一场参观活动</h2><p>公告与预约场次同时创建。多个参观时间可分别发布。</p>
            <el-form label-position="top" @submit.prevent="publish">
              <el-form-item label="所属社团"><el-select v-model="activityForm.clubId"><el-option v-for="c in clubs" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
              <el-form-item label="公告标题"><el-input v-model="activityForm.title" maxlength="64" /></el-form-item>
              <el-form-item label="活动说明"><el-input type="textarea" :rows="4" v-model="activityForm.content" maxlength="10000" /></el-form-item>
              <el-form-item label="场馆地点"><el-input v-model="activityForm.location" maxlength="200" /></el-form-item>
              <el-form-item label="预约人数上限"><el-input-number v-model="activityForm.capacity" :min="1" :max="100000" :precision="0" /></el-form-item>
              <el-form-item v-for="[key,label] in [['opensAt','预约开放'],['closesAt','预约截止'],['cancelUntil','取消截止'],['startsAt','参观开始'],['endsAt','参观结束']]" :key="key" :label="label + '（北京时间）'"><el-date-picker v-model="activityForm[key]" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
              <el-button type="primary" native-type="submit" :loading="busy">发布活动公告</el-button>
            </el-form>
          </section>
          <h2>已发布活动</h2><el-table :data="managedActivities"><el-table-column prop="title" label="标题" /><el-table-column prop="club_name" label="社团" /><el-table-column prop="booked_count" label="已预约" /><el-table-column label="操作"><template #default="{row}"><el-button @click="showRoster(row)">预约名单</el-button><el-button v-if="row.status === 'PUBLISHED'" type="danger" @click="cancelActivity(row)">取消活动</el-button></template></el-table-column></el-table>
        </el-tab-pane>
        <el-tab-pane v-if="managed" label="社团报名审批" name="enrollments">
          <el-select v-model="selectedClub" @change="loadEnrollments"><el-option v-for="c in clubs" :key="c.id" :label="c.name" :value="c.id" /></el-select><el-button @click="loadEnrollments">加载报名</el-button>
          <el-table :data="enrollments"><el-table-column label="姓名"><template #default="{row}">{{ row.submitted_name || row.real_name }}</template></el-table-column><el-table-column prop="student_no" label="学号" /><el-table-column prop="reason" label="报名理由" /><el-table-column label="状态"><template #default="{row}">{{ {1:'待审批',2:'已通过',3:'已拒绝'}[row.status] }}</template></el-table-column><el-table-column label="操作"><template #default="{row}"><template v-if="row.status === 1"><el-button :disabled="busy" @click="reviewEnrollment(row,'pass')">通过</el-button><el-button :disabled="busy" @click="reviewEnrollment(row,'reject')">拒绝</el-button></template></template></el-table-column></el-table>
        </el-tab-pane>
        <el-tab-pane v-if="admin" label="负责人授权" name="managers">
          <section class="portal-card narrow"><p>授权现有学生账号管理指定社团，负责人仍可保留自己的学生功能。</p><el-form label-position="top"><el-form-item label="社团"><el-select v-model="grantForm.clubId"><el-option v-for="c in clubs" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item><el-form-item label="负责人账号用户名"><el-input v-model="grantForm.username" /></el-form-item><el-button type="primary" :loading="busy" @click="run(() => api.post('/portal/grant', grantForm),'授权完成')">授权</el-button></el-form></section>
          <el-table :data="managers"><el-table-column prop="name" label="社团" /><el-table-column prop="username" label="账号" /><el-table-column prop="real_name" label="姓名" /><el-table-column label="操作"><template #default="{row}"><el-button :disabled="busy" @click="run(() => api.post('/portal/grant',{clubId:row.club_id,username:row.username,action:'remove'}),'已取消授权')">取消授权</el-button></template></el-table-column></el-table>
        </el-tab-pane>
      </el-tabs>
    </main>
    <el-dialog v-model="bookingOpen" title="预约参观" width="min(500px, 95vw)"><el-form label-position="top" @submit.prevent="submitBooking"><el-form-item label="学号" required><el-input v-model="bookingForm.studentNo" maxlength="32" /></el-form-item><el-form-item label="姓名" required><el-input v-model="bookingForm.realName" maxlength="32" /></el-form-item><el-button native-type="submit" type="primary" :loading="busy">提交预约</el-button></el-form></el-dialog>
    <el-dialog v-model="rosterOpen" title="预约名单" width="min(800px, 95vw)"><el-table :data="roster"><el-table-column prop="student_no" label="学号" /><el-table-column prop="real_name" label="姓名" /><el-table-column label="状态"><template #default="{row}">{{ statusName(row.status) }}</template></el-table-column></el-table></el-dialog>
  </div>
</template>

<style scoped>
.portal-shell{min-height:100vh;background:#faf8f4;color:#243731}.portal-header{background:white;border-bottom:1px solid #e6e9e3;padding:20px max(20px,calc((100vw - 1200px)/2));display:flex;justify-content:space-between;gap:20px;align-items:center;flex-wrap:wrap}.portal-header strong{font-size:26px;color:#267568;margin-right:14px}.portal-header span{font-size:14px;color:#687b74}.portal-header nav{display:flex;align-items:center;gap:20px}.portal-main{max-width:1200px;margin:auto;padding:32px 20px 70px}h1{font-size:30px;font-weight:700}h2{font-size:20px;font-weight:600;margin:14px 0}.portal-intro,p{color:#61746d;line-height:1.8;margin:12px 0}.portal-card{background:white;border:1px solid #e2e7df;border-radius:18px;padding:24px;margin:16px 0}.narrow{max-width:680px}.activity-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,340px),1fr));gap:20px}.activity-content{white-space:pre-wrap;overflow-wrap:anywhere;max-height:200px;overflow:auto}dl{font-size:14px;display:grid;grid-template-columns:75px 1fr;gap:12px}dt{color:#78877f}dd{margin:0}.activity-footer{display:flex;justify-content:space-between;align-items:center;gap:10px;margin:24px 0 12px}small{color:#77877f}.el-table{margin-top:20px}a{color:#267568}
</style>
