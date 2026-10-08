import { onMounted, onUnmounted, ref } from 'vue'

/**
 * 滚动淡入动效
 * 使用 IntersectionObserver 监听元素进入视口，添加 is-visible 类触发 CSS 过渡
 *
 * 用法：
 *   const container = ref<HTMLElement>()
 *   useScrollReveal(container)
 */
export function useScrollReveal(
  target: { value: HTMLElement | null | undefined },
  options: { threshold?: number; rootMargin?: string; once?: boolean } = {}
) {
  const { threshold = 0.12, rootMargin = '0px 0px -60px 0px', once = true } = options
  let observer: IntersectionObserver | null = null

  const observe = () => {
    const el = target.value
    if (!el) return

    // 滚动渐入动画已按需求全局移除：元素直接显示，不再挂 IntersectionObserver
    el.querySelectorAll<HTMLElement>('.reveal').forEach((item) => item.classList.add('is-visible'))
  }

  onMounted(() => {
    // 等待子组件渲染完成
    requestAnimationFrame(() => requestAnimationFrame(observe))
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })

  return { refresh: observe }
}

/**
 * 滚动进度（0 - 1）
 * 用于文章详情页的顶部阅读进度条
 */
export function useScrollProgress() {
  const progress = ref(0)

  const update = () => {
    const scrollTop = window.scrollY
    const height = document.documentElement.scrollHeight - window.innerHeight
    progress.value = height > 0 ? Math.min(1, scrollTop / height) : 0
  }

  onMounted(() => {
    update()
    window.addEventListener('scroll', update, { passive: true })
    window.addEventListener('resize', update)
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', update)
    window.removeEventListener('resize', update)
  })

  return { progress }
}

/**
 * 监听元素是否滚动出视口（用于导航栏收缩效果）
 */
export function useScrolled(offset = 12) {
  const scrolled = ref(false)

  const update = () => {
    scrolled.value = window.scrollY > offset
  }

  onMounted(() => {
    update()
    window.addEventListener('scroll', update, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', update)
  })

  return { scrolled }
}
