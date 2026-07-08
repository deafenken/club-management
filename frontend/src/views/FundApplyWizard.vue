<template>
  <el-dialog v-model="visible" title="申请经费" width="750px" top="3vh" destroy-on-close @opened="onOpen">
    <div class="wizard-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="wizard-form">

        <h4 class="sec-title">主体信息</h4>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="申请社团" prop="clubId">
              <el-select v-model="form.clubId" class="full-width" placeholder="请选择">
                <el-option v-for="c in myClubs" :key="c.id" :label="c.name" :value="c.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经费类型" prop="fundType">
              <el-select v-model="form.fundType" class="full-width">
                <el-option v-for="t in fundTypes" :key="t" :label="t" :value="t"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联活动">
              <el-select v-model="form.activityId" class="full-width" clearable placeholder="可选">
                <el-option v-for="a in activities" :key="a.id" :label="a.title" :value="a.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经费来源">
              <el-select v-model="form.source" class="full-width">
                <el-option v-for="s in sources" :key="s" :label="s" :value="s"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="指导教师姓名"><el-input v-model="form.teacherName"/></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教师签字确认单"><el-input v-model="form.teacherApprovalUrl" placeholder="上传签字确认单URL"/></el-form-item>
          </el-col>
        </el-row>

        <h4 class="sec-title">预算明细</h4>
        <el-table :data="budgetItems" border size="small" class="budget-table">
          <el-table-column label="开销项目" width="140">
            <template #default="{row,$index}"><el-input v-model="budgetItems[$index].name" size="small"/></template>
          </el-table-column>
          <el-table-column label="单价(元)" width="100">
            <template #default="{row,$index}"><el-input-number v-model="budgetItems[$index].unitPrice" size="small" :min="0" controls-position="right" style="width:100%"/></template>
          </el-table-column>
          <el-table-column label="数量" width="80">
            <template #default="{row,$index}"><el-input-number v-model="budgetItems[$index].quantity" size="small" :min="1" controls-position="right" style="width:100%"/></template>
          </el-table-column>
          <el-table-column label="小计(元)" width="100">
            <template #default="{row}">{{(row.unitPrice * row.quantity).toFixed(2)}}</template>
          </el-table-column>
          <el-table-column label="用途说明">
            <template #default="{row,$index}"><el-input v-model="budgetItems[$index].purpose" size="small"/></template>
          </el-table-column>
          <el-table-column width="70">
            <template #default="{row,$index}"><el-button size="small" type="danger" link @click="budgetItems.splice($index,1)">删除</el-button></template>
          </el-table-column>
        </el-table>
        <el-button size="small" type="primary" link @click="budgetItems.push({name:'',unitPrice:0,quantity:1,purpose:''})">+ 添加明细行</el-button>
        <div class="total-row">合计：<strong>¥{{totalAmount.toFixed(2)}}</strong></div>

        <el-form-item label="申请总金额" prop="totalAmount">
          <el-input-number v-model="form.totalAmount" :min="0.01" :precision="2" :step="100" style="width:200px"/> 元
          <el-tag v-if="form.totalAmount >= 5000" type="warning" size="small" class="ml-8">大额申请 → 需校级领导终审</el-tag>
        </el-form-item>

        <el-form-item label="附件" prop="attachmentsText">
          <el-input v-model="attachmentsText" type="textarea" :rows="2" placeholder="采购报价单、发票、合同链接等，每行一个"/>
          <el-button size="small" link type="primary" style="margin-top:4px" @click="showTemplate">📥 经费预算模板下载</el-button>
        </el-form-item>
        <el-form-item label="用途说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2"/>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
      <el-button type="primary" @click="submit" :loading="submitting">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const emit = defineEmits(['refresh'])
const visible = ref(false)
const saving = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const myClubs = ref([])
const activities = ref([])

const fundTypes = ['社团年度拨款','活动支出','设备采购','交通差旅','奖品物料','场地租赁','宣传物料']
const sources = ['校团委专项拨款','社团会费','企业赞助']
const cats = ['活动经费','设备采购','场地费用','宣传物料','奖品礼品','交通差旅','社团拨款','其他']

const form = reactive({
  clubId:null, fundType:'活动支出', activityId:null, source:'校团委专项拨款',
  teacherName:'', teacherApprovalUrl:'',
  category:'活动经费', totalAmount:null, amount:null,
  description:'', budgetItems:'', attachments:''
})

const budgetItems = ref([{name:'',unitPrice:0,quantity:1,purpose:''}])
const attachmentsText = ref('')

const totalAmount = computed(() =>
  budgetItems.value.reduce((s,i) => s + (i.unitPrice||0) * (i.quantity||0), 0))

const rules = {
  clubId:[{required:true,message:'请选择社团',trigger:'change'}],
  fundType:[{required:true,message:'请选择经费类型',trigger:'change'}],
  totalAmount:[{required:true,message:'请输入申请金额',trigger:'blur'}],
  description:[{required:true,message:'请填写用途说明',trigger:'blur'}],
}

const fetchClubs = async () => {
  try {
    const role = JSON.parse(localStorage.getItem('user')||'{}').role
    const url = role === 'ADMIN' ? '/clubs?page=1&size=200' : '/my-clubs'
    const res = await request.get(url)
    myClubs.value = res.data?.records || res.data || []
  } catch(e) {}
}

const fetchActivities = async () => {
  try {
    const res = await request.get('/activities?page=1&size=200&status=APPROVED')
    activities.value = res.data?.records || []
  } catch(e) {}
}

const open = () => { visible.value = true }
const onOpen = async () => {
  await Promise.all([fetchClubs(), fetchActivities()])
  try {
    const res = await request.get('/draft/load?appType=FUND')
    if (res.data && res.data.formData) {
      try {
        const d = JSON.parse(res.data.formData)
        Object.assign(form, d)
        if (d.budgetItems) budgetItems.value = JSON.parse(d.budgetItems)
        ElMessage.info('已恢复上次草稿')
      } catch(e) {}
    }
  } catch(e) {}
}

const saveDraft = async () => {
  saving.value = true
  try {
    form.budgetItems = JSON.stringify(budgetItems.value)
    form.totalAmount = totalAmount.value
    await request.post('/draft/save', {
      appType:'FUND', stepIndex:'0',
      formData: JSON.stringify({...form, budgetItems:form.budgetItems})
    })
    ElMessage.success('草稿已保存')
  } catch(e) {}
  saving.value = false
}

const submit = async () => {
  if (!budgetItems.value.some(i=>i.name)) { ElMessage.warning('请至少填写一条预算明细'); return }
  const cat = form.fundType
  if (cat && (cat.includes('餐饮')||cat.includes('娱乐'))) { ElMessage.error('餐饮娱乐类不符合经费使用规定'); return }

  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    const data = {
      ...form,
      category: form.fundType,
      amount: form.totalAmount || totalAmount.value,
      totalAmount: form.totalAmount || totalAmount.value,
      budgetItems: JSON.stringify(budgetItems.value),
      attachments: JSON.stringify(
        (attachmentsText.value||'').split('\n').filter(l=>l.trim()).map(l=> ({url:l.trim(),label:''}))
      )
    }
    await request.post('/fund/full', data)
    ElMessage.success('经费申请已提交！')
    visible.value = false
    emit('refresh')
  } catch(e) {}
  submitting.value = false
}

const showTemplate = async () => {
  try {
    const res = await request.get('/template-info/budget')
    ElMessage.info(res.data?.info || '经费预算明细表模板：开销项目名称、单价、数量、小计金额、详细用途说明')
  } catch(e) { ElMessage.info('经费预算明细表模板：开销项目名称、单价、数量、小计金额、详细用途说明') }
}

defineExpose({ open })
</script>

<style scoped>
.wizard-body { padding: 0 10px; }
.wizard-form { max-height: 60vh; overflow-y: auto; padding-right: 10px; }
.sec-title { margin: 16px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #ebeef5; font-size: 14px; color: #303133; }
.full-width { width: 100%; }
.budget-table { margin-bottom: 6px; }
.total-row { text-align: right; padding: 6px 0; font-size: 15px; }
.ml-8 { margin-left: 8px; }
</style>
