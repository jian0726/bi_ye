<template>
  <div>
    <PageMasthead
      title="记录"
      kicker="Moments"
      subtitle="日常的碎片，攒起来就是来路。"
      :meta="mastheadMeta"
      ghost="此时"
    />

    <div ref="root" class="pt-14 pb-[var(--section-gap)]">
      <div class="shell shell-narrow">
        <!-- 纵向时间轴：按月分组（复用首页时间轴语言） -->
        <div v-if="!loading && articles.length" class="timeline flex flex-col gap-12">
          <div v-for="group in groups" :key="group.key" class="reveal">
            <div class="timeline-month">
              <span class="month-no">{{ group.label }}</span>
              <span class="month-count">{{ group.items.length }} 篇</span>
            </div>

            <div class="flex flex-col">
              <article v-for="item in group.items" :key="item.id" class="timeline-item group">
                <RouterLink :to="`/article/${item.id}`" class="block py-5">
                  <div class="flex items-center gap-3 mb-2 text-[11.5px] text-[var(--color-text-quaternary)]">
                    <span class="tabular-nums">{{ day(item.publishTime) }}</span>
                    <span class="w-3 h-px bg-[var(--color-border-strong)]" aria-hidden="true" />
                    <span
                      v-for="tag in item.tags.slice(0, 2)"
                      :key="tag.id"
                      class="text-[var(--color-accent-2)]"
                    ># {{ tag.name }}</span>
                  </div>

                  <h3
                    class="display-serif text-[17.5px] leading-[1.5] text-[var(--color-text-primary)] transition-colors duration-300 group-hover:text-[var(--color-accent)]"
                  >
                    {{ item.title }}
                  </h3>
                  <p class="mt-2 max-w-[560px] text-[13.5px] leading-[1.75] text-[var(--color-text-secondary)] line-clamp-2">
                    {{ item.summary }}
                  </p>
                </RouterLink>
              </article>
            </div>
          </div>
        </div>

        <Skeleton v-if="loading" :count="3" />

        <div v-if="!loading && !articles.length" class="py-24 text-center">
          <p class="text-[15px] text-[var(--color-text-secondary)] mb-1.5">还没有记录</p>
          <p class="text-[13px] text-[var(--color-text-tertiary)]">从今天开始记</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import dayjs from 'dayjs'
import PageMasthead from '@/components/PageMasthead.vue'
import Skeleton from '@/components/Skeleton.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { getArticleList } from '@/api'
import type { Article } from '@/types'

/** 记录 = 分类 1（与数据库 seed 一致） */
const CATEGORY_ID = 1

const root = ref<HTMLElement>()
const { refresh } = useScrollReveal(root)

const loading = ref(true)
const articles = ref<Article[]>([])
const total = ref(0)

const mastheadMeta = computed(() => [`NO.01`, `${total.value} 篇`, `成长记录`])

interface MonthGroup {
  key: string
  label: string
  items: Article[]
}

/** 按年-月分组，组内倒序（与首页时间轴同规则） */
const groups = computed<MonthGroup[]>(() => {
  const map = new Map<string, Article[]>()
  for (const a of articles.value) {
    if (!a.publishTime) continue
    const key = dayjs(a.publishTime).format('YYYY-MM')
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

const day = (t: string | null) => (t ? dayjs(t).format('D日') : '')

onMounted(async () => {
  try {
    const res = await getArticleList({ page: 1, size: 20, categoryId: CATEGORY_ID })
    articles.value = res.records
    total.value = res.total
  } catch (err) {
    console.error('[record] 加载失败', err)
  } finally {
    loading.value = false
    await nextTick()
    requestAnimationFrame(refresh)
  }
})
</script>
