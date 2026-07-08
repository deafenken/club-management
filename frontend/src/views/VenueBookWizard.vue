<template>
  <el-dialog v-model="visible" title="场地预约" width="650px" top="3vh" destroy-on-close @opened="onOpen">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="wizard-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="申请社团" prop="clubId">
            <el-select v-model="form.clubId" class="full-width" placeholder="请选择">
              <el-option v-for="c in myClubs" :key="c.id" :label="c.name" :value="c.id"/>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="场地选择" prop="venueId">
            <el-select v-model="form.venueId" class="full-width" @change="onVenueChange" placeholder="请选择">
              <el-option v-for="v in venues" :key="v.id" :label="v.name + ' (容纳' + v.capacity + '人)'" :value="v.id"/>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="预约日期" prop="bookingDate">
            <el-date-picker v-model="form.bookingDate" type="date" class="full-width" value-format="YYYY-MM-DD"/>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="开始时间" prop="startTime">
            <el-time-select v-model="form.startTime" class="full-width" start="08:00" step="00:30" end="22:00" placeholder="开始"/>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="结束时间" prop="endTime">
            <el-time-select v-model="form.endTime" class="full-width" start="08:00" step="00:30" end="22:00" :min-time="form.startTime" placeholder="结束"/>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="预计人数" prop="expectedAttendees">
            <el-input-number v-model="form.expectedAttendees" :min="1" :max="99999" style="width:100%"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联活动">
            <el-select v-model="form.activityId" class="full-width" clearable placeholder="可选">
              <el-option v-for="a in activities" :key="a.id" :label="a.title" :value="a.id"/>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="使用用途" prop="purpose">
        <el-input v-model="form.purpose" type="textarea" :rows="2" placeholder="请说明活动内容及场地使用方式"/>
      </el-form-item>
      <el-form-item label="设备需求">
        <el-checkbox-group v-model="form.equipmentNeeds">
          <el-checkbox label="音响"/> <el-checkbox label="投影"/> <el-checkbox label="舞台"/> <el-checkbox label="灯光"/> <el-checkbox label="桌椅"/>
        </el-checkbox-group>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="提前进场">
            <el-input-number v-model="form.earlyArrivalMin" :min="15" :max="120" :step="5" style="width:100%"/> 分钟
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="场地复原说明">
            <el-input v-model="form.cleanupNote" placeholder="活动后场地恢复承诺"/>
          </el-form-item>
        </el-col>
      </el-row>
      <h4 class="sec-title">安全信息</h4>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="安全联系人" prop="safetyContact"><el-input v-model="form.safetyContact"/></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="安全电话" prop="safetyPhone"><el-input v-model="form.safetyPhone" maxlength="11"/></el-form-item>
        </el-col>
      </el-row>
      <el-form-item label=" " label-width="110px" prop="agreedDamageClause">
        <el-checkbox v-model="form.agreedDamageClause" size="default">
          我承诺损坏场地设施照价赔偿并遵守场地管理条例
        </el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
      <el-button type="primary" @click="submit" :loading="submitting">提交预约</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const emit = defineEmits(['refresh'])
const visible = ref(false)
const saving = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const myClubs = ref([])
const venues = ref([])
const activities = ref([])
const selectedVenue = ref(null)

const form = reactive({
  clubId: null, venueId: null,
  bookingDate: '', startTime: '', endTime: '',
  expectedAttendees: null, activityId: null,
  purpose: '', equipmentNeeds: [],
  earlyArrivalMin: 15, cleanupNote: '',
  safetyContact: '', safetyPhone: '',
  agreedDamageClause: false
})

const rules = {
  clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
  venueId: [{ required: true, message: '请选择场地', trigger: 'change' }],
  bookingDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  expectedAttendees: [{ required: true, message: '请输入预计人数', trigger: 'blur' }],
  purpose: [{ required: true, message: '请填写使用用途', trigger: 'blur' }],
  safetyContact: [{ required: true, message: '请填写安全联系人', trigger: 'blur' }],
  safetyPhone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  agreedDamageClause: [
    { validator: (rule, value, cb) => value ? cb() : cb(new Error('请同意赔偿条款')), trigger: 'change' }
  ]
}

const fetchClubs = async () => {
  try {
    const role = JSON.parse(localStorage.getItem('user') || '{}').role
    const url = role === 'ADMIN' ? '/clubs?page=1&size=200' : '/my-clubs'
    const res = await request.get(url)
    myClubs.value = res.data?.records || res.data || []
  } catch (e) {}
}

const fetchVenues = async () => {
  try {
    const res = await request.get('/venues')
    venues.value = res.data || []
  } catch (e) {}
}

const fetchActivities = async () => {
  try {
    const res = await request.get('/activities?page=1&size=200&status=APPROVED')
    activities.value = res.data?.records || []
  } catch (e) {}
}

const onVenueChange = (id) => {
  selectedVenue.value = venues.value.find(v => v.id === id) || null
}

const open = () => { visible.value = true }
const onOpen = async () => {
  await Promise.all([fetchClubs(), fetchVenues(), fetchActivities()])
  try {
    const res = await request.get('/draft/load?appType=VENUE')
    if (res.data && res.data.formData) {
      try {
        const d = JSON.parse(res.data.formData)
        Object.assign(form, d)
        if (d.venueId) onVenueChange(d.venueId)
        ElMessage.info('已恢复上次草稿')
      } catch (e) {}
    }
  } catch (e) {}
}

const saveDraft = async () => {
  saving.value = true
  try {
    await request.post('/draft/save', {
      appType: 'VENUE', stepIndex: '0',
      formData: JSON.stringify({ ...form })
    })
    ElMessage.success('草稿已保存')
  } catch (e) {}
  saving.value = false
}

const submit = async () => {
  if (selectedVenue.value && form.expectedAttendees > selectedVenue.value.capacity) {
    ElMessage.warning('预计人数(' + form.expectedAttendees + '人)超出场地容量(' + selectedVenue.value.capacity + '人)，请调整')
    return
  }
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    await request.post('/venue/booking/full', { ...form })
    ElMessage.success('场地预约申请已提交！')
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
.full-width { width: 100%; }
</style>
