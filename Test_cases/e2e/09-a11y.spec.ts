import { expect, test } from '@playwright/test'
import { login, selectRowWithStatus } from '../fixtures/login'

/**
 * Khoa lai cac ban sua a11y/UX tim ra trong dot QA 2026-11-26.
 * Khong co test nay thi lan refactor sau rat de xoa mat chung.
 */
test.describe('A11Y-01: hop thoai duoc khai bao dung', () => {
  test('Modal co role=dialog, aria-modal va duoc dat ten theo tieu de', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
    await page.getByRole('button', { name: 'Hủy ca', exact: true }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog).toHaveAttribute('aria-modal', 'true')
    // aria-labelledby phai tro tới dung tieu de hop thoai.
    const labelId = await dialog.getAttribute('aria-labelledby')
    expect(labelId).toBeTruthy()
    await expect(page.locator(`#${labelId}`)).toHaveText('Hủy ca')
  })

  test('nut dong hop thoai co nhan doc duoc', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
    await page.getByRole('button', { name: 'Hủy ca', exact: true }).click()
    const close = page.getByRole('dialog').getByRole('button', { name: 'Đóng hộp thoại' })
    await expect(close).toBeVisible()
    await close.click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })

  test('Esc dong duoc hop thoai', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
    await page.getByRole('button', { name: 'Hủy ca', exact: true }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })

  test('Tab khong di ra ngoai hop thoai (bay focus)', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
    await page.getByRole('button', { name: 'Hủy ca', exact: true }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    for (let i = 0; i < 12; i++) {
      await page.keyboard.press('Tab')
      const inside = await dialog.evaluate(d => d.contains(document.activeElement))
      expect(inside, `sau ${i + 1} lan Tab, focus phai con trong hop thoai`).toBe(true)
    }
  })
})

test.describe('A11Y-03: trang thai chon khong chi bang mau', () => {
  test('hai tab co role=tab va aria-selected dung', async ({ page }) => {
    await login(page, 'ktv')
    const pending = page.getByRole('tab', { name: /^Chờ \(/ })
    const done = page.getByRole('tab', { name: 'Đã thực hiện', exact: true })
    await expect(pending).toHaveAttribute('aria-selected', 'true')
    await expect(done).toHaveAttribute('aria-selected', 'false')

    await done.click()
    await expect(done).toHaveAttribute('aria-selected', 'true')
    await expect(pending).toHaveAttribute('aria-selected', 'false')
  })

  test('hang dang chon co aria-selected', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
    await expect(page.getByRole('row', { selected: true })).toHaveCount(1)
  })
})

test.describe('UX-01: bat buoc ly do co thong bao loi doc duoc', () => {
  test('bo trong o ly do roi roi khoi o -> hien loi dang chu, khong chi tooltip', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
    await page.getByRole('button', { name: 'Hủy ca', exact: true }).click()

    const dialog = page.getByRole('dialog')
    const reason = dialog.getByPlaceholder(/^Ví dụ: bệnh nhân đã xuất viện/)

    // Luc moi mo: chua cham vao o thi chua bao loi, chi hien goi y.
    await expect(dialog.getByText('Thao tác này bắt buộc nhập lý do.')).toHaveCount(0)
    await expect(reason).toHaveAttribute('aria-describedby', 'transition-reason-hint')

    // Cham vao roi de trong -> loi hien ra dang CHU, co role=alert.
    await reason.click()
    await dialog.getByRole('button', { name: 'Hủy bỏ', exact: true }).focus()
    const err = dialog.getByRole('alert')
    await expect(err).toBeVisible()
    await expect(err).toHaveText('Thao tác này bắt buộc nhập lý do.')
    await expect(reason).toHaveAttribute('aria-invalid', 'true')
    await expect(reason).toHaveAttribute('aria-describedby', 'transition-reason-error')

    // Nhap ly do -> loi mat, quay ve goi y.
    await reason.fill('Bệnh nhân đã xuất viện')
    await expect(dialog.getByRole('alert')).toHaveCount(0)
    await expect(reason).toHaveAttribute('aria-describedby', 'transition-reason-hint')
  })
})

test.describe('L1: form chan ngay sinh o tuong lai', () => {
  test('o ngay sinh cua modal tao ca co thuoc tinh max = hom nay', async ({ page }) => {
    await login(page, 'ktv')
    await page.getByTitle('Thêm bệnh nhân').click()
    const dob = page.getByRole('dialog').getByLabel('Ngày sinh', { exact: true })
    const max = await dob.getAttribute('max')
    expect(max, 'thieu thuoc tinh max -> loi L1 tro lai').toBeTruthy()
    // max phai la hom nay dang YYYY-MM-DD
    expect(max).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    const today = new Date()
    const expected = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
    expect(max).toBe(expected)
  })
})
