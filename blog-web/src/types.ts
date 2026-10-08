/**
 * 全局类型定义
 */

/** 统一响应体 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页响应体 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

/** 用户 */
export interface User {
  id: number
  /** 手机号（与邮箱至少一项，唯一，可用于登录） */
  phone?: string | null
  /** 邮箱（与手机号至少一项，唯一，可用于登录） */
  email?: string | null
  nickname: string
  avatar?: string | null
  /** 留言页自定义背景图（null = 默认夜空） */
  messageBg?: string | null
  bio?: string | null
  gender: 0 | 1 | 2
  role: 'USER' | 'ADMIN'
  status: 0 | 1
  createTime: string
}

/** 分类 */
export interface Category {
  id: number
  name: string
  slug?: string | null
  description?: string | null
  icon?: string | null
  sortOrder: number
  articleCount: number
}

/** 标签 */
export interface Tag {
  id: number
  name: string
  slug?: string | null
  color?: string | null
  articleCount: number
}

/** 文章 */
export interface Article {
  id: number
  title: string
  summary: string
  content?: string
  contentHtml?: string
  cover?: string | null
  categoryId: number | null
  category?: Category | null
  tags: Tag[]
  author: Pick<User, 'id' | 'nickname' | 'avatar'>
  status: 0 | 1 | 2
  isTop: boolean
  isRecommend: boolean
  viewCount: number
  likeCount: number
  collectCount: number
  /** 当前登录用户是否已点赞（未登录恒为 false） */
  liked?: boolean
  /** 当前登录用户是否已收藏（未登录恒为 false） */
  collected?: boolean
  wordCount: number
  readMinutes: number
  publishTime: string | null
  createTime: string
}

/** 留言 */
export interface Message {
  id: number
  user?: Pick<User, 'id' | 'nickname' | 'avatar'> | null
  nickname?: string | null
  email?: string | null
  content: string
  ipLocation?: string | null
  createTime: string
}

/** 友链 */
export interface FriendLink {
  id: number
  name: string
  url: string
  logo?: string | null
  description?: string | null
}

/** 站点配置 */
export interface SiteConfig {
  siteName: string
  siteSubtitle: string
  siteLogo: string
  siteDescription: string
  siteKeywords: string
  siteAuthor: string
  authorAvatar: string
  authorBio: string
  icpNumber: string
  policeNumber: string
  copyright: string
  githubUrl: string
  emailAddress: string
  aboutContent: string
}

/** 站点统计（刊底与关于页展示，与后台仪表盘同口径） */
export interface SiteStats {
  /** 已发布文章数 */
  articleCount: number
  categoryCount: number
  tagCount: number
  /** 全站累计浏览量（SUM(t_article.view_count)） */
  viewCount: number
}

/** 相册照片（url 空 = 渐变占位） */
export interface Photo {
  id: number
  title: string
  url?: string | null
  location?: string | null
  /** yyyy-MM-dd */
  takenDate?: string | null
  /** 宽高比，如 3/4 */
  ratio: string
  tone?: string | null
  emoji?: string | null
  sortOrder?: number
  /** 1显示 2隐藏（管理端用） */
  status?: number
}

/** 背景音乐曲目 */
export interface Music {
  id: number
  title: string
  artist?: string | null
  url: string
  sortOrder?: number
  /** 1启用 2停用（管理端用） */
  status?: number
}

/** 归档分组 */
export interface ArchiveGroup {
  year: number
  count: number
  articles: Pick<Article, 'id' | 'title' | 'createTime' | 'publishTime'>[]
}
