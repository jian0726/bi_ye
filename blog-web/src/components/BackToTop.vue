<template>
  <Transition name="back-top">
    <button
      v-if="visible"
      class="fixed bottom-8 right-8 z-[150] w-10 h-10 rounded-full glass shadow-[var(--shadow-card)] flex items-center justify-center text-[var(--color-text-secondary)] transition-all duration-300 hover:text-[var(--color-text-primary)] hover:scale-110"
      aria-label="回到顶部"
      @click="scrollToTop()"
    >
      <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <path
          d="M8 13V3M8 3L3.5 7.5M8 3l4.5 4.5"
          stroke="currentColor"
          stroke-width="1.7"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>
  </Transition>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { scrollToTop } from '@/utils/format'

const visible = ref(false)

function onScroll() {
  visible.value = window.scrollY > 600
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.back-top-enter-active,
.back-top-leave-active {
  transition: opacity 0.3s var(--ease-apple), transform 0.3s var(--ease-spring);
}

.back-top-enter-from,
.back-top-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.9);
}
</style>
