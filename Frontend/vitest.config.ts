import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'

/**
 * Cấu hình test riêng (không dùng chung vite.config.ts) để tránh nạp plugin Tailwind/React dev
 * server không cần cho test thuần logic — chỉ cần alias "@" giống vite.config.ts.
 * Test hiện có: kênh postMessage AI Panel (OHIF) ↔ ReportModal (`src/features/reports/integration`).
 */
export default defineConfig({
  test: {
    environment: 'jsdom',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
