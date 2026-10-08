/**
 * 前台接口封装
 *
 * 全部请求直连后端（/api 由 vite 代理到 8080），无本地模拟数据。
 * 返回值统一由 request.ts 拆掉 Result 外壳，函数签名保持一致。
 */
import request from './request'
import type {
  Result, PageResult, Article, Category, Tag,
  Message, FriendLink, SiteConfig, SiteStats, ArchiveGroup, User,
  Photo, Music
} from '@/types'

/* ============================ 站点 ============================ */

export function getSiteConfig(): Promise<SiteConfig> {
  return request.get<never, SiteConfig>('/portal/site/config')
}

/** 站点统计：文章数 / 分类数 / 标签数 / 全站累计浏览量（与后台仪表盘同口径） */
export function getSiteStats(): Promise<SiteStats> {
  return request.get<never, SiteStats>('/portal/site/stats')
}

/* ============================ 文章 ============================ */

export interface ArticleQuery {
  page?: number
  size?: number
  categoryId?: number
  tagId?: number
  keyword?: string
}

/** 文章列表（分页；排序固定为置顶优先 + 发布时间倒序） */
export function getArticleList(params: ArticleQuery = {}): Promise<PageResult<Article>> {
  return request.get<never, PageResult<Article>>('/portal/articles', { params })
}

/** 文章详情 */
export function getArticleDetail(id: number | string): Promise<Article> {
  return request.get<never, Article>(`/portal/articles/${id}`)
}

/** 归档 */
export function getArchive(): Promise<ArchiveGroup[]> {
  return request.get<never, ArchiveGroup[]>('/portal/articles/archive')
}

/** 点赞（按登录用户维度，未登录后端返回 401） */
export function likeArticle(id: number): Promise<void> {
  return request.post<never, void>(`/portal/articles/${id}/like`)
}

/** 取消点赞 */
export function unlikeArticle(id: number): Promise<void> {
  return request.delete<never, void>(`/portal/articles/${id}/like`)
}

/** 收藏（登录用户维度，未登录后端返回 401） */
export function collectArticle(id: number): Promise<void> {
  return request.post<never, void>(`/portal/articles/${id}/collect`)
}

/** 取消收藏 */
export function uncollectArticle(id: number): Promise<void> {
  return request.delete<never, void>(`/portal/articles/${id}/collect`)
}

/** 当前用户是否已收藏该文章（未登录返回 false，不报错） */
export function isArticleCollected(id: number | string): Promise<boolean> {
  return request.get<never, boolean>(`/portal/articles/${id}/collected`)
}

/** 我的收藏（登录用户维度，按收藏时间倒序） */
export function getMyCollections(page = 1, size = 10): Promise<PageResult<Article>> {
  return request.get<never, PageResult<Article>>('/portal/articles/me/collections', {
    params: { page, size }
  })
}

/* ============================ 分类与标签 ============================ */

export function getCategories(): Promise<Category[]> {
  return request.get<never, Category[]>('/portal/categories')
}

export function getTags(): Promise<Tag[]> {
  return request.get<never, Tag[]>('/portal/tags')
}

/* ============================ 留言板 ============================ */

export function getMessages(page = 1, size = 20): Promise<PageResult<Message>> {
  return request.get<never, PageResult<Message>>('/portal/messages', { params: { page, size } })
}

export function postMessage(data: {
  content: string
  nickname?: string
  email?: string
}): Promise<void> {
  return request.post<never, void>('/portal/messages', data)
}

/* ============================ 友链 ============================ */

export function getFriendLinks(): Promise<FriendLink[]> {
  return request.get<never, FriendLink[]>('/portal/links')
}

export function applyFriendLink(data: {
  name: string
  url: string
  logo?: string
  description?: string
  email?: string
}): Promise<void> {
  return request.post<never, void>('/portal/links/apply', data)
}

/* ============================ 相册 ============================ */

/** 相册照片（已显示，按排序权重） */
export function getPhotos(): Promise<Photo[]> {
  return request.get<never, Photo[]>('/portal/album')
}

/* ============================ 背景音乐 ============================ */

/** 全站 BGM 歌单（已启用，按排序权重） */
export function getMusicPlaylist(): Promise<Music[]> {
  return request.get<never, Music[]>('/portal/music')
}

/** 上传一首音频并直接加入全站歌单（需登录；曲名缺省用文件名去扩展名） */
export function uploadBgmMusic(file: File, title?: string): Promise<Music> {
  const form = new FormData()
  form.append('file', file)
  if (title) form.append('title', title)
  return request.post<never, Music>('/portal/music/uploads', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/** 用音乐直链加入全站歌单（需登录），返回新曲目 id */
export function addBgmMusic(data: {
  title: string
  artist?: string | null
  url: string
}): Promise<number> {
  return request.post<never, number>('/portal/music', data)
}
