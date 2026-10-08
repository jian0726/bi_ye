<template>
  <!-- 路由切换顶部进度条：每次跳页都会出现的柚橙细线 -->
  <div v-if="show" class="fixed top-0 left-0 right-0 z-[600] pointer-events-none" aria-hidden="true">
    <div
      class="h-[2.5px] route-progress-bar"
      :style="{
        width: `${progress}%`,
        opacity: done ? 0 : 1,
        transition: `width ${widthDur}ms ease-out, opacity 400ms ease`
      }"
    />
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const show = ref(false)
const progress = ref(0)
const done = ref(false)
const widthDur = ref(120)

let climbTimer = 0
let finishTimer = 0
let hideTimer = 0

function start() {
  clearTimeout(finishTimer)
  clearTimeout(hideTimer)
  clearInterval(climbTimer)
  done.value = false
  widthDur.value = 120
  progress.value = 8
  show.value = true

  // 爬升阶段：越接近上限越慢，营造「快好了」的错觉
  climbTimer = window.setInterval(() => {
    widthDur.value = 200
    progress.value = Math.min(88, progress.value + Math.max(0.6, (92 - progress.value) * 0.06))
  }, 180)
}

function complete() {
  clearInterval(climbTimer)
  widthDur.value = 260
  progress.value = 100
  done.value = true
  hideTimer = window.setTimeout(() => {
    show.value = false
    progress.value = 0
  }, 450)
}

watch(
  () => route.path,
  () => {
    start()
    // 数据页各自有骨架屏接管；进度条 700ms 后收尾
    finishTimer = window.setTimeout(complete, 700)
  }
)

onUnmounted(() => {
  clearInterval(climbTimer)
  clearTimeout(finishTimer)
  clearTimeout(hideTimer)
})
</script>

<style scoped>
.route-progress-bar {
  background: linear-gradient(90deg, var(--color-accent) 0%, var(--ink-accent) 100%);
  box-shadow: 0 0 8px color-mix(in srgb, var(--color-accent) 45%, transparent);
}
</style>
