<template>
  <div class="max-w-[560px]">
    <!-- 头像 -->
    <section class="card p-6 mb-5">
      <h2 class="text-[15px] font-semibold text-[var(--color-text-primary)] mb-5">头像</h2>
      <div class="flex items-center gap-5">
        <div
          class="w-16 h-16 rounded-full shrink-0 flex items-center justify-center text-[22px] font-semibold text-white overflow-hidden"
          :style="{ backgroundColor: stringToColor(user?.nickname) }"
        >
          <img v-if="user?.avatar" :src="user.avatar" :alt="user.nickname" class="w-full h-full object-cover" />
          <span v-else>{{ nameInitial(user?.nickname) }}</span>
        </div>
        <div>
          <input
            ref="avatarInput"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            class="hidden"
            @change="onAvatarChange"
          />
          <button
            class="btn btn-secondary !py-2 !px-4 !text-[13px] disabled:opacity-40"
            :disabled="uploadingAvatar"
            @click="pickAvatar"
          >
            {{ uploadingAvatar ? '上传中…' : '更换头像' }}
          </button>
          <p
            v-if="avatarFeedback"
            class="mt-2 text-[12px]"
            :class="avatarOk ? 'text-[var(--color-success)]' : 'text-[var(--color-danger)]'"
          >
            {{ avatarFeedback }}
          </p>
          <p v-else class="mt-2 text-[12px] text-[var(--color-text-tertiary)]">
            支持 JPG、PNG、GIF、WEBP，不超过 2MB
          </p>
        </div>
      </div>
    </section>

    <!-- 留言页背景 -->
    <section class="card p-6 mb-5">
      <h2 class="text-[15px] font-semibold text-[var(--color-text-primary)] mb-5">留言页背景</h2>
      <div class="flex items-start gap-5">
        <!-- 预览：自定义图或默认夜空 -->
        <div
          class="relative h-[84px] w-[150px] shrink-0 overflow-hidden rounded-[10px] bg-[#101714]"
        >
          <img
            v-if="user?.messageBg"
            :src="user.messageBg"
            alt="留言页背景"
            class="h-full w-full object-cover"
          />
          <div v-else class="sky-preview">
            <span class="sky-preview-moon" />
          </div>
          <span
            class="absolute bottom-1.5 left-2 text-[10px] tracking-wide text-white/70"
          >{{ user?.messageBg ? '自定义背景' : '默认夜空' }}</span>
        </div>
        <div>
          <div class="flex items-center gap-3">
            <input
              ref="bgInput"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="hidden"
              @change="onBgChange"
            />
            <button
              class="btn btn-secondary !py-2 !px-4 !text-[13px] disabled:opacity-40"
              :disabled="uploadingBg"
              @click="pickBg"
            >
              {{ uploadingBg ? '上传中…' : user?.messageBg ? '更换背景' : '上传背景' }}
            </button>
            <button
              v-if="user?.messageBg"
              class="btn btn-secondary !py-2 !px-4 !text-[13px] !text-[var(--color-text-tertiary)] disabled:opacity-40"
              :disabled="clearingBg"
              @click="onBgClear"
            >
              {{ clearingBg ? '恢复中…' : '恢复默认' }}
            </button>
          </div>
          <p
            v-if="bgFeedback"
            class="mt-2 text-[12px]"
            :class="bgOk ? 'text-[var(--color-success)]' : 'text-[var(--color-danger)]'"
          >
            {{ bgFeedback }}
          </p>
          <p v-else class="mt-2 text-[12px] leading-[1.7] text-[var(--color-text-tertiary)]">
            上传一张喜欢的图，留言页的夜空就换成它，只对你自己生效。<br />支持 JPG、PNG、GIF、WEBP，不超过 5MB
          </p>
        </div>
      </div>
    </section>

    <!-- 基本资料 -->
    <section class="card p-6 mb-5">
      <h2 class="text-[15px] font-semibold text-[var(--color-text-primary)] mb-5">基本资料</h2>

      <div class="flex flex-col gap-4">
        <div>
          <label class="block text-[12.5px] font-medium text-[var(--color-text-secondary)] mb-2">
            昵称
          </label>
          <input
            v-model="form.nickname"
            type="text"
            maxlength="20"
            class="w-full h-11 px-4 rounded-[12px] bg-[var(--color-surface-sunken)] text-[14px] text-[var(--color-text-primary)] outline-none transition-shadow duration-200 focus:shadow-[0_0_0_2px_var(--color-accent-light)]"
          />
        </div>

        <div>
          <label class="block text-[12.5px] font-medium text-[var(--color-text-secondary)] mb-2">
            个人简介
          </label>
          <textarea
            v-model="form.bio"
            rows="3"
            maxlength="255"
            placeholder="简单介绍一下自己"
            class="w-full resize-none rounded-[12px] bg-[var(--color-surface-sunken)] px-4 py-3 text-[14px] leading-[1.7] text-[var(--color-text-primary)] placeholder:text-[var(--color-text-quaternary)] outline-none transition-shadow duration-200 focus:shadow-[0_0_0_2px_var(--color-accent-light)]"
          />
          <p class="mt-1.5 text-[11.5px] text-[var(--color-text-quaternary)] text-right">
            {{ form.bio.length }} / 255
          </p>
        </div>

        <!-- 只读字段 -->
        <div>
          <label class="block text-[12.5px] font-medium text-[var(--color-text-secondary)] mb-2">
            账号
          </label>
          <div class="flex flex-col gap-2">
            <div
              v-if="user?.phone"
              class="flex items-center justify-between h-11 px-4 rounded-[12px] bg-[var(--color-surface-sunken)]"
            >
              <span class="text-[13.5px] text-[var(--color-text-secondary)]">手机号</span>
              <span class="text-[13.5px] tabular-nums text-[var(--color-text-primary)]">
                {{ maskPhone(user.phone) }}
              </span>
            </div>
            <div
              v-if="user?.email"
              class="flex items-center justify-between h-11 px-4 rounded-[12px] bg-[var(--color-surface-sunken)]"
            >
              <span class="text-[13.5px] text-[var(--color-text-secondary)]">邮箱</span>
              <span class="text-[13.5px] text-[var(--color-text-primary)]">{{ user.email }}</span>
            </div>
            <p
              v-if="!user?.phone && !user?.email"
              class="text-[12.5px] text-[var(--color-text-quaternary)]"
            >
              账号信息读取中
            </p>
          </div>
        </div>
      </div>

      <button
        class="btn btn-primary mt-6 !py-2.5 !px-6 disabled:opacity-40 disabled:cursor-not-allowed"
        :disabled="!canSave || saving"
        @click="save"
      >
        {{ saving ? '保存中…' : '保存修改' }}
      </button>
      <p
        v-if="saveFeedback"
        class="mt-3 text-[12.5px]"
        :class="saveOk ? 'text-[var(--color-success)]' : 'text-[var(--color-danger)]'"
      >
        {{ saveFeedback }}
      </p>
    </section>

    <!-- 修改密码 -->
    <section class="card p-6">
      <h2 class="text-[15px] font-semibold text-[var(--color-text-primary)] mb-5">修改密码</h2>

      <div class="flex flex-col gap-4">
        <div>
          <label class="block text-[12.5px] font-medium text-[var(--color-text-secondary)] mb-2">
            当前密码
          </label>
          <input
            v-model="pwdForm.oldPassword"
            type="password"
            autocomplete="current-password"
            class="w-full h-11 px-4 rounded-[12px] bg-[var(--color-surface-sunken)] text-[14px] text-[var(--color-text-primary)] outline-none transition-shadow duration-200 focus:shadow-[0_0_0_2px_var(--color-accent-light)]"
          />
        </div>
        <div>
          <label class="block text-[12.5px] font-medium text-[var(--color-text-secondary)] mb-2">
            新密码
          </label>
          <input
            v-model="pwdForm.newPassword"
            type="password"
            autocomplete="new-password"
            placeholder="至少 6 位"
            class="w-full h-11 px-4 rounded-[12px] bg-[var(--color-surface-sunken)] text-[14px] text-[var(--color-text-primary)] placeholder:text-[var(--color-text-quaternary)] outline-none transition-shadow duration-200 focus:shadow-[0_0_0_2px_var(--color-accent-light)]"
          />
        </div>
        <div>
          <label class="block text-[12.5px] font-medium text-[var(--color-text-secondary)] mb-2">
            确认新密码
          </label>
          <input
            v-model="pwdForm.confirmPassword"
            type="password"
            autocomplete="new-password"
            class="w-full h-11 px-4 rounded-[12px] bg-[var(--color-surface-sunken)] text-[14px] text-[var(--color-text-primary)] outline-none transition-shadow duration-200 focus:shadow-[0_0_0_2px_var(--color-accent-light)]"
          />
          <p v-if="pwdMismatch" class="mt-2 text-[12px] text-[var(--color-danger)]">
            两次输入的密码不一致
          </p>
        </div>
      </div>

      <button
        class="btn btn-primary mt-6 !py-2.5 !px-6 disabled:opacity-40 disabled:cursor-not-allowed"
        :disabled="!canChangePassword || changingPassword"
        @click="changePassword"
      >
        {{ changingPassword ? '提交中…' : '修改密码' }}
      </button>
      <p
        v-if="pwdFeedback"
        class="mt-3 text-[12.5px]"
        :class="pwdOk ? 'text-[var(--color-success)]' : 'text-[var(--color-danger)]'"
      >
        {{ pwdFeedback }}
      </p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import {
  changePassword as changePasswordApi,
  clearMessageBg,
  updateProfile,
  uploadAvatar,
  uploadMessageBg
} from '@/api/auth'
import type { User } from '@/types'
import { nameInitial, stringToColor } from '@/utils/format'

const userStore = useUserStore()
const user = computed(() => userStore.user)

const form = reactive({
  nickname: '',
  bio: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 用户信息加载后回填
watch(
  user,
  (val) => {
    if (val) {
      form.nickname = val.nickname ?? ''
      form.bio = val.bio ?? ''
    }
  },
  { immediate: true }
)

/* ---------- 头像 ---------- */
const avatarInput = ref<HTMLInputElement | null>(null)
const uploadingAvatar = ref(false)
const avatarFeedback = ref('')
const avatarOk = ref(false)

function pickAvatar() {
  avatarInput.value?.click()
}

async function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 清空以便可重复选择同一文件
  if (!file || uploadingAvatar.value) return

  uploadingAvatar.value = true
  avatarFeedback.value = ''
  try {
    const res = await uploadAvatar(file)
    userStore.setUser({ ...(user.value as User), avatar: res.avatar })
    avatarOk.value = true
    avatarFeedback.value = '头像已更新'
  } catch (err) {
    avatarOk.value = false
    avatarFeedback.value = err instanceof Error ? err.message : '头像上传失败，请稍后再试'
  } finally {
    uploadingAvatar.value = false
  }
}

/* ---------- 留言页背景 ---------- */
const bgInput = ref<HTMLInputElement | null>(null)
const uploadingBg = ref(false)
const clearingBg = ref(false)
const bgFeedback = ref('')
const bgOk = ref(false)

function pickBg() {
  bgInput.value?.click()
}

async function onBgChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 清空以便可重复选择同一文件
  if (!file || uploadingBg.value) return

  uploadingBg.value = true
  bgFeedback.value = ''
  try {
    const res = await uploadMessageBg(file)
    userStore.setUser({ ...(user.value as User), messageBg: res.messageBg })
    bgOk.value = true
    bgFeedback.value = '背景已更新，去留言页看看吧'
  } catch (err) {
    bgOk.value = false
    bgFeedback.value = err instanceof Error ? err.message : '背景上传失败，请稍后再试'
  } finally {
    uploadingBg.value = false
  }
}

async function onBgClear() {
  if (clearingBg.value) return
  clearingBg.value = true
  bgFeedback.value = ''
  try {
    const res = await clearMessageBg()
    userStore.setUser({ ...(user.value as User), messageBg: res.messageBg })
    bgOk.value = true
    bgFeedback.value = '已恢复默认夜空'
  } catch (err) {
    bgOk.value = false
    bgFeedback.value = err instanceof Error ? err.message : '恢复失败，请稍后再试'
  } finally {
    clearingBg.value = false
  }
}

/* ---------- 基本资料 ---------- */
const saving = ref(false)
const saveFeedback = ref('')
const saveOk = ref(false)

const canSave = computed(
  () => form.nickname.trim().length >= 2 && form.nickname.trim().length <= 20
)

async function save() {
  if (!canSave.value || saving.value) return
  saving.value = true
  saveFeedback.value = ''
  try {
    const res = await updateProfile({
      nickname: form.nickname.trim(),
      bio: form.bio.trim() || null
    })
    // 同步到全局登录态，顶栏/名刺立即生效
    userStore.setUser({
      ...(user.value as User),
      nickname: res.nickname
    })
    saveOk.value = true
    saveFeedback.value = '已保存'
  } catch (err) {
    saveOk.value = false
    saveFeedback.value = err instanceof Error ? err.message : '保存失败，请稍后再试'
  } finally {
    saving.value = false
  }
}

/* ---------- 修改密码 ---------- */
const changingPassword = ref(false)
const pwdFeedback = ref('')
const pwdOk = ref(false)

const pwdMismatch = computed(
  () => pwdForm.confirmPassword.length > 0 && pwdForm.newPassword !== pwdForm.confirmPassword
)

const canChangePassword = computed(
  () =>
    pwdForm.oldPassword.length >= 6 &&
    pwdForm.newPassword.length >= 6 &&
    pwdForm.newPassword === pwdForm.confirmPassword
)

async function changePassword() {
  if (!canChangePassword.value || changingPassword.value) return
  changingPassword.value = true
  pwdFeedback.value = ''
  try {
    await changePasswordApi({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    pwdOk.value = true
    pwdFeedback.value = '密码已修改，下次登录请使用新密码'
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (err) {
    pwdOk.value = false
    pwdFeedback.value = err instanceof Error ? err.message : '修改失败，请稍后再试'
  } finally {
    changingPassword.value = false
  }
}

/** 手机号脱敏 */
function maskPhone(phone: string): string {
  if (phone.length !== 11) return phone
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}
</script>

<style scoped>
/* 默认夜空缩略预览（与留言页同族墨绿） */
.sky-preview {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #1a2620 0%, #101714 62%, #0b120e 100%);
}
.sky-preview-moon {
  position: absolute;
  top: 12px;
  right: 16px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: radial-gradient(circle at 38% 34%, #fdf6e3 0%, #e8d9a8 58%, #cdb87b 100%);
  box-shadow: 0 0 14px rgba(253, 246, 227, 0.35);
}
</style>
