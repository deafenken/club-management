<template>
  <div class="step-wizard">
    <!-- 步骤条 -->
    <el-steps :active="current" align-center finish-status="success">
      <el-step
        v-for="(step, index) in steps"
        :key="index"
        :title="step.title"
        :description="step.description"
      />
    </el-steps>

    <!-- 步骤内容插槽 -->
    <div class="step-content">
      <slot :step="current" />
    </div>

    <!-- 底部操作栏 -->
    <div class="step-actions">
      <el-button
        :disabled="current === 0"
        :loading="loading"
        @click="handlePrev"
      >
        上一步
      </el-button>
      <el-button
        :loading="loading"
        @click="handleSaveDraft"
      >
        保存草稿
      </el-button>
      <el-button
        v-if="current < steps.length - 1"
        :loading="loading"
        type="primary"
        @click="handleNext"
      >
        下一步
      </el-button>
      <el-button
        v-if="current === steps.length - 1"
        type="primary"
        :loading="loading"
        @click="handleSubmit"
      >
        提交申请
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  steps: {
    type: Array,
    required: true
    // [{ title: '', description: '' }]
  },
  current: {
    type: Number,
    default: 0
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:current', 'prev', 'next', 'submit', 'save-draft'])

function handlePrev() {
  if (props.current > 0) {
    emit('update:current', props.current - 1)
    emit('prev', props.current - 1)
  }
}

function handleNext() {
  if (props.current < props.steps.length - 1) {
    emit('update:current', props.current + 1)
    emit('next', props.current + 1)
  }
}

function handleSubmit() {
  emit('submit')
}

function handleSaveDraft() {
  emit('save-draft')
}
</script>

<style scoped>
.step-wizard {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  padding: 24px;
}

.step-content {
  min-height: 200px;
  margin: 32px 0;
  padding: 0 8px;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-steps) {
  --el-color-primary: #1890ff;
}

:deep(.el-step__title) {
  font-size: 14px;
  font-weight: 600;
}

:deep(.el-step__description) {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

:deep(.el-step__head.is-finish .el-step__icon) {
  background: #1890ff;
  border-color: #1890ff;
}

:deep(.el-step__head.is-process .el-step__icon) {
  border-color: #1890ff;
  background: #1890ff;
}

:deep(.el-step__title.is-process) {
  color: #1890ff;
  font-weight: 700;
}
</style>
