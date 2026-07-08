<template>
  <div class="layout-container">
    <!-- ===== 顶部横向导航栏 ===== -->
    <header class="top-nav">
      <div class="nav-inner">
        <!-- Logo -->
        <div class="nav-logo" @click="$router.push('/')">
          <svg viewBox="0 0 32 32" fill="none" width="26" height="26">
            <circle cx="14" cy="12" r="5" fill="#fff" opacity="0.95"/>
            <path d="M8 24c0-5 6-8 6-8s6 3 6 8v3H8v-3z" fill="#fff" opacity="0.9"/>
            <circle cx="22" cy="12" r="4.5" fill="#fff" opacity="0.95"/>
            <path d="M18 24c0-4.5 5-7 5-7s5 2.5 5 7v3H18v-3z" fill="#fff" opacity="0.9"/>
          </svg>
          <span class="nav-logo-text">高校社团管理</span>
        </div>

        <!-- 菜单 -->
        <nav class="nav-menu">
          <router-link v-for="m in menus" :key="m.path" :to="m.path" class="nav-item" :class="{ active: isActive(m.path) }">
            <el-icon class="nav-icon"><component :is="m.icon" /></el-icon>
            <span>{{ m.label }}</span>
          </router-link>
        </nav>

        <!-- 右侧 -->
        <div class="nav-right">
          <!-- 通知铃铛 -->
          <el-popover placement="bottom-end" :width="400" trigger="click" @show="onPopoverShow" @hide="onPopoverHide">
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notice-badge">
                <el-button text class="notice-btn">
                  <el-icon :size="20"><Bell /></el-icon>
                </el-button>
              </el-badge>
            </template>

            <div class="notify-panel">
              <!-- 头部 + 分类标签 -->
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

              <!-- 列表（滚动分页） -->
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

              <!-- 空状态 -->
              <div class="notify-empty" v-else>
                <svg viewBox="0 0 120 120" width="80" height="80" class="empty-svg">
                  <circle cx="60" cy="45" r="20" fill="#f0f0f0"/>
                  <rect x="30" y="75" width="60" height="8" rx="4" fill="#f0f0f0"/>
                  <rect x="40" y="90" width="40" height="8" rx="4" fill="#f5f5f5"/>
                  <circle cx="55" cy="40" r="2" fill="#d9d9d9"/>
                  <circle cx="65" cy="40" r="2" fill="#d9d9d9"/>
                  <path d="M52 50 Q60 56 68 50" stroke="#d9d9d9" fill="none" stroke-width="1.5"/>
                </svg>
                <p>暂无消息通知</p>
                <span>审批结果、新公告等消息会显示在这里</span>
              </div>
            </div>
          </el-popover>

          <!-- 账户中心悬停弹窗 -->
          <el-popover placement="bottom-end" :width="280" trigger="hover" :show-after="300" @show="fetchAccountInfo">
            <template #reference>
              <div class="nav-user-area">
                <span class="nav-user">
                  <el-icon><User /></el-icon>
                  {{ user.realName || '管理员' }}
                </span>
                <!-- 调试：直接在顶栏显示手机号和学院 -->
                <span style="color:rgba(255,255,255,0.7);font-size:11px;margin-left:-4px">{{ user.phone || '无手机号' }}</span>
                <el-tag :type="roleTypeMap[user.role]" effect="plain" size="small" round>
                  {{ roleMap[user.role] || user.role }}
                </el-tag>
              </div>
            </template>

            <div class="account-popover">
              <!-- 头像 + 姓名 -->
              <div class="acct-header">
                <div class="acct-avatar">
                  <el-icon :size="28"><UserFilled /></el-icon>
                </div>
                <div class="acct-name-row">
                  <span class="acct-name">{{ user.realName || '管理员' }}</span>
                  <el-tag :type="roleTypeMap[user.role]" size="small" round>
                    {{ roleMap[user.role] || user.role }}
                  </el-tag>
                </div>
              </div>

              <!-- 详细信息 -->
              <div class="acct-details">
                <div class="acct-row">
                  <el-icon><User /></el-icon>
                  <span>用户名：{{ user.username || '-' }}</span>
                </div>
                <div class="acct-row">
                  <el-icon><Phone /></el-icon>
                  <span>{{ user.phone || userInfo.phone || '未绑定手机号' }}</span>
                </div>
                <div class="acct-row">
                  <el-icon><School /></el-icon>
                  <span>{{ user.college || userInfo.college || '未设置学院' }}</span>
                </div>
                <div class="acct-row">
                  <el-icon><Clock /></el-icon>
                  <span>注册时间：{{ formatDate(user.createTime || userInfo.createTime) }}</span>
                </div>
              </div>

              <!-- 统计卡片 -->
              <div class="acct-stats">
                <div class="acct-stat-item">
                  <span class="acct-stat-num">{{ stats.clubCount ?? '-' }}</span>
                  <span class="acct-stat-label">我的社团</span>
                </div>
                <div class="acct-stat-item">
                  <span class="acct-stat-num">{{ stats.activityCount ?? '-' }}</span>
                  <span class="acct-stat-label">参与活动</span>
                </div>
                <div class="acct-stat-item">
                  <span class="acct-stat-num">{{ stats.borrowCount ?? '-' }}</span>
                  <span class="acct-stat-label">物资借用</span>
                </div>
                <div class="acct-stat-item">
                  <span class="acct-stat-num">{{ stats.bookingCount ?? '-' }}</span>
                  <span class="acct-stat-label">场地预约</span>
                </div>
              </div>
            </div>
          </el-popover>

          <el-button type="danger" text @click="logout" class="logout-btn">
            <el-icon><SwitchButton /></el-icon> 退出
          </el-button>
        </div>
      </div>
    </header>

    <!-- ===== 主内容区 ===== -->
    <main class="main-content">
      <router-view />
      <footer class="global-footer">© 2026 高校社团综合管理系统 版权所有</footer>
    </main>
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

const allMenus = [
  { path: '/',              label: '首页看板',  icon: 'DataAnalysis', roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/clubs',         label: '社团管理',  icon: 'OfficeBuilding', roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/activities',    label: '活动管理',  icon: 'Calendar',       roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/venues',        label: '场地预约',  icon: 'Location',       roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/resources',     label: '物资管理',  icon: 'Box',            roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/funds',         label: '经费管理',  icon: 'Wallet',         roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/ai-plan',       label: 'AI智能策划', icon: 'MagicStick',    roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/notify-prefs',  label: '通知偏好',  icon: 'Bell',           roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/my-applications',label: '我的全部申请',icon: 'Document',     roles: ['ADMIN','PRESIDENT','TEACHER','STUDENT'] },
  { path: '/admin',         label: '管理员审批',  icon: 'Setting',        roles: ['ADMIN'] },
]
const menus = computed(() => allMenus.filter(m => m.roles.includes(user.role || 'STUDENT')))

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
let visiblePollPaused = false

const filterTabs = [
  { label: '全部', value: 'ALL' },
  { label: '审批', value: 'APPROVAL' },
  { label: '活动', value: 'ACTIVITY' },
  { label: '公告', value: 'ANNOUNCEMENT' },
]

// 业务类型→路由映射
const businessRouteMap = {
  CLUB_APPROVAL: '/clubs',
  MEMBER_APPROVAL: '/clubs',
  JOIN_REQUEST: '/clubs',
  ACTIVITY_APPROVAL: '/activities',
  ACTIVITY_PENDING: '/activities',
  VENUE_APPROVAL: '/venues',
  VENUE_PENDING: '/venues',
  RESOURCE_APPROVAL: '/resources',
  RESOURCE_PENDING: '/resources',
  FUND_APPROVAL: '/funds',
  FUND_PENDING: '/funds',
  ANNOUNCEMENT: '/',
  SYSTEM: '/',
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
    if (reset) {
      notifications.value = records
    } else {
      notifications.value = [...notifications.value, ...records]
    }
    hasMore.value = records.length >= 20
  } catch (e) {
    /* ignore */
  } finally {
    loadingMore.value = false
  }
}

// 滚动加载更多
const onScroll = () => {
  const el = notifyListRef.value
  if (!el || loadingMore.value || !hasMore.value) return
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 60) {
    currentPage.value++
    loadNotifications(false)
  }
}

const switchFilter = (val) => {
  filterType.value = val
  loadNotifications(true)
}

const onPopoverShow = () => {
  visiblePollPaused = true
  loadNotifications(true)
}
const onPopoverHide = () => {
  visiblePollPaused = false
}

const handleNotifyClick = async (n) => {
  if (n.isRevoked === 1) return
  // 标记已读
  if (n.isRead === 0) {
    try {
      await request.put(`/notification/read/${n.id}`)
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      // 跨标签同步
      try { new BroadcastChannel('notify-sync').postMessage({ type: 'read', id: n.id }) } catch (e) { /* */ }
    } catch (e) { /* ignore */ }
  }
  // 跳转业务页面
  const targetPath = businessRouteMap[n.businessType] || '/'
  router.push(targetPath)
  // 关闭popover（点击body空白处自然会关）
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
  } catch (e) { /* cancelled or error */ }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  const month = d.getMonth() + 1
  const day = d.getDate()
  return month + '/' + day + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 页面可见性控制轮询
const handleVisibility = () => {
  if (document.hidden) {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  } else {
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30000)
  }
}

// 跨标签同步
const setupBroadcast = () => {
  try {
    const bc = new BroadcastChannel('notify-sync')
    bc.onmessage = (e) => {
      if (e.data.type === 'read' || e.data.type === 'markAll') {
        fetchUnreadCount()
      }
    }
  } catch (e) { /* Browser doesn't support BroadcastChannel */ }
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
.layout-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #F6F8FA;
}

/* ============================================
   顶部导航栏 — 浅蓝青主色
   ============================================ */
.top-nav {
  background: linear-gradient(135deg, #1890ff 0%, #2aadd4 60%, #36cfc9 100%);
  flex-shrink: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(24, 144, 255, 0.15);
}
.nav-inner {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}
.nav-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
  margin-right: 20px;
}
.nav-logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
  letter-spacing: 0.03em;
}

/* 菜单 */
.nav-menu {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
  overflow-x: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 14px 5px;
  border-radius: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  white-space: nowrap;
  transition: all 0.2s;
  font-weight: 500;
  border-bottom: 3px solid transparent;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}
.nav-item.active {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-weight: 700;
  border-bottom-color: #fff;
}
.nav-icon { font-size: 16px; }

/* 右侧 */
.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  margin-left: 16px;
}
.notice-btn {
  color: rgba(255, 255, 255, 0.9) !important;
  padding: 4px;
}
.notice-btn:hover {
  color: #fff !important;
}
.notice-badge :deep(.el-badge__content) {
  border: 2px solid #1890ff;
}
.nav-user {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}
.nav-user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}
.nav-user-area:hover {
  background: rgba(255, 255, 255, 0.15);
}
.logout-btn {
  color: rgba(255, 255, 255, 0.85) !important;
  font-size: 13px;
}
.logout-btn:hover {
  color: #fff !important;
}

/* ============================================
   主内容
   ============================================ */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}
.global-footer {
  text-align: center;
  padding: 24px 0 12px;
  font-size: 12px;
  color: #bfbfbf;
  letter-spacing: 0.03em;
}

/* 响应式 */
@media (max-width: 900px) {
  .nav-inner {
    padding: 0 12px;
  }
  .nav-menu {
    gap: 0;
  }
  .nav-item {
    padding: 6px 8px;
    font-size: 12px;
    gap: 3px;
  }
  .nav-item span { display: none; }
  .nav-logo-text { display: none; }
  .main-content {
    padding: 12px 10px;
  }
}

/* ============================================
   通知面板
   ============================================ */
.notify-panel { max-height: 480px; display: flex; flex-direction: column; }
.notify-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 10px; border-bottom: 1px solid #f0f0f0;
  font-weight: 600; font-size: 15px;
}
.notify-header-actions { display: flex; gap: 4px; }

/* 分类标签 */
.notify-tabs {
  display: flex; gap: 6px; padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.notify-tab {
  padding: 4px 12px; border-radius: 14px; font-size: 12px;
  cursor: pointer; color: #909399; background: #f5f5f5;
  transition: all 0.2s; white-space: nowrap;
}
.notify-tab:hover { color: #1890ff; background: #e6f7ff; }
.notify-tab.active { color: #fff; background: #1890ff; }

/* 列表 */
.notify-list { flex: 1; overflow-y: auto; max-height: 360px; }
.notify-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 6px; cursor: pointer; border-radius: 6px;
  transition: background 0.15s; border-bottom: 1px solid #fafafa;
}
.notify-item:hover { background: #f0f7ff; }
.notify-item.unread { background: #f0f7ff; font-weight: 500; }
.notify-item.unread .notify-title { color: #1890ff; }
.notify-item.revoked { opacity: 0.5; cursor: default; }
.notify-item.revoked:hover { background: transparent; }
.notify-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #1890ff; flex-shrink: 0; margin-top: 6px;
}
.notify-body { flex: 1; min-width: 0; }
.notify-title {
  font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 3px;
  display: flex; align-items: center; gap: 6px;
}
.revoked-tag { font-size: 11px; }
.notify-content {
  font-size: 13px; color: #606266; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2;
  -webkit-box-orient: vertical; overflow: hidden;
}
.notify-time { font-size: 12px; color: #bbb; margin-top: 4px; }
.notify-loading {
  text-align: center; padding: 12px 0; font-size: 12px; color: #bbb;
}

/* 空状态 */
.notify-empty {
  text-align: center; padding: 32px 0; color: #bbb;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
}
.notify-empty p { font-size: 14px; color: #999; margin: 0; }
.notify-empty span { font-size: 12px; color: #ccc; }
.empty-svg { opacity: 0.5; }

/* ============================================
   账户中心弹窗
   ============================================ */
.account-popover {
  padding: 4px 0;
}
.acct-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}
.acct-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff, #36cfc9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.acct-name-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.acct-name {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}
.acct-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 12px;
}
.acct-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}
.acct-row .el-icon {
  color: #909399;
  font-size: 14px;
  flex-shrink: 0;
}
.acct-stats {
  display: flex;
  gap: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}
.acct-stat-item {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
}
.acct-stat-item:last-child {
  border-right: none;
}
.acct-stat-num {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #1890ff;
  line-height: 1.2;
}
.acct-stat-label {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* 暗黑模式适配 */
@media (prefers-color-scheme: dark) {
  .notify-panel { background: #1d1e1f; }
  .notify-header { color: #e5e5e5; border-bottom-color: #333; }
  .notify-tabs { border-bottom-color: #333; }
  .notify-tab { background: #333; color: #aaa; }
  .notify-tab.active { background: #1890ff; color: #fff; }
  .notify-item { border-bottom-color: #2a2a2a; }
  .notify-item:hover { background: #252525; }
  .notify-item.unread { background: #1a2a3a; }
  .notify-title { color: #e5e5e5; }
  .notify-content { color: #b0b0b0; }
  .notify-empty p { color: #888; }
  /* popover */
  .acct-header { border-bottom-color: #333; }
  .acct-name { color: #e5e5e5; }
  .acct-row { color: #b0b0b0; }
  .acct-details { border-bottom-color: #2a2a2a; }
  .acct-stats { border-color: #333; }
  .acct-stat-item { background: #1d1d1d; border-right-color: #333; }
  .acct-stat-num { color: #5cb8ff; }
  .acct-stat-label { color: #888; }
}
</style>
