<template>
  <div class="activity-page">
    <!-- 页头 -->
    <div class="page-header">
      <h2 class="page-title">活动管理</h2>
      <el-button type="primary" @click="activityWizardRef.open()" v-if="canCreate">发布新活动</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-card--total">
        <div class="stat-card__icon"><el-icon><DataAnalysis /></el-icon></div>
        <span class="stat-num">{{ total }}</span><span class="stat-lbl">活动总数</span>
      </div>
      <div class="stat-card stat-card--ongoing">
        <div class="stat-card__icon"><el-icon><VideoPlay /></el-icon></div>
        <span class="stat-num">{{ ongoingCount }}</span><span class="stat-lbl">进行中</span>
      </div>
      <div class="stat-card stat-card--pending">
        <div class="stat-card__icon"><el-icon><Clock /></el-icon></div>
        <span class="stat-num">{{ pendingCount }}</span><span class="stat-lbl">待审批</span>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <div class="filter-row">
      <el-input v-model="search" placeholder="搜索活动" style="width:240px" clearable class="filter-input"/>
      <el-select v-model="statusFilter" placeholder="状态筛选" style="width:150px" clearable class="filter-select">
        <el-option label="全部" value=""/><el-option label="待审批" value="PENDING"/>
        <el-option label="报名中" value="APPROVED"/><el-option label="进行中" value="ONGOING"/>
      </el-select>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" empty-text="暂无活动数据" row-class-name="activity-row"
              :header-cell-style="{background:'#fafafa',color:'#333',fontWeight:'600',fontSize:'13px'}">
      <el-table-column prop="title" label="活动名称" min-width="180"/>
      <el-table-column prop="category" label="分类" width="90"/>
      <el-table-column prop="location" label="地点" width="130"/>
      <el-table-column prop="startTime" label="开始时间" width="160"/>
      <el-table-column label="报名" width="110">
        <template #default="{row}">{{row.enrolledCount}}/{{row.maxParticipants||'不限'}}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <span class="status-tag" :class="'status-tag--' + row.status">{{statusLabel(row.status)}}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{row}">
          <div class="action-cell">
            <el-button class="btn-detail" @click="openDetail(row)">详情</el-button>
            <el-button class="btn-enroll" @click="openEnroll(row)"
                       v-if="row.status==='APPROVED'&&user.role==='STUDENT'">报名参加</el-button>
            <el-button class="btn-approve" @click="approve(row,'approve')" v-if="row.status==='PENDING'&&user.role==='ADMIN'">通过</el-button>
            <el-button class="btn-reject" @click="approve(row,'reject')" v-if="row.status==='PENDING'&&user.role==='ADMIN'">驳回</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="prev,next,total"/>

    <!-- 新建活动对话框 -->
    <el-dialog v-model="dialogVisible" title="发布新活动" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="活动标题"><el-input v-model="form.title"/></el-form-item>
        <el-form-item label="分类"><el-select v-model="form.category"><el-option v-for="c in cats" :key="c" :label="c" :value="c"/></el-select></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime"/></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime"/></el-form-item>
        <el-form-item label="地点"><el-input v-model="form.location"/></el-form-item>
        <el-form-item label="人数上限"><el-input-number v-model="form.maxParticipants" :min="0"/></el-form-item>
        <el-form-item label="所属社团"><el-select v-model="form.clubId" placeholder="请选择社团"><el-option v-for="c in myClubs" :key="c.id" :label="c.name" :value="c.id"/></el-select></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="submit">提交</el-button></template>
    </el-dialog>

    <!-- 公告建议对话框 -->
    <el-dialog v-model="annSuggestVisible" title="活动已通过" width="400px">
      <p>活动「{{ lastApprovedActivity }}」已审批通过！</p>
      <p style="color:#666">是否现在发布一则公告通知全体成员？</p>
      <template #footer>
        <el-button @click="annSuggestVisible=false">稍后</el-button>
        <el-button type="primary" @click="gotoAnnouncement">去发布公告</el-button>
      </template>
    </el-dialog>

    <!-- 报名对话框 -->
    <el-dialog v-model="enrollVisible" title="活动报名" width="480px" destroy-on-close>
      <div class="enroll-activity-info">
        <div class="enroll-activity-title">{{ enrollTarget?.title }}</div>
        <div class="enroll-activity-meta">
          <div class="meta-item"><el-icon><Calendar /></el-icon> {{ enrollTarget?.startTime }}</div>
          <div class="meta-item"><el-icon><Location /></el-icon> {{ enrollTarget?.location }}</div>
          <div class="meta-item"><el-icon><User /></el-icon> 已报名 {{ enrollTarget?.enrolledCount || 0 }}/{{ enrollTarget?.maxParticipants || '不限' }} 人</div>
        </div>
      </div>
      <el-form :model="enrollForm" :rules="enrollRules" ref="enrollFormRef" label-width="80px" class="enroll-form">
        <el-form-item label="姓名"><el-input :model-value="user.realName" disabled/></el-form-item>
        <el-form-item label="学号/工号" prop="studentId"><el-input v-model="enrollForm.studentId" placeholder="请输入学号"/></el-form-item>
        <el-form-item label="学院"><el-input :model-value="user.college" disabled/></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="enrollForm.phone" placeholder="请输入手机号"/></el-form-item>
        <el-form-item label="报名理由" prop="reason"><el-input v-model="enrollForm.reason" type="textarea" :rows="3" placeholder="你为什么想参加这个活动？（选填）"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="enrollVisible = false">取消</el-button><el-button type="primary" @click="doEnroll" :loading="enrolling">确认报名</el-button></template>
    </el-dialog>

    <!-- 活动详情对话框 -->
    <el-dialog v-model="detailVisible" title="活动详情" width="560px" destroy-on-close>
      <div class="detail-card" v-if="detailAct">
        <div class="detail-header">
          <h3 class="detail-title">{{ detailAct.title }}</h3>
          <el-tag :type="statusTag(detailAct.status)" size="small" round>{{ statusLabel(detailAct.status) }}</el-tag>
        </div>
        <div class="detail-meta">
          <div class="detail-row"><span class="detail-label">所属社团</span><span>{{ detailAct.clubName || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">活动分类</span><span>{{ detailAct.category || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">活动地点</span><span>{{ detailAct.location || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">开始时间</span><span>{{ detailAct.startTime || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">结束时间</span><span>{{ detailAct.endTime || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">报名情况</span>
            <span>{{ detailAct.enrolledCount || 0 }} / {{ detailAct.maxParticipants || '不限' }} 人</span>
          </div>
          <div class="detail-row" v-if="detailAct.status==='APPROVED'||detailAct.status==='ONGOING'">
            <span class="detail-label">签到码</span>
            <span style="font-family:monospace;font-weight:700;color:#C96442">{{ detailAct.checkinCode || '暂无' }}</span>
          </div>
        </div>
        <div class="detail-desc" v-if="detailAct.description">
          <h4>活动简介</h4>
          <p>{{ detailAct.description }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailAct?.status==='APPROVED'&&user.role==='STUDENT'" type="primary" @click="detailToEnroll">
          立即报名
        </el-button>
      </template>
    </el-dialog>
    <ActivityCreateWizard ref="activityWizardRef" @refresh="fetch" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '../api/request'
import ActivityCreateWizard from './ActivityCreateWizard.vue'

const router = useRouter()
const activityWizardRef = ref(null)
const list = ref([]); const loading = ref(false); const page = ref(1); const total = ref(0)
const search = ref(''); const statusFilter = ref(''); const dialogVisible = ref(false)
const annSuggestVisible = ref(false); const lastApprovedActivity = ref('')
const cats = ['讲座','比赛','演出','志愿','团建','其他']
const myClubs = ref([])
const user = JSON.parse(localStorage.getItem('user')||'{}')
const canCreate = computed(() => user.role === 'ADMIN' || user.role === 'PRESIDENT')
const form = reactive({title:'',category:'讲座',startTime:'',endTime:'',location:'',maxParticipants:0,description:'',clubId:null})

const enrollVisible = ref(false); const enrolling = ref(false); const enrollTarget = ref(null)
const enrollFormRef = ref(null)
const enrollForm = reactive({ studentId: '', phone: '', reason: '' })
const enrollRules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const ongoingCount = computed(() => list.value.filter(a => a.status === 'ONGOING').length)
const pendingCount = computed(() => list.value.filter(a => a.status === 'PENDING').length)

// 活动详情弹窗
const detailVisible = ref(false)
const detailAct = ref(null)
const openDetail = (row) => { detailAct.value = row; detailVisible.value = true }
const detailToEnroll = () => { detailVisible.value = false; setTimeout(() => openEnroll(detailAct.value), 200) }

const fetchClubs = async () => {
  try {
    if (user.role === 'ADMIN') {
      const r = await request.get('/clubs', { params: { page:1, size:100 } })
      if (r.code===200) myClubs.value = r.data.records || []
    } else {
      const r = await request.get('/my-clubs')
      if (r.code===200) myClubs.value = r.data || []
    }
    if (myClubs.value.length > 0 && !form.clubId) form.clubId = myClubs.value[0].id
  } catch(e) { /* ignore */ }
}

const fetch = async () => {
  loading.value = true
  const res = await request.get('/activities', { params: { page: page.value, size: 10, status: statusFilter.value } })
  if (res.code === 200) { list.value = res.data.records; total.value = res.data.total }
  loading.value = false
}

const openCreate = () => { fetchClubs(); dialogVisible.value = true }

const submit = async () => {
  if (!form.clubId) { ElMessage.warning('请选择所属社团'); return }
  const res = await request.post('/activity', form)
  ElMessage.success(res.msg || '操作成功'); dialogVisible.value = false; fetch()
}

const openEnroll = (row) => { enrollTarget.value = row; enrollForm.studentId = ''; enrollForm.phone = ''; enrollForm.reason = ''; enrollVisible.value = true }

const doEnroll = async () => {
  const valid = await enrollFormRef.value.validate().catch(() => false)
  if (!valid) return
  enrolling.value = true
  try {
    await request.post('/activity/enroll', { activityId: enrollTarget.value.id })
    ElMessage.success('报名成功！请准时参加活动 🎉')
    enrollVisible.value = false; fetch()
  } finally { enrolling.value = false }
}

const approve = async (row, action) => {
  const res = await request.put('/activity/approve', { id: row.id, action })
  if (action === 'approve' && res.data?.suggestAnnounce) {
    lastApprovedActivity.value = res.data.activityName || row.title
    annSuggestVisible.value = true
  }
  ElMessage.success(action==='approve'?'审批通过':'已驳回'); fetch()
}

const gotoAnnouncement = () => { annSuggestVisible.value = false; router.push('/admin') }

const statusTag = (s) => ({ 'DRAFT':'info','PENDING':'warning','APPROVED':'success','REJECTED':'danger','ONGOING':'','FINISHED':'info' }[s]||'info')
const statusLabel = (s) => ({ 'DRAFT':'草稿','PENDING':'待审批','APPROVED':'报名中','REJECTED':'已驳回','ONGOING':'进行中','FINISHED':'已结束' }[s]||s)

onMounted(() => { fetch(); fetchClubs() })
watch([page, statusFilter], fetch)
</script>

<style scoped>
/* ===== 页面布局 ===== */
.activity-page { max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 700; color: #222; margin: 0; }

/* ===== 统计卡片 ===== */
.stats-row { display: flex; gap: 14px; margin-bottom: 18px; }
.stat-card {
  flex: 1; border-radius: 10px; padding: 18px 22px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); text-align: center;
  transition: all 0.25s; cursor: default; position: relative; overflow: hidden;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.08); }
.stat-card__icon {
  position: absolute; top: 12px; right: 14px; font-size: 28px; opacity: 0.15;
}
.stat-card--total  { background: linear-gradient(135deg, #F6E9E2 0%, #f0f5ff 100%); border: 1px solid #F6E9E2; }
.stat-card--ongoing { background: linear-gradient(135deg, #f6ffed 0%, #f0fff0 100%); border: 1px solid #d9f7be; }
.stat-card--pending { background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%); border: 1px solid #ffe7ba; }

.stat-card--total  .stat-num { color: #C96442; }
.stat-card--ongoing .stat-num { color: #52c41a; }
.stat-card--pending .stat-num { color: #fa8c16; }
.stat-card--total  .stat-card__icon { color: #C96442; }
.stat-card--ongoing .stat-card__icon { color: #52c41a; }
.stat-card--pending .stat-card__icon { color: #fa8c16; }

.stat-num { display: block; font-size: 30px; font-weight: 800; line-height: 1.2; }
.stat-lbl { display: block; font-size: 12px; color: #8c8c8c; margin-top: 4px; letter-spacing: 1px; }

/* ===== 搜索筛选行 ===== */
.filter-row { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.filter-row :deep(.el-input__wrapper),
.filter-row :deep(.el-select .el-input__wrapper) {
  height: 40px; border-radius: 8px;
}

/* ===== 表格 ===== */
:deep(.activity-row:nth-child(even) td.el-table__cell) {
  background-color: #fafafa !important;
}
:deep(.activity-row:hover td.el-table__cell) {
  background-color: #f0f5ff !important;
}

/* ===== 状态标签 ===== */
.status-tag {
  display: inline-block; padding: 3px 10px; border-radius: 12px;
  font-size: 12px; font-weight: 600; line-height: 1.6; white-space: nowrap;
}
.status-tag--ONGOING  { background: #e6ffe6; color: #389e0d; }
.status-tag--APPROVED { background: #e6fffb; color: #08979c; }
.status-tag--PENDING  { background: #fff7e6; color: #d46b08; }
.status-tag--FINISHED { background: #f5f5f5; color: #8c8c8c; }
.status-tag--DRAFT    { background: #f5f5f5; color: #8c8c8c; }
.status-tag--REJECTED { background: #fff1f0; color: #cf1322; }

/* ===== 操作按钮容器 ===== */
.action-cell {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}

/* ===== 按钮基础样式 ===== */
.action-cell .el-button {
  height: 30px; padding: 0 14px; font-size: 12px; font-weight: 600;
  border-radius: 6px; transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 0.5px;
}
.action-cell .el-button:hover {
  transform: translateY(-1.5px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.12);
}

/* --- 详情按钮：浅蓝底+深蓝字+蓝色边框 --- */
.btn-detail {
  background: #F6E9E2 !important;
  color: #B4522F !important;
  border: 1px solid #E0A08A !important;
}
.btn-detail:hover {
  background: #F1D0C5 !important;
  color: #994226 !important;
  border-color: #D68264 !important;
  box-shadow: 0 6px 16px rgba(24,144,255,0.2) !important;
}
.btn-detail:active {
  background: #E0A08A !important;
}

/* --- 报名按钮：蓝青渐变主按钮 --- */
.btn-enroll {
  background: linear-gradient(135deg, #C96442, #BE8A3C) !important;
  color: #fff !important; border: none !important;
}
.btn-enroll:hover {
  background: linear-gradient(135deg, #C96442, #5cdbd3) !important;
  box-shadow: 0 6px 16px rgba(24,144,255,0.35) !important;
}
.btn-enroll:active {
  background: linear-gradient(135deg, #B4522F, #20a39c) !important;
}

/* --- 通过按钮：绿色底白字 --- */
.btn-approve {
  background: #52c41a !important; color: #fff !important;
  border: 1px solid #52c41a !important;
}
.btn-approve:hover {
  background: #73d13d !important;
  border-color: #73d13d !important;
  box-shadow: 0 6px 16px rgba(82,196,26,0.35) !important;
}
.btn-approve:active {
  background: #389e0d !important;
}

/* --- 驳回按钮：红色底白字 --- */
.btn-reject {
  background: #ff4d4f !important; color: #fff !important;
  border: 1px solid #ff4d4f !important;
}
.btn-reject:hover {
  background: #ff7875 !important;
  border-color: #ff7875 !important;
  box-shadow: 0 6px 16px rgba(255,77,79,0.35) !important;
}
.btn-reject:active {
  background: #cf1322 !important;
}

/* ===== 表格分页 ===== */
:deep(.el-pagination) { margin-top: 16px; justify-content: flex-end; }

/* ===== 报名弹窗 ===== */
.enroll-activity-info { background: #F6E9E2; border-radius: 10px; padding: 18px; margin-bottom: 18px; }
.enroll-activity-title { font-size: 17px; font-weight: 700; color: #222; margin-bottom: 10px; }
.enroll-activity-meta { display: flex; flex-wrap: wrap; gap: 14px; }
.meta-item { display: flex; align-items: center; gap: 5px; font-size: 13px; color: #595959; }
.meta-item .el-icon { color: #C96442; }

/* ===== 活动详情弹窗 ===== */
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

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .stats-row { flex-direction: column; }
  .action-cell { flex-direction: column; align-items: flex-start; }
}
</style>
