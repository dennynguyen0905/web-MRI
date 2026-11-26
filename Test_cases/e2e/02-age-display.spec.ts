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

  test('tuoi xap xi co tien to ~ va co tooltip giai thich', async ({ page }) => {
    await login(page, 'ktv')
    await expect(mainTable(page).locator('tbody tr').first()).toBeVisible()
    const approx = mainTable(page).getByTitle('Tuổi xấp xỉ vì ngày sinh chỉ có năm hoặc tháng')
    const count = await approx.count()
    test.skip(count === 0, 'seed hien khong co benh nhan dob_precision != DAY o tab Cho')
    for (let i = 0; i < count; i++) {
      expect((await approx.nth(i).innerText()).trim()).toMatch(/^~\d+$/)
    }
  })
})
