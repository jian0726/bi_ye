<template>
  <article
    class="group card card-hover overflow-hidden cursor-pointer !rounded-[18px]"
    @click="goDetail"
  >
    <!-- 封面：真实图片 或 渐变题图（杂志色 + 衬线大字） -->
    <div class="relative aspect-[16/9] overflow-hidden" :class="article.cover ? 'bg-[var(--color-surface-sunken)]' : ''">
      <img
        v-if="article.cover"
        :src="article.cover"
        :alt="article.title"
        loading="lazy"
        class="w-full h-full object-cover transition-transform duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.04]"
      />
      <div
        v-else
        class="absolute inset-0 flex items-center justify-center transition-transform duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.04]"
        :style="{ background: tone.gradient }"
      >
        <!-- 题图文字：大号衬线首二字 + 分类小字 -->
        <span
          class="display-serif text-[clamp(2rem,4.5vw,3rem)] tracking-[0.14em]"
          :style="{ color: 'rgba(255,255,255,0.88)' }"
        >{{ headline }}</span>
        <span
          class="absolute top-4 right-5 text-[11px] tracking-[0.22em] uppercase"
          :style="{ color: 'rgba(255,255,255,0.6)' }"
        >{{ article.category?.name ?? '文集' }}</span>
        <!-- 题图颗粒 -->
        <div class="absolute inset-0 opacity-[0.05] mix-blend-overlay noise-layer" />
      </div>

      <!-- 已收藏标记：仅登录且已收藏时出现（游客与未收藏不显示） -->
      <span v-if="article.collected" class="collect-badge" title="已收藏" aria-label="已收藏">
        <svg width="12" height="12" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M4 2h8a1 1 0 0 1 1 1v11l-5-3.3L3 14V3a1 1 0 0 1 1-1z" fill="#fff" />
        </svg>
      </span>
    </div>

    <div class="p-7">
      <!-- 元信息行 -->
      <div class="flex items-center gap-2.5 mb-3.5 text-[12px] text-[var(--color-text-tertiary)]">
        <span v-if="article.isTop" class="chip chip-accent !py-0.5 !px-2 !text-[11px]">置顶</span>
        <RouterLink
          v-if="article.category"
          :to="`/category/${article.category.id}`"
          class="font-medium text-[var(--color-accent)] transition-opacity duration-200 hover:opacity-70"
          @click.stop
        >
          {{ article.category.name }}
        </RouterLink>
        <span v-if="article.category" class="text-[var(--color-text-quaternary)]">·</span>
        <time>{{ formatDate(article.publishTime) }}</time>
      </div>

      <!-- 标题 -->
      <h2
        class="display-serif text-[20px] leading-[1.42] tracking-[0.01em] text-[var(--color-text-primary)] transition-colors duration-200 group-hover:text-[var(--color-accent)]"
      >
        {{ article.title }}
      </h2>

      <!-- 摘要 -->
      <p class="mt-2.5 text-[14px] leading-[1.72] text-[var(--color-text-secondary)] line-clamp-2">
        {{ article.summary }}
      </p>

      <!-- 底部：标签 + 统计 -->
      <div class="mt-5 flex items-end justify-between gap-4">
        <div class="flex flex-wrap gap-1.5 min-w-0">
          <RouterLink
            v-for="tag in article.tags.slice(0, 3)"
            :key="tag.id"
            :to="`/tag/${tag.id}`"
            class="chip !text-[11px]"
            @click.stop
          >
            {{ tag.name }}
          </RouterLink>
        </div>

        <div class="flex items-center gap-3.5 shrink-0 text-[12px] text-[var(--color-text-tertiary)]">
          <span class="flex items-center gap-1" :title="`${article.viewCount} 次浏览`">
            <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path
                d="M1.5 8S3.9 3.5 8 3.5 14.5 8 14.5 8 12.1 12.5 8 12.5 1.5 8 1.5 8z"
                stroke="currentColor"
                stroke-width="1.4"
              />
              <circle cx="8" cy="8" r="1.9" stroke="currentColor" stroke-width="1.4" />
            </svg>
            {{ formatCount(article.viewCount) }}
          </span>
          <span class="flex items-center gap-1" :title="`${article.likeCount} 次点赞`">
            <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path
                d="M8 13.5S2 9.8 2 6.2A3.2 3.2 0 018 3.7a3.2 3.2 0 016 2.5c0 3.6-6 7.3-6 7.3z"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linejoin="round"
              />
            </svg>
            {{ formatCount(article.likeCount) }}
          </span>
          <span class="hidden sm:flex items-center gap-1">{{ article.readMinutes }} 分钟</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import type { Article } from '@/types'
import { formatCount, formatDate } from '@/utils/format'

const props = defineProps<{
  article: Article
}>()

const router = useRouter()

function goDetail() {
  router.push(`/article/${props.article.id}`)
}

/** 无配图时的杂志色题图：按文章 id 稳定取色，四组暖调渐变轮换 */
const TONES = [
  { gradient: 'linear-gradient(135deg, #d95d18 0%, #e88a4a 55%, #f2b564 100%)' },
  { gradient: 'linear-gradient(135deg, #2f5d50 0%, #4a7f6d 55%, #8ab5a0 100%)' },
  { gradient: 'linear-gradient(135deg, #4a3f63 0%, #75648f 55%, #ab9cbf 100%)' },
  { gradient: 'linear-gradient(135deg, #b98a2e 0%, #d4ab52 55%, #e8cd8e 100%)' }
]

const tone = computed(() => TONES[Math.abs(props.article.id) % TONES.length])

/** 题图主字：标题前两个字（去掉常见标点） */
const headline = computed(() =>
  props.article.title.replace(/[《「『：:，,。（）\s]+/g, '').slice(0, 2) || '简柚'
)
</script>

<style scoped>
.noise-layer {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}

/* 已收藏徽标：封面左上角的小书签，仅已收藏时渲染 */
.collect-badge {
  position: absolute;
  left: 0.875rem;
  top: 0.875rem;
  z-index: 10;
  display: flex;
  width: 1.75rem;
  height: 1.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: color-mix(in srgb, var(--color-accent) 92%, transparent);
  box-shadow: 0 2px 8px rgb(0 0 0 / 0.18);
  backdrop-filter: blur(4px);
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.group:hover .collect-badge {
  transform: scale(1.06);
}
</style>
