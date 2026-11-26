import { expect, test } from '@playwright/test'
import { ACCOUNTS, DEMO_PASSWORD } from '../fixtures/accounts'
import { column, login, shot } from '../fixtures/login'

/** TC-01 (`doc/12` muc 3.1) + US-KTV-09 + RBAC-01/RBAC-02. */
test.describe('TC-01 / US-KTV-09: dang nhap 7 tai khoan seed', () => {
  for (const acc of Object.values(ACCOUNTS)) {
    test(`${acc.username} (${acc.role}) dang nhap duoc va hien dung tai khoan`, async ({ page }) => {
      await login(page, acc.username)
      // TopBar hien username dang text (khong co data-testid trong toan bo frontend).
      await expect(page.getByText(acc.username, { exact: true }).first()).toBeVisible()
      await expect(column(page, 'STT')).toBeVisible()
    })
  }

  test('US-KTV-09 [401]: sai mat khau bi tu choi, khong vao duoc worklist', async ({ page }) => {
    await page.goto('/')
    await page.getByPlaceholder('Nhập tên đăng nhập').fill('ktv')
    await page.getByPlaceholder('Nhập mật khẩu').fill('mat-khau-sai')
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page.getByPlaceholder('Nhập tên đăng nhập')).toBeVisible()
    await expect(column(page, 'STT')).toHaveCount(0)
    await shot(page, 'tc01-sai-mat-khau')
  })

  test('US-KTV-09 [401]: tai khoan khong ton tai bi tu choi', async ({ page }) => {
    await page.goto('/')
    await page.getByPlaceholder('Nhập tên đăng nhập').fill('khong-ton-tai')
    await page.getByPlaceholder('Nhập mật khẩu').fill(DEMO_PASSWORD)
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page.getByPlaceholder('Nhập tên đăng nhập')).toBeVisible()
  })

  test('RBAC-01: chua dang nhap thi khong vao duoc worklist', async ({ page }) => {
    await page.goto('/')
    // RequireAuth chan: man dang nhap, khong co bang worklist.
    await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible()
    await expect(column(page, 'STT')).toHaveCount(0)
  })

  test('dang xuat tra ve man dang nhap', async ({ page }) => {
    await login(page, ACCOUNTS.ktv.username)
    await page.getByText(ACCOUNTS.ktv.username, { exact: true }).first().click()
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click()
    await expect(page.getByPlaceholder('Nhập tên đăng nhập')).toBeVisible()
  })
})

/**
 * Tab mac dinh theo vai tro (`WorklistPage.tsx`, yeu cau T30 cua `doc/06b` muc 2):
 * RADIOLOGIST va APPROVER duoc chuyen san sang tab "Da thuc hien" voi tap trang thai rieng.
 */
test.describe('Tab mac dinh theo vai tro', () => {
  test('KTV mo o tab "Cho"', async ({ page }) => {
    await login(page, ACCOUNTS.ktv.username)
    // Tab Cho dung cot rieng: Ma benh nhan / Tuoi chi co o tab nay.
    await expect(column(page, 'Mã bệnh nhân')).toBeVisible()
    await expect(column(page, 'Tuổi')).toBeVisible()
  })

  test('RADIOLOGIST mo san o tab "Da thuc hien"', async ({ page }) => {
    await login(page, ACCOUNTS.bscdha.username)
    await expect(column(page, 'Trạng thái')).toBeVisible()
    await expect(column(page, 'Tuổi')).toHaveCount(0)
    await shot(page, 'tab-mac-dinh-radiologist')
  })

  test('APPROVER mo san o tab "Da thuc hien"', async ({ page }) => {
    await login(page, ACCOUNTS.bsduyet.username)
    await expect(column(page, 'Trạng thái')).toBeVisible()
  })
})
