import { expect, test } from '@playwright/test'
import { ACCOUNTS } from '../fixtures/accounts'
import { login, shot } from '../fixtures/login'

/** Smoke: app len duoc, dang nhap duoc, worklist hien du. Fail o day thi moi test sau vo nghia. */
test.describe('Smoke', () => {
  test('man dang nhap render duoc', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible()
    await expect(page.getByPlaceholder('Nhập tên đăng nhập')).toBeVisible()
    await shot(page, 'smoke-login')
  })

  test('dang nhap sai mat khau bi tu choi', async ({ page }) => {
    await page.goto('/')
    await page.getByPlaceholder('Nhập tên đăng nhập').fill('ktv')
    await page.getByPlaceholder('Nhập mật khẩu').fill('sai-mat-khau')
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page.getByPlaceholder('Nhập tên đăng nhập')).toBeVisible()
    await shot(page, 'smoke-login-sai-mat-khau')
  })

  test('dang nhap KTV vao duoc worklist', async ({ page }) => {
    await login(page, ACCOUNTS.ktv.username)
    // Worklist co 2 bang: bang chinh (STT/Ma benh nhan/...) va bang phieu lien quan o panel.
    await expect(page.getByRole('table').first()).toBeVisible()
    await expect(page.getByRole('columnheader', { name: 'Mã bệnh nhân' })).toBeVisible()
    await shot(page, 'smoke-worklist-ktv')
  })
})
