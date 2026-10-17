import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 0.0.0.0 才能被 ngrok 等隧道工具从外部连入（127.0.0.1 仅本机可访问）
    host: '0.0.0.0',
    // 放行隧道域名（ngrok 免费版每次重启都换域名，用 true 免维护）
    allowedHosts: true,
    proxy: {
      '/api': {
        // 默认打本地后端；联调/验证时可 VITE_PROXY_TARGET=http://127.0.0.1:8090 覆盖
        target: process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // 对象存储：后端 MINIO_ACCESS_URL=/files（相对路径），
      // Docker 形态由 nginx 反代，本地开发由 vite 反代到 MinIO
      '/files': {
        target: process.env.VITE_MINIO_TARGET || 'http://127.0.0.1:9000',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/files/, '')
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          markdown: ['markdown-it'],
          highlight: ['highlight.js']
        }
      }
    }
  }
})
