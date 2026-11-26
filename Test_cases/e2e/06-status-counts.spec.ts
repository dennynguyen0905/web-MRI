import { expect, test } from '@playwright/test'
import { login, mainTable, selectRowWithStatus, shot } from '../fixtures/login'

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
    const tab = page.getByRole('tab', { name: /^Chờ \(/ })
    await expect(tab).toBeVisible()
    expect(await tab.innerText()).toMatch(/^Chờ \((\d+(\.\d+)?k?)\)$/)
  })

  test('thanh loc co du 6 trang thai cua tab "Da thuc hien" + checkbox "Tất cả"', async ({ page }) => {
    await login(page, 'ktv')
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
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
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
    for (const label of DONE_LABELS) {
      const row = page.locator('label').filter({ hasText: new RegExp(`^${label}\\s*\\(`) }).first()
      expect(await row.innerText(), `so dem cua "${label}"`).toMatch(/\((\d+(\.\d+)?k?)\)/)
    }
  })

  test('TC-16: trang thai CANCELLED co du lieu that -> dem du 7/7 nhom', async ({ page }) => {
    // Truoc 2026-11-26, `mock/seed.ts` khong co ca CANCELLED nao (loi DATA-01), nen bo loc
    // "Đã hủy" luon rong va TC-16 chi kiem duoc 6/7 nhom. Nay seed da co ca id=12.
    await login(page, 'ktv')
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
    const cancelled = page.locator('label').filter({ hasText: /^Đã hủy\s*\(/ }).first()
    // Bo dem nap bang mot loi goi API rieng (`useStatusCounts`) nen phai cho — doc innerText
    // mot lan la test giong, de do oan.
    await expect(cancelled, 'bo loc "Đã hủy" phai dem duoc it nhat 1 ca')
      .toHaveText(/Đã hủy\s*\([1-9]\d*\)/)
    await shot(page, 'tc16-da-huy-co-du-lieu')
  })

  test('TC-16: loc "Đã hủy" ra dung hang, ca da huy van tra cuu duoc', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Đã hủy')).toBe(true)
    // Ca da huy KHONG duoc bien mat khoi giao dien (`doc/12` TC-16).
    await expect(mainTable(page).locator('tbody tr')).not.toHaveCount(0)
    await expect(mainTable(page).getByText('Đã hủy').first()).toBeVisible()
    await shot(page, 'tc16-loc-da-huy')
  })
})
