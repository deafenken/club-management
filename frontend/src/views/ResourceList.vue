<template>
  <div class="resource-page">

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card"><span class="stat-num">{{ items.length }}</span><span class="stat-lbl">物资种类</span></div>
      <div class="stat-card"><span class="stat-num" style="color:#fa8c16">{{ lowStockCount }}</span><span class="stat-lbl">库存紧张</span></div>
    </div>

    <div class="resource-layout">
      <div class="resource-main">
        <el-table :data="items" empty-text="暂无物资数据">
          <el-table-column prop="name" label="名称" width="160"/>
          <el-table-column prop="category" label="分类" width="100"/>
          <el-table-column label="库存" width="220">
            <template #default="{row}">
              <span>可用 {{ row.available }} / 总 {{ row.total }} {{ row.unit }}</span>
              <el-tag :type="row.available/row.total < 0.3 ? 'danger' : 'success'" size="small" style="margin-left:8px">
                {{ row.available/row.total < 0.3 ? '库存紧张' : '库存充足' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{row}">
              <el-button size="small" type="primary" @click="borrow(row)" :disabled="row.available<=0">借用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="resource-sidebar">
        <div class="sidebar-title">物资分类</div>
        <div class="sidebar-item" v-for="cs in categoryStats" :key="cs.name">
          <span class="sidebar-cat-name">{{ cs.name }}</span>
          <span class="sidebar-cat-count">{{ cs.count }}</span>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialog" title="物资借用" width="350px">
      <el-form :model="bf" label-width="90px">
        <el-form-item label="物资"><el-input :model-value="selected?.name" disabled/></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="bf.qty" :min="1" :max="selected?.available"/></el-form-item>
        <el-form-item label="归还日期"><el-date-picker v-model="bf.returnDate" type="date"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="doBorrow">确认借用</el-button></template>
    </el-dialog>

    <ResourceBorrowWizard ref="resourceWizardRef" @refresh="fetch" />
    <div class="quick-links">
      <router-link to="/" class="ql">首页看板 &rarr;</router-link>
      <router-link to="/venues" class="ql">场地预约 &rarr;</router-link>
      <router-link to="/funds" class="ql">经费管理 &rarr;</router-link>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import ResourceBorrowWizard from './ResourceBorrowWizard.vue'
const resourceWizardRef = ref(null)
const items = ref([]); const dialog = ref(false); const selected = ref(null)
const user = JSON.parse(localStorage.getItem('user')||'{}')
const bf = reactive({ qty:1, returnDate:'' })
const lowStockCount = computed(() => items.value.filter(i => i.available / i.total < 0.3).length)
const fetch = async () => { const r = await request.get('/resources'); items.value = r.data }
const borrow = (item) => { resourceWizardRef.value.open() }
const doBorrow = async () => {
  await request.post('/resource/borrow', {
    itemId: selected.value.id, quantity: bf.qty,
    borrowDate: new Date().toISOString().slice(0,10), planReturnDate: bf.returnDate })
  ElMessage.success('借用申请已提交'); dialog.value = false; fetch()
}
const categoryStats = computed(() => {
  const map = {}
  items.value.forEach(i => {
    const cat = i.category || '未分类'
    map[cat] = (map[cat] || 0) + 1
  })
  return Object.entries(map).map(([name, count]) => ({ name, count }))
})
onMounted(fetch)
</script>
<style scoped>
.resource-page { max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 700; color: #222; margin: 0; }

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card {
  flex: 1; max-width: 200px; background: #fff; border-radius: 8px; padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); text-align: center;
}
.stat-num { display: block; font-size: 26px; font-weight: 700; color: #C96442; line-height: 1.2; }
.stat-lbl { display: block; font-size: 12px; color: #888; margin-top: 2px; }

.resource-layout { display: flex; gap: 14px; }
.resource-main { flex: 1; min-width: 0; }
.resource-sidebar { width: 180px; flex-shrink: 0; background: #fff; border-radius: 8px; padding: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); align-self: flex-start; }
.sidebar-title { font-size: 13px; font-weight: 700; color: #222; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }
.sidebar-item { display: flex; justify-content: space-between; padding: 5px 0; font-size: 12px; color: #666; }
.sidebar-cat-count { color: #C96442; font-weight: 600; }

.quick-links { display: flex; gap: 10px; margin-top: 20px; flex-wrap: wrap; }
.ql { font-size: 12px; color: #C96442; background: #F6E9E2; padding: 4px 12px; border-radius: 12px; text-decoration: none; transition: all 0.2s; }
.ql:hover { background: #F1D0C5; transform: translateY(-1px); }
</style>
