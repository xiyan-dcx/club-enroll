<template>
  <div class="min-h-screen flex bg-[var(--bg-base)]">
    <!-- 侧边栏 -->
    <aside class="hidden md:flex md:flex-col w-60 bg-white border-r border-[#F2EAE0] sticky top-0 h-screen">
      <div class="h-16 flex items-center gap-2 px-5 border-b border-[#F2EAE0]">
        <div class="w-9 h-9 rounded-xl brand-hero flex items-center justify-center text-white font-bold">社</div>
        <span class="font-semibold">管理后台</span>
      </div>
      <nav class="flex-1 p-4 space-y-1">
        <RouterLink
          v-for="item in nav"
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-3 px-4 h-11 rounded-xl text-sm font-medium transition-all"
          active-class="brand-hero text-white shadow-md"
          :class="$route.path === item.path ? '' : 'text-[var(--text-primary)] hover:bg-[#FF6B6B]/10'"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="p-4 border-t border-[#F2EAE0]">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-9 h-9 rounded-full brand-mint flex items-center justify-center text-white text-sm font-bold">
            {{ (user?.realName || 'A').charAt(0) }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-semibold truncate">{{ user?.realName || '管理员' }}</div>
            <div class="text-xs text-[var(--text-secondary)]">管理员</div>
          </div>
        </div>
        <el-button class="!w-full" @click="logout">退出登录</el-button>
      </div>
    </aside>

    <!-- 内容区 -->
    <main class="flex-1 min-w-0">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Odometer, OfficeBuilding, Document, Bell } from '@element-plus/icons-vue'
import api from '@/utils/api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || 'null')

const nav = [
  { path: '/admin/dashboard',   label: '仪表盘',   icon: Odometer },
  { path: '/admin/clubs',       label: '社团管理', icon: OfficeBuilding },
  { path: '/admin/enrollments', label: '报名审批', icon: Document },
  { path: '/admin/notices',     label: '公告管理', icon: Bell },
]

async function logout() {
  try {
    await ElMessageBox.confirm('确认退出登录?', '提示', { type: 'warning' })
    try { await api.post('/auth/logout') } catch (_) {}
    localStorage.removeItem('user')
    ElMessage.success('已退出')
    router.replace('/login')
  } catch (_) { /* cancel */ }
}
</script>
