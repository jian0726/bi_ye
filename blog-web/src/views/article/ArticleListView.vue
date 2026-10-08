<template>
  <div>
    <PageMasthead
      :title="pageTitle"
      kicker="Articles"
      :subtitle="pageSubtitle"
      :meta="[`NO.05`, `${total} 篇`, '按时间']"
      :ghost="pageTitle"
    />

    <div ref="root" class="shell pt-12 pb-[var(--section-gap)]">
    <!-- 列表 -->
    <Skeleton v-if="loading" :count="5" />

    <div v-else-if="articles.length" class="grid grid-cols-1 md:grid-cols-2 gap-5">
      <ArticleCard
        v-for="(article, index) in articles"
        :key="article.id"
        :article="article"
        class="reveal"
        :class="`reveal-delay-${Math.min(index + 1, 6)}`"
      />
    </div>

    <!-- 空状态 -->
    <div v-else class="py-24 text-center">
      <div
        class="w-14 h-14 mx-auto rounded-full bg-[var(--color-surface-sunken)] flex items-center justify-center text-[var(--color-text-tertiary)] mb-5"
      >
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="10.5" cy="10.5" r="7" stroke="currentColor" stroke-width="1.8" />
          <path d="M16 16l5 5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
        </svg>
      </div>
      <p class="text-[15px] text-[var(--color-text-secondary)] mb-1.5">没有找到相关文章</p>
      <p class="text-[13px] text-[var(--color-text-tertiary)]">换个关键词或分类试试</p>
    </div>

    <!-- 分页 -->
    <div v-if="!loading && totalPages > 1" class="reveal mt-12">
      <Pagination :page="page" :total-pages="totalPages" @update:page="changePage" />
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ArticleCard from '@/components/ArticleCard.vue'
import PageMasthead from '@/components/PageMasthead.vue'
import Pagination from '@/components/Pagination.vue'
import Skeleton from '@/components/Skeleton.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { getArticleList, getCategories, getTags } from '@/api'
import type { Article, Category, Tag } from '@/types'

const route = useRoute()
const root = ref<HTMLElement>()
useScrollReveal(root)

const loading = ref(true)
const articles = ref<Article[]>([])
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])

const page = ref(1)
const total = ref(0)
const totalPages = ref(0)

/** 当前筛选条件（从路由推导，支持 /category/:id 与 /tag/:id 复用本组件） */
const activeCategoryId = computed(() => {
  if (route.name === 'category') return Number(route.params.id)
  const q = route.query.categoryId
  return q ? Number(q) : undefined
})

const activeTagId = computed(() => {
  if (route.name === 'tag') return Number(route.params.id)
  const q = route.query.tagId
  return q ? Number(q) : undefined
})

const keyword = computed(() => (route.query.keyword as string) || '')

const activeCategory = computed(
  () => categories.value.find((c) => c.id === activeCategoryId.value) ?? null
)

const activeTag = computed(() => tags.value.find((t) => t.id === activeTagId.value) ?? null)

/** 刊头标题：标签页显示标签名，其余显示「文章」 */
const pageTitle = computed(() => {
  if (route.name === 'tag' && activeTag.value) return activeTag.value.name
  return '文章'
})

const pageKicker = computed(() =>
  route.name === 'tag' ? 'Tag' : route.name === 'search' ? 'Search' : 'Articles'
)

const pageSubtitle = computed(() => {
  if (route.name === 'search' && keyword.value) return `搜索「${keyword.value}」的结果`
  if (route.name === 'tag') return '这个标签下写过的东西。'
  return '写过的所有东西都在这里。'
})

async function fetchArticles() {
  loading.value = true
  try {
    const res = await getArticleList({
      page: page.value,
      size: 10,
      categoryId: activeCategoryId.value,
      tagId: activeTagId.value,
      keyword: keyword.value || undefined
    })
    articles.value = res.records
    total.value = res.total
    totalPages.value = res.pages
  } catch (err) {
    console.error('[articles] 加载失败', err)
    articles.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(next: number) {
  page.value = next
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 路由参数变化时重新请求
watch(
  () => [route.name, route.params.id, route.query.keyword, route.query.categoryId, route.query.tagId],
  () => {
    page.value = 1
    fetchArticles()
  }
)

onMounted(async () => {
  const [cats, tagList] = await Promise.all([getCategories(), getTags()])
  categories.value = cats
  tags.value = tagList
  await fetchArticles()
})
</script>
