<template>
  <!-- 导航栏 fixed 不占流：pt 补偿把内容推到栏下（否则顶部被遮），
       min-h 用满 100dvh（否则文档比视口矮出一个栏高，底部露白） -->
  <div class="auth-split flex min-h-[100dvh] pt-[var(--header-height)]">
    <!-- 左：墨色雨幕（仅桌面端） -->
    <aside class="auth-side relative hidden w-[42%] flex-col justify-between overflow-hidden lg:flex" aria-hidden="true">
      <!-- 雨丝 -->
      <div class="absolute inset-0">
        <span
          v-for="r in 12"
          :key="r"
          class="auth-rain"
          :style="{
            left: `${(r * 83) % 96}%`,
            height: `${60 + ((r * 37) % 80)}px`,
            animationDuration: `${1.4 + ((r * 23) % 12) / 10}s`,
            animationDelay: `${-((r * 41) % 18)}s`,
            opacity: 0.14 + ((r * 17) % 14) / 100
          }"
        />
      </div>

      <!-- 顶部信息条 -->
      <div class="masthead-ink relative z-10 px-10 pt-10">
        <span>简柚</span><span>Jian You</span>
      </div>

      <!-- 中央竖排刊名 -->
      <div class="relative z-10 flex flex-1 items-center justify-center">
        <span
          class="display-serif text-[clamp(2.6rem,3.6vw,3.6rem)] leading-[1.4] tracking-[0.5em]"
          :style="{ color: 'var(--ink-text)', writingMode: 'vertical-rl', opacity: 0.92 }"
        >推门进来</span>
      </div>

      <!-- 底部一句话 -->
      <p
        class="display-serif relative z-10 px-10 pb-10 text-[13px] leading-[1.9] tracking-[0.08em]"
        :style="{ color: 'var(--ink-text-secondary)' }"
      >
        登录之后，可以点赞、收藏，<br />也可以往夜空里写一句话。
      </p>
    </aside>

    <!-- 右：暖纸表单（auth-pane：矮窗口下由 scoped 媒体查询压缩垂直节奏） -->
    <div class="auth-pane flex flex-1 items-center justify-center px-6 py-16">
      <div class="w-full max-w-[380px]">
        <!-- 标题 -->
        <header class="mb-10">
          <div class="masthead-line mb-8">登录 · Sign In</div>
          <h1 class="display-serif text-[30px] tracking-[0.02em] text-[var(--color-text-primary)]">
            回来啦
          </h1>
          <p class="mt-3 text-[13.5px] text-[var(--color-text-secondary)]">
            使用账号登录简柚
          </p>
        </header>

        <!-- 表单 -->
        <form @submit.prevent="submit">
          <!-- 切换账号提示：从「切换账号」入口跳来 -->
          <p
            v-if="switchNotice"
            class="mb-6 rounded-[12px] bg-[var(--color-surface-sunken)] px-4 py-2.5 text-[12.5px] text-[var(--color-text-tertiary)]"
          >
            {{ switchNotice }}
          </p>

          <!-- 手机号 / 邮箱 -->
          <div class="mb-7">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="login-account">
              手机号 / 邮箱
            </label>
            <input
              id="login-account"
              v-model="form.account"
              type="text"
              placeholder="请输入手机号或邮箱"
              autocomplete="username"
              class="field-underline"
              @keyup.enter="submit"
            />
          </div>

          <!-- 密码 -->
          <div class="mb-9">
            <div class="mb-1 flex items-center justify-between">
              <label class="text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="login-password">
                密码
              </label>
              <button
                type="button"
                class="text-[12px] text-[var(--color-accent)] transition-opacity duration-200 hover:opacity-70"
                @click="openReset"
              >
                忘记密码？
              </button>
            </div>
            <div class="relative">
              <input
                id="login-password"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                autocomplete="current-password"
                class="field-underline pr-9"
                @keyup.enter="submit"
              />
              <button
                type="button"
                class="absolute right-1 top-1/2 -translate-y-1/2 text-[var(--color-text-tertiary)] transition-colors duration-200 hover:text-[var(--color-text-primary)]"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <svg v-if="showPassword" width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M1.5 8S3.9 3.5 8 3.5 14.5 8 14.5 8 12.1 12.5 8 12.5 1.5 8 1.5 8z" stroke="currentColor" stroke-width="1.4" />
                  <circle cx="8" cy="8" r="1.9" stroke="currentColor" stroke-width="1.4" />
                </svg>
                <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M6.5 3.8A6.5 6.5 0 018 3.5c4.1 0 6.5 4.5 6.5 4.5a12 12 0 01-2.4 2.9M3.8 5.1A12 12 0 001.5 8S3.9 12.5 8 12.5c.8 0 1.5-.2 2.2-.5M2 2l12 12" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 提交 -->
          <button
            type="button"
            class="btn btn-primary w-full !py-2.5 disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="!canSubmit || submitting"
            @click="submit"
          >
            {{ submitting ? '登录中…' : '登录' }}
          </button>

          <!-- 提示 -->
          <p v-if="error" class="mt-4 text-center text-[12.5px] text-[var(--color-danger)]">
            {{ error }}
          </p>

        <!-- 注册入口 -->
          <p class="sign-switch mt-8 text-center text-[13px] text-[var(--color-text-secondary)]">
            还没有账号？
            <RouterLink
              to="/register"
              class="font-medium text-[var(--color-accent)] transition-opacity duration-200 hover:opacity-70"
            >
              立即注册
            </RouterLink>
          </p>

          <!-- 管理员说明 -->
          <p class="mt-3 text-center text-[12px] text-[var(--color-text-quaternary)]">
            支持手机号、邮箱登录 · 站长账号登录后直达管理后台
          </p>
        </form>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import type { User } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  account: '',
  password: ''
})

const showPassword = ref(false)
const submitting = ref(false)
const error = ref('')

const canSubmit = computed(() => form.account.trim().length >= 3 && form.password.length >= 6)

/** 从「切换账号」跳来时提示上一个账号 */
const switchNotice = computed(() => {
  const from = route.query.from
  return typeof from === 'string' && from ? `已退出「${from}」，可换一个账号登录` : ''
})

/** 从找回密码页回来时带回账号，省得用户再输一遍 */
if (typeof route.query.account === 'string' && route.query.account) {
  form.account = route.query.account
}

/** 去找回密码页，带上当前已填的账号作为线索 */
function openReset() {
  const account = form.account.trim()
  router.push({ path: '/forgot', query: account ? { account } : {} })
}

async function submit() {
  if (!canSubmit.value || submitting.value) return

  error.value = ''
  submitting.value = true

  try {
    const res = await login({
      account: form.account.trim(),
      password: form.password
    })

    // 写入登录态
    userStore.setTokens(res.token, res.refreshToken)
    userStore.setUser({
      id: res.userId,
      nickname: res.nickname,
      avatar: res.avatar,
      gender: 0,
      role: res.role,
      status: 1,
      createTime: ''
    } as User)

    // 有来源页优先回来源页；否则管理员直达后台，普通用户回首页
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect) {
      router.replace(redirect)
    } else if (res.role === 'ADMIN') {
      router.replace('/admin')
    } else {
      router.replace('/')
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
  } finally {
    submitting.value = false
  }
}

</script>

<style scoped>
/* 左侧墨色雨幕：与留言墙同族的深墨底 */
.auth-side {
  background:
    radial-gradient(ellipse 620px 320px at 82% 10%, rgba(240, 130, 74, 0.14) 0%, transparent 62%),
    linear-gradient(180deg, var(--ink-bg-deep) 0%, var(--ink-bg) 55%, #16261f 100%);
}

.masthead-ink {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: var(--ink-text-tertiary);
}
.masthead-ink span + span::before {
  content: '/';
  margin-right: 1rem;
  color: var(--ink-divider);
}

/* 雨丝（与留言墙同款，更稀疏） */
.auth-rain {
  position: absolute;
  top: -140px;
  width: 1px;
  border-radius: 2px;
  background: linear-gradient(180deg, rgba(244, 241, 232, 0) 0%, rgba(244, 241, 232, 0.5) 70%, rgba(244, 241, 232, 0.85) 100%);
  animation: auth-rain-fall linear infinite;
  will-change: transform;
}
@keyframes auth-rain-fall {
  from { transform: translateY(-12vh); }
  to { transform: translateY(120vh); }
}

/*
 * 矮窗口适配：Tailwind 的 lg: 断点只看宽度，1080×561 这类「宽而矮」的窗口
 * 仍会套用大间距（py-16/mb-10…），内容高约 611px > 可用 509px，溢出滚动、左墨块被切。
 * 这里按高度统一压缩垂直节奏，保证不出现滚动；正常高度窗口完全不受影响。
 */
@media (max-height: 720px) {
  .auth-pane {
    padding-top: 1.5rem !important;
    padding-bottom: 1.5rem !important;
  }
  .auth-pane header {
    margin-bottom: 1.25rem !important;
  }
  .auth-pane .masthead-line {
    margin-bottom: 1rem !important;
  }
  .auth-pane form > div {
    margin-bottom: 1rem !important;
  }
  .auth-pane .sign-switch {
    margin-top: 1.25rem !important;
  }
  .auth-pane form > p:last-child {
    margin-top: 0.5rem !important;
  }
}

</style>
