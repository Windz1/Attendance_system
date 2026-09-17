import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 生产环境由 Nginx 挂载在 http(s)://host/attendance/ 下；开发环境仍从根路径启动。
  base: process.env.NODE_ENV === 'production' ? '/attendance/' : '/',
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
