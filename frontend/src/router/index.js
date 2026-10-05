import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/',          redirect: '/login' },
  { path: '/login',     component: () => import('../views/Login.vue') },
  { path: '/register',  component: () => import('../views/Register.vue') },

  // 学生侧
  { path: '/student/home',         component: () => import('../views/student/StudentHome.vue') },
  { path: '/student/club/:id',     component: () => import('../views/student/ClubDetail.vue') },
  { path: '/student/enrollments',  component: () => import('../views/student/MyEnrollment.vue') },

  // 管理员侧
  { path: '/admin/dashboard',     component: () => import('../views/admin/Dashboard.vue') },
  { path: '/admin/clubs',         component: () => import('../views/admin/Clubs.vue') },
  { path: '/admin/enrollments',   component: () => import('../views/admin/Enrollments.vue') },
  { path: '/admin/notices',       component: () => import('../views/admin/Notices.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
