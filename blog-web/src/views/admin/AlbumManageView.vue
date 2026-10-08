<template>
  <div>
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-[18px] font-semibold">相册管理</h2>
      <el-button type="primary" @click="openCreate">新增照片</el-button>
    </div>

    <div v-if="error" class="bg-white rounded-xl border border-[#eee7dc] p-8 text-center">
      <p class="text-[14px] text-[#c6492f] mb-3">{{ error }}</p>
      <el-button type="primary" @click="load">重试</el-button>
    </div>

    <div v-else-if="loading" class="text-[13px] text-[#a39a8d] py-10 text-center">加载中…</div>

    <!-- 照片网格 -->
    <div v-else-if="photos.length" class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="p in photos"
        :key="p.id"
        class="bg-white rounded-xl border border-[#eee7dc] overflow-hidden group"
      >
        <div class="relative" :style="{ aspectRatio: p.ratio || '1/1' }">
          <img
            v-if="p.url"
            :src="p.url"
            :alt="p.title"
            class="absolute inset-0 h-full w-full object-cover"
          />
          <template v-else>
            <div class="absolute inset-0" :style="{ background: p.tone ?? 'linear-gradient(160deg,#cbc4b8,#8a8574)' }" />
            <p class="absolute inset-0 flex items-center justify-center text-[40px]">{{ p.emoji ?? '🖼️' }}</p>
          </template>
          <el-tag
            v-if="p.status === 2"
            size="small"
            type="info"
            effect="dark"
            class="absolute top-2 left-2"
          >隐藏</el-tag>
        </div>
        <div class="p-3">
          <p class="text-[13px] font-medium text-[#2b2622] truncate">{{ p.title }}</p>
          <p class="text-[11px] text-[#a39a8d] mt-0.5 truncate">
            {{ p.takenDate ?? '未填日期' }} · {{ p.location ?? '未填地点' }}
          </p>
          <div class="flex items-center gap-2 mt-2.5">
            <el-button size="small" text type="primary" @click="openEdit(p)">编辑</el-button>
            <el-button size="small" text @click="toggleStatus(p)">
              {{ p.status === 2 ? '显示' : '隐藏' }}
            </el-button>
            <el-button size="small" text type="danger" @click="remove(p)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="bg-white rounded-xl border border-[#eee7dc] p-10 text-center text-[13px] text-[#a39a8d]">
      还没有照片，点右上角「新增照片」开始
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑照片' : '新增照片'" width="520px">
      <el-form label-width="80px" label-position="left">
        <el-form-item label="图片" required>
          <div class="w-full">
            <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="hidden" @change="onFileChange" />
            <div class="flex items-center gap-2">
              <el-button :loading="uploading" @click="fileInput?.click()">
                {{ uploading ? '上传中…' : '本地上传' }}
              </el-button>
              <el-button @click="openPicker">从资源库选择</el-button>
            </div>
            <span v-if="uploading" class="text-[12px] text-[#a39a8d] mt-1 block">jpg / png / gif / webp，≤5MB</span>
            <p v-if="form.url" class="text-[12px] text-[#2f5d50] mt-1.5 truncate">已选图片：{{ form.url }}</p>
            <p class="text-[12px] text-[#a39a8d] mt-1">可本地上传，或在「资源管理」上传后从这里选用</p>
          </div>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="60" placeholder="如：岳麓山日出" />
        </el-form-item>
        <el-form-item label="拍摄日期">
          <el-date-picker
            v-model="form.takenDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" maxlength="60" placeholder="如：长沙 · 岳麓山" />
        </el-form-item>
        <el-form-item label="宽高比">
          <div class="flex items-center gap-2 w-full">
            <el-input v-model="form.ratio" placeholder="如 3/4" class="!w-[120px]" />
            <span class="text-[12px] text-[#a39a8d]">上传图片后自动按原图计算</span>
          </div>
        </el-form-item>
        <el-form-item label="占位渐变">
          <el-input v-model="form.tone" placeholder="可选，如 linear-gradient(160deg,#f5a15f,#b8440e)" />
        </el-form-item>
        <el-form-item label="占位 emoji">
          <el-input v-model="form.emoji" placeholder="可选，如 🌄" class="!w-[120px]" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span class="text-[12px] text-[#a39a8d] ml-2">越小越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 资源库选择弹窗（图片） -->
    <el-dialog v-model="pickerVisible" title="从资源库选择图片" width="640px" append-to-body>
      <div class="flex items-center gap-2 mb-3">
        <el-input
          v-model="pickerKeyword"
          placeholder="按文件名搜索"
          clearable
          class="!w-[240px]"
          @keyup.enter="pickerSearch"
          @clear="pickerSearch"
        />
        <el-button @click="pickerSearch">搜索</el-button>
      </div>
      <div v-if="pickerLoading" class="text-[13px] text-[#a39a8d] py-10 text-center">加载中…</div>
      <div v-else-if="pickerFiles.length" class="grid grid-cols-3 gap-3 max-h-[380px] overflow-y-auto">
        <div
          v-for="f in pickerFiles"
          :key="f.id"
          class="cursor-pointer rounded-lg border border-[#eee7dc] overflow-hidden hover:border-[#2f5d50] transition-colors"
          title="点击选用"
          @click="pick(f)"
        >
          <el-image :src="f.url" fit="cover" class="h-[110px] w-full block" lazy>
            <template #error>
              <div class="h-[110px] w-full flex items-center justify-center text-[12px] text-[#a39a8d] bg-[#f5f2ec]">预览失败</div>
            </template>
          </el-image>
          <p class="text-[11px] text-[#2b2622] truncate px-2 py-1.5" :title="f.name">{{ f.name }}</p>
        </div>
      </div>
      <div v-else class="text-[13px] text-[#a39a8d] py-10 text-center">资源库里还没有图片，先到「资源管理」上传</div>
      <div v-if="pickerTotal > pickerSize" class="flex justify-end mt-3">
        <el-pagination
          v-model:current-page="pickerPage"
          :page-size="pickerSize"
          :total="pickerTotal"
          layout="prev, pager, next"
          @current-change="loadPicker"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminGetPhotos, adminSavePhoto, adminUpdatePhoto,
  adminSetPhotoStatus, adminDeletePhoto, adminUpload,
  adminGetFiles
} from '@/api/admin'
import type { AdminFile } from '@/api/admin'
import type { Photo } from '@/types'

const photos = ref<Photo[]>([])
const loading = ref(false)
const error = ref('')

const dialog = ref(false)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

const form = reactive({
  id: 0,
  title: '',
  url: '' as string | null,
  location: '' as string | null,
  takenDate: '' as string | null,
  ratio: '1/1',
  tone: '' as string | null,
  emoji: '' as string | null,
  sortOrder: 99
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    photos.value = await adminGetPhotos()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    id: 0, title: '', url: '', location: '', takenDate: '',
    ratio: '1/1', tone: '', emoji: '', sortOrder: 99
  })
  dialog.value = true
}

function openEdit(p: Photo) {
  Object.assign(form, {
    id: p.id,
    title: p.title,
    url: p.url ?? '',
    location: p.location ?? '',
    takenDate: p.takenDate ?? '',
    ratio: p.ratio || '1/1',
    tone: p.tone ?? '',
    emoji: p.emoji ?? '',
    sortOrder: p.sortOrder ?? 99
  })
  dialog.value = true
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploading.value = true
  try {
    const { url } = await adminUpload(file)
    form.url = url
    // 按原图宽高比自动约分，如 3024x4032 -> 3/4
    form.ratio = await probeRatioFile(file)
    ElMessage.success('图片已上传')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

/* ---------- 从资源库选择图片 ---------- */

const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerKeyword = ref('')
const pickerPage = ref(1)
const pickerSize = 12
const pickerTotal = ref(0)
const pickerFiles = ref<AdminFile[]>([])

function openPicker() {
  pickerKeyword.value = ''
  pickerPage.value = 1
  pickerVisible.value = true
  loadPicker()
}

function pickerSearch() {
  pickerPage.value = 1
  loadPicker()
}

async function loadPicker() {
  pickerLoading.value = true
  try {
    const res = await adminGetFiles({
      page: pickerPage.value,
      size: pickerSize,
      keyword: pickerKeyword.value || undefined,
      type: 'image'
    })
    pickerFiles.value = res.records
    pickerTotal.value = Number(res.total)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '资源库加载失败')
  } finally {
    pickerLoading.value = false
  }
}

async function pick(f: AdminFile) {
  form.url = f.url
  pickerVisible.value = false
  // 资源库图片按 URL 重新探测宽高比
  form.ratio = await probeRatioUrl(f.url)
  ElMessage.success('已选用资源库图片')
}

function probeRatioFile(file: File): Promise<string> {
  return probeRatioUrl(URL.createObjectURL(file))
}

function probeRatioUrl(src: string): Promise<string> {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve(toRatio(img.naturalWidth, img.naturalHeight))
    img.onerror = () => resolve('1/1')
    img.src = src
  })
}

function toRatio(w: number, h: number): string {
  const gcd = (a: number, b: number): number => (b === 0 ? a : gcd(b, a % b))
  const d = gcd(w, h) || 1
  return `${Math.round(w / d)}/${Math.round(h / d)}`
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.url && !form.tone) {
    ElMessage.warning('未上传图片时请至少填一个占位渐变色')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      url: form.url || null,
      location: form.location || null,
      takenDate: form.takenDate || null,
      ratio: form.ratio || '1/1',
      tone: form.tone || null,
      emoji: form.emoji || null,
      sortOrder: form.sortOrder
    }
    if (form.id) {
      await adminUpdatePhoto(form.id, payload)
    } else {
      await adminSavePhoto(payload)
    }
    ElMessage.success('已保存')
    dialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(p: Photo) {
  try {
    await adminSetPhotoStatus(p.id, p.status === 2 ? 1 : 2)
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  }
}

async function remove(p: Photo) {
  try {
    await ElMessageBox.confirm(`确定删除「${p.title}」？`, '删除照片', { type: 'warning' })
  } catch {
    return
  }
  try {
    await adminDeletePhoto(p.id)
    ElMessage.success('已删除')
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(load)
</script>
