<template>
  <div class="max-w-[720px]">
    <h2 class="display-serif text-[22px] tracking-[0.02em] text-[var(--color-text-primary)] mb-6">我的收藏</h2>

    <div v-if="loading" class="py-16 text-center text-[13.5px] text-[var(--color-text-tertiary)]">
      正在翻…
    </div>

    <div v-else-if="!articles.length" class="empty-state card">
      <span class="empty-char">藏</span>
      <p class="mt-5 text-[14px] text-[var(--color-text-tertiary)]">还没有收藏任何文章</p>
      <RouterLink to="/archive" class="btn btn-secondary mt-6 !py-2 !px-5 !text-[13px]">
        去逛逛
      </RouterLink>
    </div>

    <div v-else class="flex flex-col">
      <RouterLink
        v-for="item in articles"
        :key="item.id"
        :to="`/article/${item.id}`"
        class="group hover-rule border-b border-[var(--color-divider)] py-5"
      >
        <div class="mb-2 flex items-center gap-2.5 text-[12px] text-[var(--color-text-tertiary)]">
          <span v-if="item.category">{{ item.category.name }}</span>
          <span v-if="item.category" class="text-[var(--color-text-quaternary)]">·</span>
          <time class="tabular-nums">{{ formatDate(item.publishTime) }}</time>
        </div>
        <h3
          class="display-serif text-[17px] leading-[1.5] text-[var(--color-text-primary)] transition-colors duration-200 group-hover:text-[var(--color-accent)]"
        >
          {{ item.title }}
        </h3>
        <p class="mt-1.5 text-[13.5px] leading-[1.7] text-[var(--color-text-secondary)] line-clamp-2">
          {{ item.summary }}
        </p>
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getMyCollections } from '@/api'
import type { Article } from '@/types'
import { formatDate } from '@/utils/format'

const articles = ref<Article[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    // 登录用户维度的真实收藏（后端按 token 中的 userId 过滤）
    const res = await getMyCollections(1, 20)
    articles.value = res.records
  } catch (err) {
    console.error('[my-collections] 加载失败', err)
  } finally {
    loading.value = false
  }
})
</script>
