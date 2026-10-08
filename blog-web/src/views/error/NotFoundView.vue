<template>
  <div class="relative flex min-h-[calc(100vh-var(--header-height))] flex-col items-center justify-center overflow-hidden px-6 py-16 text-center">
    <!-- 飘落的小卡片：呼应留言墙的留言雨，点它回首页 -->
    <button
      class="drift-card absolute left-[18%] top-0 hidden md:flex"
      aria-label="回到首页"
      @click="goHome"
    >
      <span class="display-serif text-[13px]" :style="{ color: 'var(--ink-text)' }">这里什么都没有</span>
    </button>
    <button
      class="drift-card drift-slow absolute right-[16%] top-0 hidden lg:flex"
      aria-hidden="true"
      tabindex="-1"
    >
      <span class="display-serif text-[12px]" :style="{ color: 'var(--ink-text)' }">风把它吹走了</span>
    </button>

    <!-- 衬线大字 -->
    <h1 class="display-serif max-w-[640px] text-[clamp(2.2rem,6vw,3.8rem)] leading-[1.3] tracking-[0.04em] text-[var(--color-text-primary)]">
      这一页被风吹走了
    </h1>
    <p class="mt-6 max-w-[400px] text-[14.5px] leading-[1.8] text-[var(--color-text-secondary)]">
      你访问的页面不存在，或者已经被移走了。检查一下地址，或者回到今日重新开始。
    </p>

    <div class="mt-10 flex items-center gap-3">
      <RouterLink to="/" class="btn btn-primary !px-6 !py-2.5">回今日</RouterLink>
      <button class="btn btn-secondary !px-6 !py-2.5" @click="goBack">回到上一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
/* 飘落小卡片：留言墙黑胶囊同款质感，缓慢循环下落 */
.drift-card {
  padding: 8px 16px;
  border-radius: 999px;
  background: rgba(16, 23, 20, 0.85);
  border: 1px solid rgba(244, 241, 232, 0.14);
  cursor: pointer;
  animation: drift-fall 14s linear infinite;
  will-change: transform;
}
.drift-slow {
  animation-duration: 19s;
  animation-delay: -7s;
}
@keyframes drift-fall {
  0% { transform: translateY(-8vh); opacity: 0; }
  8% { opacity: 1; }
  88% { opacity: 1; }
  100% { transform: translateY(105vh); opacity: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .drift-card { animation: none; top: 20%; }
}
</style>
