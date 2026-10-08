<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-semibold">资源管理</h2>
      <el-button type="primary" :loading="uploading" @click="fileInputRef?.click()">上传文件</el-button>
    </div>

    <!-- 筛选 -->
    <div class="flex flex-wrap gap-3 mb-4">
      <el-input
        v-model="keyword"
        placeholder="按文件名搜索"
        clearable
        class="!w-56"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button @click="load(1)">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="预览" width="150">
        <template #default="{ row }">
          <el-image
            v-if="isImage(row.type)"
            :src="row.url"
            :preview-src-list="[row.url]"
            preview-teleported
            fit="cover"
            class="w-16 h-10 rounded-md border border-[#eee7dc]"
            alt=""
          />
          <audio v-else-if="isAudio(row.type)" :src="row.url" controls preload="none" class="w-[130px] h-8" />
          <a
            v-else
            :href="row.url"
            target="_blank"
            rel="noopener"
            class="text-[12px] text-[#2f5d50] underline underline-offset-2"
          >打开</a>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <span class="text-[12px] text-[#8a8378]">{{ typeLabel(row.type) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="90">
        <template #default="{ row }">{{ formatSize(row.size) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="copyUrl(row)">复制链接</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <input
      ref="fileInputRef"
      type="file"
      accept=".jpg,.jpeg,.png,.gif,.webp,.mp3,.flac,image/*,audio/mpeg,audio/flac"
      class="hidden"
      @change="onPicked"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminGetFiles, adminDeleteFile, adminUpload, type AdminFile } from '@/api/admin'

const list = ref<AdminFile[]>([])
const loading = ref(false)
const uploading = ref(false)
const page = ref(1)
const size = 12
const total = ref(0)
const keyword = ref('')

const fileInputRef = ref<HTMLInputElement | null>(null)

function formatSize(bytes: number): string {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

function isImage(type?: string | null): boolean {
  return !!type && type.startsWith('image/')
}

function isAudio(type?: string | null): boolean {
  return !!type && type.startsWith('audio/')
}

/** 类型列展示：图片去 image/ 前缀，音频去 audio/ 前缀，无则 - */
function typeLabel(type?: string | null): string {
  if (!type) return '-'
  if (type.startsWith('image/')) return type.replace('image/', '')
  if (type.startsWith('audio/')) return type.replace('audio/', '')
  return type
}

async function load(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res = await adminGetFiles({ page: p, size, keyword: keyword.value || undefined })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function onPicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    await adminUpload(file)
    ElMessage.success('上传成功')
    load(1)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function copyUrl(row: AdminFile) {
  try {
    await navigator.clipboard.writeText(row.url)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制：' + row.url)
  }
}

async function remove(row: AdminFile) {
  try {
    await ElMessageBox.confirm(`确定删除资源「${row.name}」？正在使用它的文章将无法显示该图片。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await adminDeleteFile(row.id)
    ElMessage.success('已删除')
    load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(() => load())
</script>
