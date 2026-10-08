<template>
  <section class="pb-[var(--section-gap)]">
    <div class="shell shell-narrow">
      <!-- 区块标题 -->
      <div class="reveal flex items-end justify-between mb-14 flex-wrap gap-4">
        <div>
          <p class="kicker mb-4">Archive</p>
          <h2 class="display-serif text-[clamp(1.8rem,3.4vw,2.6rem)] text-[var(--color-text-primary)]">
            按时间翻
          </h2>
        </div>
        <span class="text-[13px] text-[var(--color-text-tertiary)]">倒序 · 共 {{ total }} 篇</span>
      </div>

      <!-- 纵向时间轴 -->
      <div v-if="!loading" class="timeline flex flex-col gap-12">
        <div v-for="group in groups" :key="group.key" class="reveal">
          <!-- 月份标记 -->
          <div class="timeline-month">
            <span class="month-no">{{ group.label }}</span>
            <span class="month-count">{{ group.items.length }} 篇</span>
          </div>

          <!-- 当月文章 -->
          <div class="flex flex-col">
            <article v-for="item in group.items" :key="item.id" class="timeline-item group">
              <RouterLink :to="`/article/${item.id}`" class="block py-5">
                <div class="flex items-center gap-3 mb-2 text-[11.5px] text-[var(--color-text-quaternary)]">
                  <span class="tabular-nums">{{ day(item.publishTime) }}</span>
                  <span class="w-3 h-px bg-[var(--color-border-strong)]" aria-hidden="true" />
                  <span>{{ item.category?.name ?? '文集' }}</span>
                  <span
                    v-for="tag in item.tags.slice(0, 2)"
                    :key="tag.id"
                    class="text-[var(--color-accent-2)]"
                  ># {{ tag.name }}</span>
                </div>

                <h3
                  class="text-[17px] leading-[1.5] font-semibold text-[var(--color-text-primary)] transition-colors duration-300 group-hover:text-[var(--color-accent)]"
                >
                  {{ item.title }}
                </h3>
                <p class="mt-2 max-w-[560px] text-[13.5px] leading-[1.75] text-[var(--color-text-secondary)] line-clamp-2">
                  {{ item.summary }}
                </p>

                <p class="mt-3 text-[11.5px] text-[var(--color-text-quaternary)] opacity-0 -translate-y-0.5 transition-all duration-300 group-hover:opacity-100 group-hover:translate-y-0">
                  {{ formatCount(item.viewCount) }} 阅读 · {{ item.likeCount }} 点赞 →
                </p>
              </RouterLink>
            </article>
          </div>
        </div>
      </div>

      <!-- 骨架 -->
      <div v-else class="flex flex-col gap-6">
        <Skeleton v-for="i in 3" :key="i" :count="2" />
      </div>

      <!-- 底部链接 -->
      <div v-if="!loading && groups.length" class="reveal mt-14 text-center">
        <RouterLink to="/archive" class="btn btn-secondary !px-7 !py-3 !rounded-[14px]">
          翻阅归档
          <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import dayjs from 'dayjs'
import type { Article } from '@/types'
import Skeleton from '@/components/Skeleton.vue'
import { formatCount } from '@/utils/format'

const props = defineProps<{ articles: Article[]; loading: boolean }>()

interface MonthGroup {
  key: string
  label: string
  items: Article[]
}

/** 按年-月分组，组内按时间倒序 */
const groups = computed<MonthGroup[]>(() => {
  const map = new Map<string, Article[]>()
  for (const a of props.articles) {
    if (!a.publishTime) continue
    const d = dayjs(a.publishTime)
    const key = d.format('YYYY-MM')
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(a)
  }
  return Array.from(map.entries())
    .sort((x, y) => (x[0] < y[0] ? 1 : -1))
    .map(([key, items]) => ({
      key,
      label: dayjs(`${key}-01`).format('M月'),
      items: items.sort((x, y) => (x.publishTime! < y.publishTime! ? 1 : -1))
    }))
})

const total = computed(() => props.articles.length)

const day = (t: string | null) => (t ? dayjs(t).format('D日') : '')
</script>
