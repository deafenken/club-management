<template>
  <div class="login-page">
    <!-- ===== 左侧品牌区 ===== -->
    <section class="brand-pane">
      <div class="brand-decor decor-1"></div>
      <div class="brand-decor decor-2"></div>

      <div class="brand-top">
        <div class="brand-mark">
          <svg viewBox="0 0 32 32" width="22" height="22" fill="none">
            <circle cx="13" cy="12" r="4.6" fill="#fff" opacity=".96"/>
            <path d="M7 24c0-4.6 6-7.4 6-7.4s6 2.8 6 7.4v2.6H7V24z" fill="#fff" opacity=".9"/>
            <circle cx="22" cy="12" r="4.1" fill="#fff" opacity=".85"/>
            <path d="M18 24c0-4.2 4.6-6.6 4.6-6.6s4.6 2.4 4.6 6.6v2.6H18V24z" fill="#fff" opacity=".8"/>
          </svg>
        </div>
        <span class="brand-mark-text">CLUB HUB</span>
      </div>

      <div class="brand-body">
        <h1 class="brand-title font-display">高校社团<br/>综合管理平台</h1>
        <p class="brand-tagline">社团活动 · 智能审批 · 资源调度</p>
        <ul class="brand-points">
          <li><span class="dot"></span>五类申请统一表单 + 多级审批流</li>
          <li><span class="dot"></span>场地 / 物资 / 经费一站式管理</li>
          <li><span class="dot"></span>AI 智能活动策划，一键生成方案</li>
        </ul>
      </div>

      <p class="brand-foot">© 2026 高校社团管理系统 · Powered by Spring Boot &amp; Vue 3</p>
    </section>

    <!-- ===== 右侧表单区 ===== -->
    <section class="form-pane">
      <div class="form-card">
        <div class="form-head">
          <h2 class="form-title">{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
          <p class="form-sub">{{ isRegister ? '填写信息完成注册' : '请登录以继续使用管理平台' }}</p>
        </div>

        <transition name="fade-slide" mode="out-in">
          <!-- 登录 -->
          <el-form v-if="!isRegister" key="login" :model="form" :rules="loginRules" ref="formRef" @submit.prevent>
            <label class="field-label">账号</label>
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入账号" size="large" @input="clearFieldError('username')">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <label class="field-label">密码</label>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password
                @input="clearFieldError('password')" @keyup.enter="login">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-button type="primary" size="large" class="submit-btn" :disabled="!loginValid" :loading="loading" @click="login">
              登 录
            </el-button>
          </el-form>

          <!-- 注册 -->
          <el-form v-else key="register" :model="regForm" :rules="regRules" ref="regFormRef">
            <div class="reg-grid">
              <el-form-item prop="username"><el-input v-model="regForm.username" placeholder="用户名" size="large" /></el-form-item>
              <el-form-item prop="realName"><el-input v-model="regForm.realName" placeholder="真实姓名" size="large" /></el-form-item>
              <el-form-item prop="password"><el-input v-model="regForm.password" type="password" placeholder="密码" size="large" show-password /></el-form-item>
              <el-form-item prop="rePassword"><el-input v-model="regForm.rePassword" type="password" placeholder="确认密码" size="large" show-password /></el-form-item>
              <el-form-item prop="phone"><el-input v-model="regForm.phone" placeholder="手机号" size="large" /></el-form-item>
              <el-form-item prop="college"><el-input v-model="regForm.college" placeholder="所在学院" size="large" /></el-form-item>
            </div>
            <el-button type="primary" size="large" class="submit-btn" :loading="regLoading" @click="register">注 册</el-button>
          </el-form>
        </transition>

        <div class="form-switch">
          <span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
          <a @click="isRegister = !isRegister; resetForms()">{{ isRegister ? '返回登录' : '立即注册' }}</a>
        </div>

        <p class="form-notice">⚠ 请使用管理员分配的账号登录，勿与他人共享密码</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '../api/request'

const router = useRouter()
const loading = ref(false); const regLoading = ref(false)
const isRegister = ref(false)
const formRef = ref(null); const regFormRef = ref(null)
const form = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', rePassword: '', realName: '', phone: '', college: '' })

const loginValid = computed(() => form.username.trim() !== '' && form.password.trim() !== '')
const clearFieldError = (f) => { formRef.value?.clearValidate(f) }

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const validateRePass = (rule, value, callback) => {
  if (value !== regForm.password) callback(new Error('两次密码不一致'))
  else callback()
}
const regRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  rePassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateRePass, trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  college: [{ required: true, message: '请输入学院', trigger: 'blur' }]
}
const resetForms = () => { formRef.value?.resetFields(); regFormRef.value?.resetFields() }

const login = async () => {
  if (!loginValid.value) { formRef.value?.validate().catch(() => {}); return }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await request.post('/user/login', form)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data))
      ElMessage.success('登录成功！欢迎回来 🎉')
      router.push('/')
    }
  } finally { loading.value = false }
}

const register = async () => {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return
  regLoading.value = true
  try {
    await request.post('/user/register', {
      username: regForm.username, password: regForm.password,
      realName: regForm.realName, phone: regForm.phone, college: regForm.college
    })
    ElMessage.success('注册成功，请登录')
    isRegister.value = false
  } finally { regLoading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: var(--clay-50);
}

/* ---------- 左侧品牌 ---------- */
.brand-pane {
  position: relative;
  flex: 1 1 52%;
  overflow: hidden;
  padding: 56px 64px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  background:
    radial-gradient(120% 90% at 15% 15%, #D9764F 0%, transparent 55%),
    linear-gradient(150deg, #C15F3C 0%, #A54029 62%, #8A3A28 100%);
}
.brand-decor { position: absolute; border-radius: 50%; pointer-events: none; }
.decor-1 { width: 460px; height: 460px; right: -140px; top: -120px;
  background: radial-gradient(circle, rgba(255,255,255,.14), transparent 68%); }
.decor-2 { width: 360px; height: 360px; left: -100px; bottom: -120px;
  background: radial-gradient(circle, rgba(0,0,0,.16), transparent 66%); }

.brand-top { position: relative; z-index: 2; display: flex; align-items: center; gap: 12px; }
.brand-mark {
  width: 40px; height: 40px; border-radius: 11px;
  background: rgba(255,255,255,.16);
  border: 1px solid rgba(255,255,255,.28);
  display: flex; align-items: center; justify-content: center;
}
.brand-mark-text { font-weight: 700; letter-spacing: .22em; font-size: 14px; opacity: .92; }

.brand-body { position: relative; z-index: 2; }
.brand-title {
  font-size: 46px; font-weight: 600; line-height: 1.18; margin: 0 0 20px;
  text-shadow: 0 2px 20px rgba(0,0,0,.12);
}
.brand-tagline { font-size: 16px; opacity: .82; letter-spacing: .08em; margin: 0 0 32px; }
.brand-points { list-style: none; display: flex; flex-direction: column; gap: 15px; }
.brand-points li { display: flex; align-items: center; gap: 12px; font-size: 14.5px; opacity: .9; }
.brand-points .dot { width: 7px; height: 7px; border-radius: 50%; background: #fff; opacity: .85; flex-shrink: 0; }

.brand-foot { position: relative; z-index: 2; font-size: 12.5px; opacity: .6; letter-spacing: .02em; }

/* ---------- 右侧表单 ---------- */
.form-pane {
  flex: 0 0 auto;
  width: min(46%, 560px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.form-card {
  width: 100%;
  max-width: 400px;
  background: var(--surface);
  border: 1px solid var(--clay-200);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
  padding: 42px 40px 32px;
  animation: cardIn .5s cubic-bezier(.2,.7,.3,1);
}
@keyframes cardIn { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: none; } }

.form-head { margin-bottom: 26px; }
.form-title { font-size: 26px; font-weight: 700; color: var(--ink-900); letter-spacing: -.01em; margin: 0 0 6px; }
.form-sub { font-size: 13.5px; color: var(--ink-500); margin: 0; }

.field-label { display: block; font-size: 12.5px; font-weight: 600; color: var(--ink-500); margin: 2px 0 7px; letter-spacing: .02em; }
.form-card :deep(.el-form-item) { margin-bottom: 16px; }
.form-card :deep(.el-input__prefix) { color: var(--ink-400); }

.reg-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }

.submit-btn {
  width: 100%; height: 46px; margin-top: 8px;
  font-size: 15px; font-weight: 650; letter-spacing: .3em;
  border-radius: 11px;
}

.form-switch { text-align: center; margin-top: 20px; font-size: 13.5px; color: var(--ink-500); }
.form-switch a { color: var(--coral); font-weight: 650; cursor: pointer; margin-left: 4px; }
.form-switch a:hover { color: var(--coral-600); text-decoration: underline; }

.form-notice {
  margin-top: 22px; padding-top: 18px; border-top: 1px solid var(--clay-150);
  font-size: 11.5px; color: var(--ink-400); text-align: center; letter-spacing: .01em;
}

/* 表单切换动效 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.fade-slide-enter-from { opacity: 0; transform: translateX(16px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-16px); }

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .brand-pane { display: none; }
  .form-pane { width: 100%; }
}
</style>
