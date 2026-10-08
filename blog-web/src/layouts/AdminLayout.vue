<template>
  <div class="min-h-screen flex bg-[#f7f5f2] text-[#2b2622]">
    <!-- 侧边栏 -->
    <aside class="w-[220px] shrink-0 flex flex-col bg-white fixed inset-y-0 left-0 z-10 border-r border-[#eee7dc]">
      <div class="h-16 flex items-center px-6 border-b border-[#f2ece2]">
        <span class="text-[17px] font-semibold tracking-wide text-[#101714] admin-brand">后台管理</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#ffffff"
        text-color="#5c564e"
        active-text-color="#d95d18"
        class="admin-menu flex-1 !border-r-0 !pt-2"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.path"
          :index="item.path"
          class="!h-11 !leading-11 !mx-2 !rounded-lg !mb-0.5"
        >
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="p-4 border-t border-[#f2ece2] flex items-center gap-3">
        <div class="w-9 h-9 rounded-full bg-[#101714] text-white flex items-center justify-center text-[15px] admin-avatar">
          {{ avatarChar }}
        </div>
        <div class="min-w-0">
          <p class="text-[13px] text-[#2b2622] truncate leading-4">{{ userStore.user?.nickname ?? '博主' }}</p>
          <p class="text-[11px] text-[#a39a8d] leading-4 mt-0.5">简柚 · 站长</p>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="flex-1 ml-[220px] flex flex-col min-h-screen">
      <header class="h-16 bg-white border-b border-[#eee7dc] flex items-center justify-end gap-4 px-6 sticky top-0 z-10">
        <RouterLink
          to="/"
          class="text-[13px] text-[#5c564e] hover:text-[#d95d18] transition-colors"
        >
          返回前台
        </RouterLink>
        <span class="w-px h-4 bg-[#eee7dc]" />
        <button
          class="text-[13px] text-[#5c564e] hover:text-[#d95d18] transition-colors"
          @click="switchAccount"
        >
          切换账号
        </button>
        <span class="w-px h-4 bg-[#eee7dc]" />
        <button
          class="text-[13px] text-[#5c564e] hover:text-[#c6492f] transition-colors"
          @click="handleLogout"
        >
          退出登录
        </button>
      </header>

      <main class="flex-1 p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { stringToColor } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = [
  { label: '仪表盘', path: '/admin' },
  { label: '文章管理', path: '/admin/articles' },
  { label: '分类管理', path: '/admin/categories' },
  { label: '标签管理', path: '/admin/tags' },
  { label: '留言管理', path: '/admin/messages' },
  { label: '友链管理', path: '/admin/links' },
  { label: '资源管理', path: '/admin/files' },
  { label: '用户管理', path: '/admin/users' },
  { label: '相册管理', path: '/admin/album' },
  { label: '音乐管理', path: '/admin/music' },
  { label: '网站设置', path: '/admin/settings' }
]

const avatarChar = computed(() => (userStore.user?.nickname ?? '柚').slice(0, 1))

/** 切换账号：退出登录并回前台登录页，带上原账号昵称做提示 */
function switchAccount() {
  const from = userStore.user?.nickname ?? ''
  userStore.logout()
  router.replace({ name: 'login', query: from ? { from } : undefined })
}

function handleLogout() {
  userStore.logout()
  router.replace('/')
}
</script>

<style scoped>
.admin-brand {
  font-family: var(--font-display);
}
.admin-avatar {
  font-family: var(--font-display);
}
</style>

<style>
/* 后台菜单：柚橙激活态（浅色侧栏） */
.admin-menu .el-menu-item {
  transition: background-color 0.15s ease, color 0.15s ease;
}
.admin-menu .el-menu-item:hover {
  background: #f7f3ec !important;
}
.admin-menu .el-menu-item.is-active {
  background: #faeee4 !important;
  font-weight: 600;
}
</style>
