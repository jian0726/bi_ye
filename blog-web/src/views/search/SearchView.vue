<template>
  <!-- 单根包裹：多根 fragment 会让 <Transition mode="out-in"> 交接卡死，根级不能有注释 -->
  <div>
    <!-- 全屏沉浸搜索首屏：深墨绿 + 中央时钟 + 玻璃搜索胶囊（poetize 式） -->
    <section
      class="search-hero relative overflow-hidden flex flex-col items-center justify-center"
      :style="{ background: 'var(--ink-bg)', minHeight: searched ? '70vh' : '100dvh' }"
    >
      <!-- 氛围光晕与颗粒 -->
      <div class="absolute inset-0 pointer-events-none" aria-hidden="true">
        <div
          class="absolute -top-[30%] left-[55%] w-[680px] h-[680px] rounded-full opacity-[0.17] blur-[130px]"
          :style="{ background: 'radial-gradient(circle, var(--ink-accent) 0%, transparent 62%)' }"
        />
        <div
          class="absolute bottom-[-45%] left-[-10%] w-[620px] h-[620px] rounded-full opacity-[0.13] blur-[120px]"
          :style="{ background: 'radial-gradient(circle, #2f6b55 0%, transparent 65%)' }"
        />
        <div class="absolute inset-0 opacity-[0.04] mix-blend-overlay noise-layer" />
      </div>

      <!-- 右缘竖排幽灵字 -->
      <span
        class="hidden xl:block absolute right-8 top-1/2 -translate-y-1/2 display-serif text-[clamp(2.6rem,4.4vw,4rem)] leading-none tracking-[0.3em] select-none pointer-events-none"
        :style="{ color: 'var(--ink-text)', opacity: 0.08, writingMode: 'vertical-rl' }"
        aria-hidden="true"
      >搜点什么</span>

      <!-- 中央：时钟 + 搜索胶囊 -->
      <div class="relative z-10 flex flex-col items-center text-center px-6 -mt-[4vh]">
        <p
          class="display-serif leading-none tracking-[0.04em] tabular-nums"
          :style="{ color: 'var(--ink-text)', fontSize: 'clamp(4rem,11vw,7rem)' }"
        >
          {{ clockTime }}
</p>
        <p class="mt-5 text-[12.5px] tracking-[0.3em]" :style="{ color: 'var(--ink-text-secondary)' }">
          {{ clockDate }} · 星期{{ weekDay }}
        </p>

        <div class="search-glass mt-12" :class="{ 'search-glass-focus': keywordFocused }">
          <svg
            width="17"
            height="17"
            viewBox="0 0 16 16"
            fill="none"
            class="shrink-0 ml-5"
            :style="{ color: 'var(--ink-text-tertiary)' }"
            aria-hidden="true"
          >
            <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.6" />
            <path d="M11 11L14 14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          </svg>
          <input
            ref="inputRef"
            v-model="keyword"
            type="text"
            placeholder="搜点什么吧"
            class="flex-1 min-w-0 bg-transparent text-[15px] tracking-[0.02em] outline-none"
            :style="{ color: 'var(--ink-text)' }"
            @focus="keywordFocused = true"
            @blur="keywordFocused = false"
            @keyup.enter="doSearch"
          />
          <button
            class="w-9 h-9 shrink-0 mr-1.5 flex items-center justify-center rounded-full text-white transition-transform duration-200 hover:scale-105"
            :style="{ backgroundColor: 'var(--ink-accent)' }"
            aria-label="检索"
            @click="doSearch"
          >
            <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <circle cx="7" cy="7" r="4.6" stroke="currentColor" stroke-width="1.7" />
              <path d="M10.6 10.6L14 14" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
            </svg>
          </button>
        </div>

        <p class="mt-5 text-[11.5px] tracking-[0.14em]" :style="{ color: 'var(--ink-text-tertiary)' }">
          回车检索 · 标题与正文
        </p>
      </div>

      <!-- 检索后：下滑提示 -->
      <div
        v-if="searched"
        class="absolute bottom-7 left-1/2 -translate-x-1/2 z-10 anim-bounce-down"
        :style="{ color: 'var(--ink-text-tertiary)' }"
        aria-hidden="true"
      >
        <svg width="18" height="18" viewBox="0 0 16 16" fill="none">
          <path d="M3 6l5 5 5-5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </div>
    </section>

    <!-- 检索结果：杂志目录式单列列表 -->
    <div v-if="searched" ref="resultsRef" class="shell pt-14 pb-[var(--section-gap)]">
      <div class="mx-auto max-w-[880px]">
        <p class="mb-8 text-[14px] text-[var(--color-text-secondary)]">
          与「<span class="font-semibold text-[var(--color-accent)]">{{ keyword }}</span>」相关的
          <span class="font-semibold text-[var(--color-text-primary)]">{{ total }}</span> 篇
        </p>

        <Skeleton v-if="loading" :count="4" />

        <div v-else-if="results.length" class="result-list">
          <RouterLink
            v-for="(item, i) in results"
            :key="item.id"
            :to="`/article/${item.id}`"
            class="result-row group"
          >
            <span class="result-index display-serif">{{ String(i + 1).padStart(2, '0') }}</span>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2.5 mb-1.5 text-[11.5px] tracking-[0.04em] text-[var(--color-text-tertiary)]">
                <span
                  v-if="item.category"
                  class="px-2 py-0.5 rounded-full"
                  :style="{ backgroundColor: 'var(--color-accent-2-light)', color: 'var(--color-accent-2)' }"
                >
                  {{ item.category.name }}
                </span>
                <time>{{ formatDate(item.publishTime) }}</time>
              </div>
              <h3 class="result-title" v-html="highlight(item.title)" />
              <p class="mt-1.5 text-[13.5px] leading-[1.75] text-[var(--color-text-secondary)] line-clamp-2" v-html="highlight(item.summary)" />
            </div>
            <svg
              width="16"
              height="16"
              viewBox="0 0 16 16"
              fill="none"
              class="result-arrow shrink-0 self-center"
              aria-hidden="true"
            >
              <path d="M3 8h9M8.5 4.5L12 8l-3.5 3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
        </div>

        <div v-else class="card py-[clamp(3.5rem,8vw,5rem)] text-center">
          <p class="display-serif text-[clamp(2.6rem,5vw,3.4rem)] leading-none text-[var(--color-text-quaternary)] opacity-45 select-none">空</p>
          <p class="mt-6 text-[14.5px] text-[var(--color-text-secondary)]">没有找到与「{{ keyword }}」相关的内容</p>
          <p class="mt-1.5 text-[12.5px] text-[var(--color-text-tertiary)]">换个关键词试试</p>
          <button class="chip mt-6 mx-auto" @click="clear">重新检索</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Skeleton from '@/components/Skeleton.vue'
import { getArticleList } from '@/api'
import type { Article } from '@/types'
import { formatDate } from '@/utils/format'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const inputRef = ref<HTMLInputElement>()
const keywordFocused = ref(false)
const keyword = ref('')
const searched = ref(false)
const loading = ref(false)
const results = ref<Article[]>([])
const total = ref(0)
const resultsRef = ref<HTMLElement>()

/** 搜索框删空时自动回到初始沉浸态（时钟 + 搜索框，收起结果区） */
watch(
  () => keyword.value.trim(),
  (v) => {
    if (!v && searched.value && !loading.value) clear()
  }
)

/* 中央时钟 */
const now = ref(new Date())
let timer: number | undefined
onMounted(() => {
  timer = window.setInterval(() => (now.value = new Date()), 1000)
  const q = route.query.keyword as string | undefined
  if (q) {
    keyword.value = q
    searched.value = true
    doSearch()
  } else {
    nextTick(() => inputRef.value?.focus())
  }
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

const clockTime = computed(() => dayjs(now.value).format('HH:mm'))
const clockDate = computed(() => dayjs(now.value).format('YYYY年M月D日'))
const weekDay = computed(() => '日一二三四五六'[dayjs(now.value).day()])

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return

  router.replace({ name: 'search', query: { keyword: kw } })
  loading.value = true
  searched.value = true

  try {
    const res = await getArticleList({ page: 1, size: 50, keyword: kw })
    results.value = res.records
    total.value = res.total
  } catch (err) {
    console.error('[search] 检索失败', err)
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
    // 结果渲染后平滑滚到结果区
    await nextTick()
    requestAnimationFrame(() => resultsRef.value?.scrollIntoView({ behavior: 'smooth' }))
  }
}

function clear() {
  keyword.value = ''
  searched.value = false
  results.value = []
  router.replace({ name: 'search' })
  window.scrollTo({ top: 0, behavior: 'smooth' })
  nextTick(() => inputRef.value?.focus())
}

/** 关键词高亮：先转义 HTML，再把命中片段包上 <mark> */
function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

function highlight(text: string | null | undefined): string {
  const safe = escapeHtml(text ?? '')
  const kw = keyword.value.trim()
  if (!kw || !safe) return safe
  const re = new RegExp(kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi')
  return safe.replace(re, (m) => `<mark>${m}</mark>`)
}
</script>

<style scoped>
.noise-layer {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}

/* 玻璃搜索胶囊：半透明白 + 模糊，聚焦时描边染柚橙 */
.search-glass {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  width: min(560px, 88vw);
  height: 3.4rem;
  border-radius: 999px;
  border: 1px solid rgba(244, 241, 232, 0.22);
  background: rgba(244, 241, 232, 0.07);
  backdrop-filter: blur(14px);
  transition: border-color var(--duration-base) var(--ease-apple), background var(--duration-base) var(--ease-apple);
}
.search-glass:focus-within,
.search-glass-focus {
  border-color: var(--ink-accent);
  background: rgba(244, 241, 232, 0.11);
}
.search-glass input {
  caret-color: var(--ink-accent);
}
.search-glass input::placeholder {
  color: var(--ink-text-tertiary);
}
.search-glass input:focus-visible {
  outline: none;
}

/* 下滑提示轻微浮动 */
.anim-bounce-down {
  animation: bounce-down 1.8s var(--ease-apple) infinite;
}
@keyframes bounce-down {
  0%,
  100% {
    transform: translate(-50%, 0);
    opacity: 0.7;
  }
  50% {
    transform: translate(-50%, 6px);
    opacity: 1;
  }
}

/* 结果列表：目录条目 + 分隔线 */
.result-row {
  display: flex;
  gap: 1.4rem;
  padding: 1.5rem 1rem;
  border-radius: 14px;
  transition: background-color var(--duration-base) var(--ease-apple);
}
.result-row + .result-row {
  border-top: 1px solid var(--color-divider);
}
.result-row:hover {
  background: var(--color-surface-sunken);
}

/* 序号：衬线大字，悬停染橙 */
.result-index {
  width: 2.4rem;
  flex-shrink: 0;
  font-size: 1.3rem;
  line-height: 1.25;
  text-align: center;
  color: var(--color-text-quaternary);
  transition: color var(--duration-base) var(--ease-apple);
}
.result-row:hover .result-index {
  color: var(--color-accent);
}

.result-title {
  font-size: 16.5px;
  line-height: 1.45;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--color-text-primary);
  transition: color var(--duration-base) var(--ease-apple);
}
.result-row:hover .result-title {
  color: var(--color-accent);
}

.result-arrow {
  color: var(--color-text-quaternary);
  opacity: 0;
  transform: translateX(-6px);
  transition: opacity var(--duration-base) var(--ease-apple), transform var(--duration-base) var(--ease-apple),
    color var(--duration-base) var(--ease-apple);
}
.result-row:hover .result-arrow {
  opacity: 1;
  transform: translateX(0);
  color: var(--color-accent);
}

/* 关键词命中高亮：默认橙字，悬停整行时反色为橙底白字（呼应选中文本色） */
.result-row :deep(mark) {
  background: transparent;
  color: var(--color-accent);
  font-weight: 600;
  border-radius: 3px;
  padding-inline: 1px;
  transition: background-color var(--duration-fast) var(--ease-apple), color var(--duration-fast) var(--ease-apple);
}
.result-row:hover :deep(mark) {
  background: var(--color-accent);
  color: #fff;
}
</style>
