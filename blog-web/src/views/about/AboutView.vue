<template>
  <div>
    <PageMasthead
      title="家"
      kicker="Home Sweet Home"
      subtitle="欢迎进来坐坐。"
      :meta="['NO.00', '简之航', '在此落脚']"
      ghost="家"
    />

    <div class="shell pt-14 pb-[var(--section-gap)]">
    <!-- 版权页式三栏：竖排大字 · 正文 · 信息栏 -->
    <div class="grid grid-cols-1 lg:grid-cols-[1fr_300px] xl:grid-cols-[64px_1fr_300px] gap-14 lg:gap-16 xl:gap-14">
      <!-- 左：竖排衬线大字（超宽屏专属，呼应文章页页边小字） -->
      <div class="hidden xl:block">
        <span
          class="sticky top-32 block display-serif text-[clamp(3rem,4vw,4rem)] leading-none tracking-[0.35em] select-none"
          :style="{ color: 'var(--color-accent-2)', writingMode: 'vertical-rl' }"
          aria-hidden="true"
        >欢迎回家</span>
      </div>

      <!-- 中：正文 -->
      <div>
        <!-- 版权页头部：信息条 + 衬线小传 -->
        <header class="mb-14">
          <div class="masthead-line mb-10">家 · About</div>
          <p class="display-serif max-w-[620px] text-[clamp(1.15rem,2vw,1.45rem)] leading-[1.95] text-[var(--color-text-primary)]">
            {{ site.authorBio }}
          </p>
        </header>

        <!-- Markdown 正文 -->
        <div v-if="aboutHtml" class="markdown-body" v-html="aboutHtml" />

        <!-- 默认内容（未配置 about 时展示） -->
        <div v-else class="markdown-body">
          <h2>这个家是什么</h2>
          <p>
            一个用来记录学习路程的地方。学到的东西、走过的路、拍下的照片、想明白的道理，都会收在这里。
            不追求更新频率，只保证每篇都是自己真正经历过的东西。
          </p>
          <p>
            在学软件技术，白天上课、泡图书馆，晚上回宿舍写点东西。
            比起教别人什么，更喜欢记录自己正在经历的日子。
          </p>

          <!-- 家里的房间：杂志目录页 -->
          <h2>家里的房间</h2>
          <div class="not-prose my-6 flex flex-col">
            <RouterLink
              v-for="(room, ri) in rooms"
              :key="room.label"
              :to="room.path"
              class="group hover-rule flex items-baseline gap-5 border-b border-[var(--color-divider)] py-4"
            >
              <span class="editorial-no !text-[1.35rem] w-9 shrink-0">{{ String(ri + 1).padStart(2, '0') }}</span>
              <span class="display-serif text-[17px] text-[var(--color-text-primary)] transition-colors duration-200 group-hover:text-[var(--color-accent)]">
                {{ room.label }}
              </span>
              <span class="ml-auto text-right text-[12.5px] text-[var(--color-text-tertiary)]">{{ room.desc }}</span>
            </RouterLink>
          </div>

          <h2>最近在做的事</h2>
          <div class="not-prose flex flex-wrap gap-2 my-6">
            <span v-for="t in stack" :key="t" class="chip">{{ t }}</span>
          </div>

          <h2>联系我</h2>
          <p>
            有问题或想交流，可以
            <a :href="`mailto:${site.emailAddress}`">发邮件</a> 给我，
            或者到 <RouterLink to="/message">留言板</RouterLink> 留言。
          </p>
        </div>

        <!-- 写给路过的人 -->
        <section class="mt-20">
          <div class="masthead-line mb-8">写给路过的人</div>
          <p class="display-serif max-w-[560px] text-[clamp(1.05rem,1.8vw,1.25rem)] leading-[2] text-[var(--color-text-secondary)]">
            这里不教你怎么活，也不打算说服谁。只是把日子一页一页写下来——哪天你路过，翻到哪页算哪页，希望有一页能让你觉得「哦，原来也有人这样过」。
          </p>
        </section>
      </div>

      <!-- 右：信息卡 -->
      <aside class="flex flex-col gap-6">
        <div class="card p-6">
          <h3 class="text-[13px] font-semibold tracking-[0.02em] uppercase text-[var(--color-text-tertiary)] mb-4">
            站点数据
          </h3>
          <div class="flex flex-col gap-3.5">
            <div v-for="item in siteStats" :key="item.label" class="flex items-center justify-between">
              <span class="text-[13.5px] text-[var(--color-text-secondary)]">{{ item.label }}</span>
              <span class="text-[14px] font-semibold tabular-nums text-[var(--color-text-primary)]">
                {{ item.value }}
              </span>
            </div>
          </div>
        </div>

        <div class="card p-6">
          <h3 class="text-[13px] font-semibold tracking-[0.02em] uppercase text-[var(--color-text-tertiary)] mb-4">
            找到我
          </h3>
          <div class="flex flex-col gap-1">
            <a
              v-if="site.githubUrl"
              :href="site.githubUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="flex items-center gap-2.5 py-2 text-[13.5px] text-[var(--color-text-secondary)] transition-colors duration-200 hover:text-[var(--color-accent)]"
            >
              GitHub
              <svg width="12" height="12" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path
                  d="M5 11l6-6M6 5h5v5"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </a>
            <a
              v-if="site.emailAddress"
              :href="`mailto:${site.emailAddress}`"
              class="flex items-center gap-2.5 py-2 text-[13.5px] text-[var(--color-text-secondary)] transition-colors duration-200 hover:text-[var(--color-accent)]"
            >
              邮箱
            </a>
            <RouterLink
              to="/message"
              class="flex items-center gap-2.5 py-2 text-[13.5px] text-[var(--color-text-secondary)] transition-colors duration-200 hover:text-[var(--color-accent)]"
            >
              留言板
            </RouterLink>
            <RouterLink
              to="/toolbox"
              class="flex items-center gap-2.5 py-2 text-[13.5px] text-[var(--color-text-secondary)] transition-colors duration-200 hover:text-[var(--color-accent)]"
            >
              百宝箱
            </RouterLink>
          </div>
        </div>
      </aside>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import PageMasthead from '@/components/PageMasthead.vue'
import { useSiteStore } from '@/stores/site'
import { getArticleList, getCategories, getSiteStats, getTags } from '@/api'
import { renderMarkdown } from '@/utils/markdown'
import { formatCount } from '@/utils/format'

const siteStore = useSiteStore()

const site = computed(() => siteStore.config ?? {
  siteName: '简柚',
  siteSubtitle: '',
  siteLogo: '',
  siteDescription: '',
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

const aboutHtml = computed(() =>
  site.value.aboutContent ? renderMarkdown(site.value.aboutContent) : ''
)

const stats = ref({ articles: 0, views: 0, categories: 0, tags: 0 })

const siteStats = computed(() => [
  { label: '文章', value: stats.value.articles },
  { label: '阅读', value: formatCount(stats.value.views) },
  { label: '分类', value: stats.value.categories },
  { label: '标签', value: stats.value.tags }
])

const stack = [
  '读完《人类简史》', '攒钱去凤凰', '每天拍一张照片',
  '把书桌收拾舒服', '给柚子拍照', '学做饭（进度 3/100）',
  '早睡（在努力）', '期末不挂科'
]

/** 家里的房间（快捷入口） */
const rooms = [
  { label: '记录', path: '/category/1', desc: '学习路程，一步步写下来' },
  { label: '游记', path: '/category/2', desc: '走过的地方' },
  { label: '随笔', path: '/category/3', desc: '想到什么写什么' },
  { label: '相册', path: '/album', desc: '快门留下的瞬间' },
  { label: '百宝箱', path: '/toolbox', desc: '顺手的工具都收在这' },
  { label: '留言', path: '/message', desc: '来串门请留言' }
]

onMounted(async () => {
  try {
    const [list, cats, tags, overview] = await Promise.all([
      getArticleList({ page: 1, size: 100 }),
      getCategories(),
      getTags(),
      getSiteStats()
    ])
    stats.value = {
      articles: list.total,
      // 阅读量取后端全站合计，与后台仪表盘同口径（不再把当页列表相加）
      views: overview.viewCount,
      categories: cats.length,
      tags: tags.length
    }
  } catch (err) {
    console.error('[about] 加载统计失败', err)
  }
})
</script>
