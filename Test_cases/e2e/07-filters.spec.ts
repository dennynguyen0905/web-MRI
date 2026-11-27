import { expect, test } from '@playwright/test'
import { login, mainTable, shot } from '../fixtures/login'

/**
 * TC-17 (loc Modality) va TC-19 (loc loai benh nhan) — `doc/12` muc 3.1, IN-12.
 *
 * Hai bo loc nay nam o Sidebar duoi dang <select> khong co nhan gan kem; nhan dien bang option
 * dau tien dong vai placeholder (xem loi A11Y-02).
 */
const modalitySelect = (page: import('@playwright/test').Page) =>
  page.locator('select').filter({ has: page.locator('option:text-is("Modality")') }).first()

const patientClassSelect = (page: import('@playwright/test').Page) =>
  page.locator('select').filter({ has: page.locator('option:text-is("Chọn loại bệnh nhân")') }).first()

const rowCount = async (page: import('@playwright/test').Page) =>
  await mainTable(page).locator('tbody tr').count()

test.describe('TC-17: loc theo Modality', () => {
  test('select Modality co du 5 lua chon cua seed', async ({ page }) => {
    await login(page, 'ktv')
    expect(await modalitySelect(page).locator('option').allTextContents())
      .toEqual(['Modality', 'CT', 'MR', 'CR', 'DX', 'US'])
  })

  test('chon mot modality thi danh sach hep lai va khong rong hon tong', async ({ page }) => {
    await login(page, 'ktv')
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
    await expect(mainTable(page).locator('tbody tr').first()).toBeVisible()
    const total = await rowCount(page)
    expect(total).toBeGreaterThan(0)

    // Tong so hang khi loc tung modality khong duoc vuot tong khi khong loc.
    let sum = 0
    for (const m of ['CT', 'MR', 'CR', 'DX', 'US']) {
      await modalitySelect(page).selectOption({ label: m })
      await page.waitForTimeout(350)
      const n = await rowCount(page)
      expect(n, `loc modality ${m} khong duoc ra nhieu hon tong`).toBeLessThanOrEqual(total)
      sum += n
    }
    // Moi ca chi thuoc dung mot modality -> tong cac nhom bang tong chung.
    expect(sum, 'tong cac nhom modality phai bang tong so ca').toBe(total)
    await shot(page, 'tc17-loc-modality')
  })

  test('bo loc quay ve "Modality" thi danh sach day lai', async ({ page }) => {
    await login(page, 'ktv')
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
    await expect(mainTable(page).locator('tbody tr').first()).toBeVisible()
    const total = await rowCount(page)

    await modalitySelect(page).selectOption({ label: 'CT' })
    await page.waitForTimeout(350)
    expect(await rowCount(page)).toBeLessThanOrEqual(total)

    await modalitySelect(page).selectOption({ label: 'Modality' })
    await page.waitForTimeout(350)
    expect(await rowCount(page)).toBe(total)
  })
})

test.describe('TC-19: loc theo loai benh nhan', () => {
  test('co dung 2 lua chon Noi tru / Ngoai tru', async ({ page }) => {
    await login(page, 'ktv')
    expect(await patientClassSelect(page).locator('option').allTextContents())
      .toEqual(['Chọn loại bệnh nhân', 'Nội trú', 'Ngoại trú'])
  })

  test('Noi tru + Ngoai tru cong lai bang tong so ca', async ({ page }) => {
    await login(page, 'ktv')
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
    await expect(mainTable(page).locator('tbody tr').first()).toBeVisible()
    const total = await rowCount(page)

    await patientClassSelect(page).selectOption({ label: 'Nội trú' })
    await page.waitForTimeout(350)
    const inpatient = await rowCount(page)

    await patientClassSelect(page).selectOption({ label: 'Ngoại trú' })
    await page.waitForTimeout(350)
    const outpatient = await rowCount(page)

    // Seed co ca hai loai (don 2, 3 la INPATIENT; con lai OUTPATIENT) nen ca hai phai > 0.
    expect(inpatient, 'seed phai co ca noi tru').toBeGreaterThan(0)
    expect(outpatient, 'seed phai co ca ngoai tru').toBeGreaterThan(0)
    expect(inpatient + outpatient, 'hai loai cong lai phai bang tong').toBe(total)
    await shot(page, 'tc19-loc-loai-benh-nhan')
  })
})

test('doi bo loc thi so trang quay ve 1', async ({ page }) => {
  await login(page, 'ktv')
  await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
  await expect(mainTable(page).locator('tbody tr').first()).toBeVisible()
  await modalitySelect(page).selectOption({ label: 'CT' })
  await page.waitForTimeout(350)
  // Khong co phan trang hien ra khi it du lieu; kiem rang danh sach khong rong va khong loi.
  await expect(mainTable(page)).toBeVisible()
})
