import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(process.cwd(), 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.CLUB_BACKEND_URL || 'http://localhost:8080/club-enroll',
        changeOrigin: true,
        cookieDomainRewrite: 'localhost',
        // 后端 context-path 是 /club-enroll，Tomcat 的 cookie Path 与之相同
        // 但浏览器看到的请求路径是 /api/...,Path 不匹配 → 下次请求不带 cookie → 一直 401
        // 改写成 / 让所有路径都能带上 JSESSIONID
        cookiePathRewrite: '/',
      },
    },
  },
})
