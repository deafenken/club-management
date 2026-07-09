<template>
  <div class="budget-table">
    <el-table :data="modelValue" border>
      <!-- 开销项目 -->
      <el-table-column label="开销项目" min-width="140">
        <template #default="{ row, $index }">
          <el-input
            :model-value="row.name"
            @update:model-value="(val) => updateCell($index, 'name', val)"
            placeholder="项目名称"
            size="small"
          />
        </template>
      </el-table-column>

      <!-- 单价 -->
      <el-table-column label="单价（元）" width="130">
        <template #default="{ row, $index }">
          <el-input-number
            :model-value="row.unitPrice"
            @update:model-value="(val) => updateCell($index, 'unitPrice', val || 0)"
            :min="0"
            :precision="2"
            :step="10"
            size="small"
            controls-position="right"
            style="width:100%"
          />
        </template>
      </el-table-column>

      <!-- 数量 -->
      <el-table-column label="数量" width="110">
        <template #default="{ row, $index }">
          <el-input-number
            :model-value="row.quantity"
            @update:model-value="(val) => updateCell($index, 'quantity', val || 0)"
            :min="0"
            :step="1"
            size="small"
            controls-position="right"
            style="width:100%"
          />
        </template>
      </el-table-column>

      <!-- 小计（自动计算） -->
      <el-table-column label="小计（元）" width="130">
        <template #default="{ row }">
          <span class="subtotal-cell">¥{{ ((row.unitPrice || 0) * (row.quantity || 0)).toFixed(2) }}</span>
        </template>
      </el-table-column>

      <!-- 用途说明 -->
      <el-table-column label="用途说明" min-width="160">
        <template #default="{ row, $index }">
          <el-input
            :model-value="row.purpose"
            @update:model-value="(val) => updateCell($index, 'purpose', val)"
            placeholder="用途说明"
            size="small"
          />
        </template>
      </el-table-column>

      <!-- 操作 -->
      <el-table-column label="操作" width="70" fixed="right">
        <template #default="{ $index }">
          <el-button
            link
            type="danger"
            size="small"
            @click="removeRow($index)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 合计行 -->
    <div class="total-row">
      <span class="total-label">合计：</span>
      <span class="total-amount">¥{{ totalAmount }}</span>
    </div>

    <!-- 添加按钮 -->
    <el-button
      link
      type="primary"
      class="add-btn"
      @click="addRow"
    >
      + 添加明细行
    </el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
    // [{ name, unitPrice, quantity, subtotal, purpose }]
  }
})

const emit = defineEmits(['update:modelValue'])

// 合计金额
const totalAmount = computed(() => {
  let sum = 0
  for (const row of props.modelValue) {
    sum += (row.unitPrice || 0) * (row.quantity || 0)
  }
  return sum.toFixed(2)
})

function emitChange(newVal) {
  emit('update:modelValue', newVal)
}

function addRow() {
  const newVal = [...props.modelValue, {
    name: '',
    unitPrice: 0,
    quantity: 0,
    subtotal: 0,
    purpose: ''
  }]
  emitChange(newVal)
}

function removeRow(index) {
  const newVal = [...props.modelValue]
  newVal.splice(index, 1)
  emitChange(newVal)
}

function updateCell(index, key, value) {
  const newVal = [...props.modelValue]
  newVal[index] = { ...newVal[index], [key]: value }
  emitChange(newVal)
}
</script>

<style scoped>
.budget-table {
  width: 100%;
}

.subtotal-cell {
  font-weight: 600;
  color: #C96442;
}

.total-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  margin-top: -1px;
}

.total-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.total-amount {
  font-size: 16px;
  font-weight: 700;
  color: #ff4d4f;
  margin-left: 8px;
}

.add-btn {
  margin-top: 8px;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-table .cell) {
  padding: 4px 8px;
}
</style>
