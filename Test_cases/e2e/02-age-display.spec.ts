import { expect, test } from '@playwright/test'
import { column, login, mainTable, shot } from '../fixtures/login'

/**
 * TC-09 (`doc/12` muc 3.1) + `doc/02-erd.md` muc 4.2 + `01-scope.md` muc 6 buoc 10.
 *
 * Yeu cau: tuoi xap xi co tien to `~`, chua co ngay sinh thi `—`, va KHONG BAO GIO ra "0".
 * Cot "Tuổi" chi ton tai o tab "Cho".
 */
test.describe('TC-09: cot Tuoi tren worklist', () => {
  test('cot "Tuổi" ton tai o tab Cho va khong o nao hien dung "0"', async ({ page }) => {
    await login(page, 'ktv')
    await expect(column(page, 'Tuổi')).toBeVisible()

    // Cho bang nap xong: luc dau bang hien "Đang tải…" va chua co hang nao.
    await expect(mainTable(page).locator('tbody tr').first()).toBeVisible()

    const colIndex = await mainTable(page).locator('thead th').evaluateAll(
      ths => ths.findIndex(th => th.textContent?.trim() === 'Tuổi'))
    expect(colIndex).toBeGreaterThan(0)

    const cells = mainTable(page).locator(`tbody tr td:nth-child(${colIndex + 1})`)
    const n = await cells.count()
    expect(n).toBeGreaterThan(0)

    for (let i = 0; i < n; i++) {
      const txt = (await cells.nth(i).innerText()).trim()
      // Chot TC-09: moi o phai la so, "~" + so, hoac em dash. Tuyet doi khong phai "0".
      expect(txt, `o Tuoi hang ${i + 1} khong duoc la "0"`).not.toBe('0')
      expect(txt, `o Tuoi hang ${i + 1} co dang la: ${txt}`).toMatch(/^(~?\d+|—)$/)
    }
    await shot(page, 'tc09-cot-tuoi')
  })

  test('tuoi xap xi co tien to ~ va tooltip giai thich (qua panel)', async ({ page }) => {
    // Cot "Tuổi" chi co o tab "Cho", nhung 3 ca SCHEDULED cua seed deu thuoc benh nhan co ngay
    // sinh DAY. Cac benh nhan xap xi (TEST0002/TEST0003 = YEAR/MONTH, TEST0008 = YEAR) nam o tab
    // "Da thuc hien". Panel ben phai cung dung `formatAge` nen kiem o day la kiem dung ham do.
    await login(page, 'ktv')
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()

    // Tim benh nhan TEST0008 (dob_precision = YEAR) bang o tim kiem tren thanh tren.
    await page.getByPlaceholder('Mã bệnh nhân').first().fill('TEST0008')
    await page.keyboard.press('Enter')
    await expect(mainTable(page).locator('tbody tr').first()).toBeVisible()
    await mainTable(page).locator('tbody tr').first().click()

    // Panel hien Tuoi dang "~NN" kem tooltip giai thich vi sao xap xi.
    const approx = page.getByTitle('Xấp xỉ vì ngày sinh chỉ có năm hoặc tháng').first()
    await expect(approx).toBeVisible()
    expect((await approx.innerText()).trim()).toMatch(/^~\d+$/)
    await shot(page, 'tc09-tuoi-xap-xi-panel')
  })

  test('benh nhan ngay sinh du (DAY) khong co tien to ~', async ({ page }) => {
    await login(page, 'ktv')
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
    await page.getByPlaceholder('Mã bệnh nhân').first().fill('TEST0001')
    await page.keyboard.press('Enter')
    await expect(mainTable(page).locator('tbody tr').first()).toBeVisible()
    await mainTable(page).locator('tbody tr').first().click()
    // Khong duoc co tooltip xap xi cho benh nhan co ngay sinh day du.
    await expect(page.getByTitle('Xấp xỉ vì ngày sinh chỉ có năm hoặc tháng')).toHaveCount(0)
  })
})
