<template>
  <el-dialog v-model="visible" title="物资借用" width="700px" top="3vh" destroy-on-close @opened="onOpen">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="wizard-form">
      <h4 class="sec-title">借用人信息</h4>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="借用人" prop="borrowerName"><el-input v-model="form.borrowerName"/></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="学号/工号"><el-input v-model="form.borrowerStudentId"/></el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="联系电话" prop="borrowerPhone"><el-input v-model="form.borrowerPhone" maxlength="11"/></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属社团" prop="clubId">
            <el-select v-model="form.clubId" class="full-width"><el-option v-for="c in myClubs" :key="c.id" :label="c.name" :value="c.id"/></el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="关联活动">
            <el-select v-model="form.activityId" class="full-width" clearable placeholder="可选">
              <el-option v-for="a in activities" :key="a.id" :label="a.title" :value="a.id"/>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <h4 class="sec-title">借用物资</h4>
      <el-radio-group v-model="borrowMode" class="mode-switch">
        <el-radio value="single">单物资借用</el-radio>
        <el-radio value="batch">批量借用</el-radio>
      </el-radio-group>

      <template v-if="borrowMode === 'single'">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="选择物资" prop="singleItemId">
              <el-select v-model="form.singleItemId" class="full-width" @change="onSingleItemChange" placeholder="请选择">
                <el-option v-for="r in resources" :key="r.id" :label="r.name + ' (可用' + r.available + ')'" :value="r.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="借用数量" prop="singleQuantity">
              <el-input-number v-model="form.singleQuantity" :min="1" :max="selectedResource?.available || 1" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="4" v-if="selectedResource">
            <span class="stock-info">可用 {{ selectedResource.available }}</span>
          </el-col>
        </el-row>
      </template>

      <template v-if="borrowMode === 'batch'">
        <el-table :data="batchItems" border size="small" class="batch-table">
          <el-table-column label="物资" min-width="170">
            <template #default="{ row, $index }">
              <el-select v-model="batchItems[$index].itemId" class="full-width" size="small" @change="(id) => onBatchItemChange($index, id)">
                <el-option v-for="r in resources" :key="r.id" :label="r.name + ' (可用' + r.available + ')'" :value="r.id"/>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template #default="{ row, $index }">
              <el-input-number v-model="batchItems[$index].quantity" size="small" :min="1" :max="getBatchMax($index)" style="width:100%"/>
            </template>
          </el-table-column>
          <el-table-column label="用途" min-width="140">
            <template #default="{ row, $index }">
              <el-input v-model="batchItems[$index].usage" size="small" placeholder="用途说明"/>
            </template>
          </el-table-column>
          <el-table-column width="60">
            <template #default="{ $index }">
              <el-button size="small" type="danger" link @click="batchItems.splice($index, 1)" :disabled="batchItems.length <= 1">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button size="small" type="primary" link @click="batchItems.push({ itemId: null, quantity: 1, usage: '' })">+ 添加物资行</el-button>
      </template>

      <h4 class="sec-title">借用详情</h4>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="借用日期" prop="borrowStartDate">
            <el-date-picker v-model="form.borrowStartDate" type="date" class="full-width" value-format="YYYY-MM-DD"/>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="计划归还" prop="planReturnDate">
            <el-date-picker v-model="form.planReturnDate" type="date" class="full-width" value-format="YYYY-MM-DD"/>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="保管人"><el-input v-model="form.custodian"/></el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="使用场景" prop="usageScenario">
        <el-input v-model="form.usageScenario" type="textarea" :rows="2" placeholder="说明物资用途及预期使用方式"/>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="贵重物资"><el-switch v-model="form.isValuable"/></el-form-item>
        </el-col>
        <el-col :span="16" v-if="form.isValuable">
          <el-form-item label="教师同意书URL" prop="attachmentUrl">
            <el-input v-model="form.attachmentUrl" placeholder="上传指导教师签字同意书链接"/>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
      <el-button type="primary" @click="submit" :loading="submitting">提交借用</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const emit = defineEmits(['refresh'])
const visible = ref(false)
const saving = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const myClubs = ref([])
const activities = ref([])
const resources = ref([])
const borrowMode = ref('single')
const selectedResource = ref(null)

const batchItems = ref([{ itemId: null, quantity: 1, usage: '' }])

const form = reactive({
  borrowerName: '', borrowerStudentId: '', borrowerPhone: '',
  clubId: null, activityId: null,
  singleItemId: null, singleQuantity: 1,
  borrowStartDate: '', planReturnDate: '',
  custodian: '', usageScenario: '',
  isValuable: false, attachmentUrl: ''
})

const rules = {
  borrowerName: [{ required: true, message: '请输入借用人姓名', trigger: 'blur' }],
  borrowerPhone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  clubId: [{ required: true, message: '请选择所属社团', trigger: 'change' }],
  singleItemId: [{ required: true, message: '请选择物资', trigger: 'change' }],
  singleQuantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  borrowStartDate: [{ required: true, message: '请选择借用日期', trigger: 'change' }],
  planReturnDate: [{ required: true, message: '请选择计划归还日期', trigger: 'change' }],
  usageScenario: [{ required: true, message: '请填写使用场景', trigger: 'blur' }]
}

const fetchClubs = async () => {
  try {
    const role = JSON.parse(localStorage.getItem('user') || '{}').role
    const url = role === 'ADMIN' ? '/clubs?page=1&size=200' : '/my-clubs'
    const res = await request.get(url)
    myClubs.value = res.data?.records || res.data || []
  } catch (e) {}
}

const fetchActivities = async () => {
  try {
    const res = await request.get('/activities?page=1&size=200&status=APPROVED')
    activities.value = res.data?.records || []
  } catch (e) {}
}

const fetchResources = async () => {
  try {
    const res = await request.get('/resources')
    resources.value = res.data || []
  } catch (e) {}
}

const onSingleItemChange = (id) => {
  selectedResource.value = resources.value.find(r => r.id === id) || null
}

const onBatchItemChange = (index, id) => {
  const r = resources.value.find(r => r.id === id)
  if (r) {
    batchItems.value[index]._available = r.available
  }
}

const getBatchMax = (index) => {
  return batchItems.value[index]?._available || 99
}

const open = () => { visible.value = true }
const onOpen = async () => {
  await Promise.all([fetchClubs(), fetchActivities(), fetchResources()])
  try {
    const res = await request.get('/draft/load?appType=RESOURCE')
    if (res.data && res.data.formData) {
      try {
        const d = JSON.parse(res.data.formData)
        Object.assign(form, d)
        borrowMode.value = d.borrowMode || 'single'
        if (d.batchItems) batchItems.value = d.batchItems
        if (d.singleItemId) onSingleItemChange(d.singleItemId)
        ElMessage.info('已恢复上次草稿')
      } catch (e) {}
    }
  } catch (e) {}
}

const saveDraft = async () => {
  saving.value = true
  try {
    await request.post('/draft/save', {
      appType: 'RESOURCE', stepIndex: '0',
      formData: JSON.stringify({ ...form, borrowMode: borrowMode.value, batchItems: batchItems.value })
    })
    ElMessage.success('草稿已保存')
  } catch (e) {}
  saving.value = false
}

const submit = async () => {
  if (borrowMode.value === 'single' && !form.singleItemId) {
    ElMessage.warning('请选择借用物资'); return
  }
  if (borrowMode.value === 'batch') {
    const hasItem = batchItems.value.some(i => i.itemId)
    if (!hasItem) { ElMessage.warning('请至少选择一条物资'); return }
  }
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    const data = {
      ...form,
      borrowMode: borrowMode.value,
      batchItems: borrowMode.value === 'batch' ? batchItems.value : null
    }
    await request.post('/resource/borrow/full', data)
    ElMessage.success('物资借用申请已提交！')
    visible.value = false
    emit('refresh')
  } catch (e) {}
  submitting.value = false
}

defineExpose({ open })
</script>

<style scoped>
.wizard-form { max-height: 60vh; overflow-y: auto; padding-right: 10px; }
.sec-title { margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #ebeef5; font-size: 14px; color: #303133; }
.mode-switch { margin-bottom: 12px; display: block; }
.mode-switch .el-radio { margin-right: 20px; }
.batch-table { margin-bottom: 6px; }
.full-width { width: 100%; }
.stock-info { color: #67c23a; font-size: 13px; line-height: 32px; }
</style>
