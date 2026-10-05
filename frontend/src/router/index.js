import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/',          redirect: '/login' },
  { path: '/login',     component: () => import('../views/Login.vue'),    meta: { public: true } },
  { path: '/register',  component: () => import('../views/Register.vue'), meta: { public: true } },

  // 学生侧
  { path: '/student/home',         component: () => import('../views/student/Home.vue'),          meta: { role: 'student' } },
  { path: '/student/club/:id',     component: () => import('../views/student/ClubDetail.vue'),    meta: { role: 'student' } },
  { path: '/student/my-enrollment',component: () => import('../views/student/MyEnrollment.vue'),  meta: { role: 'student' } },

  // 管理员侧
  { path: '/admin/dashboard',   component: () => import('../views/admin/Dashboard.vue'),   meta: { role: 'admin' } },
  { path: '/admin/clubs',       component: () => import('../views/admin/Clubs.vue'),       meta: { role: 'admin' } },
  { path: '/admin/enrollments', component: () => import('../views/admin/Enrollments.vue'), meta: { role: 'admin' } },
  { path: '/admin/notices',     component: () => import('../views/admin/Notices.vue'),     meta: { role: 'admin' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫:双角色分流
router.beforeEach((to, from, next) => {
  let user = null
  try {
    user = JSON.parse(localStorage.getItem('user') || 'null')
  } catch (e) {
    localStorage.removeItem('user')
  }

  // 1. 公开页(login / register)
  if (to.meta.public) {
    if (user) {
      const home = user.role === 'admin' ? '/admin/dashboard' : '/student/home'
      next(home)
    } else {
      next()
    }
    return
  }

  // 2. 受保护页:必须登录
  if (!user) {
    import('element-plus').then(({ ElMessage }) =>
      ElMessage.warning('请先登录')
    )
    next('/login')
    return
  }

  // 3. 角色匹配:学生不能进 admin 页
  if (to.meta.role === 'admin' && user.role !== 'admin') {
    import('element-plus').then(({ ElMessage }) =>
      ElMessage.error('无权访问该页面')
    )
    next('/student/home')
    return
  }

  next()
})

export default router
