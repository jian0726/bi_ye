<template>
  <div>
    <h2 class="text-[18px] font-semibold mb-4">用户管理</h2>

    <!-- 筛选 -->
    <div class="flex flex-wrap gap-3 mb-4">
      <el-input
        v-model="keyword"
        placeholder="按账号或昵称搜索"
        clearable
        class="!w-56"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button @click="load(1)">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="用户" min-width="180">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-2">
            <span
              class="w-7 h-7 rounded-full text-white text-[12px] flex items-center justify-center shrink-0"
              style="background: #2f5d50"
            >
              {{ (row.nickname ?? '?').slice(0, 1) }}
            </span>
            <span class="truncate">{{ row.nickname }}</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="联系方式" min-width="180">
        <template #default="{ row }">
          <span class="tabular-nums">{{ row.phone ?? row.email ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small" effect="light">
            {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light">
            {{ row.status === 1 ? '正常' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="160" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.role !== 'ADMIN'"
            link
            :type="row.status === 1 ? 'danger' : 'success'"
            size="small"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '恢复' }}
          </el-button>
          <span v-else class="text-[12px] text-[#a39a8d]">—</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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
import { adminGetUsers, adminSetUserStatus, type AdminUserVO } from '@/api/admin'

const list = ref<AdminUserVO[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const keyword = ref('')

async function load(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res = await adminGetUsers({ page: p, size, keyword: keyword.value || undefined })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row: AdminUserVO) {
  const target = row.status === 1 ? 0 : 1
  if (target === 0) {
    try {
      await ElMessageBox.confirm(`确定禁用用户「${row.nickname}」？禁用后其将无法登录。`, '禁用确认', { type: 'warning' })
    } catch {
      return
    }
  }
  try {
    await adminSetUserStatus(row.id, target)
    ElMessage.success(target === 1 ? '已恢复' : '已禁用')
    load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  }
}

onMounted(() => load())
</script>
