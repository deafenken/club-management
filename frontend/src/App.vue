<template>
  <router-view v-slot="{ Component, route }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<script setup>
import { watch } from 'vue'
import { useRouter } from 'vue-router'
import NProgress from 'nprogress'

const router = useRouter()
NProgress.configure({ showSpinner: false, speed: 400, minimum: 0.2 })

router.beforeEach(() => { NProgress.start() })
router.afterEach(() => { NProgress.done() })
router.onError(() => { NProgress.done() })
</script>

<style>
/* 页面过渡动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* NProgress 配色 */
#nprogress .bar {
  background: linear-gradient(90deg, #C96442, #E0A08A) !important;
  height: 3px !important;
}
</style>
