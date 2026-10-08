<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-semibold">友链管理</h2>
      <el-button type="primary" @click="openForm()">新增友链</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="站点名" min-width="120" />
      <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'info'" size="small" effect="light">
            {{ row.status === 1 ? '已上架' : row.status === 0 ? '待审核' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="76" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 1" link type="success" size="small" @click="setStatus(row, 1)">上架</el-button>
          <el-button v-if="row.status === 1" link type="warning" size="small" @click="setStatus(row, 2)">下架</el-button>
          <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑友链' : '新增友链'" width="500px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="站点名" required>
          <el-input v-model="form.name" maxlength="30" placeholder="站点名称" />
        </el-form-item>
        <el-form-item label="URL" required>
          <el-input v-model="form.url" placeholder="https://" />
        </el-form-item>
        <el-form-item label="Logo">
          <el-input v-model="form.logo" placeholder="Logo 图片地址，可留空" />
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
  adminGetLinks, adminSaveLink, adminUpdateLink, adminSetLinkStatus, adminDeleteLink,
  type AdminFriendLink
} from '@/api/admin'

const list = ref<AdminFriendLink[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)

const form = reactive({
  id: 0,
  name: '',
  url: '',
  logo: '',
  description: '',
  sortOrder: 99
})

async function load() {
  loading.value = true
  try {
    list.value = await adminGetLinks()
  } finally {
    loading.value = false
  }
}

function openForm(row?: AdminFriendLink) {
  form.id = row?.id ?? 0
  form.name = row?.name ?? ''
  form.url = row?.url ?? ''
  form.logo = row?.logo ?? ''
  form.description = row?.description ?? ''
  form.sortOrder = row?.sortOrder ?? 99
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim() || !form.url.trim()) {
    ElMessage.warning('站点名与 URL 必填')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      url: form.url.trim(),
      logo: form.logo || undefined,
      description: form.description || undefined,
      sortOrder: form.sortOrder
    }
    if (form.id) {
      await adminUpdateLink(form.id, payload)
    } else {
      await adminSaveLink(payload)
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

async function setStatus(row: AdminFriendLink, s: number) {
  await adminSetLinkStatus(row.id!, s)
  ElMessage.success(s === 1 ? '已上架' : '已下架')
  load()
}

async function remove(row: AdminFriendLink) {
  try {
    await ElMessageBox.confirm(`确定删除友链「${row.name}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await adminDeleteLink(row.id!)
    ElMessage.success('已删除')
    load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(load)
</script>
