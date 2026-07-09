<template>
  <div class="club-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <span></span>
      <el-button type="primary" size="large" @click="clubWizardRef.open()">申请创建社团</el-button>
    </div>

    <!-- 社长管理入口 -->
    <div v-if="isPresident" class="president-toggle">
      <el-button type="warning" @click="showMyClubs = !showMyClubs">
        <el-icon><Setting /></el-icon> {{ showMyClubs ? '返回社团列表' : '我的社团管理' }}
      </el-button>
    </div>

    <!-- 社长管理模式 -->
    <div v-if="showMyClubs && isPresident" class="president-section">
      <el-card v-for="club in myClubs" :key="club.id" class="president-card">
        <template #header>
          <div class="president-card-header">
            <span class="president-club-name">{{ club.name }}</span>
            <el-tag :type="club.status===1?'success':'warning'">
              {{ club.status===1?'已通过':'待审批' }}
            </el-tag>
          </div>
        </template>
        <div v-if="applications[club.id]?.length" class="applications-wrap">
          <div class="applications-title">待审批的加入申请：</div>
          <el-table :data="applications[club.id]" size="small">
            <el-table-column prop="applicantName" label="申请人" width="120" />
            <el-table-column prop="college" label="学院" width="160" />
            <el-table-column prop="role" label="申请角色" width="100" />
            <el-table-column prop="joinTime" label="申请时间" width="180" />
            <el-table-column label="操作" width="200">
              <template #default="{row}">
                <el-button size="small" type="success" @click="approveMember(club.id, row.id, 1)">通过</el-button>
                <el-button size="small" type="danger" @click="approveMember(club.id, row.id, 2)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="暂无待审批申请" :image-size="60" />
      </el-card>
    </div>

    <!-- 普通社团列表 -->
    <div v-if="!showMyClubs" class="club-list-section">
      <!-- 分类筛选标签 -->
      <div class="category-tags">
        <button
          v-for="cat in categoryOptions"
          :key="cat"
          class="category-tag"
          :class="{ 'category-tag--active': categoryFilter === cat }"
          @click="categoryFilter = cat"
        >{{ cat }}</button>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索社团"
          class="search-input"
          clearable
        />
      </div>

      <!-- 社团卡片网格 -->
      <div v-if="filteredList.length > 0" class="club-grid">
        <div v-for="club in filteredList" :key="club.id" class="club-card">
          <div class="club-card-body">
            <div class="club-card-top">
              <span class="club-card-name">{{ club.name }}</span>
              <el-tag size="small" class="club-card-category">{{ club.category }}</el-tag>
            </div>
            <p class="club-card-desc">{{ club.description || '暂无简介' }}</p>
            <div class="club-card-meta">
              社长: {{ club.presidentName || '待指定' }} &nbsp;|&nbsp; 成员: {{ club.memberCount }}人
            </div>
          </div>
          <div class="club-card-footer">
            <el-button size="small" class="join-btn" @click="openJoinDialog(club)">申请加入</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无社团数据" />

      <div class="recommend-section" v-if="!showMyClubs">
        <h3 class="section-title">🔥 热门社团推荐</h3>
        <div class="recommend-grid">
          <div class="recommend-card" v-for="rc in recommendClubs" :key="rc.name">
            <span class="rec-name">{{ rc.name }}</span>
            <span class="rec-desc">{{ rc.desc }}</span>
            <span class="rec-members">{{ rc.members }} 名成员</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建社团对话框 -->
    <el-dialog v-model="dialog" title="申请创建社团" width="480px" class="create-dialog">
      <el-form :model="f" label-width="80px" class="create-form">
        <el-form-item label="社团名称">
          <el-input v-model="f.name" placeholder="请输入社团名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="f.category" class="full-width">
            <el-option v-for="c in cats" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="f.description" type="textarea" :rows="3" placeholder="请输入社团简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="create">提交</el-button>
      </template>
    </el-dialog>

    <!-- 申请加入社团对话框 -->
    <el-dialog v-model="joinDialogVisible" title="申请加入社团" width="520px" destroy-on-close class="join-dialog">
      <!-- 社团信息卡片 -->
      <div class="join-club-info" v-if="joinClub">
        <div class="join-club-name">{{ joinClub.name }}</div>
        <div class="join-club-meta">
          <el-tag size="small">{{ joinClub.category }}</el-tag>
          <span>社长: {{ joinClub.presidentName }}</span>
          <span>成员: {{ joinClub.memberCount }}人</span>
        </div>
      </div>

      <el-form :model="joinForm" :rules="joinRules" ref="joinFormRef" label-width="80px" class="join-form">
        <el-form-item label="姓名">
          <el-input :model-value="user.realName" disabled />
        </el-form-item>
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="joinForm.studentId" placeholder="请输入你的学号" maxlength="20" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input :model-value="user.college" disabled />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="joinForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="申请理由" prop="reason">
          <el-input v-model="joinForm.reason" type="textarea" :rows="3"
            placeholder="请详细说明你想加入该社团的原因，比如：个人兴趣、相关经历、想获得什么…" />
        </el-form-item>
        <el-form-item label="自我介绍" prop="intro">
          <el-input v-model="joinForm.intro" type="textarea" :rows="2"
            placeholder="简单介绍一下自己（特长、兴趣爱好等，选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmJoin" :loading="joining">
          <el-icon><Check /></el-icon> 确认申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 新版社团创建向导 -->
    <ClubCreateWizard ref="clubWizardRef" @refresh="fetch" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import ClubCreateWizard from './ClubCreateWizard.vue'

const list = ref([]); const keyword = ref(''); const clubWizardRef = ref(null)
const dialog = ref(false)
const cats = ['学术科技','文化艺术','体育竞技','志愿服务','创新创业']
const f = reactive({name:'',category:'学术科技',description:''})
const user = JSON.parse(localStorage.getItem('user')||'{}')

// 社长管理
const showMyClubs = ref(false)
const myClubs = ref([])
const applications = reactive({})
const isPresident = computed(() => user.role === 'PRESIDENT' || user.role === 'ADMIN')

const fetch = async () => {
  const res = await request.get('/clubs', { params: { page:1, size:100, keyword: keyword.value } })
  if (res.code===200) {
    list.value = res.data.records
    updateRecommend(res.data.records)
  }
}

const fetchMyClubs = async () => {
  const res = await request.get('/my-clubs')
  if (res.code===200) myClubs.value = res.data
  // 为每个社团拉取待审批申请
  for (const club of myClubs.value) {
    try {
      const apps = await request.get(`/club/${club.id}/applications`)
      if (apps.code===200) applications[club.id] = apps.data
    } catch(e) { /* ignore */ }
  }
}

const create = async () => {
  await request.post('/club', f)
  ElMessage.success('提交成功！管理员审批通过后，你将成为该社团社长')
  dialog.value = false; fetch(); fetchMyClubs()
}

// ===== 加入社团弹窗 =====
const joinDialogVisible = ref(false)
const joinClub = ref(null)
const joining = ref(false)
const joinFormRef = ref(null)
const joinForm = reactive({ studentId: '', phone: '', reason: '', intro: '' })
const joinRules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  reason: [{ required: true, message: '请填写申请理由', trigger: 'blur' }],
}

const openJoinDialog = (club) => {
  joinClub.value = club
  joinForm.studentId = ''; joinForm.phone = ''; joinForm.reason = ''; joinForm.intro = ''
  joinDialogVisible.value = true
}

const confirmJoin = async () => {
  const valid = await joinFormRef.value.validate().catch(() => false)
  if (!valid) return
  joining.value = true
  try {
    await request.post('/club/join', { clubId: joinClub.value.id })
    ElMessage.success('申请已提交！请等待社长审批 🙌')
    joinDialogVisible.value = false; fetch()
  } catch (e) { /* request interceptor handles error */ }
  finally { joining.value = false }
}

const approveMember = async (clubId, memberId, status) => {
  await request.put('/club/member/approve', { id: memberId, status })
  ElMessage.success(status===1?'已通过':'已拒绝')
  // 刷新申请列表
  const apps = await request.get(`/club/${clubId}/applications`)
  if (apps.code===200) applications[clubId] = apps.data
}

// 分类筛选
const categoryFilter = ref('全部')
const categoryOptions = ['全部', '学术科技', '文化艺术', '体育竞技', '志愿服务', '创新创业']
const filteredList = computed(() => {
  if (categoryFilter.value === '全部') return list.value
  return list.value.filter(c => c.category === categoryFilter.value)
})

const recommendClubs = ref([])

const updateRecommend = (clubs) => {
  if (!clubs || clubs.length === 0) return
  const sorted = [...clubs].sort((a, b) => (b.memberCount || 0) - (a.memberCount || 0))
  recommendClubs.value = sorted.slice(0, 4).map(c => ({
    name: c.name,
    desc: c.description?.length > 8 ? c.description.slice(0, 8) + '…' : (c.description || ''),
    members: c.memberCount || 0
  }))
}

onMounted(() => { fetch(); fetchMyClubs() })
watch(keyword, fetch)
watch(showMyClubs, (v) => { if (v) fetchMyClubs() })
</script>

<style scoped>
/* ===== 页面容器 ===== */
.club-list-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ===== 页面头部 ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #222;
  margin: 0;
}

/* ===== 社长管理入口 ===== */
.president-toggle {
  margin-bottom: 16px;
}

/* ===== 社长管理模式 ===== */
.president-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.president-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.president-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.president-club-name {
  font-weight: bold;
  font-size: 16px;
}

.applications-wrap {
  margin-bottom: 8px;
}

.applications-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #333;
}

/* ===== 分类标签 ===== */
.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.category-tag {
  padding: 6px 18px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  background: #fff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s;
  outline: none;
  line-height: 1.5;
}

.category-tag:hover {
  background: #F6E9E2;
  border-color: #E0A08A;
}

.category-tag--active {
  background: #C96442;
  color: #fff;
  border-color: #C96442;
}

/* ===== 搜索栏 ===== */
.search-bar {
  margin-bottom: 20px;
  max-width: 320px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #C96442 inset;
}

/* ===== 社团卡片网格 ===== */
.club-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.club-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: box-shadow 0.3s, transform 0.3s;
}

.club-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  transform: translateY(-2px);
}

.club-card-body {
  padding: 16px 16px 12px;
  flex: 1;
}

.club-card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.club-card-name {
  font-size: 15px;
  font-weight: bold;
  color: #222;
}

.club-card-category {
  flex-shrink: 0;
}

.club-card-desc {
  color: #666;
  font-size: 13px;
  min-height: 40px;
  margin: 0 0 12px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.club-card-meta {
  color: #888;
  font-size: 12px;
}

.club-card-footer {
  padding: 10px 16px 14px;
  border-top: 1px solid #f0f0f0;
}

.join-btn {
  width: auto !important;
  padding: 6px 20px !important;
}

/* ===== 热门社团推荐 ===== */
.recommend-section { margin-top: 24px; }
.section-title { font-size: 16px; font-weight: 700; color: #222; margin: 0 0 14px; }
.recommend-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; }
.recommend-card { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); display: flex; flex-direction: column; gap: 4px; transition: all 0.2s; cursor: pointer; }
.recommend-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
.rec-name { font-size: 14px; font-weight: 600; color: #222; }
.rec-desc { font-size: 12px; color: #888; }
.rec-members { font-size: 11px; color: #aaa; }

/* ===== 创建社团对话框 ===== */
.create-form .el-form-item {
  margin-bottom: 18px;
}

.full-width {
  width: 100%;
}

/* ===== 加入社团对话框 ===== */
.join-club-info {
  background: linear-gradient(135deg, #F6E9E2 0%, #f0f5ff 100%);
  border-radius: 10px;
  padding: 18px 20px;
  margin-bottom: 20px;
}
.join-club-name {
  font-size: 18px;
  font-weight: 700;
  color: #26241F;
  margin-bottom: 8px;
}
.join-club-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #595959;
}
.join-form .el-form-item {
  margin-bottom: 16px;
}
</style>
