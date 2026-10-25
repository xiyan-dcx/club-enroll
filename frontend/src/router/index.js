import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/',          redirect: '/login' },
  { path: '/login',     component: () => import('../views/Login.vue'),    meta: { public: true } },
  { path: '/register',  component: () => import('../views/Register.vue'), meta: { public: true } },

  // 学生侧
  { path: '/student/portal', component: () => import('../views/Portal.vue'), meta: { role: 'student' } },
  { path: '/admin/portal', component: () => import('../views/Portal.vue'), meta: { role: 'admin' } },
  { path: '/student/home',         component: () => import('../views/student/Home.vue'),          meta: { role: 'student' } },
  { path: '/student/club/:id',     component: () => import('../views/student/ClubDetail.vue'),    meta: { role: 'student' } },
  { path: '/student/my-enrollment',component: () => import('../views/student/MyEnrollment.vue'),  meta: { role: 'student' } },

  // 管理员侧
  { path: '/admin/dashboard',   component: () => import('../views/admin/Dashboard.vue'),   meta: { role: 'admin' } },
  { path: '/admin/clubs',       component: () => import('../views/admin/Clubs.vue'),       meta: { role: 'admin' } },
  { path: '/admin/enrollments', component: () => import('../views/admin/Enrollments.vue'), meta: { role: 'admin' } },
  { path: '/admin/notices',     component: () => import('../views/admin/Notices.vue'),     meta: { role: 'admin' } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫:双角色分流
router.beforeEach(async (to) => {
  if (to.meta.public && !localStorage.getItem('user')) return true
  let user
  try {
    // localStorage 只用于展示，权限依据是服务器 Session。
    const { data } = await axios.get('/api/auth/me', { withCredentials: true, timeout: 10000 })
    if (data.code !== 0 || !['student', 'admin'].includes(data.data?.role)) throw new Error('无效会话')
    user = data.data
    localStorage.setItem('user', JSON.stringify(user))
  } catch (_) {
    localStorage.removeItem('user')
    if (to.meta.public) return true
    ElMessage.warning('请登录后访问；如已登录，请检查服务连接')
    return '/login'
  }
  const home = user.role === 'admin' ? '/admin/dashboard' : '/student/home'
  if (to.meta.public) return home
  if (to.meta.role && to.meta.role !== user.role) {
    ElMessage.error('无权访问该页面')
    return home
  }
  return true
})

export default router
