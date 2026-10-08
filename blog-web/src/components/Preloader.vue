<template>
  <!-- 开屏加载动画：与全站同族的墨色刊头 + 衬线站名，会话内只播一次 -->
  <Transition name="preloader">
    <div
      v-if="visible"
      class="fixed inset-0 z-[999] flex flex-col items-center justify-center overflow-hidden"
      :style="{ background: 'var(--ink-bg)' }"
      aria-hidden="true"
    >
      <!-- 光晕与噪点 -->
      <div class="absolute inset-0 pointer-events-none">
        <div
          class="absolute -top-[40%] right-[-10%] w-[680px] h-[680px] rounded-full opacity-[0.22] blur-[120px]"
          :style="{ background: 'radial-gradient(circle, var(--ink-accent) 0%, transparent 62%)' }"
        />
        <div
          class="absolute bottom-[-50%] left-[-8%] w-[560px] h-[560px] rounded-full opacity-[0.14] blur-[110px]"
          :style="{ background: 'radial-gradient(circle, #2f6b55 0%, transparent 65%)' }"
        />
        <div class="absolute inset-0 opacity-[0.04] mix-blend-overlay noise-layer" />
      </div>

      <div class="relative z-10 flex flex-col items-center">
        <!-- 期号 -->
        <p class="pre-meta mb-7" :class="{ 'is-on': stage >= 1 }">JIAN YOU · 2026</p>

        <!-- 站名 -->
        <h1
          class="display-serif text-[clamp(3.2rem,10vw,5.5rem)] leading-none tracking-[0.12em]"
          :class="{ 'is-on': stage >= 1 }"
          :style="{ color: 'var(--ink-text)' }"
        >
          简柚
        </h1>

        <!-- 进度细线 -->
        <div
          class="mt-9 h-px w-[180px] overflow-hidden"
          :style="{ background: 'var(--ink-divider)' }"
        >
          <div
            class="h-full transition-[width] duration-150 ease-out"
            :style="{ width: `${progress}%`, background: 'var(--ink-accent)' }"
          />
        </div>

        <p class="pre-meta mt-5" :class="{ 'is-on': stage >= 1 }">
          {{ progress < 100 ? '正在铺纸' : '准备好了' }}
        </p>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

const SESSION_KEY = 'jianyou-preloaded'

const visible = ref(false)
const stage = ref(0)
const progress = ref(0)

onMounted(async () => {
  // 同一会话只播一次
  if (sessionStorage.getItem(SESSION_KEY)) return
  sessionStorage.setItem(SESSION_KEY, '1')
  visible.value = true

  // 进度条动画：目标 90%，完成后冲到 100%
  const timer = window.setInterval(() => {
    progress.value = Math.min(90, progress.value + Math.ceil(Math.random() * 9))
  }, 90)

  // 最短展示 1.5s，同时等字体就绪（衬线大字不闪替换）
  const minDelay = new Promise((r) => setTimeout(r, 1500))
  const fontsReady = typeof document.fonts !== 'undefined' ? document.fonts.ready : Promise.resolve()

  await Promise.all([minDelay, fontsReady])
  window.clearInterval(timer)
  progress.value = 100
  stage.value = 1

  // 让 100% 与文字停留一拍再淡出
  await new Promise((r) => setTimeout(r, 450))
  visible.value = false
})
</script>

<style scoped>
.pre-meta {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: var(--ink-text-tertiary);
  opacity: 0;
  transform: translateY(6px);
  transition: opacity 0.7s ease, transform 0.7s ease;
}
.pre-meta.is-on {
  opacity: 1;
  transform: translateY(0);
}
h1.is-on {
  animation: pre-title 1.1s var(--ease-apple) both;
}
@keyframes pre-title {
  from {
    opacity: 0;
    transform: translateY(16px);
    letter-spacing: 0.3em;
  }
  to {
    opacity: 1;
    transform: translateY(0);
    letter-spacing: 0.12em;
  }
}

/* 整体淡出：透明 + 轻微上移 */
.preloader-leave-active {
  transition: opacity 0.65s ease, transform 0.65s ease;
}
.preloader-leave-to {
  opacity: 0;
  transform: translateY(-2.5%);
}

.noise-layer {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}
</style>
