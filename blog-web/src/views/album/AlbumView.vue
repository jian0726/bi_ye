<template>
  <div ref="root" class="min-h-[60vh] pb-[var(--section-gap)]">
    <PageMasthead
      title="相册"
      kicker="Album"
      subtitle="快门按下的瞬间不会被记住，但可以翻回来。"
      :meta="['NO.06', `${photos.length} 张`, '长沙']"
      ghost="快门"
    />

    <div class="shell pt-12">
      <!-- 空状态 -->
      <p
        v-if="!loading && photos.length === 0"
        class="py-20 text-center text-[13.5px] text-[var(--color-text-tertiary)]"
      >
        相册还是空的，等第一张照片。
      </p>

      <!-- 瀑布流照片墙 -->
      <div v-else class="columns-2 md:columns-3 lg:columns-4 gap-4 [column-fill:_balance]">
        <figure
          v-for="(photo, i) in photos"
          :key="photo.id ?? i"
          class="reveal group mb-4 break-inside-avoid cursor-zoom-in"
          :class="`reveal-delay-${(i % 6) + 1}`"
          @click="active = i"
        >
          <div
            class="relative overflow-hidden rounded-[14px] shadow-[var(--shadow-card)] transition-shadow duration-300 group-hover:shadow-[var(--shadow-lift)]"
            :style="{ aspectRatio: photo.ratio }"
          >
            <img
              v-if="photo.url"
              :src="photo.url"
              :alt="photo.title"
              loading="lazy"
              class="absolute inset-0 h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
            />
            <template v-else>
              <div class="absolute inset-0" :style="{ background: photo.tone ?? 'linear-gradient(160deg,#cbc4b8,#8a8574)' }" />
              <div class="absolute inset-0 opacity-[0.05] noise" aria-hidden="true" />
              <p class="absolute inset-0 flex items-center justify-center text-[52px]">{{ photo.emoji }}</p>
            </template>
            <figcaption class="absolute inset-x-0 bottom-0 p-4 bg-gradient-to-t from-black/45 to-transparent">
              <p class="display-serif text-[14px] text-white">{{ photo.title }}</p>
              <p class="mt-1 text-[11px] text-white/75 tabular-nums">{{ photo.takenDate ?? '' }}</p>
            </figcaption>
          </div>
        </figure>
      </div>
    </div>

    <!-- 大图弹层 -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="active !== null"
          class="fixed inset-0 z-[300] flex items-center justify-center p-6 bg-black/80 backdrop-blur-sm"
          @click.self="active = null"
        >
          <figure class="max-w-[860px] w-full">
            <div
              class="relative w-full overflow-hidden rounded-[16px]"
              :style="{ aspectRatio: photos[active].ratio, background: photos[active].tone ?? 'transparent' }"
            >
              <img
                v-if="photos[active].url"
                :src="photos[active].url ?? undefined"
                :alt="photos[active].title"
                class="absolute inset-0 h-full w-full object-contain"
              />
              <template v-else>
                <div class="absolute inset-0 opacity-[0.05] noise" aria-hidden="true" />
                <p class="absolute inset-0 flex items-center justify-center text-[64px]">{{ photos[active].emoji }}</p>
              </template>
            </div>
            <figcaption class="mt-4 text-center">
              <p class="display-serif text-[17px] text-white">{{ photos[active].title }}</p>
              <p class="mt-1 text-[12px] text-white/60 tabular-nums">{{ photos[active].takenDate ?? '' }} · {{ photos[active].location ?? '' }}</p>
            </figcaption>
          </figure>
          <button
            class="absolute top-6 right-6 w-9 h-9 rounded-full text-white/80 hover:text-white border border-white/25 flex items-center justify-center transition-colors"
            aria-label="关闭"
            @click="active = null"
          >
            <svg width="15" height="15" viewBox="0 0 16 16" fill="none"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" /></svg>
          </button>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import PageMasthead from '@/components/PageMasthead.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { getPhotos } from '@/api'
import type { Photo } from '@/types'

const root = ref<HTMLElement>()
const { refresh } = useScrollReveal(root)

const active = ref<number | null>(null)
const photos = ref<Photo[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    photos.value = await getPhotos()
  } catch {
    // 后端不可用时保持空列表，页面显示占位文案
    photos.value = []
  } finally {
    loading.value = false
    // 异步渲染后重新观察 .reveal 元素
    await nextTick()
    requestAnimationFrame(refresh)
  }
})
</script>

<style scoped>
.noise {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
