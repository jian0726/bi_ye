<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-semibold">标签管理</h2>
      <el-button type="primary" @click="openForm()">新建标签</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column label="标签" min-width="140">
        <template #default="{ row }">
          <span
            class="inline-block px-2 py-0.5 rounded-full text-[12px]"
            :style="{ backgroundColor: `${row.color ?? '#e8f3ff'}22`, color: row.color ?? '#165dff', border: `1px solid ${row.color ?? '#165dff'}55` }"
          >
            {{ row.name }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="slug" label="别名" min-width="100" />
      <el-table-column prop="color" label="颜色" width="120" />
      <el-table-column prop="articleCount" label="关联文章" width="100" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑标签' : '新建标签'" width="440px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标签名" required>
          <el-input v-model="form.name" maxlength="20" placeholder="如：大学生活" />
        </el-form-item>
        <el-form-item label="别名">
          <el-input v-model="form.slug" placeholder="URL 别名，可留空" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
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
  adminGetTags, adminSaveTag, adminUpdateTag, adminDeleteTag
} from '@/api/admin'
import type { Tag } from '@/types'

const list = ref<Tag[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)

const form = reactive({
  id: 0,
  name: '',
  slug: '',
  color: ''
})

async function load() {
  loading.value = true
  try {
    list.value = await adminGetTags()
  } finally {
    loading.value = false
  }
}

function openForm(row?: Tag) {
  form.id = row?.id ?? 0
  form.name = row?.name ?? ''
  form.slug = row?.slug ?? ''
  form.color = row?.color ?? ''
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入标签名')
    return
  }
  saving.value = true
  try {
    const payload = { name: form.name.trim(), slug: form.slug || undefined, color: form.color || undefined }
    if (form.id) {
      await adminUpdateTag(form.id, payload)
    } else {
      await adminSaveTag(payload)
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

async function remove(row: Tag) {
  try {
    await ElMessageBox.confirm(`删除标签「${row.name}」将同时解除与文章的关联，确定？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await adminDeleteTag(row.id)
    ElMessage.success('已删除')
    load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(load)
</script>
