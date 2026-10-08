<template>
  <header
    class="fixed top-0 left-0 right-0 z-[200] transition-all duration-300"
    :class="[
      scrolled || !overInk ? 'glass border-b' : 'bg-transparent',
      scrolled || !overInk ? 'border-[var(--color-divider)]' : ''
    ]"
    :data-ink="overInk ? 'true' : 'false'"
    style="height: var(--header-height)"
  >
    <nav class="shell-full h-full flex items-center gap-3">
      <!-- Logo -->
      <RouterLink
        to="/"
        class="flex items-center gap-2 shrink-0 transition-opacity duration-200 hover:opacity-70 header-ink"
      >
        <span class="text-[17px] font-semibold tracking-[-0.02em] header-ink-strong">
          {{ siteName }}
        </span>
      </RouterLink>

      <!-- 占位：导航与操作全部靠右 -->
      <div class="flex-1" aria-hidden="true" />

      <!-- 桌面端导航 -->
      <div class="hidden md:flex items-center gap-1">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="px-3 py-1.5 rounded-[10px] text-[13px] font-medium transition-all duration-200 header-ink"
          :class="
            isActive(item.path)
              ? 'header-ink-strong nav-active'
              : 'header-ink-mid'
          "
        >
          {{ item.label }}
        </RouterLink>
      </div>

      <!-- 右侧操作 -->
      <div class="flex items-center gap-1.5 shrink-0">
        <!-- 背景音乐迷你播放器 -->
        <div class="relative" @click.stop>
          <button
            id="bgm-toggle"
            class="w-8 h-8 flex items-center justify-center rounded-full header-ink-mid nav-hover"
            :aria-label="playing ? '音乐播放中，打开播放面板' : '打开背景音乐面板'"
            :title="current?.title ?? '背景音乐'"
            @click="bgmOpen = !bgmOpen"
          >
            <!-- 黑胶小圆盘：播放中缓慢旋转 -->
            <svg
              width="17"
              height="17"
              viewBox="0 0 16 16"
              fill="none"
              aria-hidden="true"
              :class="pending ? 'bgm-pending' : playing ? 'bgm-spin' : ''"
            >
              <circle cx="8" cy="8" r="6.6" stroke="currentColor" stroke-width="1.4" />
              <circle cx="8" cy="8" r="4.1" stroke="currentColor" stroke-width="0.7" opacity="0.5" />
              <circle cx="8" cy="8" r="2.2" :fill="playing ? 'var(--color-accent)' : 'currentColor'" opacity="0.85" />
              <circle cx="8" cy="8" r="0.6" :fill="playing ? '#fff' : 'var(--color-surface)'" />
            </svg>
            <!-- 播放中的小橙点 -->
            <span
              v-if="playing"
              class="absolute top-1 right-1 w-1.5 h-1.5 rounded-full"
              :style="{ backgroundColor: 'var(--color-accent)' }"
            />
          </button>

          <!-- 播放面板 -->
          <Transition name="bgm-panel">
            <div
              v-if="bgmOpen"
              class="absolute right-0 top-[calc(100%+12px)] w-[252px] rounded-[16px] overflow-hidden bg-[var(--color-surface-elevated)] border border-[var(--color-border)] shadow-[var(--shadow-lift)]"
              role="dialog"
              aria-label="背景音乐播放面板"
            >
              <!-- 曲目信息 -->
              <div class="flex items-center gap-3.5 px-4 pt-3.5 pb-3 border-b border-[var(--color-divider)]">
                <!-- 面板里的黑胶盘 -->
                <span class="relative flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[var(--ink-bg)]">
                  <svg
                    width="30"
                    height="30"
                    viewBox="0 0 16 16"
                    fill="none"
                    aria-hidden="true"
                    :class="pending ? 'bgm-pending' : playing ? 'bgm-spin' : ''"
                  >
                    <circle cx="8" cy="8" r="6.8" stroke="rgba(244,241,232,0.5)" stroke-width="0.8" />
                    <circle cx="8" cy="8" r="4.4" stroke="rgba(244,241,232,0.28)" stroke-width="0.6" />
                    <circle cx="8" cy="8" r="2.1" :fill="playing ? 'var(--color-accent)' : 'rgba(244,241,232,0.4)'" />
                    <circle cx="8" cy="8" r="0.55" fill="var(--ink-bg)" />
                  </svg>
                </span>
                <div class="min-w-0 flex-1">
                  <p class="text-[10.5px] font-semibold tracking-[0.22em] uppercase text-[var(--color-text-tertiary)]">
                    正在播放
                  </p>
                  <p class="mt-1 text-[14px] font-semibold tracking-[-0.01em] text-[var(--color-text-primary)] truncate">
                    {{ current?.title }}
                  </p>
                  <p class="text-[11.5px] text-[var(--color-text-tertiary)] truncate">
                    {{ current?.artist }}
                  </p>
                </div>
              </div>

              <!-- 控制区 -->
              <div class="flex items-center justify-center gap-5 py-3.5">
                <button
                  class="w-8 h-8 flex items-center justify-center rounded-full text-[var(--color-text-secondary)] transition-colors duration-200 hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface-sunken)]"
                  aria-label="上一首"
                  @click="prev"
                >
                  <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M12.5 3.5v9L5.5 8l7-4.5z" fill="currentColor" />
                    <path d="M3.5 3.5v9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  </svg>
                </button>
                <button
                  class="w-10 h-10 flex items-center justify-center rounded-full text-white transition-transform duration-200 hover:scale-105"
                  :style="{ backgroundColor: 'var(--color-accent)' }"
                  :aria-label="playing || pending ? '暂停' : '播放'"
                  @click="toggle"
                >
                  <!-- 加载中：小转圈 -->
                  <svg
                    v-if="pending"
                    width="16"
                    height="16"
                    viewBox="0 0 16 16"
                    fill="none"
                    class="bgm-pending"
                    aria-hidden="true"
                  >
                    <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.8" opacity="0.25" />
                    <path d="M8 2a6 6 0 016 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                  </svg>
                  <!-- 暂停图标 -->
                  <svg v-else-if="playing" width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <rect x="4" y="3" width="3" height="10" rx="1" fill="currentColor" />
                    <rect x="9" y="3" width="3" height="10" rx="1" fill="currentColor" />
                  </svg>
                  <!-- 播放图标 -->
                  <svg v-else width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M5 3.2v9.6a.5.5 0 00.77.42l7.2-4.8a.5.5 0 000-.84l-7.2-4.8A.5.5 0 005 3.2z" fill="currentColor" />
                  </svg>
                </button>
                <button
                  class="w-8 h-8 flex items-center justify-center rounded-full text-[var(--color-text-secondary)] transition-colors duration-200 hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface-sunken)]"
                  aria-label="下一首"
                  @click="next"
                >
                  <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M3.5 3.5v9l7-4.5-7-4.5z" fill="currentColor" />
                    <path d="M12.5 3.5v9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  </svg>
                </button>
              </div>

              <!-- 音量 -->
              <div class="flex items-center gap-2.5 px-4 pb-4">
                <svg width="13" height="13" viewBox="0 0 16 16" fill="none" class="shrink-0 text-[var(--color-text-tertiary)]" aria-hidden="true">
                  <path d="M3 6v4h2.5L9 13V3L5.5 6H3z" fill="currentColor" />
                  <path d="M11 5.5a3.4 3.4 0 010 5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
                </svg>
                <input
                  class="bgm-volume flex-1"
                  type="range"
                  min="0"
                  max="1"
                  step="0.01"
                  :value="volume"
                  aria-label="音量"
                  @input="setVolume(Number(($event.target as HTMLInputElement).value))"
                />
              </div>

              <!-- 把喜欢的音乐收进全站歌单（所有人可见，上传需登录） -->
              <div class="border-t border-[var(--color-divider)] px-4 pb-3.5 pt-3">
                <p class="text-[11px] leading-[1.65] text-[var(--color-text-tertiary)]">
                  上传你喜欢的音乐，收进歌单，就是这个网站的背景音乐
                </p>
                <div class="mt-2.5 flex gap-2">
                  <input
                    ref="bgmFileInput"
                    type="file"
                    accept=".mp3,.flac,audio/mpeg,audio/mp3,audio/flac,audio/x-flac"
                    class="hidden"
                    @change="onBgmFileChange"
                  />
                  <button class="bgm-mini-btn" :disabled="bgmUploading" @click="ensureBgmLogin() && bgmFileInput?.click()">
                    {{ bgmUploading ? '上传中…' : '本地上传' }}
                  </button>
                  <button
                    class="bgm-mini-btn"
                    :class="bgmLinkOpen ? 'bgm-mini-btn-active' : ''"
                    :disabled="bgmAdding"
                    @click="toggleBgmLink"
                  >音乐直链</button>
                </div>
                <div v-if="bgmLinkOpen" class="mt-2 flex items-center gap-2">
                  <input
                    v-model="bgmLinkUrl"
                    class="bgm-link-input"
                    placeholder="粘贴 mp3 / flac 直链"
                    @keyup.enter="addBgmLink"
                  />
                  <button class="bgm-mini-btn !flex-none px-3" :disabled="bgmAdding" @click="addBgmLink">
                    {{ bgmAdding ? '加入中…' : '加入' }}
                  </button>
                </div>
              </div>
            </div>
          </Transition>
        </div>

        <!-- 搜索按钮 -->
        <button
          class="w-8 h-8 flex items-center justify-center rounded-full header-ink-mid nav-hover"
          aria-label="搜索"
          @click="openSearch"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.6" />
            <path d="M11 11L14 14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          </svg>
        </button>

        <!-- 主题切换 -->
        <button
          class="w-8 h-8 flex items-center justify-center rounded-full header-ink-mid nav-hover"
          :aria-label="theme === 'dark' ? '切换到亮色模式' : '切换到暗色模式'"
          @click="toggleTheme"
        >
          <svg
            v-if="theme === 'dark'"
            width="16"
            height="16"
            viewBox="0 0 16 16"
            fill="none"
            aria-hidden="true"
          >
            <circle cx="8" cy="8" r="3.2" stroke="currentColor" stroke-width="1.6" />
            <path
              d="M8 1v1.6M8 13.4V15M15 8h-1.6M2.6 8H1M12.95 3.05l-1.13 1.13M4.18 11.82l-1.13 1.13M12.95 12.95l-1.13-1.13M4.18 4.18L3.05 3.05"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
            />
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path
              d="M13.5 9.5A5.8 5.8 0 016.5 2.5a5.8 5.8 0 107 7z"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linejoin="round"
            />
          </svg>
        </button>

        <!-- 用户入口 -->
        <RouterLink
          v-if="userStore.isLoggedIn"
          to="/profile"
          class="ml-1 w-7 h-7 rounded-full flex items-center justify-center text-[11px] font-semibold text-white overflow-hidden transition-transform duration-200 hover:scale-105"
          :style="{ backgroundColor: stringToColor(userStore.user?.nickname) }"
          :title="userStore.user?.nickname"
        >
          <img
            v-if="userStore.user?.avatar"
            :src="userStore.user.avatar"
            :alt="userStore.user.nickname"
            class="w-full h-full object-cover"
          />
          <span v-else>{{ nameInitial(userStore.user?.nickname) }}</span>
        </RouterLink>
        <RouterLink
          v-else
          to="/login"
          class="ml-1 px-3 py-1.5 rounded-[10px] text-[13px] font-medium header-accent nav-hover-soft"
        >
          登录
        </RouterLink>

        <!-- 移动端菜单 -->
        <button
          class="md:hidden w-8 h-8 flex items-center justify-center rounded-full header-ink-mid nav-hover"
          aria-label="菜单"
          @click="mobileOpen = !mobileOpen"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path
              v-if="!mobileOpen"
              d="M2 5h12M2 11h12"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
            />
            <path
              v-else
              d="M4 4l8 8M12 4l-8 8"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
            />
          </svg>
        </button>
      </div>
    </nav>

    <!-- 移动端下拉菜单 -->
    <Transition name="mobile-menu">
      <div
        v-if="mobileOpen"
        class="md:hidden glass border-b border-[var(--color-divider)]"
      >
        <div class="shell py-3 flex flex-col gap-0.5">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="px-3 py-2.5 rounded-[10px] text-[14px] font-medium transition-colors duration-200"
            :class="
              isActive(item.path)
                ? 'text-[var(--color-text-primary)] bg-[var(--color-surface-sunken)]'
                : 'text-[var(--color-text-secondary)]'
            "
            @click="mobileOpen = false"
          >
            {{ item.label }}
          </RouterLink>
        </div>
      </div>
    </Transition>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTheme } from '@/composables/useTheme'
import { useScrolled } from '@/composables/useScrollReveal'
import { useSiteStore } from '@/stores/site'
import { useUserStore } from '@/stores/user'
import { useBgm } from '@/composables/useBgm'
import { uploadBgmMusic, addBgmMusic } from '@/api'
import { nameInitial, stringToColor } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const siteStore = useSiteStore()
const userStore = useUserStore()
const { theme, toggleTheme } = useTheme()
const { scrolled } = useScrolled(8)
const { playing, pending, current, volume, toggle, next, prev, setVolume, setTrack, refreshPlaylist } = useBgm()

const mobileOpen = ref(false)
const bgmOpen = ref(false)

// 点击面板外任意处收起播放面板
function onDocClick() {
  bgmOpen.value = false
}
watch(bgmOpen, (open) => {
  if (open) document.addEventListener('click', onDocClick)
  else document.removeEventListener('click', onDocClick)
})
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))

const siteName = computed(() => siteStore.config?.siteName ?? '简柚')

/** 首页/搜索页顶部压在深墨沉浸区上：未滚动时导航用浅色字；留言页是全屏文字雨，恒为浅色字 */
const overInk = computed(
  () =>
    route.path === '/message' ||
    ((route.path === '/' || route.path === '/search') && !scrolled.value)
)

const navItems = [
  { label: '今日', path: '/' },
  { label: '记录', path: '/category/1' },
  { label: '游记', path: '/category/2' },
  { label: '随笔', path: '/category/3' },
  { label: '相册', path: '/album' },
  { label: '百宝箱', path: '/toolbox' },
  { label: '家', path: '/about' },
  { label: '留言', path: '/message' }
]

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function openSearch() {
  router.push({ name: 'search' })
}

// 路由变化时关闭移动端菜单
watch(() => route.path, () => {
  mobileOpen.value = false
})

/* ---------- 大家都能投稿：把喜欢的音乐收进全站歌单 ---------- */

const bgmFileInput = ref<HTMLInputElement>()
const bgmUploading = ref(false)
const bgmLinkOpen = ref(false)
const bgmLinkUrl = ref('')
const bgmAdding = ref(false)

/** 从直链里取曲名：取路径最后一段、去扩展名，取不到就叫「在线音乐」 */
function titleFromUrl(u: string): string {
  try {
    const path = new URL(u).pathname
    const last = path.split('/').pop() || ''
    return decodeURIComponent(last).replace(/\.(mp3|flac|wav|ogg|m4a|aac)$/i, '') || '在线音乐'
  } catch {
    return '在线音乐'
  }
}

/** 游客先引导登录；返回是否已登录 */
function ensureBgmLogin(): boolean {
  if (userStore.isLoggedIn) return true
  ElMessage.warning('登录后就能把喜欢的音乐收进歌单啦')
  return false
}

/** 入歌单后刷新播放器：正在播就切到新曲继续播，暂停中则把「正在播放」指向新曲 */
async function refreshToNewTrack(url: string) {
  const list = await refreshPlaylist()
  const idx = list.findIndex((t) => t.url === url)
  if (idx >= 0) setTrack(idx)
  ElMessage.success('已加入背景音乐歌单')
}

async function onBgmFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!ensureBgmLogin()) return
  bgmUploading.value = true
  try {
    const music = await uploadBgmMusic(file)
    await refreshToNewTrack(music.url)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    bgmUploading.value = false
  }
}

function toggleBgmLink() {
  if (!bgmLinkOpen.value && !ensureBgmLogin()) return
  bgmLinkOpen.value = !bgmLinkOpen.value
}

async function addBgmLink() {
  const raw = bgmLinkUrl.value.trim()
  if (!/^https?:\/\/\S+/i.test(raw)) {
    ElMessage.warning('请粘贴以 http(s):// 开头的音频直链')
    return
  }
  bgmAdding.value = true
  try {
    await addBgmMusic({ title: titleFromUrl(raw), artist: null, url: raw })
    await refreshToNewTrack(raw)
    bgmLinkUrl.value = ''
    bgmLinkOpen.value = false
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加入歌单失败')
  } finally {
    bgmAdding.value = false
  }
}
</script>

<style scoped>
/* 压在墨色刊头上时用暖纸色系文字，其余时候用主题变量 */
.header-ink {
  color: var(--color-text-secondary);
}
.header-ink-strong {
  color: var(--color-text-primary);
}
.header-ink-mid {
  color: var(--color-text-secondary);
}
.header-accent {
  color: var(--color-accent);
}

/* overInk 状态：透明背景 + 浅色文字（由 JS 在 header 上控制类名） */
header {
  --header-fg: var(--color-text-primary);
  --header-fg-mid: var(--color-text-secondary);
  --header-fg-hover: var(--color-text-primary);
}
header[data-ink='true'] {
  --header-fg: var(--ink-text);
  --header-fg-mid: var(--ink-text-secondary);
  --header-fg-hover: var(--ink-text);
}
.header-ink { color: var(--header-fg-mid); }
.header-ink-strong { color: var(--header-fg); }
.header-ink-mid { color: var(--header-fg-mid); }
.header-accent { color: var(--ink-accent); }

.nav-hover:hover {
  color: var(--header-fg-hover);
  background: color-mix(in srgb, var(--header-fg) 8%, transparent);
}
.nav-hover-soft:hover {
  background: var(--ink-accent-soft);
}

.nav-active {
  background: color-mix(in srgb, var(--header-fg) 9%, transparent);
}

/* 黑胶播放中：唱片缓慢旋转 */
.bgm-spin {
  animation: bgm-rotate 6s linear infinite;
  transform-origin: 50% 50%;
}
@keyframes bgm-rotate {
  to {
    transform: rotate(360deg);
  }
}

/* 在线音源加载中：脉冲呼吸 */
.bgm-pending {
  animation: bgm-pulse 1.1s ease-in-out infinite;
}
@keyframes bgm-pulse {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 1;
  }
}

/* 音量滑杆 */
.bgm-volume {
  height: 3px;
  appearance: none;
  border-radius: 999px;
  background: var(--color-surface-sunken);
  outline: none;
  cursor: pointer;
  accent-color: var(--color-accent);
}
.bgm-volume::-webkit-slider-thumb {
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-accent);
  border: none;
  transition: transform 0.15s var(--ease-apple);
}
.bgm-volume::-webkit-slider-thumb:hover {
  transform: scale(1.2);
}

/* 投稿小按钮 */
.bgm-mini-btn {
  flex: 1;
  height: 28px;
  border-radius: 9px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  background: transparent;
  cursor: pointer;
  transition: color 0.2s var(--ease-apple), border-color 0.2s var(--ease-apple),
    background 0.2s var(--ease-apple), transform 0.2s var(--ease-apple);
}
.bgm-mini-btn:hover:not(:disabled) {
  color: var(--color-text-primary);
  border-color: var(--color-accent);
  background: color-mix(in srgb, var(--color-accent) 8%, transparent);
}
.bgm-mini-btn:active:not(:disabled) {
  transform: scale(0.97);
}
.bgm-mini-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.bgm-mini-btn-active {
  color: var(--color-text-primary);
  border-color: var(--color-accent);
  background: color-mix(in srgb, var(--color-accent) 10%, transparent);
}

/* 直链输入行 */
.bgm-link-input {
  flex: 1;
  min-width: 0;
  height: 28px;
  padding: 0 10px;
  border-radius: 9px;
  border: 1px solid var(--color-border);
  background: var(--color-surface-sunken);
  color: var(--color-text-primary);
  font-size: 12px;
  outline: none;
  transition: border-color 0.2s var(--ease-apple);
}
.bgm-link-input::placeholder {
  color: var(--color-text-tertiary);
}
.bgm-link-input:focus {
  border-color: var(--color-accent);
}

/* 播放面板出入场 */
.bgm-panel-enter-active,
.bgm-panel-leave-active {
  transition: opacity 0.22s var(--ease-apple), transform 0.22s var(--ease-apple);
  transform-origin: top right;
}
.bgm-panel-enter-from,
.bgm-panel-leave-to {
  opacity: 0;
  transform: scale(0.94) translateY(-4px);
}

.mobile-menu-enter-active,
.mobile-menu-leave-active {
  transition: opacity 0.25s var(--ease-apple), transform 0.25s var(--ease-apple);
}

.mobile-menu-enter-from,
.mobile-menu-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
