<template>
  <div class="venue-page">
    <div class="page-header">
      <h2 class="page-title">场地预约</h2>
    </div>

    <div class="category-tags">
      <button v-for="c in venueCats" :key="c" class="cat-tag" :class="{ active: venueCat === c }" @click="venueCat = c">{{ c }}</button>
    </div>

    <div class="venue-grid" v-if="filteredVenues.length > 0">
      <div v-for="v in filteredVenues" :key="v.id" class="venue-card">
        <div class="venue-name">{{ v.name }}</div>
        <div class="venue-location">📍 {{ v.location }}</div>
        <div class="venue-info">
          <span>容纳 {{ v.capacity }} 人</span>
          <span v-if="v.facilities">| {{ v.facilities }}</span>
        </div>
        <div class="venue-footer">
          <el-tag :type="v.booked ? 'warning' : 'success'" size="small" round>{{ v.booked ? '已占用' : '空闲' }}</el-tag>
          <el-button size="small" type="primary" @click="book(v)">预约</el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无场地数据" />

    <div class="quick-links">
      <router-link to="/" class="ql">首页看板 →</router-link>
      <router-link to="/activities" class="ql">活动管理 →</router-link>
      <router-link to="/resources" class="ql">物资管理 →</router-link>
    </div>

    <el-dialog v-model="bookDialog" title="场地预约" width="400px">
      <el-form :model="bf" label-width="90px">
        <el-form-item label="场地"><el-input :model-value="bookedVenue?.name" disabled/></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="bf.date" type="date"/></el-form-item>
        <el-form-item label="开始时间"><el-time-select v-model="bf.start" :max-time="bf.end" placeholder="开始" start="08:00" step="00:30" end="22:00"/></el-form-item>
        <el-form-item label="结束时间"><el-time-select v-model="bf.end" :min-time="bf.start" placeholder="结束" start="08:00" step="00:30" end="22:00"/></el-form-item>
        <el-form-item label="用途"><el-input v-model="bf.purpose" type="textarea"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="bookDialog=false">取消</el-button><el-button type="primary" @click="doBook">提交预约</el-button></template>
    </el-dialog>

    <VenueBookWizard ref="venueWizardRef" @refresh="fetch" />
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import VenueBookWizard from './VenueBookWizard.vue'
const venueWizardRef = ref(null)
const venues = ref([]); const bookDialog = ref(false); const bookedVenue = ref(null)
const user = JSON.parse(localStorage.getItem('user')||'{}')
const bf = reactive({ date:'', start:'', end:'', purpose:'' })
const fetch = async () => { const r = await request.get('/venues'); venues.value = r.data }
const book = (v) => { venueWizardRef.value.open() }
const doBook = async () => {
  await request.post('/venue/booking', {
    venueId: bookedVenue.value.id, bookingDate: bf.date, startTime: bf.start, endTime: bf.end, purpose: bf.purpose })
  ElMessage.success('预约申请已提交'); bookDialog.value = false
}
const venueCats = ['全部', '活动中心', '报告厅', '运动场', '多功能厅']; const venueCat = ref('全部');
const filteredVenues = computed(() => venueCat.value === '全部' ? venues.value : venues.value.filter(v => (v.location||'').includes(venueCat.value) || (v.name||'').includes(venueCat.value)));
onMounted(fetch)
</script>
<style scoped>
.venue-page { max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 700; color: #222; margin: 0; }

.category-tags { display: flex; gap: 8px; margin-bottom: 16px; }
.cat-tag { padding: 5px 14px; border-radius: 20px; border: 1px solid #e0e0e0; background: #fff; color: #666; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.cat-tag:hover { background: #F6E9E2; border-color: #E0A08A; }
.cat-tag.active { background: #C96442; color: #fff; border-color: #C96442; }

.venue-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.venue-card {
  background: #fff; border-radius: 8px; padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); transition: all 0.2s;
  display: flex; flex-direction: column; gap: 10px;
}
.venue-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
.venue-name { font-size: 16px; font-weight: 700; color: #222; }
.venue-location { font-size: 12px; color: #888; }
.venue-info { font-size: 13px; color: #666; }
.venue-footer { display: flex; align-items: center; justify-content: space-between; margin-top: auto; }

.quick-links { display: flex; gap: 10px; margin-top: 20px; flex-wrap: wrap; }
.ql { font-size: 12px; color: #C96442; background: #F6E9E2; padding: 4px 12px; border-radius: 12px; text-decoration: none; transition: all 0.2s; }
.ql:hover { background: #F1D0C5; transform: translateY(-1px); }
</style>
