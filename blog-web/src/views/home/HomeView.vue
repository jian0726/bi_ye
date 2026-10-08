<template>
  <div ref="root">
    <!-- ① 全屏墨色刊头 + 横滑文章带 -->
    <MagazineHero
      :site="site"
      :articles="allArticles"
      :stats="stats"
      :tagline="tagline"
    />

    <!-- ② 最近写的（目录式三栏） -->
    <FeaturedEditorial v-if="recent.length" :articles="recent" />

    <!-- ③ 按时间翻（纵向时间轴） -->
    <TimelineList :articles="restArticles" :loading="loading" />

    <!-- ④ 封底收束：满幅墨色，避免大屏底部两侧空旷、页面戛然而止 -->
    <section class="relative overflow-hidden" :style="{ background: 'var(--ink-bg)' }">
      <div class="absolute inset-0 pointer-events-none" aria-hidden="true">
        <div
          class="absolute -top-[60%] left-1/2 -translate-x-1/2 w-[640px] h-[640px] rounded-full opacity-[0.16] blur-[120px]"
          :style="{ background: 'radial-gradient(circle, var(--ink-accent) 0%, transparent 62%)' }"
        />
      </div>
      <div class="relative z-10 flex flex-col items-center text-center py-[clamp(4.5rem,9vw,7rem)] px-6">
        <p class="text-[11px] font-semibold tracking-[0.3em] uppercase mb-6" :style="{ color: 'var(--ink-accent)' }">
          Keep Writing
        </p>
        <p
          class="display-serif text-[clamp(1.6rem,3vw,2.4rem)] leading-[1.5] tracking-[0.02em]"
          :style="{ color: 'var(--ink-text)' }"
        >
          先写下来，再谈写得好不好。
        </p>
        <p class="mt-5 text-[12.5px] tracking-[0.08em]" :style="{ color: 'var(--ink-text-tertiary)' }">
          已写 {{ totalArticles }} 篇 · {{ new Date().getFullYear() }} 年，还在继续
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import MagazineHero from './MagazineHero.vue'
import FeaturedEditorial from './FeaturedEditorial.vue'
import TimelineList from './TimelineList.vue'
import { useSiteStore } from '@/stores/site'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { getArticleList, getCategories, getSiteStats, getTags } from '@/api'
import type { Article, Category, SiteStats, Tag } from '@/types'
import { formatCount } from '@/utils/format'

const root = ref<HTMLElement>()
const { refresh } = useScrollReveal(root)

const siteStore = useSiteStore()

const site = computed(() => siteStore.config ?? {
  siteName: '简柚',
  siteSubtitle: '把日子写下来',
  siteLogo: '',
  siteDescription: '一个记录学习路程与日常的个人空间',
  siteKeywords: '',
  siteAuthor: '博主',
  authorAvatar: '',
  authorBio: '',
  icpNumber: '',
  policeNumber: '',
  copyright: '',
  githubUrl: '',
  emailAddress: '',
  aboutContent: ''
})

/** 刊名副行（杂志标语） */
const tagline = '把日子写下来，\n把成长留下来。'

const loading = ref(true)
const allArticles = ref<Article[]>([])
const totalArticles = ref(0)
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
/** 全站统计（含全站累计浏览量），与后台仪表盘同口径 */
const siteStats = ref<SiteStats | null>(null)

/** 「最近写的」取前 3 篇 */
const recent = computed(() => allArticles.value.slice(0, 3))

/** 时间轴去掉前 3 篇，避免重复 */
const restArticles = computed(() => allArticles.value.slice(3))

/** 刊底统计 */
const stats = computed(() => [
  { label: '篇文章', value: String(totalArticles.value) },
  { label: '个分类', value: String(categories.value.length) },
  { label: '个标签', value: String(tags.value.length) },
  {
    label: '次阅读',
    // 全站合计（后端 SUM(view_count)），与后台仪表盘一致；
    // 不能用当页列表相加——那样首页只算到最近 12 篇
    value: formatCount(siteStats.value?.viewCount ?? 0)
  }
])

onMounted(async () => {
  try {
    const [list, cats, tagList, overview] = await Promise.all([
      getArticleList({ page: 1, size: 12 }),
      getCategories(),
      getTags(),
      getSiteStats()
    ])
    allArticles.value = list.records
    totalArticles.value = list.total
    categories.value = cats
    tags.value = tagList
    siteStats.value = overview
  } catch (err) {
    console.error('[home] 加载数据失败', err)
  } finally {
    loading.value = false
    // 数据渲染完成后重新观察新增的 .reveal 元素
    await nextTick()
    requestAnimationFrame(refresh)
  }
})
</script>
