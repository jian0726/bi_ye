<template>
  <!-- 开屏加载动画（会话内只播一次） -->
  <Preloader />
  <!-- 路由切换进度条 -->
  <RouteProgress />
  <!-- 页面切换动画已下沉到 DefaultLayout 的内层 RouterView -->
  <RouterView />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import Preloader from '@/components/Preloader.vue'
import RouteProgress from '@/components/RouteProgress.vue'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()

onMounted(() => {
  siteStore.loadConfig()
})
</script>
