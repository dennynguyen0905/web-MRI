import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'

/**
 * Unit/component test cua bo kiem thu chot cuoi.
 *
 * Alias `@` tro sang `../Frontend/src` (GIONG `Frontend/vite.config.ts` va
 * `Frontend/vitest.config.ts`) de test o day import duoc ma nguon that bang cung duong dan
 * `@/shared/lib/...` nhu code san pham dung. Nho vay khi copy mot test tu `Frontend/src` sang
 * day (hoac nguoc lai) khong phai sua import.
 */
export default defineConfig({
  test: {
    environment: 'jsdom',
    include: ['unit/**/*.spec.ts', 'unit/**/*.spec.tsx'],
    setupFiles: ['./fixtures/setup.ts'],
    globals: true,
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('../Frontend/src', import.meta.url)),
    },
  },
})
