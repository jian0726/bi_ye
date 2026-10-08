import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { Result } from '@/types'

/** Axios 实例 */
const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/** 请求拦截器：注入 JWT */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------- 401 自动刷新（双 token 体系：access 过期用 refresh 静默换发后重放） ----------

/** 这些接口自身的 401 不触发刷新（避免刷新请求递归） */
const AUTH_FREE_URLS = ['/auth/refresh', '/auth/login', '/auth/register', '/auth/logout']

/** 单飞标记：并发多个请求同时 401 时只发起一次刷新，其余复用同一个 Promise */
let refreshing: Promise<boolean> | null = null

function redirectToLogin() {
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = `/login?redirect=${redirect}`
}

/**
 * 用 refresh token 换发新双 token。
 * 用独立的裸 axios 调用（不走本文件的拦截器），成功后写入 storage 并返回 true
 */
function tryRefresh(): Promise<boolean> {
  refreshing ??= (async () => {
    const refreshToken = localStorage.getItem('refresh_token')
    if (!refreshToken) return false
    try {
      const res = await axios.post('/api/auth/refresh', { refreshToken }, { timeout: 10000 })
      const payload = res.data as Result<{ token: string; refreshToken: string }>
      if (payload.code !== 200 || !payload.data?.token) return false
      localStorage.setItem('access_token', payload.data.token)
      // 轮换制：服务端已撤销旧 refresh，必须同步覆盖，否则下次刷新必失败
      localStorage.setItem('refresh_token', payload.data.refreshToken)
      return true
    } catch {
      return false
    } finally {
      refreshing = null
    }
  })()
  return refreshing
}

/** 响应拦截器：统一处理业务状态码 */
request.interceptors.response.use(
  async (response: AxiosResponse<Result>) => {
    const res = response.data

    // 二进制流直接返回
    if (response.config.responseType === 'blob') {
      return response as unknown as AxiosResponse
    }

    if (res.code === 200) {
      return res.data as never
    }

    // 401 未登录：先尝试用 refresh token 静默续期并重放原请求
    if (res.code === 401) {
      const config = response.config as InternalAxiosRequestConfig & { _retried?: boolean }
      const authFree = AUTH_FREE_URLS.some((u) => config.url?.includes(u))
      if (!config._retried && !authFree && (await tryRefresh())) {
        config._retried = true
        if (config.headers) {
          config.headers.Authorization = `Bearer ${localStorage.getItem('access_token')}`
        }
        return request(config) as never
      }
      // 无 refresh / 已被撤销 / 刷新失败：清本地并跳登录
      redirectToLogin()
      return Promise.reject(new Error(res.message || '登录已失效'))
    }

    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    let message = '网络异常，请稍后重试'

    if (status === 403) message = '无权限访问'
    else if (status === 404) message = '请求的资源不存在'
    else if (status === 429) message = '操作过于频繁，请稍后再试'
    else if (status >= 500) message = '服务器内部错误'
    else if (error.code === 'ECONNABORTED') message = '请求超时'

    return Promise.reject(new Error(message))
  }
)

export default request
