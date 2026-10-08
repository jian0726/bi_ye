<template>
  <nav v-if="totalPages > 1" class="flex items-center justify-center gap-1.5" aria-label="分页导航">
    <!-- 上一页 -->
    <button
      class="h-9 px-3 rounded-[10px] text-[13px] font-medium transition-all duration-200 disabled:opacity-35 disabled:cursor-not-allowed"
      :class="
        page > 1
          ? 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-sunken)] hover:text-[var(--color-text-primary)]'
          : 'text-[var(--color-text-tertiary)]'
      "
      :disabled="page <= 1"
      @click="go(page - 1)"
    >
      上一页
    </button>

    <!-- 页码 -->
    <template v-for="(item, index) in pageItems" :key="`${item}-${index}`">
      <span
        v-if="item === '...'"
        class="w-9 h-9 flex items-center justify-center text-[13px] text-[var(--color-text-quaternary)]"
      >
        ·
        <span class="sr-only">省略</span>
      </span>
      <button
        v-else
        class="min-w-9 h-9 px-2.5 rounded-[10px] text-[13px] font-medium transition-all duration-200"
        :class="
          item === page
            ? 'bg-[var(--color-text-primary)] text-[var(--color-surface)]'
            : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-sunken)] hover:text-[var(--color-text-primary)]'
        "
        :aria-current="item === page ? 'page' : undefined"
        @click="go(item as number)"
      >
        {{ item }}
      </button>
    </template>

    <!-- 下一页 -->
    <button
      class="h-9 px-3 rounded-[10px] text-[13px] font-medium transition-all duration-200 disabled:opacity-35 disabled:cursor-not-allowed"
      :class="
        page < totalPages
          ? 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-sunken)] hover:text-[var(--color-text-primary)]'
          : 'text-[var(--color-text-tertiary)]'
      "
      :disabled="page >= totalPages"
      @click="go(page + 1)"
    >
      下一页
    </button>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  totalPages: number
}>()

const emit = defineEmits<{
  (e: 'update:page', value: number): void
}>()

/** 生成页码序列，超过 7 页时省略中间部分 */
const pageItems = computed<(number | '...')[]>(() => {
  const total = props.totalPages
  const current = props.page

  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const items: (number | '...')[] = [1]

  if (current > 3) items.push('...')

  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  for (let i = start; i <= end; i++) items.push(i)

  if (current < total - 2) items.push('...')

  items.push(total)
  return items
})

function go(target: number) {
  if (target < 1 || target > props.totalPages || target === props.page) return
  emit('update:page', target)
}
</script>
