import { ref } from 'vue'

type Theme = 'light' | 'dark'

const STORAGE_KEY = 'theme'

/** 当前主题（模块级单例，全站共享） */
const theme = ref<Theme>('light')

/** 从 DOM 读取初始主题（index.html 中已提前应用，避免闪烁） */
function readInitialTheme(): Theme {
  if (typeof document === 'undefined') return 'light'
  return document.documentElement.classList.contains('dark') ? 'dark' : 'light'
}

theme.value = readInitialTheme()

/** 应用主题到 DOM */
function apply(next: Theme) {
  const root = document.documentElement
  root.classList.remove('light', 'dark')
  root.classList.add(next)
  localStorage.setItem(STORAGE_KEY, next)

  // 同步更新浏览器地址栏颜色
  const meta = document.querySelector('meta[name="theme-color"]:not([media])')
  if (meta) {
    meta.setAttribute('content', next === 'dark' ? '#000000' : '#ffffff')
  }
}

export function useTheme() {
  /** 切换主题 */
  const toggleTheme = () => {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    apply(theme.value)
  }

  /** 设置指定主题 */
  const setTheme = (next: Theme) => {
    theme.value = next
    apply(next)
  }

  /** 跟随系统 */
  const followSystem = () => {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    setTheme(prefersDark ? 'dark' : 'light')
  }

  return { theme, toggleTheme, setTheme, followSystem }
}
