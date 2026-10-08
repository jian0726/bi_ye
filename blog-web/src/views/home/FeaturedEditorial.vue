<template>
  <section class="pb-[var(--section-gap)]">
    <div class="shell">
      <!-- 区块标题：中性的记录式措辞 -->
      <div class="reveal flex items-end justify-between mb-14 flex-wrap gap-4">
        <div>
          <p class="kicker mb-4">Recent Entries</p>
          <h2 class="display-serif text-[clamp(1.8rem,3.4vw,2.6rem)] text-[var(--color-text-primary)]">
            最近写的
          </h2>
        </div>
        <RouterLink
          to="/archive"
          class="inline-flex items-center gap-1.5 text-[13px] text-[var(--color-text-tertiary)] hover:text-[var(--color-accent)] transition-colors"
        >
          全部记录
          <svg width="12" height="12" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </RouterLink>
      </div>

      <!-- 目录式三栏 -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-x-10 gap-y-12">
        <article v-for="(item, i) in articles" :key="item.id" class="group">
          <RouterLink :to="`/article/${item.id}`" class="block">
            <div class="reveal flex items-baseline gap-4 pb-5 border-b border-[var(--color-divider)]">
              <span class="editorial-no">0{{ i + 1 }}</span>
              <span class="text-[12px] tracking-[0.1em] text-[var(--color-text-tertiary)]">
                {{ item.category?.name ?? '未分类' }}
              </span>
            </div>

            <h3
              class="reveal reveal-delay-1 display-serif mt-6 text-[20px] leading-[1.45] text-[var(--color-text-primary)] transition-colors duration-300 group-hover:text-[var(--color-accent)] line-clamp-2"
            >
              {{ item.title }}
            </h3>

            <p class="reveal reveal-delay-2 mt-4 text-[13.5px] leading-[1.8] text-[var(--color-text-secondary)] line-clamp-3">
              {{ item.summary }}
            </p>

            <p class="reveal reveal-delay-3 mt-5 text-[12px] text-[var(--color-text-quaternary)]">
              {{ formatDate(item.publishTime) }} · {{ item.readMinutes }} 分钟
            </p>
          </RouterLink>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { Article } from '@/types'
import { formatDate } from '@/utils/format'

defineProps<{ articles: Article[] }>()
</script>
