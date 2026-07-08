<template>
  <div class="login-container">
    <!-- ===== 背景轮播层 ===== -->
    <div class="bg-carousel" @mouseenter="pauseCarousel" @mouseleave="resumeCarousel">
      <div
        v-for="(img, idx) in bgImages"
        :key="idx"
        class="bg-slide"
        :class="{ active: idx === currentBg }"
        :style="{ backgroundImage: `url(${img})` }"
      ></div>
    </div>

    <!-- 黑色遮罩 rgba(0,0,0,0.23) -->
    <div class="bg-overlay"></div>

    <!-- 光斑 -->
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <!-- 左右切换箭头 -->
    <button class="carousel-arrow carousel-arrow-left" @click="prevBg" v-if="bgImages.length > 1">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
    </button>
    <button class="carousel-arrow carousel-arrow-right" @click="nextBg" v-if="bgImages.length > 1">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
    </button>

    <!-- 轮播圆点 -->
    <div class="carousel-dots" v-if="bgImages.length > 1">
      <span v-for="(img, idx) in bgImages" :key="idx" class="carousel-dot" :class="{ active: idx === currentBg }" @click="goToBg(idx)"></span>
    </div>

    <!-- ===== 左侧品牌区：左上角定位，左8% 上18% ===== -->
    <div class="brand-area">
      <h1 class="brand-title">高校社团管理系统</h1>
      <p class="brand-tagline">社团活动 · 智能管理 · 快乐参与</p>
      <p class="brand-desc">一站式高校社团活动、人员、审批综合管理平台</p>
    </div>

    <!-- ===== 右侧登录卡片：靠右垂直居中，右10% ===== -->
    <div class="login-card">
      <!-- 安全警示 -->
      <p class="security-notice">⚠ 请使用管理员分配的账号登录，勿与他人共享密码</p>

      <h2 class="card-title">欢迎登录</h2>

      <transition name="form-switch" mode="out-in">
        <!-- ===== 登录表单 ===== -->
        <div v-if="!isRegister" key="login" class="form-body">
          <el-form :model="form" :rules="loginRules" ref="formRef" @submit.prevent>
            <el-form-item prop="username">
              <el-input
                v-model="form.username" placeholder="请输入账号" size="large" class="custom-input"
                @input="clearFieldError('username')"
              ><template #prefix><el-icon class="input-icon"><User /></el-icon></template></el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password" type="password" placeholder="请输入密码" size="large"
                class="custom-input" show-password
                @input="clearFieldError('password')" @keyup.enter="login"
              ><template #prefix><el-icon class="input-icon"><Lock /></el-icon></template></el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" class="login-btn" :disabled="!loginValid" @click="login" :loading="loading">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- ===== 注册表单 ===== -->
        <div v-else key="register" class="form-body">
          <el-form :model="regForm" :rules="regRules" ref="regFormRef">
            <el-form-item prop="username">
              <el-input v-model="regForm.username" placeholder="用户名" size="large" class="custom-input">
                <template #prefix><el-icon class="input-icon"><User /></el-icon></template></el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="regForm.password" type="password" placeholder="密码" size="large" class="custom-input" show-password>
                <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template></el-input>
            </el-form-item>
            <el-form-item prop="rePassword">
              <el-input v-model="regForm.rePassword" type="password" placeholder="确认密码" size="large" class="custom-input" show-password>
                <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template></el-input>
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="regForm.realName" placeholder="真实姓名" size="large" class="custom-input">
                <template #prefix><el-icon class="input-icon"><UserFilled /></el-icon></template></el-input>
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="regForm.phone" placeholder="手机号" size="large" class="custom-input">
                <template #prefix><el-icon class="input-icon"><Phone /></el-icon></template></el-input>
            </el-form-item>
            <el-form-item prop="college">
              <el-input v-model="regForm.college" placeholder="所在学院" size="large" class="custom-input">
                <template #prefix><el-icon class="input-icon"><School /></el-icon></template></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" class="login-btn" @click="register" :loading="regLoading">注册</el-button>
            </el-form-item>
          </el-form>
        </div>
      </transition>

      <div class="switch-area">
        <el-button type="primary" @click="isRegister=!isRegister;resetForms()" class="switch-btn">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </el-button>
      </div>
    </div>

    <!-- ===== 页脚 ===== -->
    <footer class="login-footer">© 2026 高校社团管理系统 &nbsp;|&nbsp; Powered by Spring Boot &amp; Vue3</footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '../api/request'

// ===== 背景轮播 =====
import img1 from '../assets/images/111.jpg'
import img2 from '../assets/images/p.jpg'
import img3 from '../assets/images/j.jpg'
const bgImages = [img1, img2, img3]
const currentBg = ref(0)
let carouselTimer = null
let paused = false

const nextBg = () => { currentBg.value = (currentBg.value + 1) % bgImages.length }
const prevBg = () => { currentBg.value = (currentBg.value - 1 + bgImages.length) % bgImages.length }
const goToBg = (idx) => { currentBg.value = idx }

const startTimer = () => {
  clearInterval(carouselTimer)
  if (bgImages.length > 1) carouselTimer = setInterval(nextBg, 5000)
}
const pauseCarousel = () => { paused = true; clearInterval(carouselTimer) }
const resumeCarousel = () => { paused = false; if (bgImages.length > 1) startTimer() }

onMounted(() => { if (bgImages.length > 1) startTimer() })
onUnmounted(() => clearInterval(carouselTimer))

// ===== 登录/注册 =====
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
/* ============================================
   全局容器 + 入场淡入
   ============================================ */
.login-container {
  height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
  background: #1a1a2e;
  animation: pageIn 0.6s ease;
}
@keyframes pageIn { from { opacity: 0; } to { opacity: 1; } }

/* ============================================
   背景轮播
   ============================================ */
.bg-carousel { position: absolute; inset: 0; z-index: 0; }
.bg-slide {
  position: absolute; inset: 0;
  background-size: cover; background-position: center; background-repeat: no-repeat;
  opacity: 0; transition: opacity 1.0s ease;
}
.bg-slide.active { opacity: 1; }

/* 黑色遮罩 rgba(0,0,0,0.23) */
.bg-overlay {
  position: absolute; inset: 0; z-index: 1;
  background: rgba(0, 0, 0, 0.23);
}

/* 光斑 */
.bg-glow { position: absolute; border-radius: 50%; filter: blur(100px); pointer-events: none; z-index: 1; }
.bg-glow-1 { width: 320px; height: 320px; background: rgba(24,144,255,0.05); top: -60px; right: -80px; animation: glowFloat 10s ease-in-out infinite; }
.bg-glow-2 { width: 260px; height: 260px; background: rgba(54,207,201,0.04); bottom: -40px; left: -60px; animation: glowFloat 12s ease-in-out infinite reverse; }
@keyframes glowFloat {
  0%, 100% { transform: translate(0,0) scale(1); }
  33%  { transform: translate(20px,-20px) scale(1.06); }
  66%  { transform: translate(-12px,12px) scale(0.94); }
}

/* 切换箭头 */
.carousel-arrow {
  position: absolute; top: 50%; transform: translateY(-50%); z-index: 3;
  width: 38px; height: 38px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.25);
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(6px); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.25s; opacity: 0.45;
}
.carousel-arrow:hover { opacity: 1; background: rgba(255,255,255,0.18); border-color: rgba(255,255,255,0.45); }
.carousel-arrow-left  { left: 24px; }
.carousel-arrow-right { right: 24px; }

/* 轮播圆点 */
.carousel-dots {
  position: absolute; bottom: 52px; left: 50%; transform: translateX(-50%);
  display: flex; gap: 12px; z-index: 3;
}
.carousel-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: rgba(255,255,255,0.32); cursor: pointer; transition: all 0.3s;
}
.carousel-dot.active { background: #fff; transform: scale(1.6); box-shadow: 0 0 10px rgba(255,255,255,0.5); }
.carousel-dot:hover { background: rgba(255,255,255,0.7); }

/* ============================================
   左侧品牌区：左上角定位 left:8% top:18%
   文字直接浮在遮罩上，无包裹色块
   ============================================ */
.brand-area {
  position: absolute;
  top: 18%;
  left: 8%;
  z-index: 2;
  max-width: 380px;
}

.brand-title {
  font-size: 44px; font-weight: 900; color: #fff;
  margin: 0 0 18px; letter-spacing: 0.06em; line-height: 1.2;
  text-shadow: 0 1px 4px rgba(0,0,0,0.4);
  -webkit-font-smoothing: antialiased;
}

.brand-tagline {
  font-size: 16px; font-weight: 300; color: rgba(255,255,255,0.76);
  margin: 0 0 16px; letter-spacing: 0.07em;
  text-shadow: 0 1px 3px rgba(0,0,0,0.3);
}

.brand-desc {
  font-size: 13px; font-weight: 300; color: rgba(255,255,255,0.42);
  margin: 0; line-height: 1.8;
  text-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

/* ============================================
   右侧登录卡片：靠右垂直居中 right:10%
   固定宽度400px 半透明86% 圆角12px
   ============================================ */
.login-card {
  position: absolute;
  right: 10%;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 400px;
  background: rgba(255, 255, 255, 0.86);
  border-radius: 12px;
  padding: 32px 32px 28px;
  box-shadow: 0 6px 36px rgba(0,0,0,0.10), 0 1px 4px rgba(0,0,0,0.05);
}

/* 安全警示 */
.security-notice {
  font-size: 12px; color: rgba(220, 100, 90, 0.9);
  text-align: center; margin: 0 0 20px;
  letter-spacing: 0.03em;
}

.card-title {
  font-size: 21px; font-weight: 700; color: #1a1a2e;
  text-align: center; margin: 0 0 28px; letter-spacing: 0.03em;
}

.form-body { min-height: 100px; }

/* ============================================
   输入框
   ============================================ */
.custom-input :deep(.el-input__wrapper) {
  border-radius: 8px; box-shadow: 0 0 0 1px #d9d9d9;
  background: #fff !important; transition: all 0.25s; padding: 4px 12px;
}
.custom-input :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #b3b3b3; }
.custom-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px rgba(24,144,255,0.25); }
.custom-input :deep(.el-input__inner) { color: #262626 !important; font-size: 14px; }
.custom-input :deep(.el-input__inner::placeholder) { color: #bfbfbf !important; font-size: 13px; }
.input-icon { color: #1890ff !important; font-size: 17px; }
.custom-input :deep(.el-input__suffix) { color: #8c8c8c; }
.custom-input :deep(.el-input__suffix:hover) { color: #1890ff; }
.el-form-item { margin-bottom: 24px; }

/* ============================================
   登录按钮
   ============================================ */
.login-btn {
  width: 100%; height: 48px; font-size: 17px; font-weight: 700;
  letter-spacing: 0.10em; border-radius: 8px !important;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  border: none !important; color: #fff !important; transition: all 0.3s ease;
}
.login-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #096dd9 0%, #13c2c2 100%) !important;
  transform: scale(1.02); box-shadow: 0 8px 24px rgba(24,144,255,0.35) !important;
}
.login-btn:active:not(:disabled) { transform: scale(0.98); }
.login-btn:disabled {
  background: #d9d9d9 !important; color: #fff !important;
  cursor: not-allowed; box-shadow: none !important; transform: none !important;
}

/* 切换按钮 */
.switch-area { text-align: center; margin-top: 14px; }
.switch-btn {
  font-weight: 600 !important; font-size: 14px !important; color: #fff !important;
  background: #1890ff !important; border: none !important;
  padding: 6px 22px !important; border-radius: 6px !important; transition: all 0.25s;
}
.switch-btn:hover {
  background: #40a9ff !important; box-shadow: 0 4px 12px rgba(24,144,255,0.25) !important;
  transform: translateY(-1px);
}

/* 页脚 */
.login-footer {
  position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%); z-index: 2;
  font-size: 12px; color: rgba(255,255,255,0.30); letter-spacing: 0.03em; white-space: nowrap;
}

/* 表单切换动画 */
.form-switch-enter-active, .form-switch-leave-active { transition: all 0.25s ease; }
.form-switch-enter-from { opacity: 0; transform: translateX(24px); }
.form-switch-leave-to   { opacity: 0; transform: translateX(-24px); }

/* ============================================
   响应式：手机/平板 上下布局
   ============================================ */
@media (max-width: 900px) {
  .brand-area {
    position: static;
    text-align: center; margin: 0 auto;
    padding: 30px 20px 10px;
    max-width: 100%;
  }
  .brand-title { font-size: 30px; }
  .brand-tagline { font-size: 14px; }
  .brand-desc { font-size: 12px; }

  .login-card {
    position: static;
    transform: none;
    margin: 0 auto;
    width: min(400px, 90vw);
    padding: 28px 22px 24px;
  }

  .carousel-arrow { display: none; }
  .login-footer { font-size: 11px; bottom: 8px; }
}
</style>
