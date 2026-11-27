import { expect, test } from '@playwright/test'
import { login, selectRowWithStatus } from '../fixtures/login'

/**
 * Nhom kiem thu "kieu nguoi dung" — thao tac nhu KTV tiep nhan benh nhan that, tim loi nghiep vu
 * ma test don vi khong bao gio bat duoc. Phien tham do 2026-11-27.
 *
 * Lien quan `doc/13` US-KTV-01 (tao benh nhan) va US-KTV-02 (tim benh nhan de TRANH TAO TRUNG).
 */
async function openAdd(page: import('@playwright/test').Page) {
  await page.getByTitle('Thêm bệnh nhân').click()
  await expect(page.getByText('THÊM CA CHỤP', { exact: true })).toBeVisible()
  return page.getByRole('dialog')
}

async function fillOrder(dlg: ReturnType<typeof openAdd> extends Promise<infer T> ? T : never) {
  await dlg.getByLabel('Khoa chỉ định *').selectOption({ index: 1 })
  await dlg.getByLabel('Bác sĩ chỉ định *').fill('BS. Kiem Thu')
  await dlg.getByLabel('Dịch vụ chụp *').selectOption({ index: 1 })
}

test.describe('Trung ho so benh nhan', () => {
  test('DUP-01 [LOI CON MO]: tao duoc ho so trung nguoi ma khong canh bao', async ({ page }) => {
    // Cung ho ten + ngay sinh + gioi tinh voi TEST0004, khac ma, KHONG nhap BHXH.
    // Backend chi chan trung `patient_code` va trung `social_insurance_no`
    // (`PatientServiceImpl.create` + `rejectSuspectedDuplicateBhxh`), khong co luat nao cho
    // "cung ten + cung ngay sinh". Test nay GHI NHAN hanh vi hien tai; khi backend them luat
    // canh bao thi test se do va phai cap nhat — do la chu y.
    await login(page, 'ktv')
    const dlg = await openAdd(page)
    await dlg.getByLabel('Mã bệnh nhân *').fill('TEST8101')
    await dlg.getByLabel('Họ tên *').fill('Phạm Thị Giả D')
    await dlg.getByLabel('Ngày sinh', { exact: true }).fill('1990-11-20')
    await dlg.getByLabel('Giới tính').selectOption({ label: 'Nữ' })
    await fillOrder(dlg)
    await dlg.getByRole('button', { name: 'Tạo mới', exact: true }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })

  test('DUP-02: tim benh nhan trong modal phai tim duoc theo HO TEN, khong chi theo ma', async ({ page }) => {
    // US-KTV-02: "Tim benh nhan de tranh tao trung". Truoc 2026-11-27 o nay chi gui
    // `patient_code` nen go ho ten luon ra "khong tim thay" -> KTV buoc phai tao moi.
    await login(page, 'ktv')
    const dlg = await openAdd(page)
    await dlg.getByRole('checkbox', { name: 'Thêm mới bệnh nhân' }).uncheck()
    await dlg.getByPlaceholder('Nhập mã bệnh nhân, họ tên hoặc mã BHXH').fill('Phạm Thị Giả D')
    await dlg.getByRole('button', { name: 'Tìm kiếm', exact: true }).click()
    await expect(dlg.getByText('Phạm Thị Giả D').first()).toBeVisible()
  })

  test('DUP-02: tim duoc theo ma BHXH', async ({ page }) => {
    await login(page, 'ktv')
    const dlg = await openAdd(page)
    await dlg.getByRole('checkbox', { name: 'Thêm mới bệnh nhân' }).uncheck()
    await dlg.getByPlaceholder('Nhập mã bệnh nhân, họ tên hoặc mã BHXH').fill('GIA0000000001')
    await dlg.getByRole('button', { name: 'Tìm kiếm', exact: true }).click()
    await expect(dlg.getByText('Nguyễn Văn Giả A').first()).toBeVisible()
  })

  test('DUP-02: van tim duoc theo ma benh nhan nhu truoc', async ({ page }) => {
    await login(page, 'ktv')
    const dlg = await openAdd(page)
    await dlg.getByRole('checkbox', { name: 'Thêm mới bệnh nhân' }).uncheck()
    await dlg.getByPlaceholder('Nhập mã bệnh nhân, họ tên hoặc mã BHXH').fill('TEST0001')
    await dlg.getByRole('button', { name: 'Tìm kiếm', exact: true }).click()
    await expect(dlg.getByText('Nguyễn Văn Giả A').first()).toBeVisible()
  })

  test('trung ma benh nhan: CO bao loi ro rang', async ({ page }) => {
    await login(page, 'ktv')
    const dlg = await openAdd(page)
    await dlg.getByLabel('Mã bệnh nhân *').fill('TEST0001')
    await dlg.getByLabel('Họ tên *').fill('Nguoi Khac Hoan Toan')
    await fillOrder(dlg)
    await dlg.getByRole('button', { name: 'Tạo mới', exact: true }).click()
    await expect(dlg.getByText(/Mã bệnh nhân đã tồn tại/)).toBeVisible()
  })

  test('trung ma BHXH: CO bao loi ro rang', async ({ page }) => {
    await login(page, 'ktv')
    const dlg = await openAdd(page)
    await dlg.getByLabel('Mã bệnh nhân *').fill('TEST8102')
    await dlg.getByLabel('Họ tên *').fill('Nguoi Khac Trung BHXH')
    await dlg.getByLabel('Mã BHXH').fill('GIA0000000001')
    await fillOrder(dlg)
    await dlg.getByRole('button', { name: 'Tạo mới', exact: true }).click()
    await expect(dlg.getByText(/Mã BHXH này đã gắn với một bệnh nhân khác/)).toBeVisible()
  })

  test('ho ten chi gom khoang trang: nut "Tạo mới" bi khoa', async ({ page }) => {
    await login(page, 'ktv')
    const dlg = await openAdd(page)
    await dlg.getByLabel('Mã bệnh nhân *').fill('TEST8103')
    await dlg.getByLabel('Họ tên *').fill('      ')
    await fillOrder(dlg)
    await expect(dlg.getByRole('button', { name: 'Tạo mới', exact: true })).toBeDisabled()
  })
})

test.describe('A11Y-04: ClinicalModal tu viet overlay rieng', () => {
  test('ClinicalModal co role=dialog, aria-modal va nut dong co nhan', async ({ page }) => {
    await login(page, 'bslamsang')
    expect(await selectRowWithStatus(page, 'Chưa đọc')).toBe(true)
    await page.getByRole('button', { name: 'Lâm sàng', exact: true }).click()
    const dlg = page.getByRole('dialog')
    await expect(dlg).toBeVisible()
    await expect(dlg).toHaveAttribute('aria-modal', 'true')
    const id = await dlg.getAttribute('aria-labelledby')
    await expect(page.locator(`#${id}`)).toHaveText('Thông tin lâm sàng')
    await expect(dlg.getByRole('button', { name: 'Đóng hộp thoại' })).toBeVisible()
  })

  test('Esc dong duoc ClinicalModal', async ({ page }) => {
    await login(page, 'bslamsang')
    expect(await selectRowWithStatus(page, 'Chưa đọc')).toBe(true)
    await page.getByRole('button', { name: 'Lâm sàng', exact: true }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })
})
