<template>
  <el-dialog v-model="visible" title="申请创建社团" width="780px" top="3vh" destroy-on-close @opened="onOpen">
    <div class="wizard-body">
      <el-steps :active="step" align-center class="wizard-steps">
        <el-step title="基础信息" description="背景与定位" />
        <el-step title="负责人与教师" description="社长/发起人/指导教师" />
        <el-step title="运营规划" description="章程/活动/经费" />
        <el-step title="合规承诺" description="确认并提交" />
      </el-steps>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="wizard-form">

        <!-- Step 1: 基础信息 -->
        <template v-if="step === 0">
          <h4 class="step-title">社团基础信息</h4>
          <el-form-item label="社团名称" prop="name"><el-input v-model="form.name" maxlength="50" show-word-limit placeholder="请输入社团全称"/></el-form-item>
          <el-form-item label="分类" prop="category"><el-select v-model="form.category" class="full-width"><el-option v-for="c in cats" :key="c" :label="c" :value="c"/></el-select></el-form-item>
          <el-form-item label="成立背景" prop="foundingBackground"><el-input v-model="form.foundingBackground" type="textarea" :rows="3" placeholder="说明校内同类社团缺口、学生需求"/></el-form-item>
          <el-form-item label="差异化定位" prop="differentiation"><el-input v-model="form.differentiation" type="textarea" :rows="3" placeholder="与现有社团区分特色，避免重复社团"/></el-form-item>
          <el-form-item label="预计招新规模"><el-input-number v-model="form.recruitScale" :min="1" :max="500" /> 人</el-form-item>
          <el-form-item label="招生学院范围"><el-input v-model="form.recruitColleges" placeholder="如：全校所有学院 / 限定计算机学院等"/></el-form-item>
          <el-form-item label="社团简介" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit/></el-form-item>
        </template>

        <!-- Step 2: 负责人与指导教师 -->
        <template v-if="step === 1">
          <h4 class="step-title">首任社长信息</h4>
          <el-row :gutter="16">
            <el-col :span="8"><el-form-item label="社长姓名" prop="presidentName"><el-input v-model="form.presidentName"/></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="学号" prop="presidentStudentId"><el-input v-model="form.presidentStudentId"/></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="年级"><el-select v-model="form.presidentGrade" class="full-width"><el-option v-for="g in ['大一','大二','大三','大四','研一','研二','研三']" :key="g" :label="g" :value="g"/></el-select></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="学院专业" prop="presidentCollege"><el-input v-model="form.presidentCollege"/></el-form-item></el-col>
            <el-col :span="6"><el-form-item label="手机号" prop="presidentPhone"><el-input v-model="form.presidentPhone" maxlength="11"/></el-form-item></el-col>
            <el-col :span="6"><el-form-item label="QQ"><el-input v-model="form.presidentQq"/></el-form-item></el-col>
          </el-row>

          <h4 class="step-title">核心发起人（至少8人）</h4>
          <el-table :data="initiators" border size="small">
            <el-table-column label="姓名"><template #default="{row,$index}"><el-input v-model="initiators[$index].name" size="small"/></template></el-table-column>
            <el-table-column label="学号"><template #default="{row,$index}"><el-input v-model="initiators[$index].studentId" size="small"/></template></el-table-column>
            <el-table-column label="分工"><template #default="{row,$index}"><el-select v-model="initiators[$index].role" size="small" class="full-width"><el-option v-for="r in initiatorRoles" :key="r" :label="r" :value="r"/></el-select></template></el-table-column>
            <el-table-column width="70"><template #default="{row,$index}"><el-button size="small" type="danger" link @click="initiators.splice($index,1)" :disabled="initiators.length<=8">删除</el-button></template></el-table-column>
          </el-table>
          <el-button size="small" type="primary" link @click="initiators.push({name:'',studentId:'',role:'宣传'})">+ 添加发起人</el-button>
          <p class="hint" v-if="initiators.length < 8">⚠ 至少需要8名发起人（当前{{initiators.length}}人）</p>

          <h4 class="step-title">指导教师信息</h4>
          <el-row :gutter="16">
            <el-col :span="8"><el-form-item label="姓名" prop="advisorName"><el-input v-model="form.advisorName"/></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="工号" prop="advisorTeacherId"><el-input v-model="form.advisorTeacherId"/></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="职务"><el-input v-model="form.advisorTitle"/></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="所属院系" prop="advisorDept"><el-input v-model="form.advisorDept"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="办公电话" prop="advisorPhone"><el-input v-model="form.advisorPhone"/></el-form-item></el-col>
          </el-row>
          <el-form-item label="教师同意书"><el-input v-model="form.advisorAgreementUrl" placeholder="上传指导教师签字同意书链接"/><el-button size="small" link type="primary" class="tpl-btn" @click="showTemplate('charter')">📥 章程模板下载</el-button></el-form-item>
        </template>

        <!-- Step 3: 运营与活动规划 -->
        <template v-if="step === 2">
          <h4 class="step-title">运营与活动规划</h4>
          <el-form-item label="社团章程"><el-input v-model="form.charterText" type="textarea" :rows="5" maxlength="3000" show-word-limit placeholder="需覆盖组织架构、会员管理、财务制度、活动安全规范"/></el-form-item>
          <el-form-item label="章程文件"><el-input v-model="form.charterFileUrl" placeholder="上传 Word/PDF 章程文件链接"/></el-form-item>
          <el-form-item label="学期活动方案"><el-input v-model="form.semesterPlan" type="textarea" :rows="4" placeholder="常规月度活动、品牌特色活动"/></el-form-item>
          <el-form-item label="安全应急预案"><el-input v-model="form.clubSafetyPlan" type="textarea" :rows="3" placeholder="活动安全应急预案"/></el-form-item>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="会费标准"><el-input v-model="form.feeStandard" placeholder="如：50元/人/学年"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="年度预算"><el-input-number v-model="form.annualBudget" :min="0" :precision="2" :step="100" style="width:100%"/> 元</el-form-item></el-col>
          </el-row>
        </template>

        <!-- Step 4: 合规承诺与附件汇总 -->
        <template v-if="step === 3">
          <h4 class="step-title">合规承诺书</h4>
          <el-checkbox-group v-model="commitChecks" class="commit-list">
            <el-checkbox label="1">自愿遵守本校学生社团管理条例</el-checkbox>
            <el-checkbox label="2">社团不开展商业盈利、违规校外活动</el-checkbox>
            <el-checkbox label="3">社团负责人全权承担活动安全、管理责任</el-checkbox>
          </el-checkbox-group>
          <p v-if="commitChecks.length < 3" class="hint">⚠ 请勾选全部3项承诺方可提交</p>

          <h4 class="step-title">附件汇总</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="教师同意书">{{form.advisorAgreementUrl || '未上传'}}</el-descriptions-item>
            <el-descriptions-item label="社团章程文件">{{form.charterFileUrl || '未上传'}}</el-descriptions-item>
            <el-descriptions-item label="学期活动方案">{{form.semesterPlan || '未填写'}}</el-descriptions-item>
            <el-descriptions-item label="核心发起人">{{initiators.length}}人</el-descriptions-item>
          </el-descriptions>
        </template>

      </el-form>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-if="step === 3" type="primary" @click="submit" :loading="submitting" :disabled="commitChecks.length < 3">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const emit = defineEmits(['refresh'])
const visible = ref(false)
const step = ref(0)
const saving = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const cats = ['学术科技','文化艺术','体育竞技','志愿服务','创新创业']
const initiatorRoles = ['宣传','财务','活动策划','外联','后勤','秘书','技术','安全']

const form = reactive({
  name:'', category:'学术科技', description:'',
  foundingBackground:'', differentiation:'', recruitScale:30, recruitColleges:'',
  presidentName:'', presidentStudentId:'', presidentCollege:'', presidentGrade:'',
  presidentPhone:'', presidentQq:'',
  advisorName:'', advisorTeacherId:'', advisorDept:'', advisorPhone:'', advisorTitle:'',
  advisorAgreementUrl:'',
  charterText:'', charterFileUrl:'', semesterPlan:'', clubSafetyPlan:'',
  feeStandard:'', annualBudget:null,
  commitmentSigned:0, attachments:''
})

const initiators = ref([
  {name:'',studentId:'',role:'宣传'},{name:'',studentId:'',role:'财务'},
  {name:'',studentId:'',role:'活动策划'},{name:'',studentId:'',role:'外联'},
  {name:'',studentId:'',role:'后勤'},{name:'',studentId:'',role:'秘书'},
  {name:'',studentId:'',role:'技术'},{name:'',studentId:'',role:'安全'}
])
const commitChecks = ref([])

const rules = {
  name:[{required:true,message:'请输入社团名称',trigger:'blur'}],
  category:[{required:true,message:'请选择分类',trigger:'change'}],
  description:[{required:true,message:'请输入社团简介',trigger:'blur'}],
  foundingBackground:[{required:true,message:'请输入社团成立背景',trigger:'blur'}],
  differentiation:[{required:true,message:'请输入社团差异化定位',trigger:'blur'}],
  presidentName:[{required:true,message:'请输入社长姓名',trigger:'blur'}],
  presidentStudentId:[{required:true,message:'请输入社长学号',trigger:'blur'}],
  presidentPhone:[{required:true,pattern:/^1[3-9]\d{9}$/,message:'手机号格式不正确',trigger:'blur'}],
  advisorName:[{required:true,message:'请输入指导教师姓名',trigger:'blur'}],
  advisorTeacherId:[{required:true,message:'请输入指导教师工号',trigger:'blur'}],
  advisorDept:[{required:true,message:'请输入教师所属院系',trigger:'blur'}],
  advisorPhone:[{required:true,message:'请输入教师办公电话',trigger:'blur'}],
}

const open = () => { visible.value = true }
const onOpen = async () => {
  step.value = 0
  try {
    const res = await request.get('/draft/load?appType=CLUB')
    if (res.data && res.data.formData) {
      try {
        const d = JSON.parse(res.data.formData)
        Object.assign(form, d)
        step.value = res.data.stepIndex || 0
        ElMessage.info('已恢复上次草稿')
      } catch(e) {}
    }
  } catch(e) {}
}

const nextStep = async () => {
  try { await formRef.value?.validate() } catch { return }
  step.value++
}

const saveDraft = async () => {
  saving.value = true
  try {
    await request.post('/draft/save', {
      appType: 'CLUB',
      stepIndex: String(step.value),
      formData: JSON.stringify({...form})
    })
    ElMessage.success('草稿已保存')
  } catch(e) {}
  saving.value = false
}

const submit = async () => {
  if (initiators.value.length < 8) { ElMessage.warning('至少需要8名核心发起人'); return }
  if (commitChecks.value.length < 3) { ElMessage.warning('请勾选全部合规承诺'); return }
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    const data = {...form,
      commitmentSigned: commitChecks.value.length >= 3 ? 1 : 0,
      initiators: JSON.stringify(initiators.value),
      attachments: JSON.stringify({
        advisorAgreement: form.advisorAgreementUrl,
        charter: form.charterFileUrl,
        plan: form.semesterPlan
      })
    }
    await request.post('/club/full', data)
    ElMessage.success('社团申请已提交！请等待多级审批')
    visible.value = false
    emit('refresh')
  } catch(e) {}
  submitting.value = false
}

const showTemplate = async (type) => {
  try {
    const res = await request.get(`/template-info/${type}`)
    ElMessage.info(res.data?.info || '模板信息获取中...')
  } catch(e) { ElMessage.info('模板：包含组织架构、会员管理、财务制度、活动安全规范四章') }
}

defineExpose({ open })
</script>

<style scoped>
.wizard-body { padding: 0 10px; }
.wizard-steps { margin-bottom: 30px; }
.wizard-form { margin-top: 20px; max-height: 55vh; overflow-y: auto; padding-right: 10px; }
.step-title { margin: 18px 0 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; color: #303133; font-size: 15px; }
.full-width { width: 100%; }
.hint { color: #e6a23c; font-size: 13px; margin-top: 4px; }
.tpl-btn { margin-left: 8px; vertical-align: middle; }
.commit-list { display: flex; flex-direction: column; gap: 12px; padding: 8px 0; }
.commit-list :deep(.el-checkbox) { font-size: 14px; height: auto; }
</style>
