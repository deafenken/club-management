<template>
  <div class="file-uploader">
    <el-input
      :model-value="modelValue"
      @update:model-value="handleInput"
      placeholder="请输入文件链接或上传至云存储后填入URL"
    >
      <template #prepend>
        <span class="prepend-label">{{ label }}</span>
      </template>
    </el-input>

    <div class="uploader-extra">
      <el-button
        v-if="templateType"
        size="small"
        link
        type="primary"
        @click="downloadTemplate"
      >
        📥 下载模板
      </el-button>
      <span class="helper-text">请输入文件链接或上传至云存储后填入URL</span>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request.js'

const props = defineProps({
  label: {
    type: String,
    default: '文件链接'
  },
  modelValue: {
    type: String,
    default: ''
  },
  templateType: {
    type: String,
    default: ''
    // 'charter' | 'activity_plan' | 'budget'
  }
})

const emit = defineEmits(['update:modelValue'])

function handleInput(val) {
  emit('update:modelValue', val)
}

async function downloadTemplate() {
  try {
    const res = await request.get('/template-info/' + props.templateType)
    const info = res && res.data ? res.data : res
    ElMessage.info(
      '模板信息：' + (typeof info === 'string' ? info : JSON.stringify(info, null, 2))
    )
  } catch {
    ElMessage.info('模板暂不可用，请联系管理员获取模板文件')
  }
}
</script>

<style scoped>
.file-uploader {
  width: 100%;
}

.prepend-label {
  font-weight: 500;
  color: #444;
}

.uploader-extra {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.helper-text {
  font-size: 12px;
  color: #999;
}
</style>
