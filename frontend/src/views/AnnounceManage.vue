<template>
  <div class="announce-manage-page" v-if="user.role === 'ADMIN'">
    <div class="page-header">
      <h2><el-icon><Notification /></el-icon> 公告发布管理</h2>
      <p class="subtitle">管理员可在此发布平台公告，推送至指定用户群体</p>
    </div>

    <!-- 发布表单 -->
    <div class="publish-card">
      <h3>发布新公告</h3>
      <el-form :model="form" label-width="80px">
        <el-form-item label="公告标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入公告内容" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="推送范围">
          <el-radio-group v-model="form.target">
            <el-radio value="ALL">全体用户</el-radio>
            <el-radio value="PRESIDENT">仅社长</el-radio>
            <el-radio value="STUDENT">仅学生</el-radio>
            <el-radio value="ADMIN">仅管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="publish" :loading="publishing">
            <el-icon><Promotion /></el-icon> 发布并推送
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 已发布列表 -->
    <div class="history-card">
      <h3>已发布公告（最近10条）</h3>
      <el-table :data="announcements" stripe style="width:100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="isTop" label="置顶" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isTop === 1" size="small" type="warning">置顶</el-tag>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="toggleTop(row)">
              {{ row.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
  <div v-else class="forbidden-page">
    <el-result icon="warning" title="403 无权限访问" sub-title="仅管理员可发布和管理公告">
      <template #extra><el-button type="primary" @click="$router.push('/')">返回首页</el-button></template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request.js'

const user = JSON.parse(localStorage.getItem('user')||'{}')

const form = reactive({ title: '', content: '', target: 'ALL' })
const publishing = ref(false)
const announcements = ref([])

const loadAnnouncements = async () => {
  try {
    const res = await request.get('/announcements?page=1&size=10')
    announcements.value = res.data.records || res.data || []
  } catch (e) { /* ignore */ }
}

const publish = async () => {
  if (!form.title.trim()) return ElMessage.warning('请输入公告标题')
  if (!form.content.trim()) return ElMessage.warning('请输入公告内容')
  publishing.value = true
  try {
    await request.post('/announcement', {
      title: form.title,
      content: form.content,
      target: form.target
    })
    ElMessage.success('公告发布成功，已推送通知')
    form.title = ''
    form.content = ''
    form.target = 'ALL'
    loadAnnouncements()
  } catch (e) { /* ignore */ }
  finally { publishing.value = false }
}

const toggleTop = async (row) => {
  try {
    await request.put(`/announcement/${row.id}/top`)
    row.isTop = row.isTop === 1 ? 0 : 1
    ElMessage.success(row.isTop === 1 ? '已置顶' : '已取消置顶')
  } catch (e) { /* ignore */ }
}

const formatDate = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour:'2-digit', minute:'2-digit' })
}

onMounted(loadAnnouncements)
</script>

<style scoped>
.announce-manage-page { max-width: 800px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 {
  font-size: 20px; font-weight: 700; color: #303133;
  display: flex; align-items: center; gap: 8px; margin: 0 0 6px;
}
.page-header .subtitle { color: #909399; font-size: 14px; margin: 0; }
.publish-card, .history-card {
  background: #fff; border-radius: 12px; padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04); margin-bottom: 20px;
}
.publish-card h3, .history-card h3 {
  font-size: 16px; font-weight: 600; color: #303133; margin: 0 0 16px;
}
</style>
