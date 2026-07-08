<template>
  <div class="app-center-page">
    <div class="page-header">
      <h1>我的全部申请</h1>
      <p class="subtitle">集中查看社团创建、活动、场地、物资、经费五类申请记录与审批进度</p>
    </div>

    <el-tabs v-model="activeTab" @tab-change="fetchData" class="center-tabs">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="社团创建" name="CLUB" />
      <el-tab-pane label="活动发布" name="ACTIVITY" />
      <el-tab-pane label="场地预约" name="VENUE" />
      <el-tab-pane label="物资借用" name="RESOURCE" />
      <el-tab-pane label="经费申请" name="FUND" />
    </el-tabs>

    <div class="filter-bar">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="fetchData" size="default" style="width:160px">
        <el-option label="全部状态" value="" />
        <el-option label="待审批" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已完成" value="FINISHED" />
      </el-select>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无申请记录" />

      <div v-for="item in list" :key="item.appType+'-'+item.businessId" class="app-card">
        <div class="app-card-header">
          <el-tag :type="appTypeTag(item.appType)" size="small" effect="plain">{{appTypeLabel(item.appType)}}</el-tag>
          <span class="app-card-title">{{item.title}}</span>
          <el-tag :type="statusTag(item.status)" size="small" class="app-card-status">{{item.statusLabel || item.status}}</el-tag>
        </div>

        <div class="app-card-body">
          <span class="app-card-time">{{item.createTime}}</span>
          <span v-if="item.rejectReason" class="reject-info">驳回原因：{{item.rejectReason}}</span>
        </div>

        <div class="app-card-timeline" v-if="item.timeline && item.timeline.length > 0">
          <el-timeline>
            <el-timeline-item
              v-for="t in item.timeline" :key="t.id"
              :timestamp="t.createTime"
              :type="t.status === 'APPROVED' ? 'success' : t.status === 'REJECTED' ? 'danger' : 'warning'"
              size="small"
            >
              {{t.stepName}} —
              <el-tag :type="t.status === 'APPROVED' ? 'success' : t.status === 'REJECTED' ? 'danger' : 'warning'" size="small" effect="plain">{{t.status==='APPROVED'?'通过':t.status==='REJECTED'?'驳回':'待审'}}</el-tag>
              <span v-if="t.comment && t.comment !== '上级已驳回'"> — {{t.comment}}</span>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div class="app-card-actions" v-if="item.status === 'REJECTED' || item.status === 'PENDING'">
          <el-button size="small" type="primary" @click="resubmit(item)">重新编辑提交</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'

const router = useRouter()
const activeTab = ref('')
const statusFilter = ref('')
const list = ref([])
const loading = ref(false)

const appTypeLabel = t => ({CLUB:'社团创建',ACTIVITY:'活动发布',VENUE:'场地预约',RESOURCE:'物资借用',FUND:'经费申请'}[t]||t)
const appTypeTag = t => ({CLUB:'',ACTIVITY:'success',VENUE:'',RESOURCE:'warning',FUND:'danger'}[t]||'')
const statusTag = s => {
  if (!s) return 'info'
  if (s === 'APPROVED' || s === '已通过') return 'success'
  if (s === 'PENDING' || s === '待审批' || s.includes('PENDING')) return 'warning'
  if (s === 'REJECTED' || s === '已驳回') return 'danger'
  if (s === 'FINISHED' || s === '已结束') return ''
  return 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (activeTab.value) params.type = activeTab.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await request.get('/my-applications', {params})
    list.value = res.data || []
  } catch(e) {}
  loading.value = false
}

const resubmit = (item) => {
  // 导航到对应页面打开对应弹窗
  const pageMap = {
    CLUB: '/clubs', ACTIVITY: '/activities', VENUE: '/venues',
    RESOURCE: '/resources', FUND: '/funds'
  }
  const path = pageMap[item.appType] || '/'
  router.push(path)
}

fetchData()
</script>

<style scoped>
.app-center-page { max-width: 960px; margin: 0 auto; padding: 24px; }
.page-header { margin-bottom: 20px; }
.page-header h1 { font-size: 22px; margin: 0; color: #303133; }
.subtitle { color: #909399; font-size: 13px; margin: 4px 0 0; }
.center-tabs { margin-bottom: 12px; }
.filter-bar { margin-bottom: 16px; }
.app-card { background: #fff; border-radius: 8px; padding: 16px 20px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.app-card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.app-card-title { font-size: 15px; font-weight: 500; flex: 1; }
.app-card-body { display: flex; gap: 16px; font-size: 13px; color: #909399; margin-bottom: 8px; }
.reject-info { color: #f56c6c; }
.app-card-timeline { margin-top: 10px; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.app-card-actions { margin-top: 8px; text-align: right; }
</style>
