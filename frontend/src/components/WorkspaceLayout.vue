<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { me, logout } from '../api/auth'
const props = defineProps({ title: String, admin: Boolean })
const router = useRouter()
const user = ref(null)
const ready = ref(false)
const leaving = ref(false)
onMounted(async () => {
  try {
    user.value = (await me()).data
    if ((user.value.role === 'admin') !== props.admin) {
      await router.replace(user.value.role === 'admin' ? '/admin/dashboard' : '/student/home')
      return
    }
    ready.value = true
  } catch { /* 请求封装处理登录过期 */ }
})
async function signOut() {
  leaving.value = true
  try { await logout(); localStorage.removeItem('user'); await router.replace('/login') }
  catch { /* 请求封装显示错误 */ }
  finally { leaving.value = false }
}
</script>
<template>
  <div class="workspace">
    <header class="workspace-header">
      <strong>校园社团报名通</strong>
      <nav v-if="admin">
        <RouterLink to="/admin/dashboard">概览</RouterLink>
        <RouterLink to="/admin/clubs">社团管理</RouterLink>
        <RouterLink to="/admin/enrollments">报名审批</RouterLink>
        <RouterLink to="/admin/notices">公告管理</RouterLink>
      </nav>
      <nav v-else>
        <RouterLink to="/student/home">探索社团</RouterLink>
        <RouterLink to="/student/enrollments">我的报名</RouterLink>
      </nav>
      <span>{{ user?.realName }}</span>
      <el-button :loading="leaving" @click="signOut">退出登录</el-button>
    </header>
    <main class="workspace-main">
      <h1>{{ title }}</h1>
      <slot v-if="ready" />
      <el-skeleton v-else :rows="4" animated />
    </main>
  </div>
</template>
