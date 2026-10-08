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

/** 响应拦截器：统一处理业务状态码 */
request.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const res = response.data

    // 二进制流直接返回
    if (response.config.responseType === 'blob') {
      return response as unknown as AxiosResponse
    }

    if (res.code === 200) {
      return res.data as never
    }

    // 401 未登录
    if (res.code === 401) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      const redirect = encodeURIComponent(window.location.pathname + window.location.search)
      window.location.href = `/login?redirect=${redirect}`
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
