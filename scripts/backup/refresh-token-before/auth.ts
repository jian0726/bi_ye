/**
 * 认证接口：登录 / 注册 / 当前用户
 */
import request from './request'

/** 登录/注册成功返回 */
export interface LoginResult {
  token: string
  userId: number
  nickname: string
  avatar: string | null
  /** 留言页自定义背景图（null = 默认夜空） */
  messageBg: string | null
  role: 'USER' | 'ADMIN'
}

/** 登录（手机号 / 邮箱 任一） */
export function login(data: { account: string; password: string }): Promise<LoginResult> {
  return request.post<never, LoginResult>('/auth/login', data)
}

/** 注册（手机号 / 邮箱至少填一个 + 昵称 + 密码；无独立用户名字段） */
export function register(data: {
  nickname: string
  password: string
  phone?: string | null
  email?: string | null
}): Promise<LoginResult> {
  return request.post<never, LoginResult>('/auth/register', data)
}

/** 当前登录用户（用于刷新页面后恢复登录态） */
export function getMe(): Promise<LoginResult> {
  return request.get<never, LoginResult>('/auth/me')
}

/** 修改个人资料（昵称 / 简介），返回更新后的用户信息 */
export function updateProfile(data: { nickname: string; bio?: string | null }): Promise<LoginResult> {
  return request.put<never, LoginResult>('/auth/profile', data)
}

/** 修改密码（需校验当前密码） */
export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<void> {
  return request.put<never, void>('/auth/password', data)
}

/** 上传/更换头像（图片 ≤2MB），返回更新后的用户信息 */
export function uploadAvatar(file: File): Promise<LoginResult> {
  const form = new FormData()
  form.append('file', file)
  return request.post<never, LoginResult>('/auth/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

/** 上传/更换留言页自定义背景（图片 ≤5MB，登记资源管理），返回更新后的用户信息 */
export function uploadMessageBg(file: File): Promise<LoginResult> {
  const form = new FormData()
  form.append('file', file)
  return request.post<never, LoginResult>('/auth/message-bg', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/** 清除留言页自定义背景，恢复默认夜空 */
export function clearMessageBg(): Promise<LoginResult> {
  return request.delete<never, LoginResult>('/auth/message-bg')
}

// ---------- 找回密码（无需登录） ----------

/** 可用的验证渠道 */
export interface ResetChannel {
  /** phone / email */
  channel: 'phone' | 'email'
  /** 脱敏后的接收目标，如 138****0001 / ji***@local.dev */
  masked: string
}

/** 找回密码第一步响应 */
export interface FindAccountResult {
  exists: boolean
  channels: ResetChannel[]
}

/**
 * 第一步：查该账号可用的验证方式
 * 找回方式跟随注册时留下的联系方式：只有手机号就只能走手机号，两者都有则可选
 */
export function findAccount(account: string): Promise<FindAccountResult> {
  return request.post<never, FindAccountResult>('/auth/find-account', { account })
}

/**
 * 第二步：发送验证码
 * 开发模式下返回值带 devCode 便于联调；接入真实短信/邮件后为 undefined
 */
export function sendResetCode(account: string, channel: string): Promise<{ devCode?: string }> {
  return request.post<never, { devCode?: string }>('/auth/send-code', { account, channel })
}

/** 第三步：校验验证码并重置密码 */
export function resetPassword(data: {
  account: string
  channel: string
  code: string
  newPassword: string
}): Promise<void> {
  return request.post<never, void>('/auth/reset-password', data)
}

