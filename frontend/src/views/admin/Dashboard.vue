<template>
  <AdminLayout>
    <div class="p-6 lg:p-10">
      <!-- 欢迎条 -->
      <div class="brand-hero rounded-3xl p-8 lg:p-10 text-white mb-8 relative overflow-hidden shadow-xl">
        <div class="absolute -top-10 -right-10 w-72 h-72 rounded-full bg-white/15 blur-3xl"></div>
        <div class="absolute -bottom-10 left-1/3 w-64 h-64 rounded-full bg-[#FFE066]/30 blur-3xl"></div>

        <div class="relative">
          <p class="text-white/85 mb-2">{{ greeting }},</p>
          <h1 class="text-3xl lg:text-4xl font-bold mb-2">{{ user?.realName || '管理员' }} 👋</h1>
          <p class="text-white/85">今天的社团运营从这里开始</p>
        </div>
      </div>

      <!-- 统计卡 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <article
          v-for="card in cards"
          :key="card.key"
          class="rounded-2xl p-6 text-white shadow-lg brand-card-hover cursor-pointer relative overflow-hidden"
          :class="card.bg"
          @click="card.go && router.push(card.go)"
        >
          <div class="absolute -top-6 -right-6 w-32 h-32 rounded-full bg-white/15 blur-2xl"></div>
          <div class="relative">
            <div class="flex items-center justify-between mb-4">
              <span class="text-sm text-white/85">{{ card.label }}</span>
              <div class="w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center">
                <el-icon size="20"><component :is="card.icon" /></el-icon>
              </div>
            </div>
            <div class="text-4xl font-bold mb-1">{{ stats[card.key] }}</div>
            <div class="text-xs text-white/80">{{ card.tip }}</div>
          </div>
        </article>
      </div>

      <!-- 快捷入口 -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 bg-white rounded-2xl p-6 lg:p-8 shadow-md border border-[#F2EAE0]">
          <h2 class="text-lg font-bold mb-5 flex items-center gap-2">
            <span class="w-1 h-5 rounded brand-hero"></span>
            待办事项
          </h2>
          <div class="space-y-3">
            <div class="flex items-center justify-between p-4 rounded-xl bg-[#FF6B6B]/8 border border-[#FF6B6B]/15">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-[#FF6B6B]/20 flex items-center justify-center text-[#FF6B6B]">
                  <el-icon><Document /></el-icon>
                </div>
                <div>
                  <div class="font-semibold">{{ stats.pendingCount }} 条报名待审批</div>
                  <div class="text-xs text-[var(--text-secondary)]">学生在等你的回复</div>
                </div>
              </div>
              <el-button type="primary" @click="router.push('/admin/enrollments')">去审批</el-button>
            </div>
            <div class="flex items-center justify-between p-4 rounded-xl bg-[#4ECDC4]/8 border border-[#4ECDC4]/15">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-[#4ECDC4]/20 flex items-center justify-center text-[#2E8B7C]">
                  <el-icon><OfficeBuilding /></el-icon>
                </div>
                <div>
                  <div class="font-semibold">维护社团信息</div>
                  <div class="text-xs text-[var(--text-secondary)]">新增、修改、下架社团</div>
                </div>
              </div>
              <el-button @click="router.push('/admin/clubs')">前往</el-button>
            </div>
            <div class="flex items-center justify-between p-4 rounded-xl bg-[#FFD86B]/15 border border-[#FFD86B]/30">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-[#FFD86B]/30 flex items-center justify-center text-[#8A6500]">
                  <el-icon><Bell /></el-icon>
                </div>
                <div>
                  <div class="font-semibold">发布社团公告</div>
                  <div class="text-xs text-[var(--text-secondary)]">通知学生最新动态</div>
                </div>
              </div>
              <el-button @click="router.push('/admin/notices')">去发布</el-button>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-2xl p-6 lg:p-8 shadow-md border border-[#F2EAE0]">
          <h2 class="text-lg font-bold mb-5 flex items-center gap-2">
            <span class="w-1 h-5 rounded brand-mint"></span>
            系统贴士
          </h2>
          <ul class="space-y-3 text-sm text-[var(--text-secondary)] leading-relaxed">
            <li class="flex gap-2">
              <span class="text-[#FF6B6B] font-bold">·</span>
              管理员可以查看学生侧页面,但学生不能进入管理后台
            </li>
            <li class="flex gap-2">
              <span class="text-[#FF6B6B] font-bold">·</span>
              社团下架后,学生看不到,但历史报名记录保留
            </li>
            <li class="flex gap-2">
              <span class="text-[#FF6B6B] font-bold">·</span>
              拒绝报名必须填写理由,通过则可选
            </li>
            <li class="flex gap-2">
              <span class="text-[#FF6B6B] font-bold">·</span>
              公告只能发给在线社团
            </li>
          </ul>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, User, Document, Bell } from '@element-plus/icons-vue'
import AdminLayout from './AdminLayout.vue'
import api from '@/utils/api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || 'null')

const stats = ref({ clubCount: 0, studentCount: 0, pendingCount: 0, noticeCount: 0 })

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const cards = [
  { key: 'clubCount',    label: '社团总数',   tip: '正在运营的社团',  icon: OfficeBuilding, bg: 'brand-hero',   go: '/admin/clubs' },
  { key: 'studentCount', label: '注册学生',   tip: '平台活跃用户',    icon: User,           bg: 'brand-mint' },
  { key: 'pendingCount', label: '待审批报名', tip: '点击立即处理',    icon: Document,       bg: 'brand-violet', go: '/admin/enrollments' },
  { key: 'noticeCount',  label: '已发布公告', tip: '历史累计',        icon: Bell,           bg: 'brand-sun',    go: '/admin/notices' },
]

async function loadStats() {
  try {
    // 单接口聚合:后端 StatServlet 一次返回 4 个统计字段
    // 字段约定:{ clubCount, studentCount, pendingCount, noticeCount, todayNew, monthlyPassed }
    const resp = await api.get('/stat/dashboard')
    const d = resp.data || {}
    stats.value = {
      clubCount:    d.clubCount    ?? 0,
      studentCount: d.studentCount ?? 0,
      pendingCount: d.pendingCount ?? 0,
      noticeCount:  d.noticeCount  ?? 0,
    }
  } catch (_) {
    // 静默,卡片显示 0 也无妨。错误提示由 axios 拦截器统一处理
  }
}

onMounted(loadStats)
</script>
