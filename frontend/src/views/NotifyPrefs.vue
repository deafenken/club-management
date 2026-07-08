<template>
  <div class="notify-prefs-page">
    <div class="page-header">
      <h2><el-icon><Bell /></el-icon> 通知偏好设置</h2>
      <p class="subtitle">选择你需要接收的通知类型，关闭后不再推送对应消息</p>
    </div>

    <div class="prefs-card">
      <div class="pref-group" v-for="group in groupedPrefs" :key="group.category">
        <h3 class="group-title">{{ group.label }}</h3>
        <div class="pref-row" v-for="pref in group.items" :key="pref.typeCode">
          <div class="pref-info">
            <span class="pref-label">{{ pref.typeLabel }}</span>
          </div>
          <el-switch
            v-model="pref.enabled"
            active-color="#1890ff"
            @change="(val) => savePref(pref.typeCode, val)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request.js'

const preferences = ref([])

const groupedPrefs = computed(() => {
  const map = {
    APPROVAL: { label: '审批通知', category: 'APPROVAL', items: [] },
    ANNOUNCEMENT: { label: '公告通知', category: 'ANNOUNCEMENT', items: [] },
    SYSTEM: { label: '系统通知', category: 'SYSTEM', items: [] },
  }
  for (const p of preferences.value) {
    const cat = p.category || 'SYSTEM'
    if (map[cat]) map[cat].items.push(p)
  }
  return Object.values(map).filter(g => g.items.length > 0)
})

const loadPrefs = async () => {
  try {
    const res = await request.get('/notification/preferences')
    preferences.value = res.data || []
  } catch (e) { /* ignore */ }
}

const savePref = async (typeCode, enabled) => {
  try {
    await request.put('/notification/preference', { typeCode, enabled })
    ElMessage.success(enabled ? '已开启通知' : '已关闭通知')
  } catch (e) { /* ignore */ }
}

onMounted(loadPrefs)
</script>

<style scoped>
.notify-prefs-page {
  max-width: 700px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 24px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 6px;
}
.page-header .subtitle {
  color: #909399;
  font-size: 14px;
  margin: 0;
}
.prefs-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.pref-group {
  margin-bottom: 20px;
}
.pref-group:last-child { margin-bottom: 0; }
.group-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.pref-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}
.pref-row + .pref-row { border-top: 1px solid #fafafa; }
.pref-label { font-size: 14px; color: #606266; }
</style>
