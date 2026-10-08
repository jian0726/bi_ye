/**
 * 管理后台接口（/admin/**，需 JWT 且 ADMIN 角色，token 由 request.ts 统一注入）
 */
import request from './request'
import type { Article, Category, FriendLink, Message, Music, PageResult, Photo, SiteConfig, Tag } from '@/types'

/* ============================ 仪表盘 ============================ */

export interface ProvinceStat {
  province: string
  count: number
}

export interface DashboardStats {
  articleTotal: number
  publishedTotal: number
  draftTotal: number
  messageTotal: number
  linkPendingTotal: number
  userTotal: number
  viewTotal: number
  todayVisitTotal: number
  provinceStats: ProvinceStat[]
}

export function getDashboard(): Promise<DashboardStats> {
  return request.get<never, DashboardStats>('/admin/dashboard')
}

/* ============================ 网站设置 ============================ */

export function adminGetSiteConfig(): Promise<SiteConfig> {
  return request.get<never, SiteConfig>('/admin/site/config')
}

export function adminUpdateSiteConfig(data: Partial<SiteConfig>): Promise<void> {
  return request.put<never, void>('/admin/site/config', data)
}

/* ============================ 相册管理 ============================ */

export interface AdminPhotoSave {
  title: string
  url?: string | null
  location?: string | null
  /** yyyy-MM-dd */
  takenDate?: string | null
  /** 宽高比，如 3/4 */
  ratio?: string
  tone?: string | null
  emoji?: string | null
  sortOrder?: number
}

export function adminGetPhotos(): Promise<Photo[]> {
  return request.get<never, Photo[]>('/admin/photos')
}

export function adminSavePhoto(data: AdminPhotoSave): Promise<number> {
  return request.post<never, number>('/admin/photos', data)
}

export function adminUpdatePhoto(id: number, data: AdminPhotoSave): Promise<void> {
  return request.put<never, void>(`/admin/photos/${id}`, data)
}

/** 显示 / 隐藏（status: 1 显示 2 隐藏） */
export function adminSetPhotoStatus(id: number, status: number): Promise<void> {
  return request.put<never, void>(`/admin/photos/${id}/status`, null, { params: { status } })
}

export function adminDeletePhoto(id: number): Promise<void> {
  return request.delete<never, void>(`/admin/photos/${id}`)
}

/* ============================ 音乐管理 ============================ */

export interface AdminMusicSave {
  title: string
  artist?: string | null
  url: string
  sortOrder?: number
}

export function adminGetMusics(): Promise<Music[]> {
  return request.get<never, Music[]>('/admin/musics')
}

export function adminSaveMusic(data: AdminMusicSave): Promise<number> {
  return request.post<never, number>('/admin/musics', data)
}

export function adminUpdateMusic(id: number, data: AdminMusicSave): Promise<void> {
  return request.put<never, void>(`/admin/musics/${id}`, data)
}

/** 启用 / 停用（status: 1 启用 2 停用） */
export function adminSetMusicStatus(id: number, status: number): Promise<void> {
  return request.put<never, void>(`/admin/musics/${id}/status`, null, { params: { status } })
}

export function adminDeleteMusic(id: number): Promise<void> {
  return request.delete<never, void>(`/admin/musics/${id}`)
}

/* ============================ 文章管理 ============================ */

export interface AdminArticleQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
  categoryId?: number
}

export function adminGetArticles(params: AdminArticleQuery): Promise<PageResult<Article>> {
  return request.get<never, PageResult<Article>>('/admin/articles', { params })
}

export function adminGetArticle(id: number): Promise<Article> {
  return request.get<never, Article>(`/admin/articles/${id}`)
}

export interface AdminArticleSave {
  title: string
  summary?: string
  content?: string
  cover?: string
  categoryId?: number | null
  tagIds?: number[]
  status?: number
  isTop?: boolean
  isRecommend?: boolean
}

export function adminSaveArticle(data: AdminArticleSave): Promise<number> {
  return request.post<never, number>('/admin/articles', data)
}

export function adminUpdateArticle(id: number, data: AdminArticleSave): Promise<void> {
  return request.put<never, void>(`/admin/articles/${id}`, data)
}

export function adminDeleteArticle(id: number): Promise<void> {
  return request.delete<never, void>(`/admin/articles/${id}`)
}

/** 发布 / 隐藏（status: 1 发布 2 隐藏） */
export function adminSetArticleStatus(id: number, status: number): Promise<void> {
  return request.put<never, void>(`/admin/articles/${id}/status`, null, { params: { status } })
}

/** 置顶切换 */
export function adminToggleArticleTop(id: number): Promise<void> {
  return request.put<never, void>(`/admin/articles/${id}/top`)
}

/* ============================ 分类管理 ============================ */

export function adminGetCategories(): Promise<Category[]> {
  return request.get<never, Category[]>('/admin/categories')
}

export interface AdminCategorySave {
  name: string
  slug?: string
  description?: string
  icon?: string
  sortOrder?: number
}

export function adminSaveCategory(data: AdminCategorySave): Promise<number> {
  return request.post<never, number>('/admin/categories', data)
}

export function adminUpdateCategory(id: number, data: AdminCategorySave): Promise<void> {
  return request.put<never, void>(`/admin/categories/${id}`, data)
}

export function adminDeleteCategory(id: number): Promise<void> {
  return request.delete<never, void>(`/admin/categories/${id}`)
}

/* ============================ 标签管理 ============================ */

export function adminGetTags(): Promise<Tag[]> {
  return request.get<never, Tag[]>('/admin/tags')
}

export interface AdminTagSave {
  name: string
  slug?: string
  color?: string
}

export function adminSaveTag(data: AdminTagSave): Promise<number> {
  return request.post<never, number>('/admin/tags', data)
}

export function adminUpdateTag(id: number, data: AdminTagSave): Promise<void> {
  return request.put<never, void>(`/admin/tags/${id}`, data)
}

export function adminDeleteTag(id: number): Promise<void> {
  return request.delete<never, void>(`/admin/tags/${id}`)
}

/* ============================ 留言管理 ============================ */

export function adminGetMessages(params: {
  page?: number
  size?: number
  ipLocation?: string
}): Promise<PageResult<Message>> {
  return request.get<never, PageResult<Message>>('/admin/messages', { params })
}

export function adminDeleteMessage(id: number): Promise<void> {
  return request.delete<never, void>(`/admin/messages/${id}`)
}

/* ============================ 友链管理 ============================ */

export interface AdminFriendLink extends Omit<FriendLink, 'id'> {
  id?: number
  status?: number
  sortOrder?: number
  createTime?: string
}

export function adminGetLinks(): Promise<AdminFriendLink[]> {
  return request.get<never, AdminFriendLink[]>('/admin/links')
}

export function adminSaveLink(data: AdminFriendLink): Promise<number> {
  return request.post<never, number>('/admin/links', data)
}

export function adminUpdateLink(id: number, data: AdminFriendLink): Promise<void> {
  return request.put<never, void>(`/admin/links/${id}`, data)
}

/** 审核上架 / 下架（status: 1 上架 2 下架） */
export function adminSetLinkStatus(id: number, status: number): Promise<void> {
  return request.put<never, void>(`/admin/links/${id}/status`, null, { params: { status } })
}

export function adminDeleteLink(id: number): Promise<void> {
  return request.delete<never, void>(`/admin/links/${id}`)
}

/* ============================ 用户管理 ============================ */

export interface AdminUserVO {
  id: number
  phone?: string | null
  email?: string | null
  nickname: string
  avatar?: string | null
  role: 'ADMIN' | 'USER'
  status: number
  createTime: string
}

export function adminGetUsers(params: {
  page?: number
  size?: number
  keyword?: string
}): Promise<PageResult<AdminUserVO>> {
  return request.get<never, PageResult<AdminUserVO>>('/admin/users', { params })
}

/** 启用 / 禁用用户（status: 0 禁用 1 正常） */
export function adminSetUserStatus(id: number, status: number): Promise<void> {
  return request.put<never, void>(`/admin/users/${id}/status`, null, { params: { status } })
}

/* ============================ 资源管理 ============================ */

export interface AdminFile {
  id: number
  name: string
  objectKey: string
  url: string
  type?: string | null
  size: number
  createTime: string
}

export function adminGetFiles(params: {
  page?: number
  size?: number
  keyword?: string
  /** 按类型过滤：image / audio（后端按 MIME 前缀匹配） */
  type?: 'image' | 'audio'
}): Promise<PageResult<AdminFile>> {
  return request.get<never, PageResult<AdminFile>>('/admin/files', { params })
}

export function adminDeleteFile(id: number): Promise<void> {
  return request.delete<never, void>(`/admin/files/${id}`)
}

/* ============================ 文件上传 ============================ */

/** 上传文件到 MinIO（图片 jpg/png/gif/webp 或音频 mp3/flac），返回访问地址 */
export function adminUpload(file: File): Promise<{ url: string }> {
  const form = new FormData()
  form.append('file', file)
  return request.post<never, { url: string }>('/admin/uploads', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
