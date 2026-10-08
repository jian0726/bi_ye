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
        <span>简柚</span><span>Reset</span>
      </div>

      <div class="relative z-10 flex flex-1 items-center justify-center">
        <span
          class="display-serif text-[clamp(2.6rem,3.6vw,3.6rem)] leading-[1.4] tracking-[0.5em]"
          :style="{ color: 'var(--ink-text)', writingMode: 'vertical-rl', opacity: 0.92 }"
        >找回钥匙</span>
      </div>

      <p
        class="display-serif relative z-10 px-10 pb-10 text-[13px] leading-[1.9] tracking-[0.08em]"
        :style="{ color: 'var(--ink-text-secondary)' }"
      >
        用注册时留下的手机号或邮箱，<br />重新设一个密码。
      </p>
    </aside>

    <!-- 右：暖纸表单 -->
    <div class="auth-pane flex flex-1 items-center justify-center px-6 py-16">
      <div class="w-full max-w-[380px]">
        <header class="mb-10">
          <div class="masthead-line mb-8">找回密码 · Reset</div>
          <h1 class="display-serif text-[30px] tracking-[0.02em] text-[var(--color-text-primary)]">
            {{ stepTitle }}
          </h1>
          <p class="mt-3 text-[13.5px] text-[var(--color-text-secondary)]">
            {{ stepHint }}
          </p>
        </header>

        <!-- 步骤指示 -->
        <ol class="step-track mb-8">
          <li v-for="(label, i) in STEP_LABELS" :key="label" :class="{ done: step > i + 1, active: step === i + 1 }">
            <span class="step-dot">{{ i + 1 }}</span>
            <span class="step-text">{{ label }}</span>
          </li>
        </ol>

        <!-- 步骤一：输入账号 -->
        <form v-if="step === 1" @submit.prevent="probe">
          <div class="mb-7">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="fp-account">
              手机号 / 邮箱
            </label>
            <input
              id="fp-account"
              v-model="form.account"
              type="text"
              placeholder="请输入注册时使用的手机号或邮箱"
              autocomplete="username"
              class="field-underline"
              @keyup.enter="probe"
            />
          </div>

          <button
            type="button"
            class="btn btn-primary w-full !py-2.5 disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="!form.account.trim() || probing"
            @click="probe"
          >
            {{ probing ? '查询中…' : '下一步' }}
          </button>

          <p class="sign-switch mt-8 text-center text-[13px] text-[var(--color-text-secondary)]">
            想起来了？
            <RouterLink
              to="/login"
              class="font-medium text-[var(--color-accent)] transition-opacity duration-200 hover:opacity-70"
            >
              返回登录
            </RouterLink>
          </p>
        </form>

        <!-- 步骤二：选渠道 + 填验证码 -->
        <form v-else-if="step === 2" @submit.prevent="verifyAndNext">
          <!-- 渠道选择：账号同时绑了手机号和邮箱时才出现 -->
          <div v-if="channels.length > 1" class="mb-7">
            <label class="mb-2 block text-[12.5px] font-medium text-[var(--color-text-secondary)]">
              选择接收方式
            </label>
            <div class="flex gap-2">
              <button
                v-for="c in channels"
                :key="c.channel"
                type="button"
                class="channel-chip"
                :class="{ 'channel-chip-on': form.channel === c.channel }"
                @click="pickChannel(c.channel)"
              >
                {{ c.channel === 'phone' ? '手机号' : '邮箱' }}
                <span class="channel-target">{{ c.masked }}</span>
              </button>
            </div>
          </div>

          <!-- 只有一个渠道时直接说明发到哪 -->
          <p
            v-else-if="currentChannel"
            class="mb-7 rounded-[12px] bg-[var(--color-surface-sunken)] px-4 py-2.5 text-[12.5px] text-[var(--color-text-tertiary)]"
          >
            验证码将发送至{{ currentChannel.channel === 'phone' ? '手机号' : '邮箱' }}
            <span class="font-medium text-[var(--color-text-secondary)]">{{ currentChannel.masked }}</span>
          </p>

          <div class="mb-7">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="fp-code">
              验证码
            </label>
            <div class="flex items-end gap-3">
              <input
                id="fp-code"
                v-model="form.code"
                type="text"
                inputmode="numeric"
                maxlength="6"
                placeholder="6 位数字"
                autocomplete="one-time-code"
                class="field-underline flex-1"
                @keyup.enter="verifyAndNext"
              />
              <button
                type="button"
                class="send-btn"
                :disabled="cooldown > 0 || sending"
                @click="doSendCode"
              >
                {{ cooldown > 0 ? `${cooldown}s` : sending ? '发送中…' : '发送验证码' }}
              </button>
            </div>
            <!-- 开发模式提示：验证码不经真实通道下发 -->
            <p v-if="devCode" class="mt-2 text-[12px] text-[var(--color-accent)]">
              开发模式：验证码为 <span class="font-medium tracking-[0.2em]">{{ devCode }}</span>
            </p>
          </div>

          <button
            type="button"
            class="btn btn-primary w-full !py-2.5 disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="form.code.trim().length !== 6 || verifying"
            @click="verifyAndNext"
          >
            {{ verifying ? '校验中…' : '下一步' }}
          </button>

          <p class="sign-switch mt-6 text-center text-[13px] text-[var(--color-text-secondary)]">
            <button type="button" class="text-[var(--color-accent)] transition-opacity duration-200 hover:opacity-70" @click="backToStep1">
              换个账号
            </button>
          </p>
        </form>

        <!-- 步骤三：设新密码 -->
        <form v-else @submit.prevent="doReset">
          <div class="mb-7">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="fp-pwd">
              新密码
            </label>
            <input
              id="fp-pwd"
              v-model="form.newPassword"
              type="password"
              placeholder="6-32 位"
              autocomplete="new-password"
              class="field-underline"
              @keyup.enter="doReset"
            />
          </div>

          <div class="mb-9">
            <label class="mb-1 block text-[12.5px] font-medium text-[var(--color-text-secondary)]" for="fp-pwd2">
              确认新密码
            </label>
            <input
              id="fp-pwd2"
              v-model="form.confirmPassword"
              type="password"
              placeholder="再输一次"
              autocomplete="new-password"
              class="field-underline"
              @keyup.enter="doReset"
            />
          </div>

          <button
            type="button"
            class="btn btn-primary w-full !py-2.5 disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="!canReset || resetting"
            @click="doReset"
          >
            {{ resetting ? '提交中…' : '重设密码' }}
          </button>
        </form>

        <!-- 提示 -->
        <p v-if="error" class="mt-4 text-center text-[12.5px] text-[var(--color-danger)]">
          {{ error }}
        </p>
        <p v-if="notice" class="mt-4 text-center text-[12.5px] text-[var(--color-success)]">
          {{ notice }}
        </p>

        <p v-if="step === 3" class="mt-3 text-center text-[12px] text-[var(--color-text-quaternary)]">
          重设成功后请用新密码登录
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { findAccount, resetPassword, sendResetCode, type ResetChannel } from '@/api/auth'

const route = useRoute()
const router = useRouter()

const STEP_LABELS = ['输入账号', '验证身份', '设置新密码']

const step = ref(1)
const stepTitle = computed(() => ['', '先确认是你', '换个新密码'][step.value])
const stepHint = computed(() => [
  '',
  '我们会把验证码发到注册时留下的联系方式',
  '记住它，下次推门就用这个'
][step.value])

const form = reactive({
  account: '',
  channel: '' as '' | 'phone' | 'email',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

/** 从登录页跳来时带上已填的账号，省一步输入 */
if (typeof route.query.account === 'string' && route.query.account) {
  form.account = route.query.account
}

const channels = ref<ResetChannel[]>([])
const probing = ref(false)
const sending = ref(false)
const verifying = ref(false)
const resetting = ref(false)
const error = ref('')
const notice = ref('')
/** 开发模式下后端回传的验证码，仅用于本机联调 */
const devCode = ref('')
/** 重发倒计时（秒） */
const cooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const currentChannel = computed(() => channels.value.find((c) => c.channel === form.channel) || null)

const canReset = computed(
  () =>
    form.newPassword.length >= 6 &&
    form.newPassword.length <= 32 &&
    form.newPassword === form.confirmPassword
)

function clearTips() {
  error.value = ''
  notice.value = ''
}

/** 步骤一：查账号可用的验证方式 —— 找回方式跟随注册时用的手机号或邮箱 */
async function probe() {
  const account = form.account.trim()
  if (!account || probing.value) return
  clearTips()
  probing.value = true
  try {
    const res = await findAccount(account)
    if (!res.exists || !res.channels.length) {
      error.value = '没有找到这个账号，请检查手机号或邮箱是否输错'
      return
    }
    channels.value = res.channels
    // 只有一个可用渠道时自动选中，用户不用再点一次
    form.channel = res.channels[0].channel
    devCode.value = ''
    form.code = ''
    step.value = 2
    await doSendCode()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '查询失败'
  } finally {
    probing.value = false
  }
}

function pickChannel(channel: 'phone' | 'email') {
  if (form.channel === channel) return
  form.channel = channel
  form.code = ''
  devCode.value = ''
  // 换渠道需重新取码，重置倒计时让按钮可点
  cooldown.value = 0
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
  void doSendCode()
}

async function doSendCode() {
  if (!form.channel || sending.value || cooldown.value > 0) return
  clearTips()
  sending.value = true
  try {
    const res = await sendResetCode(form.account.trim(), form.channel)
    devCode.value = res?.devCode || ''
    notice.value = devCode.value ? '' : '验证码已发送，请查收'
    startCooldown()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发送失败'
  } finally {
    sending.value = false
  }
}

function startCooldown() {
  cooldown.value = 60
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0 && cooldownTimer) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

/** 步骤二：验证码暂存，进入设密码步骤时随后端一并提交校验 */
function verifyAndNext() {
  if (form.code.trim().length !== 6) {
    error.value = '请输入 6 位验证码'
    return
  }
  clearTips()
  step.value = 3
}

function backToStep1() {
  clearTips()
  step.value = 1
  form.code = ''
  devCode.value = ''
  channels.value = []
  form.channel = ''
}

/** 步骤三：提交验证码 + 新密码 */
async function doReset() {
  if (!canReset.value || resetting.value) return
  clearTips()
  resetting.value = true
  try {
    await resetPassword({
      account: form.account.trim(),
      channel: form.channel,
      code: form.code.trim(),
      newPassword: form.newPassword
    })
    notice.value = '密码已重设，正在返回登录…'
    setTimeout(() => {
      router.replace({ path: '/login', query: { account: form.account.trim() } })
    }, 1200)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '重置失败'
    // 验证码类错误退回第二步，让用户重取
    if (error.value.includes('验证码')) {
      step.value = 2
      form.code = ''
      devCode.value = ''
      cooldown.value = 0
    }
  } finally {
    resetting.value = false
  }
}

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style scoped>
/* 左侧墨色雨幕：与登录页同族 */
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

/* 步骤指示：细线 + 序号，不走满屏进度条那种重样式 */
.step-track {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.step-track li {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex: 1;
  font-size: 11.5px;
  color: var(--color-text-quaternary);
  transition: color 0.3s ease;
}
.step-track li + li::before {
  content: '';
  flex: none;
  width: 1.25rem;
  height: 1px;
  background: var(--color-border);
}
.step-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  font-size: 10.5px;
  line-height: 1;
  transition: all 0.3s ease;
}
.step-track li.active {
  color: var(--color-accent);
}
.step-track li.active .step-dot {
  border-color: var(--color-accent);
  background: var(--color-accent);
  color: #fff;
}
.step-track li.done {
  color: var(--color-text-tertiary);
}
.step-track li.done .step-dot {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
@media (max-width: 420px) {
  .step-text { display: none; }
}

/* 渠道选择芯片 */
.channel-chip {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  font-size: 12.5px;
  color: var(--color-text-secondary);
  text-align: left;
  background: transparent;
  transition: all 0.2s ease;
}
.channel-chip:hover {
  border-color: var(--color-accent);
}
.channel-chip-on {
  border-color: var(--color-accent);
  background: color-mix(in srgb, var(--color-accent) 7%, transparent);
  color: var(--color-text-primary);
}
.channel-target {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

/* 发送验证码按钮 */
.send-btn {
  flex: none;
  padding-bottom: 0.35rem;
  font-size: 12.5px;
  white-space: nowrap;
  color: var(--color-accent);
  transition: opacity 0.2s ease;
}
.send-btn:disabled {
  color: var(--color-text-quaternary);
  cursor: not-allowed;
}
.send-btn:not(:disabled):hover {
  opacity: 0.7;
}

/*
 * 矮窗口适配：与登录页一致——Tailwind 的断点只看宽度，
 * 1080×561 这类宽矮窗口会套用大间距导致溢出，这里按高度统一压缩。
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
  .auth-pane .step-track {
    margin-bottom: 1.25rem !important;
  }
  .auth-pane form > div {
    margin-bottom: 1rem !important;
  }
  .auth-pane .sign-switch {
    margin-top: 1.25rem !important;
  }
}
</style>
