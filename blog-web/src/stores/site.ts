import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SiteConfig } from '@/types'
import { getSiteConfig } from '@/api'

/** 站点配置（全站共享，仅加载一次） */
export const useSiteStore = defineStore('site', () => {
  const config = ref<SiteConfig | null>(null)
  const loaded = ref(false)
  const loading = ref(false)

  async function loadConfig(force = false) {
    if ((loaded.value && !force) || loading.value) return config.value

    loading.value = true
    try {
      config.value = await getSiteConfig()
      loaded.value = true
      // 同步页面标题与 meta
      if (config.value) {
        document.title = `${config.value.siteName} · ${config.value.siteSubtitle}`
      }
    } catch (err) {
      console.error('[site] 加载站点配置失败', err)
    } finally {
      loading.value = false
    }
    return config.value
  }

  return { config, loaded, loading, loadConfig }
})
