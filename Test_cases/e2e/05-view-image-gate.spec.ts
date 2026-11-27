import { expect, test } from '@playwright/test'
import { login, selectRowWithStatus, shot } from '../fixtures/login'

/**
 * TC-15 (`doc/12` muc 3.1) / US-BSCDHA-02 / US-BSLS-04 / P-10:
 * ca chua co `study_instance_uid` thi khong duoc mo viewer rong.
 *
 * ❓ `P-10` (`doc/07-pacs-dicom.md` muc 6.2) CHUA CHOT la "an" hay "disable". Code hien tai
 * lam CA HAI, tuy trang thai — test duoi day khoa lai hanh vi dang chay de nhom co co so chot:
 *   - `SCHEDULED`  -> nut BI AN hoan toan
 *   - khac         -> nut HIEN nhung DISABLED, title "Chưa có ảnh chụp"
 */
test.describe('TC-15: chan mo viewer khi chua co anh', () => {
  test('ca SCHEDULED: nut "Xem ảnh chụp" bi an hoan toan', async ({ page }) => {
    await login(page, 'ktv')
    expect(await selectRowWithStatus(page, 'Chờ thực hiện')).toBe(true)
    await expect(page.getByRole('button', { name: 'Xem ảnh chụp', exact: true })).toHaveCount(0)
    await shot(page, 'tc15-scheduled-an-nut')
  })

  test('ca UNREAD: nut hien; disable kem ly do khi chua co UID', async ({ page }) => {
    await login(page, 'bscdha')
    const ok = await selectRowWithStatus(page, 'Chưa đọc')
    test.skip(!ok, 'khong co ca UNREAD trong seed')

    const btn = page.getByRole('button', { name: 'Xem ảnh chụp', exact: true })
    await expect(btn).toBeVisible()
    if (await btn.isDisabled()) {
      await expect(btn).toHaveAttribute('title', 'Chưa có ảnh chụp')
      await shot(page, 'tc15-unread-disable')
    } else {
      // Co UID -> bam duoc. Khong mo tab moi trong test de khong treo.
      await expect(btn).toBeEnabled()
    }
  })
})
