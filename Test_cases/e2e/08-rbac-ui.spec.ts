import { expect, test } from '@playwright/test'
import { ACCOUNTS } from '../fixtures/accounts'
import { login, selectRowWithStatus, shot, visibleTransitionButtons } from '../fixtures/login'

/**
 * MA TRAN QUYEN O TANG GIAO DIEN — 5 vai tro x 7 trang thai.
 *
 * Day la vung rui ro cao nhat cua frontend va truoc test nay phu 0%: backend co
 * RBAC-01..15 chan rat chac, nhung khong backend nao kiem duoc "PatientPanel co dung nut
 * khong". Loi "hien nut khong duoc phep" chi lo ra o day.
 *
 * Dac ta: `doc/06b-ui-permission-spec.md` muc 2 + bang PERMISSIONS trong
 * `Frontend/src/shared/lib/auth.ts`. Lien quan RBAC-03/04/05/06 (`doc/06` muc 8).
 *
 * Bang quyen (auth.ts): ADMIN khong co report.write/report.approve (D15);
 * RADIOLOGIST chi report.write; APPROVER co report.approve + study.reopen;
 * TECHNICIAN co study.markReceived + study.cancel; CLINICIAN khong co buoc chuyen nao.
 */
const EXPECTED: Record<string, Record<string, string[]>> = {
  'Chờ thực hiện': {
    admin: ['Hủy ca', 'Đánh dấu đã có ảnh'],
    ktv: ['Hủy ca', 'Đánh dấu đã có ảnh'],
    bslamsang: [],
    bscdha: [],
    bsduyet: [],
  },
  'Chưa đọc': {
    admin: [], ktv: [], bslamsang: [], bscdha: ['Mở ca để đọc'], bsduyet: [],
  },
  'Đang đọc': {
    admin: ['Giải phóng khóa'], ktv: [], bslamsang: [],
    bscdha: ['Bỏ dở', 'Chốt đọc'], bsduyet: [],
  },
  'Đọc xong': {
    admin: [], ktv: [], bslamsang: [], bscdha: [], bsduyet: ['Mở ca để duyệt'],
  },
  'Đang duyệt': {
    admin: [], ktv: [], bslamsang: [], bscdha: [],
    bsduyet: ['Duyệt', 'Trả lại để sửa'],
  },
  'Duyệt xong': {
    admin: ['Mở lại ca'], ktv: [], bslamsang: [], bscdha: [], bsduyet: ['Mở lại ca'],
  },
  'Đã hủy': {
    admin: [], ktv: [], bslamsang: [], bscdha: [], bsduyet: [],
  },
}

for (const [statusLabel, perRole] of Object.entries(EXPECTED)) {
  test.describe(`Trang thai "${statusLabel}"`, () => {
    for (const [username, expected] of Object.entries(perRole)) {
      const role = ACCOUNTS[username].role
      test(`${role} (${username}) chi thay: ${expected.length ? expected.join(' + ') : '(khong nut nao)'}`,
        async ({ page }) => {
          await login(page, username)
          const found = await selectRowWithStatus(page, statusLabel)
          test.skip(!found, `khong co ca nao o trang thai "${statusLabel}" trong seed`)
          expect(await visibleTransitionButtons(page)).toEqual([...expected].sort())
        })
    }
  })
}

test.describe('RBAC-03: nut tao benh nhan theo vai tro', () => {
  // `patient.create`: ADMIN, CLINICIAN, TECHNICIAN co; RADIOLOGIST/APPROVER khong.
  for (const [username, allowed] of Object.entries({
    admin: true, bslamsang: true, ktv: true, bscdha: false, bsduyet: false,
  })) {
    test(`${ACCOUNTS[username].role} (${username}): nut "Thêm bệnh nhân" ${allowed ? 'dung duoc' : 'bi khoa'}`,
      async ({ page }) => {
        await login(page, username)
        if (allowed) {
          // Co quyen: title la ten thao tac, nut dung duoc.
          await expect(page.getByTitle('Thêm bệnh nhân')).toBeEnabled()
        } else {
          // Thieu quyen: nut KHONG bi an ma doi title thanh ly do tu choi
          // (`deniedReason()` sinh text "Cần vai trò: ...") roi disable. Hanh vi nay tot cho
          // nguoi dung vi noi ro vi sao khong bam duoc.
          await expect(page.getByTitle('Thêm bệnh nhân')).toHaveCount(0)
          const denied = page.getByTitle(/^Cần vai trò:/).first()
          await expect(denied).toBeVisible()
          await expect(denied).toBeDisabled()
        }
      })
  }
})

test('anh chup ma tran: panel cua tung vai tro o trang thai "Đang đọc"', async ({ page }) => {
  for (const u of ['admin', 'ktv', 'bscdha', 'bsduyet']) {
    await login(page, u)
    if (await selectRowWithStatus(page, 'Đang đọc')) await shot(page, `rbac-dangdoc-${u}`)
    await page.getByText(u, { exact: true }).first().click()
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click()
  }
})
