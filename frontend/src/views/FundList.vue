<template>
  <div class="fund-page">
    <div class="page-header">
      <span></span>
      <el-button type="primary" @click="fundWizardRef.open()" :disabled="myClubs.length === 0">
        <el-icon><Plus /></el-icon> 申请经费
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card"><span class="stat-num">{{ funds.length }}</span><span class="stat-lbl">经费记录</span></div>
      <div class="stat-card"><span class="stat-num" style="color:#52c41a">&yen;{{ approvedAmount }}</span><span class="stat-lbl">已通过金额</span></div>
      <div class="stat-card"><span class="stat-num" style="color:#fa8c16">&yen;{{ pendingAmount }}</span><span class="stat-lbl">待审批金额</span></div>
    </div>

    <!-- 筛选 -->
    <div class="filter-row">
      <el-input v-model="searchKeyword" placeholder="搜索经费记录" style="width:200px" clearable/>
      <el-select v-model="filterStatus" placeholder="筛选状态" clearable size="default" style="width:140px" @change="fetchFunds">
        <el-option label="全部" value=""/>
        <el-option label="待审批" value="PENDING"/>
        <el-option label="已通过" value="APPROVED"/>
        <el-option label="已驳回" value="REJECTED"/>
      </el-select>
    </div>

    <el-table :data="displayFunds" v-loading="loading" :empty-text="emptyText">
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column label="社团" width="140">
        <template #default="{ row }">{{ getClubName(row.clubId) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 'INCOME' ? 'success' : 'danger'" size="small">
            {{ row.type === 'INCOME' ? '收入' : '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额（元）" width="110">
        <template #default="{ row }">
          <span :style="{ color: row.type==='INCOME'?'#52c41a':'#ff4d4f', fontWeight:'bold' }">
            {{ row.type==='INCOME'?'+':'-' }}{{ row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100"/>
      <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip/>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170"/>
    </el-table>

    <el-dialog v-model="showDialog" title="申请经费" width="460px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="所属社团" prop="clubId">
          <el-select v-model="form.clubId" placeholder="选择社团" style="width:100%">
            <el-option v-for="c in myClubs" :key="c.id" :label="c.name" :value="c.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="INCOME">收入（拨款）</el-radio>
            <el-radio value="EXPENSE">支出（报销）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="100" style="width:100%" placeholder="请输入金额"/>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
            <el-option label="活动经费" value="活动经费"/><el-option label="设备采购" value="设备采购"/>
            <el-option label="场地费用" value="场地费用"/><el-option label="宣传物料" value="宣传物料"/>
            <el-option label="餐饮招待" value="餐饮招待"/><el-option label="奖品礼品" value="奖品礼品"/>
            <el-option label="交通差旅" value="交通差旅"/><el-option label="社团拨款" value="社团拨款"/>
            <el-option label="其他" value="其他"/>
          </el-select>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请详细说明经费用途..."/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFund" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <div class="quick-links">
      <router-link to="/" class="ql">首页看板 &rarr;</router-link>
      <router-link to="/clubs" class="ql">社团管理 &rarr;</router-link>
      <router-link to="/resources" class="ql">物资管理 &rarr;</router-link>
    </div>

    <FundApplyWizard ref="fundWizardRef" @refresh="fetchFunds" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import FundApplyWizard from './FundApplyWizard.vue'

const fundWizardRef = ref(null)
const funds = ref([])
const myClubs = ref([])
const loading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const filterStatus = ref('')
const formRef = ref(null)
const user = JSON.parse(localStorage.getItem('user') || '{}')

const form = reactive({ clubId: null, type: 'EXPENSE', amount: null, category: '', description: '' })

const rules = {
  clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请填写说明', trigger: 'blur' }]
}

const statusTag = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s)

const clubMap = ref({})
const getClubName = (id) => clubMap.value[id] || `社团${id}`

const approvedAmount = computed(() => funds.value.filter(f => f.status === 'APPROVED').reduce((s, f) => s + (f.amount || 0), 0))
const pendingAmount = computed(() => funds.value.filter(f => f.status === 'PENDING').reduce((s, f) => s + (f.amount || 0), 0))

const searchKeyword = ref('')

const emptyText = computed(() => {
  if (myClubs.value.length === 0) return '你还未加入任何社团，暂无经费数据。请先加入社团后再查看'
  return '暂无经费记录，点击上方按钮申请经费'
})

const displayFunds = computed(() => {
  if (!searchKeyword.value) return funds.value
  const kw = searchKeyword.value.toLowerCase()
  return funds.value.filter(f =>
    (f.description || '').toLowerCase().includes(kw) ||
    (f.category || '').toLowerCase().includes(kw)
  )
})

onMounted(async () => {
  const clubRes = await request.get('/my-clubs')
  if (clubRes.code === 200) { myClubs.value = clubRes.data || []; myClubs.value.forEach(c => { clubMap.value[c.id] = c.name }) }
  const allClubs = await request.get('/clubs', { params: { page: 1, size: 200 } })
  if (allClubs.code === 200) { (allClubs.data.records || []).forEach(c => { clubMap.value[c.id] = c.name }) }
  fetchFunds()
})

const fetchFunds = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value) params.status = filterStatus.value
    const res = await request.get('/funds', { params })
    if (res.code === 200) {
      funds.value = res.data.records || []
    }
  } finally { loading.value = false }
}

const submitFund = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await request.post('/fund', { ...form })
    if (res.code === 200) {
      ElMessage.success('经费申请已提交，等待管理员审批')
      showDialog.value = false
      form.clubId = null; form.type = 'EXPENSE'; form.amount = null; form.category = ''; form.description = ''
      fetchFunds()
    }
  } finally { submitting.value = false }
}
</script>

<style scoped>
.fund-page { max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 700; color: #222; margin: 0; }

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card {
  flex: 1; background: #fff; border-radius: 8px; padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); text-align: center;
}
.stat-num { display: block; font-size: 26px; font-weight: 700; color: #C96442; line-height: 1.2; }
.stat-lbl { display: block; font-size: 12px; color: #888; margin-top: 2px; }

.filter-row { display: flex; gap: 10px; margin-bottom: 14px; }

.quick-links { display: flex; gap: 10px; margin-top: 20px; flex-wrap: wrap; }
.ql { font-size: 12px; color: #C96442; background: #F6E9E2; padding: 4px 12px; border-radius: 12px; text-decoration: none; transition: all 0.2s; }
.ql:hover { background: #F1D0C5; transform: translateY(-1px); }
</style>
