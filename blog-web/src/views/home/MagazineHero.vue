<template>
  <section
    class="ink-hero relative overflow-hidden"
    :style="{ background: 'var(--ink-bg)' }"
  >
    <!-- 纸纹与光晕 -->
    <div class="absolute inset-0 pointer-events-none" aria-hidden="true">
      <div
        class="absolute -top-[30%] right-[-15%] w-[820px] h-[820px] rounded-full opacity-[0.22] blur-[130px]"
        :style="{ background: 'radial-gradient(circle, var(--ink-accent) 0%, transparent 62%)' }"
      />
      <div
        class="absolute bottom-[-35%] left-[-10%] w-[680px] h-[680px] rounded-full opacity-[0.14] blur-[120px]"
        :style="{ background: 'radial-gradient(circle, #2f6b55 0%, transparent 65%)' }"
      />
      <!-- 细颗粒噪点，模拟纸张 -->
      <div class="absolute inset-0 opacity-[0.035] mix-blend-overlay noise-layer" />
    </div>

    <!-- 巨型竖排刊名（贯穿左边缘的装饰性标识） -->
    <div
      class="vertical-masthead hidden xl:flex absolute left-4 top-1/2 -translate-y-1/2 z-20 select-none pointer-events-none"
      aria-hidden="true"
    >
      <span
        class="display-serif text-[clamp(3rem,5vw,4.6rem)] leading-none tracking-[0.32em]"
        :style="{ color: 'var(--ink-text)', opacity: 0.1, writingMode: 'vertical-rl', transform: `translateY(${mastheadShift}px)` }"
      >
        简柚记录
      </span>
    </div>

    <div class="relative z-10 shell-full pt-[calc(var(--header-height)+clamp(3rem,7vw,5rem))] pb-[clamp(3.5rem,8vw,6rem)]">
      <!-- 刊头信息条 -->
      <div class="reveal masthead-ink mb-14">
        <span>NO. {{ issueNo }}</span>
        <span class="hidden sm:inline">{{ todayLabel }}</span>
        <span>{{ site.siteAuthor }} 的手记</span>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-14 lg:gap-10 items-center">
        <!-- 左：刊名 -->
        <div class="lg:col-span-6 xl:col-span-7">
          <h1 class="reveal reveal-delay-1">
            <span
              class="display-serif block text-[clamp(3.6rem,9vw,6.6rem)] leading-[1.02] tracking-[0.02em]"
              :style="{ color: 'var(--ink-text)' }"
            >
              {{ site.siteName }}
            </span>
            <span
              class="mt-6 block display-serif text-[clamp(1.05rem,2.1vw,1.5rem)] leading-[1.85] whitespace-pre-line"
              :style="{ color: 'var(--ink-text-secondary)' }"
            >
              {{ tagline }}
            </span>
          </h1>

          <div class="reveal reveal-delay-2 mt-11 flex items-center gap-5">
            <RouterLink
              to="/archive"
              class="ink-btn-primary"
            >
              按时间翻
              <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </RouterLink>
            <RouterLink to="/about" class="ink-btn-ghost">我是谁</RouterLink>
          </div>

          <!-- 手写签名式的落款 -->
          <p
            class="reveal reveal-delay-3 mt-12 text-[13px] leading-[2] tracking-[0.02em]"
            :style="{ color: 'var(--ink-text-tertiary)' }"
          >
            {{ site.siteAuthor }} · {{ authorLine }}
          </p>
        </div>

        <!-- 右：横滑文章带（滚轮横滑 / 拖动） -->
        <div class="lg:col-span-6 xl:col-span-5">
          <div class="reveal reveal-delay-2 flex items-center justify-between mb-5">
            <span class="kicker-ink">最近落笔</span>
            <span class="flex items-center gap-3">
              <button class="ink-nav" aria-label="向左" @click="scrollBand(-1)">
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M10 4L6 8l4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" /></svg>
              </button>
              <button class="ink-nav" aria-label="向右" @click="scrollBand(1)">
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" /></svg>
              </button>
            </span>
          </div>

          <!-- 横向滚动带 -->
          <div
            ref="bandEl"
            class="band-scroller flex gap-4 overflow-x-auto pb-3"
            @wheel.prevent="onWheel"
            @mousedown="onDragStart"
          >
            <RouterLink
              v-for="(item, i) in bandItems"
              :key="item.id"
              :to="`/article/${item.id}`"
              class="band-card shrink-0 w-[236px] rounded-[16px] overflow-hidden group"
              :style="{ background: 'var(--ink-bg-deep)', border: '1px solid var(--ink-divider)' }"
            >
              <!-- 顶部色块（无配图时的视觉代替） -->
              <div class="h-[6px]" :style="{ background: bandColors[i % bandColors.length] }" />

              <div class="p-5 h-[190px] flex flex-col justify-between">
                <div class="flex items-start justify-between gap-3">
                  <span
                    class="text-[11px] tracking-[0.14em] uppercase"
                    :style="{ color: 'var(--ink-text-tertiary)' }"
                  >{{ item.category?.name ?? '未分类' }}</span>
                  <span class="display-serif text-[13px]" :style="{ color: 'var(--ink-accent)' }">
                    {{ String(i + 1).padStart(2, '0') }}
                  </span>
                </div>

                <div>
                  <h3
                    class="display-serif text-[16.5px] leading-[1.5] line-clamp-3 transition-opacity duration-300 group-hover:opacity-75"
                    :style="{ color: 'var(--ink-text)' }"
                  >
                    {{ item.title }}
                  </h3>
                  <p
                    class="mt-3.5 text-[11.5px]"
                    :style="{ color: 'var(--ink-text-tertiary)' }"
                  >
                    {{ formatDate(item.publishTime) }}
                  </p>
                </div>
              </div>
            </RouterLink>

            <div v-if="!bandItems.length" class="text-[13px]" :style="{ color: 'var(--ink-text-tertiary)' }">
              还没有文章
            </div>
          </div>

          <p class="mt-4 text-[11.5px]" :style="{ color: 'var(--ink-text-tertiary)' }">
            滚轮横向翻动 · 或按住拖动
          </p>
        </div>
      </div>

      <!-- 刊底统计 -->
      <div
        class="reveal reveal-delay-2 mt-16 pt-8 flex items-center justify-between flex-wrap gap-6"
        :style="{ borderTop: '1px solid var(--ink-divider)' }"
      >
        <div v-for="stat in stats" :key="stat.label" class="flex items-baseline gap-2">
          <span class="display-serif text-[25px] tabular-nums" :style="{ color: 'var(--ink-text)' }">{{ stat.value }}</span>
          <span class="text-[12px]" :style="{ color: 'var(--ink-text-tertiary)' }">{{ stat.label }}</span>
        </div>
        <span class="text-[11px] tracking-[0.28em]" :style="{ color: 'var(--ink-text-tertiary)' }">
          KEEP RECORDING
        </span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import dayjs from 'dayjs'
import type { Article, SiteConfig } from '@/types'
import { formatDate } from '@/utils/format'

const props = defineProps<{
  site: SiteConfig
  articles: Article[]
  stats: { label: string; value: string }[]
  tagline: string
}>()

/* ---------------- 日期与期号 ---------------- */

const todayLabel = computed(() => dayjs().format('YYYY.MM.DD'))

const issueNo = computed(() => {
  const start = dayjs('2026-01-01')
  return String(dayjs().diff(start, 'week') + 1).padStart(2, '0')
})

const authorLine = computed(() => {
  const bio = props.site.authorBio || ''
  return bio.split('。')[0] || '把日子写下来'
})

/* ---------------- 横滑文章带 ---------------- */

const bandEl = ref<HTMLElement>()
/** 最多取 8 篇 */
const bandItems = computed(() => props.articles.slice(0, 8))

/** 卡片顶部色条：墨色系下的高对比点缀 */
const bandColors = [
  'linear-gradient(90deg, #e8702a, #f5a15f)',
  'linear-gradient(90deg, #2f6b55, #5da291)',
  'linear-gradient(90deg, #d9c9a3, #f0e4c8)',
  'linear-gradient(90deg, #a8452a, #d97757)'
]

const scrollBand = (dir: number) => {
  bandEl.value?.scrollBy({ left: dir * 260, behavior: 'smooth' })
}

/** 鼠标滚轮 → 横向滚动 */
const onWheel = (e: WheelEvent) => {
  const el = bandEl.value
  if (!el) return
  const delta = Math.abs(e.deltaY) > Math.abs(e.deltaX) ? e.deltaY : e.deltaX
  el.scrollLeft += delta
}

/** 按住拖动 */
let dragging = false
let dragStartX = 0
let dragStartScroll = 0

const onDragStart = (e: MouseEvent) => {
  const el = bandEl.value
  if (!el) return
  dragging = true
  dragStartX = e.pageX
  dragStartScroll = el.scrollLeft
  el.style.cursor = 'grabbing'
}

const onDragMove = (e: MouseEvent) => {
  if (!dragging || !bandEl.value) return
  e.preventDefault()
  bandEl.value.scrollLeft = dragStartScroll - (e.pageX - dragStartX)
}

const onDragEnd = () => {
  dragging = false
  if (bandEl.value) bandEl.value.style.cursor = 'grab'
}

/* ---------------- 竖排刊名视差 ---------------- */

const mastheadShift = ref(0)
const onScroll = () => {
  mastheadShift.value = Math.min(window.scrollY * 0.12, 90)
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
  if (bandEl.value) bandEl.value.style.cursor = 'grab'
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
})
</script>

<style scoped>
/* 墨色刊头专用按钮 */
.ink-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.85rem 1.7rem;
  border-radius: 13px;
  font-size: 14px;
  font-weight: 600;
  color: #101714;
  background: var(--ink-text);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.25s;
}
.ink-btn-primary:hover {
  transform: translateY(-2px);
  opacity: 0.9;
}

.ink-btn-ghost {
  display: inline-flex;
  align-items: center;
  padding: 0.85rem 1.4rem;
  border-radius: 13px;
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-text);
  border: 1px solid var(--ink-divider);
  transition: background-color 0.25s, border-color 0.25s;
}
.ink-btn-ghost:hover {
  background: rgba(244, 241, 232, 0.06);
  border-color: rgba(244, 241, 232, 0.24);
}

/* 墨色刊头的信息条 */
.masthead-ink {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: var(--ink-text-tertiary);
}
.masthead-ink::before,
.masthead-ink::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--ink-divider);
}

/* 墨色小标 */
.kicker-ink {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--ink-accent);
}
.kicker-ink::before {
  content: '';
  width: 20px;
  height: 1.5px;
  background: currentColor;
}

/* 左右导航按钮 */
.ink-nav {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--ink-text-secondary);
  border: 1px solid var(--ink-divider);
  transition: all 0.22s ease;
}
.ink-nav:hover {
  color: var(--ink-text);
  border-color: rgba(244, 241, 232, 0.32);
}

/* 横滑带：隐藏滚动条 */
.band-scroller {
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.band-scroller::-webkit-scrollbar {
  display: none;
}

.band-card {
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.band-card:hover {
  transform: translateY(-6px);
}

/* 细颗粒噪点（用 SVG 内联 data-uri 生成的纸张颗粒） */
.noise-layer {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}
</style>
