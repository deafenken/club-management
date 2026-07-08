<template>
  <div>
    <el-card style="margin-bottom:16px">
      <template #header><span style="font-weight:bold;font-size:16px">AI 智能活动策划</span>
        <el-tag type="warning" style="margin-left:8px">DeepSeek</el-tag></template>
      <el-input v-model="requirement" type="textarea" :rows="3"
        placeholder="描述你的活动需求，例如：我想在10月中旬办一场面向全校新生的迎新晚会，预计300人参加，需要舞台表演和互动游戏环节..."/>
      <div style="margin-top:12px;display:flex;gap:8px;flex-wrap:wrap">
        <el-button v-for="tpl in templates" :key="tpl.label" size="small" @click="requirement=tpl.text">{{tpl.label}}</el-button>
      </div>
      <div style="margin-top:16px">
        <el-button type="primary" @click="generate" :loading="generating" size="large">
          <el-icon><MagicStick/></el-icon> {{ generating ? 'AI 策划中...' : '开始智能策划' }}</el-button>
        <span v-if="elapsed" style="margin-left:12px;color:#999">总耗时: {{elapsed}}ms</span>
      </div>
    </el-card>

    <!-- 结果展示 -->
    <el-card v-if="result">
      <div class="markdown-body" v-html="renderedPRD" style="max-height:700px;overflow-y:auto;padding:16px;background:#fafafa;border-radius:8px"/>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import request from '../api/request'

const requirement = ref('')
const generating = ref(false)
const elapsed = ref(0)
const result = ref(null)
const renderedPRD = ref('')

const templates = [
  { label:'迎新晚会', text:'我想在10月中旬办一场面向全校新生的迎新晚会，预计300人参加，地点在大学生活动中心，需要舞台表演、灯光音响、互动抽奖环节，预算约5000元' },
  { label:'编程比赛', text:'计算机协会计划举办一场ACM校内选拔赛，需要机房场地、50台电脑、比赛题目和奖品，预计80人参加，时间在11月' },
  { label:'志愿活动', text:'志愿者协会要去社区做一次环保宣传活动，需要展板、宣传册、志愿者马甲，预计20名志愿者参加' },
]

const generate = async () => {
  if (!requirement.value.trim()) { ElMessage.warning('请先输入活动需求'); return }
  generating.value = true; result.value = null
  const user = JSON.parse(localStorage.getItem('user') || '{}')

  try {
    const start = Date.now()
    const res = await request.post('/ai/plan', { requirement: requirement.value }, { timeout: 170000 })
    elapsed.value = Date.now() - start
    if (res.code === 200) {
      result.value = res.data
      if (res.data.prd) renderedPRD.value = marked.parse(res.data.prd)
      ElMessage.success(`策划完成！耗时 ${elapsed.value}ms`)
    }
  } catch(e) {
    // 拦截器已显示具体错误
  } finally { generating.value = false }
}
</script>

<style scoped>
.markdown-body :deep(h2) { color:#303133; border-bottom:2px solid #409EFF; padding-bottom:6px; margin-top:20px }
.markdown-body :deep(h3) { color:#606266; margin-top:16px }
.markdown-body :deep(table) { border-collapse:collapse; width:100% }
.markdown-body :deep(th),.markdown-body :deep(td) { border:1px solid #ddd; padding:8px; text-align:left }
.markdown-body :deep(th) { background:#f5f7fa }
.markdown-body :deep(ul),.markdown-body :deep(ol) { padding-left:20px }
</style>
