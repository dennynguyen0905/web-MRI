import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

/**
 * Base URL của API là đường dẫn tương đối `/api/v1` cùng origin (doc/14 mục 3.2):
 * trong k8s và Compose, nginx của ris-frontend proxy `/api/` sang backend nên không cần CORS.
 * Khi chạy `npm run dev`, proxy dưới đây thay vai trò của nginx.
 */
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_DEV_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
