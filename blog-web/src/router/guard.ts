import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getMe } from '@/api/auth'
import type { User } from '@/types'

/** 路由守卫：页面标题 + 登录态恢复 + 登录/管理员权限校验 */
export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to) => {
    // 更新页面标题
    const title = to.meta.title as string | undefined
    if (title) {
      document.title = `${title} · 简柚`
    }

    const userStore = useUserStore()

    // 刷新后内存 user 丢失但 token 仍在：调 /auth/me 恢复登录态
    if (userStore.accessToken && !userStore.user) {
      try {
        const me = await getMe()
        userStore.setUser({
          id: me.userId,
          nickname: me.nickname,
          avatar: me.avatar,
          messageBg: me.messageBg,
          gender: 0,
          role: me.role,
          status: 1,
          createTime: ''
        } as User)
      } catch {
        userStore.logout()
      }
    }

    // 需要登录的页面
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    // 管理后台：须为 ADMIN
    if (to.meta.requiresAdmin && !userStore.isAdmin) {
      return userStore.isLoggedIn
        ? { name: 'home' }
        : { name: 'login', query: { redirect: to.fullPath } }
    }

    return true
  })
}
