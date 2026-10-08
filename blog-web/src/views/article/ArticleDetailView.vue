<template>
  <div>
    <!-- 阅读进度条 -->
    <div
      class="fixed top-0 left-0 h-[2px] z-[300] bg-[var(--color-accent)] transition-[width] duration-75"
      :style="{ width: `${progress * 100}%` }"
      aria-hidden="true"
    />

    <!-- 加载中：墨色刊头骨架 + 正文骨架 -->
    <template v-if="loading">
      <section class="relative overflow-hidden" :style="{ background: 'var(--ink-bg)' }">
        <div class="absolute inset-0 pointer-events-none" aria-hidden="true">
          <div
            class="absolute -top-[45%] right-[-12%] w-[720px] h-[720px] rounded-full opacity-[0.18] blur-[120px]"
            :style="{ background: 'radial-gradient(circle, var(--ink-accent) 0%, transparent 62%)' }"
          />
          <div class="absolute inset-0 opacity-[0.035] mix-blend-overlay noise-layer" />
        </div>
        <div
          class="relative z-10 shell-full flex flex-col justify-end"
          :style="{ minHeight: '52vh', paddingTop: 'calc(var(--header-height) + clamp(2rem, 4vw, 3rem))', paddingBottom: 'clamp(2.5rem, 5vw, 4rem)' }"
        >
          <div class="skeleton-ink h-3 w-44 mb-10" />
          <div class="skeleton-ink w-3/4 mb-4" style="height: clamp(2rem, 5vw, 3.4rem)" />
          <div class="skeleton-ink w-2/5 mb-9" style="height: clamp(2rem, 5vw, 3.4rem)" />
          <div class="skeleton-ink h-4 w-64" />
        </div>
      </section>
      <div class="shell-prose pt-12 pb-[var(--section-gap)]">
        <div class="flex flex-col gap-3">
          <div v-for="i in 8" :key="i" class="skeleton h-4" :class="i % 3 === 0 ? 'w-4/5' : 'w-full'" />
        </div>
      </div>
    </template>

    <template v-else-if="article">
      <!-- ===== 文章刊头：与起始页同族的墨色杂志头 ===== -->
      <section class="relative overflow-hidden" :style="{ background: 'var(--ink-bg)' }">
        <div class="absolute inset-0 pointer-events-none" aria-hidden="true">
          <div
            class="absolute -top-[45%] right-[-12%] w-[720px] h-[720px] rounded-full opacity-[0.20] blur-[120px]"
            :style="{ background: 'radial-gradient(circle, var(--ink-accent) 0%, transparent 62%)' }"
          />
          <div
            class="absolute bottom-[-55%] left-[-8%] w-[620px] h-[620px] rounded-full opacity-[0.13] blur-[110px]"
            :style="{ background: 'radial-gradient(circle, #2f6b55 0%, transparent 65%)' }"
          />
          <div class="absolute inset-0 opacity-[0.035] mix-blend-overlay noise-layer" />
        </div>

        <!-- 竖排幽灵字：分类名 -->
        <div
          v-if="article.category"
          class="hidden xl:flex absolute right-6 top-1/2 -translate-y-1/2 z-10 select-none pointer-events-none"
          aria-hidden="true"
        >
          <span
            class="display-serif text-[clamp(2.6rem,4.4vw,4rem)] leading-none tracking-[0.3em]"
            :style="{ color: 'var(--ink-text)', opacity: 0.09, writingMode: 'vertical-rl' }"
          >{{ article.category.name }}</span>
        </div>

        <div
          class="relative z-10 shell-full flex flex-col justify-end"
          :style="{ minHeight: '52vh', paddingTop: 'calc(var(--header-height) + clamp(2rem, 4vw, 3rem))', paddingBottom: 'clamp(2.5rem, 5vw, 4rem)' }"
        >
          <!-- 面包屑 -->
          <nav
            class="flex items-center gap-2 text-[12.5px] mb-8"
            :style="{ color: 'var(--ink-text-tertiary)' }"
          >
            <RouterLink
              to="/"
              class="transition-colors duration-200 hover:text-[var(--ink-text)]"
            >
              今日
            </RouterLink>
            <span class="opacity-50">/</span>
            <RouterLink
              v-if="article.category"
              :to="`/category/${article.category.id}`"
              class="transition-colors duration-200 hover:text-[var(--ink-text)]"
            >
              {{ article.category.name }}
            </RouterLink>
            <span v-else>未分类</span>
          </nav>

          <!-- 期号信息条 -->
          <div class="masthead-ink mb-8">
            <span>NO.{{ issueNo }}</span>
            <span>{{ formatDate(article.publishTime) }}</span>
            <span>{{ article.wordCount }} 字</span>
            <span>{{ article.readMinutes }} 分钟阅读</span>
          </div>

          <!-- 衬线大标题 -->
          <h1
            class="display-serif max-w-[820px] text-[clamp(2.1rem,5vw,3.9rem)] leading-[1.18] tracking-[0.01em]"
            :style="{ color: 'var(--ink-text)' }"
          >
            {{ article.title }}
          </h1>

          <!-- 摘要 -->
          <p
            v-if="article.summary"
            class="mt-6 max-w-[620px] display-serif text-[clamp(1rem,1.6vw,1.15rem)] leading-[1.9]"
            :style="{ color: 'var(--ink-text-secondary)' }"
          >
            {{ article.summary }}
          </p>

          <!-- 作者 + 标签 -->
          <div class="mt-9 flex flex-wrap items-center gap-x-5 gap-y-3">
            <span
              class="flex items-center gap-2.5 text-[13px]"
              :style="{ color: 'var(--ink-text-secondary)' }"
            >
              <span
                class="w-7 h-7 rounded-full flex items-center justify-center text-[11px] font-semibold text-white"
                :style="{ backgroundColor: stringToColor(article.author?.nickname) }"
              >
                {{ nameInitial(article.author?.nickname) }}
              </span>
              {{ article.author?.nickname }}
            </span>
            <div v-if="article.tags?.length" class="flex flex-wrap gap-2">
              <RouterLink
                v-for="tag in article.tags"
                :key="tag.id"
                :to="`/tag/${tag.id}`"
                class="chip-ink"
              >
                <span
                  class="w-1.5 h-1.5 rounded-full mr-1.5 shrink-0"
                  :style="{ backgroundColor: tag.color ?? 'var(--ink-accent)' }"
                />
                {{ tag.name }}
              </RouterLink>
            </div>
          </div>
        </div>
      </section>

      <article class="shell-prose article-narrow pt-10 pb-[var(--section-gap)]">
      <!-- 页边竖排小字：填充正文两侧留白（仅超宽屏显示） -->
      <span
        class="hidden 2xl:block fixed left-8 top-1/2 -translate-y-1/2 display-serif text-[13px] tracking-[0.5em] select-none pointer-events-none z-10"
        :style="{ color: 'var(--color-text-quaternary)', writingMode: 'vertical-rl' }"
        aria-hidden="true"
      >简柚志 · {{ article.category?.name ?? '文集' }}</span>
      <span
        class="hidden 2xl:block fixed right-8 top-1/2 -translate-y-1/2 display-serif text-[13px] tracking-[0.5em] select-none pointer-events-none z-10"
        :style="{ color: 'var(--color-text-quaternary)', writingMode: 'vertical-rl' }"
        aria-hidden="true"
      >{{ publishYear }} 年 · {{ article.readMinutes }} 分钟</span>
      <!-- ===== 正文 + 目录 ===== -->
      <div class="relative">
        <!-- 目录（桌面端侧边悬浮） -->
        <nav
          v-if="toc.length"
          class="hidden xl:block absolute -right-[260px] top-0 w-[220px]"
          aria-label="目录"
        >
          <div class="sticky top-[calc(var(--header-height)+32px)]">
            <h4
              class="text-[12px] font-semibold tracking-[0.04em] uppercase text-[var(--color-text-tertiary)] mb-3.5"
            >
              目录
            </h4>
            <ul class="flex flex-col gap-2.5 border-l border-[var(--color-divider)]">
              <li v-for="item in toc" :key="item.id">
                <a
                  :href="`#${item.id}`"
                  class="block text-[12.5px] leading-[1.5] text-[var(--color-text-tertiary)] border-l-2 -ml-px pl-3.5 transition-all duration-200 hover:text-[var(--color-text-primary)]"
                  :class="[
                    item.level === 3 ? 'pl-6' : '',
                    activeHeading === item.id
                      ? 'border-[var(--color-accent)] !text-[var(--color-accent)]'
                      : 'border-transparent'
                  ]"
                >
                  {{ item.text }}
                </a>
              </li>
            </ul>
          </div>
        </nav>

        <!-- 正文 -->
        <div class="markdown-body" v-html="contentHtml" />
      </div>

      <!-- ===== 操作栏 ===== -->
      <div class="mt-16 flex items-center justify-center gap-3">
        <button
          class="btn !px-5 !py-2.5 gap-2"
          :class="
            liked
              ? 'bg-[var(--color-accent)] text-white'
              : 'bg-[var(--color-surface-sunken)] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
          "
          @click="toggleLike"
        >
          <svg width="15" height="15" viewBox="0 0 16 16" :fill="liked ? 'currentColor' : 'none'" aria-hidden="true">
            <path
              d="M8 13.5S2 9.8 2 6.2A3.2 3.2 0 018 3.7a3.2 3.2 0 016 2.5c0 3.6-6 7.3-6 7.3z"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linejoin="round"
            />
          </svg>
          {{ likeCount }}
        </button>

        <button
          class="btn !px-5 !py-2.5 gap-2"
          :class="
            collected
              ? 'bg-[var(--color-accent)] text-white'
              : 'bg-[var(--color-surface-sunken)] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
          "
          @click="toggleCollect"
        >
          <svg width="15" height="15" viewBox="0 0 16 16" :fill="collected ? 'currentColor' : 'none'" aria-hidden="true">
            <path
              d="M4 2.5h8a1 1 0 011 1v10l-5-3-5 3v-10a1 1 0 011-1z"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linejoin="round"
            />
          </svg>
          {{ collectCount }}
        </button>

        <button
          class="btn bg-[var(--color-surface-sunken)] text-[var(--color-text-secondary)] !px-5 !py-2.5 gap-2 hover:text-[var(--color-text-primary)]"
          @click="copyLink"
        >
          <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path
              d="M6.5 9.5a2.6 2.6 0 003.9 0l2.1-2.1a2.6 2.6 0 10-3.7-3.7l-.9.9M9.5 6.5a2.6 2.6 0 00-3.9 0L3.5 8.6a2.6 2.6 0 103.7 3.7l.9-.9"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
            />
          </svg>
          {{ copied ? '已复制' : '分享' }}
        </button>
      </div>

      <!-- 上下篇：杂志式翻页 -->
      <nav class="mt-16 grid grid-cols-1 sm:grid-cols-2 border-t border-[var(--color-divider)]">
        <RouterLink
          v-if="prevArticle"
          :to="`/article/${prevArticle.id}`"
          class="group py-7 pr-8 border-b sm:border-b-0 sm:border-r border-[var(--color-divider)]"
        >
          <div class="flex items-center gap-2 text-[11px] font-medium tracking-[0.22em] uppercase text-[var(--color-text-tertiary)] mb-2.5">
            <span class="transition-transform duration-300 group-hover:-translate-x-1">←</span>
            上一篇
          </div>
          <div
            class="display-serif text-[17px] leading-[1.5] text-[var(--color-text-primary)] transition-colors duration-200 group-hover:text-[var(--color-accent)] line-clamp-2"
          >
            {{ prevArticle.title }}
          </div>
        </RouterLink>
        <div v-else class="hidden sm:block sm:border-r border-[var(--color-divider)]" />

        <RouterLink
          v-if="nextArticle"
          :to="`/article/${nextArticle.id}`"
          class="group py-7 pl-8 text-right"
        >
          <div class="flex items-center justify-end gap-2 text-[11px] font-medium tracking-[0.22em] uppercase text-[var(--color-text-tertiary)] mb-2.5">
            下一篇
            <span class="transition-transform duration-300 group-hover:translate-x-1">→</span>
          </div>
          <div
            class="display-serif text-[17px] leading-[1.5] text-[var(--color-text-primary)] transition-colors duration-200 group-hover:text-[var(--color-accent)] line-clamp-2"
          >
            {{ nextArticle.title }}
          </div>
        </RouterLink>
      </nav>
    </article>
    </template>
    <!-- 文章不存在 -->
    <div v-else class="shell-prose pt-[calc(var(--header-height)+6rem)] pb-[var(--section-gap)] text-center">
      <p class="text-[15px] text-[var(--color-text-secondary)] mb-6">文章不存在或已被删除</p>
      <RouterLink to="/" class="btn btn-primary">回今日</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useScrollProgress } from '@/composables/useScrollReveal'
import { useUserStore } from '@/stores/user'
import {
  collectArticle,
  getArticleDetail,
  getArticleList,
  isArticleCollected,
  likeArticle,
  uncollectArticle,
  unlikeArticle
} from '@/api'
import type { Article } from '@/types'
import { formatDate, nameInitial, stringToColor } from '@/utils/format'
import { extractToc, renderMarkdown, withHeadingAnchors, type TocItem } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { progress } = useScrollProgress()

const loading = ref(true)
const article = ref<Article | null>(null)

/** 页边竖排小字用的发布年份 */
const publishYear = computed(() =>
  article.value?.publishTime ? dayjs(article.value.publishTime).format('YYYY') : ''
)
const toc = ref<TocItem[]>([])
const contentHtml = ref('')
const activeHeading = ref('')

const prevArticle = ref<Article | null>(null)
const nextArticle = ref<Article | null>(null)

const liked = ref(false)
const collected = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)
const likeBusy = ref(false)

const copied = ref(false)

/** 期号：文章 id 后两位，呼应全站 NO.xx 信息条体系 */
const issueNo = computed(() => String((article.value?.id ?? 0) % 100).padStart(2, '0'))

/** 渲染正文 */
function buildContent(source: string) {
  const html = renderMarkdown(source)
  contentHtml.value = withHeadingAnchors(html)
  toc.value = extractToc(contentHtml.value)
}

/** 加载文章 */
async function loadArticle() {
  loading.value = true
  const id = Number(route.params.id)

  try {
    const data = await getArticleDetail(id)
    article.value = data
    likeCount.value = data.likeCount
    collectCount.value = data.collectCount
    // 点赞状态由后端按当前登录账号回显（刷新后红心保持）
    liked.value = Boolean(data.liked)
    buildContent(data.content ?? '')

    // 收藏状态（未登录后端返回 false，不报错）
    try {
      collected.value = await isArticleCollected(id)
    } catch {
      collected.value = false
    }

    // 上一篇 / 下一篇
    const list = await getArticleList({ page: 1, size: 100 })
    const index = list.records.findIndex((a) => a.id === id)
    if (index > -1) {
      prevArticle.value = list.records[index - 1] ?? null
      nextArticle.value = list.records[index + 1] ?? null
    }
  } catch (err) {
    console.error('[article] 加载失败', err)
    article.value = null
  } finally {
    loading.value = false
    await nextTick()
    setupHeadingObserver()
  }
}

/** 监听标题进入视口，高亮目录 */
let headingObserver: IntersectionObserver | null = null

function setupHeadingObserver() {
  headingObserver?.disconnect()
  const headings = document.querySelectorAll<HTMLElement>('.markdown-body h2[id], .markdown-body h3[id]')
  if (!headings.length) return

  headingObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries.filter((e) => e.isIntersecting)
      if (visible.length) {
        activeHeading.value = visible[0].target.id
      }
    },
    { rootMargin: '-80px 0px -70% 0px', threshold: 0 }
  )

  headings.forEach((h) => headingObserver!.observe(h))
}

/** 点赞：真实接口（按登录账号维度落库，未登录引导登录）；乐观更新，与后端状态冲突时以服务端为准 */
async function toggleLike() {
  if (likeBusy.value) return
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  likeBusy.value = true
  const next = !liked.value
  liked.value = next
  likeCount.value += next ? 1 : -1
  try {
    if (next) await likeArticle(Number(route.params.id))
    else await unlikeArticle(Number(route.params.id))
  } catch (err) {
    const msg = err instanceof Error ? err.message : ''
    if (msg.includes('已经点过赞')) {
      // 本地状态过期：后端已有点赞记录，计数里本就包含这一次
      liked.value = true
      likeCount.value -= 1
    } else if (msg.includes('还没有点过赞')) {
      liked.value = false
      likeCount.value += 1
    } else {
      liked.value = !next
      likeCount.value += next ? -1 : 1
      if (msg.includes('请先登录')) {
        // 登录态已失效：回滚后引导重新登录
        router.push({ name: 'login', query: { redirect: route.fullPath } })
      }
    }
  } finally {
    likeBusy.value = false
  }
}

/** 收藏：真实接口，按登录用户隔离；未登录引导去登录 */
async function toggleCollect() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const id = Number(route.params.id)
  const next = !collected.value
  // 乐观更新，失败再回滚
  collected.value = next
  collectCount.value += next ? 1 : -1
  try {
    if (next) await collectArticle(id)
    else await uncollectArticle(id)
  } catch (err) {
    collected.value = !next
    collectCount.value += next ? -1 : 1
    console.warn('[article] 收藏操作失败', err)
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    copied.value = true
    setTimeout(() => (copied.value = false), 2000)
  } catch {
    copied.value = false
  }
}


watch(() => route.params.id, loadArticle)

onMounted(loadArticle)
</script>

<style scoped>
/* 正文窄版心：680px 阅读宽度，标题目录悬浮在外侧 */
.article-narrow {
  max-width: 680px;
  margin-inline: auto;
}

/* 刊头期号信息条：与 PageMasthead 同族（单侧短线变体） */
.masthead-ink {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.6rem 1rem;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: var(--ink-text-tertiary);
}
.masthead-ink::before {
  content: '';
  width: 32px;
  height: 1px;
  background: var(--ink-divider);
}

/* 墨色区标签胶囊 */
.chip-ink {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--ink-text-secondary);
  border: 1px solid var(--ink-divider);
  background: rgba(244, 241, 232, 0.04);
  transition: color 0.2s, border-color 0.2s;
}
.chip-ink:hover {
  color: var(--ink-text);
  border-color: var(--ink-accent);
}

/* 刊头噪点（同 PageMasthead） */
.noise-layer {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}

/* 墨色区加载骨架 */
.skeleton-ink {
  background: rgba(244, 241, 232, 0.08);
  border-radius: 6px;
}
</style>
