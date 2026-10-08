<template>
  <div>
    <PageMasthead
      title="随笔"
      kicker="Essays"
      subtitle="想到什么写什么，短句也值得留下。"
      :meta="mastheadMeta"
      ghost="随想"
    />

    <div ref="root" class="shell shell-narrow pt-14 pb-[var(--section-gap)]">
      <Skeleton v-if="loading" :count="3" />

      <!-- 杂志目录式列表：大序号 + 细线 + 衬线标题 -->
      <div v-else-if="articles.length" class="flex flex-col">
        <article
          v-for="(article, index) in articles"
          :key="article.id"
          class="group reveal"
          :class="`reveal-delay-${Math.min(index + 1, 6)}`"
        >
          <RouterLink
            :to="`/article/${article.id}`"
            class="grid grid-cols-[auto_1fr] gap-6 sm:gap-9 items-baseline py-9 border-t border-[var(--color-divider)]"
          >
            <span class="editorial-no tabular-nums">0{{ index + 1 }}</span>

            <div class="min-w-0">
              <div class="flex items-center gap-3 mb-2.5 text-[11.5px] tracking-[0.08em] text-[var(--color-text-quaternary)]">
                <time class="tabular-nums">{{ formatDate(article.publishTime) }}</time>
                <span class="w-3 h-px bg-[var(--color-border-strong)]" aria-hidden="true" />
                <span
                  v-for="tag in article.tags.slice(0, 2)"
                  :key="tag.id"
                  class="text-[var(--color-accent-2)]"
                ># {{ tag.name }}</span>
              </div>

              <h2
                class="display-serif text-[clamp(1.25rem,2.4vw,1.7rem)] leading-[1.45] text-[var(--color-text-primary)] transition-colors duration-300 group-hover:text-[var(--color-accent)]"
              >
                {{ article.title }}
              </h2>

              <p class="mt-3 text-[14px] leading-[1.8] text-[var(--color-text-secondary)] line-clamp-2">
                {{ article.summary }}
              </p>

              <p
                class="mt-4 text-[12px] text-[var(--color-text-tertiary)] opacity-0 -translate-y-0.5 transition-all duration-300 group-hover:opacity-100 group-hover:translate-y-0"
              >
                {{ article.readMinutes }} 分钟 · {{ formatCount(article.viewCount) }} 阅读 →
              </p>
            </div>
          </RouterLink>
        </article>

        <div class="border-t border-[var(--color-divider)]" />
      </div>

      <div v-else class="py-24 text-center">
        <p class="text-[15px] text-[var(--color-text-secondary)] mb-1.5">还没有随笔</p>
        <p class="text-[13px] text-[var(--color-text-tertiary)]">想到什么，随时写下来</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import PageMasthead from '@/components/PageMasthead.vue'
import Skeleton from '@/components/Skeleton.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { getArticleList } from '@/api'
import type { Article } from '@/types'
import { formatCount, formatDate } from '@/utils/format'

/** 随笔 = 分类 3（与数据库 seed 一致） */
const CATEGORY_ID = 3

const root = ref<HTMLElement>()
const { refresh } = useScrollReveal(root)

const loading = ref(true)
const articles = ref<Article[]>([])
const total = ref(0)

const mastheadMeta = computed(() => [`NO.03`, `${total.value} 篇`, `随想集`])

onMounted(async () => {
  try {
    const res = await getArticleList({ page: 1, size: 20, categoryId: CATEGORY_ID })
    articles.value = res.records
    total.value = res.total
  } catch (err) {
    console.error('[essay] 加载失败', err)
  } finally {
    loading.value = false
    await nextTick()
    requestAnimationFrame(refresh)
  }
})
</script>
