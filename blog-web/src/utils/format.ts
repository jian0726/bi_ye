import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/** 格式化日期：2026-09-15 */
export function formatDate(date: string | Date | null | undefined): string {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD')
}

/** 格式化日期时间：2026-09-15 10:30 */
export function formatDateTime(date: string | Date | null | undefined): string {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

/** 相对时间：3 天前 */
export function fromNow(date: string | Date | null | undefined): string {
  if (!date) return ''
  return dayjs(date).fromNow()
}

/** 数字缩写：2847 → 2.8k */
export function formatCount(num: number | null | undefined): string {
  if (num == null) return '0'
  if (num < 1000) return String(num)
  if (num < 10000) return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return (num / 10000).toFixed(1).replace(/\.0$/, '') + 'w'
}

/** 估算阅读时长 */
export function estimateReadMinutes(text: string): number {
  const count = text.length
  return Math.max(1, Math.ceil(count / 400))
}

/** 从 Markdown 中提取纯文本用于计算字数与摘要 */
export function stripMarkdown(md: string): string {
  return md
    .replace(/```[\s\S]*?```/g, '')
    .replace(/`[^`]*`/g, '')
    .replace(/!\[.*?\]\(.*?\)/g, '')
    .replace(/\[(.*?)\]\(.*?\)/g, '$1')
    .replace(/[#>*_~\-]+/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

/** 生成昵称首字母/首字（无头像时使用） */
export function nameInitial(name: string | null | undefined): string {
  if (!name) return '?'
  return name.trim().charAt(0).toUpperCase()
}

/** 根据字符串生成稳定的柔和背景色（头像占位） */
export function stringToColor(str: string | null | undefined): string {
  if (!str) return 'hsl(210, 12%, 62%)'
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  const hue = Math.abs(hash) % 360
  return `hsl(${hue}, 42%, 56%)`
}

/** 防抖 */
export function debounce<T extends (...args: never[]) => void>(fn: T, wait = 300) {
  let timer: ReturnType<typeof setTimeout> | null = null
  return (...args: Parameters<T>) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), wait)
  }
}

/** 滚动到页面顶部 */
export function scrollToTop(smooth = true) {
  window.scrollTo({ top: 0, behavior: smooth ? 'smooth' : 'auto' })
}
