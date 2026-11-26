import { expect, test } from '@playwright/test'
import { login, shot } from '../fixtures/login'

/**
 * TC-16 (`doc/12` muc 3.1): worklist phai dem du 7 nhom trang thai, ke ca `CANCELLED`.
 *
 * Phan bo giao dien: `SCHEDULED` la tab "Cho" (co so dem tren tab); 6 trang thai con lai la
 * checkbox tren thanh loc, moi cai kem so dem.
 */
const DONE_LABELS = ['Chưa đọc', 'Đang đọc', 'Đọc xong', 'Đang duyệt', 'Duyệt xong', 'Đã hủy']

test.describe('TC-16: dem du 7 nhom trang thai', () => {
  test('tab "Cho" hien so dem SCHEDULED', async ({ page }) => {
    await login(page, 'ktv')
    const tab = page.getByRole('button', { name: /^Chờ \(/ })
    await expect(tab).toBeVisible()
    expect(await tab.innerText()).toMatch(/^Chờ \((\d+(\.\d+)?k?)\)$/)
  })

  test('thanh loc co du 6 trang thai cua tab "Da thuc hien" + checkbox "Tất cả"', async ({ page }) => {
    await login(page, 'ktv')
    await page.getByRole('button', { name: 'Đã thực hiện', exact: true }).click()
    for (const label of DONE_LABELS) {
      await expect(
        page.getByRole('checkbox', { name: new RegExp(`^${label}`) }),
        `thieu checkbox trang thai "${label}"`,
      ).toHaveCount(1)
    }
    await expect(page.getByRole('checkbox', { name: /^Tất cả/ })).toHaveCount(1)
    await shot(page, 'tc16-thanh-loc-trang-thai')
  })

  test('moi trang thai hien so dem dang (n)', async ({ page }) => {
    await login(page, 'ktv')
    await page.getByRole('button', { name: 'Đã thực hiện', exact: true }).click()
    for (const label of DONE_LABELS) {
      const row = page.locator('label').filter({ hasText: new RegExp(`^${label}\\s*\\(`) }).first()
      expect(await row.innerText(), `so dem cua "${label}"`).toMatch(/\((\d+(\.\d+)?k?)\)/)
    }
  })

  test('TC-16 [HAN CHE DU LIEU]: mock seed khong co ca CANCELLED nao', async ({ page }) => {
    // Ghi lai han che da biet, KHONG phai loi code: `mock/seed.ts` co chu y bo ca CANCELLED
    // (comment dong 8) vi khi viet seed thi `CANCELLED` con la de xuat. Nay trang thai nay da
    // duoc hien thuc day du (types, status.ts, T10, StudyTransitionDialog) nen seed lech.
    // Hau qua: bo loc "Đã hủy" ton tai nhung khong bao gio ra hang nao o che do mock, va TC-16
    // KHONG kiem chung duoc day du bang mock. Xem reports/bug-report.md ma DATA-01.
    await login(page, 'ktv')
    await page.getByRole('button', { name: 'Đã thực hiện', exact: true }).click()
    const cancelled = page.locator('label').filter({ hasText: /^Đã hủy\s*\(/ }).first()
    const text = await cancelled.innerText()
    // innerText co dau xuong dong giua nhan va so dem -> chuan hoa ve mot space.
    expect(text.replace(/\s+/g, ' ').trim()).toBe('Đã hủy (0)')
    await shot(page, 'data01-da-huy-dem-0')
  })
})
