import { expect, test } from '@playwright/test'
import { login, shot } from '../fixtures/login'

/**
 * TC-02 / US-KTV-01 / US-KTV-04 / US-BSLS-01: tao benh nhan + chi dinh + ca chup.
 *
 * Trong so nay co bai dieu tra loi L1: form co chan NGAY SINH O TUONG LAI hay khong.
 */
async function openAddStudy(page: import('@playwright/test').Page) {
  await page.getByTitle('Thêm bệnh nhân').click()
  await expect(page.getByText('THÊM CA CHỤP', { exact: true })).toBeVisible()
}

test.describe('TC-02: modal "THÊM CA CHỤP"', () => {
  test('mo duoc, mac dinh tick "Thêm mới bệnh nhân", liet ke truong con thieu', async ({ page }) => {
    await login(page, 'ktv')
    await openAddStudy(page)
    await expect(page.getByRole('checkbox', { name: 'Thêm mới bệnh nhân' })).toBeChecked()
    // Footer liet ke truong bat buoc con trong; `Thời gian chụp dự kiến` da duoc dien san.
    await expect(page.getByText(/^Còn thiếu:/)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Tạo mới', exact: true })).toBeDisabled()
    await shot(page, 'tc02-modal-them-ca-chup')
  })

  test('US-KTV-04 [422]: thieu truong bat buoc thi khong gui duoc', async ({ page }) => {
    await login(page, 'ktv')
    await openAddStudy(page)
    await page.getByLabel('Mã bệnh nhân *').fill('TEST9001')
    // Chua dien Ho ten / Khoa / Bac si / Dich vu -> van phai bi khoa.
    await expect(page.getByRole('button', { name: 'Tạo mới', exact: true })).toBeDisabled()
    await expect(page.getByText(/^Còn thiếu:.*Họ tên/)).toBeVisible()
  })

  test('co o chon do chinh xac ngay sinh voi 3 muc', async ({ page }) => {
    await login(page, 'ktv')
    await openAddStudy(page)
    const sel = page.getByLabel('Độ chính xác ngày sinh')
    await expect(sel).toBeVisible()
    for (const label of ['Đủ ngày', 'Chỉ tháng', 'Chỉ năm']) {
      await expect(sel.getByRole('option', { name: label })).toHaveCount(1)
    }
  })
})

/**
 * ===================================================================================
 * L1 — DIEU TRA: form co chan ngay sinh o TUONG LAI khong?
 * ===================================================================================
 *
 * Nen: `Backend/src/main/java/com/vrris/pacs/rule/AgeCalculator.java` nem
 * `IllegalArgumentException("asOf khong duoc truoc dateOfBirth")` khi ngay sinh > hom nay.
 * `PatientCreateRequest.dateOfBirth` KHONG co `@PastOrPresent`. Nen neu form cho nhap ngay sinh
 * tuong lai va backend chap, thi lan doc lai benh nhan do se lam API no 500.
 *
 * Test nay KHONG assert dung/sai de khong lam ca bo test do; no GHI NHAN hanh vi that va chup
 * anh lam bang chung cho `reports/bug-report.md`.
 */
test('L1 [DIEU TRA]: form co chan ngay sinh o tuong lai?', async ({ page }) => {
  await login(page, 'ktv')
  await openAddStudy(page)

  const FUTURE = '2030-01-01'
  await page.getByLabel('Mã bệnh nhân *').fill('TEST9099')
  await page.getByLabel('Họ tên *').fill('Bệnh nhân Tương Lai (test L1)')
  await page.getByLabel('Ngày sinh', { exact: true }).fill(FUTURE)

  // Ngay sinh tuong lai co bi chan ngay o input khong? (max attribute)
  const maxAttr = await page.getByLabel('Ngày sinh', { exact: true }).getAttribute('max')
  const value = await page.getByLabel('Ngày sinh', { exact: true }).inputValue()

  await shot(page, 'L1-form-nhan-ngay-sinh-2030')

  console.log('### L1 KET QUA DIEU TRA ###')
  console.log(`  input[type=date] thuoc tinh max : ${maxAttr ?? '(KHONG CO - khong chan tuong lai)'}`)
  console.log(`  gia tri input sau khi dien      : ${value}`)
  console.log(`  form chap ngay sinh tuong lai   : ${value === FUTURE ? 'CO -> LOI L1 con ton tai o tang form' : 'KHONG'}`)

  // Ghi nhan: neu khong co `max` thi khong co gi chan o tang trinh duyet.
  expect(value).toBe(FUTURE)
})
