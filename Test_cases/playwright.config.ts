import { defineConfig, devices } from '@playwright/test'

/**
 * E2E chot cuoi truoc deploy.
 *
 * Chay tren MOCK MODE cua Frontend (`VITE_API_MODE` khong dat -> mac dinh 'mock', xem
 * `Frontend/src/shared/api/client.ts`). Nho vay toan bo luong nghiep vu kiem duoc ma KHONG can
 * backend, MySQL hay Docker — quan trong vi moi truong CI/container thuong khong co Docker.
 *
 * Muon chay that vao backend: dat VITE_API_MODE=live va tro VITE_API_BASE_URL sang backend,
 * roi chay lai. Khi do cac test phu thuoc du lieu seed V7/V8 moi dung.
 */
const PORT = 5174

export default defineConfig({
  testDir: './e2e',
  outputDir: './reports/test-results',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { outputFolder: './reports/playwright-report', open: 'never' }],
    ['json', { outputFile: './reports/e2e-results.json' }],
  ],
  timeout: 60_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: `http://127.0.0.1:${PORT}`,
    trace: 'off',
    screenshot: 'only-on-failure',
    video: 'off',
    viewport: { width: 1600, height: 950 },
    launchOptions: { args: ['--no-sandbox', '--disable-dev-shm-usage'] },
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    // `--host 127.0.0.1` LA BAT BUOC: Vite mac dinh bind vao `localhost`, tren runner CI thuong
    // phan giai ra IPv6 (::1), trong khi Playwright do `url` o IPv4 (127.0.0.1) -> cho mai khong
    // thay va bao "Timed out waiting ... from config.webServer". Da gap that tren GitHub Actions.
    command: `npm run dev -- --port ${PORT} --strictPort --host 127.0.0.1`,
    cwd: '../Frontend',
    url: `http://127.0.0.1:${PORT}`,
    // Tren CI luon dung server moi; o may ca nhan thi dung lai cho nhanh.
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
    // 'pipe' de khi server khong len thi log cua Vite hien ra trong CI, khong bi nuot.
    stdout: 'pipe',
    stderr: 'pipe',
  },
})
