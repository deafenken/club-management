<template>
  <el-dialog v-model="visible" title="发布新活动" width="780px" top="3vh" destroy-on-close @opened="onOpen">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="wizard-form">
      <h4 class="sec-title">基本信息</h4>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="活动标题" prop="title"><el-input v-model="form.title" maxlength="100" show-word-limit/></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="主办社团" prop="clubId">
            <el-select v-model="form.clubId" class="full-width"><el-option v-for="c in myClubs" :key="c.id" :label="c.name" :value="c.id"/></el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="活动分类" prop="category"><el-select v-model="form.category" class="full-width"><el-option v-for="c in cats" :key="c" :label="c" :value="c"/></el-select></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="面向对象" prop="targetAudience"><el-select v-model="form.targetAudience" class="full-width"><el-option v-for="a in audiences" :key="a" :label="a" :value="a"/></el-select></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="活动地点" prop="location"><el-input v-model="form.location"/></el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" class="full-width" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" class="full-width" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="报名截止" prop="regDeadline"><el-date-picker v-model="form.regDeadline" type="datetime" class="full-width" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="最大人数"><el-input-number v-model="form.maxParticipants" :min="1" :max="9999" style="width:100%"/></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="主办方" prop="organizer"><el-input v-model="form.organizer"/></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="协办方"><el-input v-model="form.coOrganizer"/></el-form-item>
        </el-col>
      </el-row>
      <h4 class="sec-title">安全与附件</h4>
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item label="安全联系人"><el-input v-model="form.safetyContact"/></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="安全电话" prop="safetyPhone"><el-input v-model="form.safetyPhone" maxlength="11"/></el-form-item></el-col>
      </el-row>
      <el-form-item label="安全预案"><el-input v-model="form.safetyPlan" type="textarea" :rows="2"/></el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="策划方案URL"><el-input v-model="form.planFileUrl" placeholder="策划方案链接"/></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="海报URL"><el-input v-model="form.posterUrl" placeholder="宣传海报链接"/></el-form-item></el-col>
      </el-row>
      <h4 class="sec-title">预算与收费</h4>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="预算金额"><el-input-number v-model="form.budgetAmount" :min="0" :precision="2" :step="100" style="width:100%"/> 元</el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="经费来源"><el-select v-model="form.fundSource" class="full-width"><el-option v-for="s in fundSources" :key="s" :label="s" :value="s"/></el-select></el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="是否收费"><el-switch v-model="form.isFee"/></el-form-item>
        </el-col>
        <el-col :span="8" v-if="form.isFee">
          <el-form-item label="收费标准"><el-input-number v-model="form.feeAmount" :min="0" :precision="2" style="width:100%"/> 元/人</el-form-item>
        </el-col>
      </el-row>
      <h4 class="sec-title">校外嘉宾 <el-tag size="small" type="info">选填</el-tag></h4>
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item label="嘉宾姓名"><el-input v-model="form.guestName"/></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="嘉宾单位"><el-input v-model="form.guestOrg"/></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="资质证明"><el-input v-model="form.guestCredential" placeholder="资质证明URL"/></el-form-item></el-col>
      </el-row>
      <h4 class="sec-title">
        校外活动 <el-switch v-model="form.isOffCampus" size="small" class="ml-8"/>
      </h4>
      <template v-if="form.isOffCampus">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="校外地点"><el-input v-model="form.offCampusLocation"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="交通方式"><el-input v-model="form.offCampusTransport"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="报备说明"><el-input v-model="form.offCampusFiling" type="textarea" :rows="2"/></el-form-item>
      </template>
      <h4 class="sec-title resource-sec">资源联动</h4>
      <el-row :gutter="16">
        <el-col :span="8">
          <div class="resource-item"><el-switch v-model="form.needVenue" size="small"/><span class="ml-8">联动场地</span></div>
          <el-select v-if="form.needVenue" v-model="form.linkedVenueBookingId" class="full-width mt-4" placeholder="选择已批准场地" clearable>
            <el-option v-for="v in venueBookings" :key="v.id" :label="v.venueName + ' ' + v.bookingDate" :value="v.id"/>
          </el-select>
        </el-col>
        <el-col :span="8">
          <div class="resource-item"><el-switch v-model="form.needResource" size="small"/><span class="ml-8">联动物资</span></div>
          <el-select v-if="form.needResource" v-model="form.linkedResourceBorrowId" class="full-width mt-4" placeholder="选择已批准物资" clearable>
            <el-option v-for="r in resourceBorrows" :key="r.id" :label="r.itemName + ' x' + r.quantity" :value="r.id"/>
          </el-select>
        </el-col>
        <el-col :span="8">
          <div class="resource-item"><el-switch v-model="form.needFund" size="small"/><span class="ml-8">联动经费</span></div>
          <el-select v-if="form.needFund" v-model="form.linkedFundRecordId" class="full-width mt-4" placeholder="选择已批准经费" clearable>
            <el-option v-for="f in fundRecords" :key="f.id" :label="'&#165;' + f.amount + ' ' + (f.description || '')" :value="f.id"/>
          </el-select>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
      <el-button type="primary" @click="submit" :loading="submitting">提交申请</el-button>
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

const cats = ['讲座', '比赛', '演出', '志愿', '团建', '其他']
const audiences = ['全校开放', '仅社团内部', '指定学院']
const fundSources = ['校拨款', '社团会费', '校外赞助']

const venueBookings = ref([])
const resourceBorrows = ref([])
const fundRecords = ref([])

const form = reactive({
  title: '', clubId: null, category: '讲座', targetAudience: '全校开放',
  startTime: '', endTime: '', regDeadline: '', location: '',
  maxParticipants: null, organizer: '', coOrganizer: '',
  safetyContact: '', safetyPhone: '', safetyPlan: '',
  planFileUrl: '', posterUrl: '',
  budgetAmount: null, fundSource: '校拨款',
  isFee: false, feeAmount: null,
  guestName: '', guestOrg: '', guestCredential: '',
  isOffCampus: false, offCampusLocation: '', offCampusTransport: '', offCampusFiling: '',
  needVenue: false, needResource: false, needFund: false,
  linkedVenueBookingId: null, linkedResourceBorrowId: null, linkedFundRecordId: null
})

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  clubId: [{ required: true, message: '请选择主办社团', trigger: 'change' }],
  category: [{ required: true, message: '请选择活动分类', trigger: 'change' }],
  targetAudience: [{ required: true, message: '请选择面向对象', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  regDeadline: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  organizer: [{ required: true, message: '请输入主办方', trigger: 'blur' }],
  safetyPhone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const fetchClubs = async () => {
  try {
    const role = JSON.parse(localStorage.getItem('user') || '{}').role
    const url = role === 'ADMIN' ? '/clubs?page=1&size=200' : '/my-clubs'
    const res = await request.get(url)
    myClubs.value = res.data?.records || res.data || []
  } catch (e) {}
}

const fetchLinkages = async () => {
  try {
    const [vRes, rRes, fRes] = await Promise.all([
      request.get('/venue/bookings?page=1&size=200&status=APPROVED'),
      request.get('/resource/borrows?page=1&size=200&status=APPROVED'),
      request.get('/funds?page=1&size=200&status=APPROVED')
    ])
    venueBookings.value = vRes.data?.records || []
    resourceBorrows.value = rRes.data?.records || []
    fundRecords.value = fRes.data?.records || []
  } catch (e) {}
}

const open = () => { visible.value = true }

const onOpen = async () => {
  await Promise.all([fetchClubs(), fetchLinkages()])
  try {
    const res = await request.get('/draft/load?appType=ACTIVITY')
    if (res.data && res.data.formData) {
      try {
        const d = JSON.parse(res.data.formData)
        Object.assign(form, d)
        ElMessage.info('已恢复上次草稿')
      } catch (e) {}
    }
  } catch (e) {}
}

const saveDraft = async () => {
  saving.value = true
  try {
    await request.post('/draft/save', {
      appType: 'ACTIVITY', stepIndex: '0',
      formData: JSON.stringify({ ...form })
    })
    ElMessage.success('草稿已保存')
  } catch (e) {}
  saving.value = false
}

const submit = async () => {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    await request.post('/activity/full', {
      ...form,
      // 后端这些字段是 Integer(0/1)，Jackson 不接受布尔值，需显式转换
      isFee: form.isFee ? 1 : 0,
      needVenue: form.needVenue ? 1 : 0,
      needResource: form.needResource ? 1 : 0,
      needFund: form.needFund ? 1 : 0
    })
    ElMessage.success('活动申请已提交！')
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
.resource-sec { background: #f5f7fa; padding: 10px 12px; border-radius: 6px; margin: 10px 0; }
.resource-item { display: flex; align-items: center; margin-bottom: 4px; }
.full-width { width: 100%; }
.ml-8 { margin-left: 8px; }
.mt-4 { margin-top: 4px; }
</style>
