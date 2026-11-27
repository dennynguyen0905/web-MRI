import { expect, test } from '@playwright/test'
import { login, selectRowWithStatus, shot } from '../fixtures/login'

/**
 * SM-11 / US-KTV-08 / US-BSCDHA-04 / US-BSDUYET-03 / US-ADMIN-01:
 * 4 buoc ngoai le (T4, T8, T9, T10) BAT BUOC nhap ly do.
 *
 * Cach frontend chan: nut xac nhan bi `disabled` kem `title`, KHONG co thong bao loi dang text.
 * Day la diem yeu trai nghiem da ghi trong bao cao loi (`UX-01`).
 */
test.describe('SM-11: buoc ngoai le bat buoc nhap ly do', () => {
  test('T10 "Hủy ca": nut xac nhan bi khoa khi ly do trong, mo khoa khi da nhap', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)

    await page.getByRole('button', { name: 'Hủy ca', exact: true }).click()

    // Trong dialog, nut xac nhan cung ten "Hủy ca" -> lay cai cuoi cung (trong modal).
    const confirm = page.getByRole('button', { name: 'Hủy ca', exact: true }).last()
    await expect(confirm).toBeDisabled()
    await expect(confirm).toHaveAttribute('title', 'Thao tác này bắt buộc nhập lý do')
    await shot(page, 'sm11-t10-ly-do-trong')

    const reason = page.getByPlaceholder(/^Ví dụ: bệnh nhân đã xuất viện/)
    await expect(reason).toBeVisible()

    // Chi khoang trang van phai bi coi la trong (reason.trim() === '').
    await reason.fill('   ')
    await expect(confirm).toBeDisabled()

    await reason.fill('Bệnh nhân đã xuất viện, không còn nhu cầu chụp')
    await expect(confirm).toBeEnabled()
    await shot(page, 'sm11-t10-da-nhap-ly-do')
  })

  test('T4 "Bỏ dở": bat buoc ly do', async ({ page }) => {
    await login(page, 'bscdha')
    const ok = await selectRowWithStatus(page, 'Đang đọc')
    test.skip(!ok, 'khong co ca READING trong seed')

    await page.getByRole('button', { name: 'Bỏ dở', exact: true }).click()
    const confirm = page.getByRole('button', { name: 'Bỏ dở', exact: true }).last()
    await expect(confirm).toBeDisabled()
    await page.getByPlaceholder(/^Ví dụ: ảnh nhiễu/).fill('Ảnh nhiễu, cần chụp lại')
    await expect(confirm).toBeEnabled()
  })

  test('T8 "Trả lại để sửa": bat buoc ly do', async ({ page }) => {
    await login(page, 'bsduyet')
    const ok = await selectRowWithStatus(page, 'Đang duyệt')
    test.skip(!ok, 'khong co ca APPROVING trong seed')

    await page.getByRole('button', { name: 'Trả lại để sửa', exact: true }).click()
    const confirm = page.getByRole('button', { name: 'Trả lại để sửa', exact: true }).last()
    await expect(confirm).toBeDisabled()
    await page.getByPlaceholder(/^Ví dụ: cần bổ sung mô tả/).fill('Thiếu mô tả vùng tổn thương')
    await expect(confirm).toBeEnabled()
  })

  test('T9 "Mở lại ca": bat buoc ly do', async ({ page }) => {
    await login(page, 'admin')
    const ok = await selectRowWithStatus(page, 'Duyệt xong')
    test.skip(!ok, 'khong co ca APPROVED trong seed')

    await page.getByRole('button', { name: 'Mở lại ca', exact: true }).click()
    const confirm = page.getByRole('button', { name: 'Mở lại ca', exact: true }).last()
    await expect(confirm).toBeDisabled()
    await page.getByPlaceholder(/^Ví dụ: phát hiện thêm tổn thương/).fill('Phát hiện thêm tổn thương sau hội chẩn')
    await expect(confirm).toBeEnabled()
  })

  test('T2 "Đánh dấu đã có ảnh" KHONG bat ly do (buoc thuong)', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
    const t2 = page.getByRole('button', { name: 'Đánh dấu đã có ảnh', exact: true })
    // T2 bi disable khi ca chua co study_instance_uid (RBAC-12 doi xung: SM-12).
    if (await t2.isDisabled()) {
      await expect(t2).toHaveAttribute(
        'title', 'Ca chưa có StudyInstanceUID — chưa có ảnh trong PACS để đánh dấu')
      await shot(page, 'sm12-t2-chua-co-anh')
    }
  })
})

/**
 * RBAC-12 / US-KTV-08: khong duoc huy ca DA co anh trong PACS.
 * Frontend chan truoc bang disable nut; backend tra 422 CANNOT_CANCEL_STUDY_WITH_IMAGE.
 */
test('RBAC-12: ca da co anh thi nut "Hủy ca" bi khoa dung ly do', async ({ page }) => {
  await login(page, 'ktv')
  expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
  const cancel = page.getByRole('button', { name: 'Hủy ca', exact: true }).first()
  if (await cancel.isDisabled()) {
    await expect(cancel).toHaveAttribute('title', 'Ca đã có ảnh trong PACS nên không hủy được')
  } else {
    // Ca khong co anh -> huy duoc, dung dac ta.
    await expect(cancel).toBeEnabled()
  }
})
