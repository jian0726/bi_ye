<template>
  <footer class="mt-24 border-t border-[var(--color-divider)]">
    <div class="shell py-16">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-12 md:gap-8">
        <!-- 左：站点信息 -->
        <div class="md:col-span-1">
          <h3 class="text-[15px] font-semibold text-[var(--color-text-primary)] mb-3">
            {{ site.siteName }}
          </h3>
          <p class="text-[13px] leading-[1.7] text-[var(--color-text-secondary)] max-w-[280px]">
            {{ site.siteDescription }}
          </p>
          <div class="flex items-center gap-3 mt-5">
            <a
              v-if="site.githubUrl"
              :href="site.githubUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="text-[var(--color-text-tertiary)] transition-colors duration-200 hover:text-[var(--color-text-primary)]"
              aria-label="GitHub"
            >
              <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path
                  d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0016 8c0-4.42-3.58-8-8-8z"
                />
              </svg>
            </a>
            <a
              v-if="site.emailAddress"
              :href="`mailto:${site.emailAddress}`"
              class="text-[var(--color-text-tertiary)] transition-colors duration-200 hover:text-[var(--color-text-primary)]"
              aria-label="邮箱"
            >
              <svg width="18" height="18" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <rect x="1.5" y="3.5" width="13" height="9" rx="2" stroke="currentColor" stroke-width="1.4" />
                <path d="M2 5l6 4 6-4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
              </svg>
            </a>
          </div>
        </div>

        <!-- 中：导航 -->
        <div>
          <h4 class="text-[13px] font-semibold text-[var(--color-text-primary)] mb-3.5">导航</h4>
          <ul class="space-y-2.5">
            <li v-for="item in navLinks" :key="item.path">
              <RouterLink
                :to="item.path"
                class="text-[13px] text-[var(--color-text-secondary)] transition-colors duration-200 hover:text-[var(--color-text-primary)]"
              >
                {{ item.label }}
              </RouterLink>
            </li>
          </ul>
        </div>

        <!-- 右：分类 -->
        <div>
          <h4 class="text-[13px] font-semibold text-[var(--color-text-primary)] mb-3.5">分类</h4>
          <ul class="space-y-2.5">
            <li v-for="cat in categories.slice(0, 5)" :key="cat.id">
              <RouterLink
                :to="`/category/${cat.id}`"
                class="text-[13px] text-[var(--color-text-secondary)] transition-colors duration-200 hover:text-[var(--color-text-primary)]"
              >
                {{ cat.name }}
                <span class="text-[var(--color-text-quaternary)] ml-1">{{ cat.articleCount }}</span>
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>

      <!-- 底部版权 -->
      <div
        class="mt-14 pt-6 border-t border-[var(--color-divider)] flex flex-col sm:flex-row items-center justify-between gap-3"
      >
        <p class="text-[12px] text-[var(--color-text-tertiary)]">
          {{ site.copyright }}
          <span v-if="site.siteAuthor" class="ml-1">· {{ site.siteAuthor }}</span>
        </p>
        <div class="flex items-center gap-4 text-[12px] text-[var(--color-text-tertiary)]">
          <a
            v-if="site.icpNumber"
            href="https://beian.miit.gov.cn"
            target="_blank"
            rel="noopener noreferrer"
            class="transition-colors duration-200 hover:text-[var(--color-text-secondary)]"
          >
            {{ site.icpNumber }}
          </a>
          <span>把日子写下来</span>
        </div>
      </div>
      <p class="mt-3 text-center text-[11px] text-[var(--color-text-quaternary)]">
        背景音乐：Outfoxing the Fox — Jan Morgenstern · MDN Web Audio 示例 · CC BY 3.0 / SoundHelix 示例曲（免费使用）
      </p>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useSiteStore } from '@/stores/site'
import { getCategories } from '@/api'
import type { Category } from '@/types'

const siteStore = useSiteStore()
const categories = ref<Category[]>([])

const site = computed(() => siteStore.config ?? {
  siteName: '简柚',
  siteSubtitle: '',
  siteLogo: '',
  siteDescription: '',
  siteKeywords: '',
  siteAuthor: '',
  authorAvatar: '',
  authorBio: '',
  icpNumber: '',
  policeNumber: '',
  copyright: '© 2026 简柚',
  githubUrl: '',
  emailAddress: '',
  aboutContent: ''
})

const navLinks = [
  { label: '今日', path: '/' },
  { label: '归档', path: '/archive' },
  { label: '记录', path: '/category/1' },
  { label: '游记', path: '/category/2' },
  { label: '随笔', path: '/category/3' },
  { label: '相册', path: '/album' },
  { label: '百宝箱', path: '/toolbox' },
  { label: '家', path: '/about' },
  { label: '留言', path: '/message' }
]

onMounted(async () => {
  try {
    categories.value = await getCategories()
  } catch (err) {
    console.error('[footer] 加载分类失败', err)
  }
})
</script>
