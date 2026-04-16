import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/login', component: () => import('../views/login/LoginPage.vue') },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/dashboard',
    component: () => import('../layouts/admin/AdminLayout.vue'),
    children: [
      { path: '', component: () => import('../views/dashboard/AdminDashboard.vue') },
      { path: '/system/member', component: () => import('../views/system/MemberPage.vue') },
      { path: '/student/list', component: () => import('../views/student/StudentPage.vue') },
      { path: '/student/class', component: () => import('../views/student/ClassPage.vue') },
      { path: '/attendance/plan', component: () => import('../views/attendance/PlanPage.vue') },
      { path: '/attendance/record', component: () => import('../views/attendance/RecordPage.vue') },
      { path: '/attendance/report', component: () => import('../views/attendance/ReportPage.vue') }
    ]
  },
  {
    path: '/m',
    component: () => import('../layouts/mobile/MobileLayout.vue'),
    children: [
      { path: 'tasks', component: () => import('../views/mobile/TaskListPage.vue') },
      { path: 'checkin/:taskId', component: () => import('../views/mobile/CheckinPage.vue') },
      { path: 'classes', component: () => import('../views/mobile/MyClassPage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const store = useUserStore()
  if (to.path === '/login') return next()
  if (!store.token) return next('/login')

  try {
    await store.ensureProfile()
  } catch (_) {
    store.logout()
    return next('/login')
  }

  if (to.path.startsWith('/dashboard') || to.path.startsWith('/system') || to.path.startsWith('/student') || to.path.startsWith('/attendance')) {
    if (!store.isAdmin) return next('/m/tasks')
  }
  if (to.path.startsWith('/m') && store.isAdmin) return next('/dashboard')
  next()
})

export default router
