<template>
  <div class="app-shell" :class="{ collapsed }">
    <!-- ===== 左侧边栏 ===== -->
    <aside class="sidebar">
      <div class="sb-brand" @click="$router.push('/')">
        <div class="sb-logo">
          <svg viewBox="0 0 32 32" fill="none" width="20" height="20">
            <circle cx="13" cy="12" r="4.6" fill="#fff" opacity=".96"/>
            <path d="M7 24c0-4.6 6-7.4 6-7.4s6 2.8 6 7.4v2.6H7V24z" fill="#fff" opacity=".9"/>
            <circle cx="22" cy="12" r="4.1" fill="#fff" opacity=".85"/>
            <path d="M18 24c0-4.2 4.6-6.6 4.6-6.6s4.6 2.4 4.6 6.6v2.6H18V24z" fill="#fff" opacity=".8"/>
          </svg>
        </div>
        <span class="sb-brand-text" v-show="!collapsed">高校社团管理</span>
      </div>

      <nav class="sb-nav">
        <div class="sb-group" v-for="g in visibleGroups" :key="g.title">
          <div class="sb-group-title" v-show="!collapsed">{{ g.title }}</div>
          <el-tooltip v-for="m in g.items" :key="m.path" :content="m.label" placement="right" :disabled="!collapsed" :offset="14">
            <router-link :to="m.path" class="sb-item" :class="{ active: isActive(m.path) }">
              <el-icon class="sb-icon"><component :is="m.icon" /></el-icon>
              <span class="sb-label" v-show="!collapsed">{{ m.label }}</span>
            </router-link>
          </el-tooltip>
        </div>
      </nav>

    </aside>

    <!-- ===== 右侧主区 ===== -->
    <div class="app-main">
      <header class="topbar">
        <div class="tb-left">
          <button class="tb-collapse" @click="toggleCollapse" :title="collapsed ? '展开菜单' : '收起菜单'">
            <el-icon :size="19"><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
          </button>
          <h1 class="tb-title">{{ pageTitle }}</h1>
        </div>

        <div class="tb-right">
          <!-- 通知铃铛 -->
          <el-popover placement="bottom-end" :width="400" trigger="click" @show="onPopoverShow" @hide="onPopoverHide">
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notice-badge">
                <button class="tb-icon-btn"><el-icon :size="19"><Bell /></el-icon></button>
              </el-badge>
            </template>

            <div class="notify-panel">
              <div class="notify-header">
                <span>消息通知</span>
                <div class="notify-header-actions">
                  <el-button v-if="unreadCount > 0" text size="small" type="primary" @click="markAllRead">全部已读</el-button>
                  <el-button v-if="notifications.length > 0" text size="small" type="danger" @click="clearAll">清空</el-button>
                </div>
              </div>
              <div class="notify-tabs">
                <span v-for="t in filterTabs" :key="t.value"
                  class="notify-tab" :class="{ active: filterType === t.value }"
                  @click="switchFilter(t.value)">{{ t.label }}</span>
              </div>

              <div class="notify-list" ref="notifyListRef" @scroll="onScroll" v-if="notifications.length > 0">
                <div v-for="n in notifications" :key="n.id"
                  class="notify-item" :class="{ unread: n.isRead === 0, revoked: n.isRevoked === 1 }"
                  @click="handleNotifyClick(n)">
                  <div class="notify-dot" v-if="n.isRead === 0"></div>
                  <div class="notify-body">
                    <div class="notify-title">
                      <el-tag v-if="n.isRevoked === 1" size="small" type="info" class="revoked-tag">已撤回</el-tag>
                      {{ n.title }}
                    </div>
                    <div class="notify-content">{{ n.isRevoked === 1 ? '该通知已被撤回' : n.content }}</div>
                    <div class="notify-time">{{ formatTime(n.createTime) }}</div>
                  </div>
                </div>
                <div v-if="loadingMore" class="notify-loading"><el-icon class="is-loading"><Loading /></el-icon> 加载中…</div>
                <div v-if="!hasMore && notifications.length > 0" class="notify-loading">没有更多了</div>
              </div>

              <div class="notify-empty" v-else>
                <svg viewBox="0 0 120 120" width="72" height="72" class="empty-svg">
                  <circle cx="60" cy="45" r="20" fill="#EEEBE1"/>
                  <rect x="30" y="75" width="60" height="8" rx="4" fill="#EEEBE1"/>
                  <rect x="40" y="90" width="40" height="8" rx="4" fill="#F5F3EC"/>
                  <circle cx="55" cy="40" r="2" fill="#D6D2C4"/>
                  <circle cx="65" cy="40" r="2" fill="#D6D2C4"/>
                  <path d="M52 50 Q60 56 68 50" stroke="#D6D2C4" fill="none" stroke-width="1.5"/>
                </svg>
                <p>暂无消息通知</p>
                <span>审批结果、新公告等消息会显示在这里</span>
              </div>
            </div>
          </el-popover>

          <!-- 账户中心 -->
          <el-popover placement="bottom-end" :width="280" trigger="hover" :show-after="200" @show="fetchAccountInfo">
            <template #reference>
              <div class="tb-user">
                <div class="tb-avatar">{{ (user.realName || 'U').charAt(0) }}</div>
                <div class="tb-user-meta">
                  <span class="tb-user-name">{{ user.realName || '用户' }}</span>
                  <span class="tb-user-role">{{ roleMap[user.role] || user.role }}</span>
                </div>
                <el-icon class="tb-user-caret"><ArrowDown /></el-icon>
              </div>
            </template>

            <div class="account-popover">
              <div class="acct-header">
                <div class="acct-avatar"><el-icon :size="28"><UserFilled /></el-icon></div>
                <div class="acct-name-row">
                  <span class="acct-name">{{ user.realName || '用户' }}</span>
                  <el-tag :type="roleTypeMap[user.role]" size="small" round>{{ roleMap[user.role] || user.role }}</el-tag>
                </div>
              </div>
              <div class="acct-details">
                <div class="acct-row"><el-icon><User /></el-icon><span>用户名：{{ user.username || '-' }}</span></div>
                <div class="acct-row"><el-icon><Phone /></el-icon><span>{{ user.phone || userInfo.phone || '未绑定手机号' }}</span></div>
                <div class="acct-row"><el-icon><School /></el-icon><span>{{ user.college || userInfo.college || '未设置学院' }}</span></div>
                <div class="acct-row"><el-icon><Clock /></el-icon><span>注册时间：{{ formatDate(user.createTime || userInfo.createTime) }}</span></div>
              </div>
              <div class="acct-stats">
                <div class="acct-stat-item"><span class="acct-stat-num">{{ stats.clubCount ?? '-' }}</span><span class="acct-stat-label">我的社团</span></div>
                <div class="acct-stat-item"><span class="acct-stat-num">{{ stats.activityCount ?? '-' }}</span><span class="acct-stat-label">参与活动</span></div>
                <div class="acct-stat-item"><span class="acct-stat-num">{{ stats.borrowCount ?? '-' }}</span><span class="acct-stat-label">物资借用</span></div>
                <div class="acct-stat-item"><span class="acct-stat-num">{{ stats.bookingCount ?? '-' }}</span><span class="acct-stat-label">场地预约</span></div>
              </div>
              <div class="acct-logout" @click="logout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </div>
            </div>
          </el-popover>
        </div>
      </header>

      <main class="content">
        <router-view />
        <footer class="global-footer">© 2026 高校社团综合管理系统 · Powered by Spring Boot &amp; Vue 3</footer>
      </main>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request.js'

const router = useRouter()
const route = useRoute()
const user = reactive(JSON.parse(localStorage.getItem('user') || '{}'))

// ---- 侧边栏折叠 ----
const collapsed = ref(localStorage.getItem('sb-collapsed') === '1')
const toggleCollapse = () => {
  collapsed.value = !collapsed.value
  localStorage.setItem('sb-collapsed', collapsed.value ? '1' : '0')
}

const pageTitle = computed(() => route.meta?.title || '首页看板')

const stats = reactive({ clubCount: null, activityCount: null, borrowCount: null, bookingCount: null })
const statsLoaded = ref(false)
const userInfo = reactive({ phone: '', college: '', createTime: '' })

const fetchAccountInfo = async () => {
  if (statsLoaded.value) return
  try {
    const [statsRes, infoRes] = await Promise.all([
      request.get('/user/stats'),
      request.get('/user/info')
    ])
    Object.assign(stats, statsRes.data)
    if (infoRes.data) {
      userInfo.phone = infoRes.data.phone || ''
      userInfo.college = infoRes.data.college || ''
      userInfo.createTime = infoRes.data.createTime || ''
    }
    statsLoaded.value = true
  } catch (e) { /* ignore */ }
}

const formatDate = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

// ---- 分组菜单 ----
const menuGroups = [
  { title: '概览', items: [
    { path: '/', label: '首页看板', icon: 'DataAnalysis' },
  ]},
  { title: '社团业务', items: [
    { path: '/clubs', label: '社团管理', icon: 'OfficeBuilding' },
    { path: '/activities', label: '活动管理', icon: 'Calendar' },
    { path: '/venues', label: '场地预约', icon: 'Location' },
    { path: '/resources', label: '物资管理', icon: 'Box' },
    { path: '/funds', label: '经费管理', icon: 'Wallet' },
  ]},
  { title: '智能工具', items: [
    { path: '/ai-plan', label: 'AI 智能策划', icon: 'MagicStick' },
  ]},
  { title: '个人中心', items: [
    { path: '/my-applications', label: '我的申请', icon: 'Document' },
    { path: '/notify-prefs', label: '通知偏好', icon: 'Bell' },
  ]},
  { title: '系统管理', roles: ['ADMIN'], items: [
    { path: '/admin', label: '审批中心', icon: 'Setting' },
    { path: '/announce-manage', label: '公告管理', icon: 'ChatDotSquare' },
  ]},
]
const visibleGroups = computed(() =>
  menuGroups.filter(g => !g.roles || g.roles.includes(user.role || 'STUDENT'))
)

const roleMap = { ADMIN: '管理员', PRESIDENT: '社长', TEACHER: '指导老师', STUDENT: '学生' }
const roleTypeMap = { ADMIN: 'danger', PRESIDENT: 'warning', TEACHER: 'success', STUDENT: '' }

const isActive = (p) => {
  if (p === '/') return route.path === '/'
  return route.path.startsWith(p)
}

const logout = () => {
  localStorage.clear()
  router.push('/login')
}

// ====== 通知系统 ======
const unreadCount = ref(0)
const notifications = ref([])
const filterType = ref('ALL')
const currentPage = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)
const notifyListRef = ref(null)
let pollTimer = null

const filterTabs = [
  { label: '全部', value: 'ALL' },
  { label: '审批', value: 'APPROVAL' },
  { label: '活动', value: 'ACTIVITY' },
  { label: '公告', value: 'ANNOUNCEMENT' },
]

const businessRouteMap = {
  CLUB_APPROVAL: '/clubs', MEMBER_APPROVAL: '/clubs', JOIN_REQUEST: '/clubs',
  ACTIVITY_APPROVAL: '/activities', ACTIVITY_PENDING: '/activities',
  VENUE_APPROVAL: '/venues', VENUE_PENDING: '/venues',
  RESOURCE_APPROVAL: '/resources', RESOURCE_PENDING: '/resources',
  FUND_APPROVAL: '/funds', FUND_PENDING: '/funds',
  ANNOUNCEMENT: '/', SYSTEM: '/',
}

const fetchUnreadCount = async () => {
  try {
    const res = await request.get('/notification/unread-count')
    unreadCount.value = res.data.count
  } catch (e) { /* ignore */ }
}

const loadNotifications = async (reset = true) => {
  try {
    if (reset) { currentPage.value = 1; hasMore.value = true }
    loadingMore.value = true
    const res = await request.get('/notifications', {
      params: { page: currentPage.value, size: 20, filterType: filterType.value }
    })
    const records = res.data.records || []
    notifications.value = reset ? records : [...notifications.value, ...records]
    hasMore.value = records.length >= 20
  } catch (e) { /* ignore */ } finally { loadingMore.value = false }
}

const onScroll = () => {
  const el = notifyListRef.value
  if (!el || loadingMore.value || !hasMore.value) return
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 60) {
    currentPage.value++
    loadNotifications(false)
  }
}

const switchFilter = (val) => { filterType.value = val; loadNotifications(true) }
const onPopoverShow = () => { loadNotifications(true) }
const onPopoverHide = () => {}

const handleNotifyClick = async (n) => {
  if (n.isRevoked === 1) return
  if (n.isRead === 0) {
    try {
      await request.put(`/notification/read/${n.id}`)
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      try { new BroadcastChannel('notify-sync').postMessage({ type: 'read', id: n.id }) } catch (e) { /* */ }
    } catch (e) { /* ignore */ }
  }
  router.push(businessRouteMap[n.businessType] || '/')
}

const markAllRead = async () => {
  try {
    const res = await request.put('/notification/read-all')
    ElMessage.success(res.data.msg || '全部已读')
    notifications.value.forEach(n => n.isRead = 1)
    unreadCount.value = 0
    try { new BroadcastChannel('notify-sync').postMessage({ type: 'markAll' }) } catch (e) { /* */ }
  } catch (e) { /* ignore */ }
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有通知吗？此操作不可恢复', '确认清空', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    const res = await request.delete('/notification/clear-all')
    ElMessage.success(res.data.msg || '已清空')
    notifications.value = []
    unreadCount.value = 0
  } catch (e) { /* cancelled */ }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const diff = new Date() - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return (d.getMonth() + 1) + '/' + d.getDate() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const handleVisibility = () => {
  if (document.hidden) {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  } else {
    fetchUnreadCount()
    if (!pollTimer) pollTimer = setInterval(fetchUnreadCount, 30000)
  }
}

const setupBroadcast = () => {
  try {
    const bc = new BroadcastChannel('notify-sync')
    bc.onmessage = (e) => {
      if (e.data.type === 'read' || e.data.type === 'markAll') fetchUnreadCount()
    }
  } catch (e) { /* unsupported */ }
}

onMounted(() => {
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 30000)
  document.addEventListener('visibilitychange', handleVisibility)
  setupBroadcast()
})
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('visibilitychange', handleVisibility)
})
</script>

<style scoped>
.app-shell { display: flex; height: 100vh; background: var(--clay-50); }

/* ============================================
   侧边栏
   ============================================ */
.sidebar {
  width: 236px;
  flex-shrink: 0;
  background: var(--surface);
  border-right: 1px solid var(--clay-200);
  display: flex;
  flex-direction: column;
  transition: width .22s cubic-bezier(.4,0,.2,1);
  overflow: hidden;
}
.app-shell.collapsed .sidebar { width: 68px; }

.sb-brand {
  display: flex; align-items: center; gap: 11px;
  height: 60px; padding: 0 20px; cursor: pointer; flex-shrink: 0;
  border-bottom: 1px solid var(--clay-150);
}
.sb-logo {
  width: 34px; height: 34px; border-radius: 9px; flex-shrink: 0;
  background: linear-gradient(135deg, #D9764F, #B4522F);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(201,100,66,.3);
}
.sb-brand-text { font-size: 15.5px; font-weight: 700; color: var(--ink-900); white-space: nowrap; letter-spacing: .01em; }

.sb-nav { flex: 1; overflow-y: auto; overflow-x: hidden; padding: 12px 12px 8px; }
.sb-group { margin-bottom: 10px; }
.sb-group-title {
  font-size: 11px; font-weight: 700; color: var(--ink-400);
  letter-spacing: .08em; padding: 8px 12px 6px; white-space: nowrap;
}
.sb-item {
  display: flex; align-items: center; gap: 11px;
  height: 40px; padding: 0 12px; margin-bottom: 2px;
  border-radius: 9px; text-decoration: none; white-space: nowrap;
  color: var(--ink-500); font-size: 14px; font-weight: 500;
  position: relative; transition: background .15s, color .15s;
}
.sb-item:hover { background: var(--clay-100); color: var(--ink-900); }
.sb-item.active { background: var(--coral-50); color: var(--coral); font-weight: 650; }
.sb-item.active::before {
  content: ''; position: absolute; left: -12px; top: 50%; transform: translateY(-50%);
  width: 3px; height: 20px; border-radius: 0 3px 3px 0; background: var(--coral);
}
.sb-icon { font-size: 18px; flex-shrink: 0; }
.app-shell.collapsed .sb-item { justify-content: center; padding: 0; }
.app-shell.collapsed .sb-item.active::before { left: 0; }

.sb-foot {
  display: flex; align-items: center; gap: 10px;
  height: 46px; padding: 0 22px; cursor: pointer; flex-shrink: 0;
  border-top: 1px solid var(--clay-150); color: var(--ink-400);
  font-size: 13px; transition: color .15s, background .15s;
}
.sb-foot:hover { color: var(--coral); background: var(--clay-100); }
.app-shell.collapsed .sb-foot { justify-content: center; padding: 0; }

/* ============================================
   主区 + 顶栏
   ============================================ */
.app-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.topbar {
  height: 60px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 26px;
  background: rgba(250,249,245,.82);
  backdrop-filter: saturate(180%) blur(10px);
  border-bottom: 1px solid var(--clay-200);
  position: sticky; top: 0; z-index: 50;
}
.tb-left { display: flex; align-items: center; gap: 12px; }
.tb-collapse {
  width: 36px; height: 36px; border-radius: 9px; border: none; cursor: pointer;
  background: transparent; color: var(--ink-500);
  display: flex; align-items: center; justify-content: center; transition: all .15s;
}
.tb-collapse:hover { background: var(--clay-150); color: var(--coral); }
.tb-title { font-size: 18px; font-weight: 700; color: var(--ink-900); letter-spacing: -.01em; margin: 0; }
.tb-right { display: flex; align-items: center; gap: 8px; }

.tb-icon-btn {
  width: 38px; height: 38px; border-radius: 10px; border: none; cursor: pointer;
  background: transparent; color: var(--ink-500);
  display: flex; align-items: center; justify-content: center; transition: all .15s;
}
.tb-icon-btn:hover { background: var(--clay-150); color: var(--ink-900); }

.tb-user {
  display: flex; align-items: center; gap: 9px;
  padding: 5px 10px 5px 6px; border-radius: 11px; cursor: pointer;
  transition: background .15s;
}
.tb-user:hover { background: var(--clay-150); }
.tb-avatar {
  width: 34px; height: 34px; border-radius: 9px; flex-shrink: 0;
  background: linear-gradient(135deg, #D9764F, #B4522F); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 15px;
}
.tb-user-meta { display: flex; flex-direction: column; line-height: 1.25; }
.tb-user-name { font-size: 13.5px; font-weight: 650; color: var(--ink-900); }
.tb-user-role { font-size: 11.5px; color: var(--ink-400); }
.tb-user-caret { color: var(--ink-400); font-size: 13px; }

.content { flex: 1; overflow-y: auto; padding: 24px 28px; }
.global-footer {
  text-align: center; padding: 28px 0 12px;
  font-size: 12px; color: var(--ink-400); letter-spacing: .02em;
}

/* 视图过渡 */
.view-fade-enter-active, .view-fade-leave-active { transition: opacity .2s ease, transform .2s ease; }
.view-fade-enter-from { opacity: 0; transform: translateY(8px); }
.view-fade-leave-to { opacity: 0; }

/* 账户弹窗内的退出 */
.acct-logout {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 12px; padding: 9px; border-radius: 9px; cursor: pointer;
  color: var(--rust); font-size: 13.5px; font-weight: 600;
  background: var(--clay-100); transition: background .15s;
}
.acct-logout:hover { background: #F5E4E1; }

/* 响应式：窄屏自动折叠 */
@media (max-width: 820px) {
  .sidebar { width: 68px; }
  .sb-brand-text, .sb-label, .sb-group-title, .sb-foot span { display: none; }
  .sb-item { justify-content: center; padding: 0; }
  .tb-user-meta { display: none; }
  .content { padding: 16px 14px; }
}

/* ============================================
   通知面板（弹窗内容，保留原样式）
   ============================================ */
.notify-panel { max-height: 480px; display: flex; flex-direction: column; }
.notify-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 10px; border-bottom: 1px solid var(--clay-150);
  font-weight: 700; font-size: 15px; color: var(--ink-900);
}
.notify-header-actions { display: flex; gap: 4px; }
.notify-tabs { display: flex; gap: 6px; padding: 10px 0; border-bottom: 1px solid var(--clay-150); }
.notify-tab {
  padding: 4px 12px; border-radius: 14px; font-size: 12px; cursor: pointer;
  color: var(--ink-500); background: var(--clay-100); transition: all .2s; white-space: nowrap;
}
.notify-tab:hover { color: var(--coral); background: var(--coral-50); }
.notify-tab.active { color: #fff; background: var(--coral); }
.notify-list { flex: 1; overflow-y: auto; max-height: 360px; }
.notify-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 6px; cursor: pointer; border-radius: 8px;
  transition: background .15s; border-bottom: 1px solid var(--clay-150);
}
.notify-item:hover { background: var(--clay-100); }
.notify-item.unread { background: var(--coral-50); }
.notify-item.unread .notify-title { color: var(--coral); }
.notify-item.revoked { opacity: .5; cursor: default; }
.notify-item.revoked:hover { background: transparent; }
.notify-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--coral); flex-shrink: 0; margin-top: 6px; }
.notify-body { flex: 1; min-width: 0; }
.notify-title { font-size: 14px; font-weight: 600; color: var(--ink-900); margin-bottom: 3px; display: flex; align-items: center; gap: 6px; }
.revoked-tag { font-size: 11px; }
.notify-content {
  font-size: 13px; color: var(--ink-500); line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.notify-time { font-size: 12px; color: var(--ink-400); margin-top: 4px; }
.notify-loading { text-align: center; padding: 12px 0; font-size: 12px; color: var(--ink-400); }
.notify-empty { text-align: center; padding: 32px 0; display: flex; flex-direction: column; align-items: center; gap: 6px; }
.notify-empty p { font-size: 14px; color: var(--ink-500); margin: 0; }
.notify-empty span { font-size: 12px; color: var(--ink-400); }
.empty-svg { opacity: .7; }

/* ============================================
   账户中心弹窗
   ============================================ */
.account-popover { padding: 4px 0; }
.acct-header {
  display: flex; align-items: center; gap: 12px;
  padding-bottom: 14px; border-bottom: 1px solid var(--clay-150); margin-bottom: 12px;
}
.acct-avatar {
  width: 48px; height: 48px; border-radius: 12px; flex-shrink: 0;
  background: linear-gradient(135deg, #D9764F, #B4522F);
  display: flex; align-items: center; justify-content: center; color: #fff;
}
.acct-name-row { display: flex; flex-direction: column; gap: 4px; }
.acct-name { font-size: 16px; font-weight: 700; color: var(--ink-900); }
.acct-details {
  display: flex; flex-direction: column; gap: 8px;
  padding-bottom: 12px; border-bottom: 1px solid var(--clay-150); margin-bottom: 12px;
}
.acct-row { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--ink-500); }
.acct-row .el-icon { color: var(--ink-400); font-size: 14px; flex-shrink: 0; }
.acct-stats { display: flex; border-radius: 10px; overflow: hidden; border: 1px solid var(--clay-200); }
.acct-stat-item { flex: 1; text-align: center; padding: 10px 0; background: var(--clay-100); border-right: 1px solid var(--clay-200); }
.acct-stat-item:last-child { border-right: none; }
.acct-stat-num { display: block; font-size: 18px; font-weight: 700; color: var(--coral); line-height: 1.2; }
.acct-stat-label { display: block; font-size: 11px; color: var(--ink-400); margin-top: 2px; }
</style>
