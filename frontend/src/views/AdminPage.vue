<template>
  <div class="admin-page" v-if="user.role === 'ADMIN'">
    <el-tabs v-model="tab">
      <el-tab-pane label="社团审批" name="clubs">
        <el-table :data="pendingClubs">
          <template #empty>暂无数据</template>
          <el-table-column prop="name" label="社团名称"/>
          <el-table-column prop="presidentName" label="申请人" width="100"/>
          <el-table-column prop="category" label="分类" width="100"/>
          <el-table-column prop="description" label="简介"/>
          <el-table-column label="操作" width="200">
            <template #default="{row}">
              <el-button size="small" type="success" @click="approveClub(row.id,1)">通过</el-button>
              <el-button size="small" type="danger" @click="approveClub(row.id,2)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="活动审批" name="activities">
        <el-table :data="pendingActivities">
          <template #empty>暂无数据</template>
          <el-table-column prop="title" label="活动名称" min-width="160"/>
          <el-table-column prop="category" label="分类" width="80"/>
          <el-table-column prop="location" label="地点" width="120"/>
          <el-table-column prop="startTime" label="开始时间" width="160"/>
          <el-table-column label="操作" width="200">
            <template #default="{row}">
              <el-button size="small" type="success" @click="approveActivity(row.id,'approve')">通过</el-button>
              <el-button size="small" type="danger" @click="approveActivity(row.id,'reject')">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="场地预约审批" name="venues">
        <el-table :data="venueBookings">
          <template #empty>暂无数据</template>
          <el-table-column prop="venueName" label="场地" width="120"/>
          <el-table-column prop="userName" label="申请人" width="100"/>
          <el-table-column prop="bookingDate" label="日期" width="120"/>
          <el-table-column label="时段" width="160">
            <template #default="{row}">{{row.startTime}} ~ {{row.endTime}}</template>
          </el-table-column>
          <el-table-column prop="purpose" label="用途" min-width="160"/>
          <el-table-column label="状态" width="100">
            <template #default="{row}">
              <el-tag :type="statusTag(row.status)">{{ {PENDING:'待审批', APPROVED:'已通过', REJECTED:'已驳回'}[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" v-if="hasPending(venueBookings)">
            <template #default="{row}">
              <el-button size="small" type="success" v-if="row.status==='PENDING'" @click="approveVenue(row.id,true)">通过</el-button>
              <el-button size="small" type="danger" v-if="row.status==='PENDING'" @click="approveVenue(row.id,false)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="物资借用审批" name="resources">
        <el-table :data="resourceBorrows">
          <template #empty>暂无数据</template>
          <el-table-column prop="itemName" label="物资" width="120"/>
          <el-table-column prop="userName" label="借用人" width="100"/>
          <el-table-column prop="quantity" label="数量" width="80"/>
          <el-table-column prop="borrowDate" label="借用日期" width="120"/>
          <el-table-column prop="planReturnDate" label="预计归还" width="120"/>
          <el-table-column label="状态" width="100">
            <template #default="{row}">
              <el-tag :type="statusTag(row.status)">{{ {PENDING:'待审批', APPROVED:'已通过', REJECTED:'已驳回'}[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" v-if="hasPending(resourceBorrows)">
            <template #default="{row}">
              <el-button size="small" type="success" v-if="row.status==='PENDING'" @click="approveBorrow(row.id,true)">通过</el-button>
              <el-button size="small" type="danger" v-if="row.status==='PENDING'" @click="approveBorrow(row.id,false)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users">
          <template #empty>暂无数据</template>
          <el-table-column prop="username" label="用户名"/>
          <el-table-column prop="realName" label="姓名"/>
          <el-table-column prop="role" label="角色"/>
          <el-table-column prop="college" label="学院"/>
          <el-table-column label="状态">
            <template #default="{row}">
              <el-tag :type="row.status===1?'success':'danger'">{{row.status===1?'正常':'禁用'}}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{row}">
              <el-button v-if="row.status===1" size="small" type="danger" @click="toggleUser(row.id,0)">禁用</el-button>
              <el-button v-else size="small" type="success" @click="toggleUser(row.id,1)">启用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="经费管理" name="funds">
        <el-table :data="funds">
          <template #empty>暂无数据</template>
          <el-table-column prop="clubId" label="社团ID" width="80"/>
          <el-table-column prop="type" label="类型" width="80">
            <template #default="{row}">
              <el-tag :type="row.type==='INCOME'?'success':'danger'">{{row.type==='INCOME'?'收入':'支出'}}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="100"/>
          <el-table-column prop="category" label="分类" width="100"/>
          <el-table-column prop="description" label="说明"/>
          <el-table-column label="状态" width="100">
            <template #default="{row}">
              <el-tag :type="statusTag(row.status)">{{ {PENDING:'待审批', APPROVED:'已通过', REJECTED:'已驳回'}[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{row}">
              <el-button size="small" type="success" v-if="row.status==='PENDING'" @click="approveFund(row.id,true)">通过</el-button>
              <el-button size="small" type="danger" v-if="row.status==='PENDING'" @click="approveFund(row.id,false)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="公告管理" name="announcements">
        <el-button type="primary" size="small" class="ann-btn" @click="annDialog=true">发布公告</el-button>
        <el-table :data="announcements">
          <template #empty>暂无数据</template>
          <el-table-column label="置顶" width="70">
            <template #default="{row}">
              <el-switch :model-value="row.isTop===1" @change="toggleTop(row)" size="small"/>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题"/>
          <el-table-column prop="createTime" label="发布时间" width="180"/>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 发布公告对话框 -->
    <el-dialog v-model="annDialog" title="发布公告" width="400px">
      <el-form :model="ann">
        <el-form-item label="标题"><el-input v-model="ann.title"/></el-form-item>
        <el-form-item label="内容"><el-input v-model="ann.content" type="textarea" :rows="4"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="annDialog=false">取消</el-button>
        <el-button type="primary" @click="postAnn">发布</el-button>
      </template>
    </el-dialog>
  </div>
  <div v-else class="forbidden-page">
    <el-result icon="warning" title="403 无权限访问" sub-title="仅管理员可访问系统设置页面">
      <template #extra><el-button type="primary" @click="$router.push('/')">返回首页</el-button></template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'

const user = JSON.parse(localStorage.getItem('user')||'{}')

const tab = ref('clubs')
const pendingClubs = ref([]); const pendingActivities = ref([])
const users = ref([]); const announcements = ref([])
const venueBookings = ref([]); const resourceBorrows = ref([])
const funds = ref([]); const annDialog = ref(false)
const ann = reactive({ title:'', content:'' })

const hasPending = (list) => list.some(i => i.status === 'PENDING')
const statusTag = (s) => ({ 'PENDING':'warning','APPROVED':'success','REJECTED':'danger' }[s]||'info')

const fetch = async () => {
  // 待审批社团 (status=0)
  const clubs = await request.get('/clubs', { params: { page:1, size:100, status:0 } })
  pendingClubs.value = (clubs.data.records||[]).filter(c => c.status === 0)
  // 用户
  const u = await request.get('/users', { params: { page:1, size:100 } })
  users.value = (u.data.records||[])
  // 公告
  const a = await request.get('/announcements')
  announcements.value = a.data||[]
  // 场地预约
  const v = await request.get('/venue/bookings')
  venueBookings.value = v.data.records||[]
  // 物资借用
  const r = await request.get('/resource/borrows')
  resourceBorrows.value = r.data.records||[]
  // 待审批活动
  const acts = await request.get('/activities', { params: { page:1, size:100, status:'PENDING' } })
  pendingActivities.value = acts.data.records||[]
  // 经费记录
  const f = await request.get('/funds')
  funds.value = f.data.records||[]
}

// 多级审批：统一调用 /api/approve
const doMultiApprove = async (appType, businessId, action) => {
  let comment = ''
  if (action === 'reject') {
    try {
      const { value } = await ElMessageBox.prompt('请输入驳回理由/修改意见', '驳回申请', {
        confirmButtonText: '确认驳回', cancelButtonText: '取消',
        inputType: 'textarea', inputPlaceholder: '请详细说明驳回原因，以便申请人修改后重新提交'
      })
      comment = value || ''
    } catch { return } // 用户取消
  }
  try {
    const res = await request.put('/approve', { appType, businessId, action, reason: comment, comment })
    ElMessage.success(res.data?.msg || (action==='approve'?'审批通过':'已驳回'))
    fetch()
  } catch(e) {}
}

const approveClub = (id, status) => doMultiApprove('CLUB', id, status===1?'approve':'reject')
const approveActivity = (id, action) => doMultiApprove('ACTIVITY', id, action)
const approveVenue = (id, approve) => doMultiApprove('VENUE', id, approve?'approve':'reject')
const approveBorrow = (id, approve) => doMultiApprove('RESOURCE', id, approve?'approve':'reject')
const approveFund = (id, approve) => doMultiApprove('FUND', id, approve?'approve':'reject')

const toggleUser = async (userId, status) => {
  await request.put('/user/status', { userId, status })
  ElMessage.success(status===1?'用户已启用':'用户已禁用'); fetch()
}

const postAnn = async () => {
  await request.post('/announcement', ann); ElMessage.success('发布成功'); annDialog.value = false; fetch()
}

const toggleTop = async (row) => {
  await request.put(`/announcement/${row.id}/top`)
  row.isTop = row.isTop === 1 ? 0 : 1
  ElMessage.success(row.isTop===1?'已置顶':'已取消置顶')
}

onMounted(fetch)
watch(tab, (t) => {
  if (t === 'activities') {
    request.get('/activities', { params: { page:1, size:100, status:'PENDING' } }).then(r => { if (r.code===200) pendingActivities.value = r.data.records||[] })
  }
  if (t === 'venues') {
    request.get('/venue/bookings').then(r => { if (r.code===200) venueBookings.value = r.data.records||[] })
  }
  if (t === 'resources') {
    request.get('/resource/borrows').then(r => { if (r.code===200) resourceBorrows.value = r.data.records||[] })
  }
})
</script>

<style scoped>
.admin-page :deep(.el-tabs__item.is-active) {
  font-weight: 700;
  color: #C96442;
}

.admin-page :deep(.el-tabs__active-bar) {
  background-color: #C96442;
  height: 3px;
}

.admin-page :deep(.el-tabs__item) {
  color: #606266;
}

.admin-page :deep(.el-tabs__item:hover) {
  color: #C96442;
}

.admin-page :deep(.el-table) {
  background: #fff;
  font-size: 14px;
}

.admin-page :deep(.el-table th.el-table__cell) {
  background-color: #fafafa;
  color: #303133;
  font-weight: 600;
}

.admin-page :deep(.el-table .el-table__empty-block) {
  color: #909399;
}

.ann-btn {
  margin-bottom: 8px;
}
</style>
