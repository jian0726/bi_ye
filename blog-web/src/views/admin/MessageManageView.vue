<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-semibold">留言管理</h2>
      <div class="flex items-center gap-2">
        <el-input
          v-model="ipLocation"
          placeholder="按 IP 属地筛选，如 湖南"
          clearable
          class="!w-52"
          @keyup.enter="load(1)"
          @clear="load(1)"
        />
        <el-button @click="load(1)">查询</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="留言人" width="120">
        <template #default="{ row }">{{ row.user?.nickname ?? row.nickname ?? '访客' }}</template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
      <el-table-column label="IP 属地" width="120">
        <template #default="{ row }">
          <span v-if="row.ipLocation">{{ row.ipLocation }}</span>
          <span v-else class="text-[#86909c]">未知</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="mt-4 flex justify-end">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="(p: number) => load(p)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminGetMessages, adminDeleteMessage } from '@/api/admin'
import type { Message } from '@/types'

const list = ref<Message[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const ipLocation = ref('')

async function load(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res = await adminGetMessages({
      page: p,
      size,
      ipLocation: ipLocation.value.trim() || undefined,
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function remove(row: Message) {
  try {
    await ElMessageBox.confirm('确定删除该留言？', '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await adminDeleteMessage(row.id)
    ElMessage.success('已删除')
    load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(() => load())
</script>
