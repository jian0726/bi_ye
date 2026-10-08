<template>
  <div ref="root" class="pb-[var(--section-gap)]">
    <PageMasthead
      title="归档"
      kicker="Archive"
      subtitle="全部文章按时间倒序，一路翻回起点。"
      :meta="[`NO.04`, `${totalCount} 篇`, '倒序']"
      ghost="来路"
    />

    <div class="shell pt-12">
      <header class="sr-only"><h1>归档</h1></header>

      <!-- 年份快速跳转 -->
      <div v-if="groups.length > 1" class="reveal reveal-delay-1 mb-12 flex flex-wrap justify-center gap-2">
        <a v-for="g in groups" :key="g.year" :href="`#year-${g.year}`" class="chip">
          {{ g.year }}
          <span class="ml-1.5 text-[var(--color-text-quaternary)]">{{ g.count }}</span>
        </a>
      </div>

      <div v-if="loading" class="flex flex-col items-center gap-6 py-10">
        <div v-for="i in 6" :key="i" class="skeleton h-16 w-full max-w-[520px]" :class="i % 2 ? 'self-start' : 'self-end'" />
      </div>

      <!-- ===== 中央时间线：左右交替卡片 ===== -->
      <div v-else class="arc-timeline mx-auto max-w-[880px]">
        <template v-for="group in groups" :key="group.year">
          <!-- 年份节点：骑在中央线上 -->
          <div :id="`year-${group.year}`" class="arc-year reveal">
            <span class="arc-year-pill display-serif">{{ group.year }}</span>
          </div>

          <template v-for="(item, i) in group.articles" :key="item.id">
            <RouterLink
              :to="`/article/${item.id}`"
              class="arc-item reveal"
              :class="isLeft(itemIndex(group, i)) ? 'arc-left' : 'arc-right'"
            >
              <span
                class="arc-dot"
                :style="{ backgroundColor: dotColor(itemIndex(group, i)) }"
                aria-hidden="true"
              />
              <span
                class="arc-card block rounded-[10px] px-4 py-3.5 transition-transform duration-200 hover:-translate-y-0.5"
                :style="{ backgroundColor: cardColor(itemIndex(group, i)) }"
              >
                <span class="block text-[13.5px] leading-[1.75] text-[#3d3a34]">
                  {{ item.title }}
                </span>
                <span class="mt-2 block text-[11px] tabular-nums text-[#8a857a]">
                  {{ formatDate(item.publishTime) }}
                </span>
              </span>
            </RouterLink>
          </template>
        </template>

        <!-- 起点标记 + 纸飞机 -->
        <div class="arc-end relative flex flex-col items-center pt-2">
          <span class="arc-origin" aria-hidden="true" />
          <button
            class="arc-plane mt-6 flex h-11 w-11 items-center justify-center rounded-full border border-[#34a97c] bg-[var(--color-surface-elevated)] transition-transform duration-200 hover:-translate-y-1"
            aria-label="回到顶部"
            @click="toTop"
          >
            <svg width="18" height="18" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M14 2L7.5 8.5M14 2L9.5 14 7.5 8.5 2 6.5 14 2z" stroke="#34a97c" stroke-width="1.3" stroke-linejoin="round" />
            </svg>
          </button>
          <p class="mt-3 text-[12px] tracking-[0.08em] text-[var(--color-text-tertiary)]">
            点一下纸飞机，回到起点
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import PageMasthead from '@/components/PageMasthead.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { getArchive } from '@/api'
import type { ArchiveGroup } from '@/types'
import { formatDate } from '@/utils/format'

const root = ref<HTMLElement>()
const { refresh } = useScrollReveal(root)

const loading = ref(true)
const groups = ref<ArchiveGroup[]>([])

const totalCount = computed(() => groups.value.reduce((sum, g) => sum + g.count, 0))

/** 全局序号：跨年份连续交替左右 */
function itemIndex(group: ArchiveGroup, i: number): number {
  const gi = groups.value.indexOf(group)
  let before = 0
  for (let k = 0; k < gi; k++) before += groups.value[k].articles.length
  return before + i
}

/** 柔和糖果色系（卡片底 / 线点），按序号循环 */
const cardPalette = ['#fdeef0', '#e7f5e9', '#fdf6dd', '#e3f2fd', '#ece9fb', '#ffedd9']
const dotPalette = ['#f28ca0', '#34a97c', '#f2a94c', '#5b8def', '#6a6ae0', '#f2955c']

const cardColor = (idx: number) => cardPalette[idx % cardPalette.length]
const dotColor = (idx: number) => dotPalette[idx % dotPalette.length]
const isLeft = (idx: number) => idx % 2 === 0

function toTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  try {
    groups.value = await getArchive()
  } catch (err) {
    console.error('[archive] 加载失败', err)
  } finally {
    loading.value = false
    // 数据渲染完成后再处理 .reveal，避免卡片停留在透明态
    await nextTick()
    requestAnimationFrame(refresh)
  }
})
</script>

<style scoped>
/* ---------- 中央时间线 ---------- */
.arc-timeline {
  position: relative;
  padding: 26px 0 10px;
}
/* 中央绿线：上下两端渐隐 */
.arc-timeline::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  transform: translateX(-50%);
  background: linear-gradient(
    to bottom,
    transparent 0%,
    #34a97c 5%,
    #34a97c 96%,
    transparent 100%
  );
}

/* 年份节点：骑线小胶囊 */
.arc-year {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  padding: 18px 0 26px;
}
.arc-year-pill {
  padding: 4px 18px;
  border-radius: 999px;
  background: var(--color-surface-elevated);
  border: 1.5px solid #34a97c;
  color: #2c7d5d;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.12em;
  box-shadow: 0 2px 10px rgba(52, 169, 124, 0.14);
}

/* 条目：左右交替 */
.arc-item {
  position: relative;
  display: block;
  width: calc(50% - 44px);
  margin-bottom: 30px;
}
.arc-left {
  margin-right: auto;
}
.arc-right {
  margin-left: auto;
}

/* 线点：贴在中央线上 */
.arc-dot {
  position: absolute;
  top: 16px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(52, 169, 124, 0.12);
}
.arc-left .arc-dot {
  right: -51px;
}
.arc-right .arc-dot {
  left: -51px;
}

/* 起点空心红圈 */
.arc-origin {
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #e05a4e;
  background: var(--color-surface);
}

/* ---------- 窄屏：单列靠左 ---------- */
@media (max-width: 720px) {
  .arc-timeline::before {
    left: 10px;
    transform: none;
  }
  .arc-item,
  .arc-item.arc-right {
    width: auto;
    margin-left: 34px;
    margin-right: 0;
  }
  .arc-item .arc-dot,
  .arc-item.arc-right .arc-dot {
    left: -31px;
    right: auto;
  }
  .arc-origin {
    left: 10px;
    transform: none;
    margin-left: -4px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .arc-card,
  .arc-plane {
    transition: none;
  }
}
</style>
