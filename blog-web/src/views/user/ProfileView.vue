<template>
  <div>
    <PageMasthead
      title="个人中心"
      kicker="Reader's Desk"
      subtitle="翻一翻自己写过的字，收一收喜欢的篇目。"
      :meta="['读者台', userStore.user?.nickname ?? '']"
      ghost="读者"
    />

    <div class="shell pt-12 pb-[var(--section-gap)]">
      <div class="grid grid-cols-1 lg:grid-cols-[240px_1fr] gap-10 lg:gap-14">
        <!-- 左：读者名刺 + 目录式导航 -->
        <aside>
          <div class="card hover-rule p-6 lg:sticky lg:top-[calc(var(--header-height)+32px)]">
            <!-- 名刺 -->
            <div class="mb-6 flex items-center gap-4 border-b border-[var(--color-divider)] pb-5">
              <div
                class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-full text-[18px] font-semibold text-white"
                :style="{ backgroundColor: stringToColor(userStore.user?.nickname) }"
              >
                <img
                  v-if="userStore.user?.avatar"
                  :src="userStore.user.avatar"
                  :alt="userStore.user?.nickname"
                  class="h-full w-full object-cover"
                />
                <span v-else>{{ nameInitial(userStore.user?.nickname) }}</span>
              </div>
              <div class="min-w-0">
                <p class="display-serif truncate text-[17px] text-[var(--color-text-primary)]">
                  {{ userStore.user?.nickname }}
                </p>
                <p class="mt-0.5 truncate text-[11.5px] text-[var(--color-text-tertiary)]">
                  {{ userStore.isAdmin ? '站长' : '读者' }}
                </p>
              </div>
            </div>

            <!-- 账号操作 -->
            <div class="mb-6 flex flex-col gap-2 border-b border-[var(--color-divider)] pb-6">
              <RouterLink
                v-if="userStore.isAdmin"
                to="/admin"
                class="btn btn-primary w-full !py-2.5 !text-[13px]"
              >
                进入管理后台
              </RouterLink>
              <button class="btn btn-secondary w-full !py-2.5 !text-[13px]" @click="switchAccount">
                切换账号
              </button>
              <button
                class="btn btn-secondary w-full !py-2.5 !text-[13px] !text-[var(--color-danger)]"
                @click="logout"
              >
                退出登录
              </button>
            </div>

            <!-- 目录式导航 -->
            <nav class="flex gap-1 overflow-x-auto lg:flex-col lg:overflow-visible">
              <RouterLink
                v-for="(item, i) in tabs"
                :key="item.path"
                :to="item.path"
                class="group flex shrink-0 items-baseline gap-3 border-l-2 px-4 py-2.5 transition-all duration-200 lg:border-l-2 lg:px-0 lg:py-2 lg:pl-3"
                :class="
                  route.path === item.path
                    ? 'border-[var(--color-accent)] text-[var(--color-text-primary)]'
                    : 'border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                "
              >
                <span
                  class="display-serif text-[12px]"
                  :class="route.path === item.path ? 'text-[var(--color-accent)]' : 'text-[var(--color-text-quaternary)]'"
                >{{ String(i + 1).padStart(2, '0') }}</span>
                <span class="text-[13.5px] font-medium">{{ item.label }}</span>
              </RouterLink>
            </nav>
          </div>
        </aside>

        <!-- 内容区 -->
        <div class="min-w-0">
          <RouterView />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import PageMasthead from '@/components/PageMasthead.vue'
import { useUserStore } from '@/stores/user'
import { nameInitial, stringToColor } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const tabs = [
  { label: '基本资料', path: '/profile' },
  { label: '我的收藏', path: '/profile/collections' }
]

/** 切换账号：退出登录并回登录页，带上原账号昵称做提示 */
function switchAccount() {
  const from = userStore.user?.nickname ?? ''
  userStore.logout()
  router.replace({ name: 'login', query: from ? { from } : undefined })
}

/** 退出登录：回首页 */
function logout() {
  userStore.logout()
  router.replace('/')
}
</script>
