<template>
  <div v-loading="loading" class="approval-timeline">
    <!-- 暂无记录 -->
    <div v-if="!loading && records.length === 0" class="empty-state">
      <span>暂无审批记录</span>
    </div>

    <!-- 审批时间线 -->
    <el-timeline v-if="records.length > 0">
      <el-timeline-item
        v-for="(item, index) in records"
        :key="index"
        :timestamp="item.time || item.createTime || ''"
        placement="top"
      >
        <div class="timeline-item">
          <div class="timeline-header">
            <span class="step-name">{{ item.stepName }}</span>
            <el-tag :type="statusTagType(item.status)" size="small">
              {{ statusLabel(item.status) }}
            </el-tag>
          </div>
          <p v-if="item.comment" class="timeline-comment">{{ item.comment }}</p>
          <p v-if="item.approver" class="timeline-approver">审批人：{{ item.approver }}</p>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import request from '../api/request.js'

const props = defineProps({
  appType: {
    type: String,
    required: true
  },
  businessId: {
    type: [Number, String],
    required: true
  }
})

const loading = ref(false)
const records = ref([])

function statusTagType(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return map[status] || status
}

async function fetchTimeline() {
  loading.value = true
  try {
    const res = await request.get('/approval/timeline', {
      params: {
        appType: props.appType,
        businessId: props.businessId
      }
    })
    records.value = (res && res.data) ? res.data : (Array.isArray(res) ? res : [])
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchTimeline()
})

watch(
  () => [props.appType, props.businessId],
  () => {
    fetchTimeline()
  }
)
</script>

<style scoped>
.approval-timeline {
  min-height: 80px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  color: #999;
  font-size: 14px;
}

.timeline-item {
  padding: 4px 0;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.step-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.timeline-comment {
  font-size: 13px;
  color: #666;
  margin: 4px 0;
  line-height: 1.5;
}

.timeline-approver {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
