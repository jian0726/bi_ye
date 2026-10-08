<template>
  <div>
    <PageMasthead
      title="游记"
      kicker="Travel Notes"
      subtitle="山川湖海，都要亲自去看。"
      :meta="mastheadMeta"
      ghost="在路上"
    />

    <div ref="root" class="shell pt-14 pb-[var(--section-gap)]">
      <Skeleton v-if="loading" :count="4" />

      <div v-else-if="articles.length" class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <ArticleCard
          v-for="(article, index) in articles"
          :key="article.id"
          :article="article"
          class="reveal"
          :class="`reveal-delay-${Math.min(index + 1, 6)}`"
        />
      </div>

      <div v-else class="py-24 text-center">
        <p class="text-[15px] text-[var(--color-text-secondary)] mb-1.5">还没写过游记</p>
        <p class="text-[13px] text-[var(--color-text-tertiary)]">等下一趟出发</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import PageMasthead from '@/components/PageMasthead.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import Skeleton from '@/components/Skeleton.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { getArticleList } from '@/api'
import type { Article } from '@/types'

/** 游记 = 分类 2（与数据库 seed 一致） */
const CATEGORY_ID = 2

const root = ref<HTMLElement>()
const { refresh } = useScrollReveal(root)

const loading = ref(true)
const articles = ref<Article[]>([])
const total = ref(0)

const mastheadMeta = computed(() => [`NO.02`, `${total.value} 篇`, `在路上`])

onMounted(async () => {
  try {
    const res = await getArticleList({ page: 1, size: 20, categoryId: CATEGORY_ID })
    articles.value = res.records
    total.value = res.total
  } catch (err) {
    console.error('[travel] 加载失败', err)
  } finally {
    loading.value = false
    await nextTick()
    requestAnimationFrame(refresh)
  }
})
</script>
