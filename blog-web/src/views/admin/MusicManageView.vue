<template>
  <div>
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-[18px] font-semibold">音乐管理</h2>
      <el-button type="primary" @click="openCreate">新增曲目</el-button>
    </div>

    <div v-if="error" class="bg-white rounded-xl border border-[#eee7dc] p-8 text-center">
      <p class="text-[14px] text-[#c6492f] mb-3">{{ error }}</p>
      <el-button type="primary" @click="load">重试</el-button>
    </div>

    <div v-else class="bg-white rounded-xl border border-[#eee7dc] p-2">
      <el-table :data="musics" v-loading="loading" empty-text="还没有曲目，点右上角「新增曲目」开始">
        <el-table-column label="曲名" min-width="160">
          <template #default="{ row }">
            <span class="text-[13px] font-medium text-[#2b2622]">{{ row.title }}</span>
            <el-tag v-if="row.status === 2" size="small" type="info" effect="light" class="ml-2">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="artist" label="作者 / 来源" min-width="160" show-overflow-tooltip />
        <el-table-column prop="url" label="音频地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" text @click="toggleStatus(row)">
              {{ row.status === 2 ? '启用' : '停用' }}
            </el-button>
            <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="text-[12px] text-[#a39a8d] px-4 py-3">
        歌单实时作用于全站背景音乐播放器；「停用」的曲目不会出现在前台歌单里。
      </p>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑曲目' : '新增曲目'" width="520px">
      <el-form label-width="90px" label-position="left">
        <el-form-item label="音频" required>
          <div class="w-full">
            <input
              ref="fileInput"
              type="file"
              accept=".mp3,.flac,audio/mpeg,audio/mp3,audio/flac,audio/x-flac"
              class="hidden"
              @change="onFileChange"
            />
            <div class="flex items-center gap-2">
              <el-button :loading="uploading" @click="fileInput?.click()">
                {{ uploading ? '上传中…' : '本地上传' }}
              </el-button>
              <el-button @click="openPicker">从资源库选择</el-button>
            </div>
            <span v-if="uploading" class="text-[12px] text-[#a39a8d] mt-1 block">mp3 / flac，≤1028MB</span>
            <p v-else class="text-[12px] text-[#a39a8d] mt-1">可本地上传，或在「资源管理」上传后从这里选用</p>
          </div>
        </el-form-item>
        <el-form-item label="音频地址">
          <el-input v-model="form.url" placeholder="上传后自动填入，也可直接粘贴在线 mp3 / flac 直链" />
        </el-form-item>
        <el-form-item label="曲名" required>
          <el-input v-model="form.title" maxlength="60" placeholder="如：Outfoxing the Fox" />
        </el-form-item>
        <el-form-item label="作者 / 来源">
          <el-input v-model="form.artist" maxlength="60" placeholder="如：SoundHelix · 免费示例曲" />
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

    <!-- 资源库选择弹窗（音频） -->
    <el-dialog v-model="pickerVisible" title="从资源库选择音频" width="560px" append-to-body>
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
      <template v-else-if="pickerFiles.length">
        <div class="max-h-[360px] overflow-y-auto divide-y divide-[#f0eae0]">
          <div v-for="f in pickerFiles" :key="f.id" class="py-2.5 flex items-center gap-3">
            <div class="flex-1 min-w-0">
              <p class="text-[13px] text-[#2b2622] truncate" :title="f.name">{{ f.name }}</p>
              <audio :src="f.url" controls preload="none" class="h-8 w-full mt-1" />
            </div>
            <el-button size="small" type="primary" @click="pick(f)">选择</el-button>
          </div>
        </div>
        <div v-if="pickerTotal > pickerSize" class="flex justify-end mt-3">
          <el-pagination
            v-model:current-page="pickerPage"
            :page-size="pickerSize"
            :total="pickerTotal"
            layout="prev, pager, next"
            @current-change="loadPicker"
          />
        </div>
      </template>
      <div v-else class="text-[13px] text-[#a39a8d] py-10 text-center">资源库里还没有音频，先到「资源管理」上传</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminGetMusics, adminSaveMusic, adminUpdateMusic,
  adminSetMusicStatus, adminDeleteMusic, adminUpload,
  adminGetFiles
} from '@/api/admin'
import type { AdminFile } from '@/api/admin'
import type { Music } from '@/types'

const musics = ref<Music[]>([])
const loading = ref(false)
const error = ref('')

const dialog = ref(false)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

const form = reactive({
  id: 0,
  title: '',
  artist: '' as string | null,
  url: '',
  sortOrder: 99
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    musics.value = await adminGetMusics()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: 0, title: '', artist: '', url: '', sortOrder: 99 })
  dialog.value = true
}

function openEdit(m: Music) {
  Object.assign(form, {
    id: m.id,
    title: m.title,
    artist: m.artist ?? '',
    url: m.url,
    sortOrder: m.sortOrder ?? 99
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
    if (!form.title) {
      form.title = file.name.replace(/\.(mp3|flac)$/i, '')
    }
    ElMessage.success('音频已上传')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

/* ---------- 从资源库选择音频 ---------- */

const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerKeyword = ref('')
const pickerPage = ref(1)
const pickerSize = 8
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
      type: 'audio'
    })
    pickerFiles.value = res.records
    pickerTotal.value = Number(res.total)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '资源库加载失败')
  } finally {
    pickerLoading.value = false
  }
}

function pick(f: AdminFile) {
  form.url = f.url
  // 曲名为空时用资源库文件名（去扩展名）补上，与上传行为一致
  if (!form.title) {
    form.title = f.name.replace(/\.(mp3|flac|wav|ogg|m4a|aac)$/i, '')
  }
  pickerVisible.value = false
  ElMessage.success('已选用资源库音频')
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写曲名')
    return
  }
  if (!form.url.trim()) {
    ElMessage.warning('请上传音频或填写音频地址')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      artist: form.artist || null,
      url: form.url.trim(),
      sortOrder: form.sortOrder
    }
    if (form.id) {
      await adminUpdateMusic(form.id, payload)
    } else {
      await adminSaveMusic(payload)
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

async function toggleStatus(m: Music) {
  try {
    await adminSetMusicStatus(m.id, m.status === 2 ? 1 : 2)
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  }
}

async function remove(m: Music) {
  try {
    await ElMessageBox.confirm(`确定删除「${m.title}」？`, '删除曲目', { type: 'warning' })
  } catch {
    return
  }
  try {
    await adminDeleteMusic(m.id)
    ElMessage.success('已删除')
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(load)
</script>
