<template>
  <div class="dashboard">
    <!-- ===== 1. 欢迎横幅 ===== -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <h2>你好，{{ user.realName }}！</h2>
        <p>欢迎使用高校社团综合管理平台</p>
      </div>
      <div class="welcome-right">
        <span class="welcome-date">{{ today }}</span>
      </div>
    </div>

    <!-- ===== 2. 待办审批事项 ===== -->
    <div class="section-header">
      <h3 class="section-title">⏳ 待办审批事项</h3>
      <span class="section-badge">{{ pendingTotal }} 项待处理</span>
    </div>
    <div class="pending-grid" v-if="pendingItems.length > 0">
      <div v-for="item in pendingItems" :key="item.id" class="pending-card">
        <div class="pending-type">
          <span class="pending-dot" :style="{ background: item.color }"></span>
          {{ item.type }}
        </div>
        <div class="pending-info">
          <span class="pending-name">{{ item.name }}</span>
          <span class="pending-applicant">{{ item.applicant }}</span>
        </div>
        <span class="pending-time">{{ item.time }}</span>
        <div class="pending-actions">
          <el-button size="small" type="primary" @click="approve(item)" class="approve-btn">通过</el-button>
          <el-button size="small" @click="reject(item)">驳回</el-button>
        </div>
      </div>
    </div>
    <div class="empty-pending" v-else>
      <el-icon :size="32" color="#b7eb8f"><CircleCheck /></el-icon>
      <span>暂无待处理事项，太棒了！</span>
    </div>

    <!-- ===== 3. 快捷功能卡片 ===== -->
    <div class="section-header">
      <h3 class="section-title">⚡ 快捷功能</h3>
    </div>
    <div class="quick-cards">
      <div v-for="qc in quickCards" :key="qc.label" class="quick-card" @click="$router.push(qc.path)">
        <div class="quick-icon" :style="{ background: qc.bg }">
          <el-icon :size="22" :color="qc.color"><component :is="qc.icon" /></el-icon>
        </div>
        <div class="quick-text">
          <span class="quick-label">{{ qc.label }}</span>
          <span class="quick-desc">{{ qc.desc }}</span>
        </div>
      </div>
    </div>

    <!-- ===== 4. 左右两栏 ===== -->
    <div class="two-col">
      <!-- 左栏：近期活动 -->
      <div class="col-left">
        <div class="section-header">
          <h3 class="section-title">📅 近期社团活动</h3>
          <el-button type="primary" size="small" text @click="$router.push('/activities')">查看全部 →</el-button>
        </div>
        <div class="activity-list" v-if="recentActivities.length > 0">
          <div v-for="act in recentActivities" :key="act.id" class="activity-item" @click="openActivityDetail(act)">
            <div class="act-left">
              <div class="act-date-box">
                <span class="act-date-day">{{ act.day }}</span>
                <span class="act-date-month">{{ act.month }}</span>
              </div>
              <div class="act-info">
                <span class="act-title">{{ act.name }}</span>
                <span class="act-club">{{ act.clubName }} · {{ act.venue }}</span>
              </div>
            </div>
            <div class="act-right">
              <el-tag :type="act.statusType" size="small" round>{{ act.statusText }}</el-tag>
              <el-icon class="act-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
        <div class="empty-state" v-else>
          <span>本周暂无活动安排</span>
        </div>
      </div>

      <!-- 右栏：数据统计 + 迷你图表 -->
      <div class="col-right">
        <div class="section-header">
          <h3 class="section-title">📊 数据概览</h3>
        </div>
        <div class="mini-stats">
          <div class="mini-stat" v-for="s in miniStats" :key="s.label">
            <div class="mini-stat-value" :style="{ color: s.color }">{{ s.value }}</div>
            <div class="mini-stat-label">{{ s.label }}</div>
          </div>
        </div>
        <div ref="miniChart" class="mini-chart"></div>
      </div>
    </div>

    <!-- ===== 5. 平台公告区 ===== -->
    <div class="section-header">
      <h3 class="section-title">📢 平台公告</h3>
      <el-button type="primary" size="small" text @click="showAll=!showAll">
        {{ showAll ? '收起' : '查看全部' }} →
      </el-button>
    </div>
    <div class="announce-list" v-if="announcements.length > 0">
      <div v-for="(a, idx) in displayedAnnouncements" :key="a.id" class="announce-item"
           :class="{ 'announce-top': a.isTop === 1 }">
        <div class="announce-head">
          <el-tag v-if="a.isTop===1" type="danger" size="small" effect="dark" round>置顶</el-tag>
          <span class="announce-title">{{ a.title }}</span>
          <span class="announce-time">{{ a.createTime }}</span>
        </div>
        <p class="announce-body">{{ a.content }}</p>
      </div>
    </div>
    <div class="empty-state" v-else>
      <span>暂无公告</span>
    </div>

    <!-- ===== 6. 快速跳转 ===== -->
    <div class="quick-links">
      <router-link to="/clubs" class="quick-link-pill">社团管理 →</router-link>
      <router-link to="/activities" class="quick-link-pill">活动管理 →</router-link>
      <router-link to="/venues" class="quick-link-pill">场地预约 →</router-link>
      <router-link to="/admin" class="quick-link-pill">管理员审批 →</router-link>
    </div>

    <!-- ===== 7. 页脚 ===== -->
    <footer class="dash-footer">
      © 2026 高校社团管理系统 &nbsp;|&nbsp; Powered by Spring Boot &amp; Vue3
    </footer>

    <!-- ===== 活动详情弹窗 ===== -->
    <el-dialog v-model="detailVisible" title="活动详情" width="560px" destroy-on-close>
      <div class="detail-card" v-if="detailAct">
        <div class="detail-header">
          <h3 class="detail-title">{{ detailAct.name }}</h3>
          <el-tag :type="detailAct.statusType" size="small" round>{{ detailAct.statusText }}</el-tag>
        </div>
        <div class="detail-meta">
          <div class="detail-row"><span class="detail-label">所属社团</span><span>{{ detailAct.clubName }}</span></div>
          <div class="detail-row"><span class="detail-label">活动分类</span><span>{{ detailAct.category || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">活动地点</span><span>{{ detailAct.venue || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">开始时间</span><span>{{ detailAct.startTime || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">结束时间</span><span>{{ detailAct.endTime || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">报名情况</span>
            <span>{{ detailAct.enrolledCount || 0 }} / {{ detailAct.maxParticipants || '不限' }} 人</span>
          </div>
        </div>
        <div class="detail-desc" v-if="detailAct.description">
          <h4>活动简介</h4>
          <p>{{ detailAct.description }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="goToActivity(detailAct?.id)" v-if="detailAct">前往活动页</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '../api/request'

const router = useRouter()
const user = reactive(JSON.parse(localStorage.getItem('user') || '{}'))

const today = computed(() => {
  const d = new Date()
  const arr = ['日','一','二','三','四','五','六']
  return `${d.getFullYear()}年${d.getMonth()+1}月${d.getDate()}日 星期${arr[d.getDay()]}`
})

// ===== 快捷功能 =====
const quickCards = [
  { label:'社团信息管理', desc:'管理社团档案与成员', icon:'OfficeBuilding', color:'#C96442', bg:'#F6E9E2', path:'/clubs' },
  { label:'活动发布审批', desc:'创建与审核社团活动', icon:'Calendar',       color:'#52c41a', bg:'#f6ffed', path:'/activities' },
  { label:'成员招新管理', desc:'处理入社申请与审批', icon:'User',           color:'#fa8c16', bg:'#fff7e6', path:'/clubs' },
  { label:'场地物资申领', desc:'预约场地与借用物资', icon:'Location',       color:'#BE8A3C', bg:'#e6fffb', path:'/venues' },
  { label:'社团经费管理', desc:'经费申请与报销审批', icon:'Wallet',         color:'#597ef7', bg:'#f0f5ff', path:'/funds' },
  { label:'平台公告发布', desc:'发布系统通知与公告', icon:'Notification',   color:'#ff4d4f', bg:'#fff2f0', path:'/admin' },
]

// ===== 待办审批 =====
const pendingItems = ref([])
const pendingTotal = computed(() => pendingItems.value.length)

// ===== 近期活动 =====
const recentActivities = ref([])

// ===== 迷你统计 =====
const miniStats = ref([
  { label:'社团总数', value:0, color:'#C96442' },
  { label:'活动总数', value:0, color:'#52c41a' },
  { label:'用户总量', value:0, color:'#fa8c16' },
])

// ===== 公告 =====
const announcements = ref([])
const showAll = ref(false)
const displayedAnnouncements = computed(() =>
  showAll.value ? announcements.value : announcements.value.slice(0, 3)
)

// ===== 活动详情弹窗 =====
const detailVisible = ref(false)
const detailAct = ref(null)

const openActivityDetail = (act) => {
  detailAct.value = act
  detailVisible.value = true
}

const goToActivity = (id) => {
  detailVisible.value = false
  router.push('/activities')
}

const miniChart = ref(null)

onMounted(async () => {
  try {
    const res = await request.get('/dashboard')
    const d = res.data

    miniStats.value[0].value = d.clubCount || 0
    miniStats.value[1].value = d.activityCount || 0
    miniStats.value[2].value = d.userCount || 0

    // 真实待办项（从后端按角色过滤）
    pendingItems.value = d.pendingItems || []
    if (d.pendingApprovals > 0 && pendingItems.value.length === 0) {
      // 有数量但没明细时显示汇总（兼容旧版）
      pendingItems.value.push({
        id: 0, type:'系统通知', name:'有 ' + d.pendingApprovals + ' 项待审批事项',
        applicant:'请前往管理员审批页面处理', time:'', color:'#fa8c16'
      })
    }

    await nextTick()
    renderMiniChart(d.clubRank || [])
  } catch(e) { console.error(e) }

  // 公告
  try {
    const a = await request.get('/announcements')
    if (a.code === 200) {
      const list = a.data || []
      list.sort((x,y) => {
        if (x.isTop !== y.isTop) return (y.isTop||0) - (x.isTop||0)
        return (y.createTime||'').localeCompare(x.createTime||'')
      })
      announcements.value = list
    }
  } catch(e) { console.error(e) }

  // 活动列表
  try {
    const act = await request.get('/activities', { params: { page:1, size:5 } })
    if (act.code === 200 && act.data?.records) {
      recentActivities.value = act.data.records.slice(0, 5).map(a => {
        const d = new Date(a.startTime || Date.now())
        const statusMap = { 'DRAFT':['info','草稿'], 'PENDING':['warning','待审批'], 'APPROVED':['success','报名中'], 'ONGOING':['success','进行中'], 'REJECTED':['danger','已驳回'], 'FINISHED':['info','已结束'] }
        const [statusType, statusText] = statusMap[a.status] || ['info',a.status]
        return {
          id: a.id, name: a.title, clubName: a.clubName || '', venue: a.location || a.venue || '',
          day: d.getDate(), month: (d.getMonth()+1)+'月',
          startTime: a.startTime, endTime: a.endTime,
          enrolledCount: a.enrolledCount, maxParticipants: a.maxParticipants,
          category: a.category, description: a.description,
          status: a.status, statusType, statusText,
        }
      })
    }
  } catch(e) { console.error(e) }
})

const renderMiniChart = (rank) => {
  if (!miniChart.value) return
  const c = echarts.init(miniChart.value)
  c.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 8, top: 8, bottom: 0, containLabel: true },
    xAxis: {
      type: 'category',
      data: rank.slice(0, 6).map(r => r.name?.length > 3 ? r.name.slice(0,3)+'…' : r.name),
      axisLabel: { fontSize: 10, color: '#8c8c8c' },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f0f0', type:'dashed' } },
      axisLabel: { fontSize: 10, color: '#8c8c8c' },
    },
    series: [{
      type: 'bar',
      barWidth: 20,
      data: rank.slice(0, 6).map(r => r.activityCount || 0),
      itemStyle: {
        borderRadius: [6,6,0,0],
        color: new echarts.graphic.LinearGradient(0,0,0,1,[
          { offset:0, color:'#E0A08A' },{ offset:1, color:'#C96442' }
        ])
      }
    }]
  })
}

const approve = async (item) => {
  if (item.id === 0 || !item.appType) {
    router.push('/admin')
    return
  }
  try {
    const apiMap = {
      'CLUB':     { url: '/club/approve',          body: { id: item.id, status: 1 } },
      'ACTIVITY': { url: '/activity/approve',       body: { id: item.id, action: 'approve' } },
      'VENUE':    { url: '/venue/booking/approve',   body: { id: item.id, approve: true } },
      'RESOURCE': { url: '/resource/borrow/approve', body: { id: item.id, approve: true } },
      'FUND':     { url: '/fund/approve',            body: { id: item.id, approve: true } },
    }
    const cfg = apiMap[item.appType]
    if (!cfg) return ElMessage.warning('不支持的审批类型')
    await request.put(cfg.url, cfg.body)
    ElMessage.success(`已通过: ${item.name}`)
    pendingItems.value = pendingItems.value.filter(i => i.id !== item.id)
  } catch(e) { ElMessage.error('操作失败: ' + (e.response?.data?.msg || e.message)) }
}
const reject = async (item) => {
  if (item.id === 0 || !item.appType) {
    router.push('/admin')
    return
  }
  try {
    const apiMap = {
      'CLUB':     { url: '/club/approve',          body: { id: item.id, status: 2 } },
      'ACTIVITY': { url: '/activity/approve',       body: { id: item.id, action: 'reject' } },
      'VENUE':    { url: '/venue/booking/approve',   body: { id: item.id, approve: false } },
      'RESOURCE': { url: '/resource/borrow/approve', body: { id: item.id, approve: false } },
      'FUND':     { url: '/fund/approve',            body: { id: item.id, approve: false } },
    }
    const cfg = apiMap[item.appType]
    if (!cfg) return ElMessage.warning('不支持的审批类型')
    await request.put(cfg.url, cfg.body)
    ElMessage.info(`已驳回: ${item.name}`)
    pendingItems.value = pendingItems.value.filter(i => i.id !== item.id)
  } catch(e) { ElMessage.error('操作失败: ' + (e.response?.data?.msg || e.message)) }
}
</script>

<style scoped>
.dashboard { max-width: 1200px; margin: 0 auto; }

/* ============================================
   1. 欢迎横幅
   ============================================ */
.welcome-banner {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #C96442 0%, #BE8A3C 100%);
  border-radius: 8px; padding: 22px 28px; margin-bottom: 24px; color: #fff;
}
.welcome-left h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; }
.welcome-left p { margin: 0; font-size: 13px; opacity: 0.85; }
.welcome-date { font-size: 13px; opacity: 0.8; white-space: nowrap; }

/* ============================================
   区块标题
   ============================================ */
.section-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px;
}
.section-title { margin: 0; font-size: 15px; font-weight: 700; color: #26241F; }
.section-badge { font-size: 12px; color: #fa8c16; background: #fff7e6; padding: 2px 10px; border-radius: 10px; font-weight: 500; }

/* ============================================
   2. 待办审批
   ============================================ */
.pending-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 12px; margin-bottom: 28px;
}
.pending-card {
  display: flex; align-items: center; gap: 12px;
  background: #fff; border-radius: 8px; padding: 14px 18px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  transition: all 0.2s;
}
.pending-card:hover { box-shadow: 0 3px 12px rgba(0,0,0,0.06); transform: translateY(-1px); }
.pending-dot { width: 7px; height: 7px; border-radius: 50%; display: inline-block; margin-right: 4px; }
.pending-type { font-size: 11px; color: #8c8c8c; white-space: nowrap; min-width: 60px; display: flex; align-items: center; }
.pending-info { flex: 1; min-width: 0; }
.pending-name { font-size: 13px; font-weight: 600; color: #262626; display: block; }
.pending-applicant { font-size: 11px; color: #8c8c8c; }
.pending-time { font-size: 11px; color: #bfbfbf; white-space: nowrap; }
.pending-actions { display: flex; gap: 6px; flex-shrink: 0; }
.pending-actions .el-button { padding: 4px 12px; font-size: 12px; }
.approve-btn { background: #52c41a !important; border-color: #52c41a !important; }
.approve-btn:hover { background: #73d13d !important; }
.empty-pending {
  display: flex; align-items: center; gap: 10px; background: #fff;
  border-radius: 8px; padding: 20px 24px; margin-bottom: 28px;
  color: #52c41a; font-size: 13px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

/* ============================================
   3. 快捷功能卡片
   ============================================ */
.quick-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px; margin-bottom: 28px;
}
.quick-card {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border-radius: 8px; padding: 20px 18px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  cursor: pointer; transition: all 0.2s;
}
.quick-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
.quick-icon {
  width: 48px; height: 48px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.quick-label { font-size: 13px; font-weight: 600; color: #262626; display: block; }
.quick-desc { font-size: 11px; color: #8c8c8c; display: block; margin-top: 2px; }

/* ============================================
   4. 两栏布局
   ============================================ */
.two-col { display: flex; gap: 16px; margin-bottom: 28px; }
.col-left  { flex: 1; min-width: 0; }
.col-right { flex: 1; min-width: 0; }

/* 活动列表 */
.activity-list { display: flex; flex-direction: column; gap: 8px; }
.activity-item {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-radius: 8px; padding: 12px 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  transition: all 0.2s;
}
.activity-item:hover { box-shadow: 0 3px 12px rgba(0,0,0,0.06); }
.act-left { display: flex; align-items: center; gap: 12px; }
.act-date-box {
  width: 44px; height: 44px; border-radius: 8px;
  background: #F6E9E2; display: flex; flex-direction: column;
  align-items: center; justify-content: center; flex-shrink: 0;
}
.act-date-day { font-size: 16px; font-weight: 700; color: #C96442; line-height: 1; }
.act-date-month { font-size: 10px; color: #8c8c8c; }
.activity-item { cursor: pointer; }
.act-title { font-size: 13px; font-weight: 600; color: #262626; display: block; }
.act-club { font-size: 11px; color: #8c8c8c; }
.act-right { display: flex; align-items: center; gap: 8px; }
.act-arrow { color: #bfbfbf; font-size: 14px; transition: all 0.2s; }
.activity-item:hover .act-arrow { color: #C96442; transform: translateX(3px); }

/* 迷你统计 */
.mini-stats { display: flex; gap: 10px; margin-bottom: 12px; }
.mini-stat {
  flex: 1; background: #fff; border-radius: 8px; padding: 14px 12px;
  text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.mini-stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.mini-stat-label { font-size: 11px; color: #8c8c8c; margin-top: 2px; }
.mini-chart { background: #fff; border-radius: 8px; padding: 8px; height: 260px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }

/* ============================================
   5. 公告
   ============================================ */
.announce-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 28px; max-height: 400px; overflow-y: auto; }
.announce-item {
  background: #fff; border-radius: 8px; padding: 14px 18px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.announce-item.announce-top { border-left: 3px solid #ff4d4f; background: #fffbfb; }
.announce-head { display: flex; align-items: center; gap: 8px; }
.announce-title { font-size: 14px; font-weight: 600; color: #262626; flex: 1; }
.announce-time { font-size: 11px; color: #bfbfbf; white-space: nowrap; }
.announce-body { font-size: 12px; color: #8c8c8c; margin: 6px 0 0; line-height: 1.6; }

/* 空状态 */
.empty-state { text-align: center; padding: 24px; color: #bfbfbf; font-size: 13px; background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }

/* ============================================
   6. 快速跳转
   ============================================ */
.quick-links { display: flex; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; }
.quick-link-pill { font-size: 12px; color: #C96442; background: #F6E9E2; padding: 4px 12px; border-radius: 12px; text-decoration: none; transition: all 0.2s; }
.quick-link-pill:hover { background: #bae7ff; transform: translateY(-1px); }

/* ============================================
   7. 页脚
   ============================================ */
.dash-footer { text-align: center; padding: 20px 0 8px; font-size: 12px; color: #bfbfbf; letter-spacing: 0.03em; }

/* 响应式 */
@media (max-width: 768px) {
  .two-col { flex-direction: column; }
  .col-right { width: 100%; flex: none; }
  .pending-grid { grid-template-columns: 1fr; }
  .quick-cards { grid-template-columns: repeat(2, 1fr); }
  .welcome-banner { flex-direction: column; align-items: flex-start; gap: 6px; }
  .mini-chart { height: 220px; }
}

/* 活动详情弹窗 */
.detail-header { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; }
.detail-title { font-size: 18px; font-weight: 700; color: #222; margin: 0; }
.detail-meta { background: #fafafa; border-radius: 8px; padding: 16px; }
.detail-row {
  display: flex; padding: 8px 0; font-size: 14px; color: #333;
  border-bottom: 1px solid #f0f0f0;
}
.detail-row:last-child { border-bottom: none; }
.detail-label { width: 90px; color: #8c8c8c; flex-shrink: 0; }
.detail-desc { margin-top: 18px; }
.detail-desc h4 { font-size: 14px; font-weight: 600; color: #333; margin: 0 0 8px; }
.detail-desc p { font-size: 13px; color: #595959; line-height: 1.7; margin: 0; }
</style>
