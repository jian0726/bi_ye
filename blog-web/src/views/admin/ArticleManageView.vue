<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-semibold">文章管理</h2>
      <el-button type="primary" @click="router.push('/admin/articles/edit')">新增文章</el-button>
    </div>

    <!-- 筛选 -->
    <div class="flex flex-wrap gap-3 mb-4">
      <el-input
        v-model="query.keyword"
        placeholder="按标题搜索"
        clearable
        class="!w-56"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable class="!w-32" @change="load(1)">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已隐藏" :value="2" />
      </el-select>
      <el-select v-model="query.categoryId" placeholder="全部分类" clearable class="!w-36" @change="load(1)">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button @click="load(1)">查询</el-button>
      <el-button @click="clearFilter">清除参数</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="封面" width="90">
        <template #default="{ row }">
          <img
            v-if="row.cover"
            :src="row.cover"
            class="w-14 h-9 object-cover rounded-md border border-[#eee7dc]"
            alt=""
          />
          <span
            v-else
            class="inline-block w-14 h-9 rounded-md"
            :style="{ background: cardGradient(row.id) }"
          />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <el-tag v-if="row.isTop" size="small" type="warning" effect="plain">顶</el-tag>
            {{ row.title }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="90">
        <template #default="{ row }">{{ row.category?.name ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="是否可见" width="96">
        <template #default="{ row }">
          <el-tag v-if="row.status === 0" size="small" type="info" effect="light">草稿</el-tag>
          <el-switch
            v-else
            :model-value="row.status === 1"
            @change="(v: string | number | boolean) => switchStatus(row, v as boolean)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="76" />
      <el-table-column prop="likeCount" label="点赞" width="76" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="router.push(`/admin/articles/edit?id=${row.id}`)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="success" size="small" @click="publish(row)">发布</el-button>
          <el-button link size="small" @click="toggleTop(row)">{{ row.isTop ? '取消置顶' : '置顶' }}</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="mt-4 flex justify-end">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="(p: number) => load(p)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminGetArticles, adminDeleteArticle,
  adminSetArticleStatus, adminToggleArticleTop,
  adminGetCategories, adminGetTags
} from '@/api/admin'
import type { Article, Category, Tag } from '@/types'

const router = useRouter()

const list = ref<Article[]>([])
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const loading = ref(false)
const total = ref(0)

const query = reactive({ page: 1, size: 10, keyword: '', status: undefined as number | undefined, categoryId: undefined as number | undefined })

/** 无封面时按文章 id 生成杂志渐变题图色 */
function cardGradient(id: number): string {
  const palettes = [
    ['#101714', '#2f5d50'],
    ['#d95d18', '#e8a04c'],
    ['#2f5d50', '#5b8a7a'],
    ['#3a3226', '#d95d18']
  ]
  const [a, b] = palettes[id % palettes.length]
  return `linear-gradient(135deg, ${a}, ${b})`
}

async function load(page = query.page) {
  query.page = page
  loading.value = true
  try {
    const res = await adminGetArticles({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
      categoryId: query.categoryId
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function clearFilter() {
  query.keyword = ''
  query.status = undefined
  query.categoryId = undefined
  load(1)
}

/** 列表内直接切换 可见(1)/隐藏(2) */
async function switchStatus(row: Article, visible: boolean) {
  const target = visible ? 1 : 2
  try {
    await adminSetArticleStatus(row.id, target)
    row.status = target
    ElMessage.success(visible ? '已发布' : '已隐藏')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  }
}

/** 草稿一键发布 */
async function publish(row: Article) {
  await adminSetArticleStatus(row.id, 1)
  ElMessage.success('已发布')
  load()
}

async function toggleTop(row: Article) {
  await adminToggleArticleTop(row.id)
  load()
}

async function remove(row: Article) {
  try {
    await ElMessageBox.confirm(`确定删除文章「${row.title}」？其标签关联将一并解除。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await adminDeleteArticle(row.id)
    ElMessage.success('已删除')
    load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(async () => {
  load()
  categories.value = await adminGetCategories()
  tags.value = await adminGetTags()
})
</script>
