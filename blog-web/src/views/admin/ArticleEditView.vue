<template>
  <div>
    <!-- 顶部操作条 -->
    <div class="flex items-center justify-between mb-5">
      <div class="flex items-center gap-3">
        <el-button link @click="router.push('/admin/articles')">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <h2 class="text-[18px] font-semibold">{{ form.id ? '编辑文章' : '写文章' }}</h2>
      </div>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </div>

    <!-- 基本信息 -->
    <div class="bg-white rounded-xl border border-[#eee7dc] p-5 mb-5">
      <el-form :model="form" label-width="76px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="100" placeholder="文章标题" size="large" />
        </el-form-item>
        <div class="flex flex-wrap gap-4">
          <el-form-item label="分类" class="flex-1 min-w-[180px]">
            <el-select v-model="form.categoryId" placeholder="选择分类" clearable class="!w-full">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" class="flex-1 min-w-[220px]">
            <el-radio-group v-model="form.status">
              <el-radio-button :value="0">草稿</el-radio-button>
              <el-radio-button :value="1">发布</el-radio-button>
              <el-radio-button :value="2">隐藏</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标签" class="flex-[2] min-w-[240px]">
            <el-select v-model="form.tagIds" multiple placeholder="选择标签" class="!w-full">
              <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="200" placeholder="列表页展示的摘要，可留空" />
        </el-form-item>
        <el-form-item label="封面">
          <div class="w-full flex items-center gap-3">
            <el-input v-model="form.cover" placeholder="封面图片地址，可留空" class="!flex-1" />
            <el-button :loading="coverUploading" @click="pickCover">
              {{ coverUploading ? '上传中…' : '上传封面' }}
            </el-button>
            <img
              v-if="form.cover"
              :src="form.cover"
              class="w-16 h-10 object-cover rounded-md border border-[#eee7dc]"
              alt="封面预览"
            />
            <el-button v-if="form.cover" link type="danger" @click="form.cover = ''">移除</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- Markdown 编辑器 -->
    <div class="bg-white rounded-xl border border-[#eee7dc] overflow-hidden">
      <!-- 工具栏 -->
      <div class="flex items-center flex-wrap gap-1 px-3 py-2 border-b border-[#f2ece2]">
        <button
          v-for="btn in toolbar"
          :key="btn.title"
          type="button"
          class="editor-btn"
          :title="btn.title"
          :disabled="uploading"
          @click="btn.action()"
        >
          <span v-if="btn.text" class="text-[13px] leading-6" :class="btn.bold ? 'font-bold' : ''">{{ btn.text }}</span>
          <el-icon v-else><component :is="btn.icon" /></el-icon>
        </button>
        <span class="flex-1" />
        <span class="text-[12px] text-[#a39a8d]">{{ uploading ? '图片上传中…' : `已输入 ${form.content.length} 字` }}</span>
      </div>

      <!-- 编辑 / 预览 分栏 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 divide-x divide-[#f2ece2]">
        <textarea
          ref="editorRef"
          v-model="form.content"
          class="editor-area"
          placeholder="开始编辑…（支持 Markdown）"
          @drop.prevent="onDrop"
          @dragover.prevent
        />
        <div class="h-[480px] overflow-auto px-5 py-4 bg-[#fdfcf9]">
          <div v-if="form.content" class="markdown-body" v-html="previewHtml" />
          <p v-else class="text-[13px] text-[#a39a8d]">预览区：左侧输入的 Markdown 将实时渲染在这里</p>
        </div>
      </div>
    </div>

    <!-- 选项 -->
    <div class="bg-white rounded-xl border border-[#eee7dc] p-5 mt-5 flex items-center gap-6">
      <el-checkbox v-model="form.isTop">置顶</el-checkbox>
      <el-checkbox v-model="form.isRecommend">推荐</el-checkbox>
      <span class="flex-1" />
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </div>

    <!-- 隐藏的图片选择器（正文插图） -->
    <input ref="imageInputRef" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="hidden" @change="onImagePicked" />
    <!-- 隐藏的图片选择器（封面） -->
    <input ref="coverInputRef" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="hidden" @change="onCoverPicked" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Picture } from '@element-plus/icons-vue'
import {
  adminGetArticle, adminSaveArticle, adminUpdateArticle,
  adminGetCategories, adminGetTags, adminUpload
} from '@/api/admin'
import { renderMarkdown } from '@/utils/markdown'
import type { Category, Tag } from '@/types'

const route = useRoute()
const router = useRouter()

const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const saving = ref(false)
const uploading = ref(false)
const coverUploading = ref(false)

const form = reactive({
  id: 0,
  title: '',
  summary: '',
  content: '',
  cover: '',
  categoryId: undefined as number | undefined,
  tagIds: [] as number[],
  status: 0,
  isTop: false,
  isRecommend: false
})

const editorRef = ref<HTMLTextAreaElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)
const coverInputRef = ref<HTMLInputElement | null>(null)

const previewHtml = computed(() => renderMarkdown(form.content))

/* ---------------- 工具栏 ---------------- */

/** 在光标处包裹选区或插入文本 */
function wrapSelection(prefix: string, suffix = '', placeholder = '') {
  const ta = editorRef.value
  if (!ta) return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const value = form.content
  const selected = value.slice(start, end) || placeholder
  form.content = value.slice(0, start) + prefix + selected + suffix + value.slice(end)
  requestAnimationFrame(() => {
    ta.focus()
    const cursor = start + prefix.length
    ta.setSelectionRange(cursor, cursor + selected.length)
  })
}

function insertBlock(text: string) {
  const ta = editorRef.value
  if (!ta) return
  const start = ta.selectionStart
  const value = form.content
  const lineStart = value.lastIndexOf('\n', Math.max(start - 1, 0)) + 1
  form.content = value.slice(0, lineStart) + text + value.slice(lineStart)
  requestAnimationFrame(() => {
    ta.focus()
    const cursor = lineStart + text.length
    ta.setSelectionRange(cursor, cursor)
  })
}

async function uploadImage(file: File): Promise<string | null> {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return null
  }
  uploading.value = true
  try {
    const res = await adminUpload(file)
    return res.url
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
    return null
  } finally {
    uploading.value = false
  }
}

async function insertImage(file: File) {
  const url = await uploadImage(file)
  if (!url) return
  const name = file.name.replace(/\.[a-zA-Z]+$/, '')
  wrapSelection(`![${name}](${url})`)
}

function onImagePicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) insertImage(file)
  input.value = ''
}

function onCoverPicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    coverUploading.value = true
    uploadImage(file)
      .then((url) => {
        if (url) form.cover = url
      })
      .finally(() => {
        coverUploading.value = false
      })
  }
  input.value = ''
}

function pickCover() {
  coverInputRef.value?.click()
}

/** 拖拽图片到编辑器直接上传插图 */
async function onDrop(e: DragEvent) {
  const file = e.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('image/')) {
    await insertImage(file)
  }
}

interface ToolbarBtn {
  title: string
  text?: string
  bold?: boolean
  icon?: Component
  action: () => void
}

const toolbar: ToolbarBtn[] = [
  { title: '二级标题', text: 'H2', action: () => insertBlock('## 标题\n') },
  { title: '加粗', text: 'B', bold: true, action: () => wrapSelection('**', '**', '加粗文字') },
  { title: '斜体', text: 'I', action: () => wrapSelection('*', '*', '斜体文字') },
  { title: '引用', text: '❝', action: () => insertBlock('> 引用内容\n') },
  { title: '行内代码', text: '‹›', action: () => wrapSelection('`', '`', 'code') },
  { title: '代码块', text: '{ }', action: () => insertBlock('\n```java\n\n```\n') },
  { title: '无序列表', text: '•', action: () => insertBlock('- 列表项\n') },
  { title: '有序列表', text: '1.', action: () => insertBlock('1. 列表项\n') },
  { title: '链接', text: '🔗', action: () => wrapSelection('[', '](https://)', '链接文字') },
  { title: '插入图片', icon: Picture, action: () => imageInputRef.value?.click() },
  { title: '分割线', text: '—', action: () => insertBlock('\n---\n') }
]

/* ---------------- 数据加载与保存 ---------------- */

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      summary: form.summary,
      content: form.content,
      cover: form.cover,
      categoryId: form.categoryId ?? null,
      tagIds: form.tagIds,
      status: form.status,
      isTop: form.isTop,
      isRecommend: form.isRecommend
    }
    if (form.id) {
      await adminUpdateArticle(form.id, payload)
    } else {
      await adminSaveArticle(payload)
    }
    ElMessage.success('已保存')
    router.push('/admin/articles')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  categories.value = await adminGetCategories()
  tags.value = await adminGetTags()

  const id = Number(route.query.id)
  if (id) {
    const detail = await adminGetArticle(id)
    form.id = detail.id
    form.title = detail.title
    form.summary = detail.summary
    form.content = detail.content ?? ''
    form.cover = detail.cover ?? ''
    form.categoryId = detail.categoryId ?? undefined
    form.tagIds = detail.tags?.map((t) => t.id) ?? []
    form.status = detail.status
    form.isTop = detail.isTop
    form.isRecommend = detail.isRecommend
  }
})
</script>

<style scoped>
.editor-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  padding: 0 6px;
  border-radius: 6px;
  color: #5c564e;
  transition: background-color 0.15s ease, color 0.15s ease;
}
.editor-btn:hover {
  background: #f7f3ec;
  color: #d95d18;
}
.editor-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.editor-area {
  height: 480px;
  padding: 16px 20px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13.5px;
  line-height: 1.8;
  color: #2b2622;
  resize: none;
  outline: none;
  background: #fffefe;
}
.editor-area::placeholder {
  color: #b3aa9c;
}
</style>
