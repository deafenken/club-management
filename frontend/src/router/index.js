import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/', component: () => import('../views/Layout.vue'),
    children: [
      { path: '', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '首页看板' } },
      { path: 'clubs', name: 'Clubs', component: () => import('../views/ClubList.vue'), meta: { title: '社团管理' } },
      { path: 'activities', name: 'Activities', component: () => import('../views/ActivityList.vue'), meta: { title: '活动管理' } },
      { path: 'venues', name: 'Venues', component: () => import('../views/VenueList.vue'), meta: { title: '场地预约' } },
      { path: 'resources', name: 'Resources', component: () => import('../views/ResourceList.vue'), meta: { title: '物资管理' } },
      { path: 'ai-plan', name: 'AiPlan', component: () => import('../views/AiPlan.vue'), meta: { title: 'AI智能策划' } },
      { path: 'funds', name: 'Funds', component: () => import('../views/FundList.vue'), meta: { title: '经费管理' } },
      { path: 'notify-prefs', name: 'NotifyPrefs', component: () => import('../views/NotifyPrefs.vue'), meta: { title: '通知偏好' } },
      { path: 'announce-manage', name: 'AnnounceManage', component: () => import('../views/AnnounceManage.vue'), meta: { title: '公告管理' } },
      { path: 'my-applications', name: 'MyApplications', component: () => import('../views/ApplicationCenter.vue'), meta: { title: '我的申请' } },
      { path: 'admin', name: 'Admin', component: () => import('../views/AdminPage.vue'), meta: { title: '审批中心' } },
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

// 路由守卫：未登录跳转登录页 + 角色权限校验
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) return next('/login')
  // 管理员专属页面
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  const adminOnly = ['/admin', '/announce-manage']
  if (adminOnly.includes(to.path) && user.role !== 'ADMIN') return next('/')
  next()
})

export default router
