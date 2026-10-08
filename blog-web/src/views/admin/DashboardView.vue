<template>
  <div>
    <h2 class="text-[18px] font-semibold mb-5">仪表盘</h2>

    <!-- 统计卡 -->
    <div v-if="error" class="bg-white rounded-xl border border-[#eee7dc] p-8 text-center">
      <p class="text-[14px] text-[#c6492f] mb-3">{{ error }}</p>
      <el-button type="primary" @click="load">重试</el-button>
    </div>
    <div v-else class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div
        v-for="card in cards"
        :key="card.label"
        class="bg-white rounded-xl p-5 border border-[#eee7dc] cursor-pointer transition-shadow hover:shadow-md"
        @click="card.to && router.push(card.to)"
      >
        <p class="text-[13px] text-[#8a8378] mb-2">{{ card.label }}</p>
        <p class="text-[26px] font-semibold tabular-nums leading-none" :class="card.accent">
          <span v-if="loading">--</span>
          <span v-else>{{ card.value }}</span>
        </p>
      </div>
    </div>

    <!-- 今日访问省份统计 -->
    <div class="bg-white rounded-xl border border-[#eee7dc] p-5 mt-5">
      <div class="flex items-center justify-between mb-3">
        <p class="font-medium text-[14px]">今日访问省份统计</p>
        <span class="text-[12px] text-[#a39a8d]">基于访问日志埋点</span>
      </div>
      <div
        v-if="!loading && provinceStats.length === 0"
        class="text-[13px] text-[#a39a8d] py-6 text-center"
      >
        今日还没有访问记录
      </div>
      <ul v-else class="space-y-2.5">
        <li v-for="p in provinceStats" :key="p.province" class="flex items-center gap-3">
          <span class="w-16 shrink-0 text-[13px] text-[#5c564e] text-right">{{ p.province }}</span>
          <div class="flex-1 h-[8px] rounded-full bg-[#f2ece2] overflow-hidden">
            <div
              class="h-full rounded-full bg-[#2f5d50] transition-all"
              :style="{ width: provinceWidth(p.count) }"
            />
          </div>
          <span class="w-12 shrink-0 text-[13px] tabular-nums text-[#2b2622]">{{ p.count }}</span>
        </li>
      </ul>
    </div>

    <!-- 最近动态 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-5">
      <!-- 最近留言 -->
      <div class="bg-white rounded-xl border border-[#eee7dc] p-5">
        <div class="flex items-center justify-between mb-3">
          <p class="font-medium text-[14px]">最近留言</p>
          <RouterLink to="/admin/messages" class="text-[12px] text-[#d95d18]">全部留言 →</RouterLink>
        </div>
        <div v-if="recentMessages.length === 0" class="text-[13px] text-[#a39a8d] py-6 text-center">
          还没有留言
        </div>
        <ul v-else class="divide-y divide-[#f2ece2]">
          <li v-for="m in recentMessages" :key="m.id" class="py-2.5">
            <p class="text-[13px] text-[#2b2622] truncate">{{ m.content }}</p>
            <p class="text-[12px] text-[#a39a8d] mt-0.5 truncate">
              {{ m.user?.nickname ?? m.nickname ?? '访客' }} · {{ m.createTime }}
            </p>
          </li>
        </ul>
      </div>
    </div>

    <!-- 提示 -->
    <div class="mt-5 bg-white rounded-xl border border-[#eee7dc] p-5 text-[13px] leading-7 text-[#5c564e]">
      <p class="font-medium text-[#2b2622] mb-1">快捷说明</p>
      <p>· 待审友链请进入「友链管理」处理；</p>
      <p>· 文章支持草稿保存，发布后前台即刻可见；</p>
      <p>· 上传的图片集中在「资源管理」，可复制链接在正文中引用。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import {
  getDashboard, adminGetMessages,
  type DashboardStats
} from '@/api/admin'
import type { Message } from '@/types'

const router = useRouter()

const stats = ref<DashboardStats | null>(null)
const loading = ref(false)
const error = ref('')
const recentMessages = ref<Message[]>([])

const cards = computed(() => [
  { label: '文章总数', value: stats.value?.articleTotal ?? 0, accent: '', to: '/admin/articles' },
  { label: '已发布', value: stats.value?.publishedTotal ?? 0, accent: 'text-[#2f5d50]', to: '/admin/articles' },
  { label: '草稿', value: stats.value?.draftTotal ?? 0, accent: 'text-[#d95d18]', to: '/admin/articles' },
  { label: '全站浏览量', value: stats.value?.viewTotal ?? 0, accent: '', to: undefined },
  { label: '留言总数', value: stats.value?.messageTotal ?? 0, accent: '', to: '/admin/messages' },
  { label: '待审友链', value: stats.value?.linkPendingTotal ?? 0, accent: 'text-[#c6492f]', to: '/admin/links' },
  { label: '今日访问', value: stats.value?.todayVisitTotal ?? 0, accent: 'text-[#2f5d50]', to: undefined }
])

const provinceStats = computed(() => stats.value?.provinceStats ?? [])

/** 省份条形图宽度（相对第一名） */
function provinceWidth(count: number): string {
  const max = provinceStats.value[0]?.count ?? 0
  return max > 0 ? `${Math.max((count / max) * 100, 8)}%` : '0%'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [s, messages] = await Promise.all([
      getDashboard(),
      adminGetMessages({ page: 1, size: 5 })
    ])
    stats.value = s
    recentMessages.value = messages.records
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
