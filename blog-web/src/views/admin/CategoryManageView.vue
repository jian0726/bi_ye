<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-semibold">分类管理</h2>
      <el-button type="primary" @click="openForm()">新建分类</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="name" label="分类名" min-width="120" />
      <el-table-column prop="slug" label="别名" min-width="100" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="articleCount" label="文章数" width="90" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新建分类'" width="480px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="分类名" required>
          <el-input v-model="form.name" maxlength="20" placeholder="如：记录" />
        </el-form-item>
        <el-form-item label="别名">
          <el-input v-model="form.slug" placeholder="URL 别名，可留空" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="100" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="1" :max="99" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminGetCategories, adminSaveCategory, adminUpdateCategory, adminDeleteCategory
} from '@/api/admin'
import type { Category } from '@/types'

const list = ref<Category[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)

const form = reactive({
  id: 0,
  name: '',
  slug: '',
  description: '',
  sortOrder: 99
})

async function load() {
  loading.value = true
  try {
    list.value = await adminGetCategories()
  } finally {
    loading.value = false
  }
}

function openForm(row?: Category) {
  form.id = row?.id ?? 0
  form.name = row?.name ?? ''
  form.slug = row?.slug ?? ''
  form.description = row?.description ?? ''
  form.sortOrder = row?.sortOrder ?? 99
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      slug: form.slug || undefined,
      description: form.description || undefined,
      sortOrder: form.sortOrder
    }
    if (form.id) {
      await adminUpdateCategory(form.id, payload)
    } else {
      await adminSaveCategory(payload)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: Category) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await adminDeleteCategory(row.id)
    ElMessage.success('已删除')
    load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(load)
</script>
