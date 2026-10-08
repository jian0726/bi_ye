import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { logoutApi } from '@/api/auth'
import type { User } from '@/types'

/** 用户登录状态 */
export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))

  const isLoggedIn = computed(() => !!accessToken.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  /** 设置令牌 */
  function setTokens(access: string, refresh?: string) {
    accessToken.value = access
    localStorage.setItem('access_token', access)
    if (refresh) localStorage.setItem('refresh_token', refresh)
  }

  /** 设置用户信息 */
  function setUser(u: User | null) {
    user.value = u
  }

  /** 退出登录：先撤销服务端 refresh（后台静默，失败不阻塞），再清本地 */
  function logout() {
    const refreshToken = localStorage.getItem('refresh_token')
    user.value = null
    accessToken.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    if (refreshToken) {
      // fire-and-forget：即使请求失败，refresh 也会在 7 天后自然过期
      logoutApi(refreshToken).catch(() => {})
    }
  }

  return { user, accessToken, isLoggedIn, isAdmin, setTokens, setUser, logout }
})
