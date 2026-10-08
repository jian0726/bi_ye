<template>
  <div ref="root" class="min-h-[60vh] pb-[var(--section-gap)]">
    <PageMasthead
      title="百宝箱"
      kicker="Toolbox"
      subtitle="平时用得顺手的工具和网站，收在一起，免得每次都重新找。"
      :meta="['NO.07', '常备', '越攒越多']"
      ghost="百宝"
    />

    <div class="shell pt-12">
      <!-- 分组工具卡 -->
      <section v-for="group in groups" :key="group.name" class="mb-14">
        <h2 class="reveal display-serif text-[20px] text-[var(--color-text-primary)] mb-6 flex items-center gap-3">
          {{ group.name }}
          <span class="text-[12px] font-normal text-[var(--color-text-quaternary)]">{{ group.items.length }}</span>
        </h2>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <a
            v-for="(tool, i) in group.items"
            :key="tool.name"
            :href="tool.url"
            target="_blank"
            rel="noopener"
            class="reveal group card card-hover !rounded-[14px] p-5 flex items-start gap-4"
            :class="`reveal-delay-${(i % 6) + 1}`"
          >
            <span
              class="shrink-0 w-10 h-10 rounded-[12px] flex items-center justify-center text-[18px]"
              :style="{ background: tool.tint }"
            >{{ tool.emoji }}</span>
            <span class="min-w-0">
              <span class="block text-[14.5px] font-semibold text-[var(--color-text-primary)] transition-colors duration-200 group-hover:text-[var(--color-accent)]">
                {{ tool.name }}
              </span>
              <span class="mt-1 block text-[12.5px] leading-[1.65] text-[var(--color-text-secondary)] line-clamp-2">
                {{ tool.desc }}
              </span>
            </span>
          </a>
        </div>
      </section>

      <!-- 友链 -->
      <section>
        <h2 class="reveal display-serif text-[20px] text-[var(--color-text-primary)] mb-6 flex items-center gap-3">
          朋友们的站
          <span class="text-[12px] font-normal text-[var(--color-text-quaternary)]">{{ links.length }}</span>
        </h2>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <a
            v-for="(link, i) in links"
            :key="link.id"
            :href="link.url"
            target="_blank"
            rel="noopener"
            class="reveal card card-hover !rounded-[14px] p-5"
            :class="`reveal-delay-${(i % 6) + 1}`"
          >
            <div class="flex items-center gap-3">
              <span
                class="w-9 h-9 rounded-full flex items-center justify-center text-[13px] font-semibold text-white shrink-0"
                :style="{ backgroundColor: stringToColor(link.name) }"
              >{{ nameInitial(link.name) }}</span>
              <span class="text-[14px] font-semibold text-[var(--color-text-primary)] truncate">{{ link.name }}</span>
            </div>
            <p class="mt-3 text-[12.5px] leading-[1.65] text-[var(--color-text-secondary)] line-clamp-2">
              {{ link.description }}
            </p>
          </a>
        </div>

        <div class="reveal mt-8 flex flex-wrap items-center gap-x-2 gap-y-1.5 text-[12.5px] text-[var(--color-text-tertiary)]">
          <span>想交换友链？</span>
          <button
            type="button"
            class="font-medium text-[var(--color-accent)] underline-offset-4 transition-opacity duration-200 hover:underline hover:opacity-80"
            @click="openApply"
          >
            填一份申请
          </button>
          <span class="text-[var(--color-text-quaternary)]">，站长看过就挂上去。</span>
        </div>
      </section>
    </div>

    <!-- 友链申请弹层 -->
    <Teleport to="body">
      <Transition name="apply-fade">
        <div v-if="applyOpen" class="apply-mask" @click.self="closeApply">
          <div class="apply-panel" role="dialog" aria-modal="true" aria-label="申请友链">
            <header class="mb-7">
              <p class="apply-kicker">Friend Link</p>
              <h3 class="display-serif text-[24px] leading-[1.35] text-[var(--color-text-primary)]">
                申请友链
              </h3>
              <p class="mt-2 text-[12.5px] text-[var(--color-text-secondary)]">
                留下你的站点，站长审核通过后会出现在「朋友们的站」里。
              </p>
            </header>

            <form @submit.prevent="submitApply">
              <div class="apply-field">
                <label for="fl-name">站点名称 <span class="req">*</span></label>
                <input
                  id="fl-name"
                  v-model="applyForm.name"
                  type="text"
                  maxlength="30"
                  placeholder="比如：某某的笔记本"
                  class="field-underline"
                />
              </div>

              <div class="apply-field">
                <label for="fl-url">站点地址 <span class="req">*</span></label>
                <input
                  id="fl-url"
                  v-model="applyForm.url"
                  type="text"
                  maxlength="200"
                  placeholder="https://"
                  class="field-underline"
                />
                <p v-if="urlInvalid" class="apply-hint">地址需以 http:// 或 https:// 开头</p>
              </div>

              <div class="apply-field">
                <label for="fl-desc">一句话介绍</label>
                <input
                  id="fl-desc"
                  v-model="applyForm.description"
                  type="text"
                  maxlength="60"
                  placeholder="选填，最多 60 字"
                  class="field-underline"
                />
              </div>

              <div class="apply-field">
                <label for="fl-logo">图标地址</label>
                <input
                  id="fl-logo"
                  v-model="applyForm.logo"
                  type="text"
                  maxlength="300"
                  placeholder="选填，头像或图标链接"
                  class="field-underline"
                />
              </div>

              <div class="apply-field">
                <label for="fl-email">联系邮箱</label>
                <input
                  id="fl-email"
                  v-model="applyForm.email"
                  type="text"
                  maxlength="50"
                  placeholder="选填，方便站长联系你"
                  class="field-underline"
                />
                <p v-if="emailInvalid" class="apply-hint">邮箱格式看起来不太对</p>
              </div>

              <p v-if="applyError" class="apply-msg apply-msg--err">{{ applyError }}</p>
              <p v-if="applyDone" class="apply-msg apply-msg--ok">已提交，等站长看过就会挂上去。</p>

              <div class="mt-8 flex items-center gap-3">
                <button type="button" class="apply-cancel" @click="closeApply">
                  {{ applyDone ? '关闭' : '取消' }}
                </button>
                <button
                  type="submit"
                  class="btn btn-primary flex-1 !py-2.5 disabled:opacity-40 disabled:cursor-not-allowed"
                  :disabled="!canApply || applying"
                >
                  {{ applying ? '提交中…' : '提交申请' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import PageMasthead from '@/components/PageMasthead.vue'
import { applyFriendLink, getFriendLinks } from '@/api'
import type { FriendLink } from '@/types'
import { nameInitial, stringToColor } from '@/utils/format'
import { useScrollReveal } from '@/composables/useScrollReveal'

const root = ref<HTMLElement>()
const { refresh } = useScrollReveal(root)

interface Tool {
  name: string
  desc: string
  url: string
  emoji: string
  tint: string
}

interface ToolGroup {
  name: string
  items: Tool[]
}

const links = ref<FriendLink[]>([])

/* ============================ 友链申请 ============================ */

const applyOpen = ref(false)
const applying = ref(false)
const applyError = ref('')
const applyDone = ref(false)

const applyForm = reactive({
  name: '',
  url: '',
  description: '',
  logo: '',
  email: ''
})

/** 站点地址必须带协议头，否则留库里也点不开 */
const urlInvalid = computed(
  () => applyForm.url.trim().length > 0 && !/^https?:\/\/.+/i.test(applyForm.url.trim())
)

const emailInvalid = computed(() => {
  const e = applyForm.email.trim()
  return e.length > 0 && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e)
})

const canApply = computed(
  () => applyForm.name.trim().length > 0 && applyForm.url.trim().length > 0 && !urlInvalid.value && !emailInvalid.value
)

function openApply() {
  applyError.value = ''
  applyDone.value = false
  applyOpen.value = true
}

function closeApply() {
  applyOpen.value = false
}

async function submitApply() {
  if (!canApply.value || applying.value) return
  applyError.value = ''
  applying.value = true
  try {
    await applyFriendLink({
      name: applyForm.name.trim(),
      url: applyForm.url.trim(),
      description: applyForm.description.trim() || undefined,
      logo: applyForm.logo.trim() || undefined,
      email: applyForm.email.trim() || undefined
    })
    applyDone.value = true
    applyForm.name = ''
    applyForm.url = ''
    applyForm.description = ''
    applyForm.logo = ''
    applyForm.email = ''
    setTimeout(() => {
      applyOpen.value = false
    }, 2600)
  } catch (err) {
    applyError.value = err instanceof Error ? err.message : '提交失败，稍后再试'
  } finally {
    applying.value = false
  }
}

onMounted(async () => {
  links.value = await getFriendLinks()
  // 友链卡片异步渲染，等 DOM 更新后重新观察 .reveal
  await nextTick()
  requestAnimationFrame(refresh)
})

/** 演示数据 */
const groups: ToolGroup[] = [
  {
    name: '写代码',
    items: [
      { name: 'JetBrains 全家桶', desc: '学生认证免费，IDEA + WebStorm 解决一切。', url: 'https://www.jetbrains.com', emoji: '🧠', tint: 'rgba(217,93,24,0.1)' },
      { name: 'VS Code', desc: '轻量编辑器，配插件就是另一把瑞士军刀。', url: 'https://code.visualstudio.com', emoji: '⚡', tint: 'rgba(47,93,80,0.1)' },
      { name: 'GitHub', desc: '代码放这里，毕业设计也在里面。', url: 'https://github.com', emoji: '🐙', tint: 'rgba(58,124,165,0.1)' },
      { name: 'Vite', desc: '秒开的前端构建工具，中文文档友好。', url: 'https://cn.vitejs.dev', emoji: '📦', tint: 'rgba(217,147,10,0.1)' }
    ]
  },
  {
    name: '查资料',
    items: [
      { name: 'MDN Web Docs', desc: '前端问题的最终答案，别再问 CSDN 抄来抄去了。', url: 'https://developer.mozilla.org/zh-CN/', emoji: '📚', tint: 'rgba(47,93,80,0.1)' },
      { name: 'MyBatis-Plus 文档', desc: '条件构造器怎么写，这里说得最清楚。', url: 'https://baomidou.com', emoji: '🍃', tint: 'rgba(61,138,80,0.1)' },
      { name: 'Spring 官方文档', desc: 'Spring Boot 3 的权威参考，英文但值得啃。', url: 'https://spring.io/projects/spring-boot', emoji: '🌱', tint: 'rgba(61,138,80,0.1)' },
      { name: '菜鸟教程', desc: '入门新语言时先过一遍，快。', url: 'https://www.runoob.com', emoji: '🐦', tint: 'rgba(213,68,44,0.1)' }
    ]
  },
  {
    name: '小工具',
    items: [
      { name: 'Excalidraw', desc: '手绘风白板，画流程图和架构草图神器。', url: 'https://excalidraw.com', emoji: '✏️', tint: 'rgba(217,147,10,0.1)' },
      { name: 'TinyPNG', desc: '图片压缩，博客配图先用它过一遍。', url: 'https://tinypng.com', emoji: '🗜️', tint: 'rgba(58,124,165,0.1)' },
      { name: 'Regex101', desc: '正则可视化调试，救我过好几次命。', url: 'https://regex101.com', emoji: '🔤', tint: 'rgba(217,93,24,0.1)' },
      { name: 'Linux 命令查询', desc: '部署服务器时忘了参数就查这里。', url: 'https://wangchujiang.com/linux-command/', emoji: '🐧', tint: 'rgba(47,93,80,0.1)' }
    ]
  }
]
</script>

<style scoped>
/* 友链申请弹层：遮罩用墨色半透明 + 模糊，面板走暖纸白 */
.apply-mask {
  position: fixed;
  inset: 0;
  /* 高于固定导航栏（AppHeader 为 z-200），避免被压在下面 */
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem;
  background: rgba(16, 23, 20, 0.52);
  backdrop-filter: blur(6px);
}

.apply-panel {
  width: 100%;
  max-width: 420px;
  max-height: calc(100dvh - 2.5rem);
  overflow-y: auto;
  padding: 2rem;
  border-radius: 18px;
  background: var(--color-surface-elevated);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.24);
}

.apply-kicker {
  margin-bottom: 0.75rem;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: var(--color-text-quaternary);
}

.apply-field + .apply-field {
  margin-top: 1.25rem;
}

.apply-field label {
  display: block;
  margin-bottom: 0.25rem;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.apply-field .req {
  color: var(--color-accent);
}

.apply-hint {
  margin-top: 0.35rem;
  font-size: 11.5px;
  color: var(--color-danger);
}

.apply-msg {
  margin-top: 1.25rem;
  font-size: 12.5px;
  text-align: center;
}
.apply-msg--err {
  color: var(--color-danger);
}
.apply-msg--ok {
  color: var(--color-success);
}

.apply-cancel {
  flex: none;
  padding: 0 1rem;
  font-size: 13px;
  color: var(--color-text-secondary);
  transition: color 0.2s ease;
}
.apply-cancel:hover {
  color: var(--color-text-primary);
}

.apply-fade-enter-active,
.apply-fade-leave-active {
  transition: opacity 0.22s ease;
}
.apply-fade-enter-from,
.apply-fade-leave-to {
  opacity: 0;
}
</style>
