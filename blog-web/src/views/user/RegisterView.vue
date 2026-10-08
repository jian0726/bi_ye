<template>
  <!-- 导航栏 fixed 不占流：pt 补偿把内容推到栏下（否则顶部被遮），
       min-h 用满 100dvh（否则文档比视口矮出一个栏高，底部露白） -->
  <div class="auth-split flex min-h-[100dvh] pt-[var(--header-height)]">
    <!-- 左：墨色雨幕（仅桌面端） -->
    <aside class="auth-side relative hidden w-[42%] flex-col justify-between overflow-hidden lg:flex" aria-hidden="true">
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

      <div class="masthead-ink relative z-10 px-10 pt-10">
        <span>简柚</span><span>Join Us</span>
      </div>

      <div class="relative z-10 flex flex-1 items-center justify-center">
        <span
          class="display-serif text-[clamp(2.6rem,3.6vw,3.6rem)] leading-[1.4] tracking-[0.5em]"
          :style="{ color: 'var(--ink-text)', writingMode: 'vertical-rl', opacity: 0.92 }"
        >落个脚</span>
      </div>

      <p
        class="display-serif relative z-10 px-10 pb-10 text-[13px] leading-[1.9] tracking-[0.08em]"
        :style="{ color: 'var(--ink-text-secondary)' }"
      >
        注册一个账号，把你的话<br />也留在这个家的墙上。
      </p>
    </aside>

    <!-- 右：暖纸表单（auth-pane：矮窗口下由 scoped 媒体查询压缩垂直节奏） -->
    <div class="auth-pane flex flex-1 items-center justify-center px-6 py-16">
      <div class="w-full max-w-[380px]">
        <header class="mb-10">
          <div class="masthead-line mb-8">注册 · Sign Up</div>
          <h1 class="display-serif text-[30px] tracking-[0.02em] text-[var(--color-text-primary)]">
            创建账号
          </h1>
          <p class="mt-3 text-[13.5px] text-[var(--color-text-secondary)]">
            参与点赞与收藏
          </p>
        </header>

        <form @submit.prevent="submit">
          <!-- 手机号 -->
          <div class="mb-7">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="reg-phone">
              手机号
            </label>
            <input
              id="reg-phone"
              v-model="form.phone"
              type="tel"
              placeholder="11 位手机号，用于登录"
              maxlength="11"
              autocomplete="tel"
              class="field-underline"
            />
            <p v-if="form.phone && !phoneValid" class="mt-1.5 text-[12px] text-[var(--color-danger)]">
              手机号格式不正确
            </p>
          </div>

          <!-- 邮箱 -->
          <div class="mb-7">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="reg-email">
              邮箱
            </label>
            <input
              id="reg-email"
              v-model="form.email"
              type="email"
              placeholder="邮箱地址，用于登录"
              maxlength="100"
              autocomplete="email"
              class="field-underline"
            />
            <p v-if="form.email && !emailValid" class="mt-1.5 text-[12px] text-[var(--color-danger)]">
              邮箱格式不正确
            </p>
          </div>

          <!-- 联系方式提示 -->
          <p class="-mt-4 mb-7 text-[12px] text-[var(--color-text-quaternary)]">
            手机号与邮箱至少填一个，两者都可用于登录
          </p>

          <!-- 昵称 -->
          <div class="mb-7">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="reg-nickname">
              昵称 <span class="text-[var(--color-danger)]">*</span>
            </label>
            <input
              id="reg-nickname"
              v-model="form.nickname"
              type="text"
              placeholder="2-20 个字符"
              maxlength="20"
              class="field-underline"
            />
          </div>

          <!-- 密码 -->
          <div class="mb-9">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="reg-password">
              密码 <span class="text-[var(--color-danger)]">*</span>
            </label>
            <input
              id="reg-password"
              v-model="form.password"
              type="password"
              placeholder="至少 6 位，建议包含字母和数字"
              autocomplete="new-password"
              class="field-underline"
            />
            <!-- 密码强度 -->
            <div v-if="form.password" class="mt-3 flex items-center gap-2">
              <div class="h-[3px] flex-1 overflow-hidden rounded-full bg-[var(--color-surface-sunken)]">
                <div
                  class="h-full rounded-full transition-all duration-300"
                  :style="{ width: `${strength.percent}%`, backgroundColor: strength.color }"
                />
              </div>
              <span class="shrink-0 text-[11.5px]" :style="{ color: strength.color }">
                {{ strength.label }}
              </span>
            </div>
          </div>

          <button
            type="button"
            class="btn btn-primary w-full !py-2.5 disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="!canSubmit || submitting"
            @click="submit"
          >
            {{ submitting ? '注册中…' : '创建账号' }}
          </button>

          <p v-if="error" class="mt-4 text-center text-[12.5px] text-[var(--color-danger)]">
            {{ error }}
          </p>

          <p class="sign-switch mt-8 text-center text-[13px] text-[var(--color-text-secondary)]">
            已有账号？
            <RouterLink
              to="/login"
              class="font-medium text-[var(--color-accent)] transition-opacity duration-200 hover:opacity-70"
            >
              去登录
            </RouterLink>
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'
import type { User } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const submitting = ref(false)
const error = ref('')

const form = reactive({
  phone: '',
  email: '',
  nickname: '',
  password: ''
})

const PHONE_RE = /^1[3-9]\d{9}$/
const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const phoneValid = computed(() => PHONE_RE.test(form.phone.trim()))
const emailValid = computed(() => EMAIL_RE.test(form.email.trim()))

/** 手机号/邮箱至少填一个，且填了的必须格式正确 */
const contactValid = computed(() => {
  const p = form.phone.trim()
  const e = form.email.trim()
  if (p === '' && e === '') return false
  if (p !== '' && !phoneValid.value) return false
  if (e !== '' && !emailValid.value) return false
  return true
})

const canSubmit = computed(
  () =>
    contactValid.value &&
    form.nickname.trim().length >= 2 &&
    form.password.length >= 6
)

/** 密码强度 */
const strength = computed(() => {
  const pwd = form.password
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^\w\s]/.test(pwd)) score++

  if (score <= 2) return { percent: 33, label: '弱', color: 'var(--color-danger)' }
  if (score <= 3) return { percent: 66, label: '中', color: 'var(--color-warning)' }
  return { percent: 100, label: '强', color: 'var(--color-success)' }
})

async function submit() {
  if (!canSubmit.value || submitting.value) return

  error.value = ''
  submitting.value = true

  try {
    const res = await register({
      phone: form.phone.trim() ? form.phone.trim() : null,
      email: form.email.trim() ? form.email.trim() : null,
      nickname: form.nickname.trim(),
      password: form.password
    })

    // 注册成功即登录
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

    router.replace('/')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '注册失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
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
 * 仍会套用大间距（py-16/mb-10…），内容溢出视口、左墨块被切。
 * 按高度统一压缩垂直节奏，保证不出现滚动；正常高度窗口完全不受影响。
 * （注册页字段更多，压缩后仍略高时允许极小滚动）
 */
@media (max-height: 720px) {
  .auth-pane {
    padding-top: 1.25rem !important;
    padding-bottom: 1.25rem !important;
  }
  .auth-pane header {
    margin-bottom: 1rem !important;
  }
  .auth-pane .masthead-line {
    margin-bottom: 0.75rem !important;
  }
  .auth-pane form > div {
    margin-bottom: 0.875rem !important;
  }
  .auth-pane .sign-switch {
    margin-top: 1rem !important;
  }
}
</style>
