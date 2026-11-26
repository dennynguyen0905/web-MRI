import type { Page } from '@playwright/test'
import { DEMO_PASSWORD } from './accounts'

/**
 * Dang nhap qua form that (khong tiem token) de buoc di dung luong RequireAuth.
 *
 * LUU Y VE SELECTOR: o `Frontend/src/features/auth/pages/LoginPage.tsx` hai o nhap KHONG co
 * `id`, `name` hay `aria-label` — chi co `placeholder` va `autoComplete`. Vi vay o day dung
 * `getByPlaceholder`. Day la diem yeu ve kha nang truy cap (a11y), da ghi trong bao cao loi.
 */
export async function login(page: Page, username: string): Promise<void> {
  await page.goto('/')
  await page.getByPlaceholder('Nhập tên đăng nhập').fill(username)
  await page.getByPlaceholder('Nhập mật khẩu').fill(DEMO_PASSWORD)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await page.getByPlaceholder('Nhập tên đăng nhập').waitFor({ state: 'detached' })
}

/** Chup anh vao thu muc bao cao loi (reports/ bi gitignore). */
export async function shot(page: Page, name: string): Promise<void> {
  await page.screenshot({ path: `reports/screenshots/${name}.png`, fullPage: false, scale: 'css' })
}

/**
 * Bang worklist CHINH.
 *
 * Trang co 2 bang: bang chinh va bang "phieu lien quan" trong panel ben phai. Hai bang dung
 * trung nhieu tieu de (`Họ tên`, `Trạng thái`), nen moi assert ve cot phai khoanh vung bang
 * chinh, neu khong Playwright bao strict mode violation.
 */
export function mainTable(page: Page) {
  return page.getByRole('table').first()
}

/** Tieu de cot cua bang chinh. */
export function column(page: Page, name: string) {
  return mainTable(page).getByRole('columnheader', { name, exact: true })
}

/**
 * Chon mot hang co dung trang thai can kiem, roi mo PatientPanel.
 *
 * `SCHEDULED` nam o tab "Cho"; 6 trang thai con lai nam o tab "Da thuc hien" va phai tick
 * checkbox trang thai tuong ung tren thanh loc (sau `cfd3ffa` thanh nay da chuyen len tren).
 *
 * Tra ve false khi khong co hang nao o trang thai do — test goi phai tu quyet dinh skip.
 */
export async function selectRowWithStatus(page: Page, statusLabel: string): Promise<boolean> {
  if (statusLabel === 'Chờ thực hiện') {
    await page.getByRole('tab', { name: /^Chờ \(/ }).click()
  } else {
    await page.getByRole('tab', { name: 'Đã thực hiện', exact: true }).click()
    // Tick dung mot trang thai: bo cac checkbox khac de danh sach chi con trang thai can kiem.
    for (const other of ['Chưa đọc', 'Đang đọc', 'Đọc xong', 'Đang duyệt', 'Duyệt xong', 'Đã hủy']) {
      const cb = page.getByRole('checkbox', { name: new RegExp(`^${other}`) })
      const want = other === statusLabel
      if ((await cb.isChecked()) !== want) await cb.setChecked(want)
    }
  }
  const rows = mainTable(page).locator('tbody tr')
  await page.waitForTimeout(400)
  if ((await rows.count()) === 0) return false
  await rows.first().click()
  return true
}

/** Cac nut chuyen trang thai T2-T10 dang hien trong PatientPanel. */
export const TRANSITION_BUTTONS = [
  'Đánh dấu đã có ảnh', 'Hủy ca', 'Mở ca để đọc', 'Chốt đọc', 'Bỏ dở',
  'Giải phóng khóa', 'Mở ca để duyệt', 'Duyệt', 'Trả lại để sửa', 'Mở lại ca',
] as const

export async function visibleTransitionButtons(page: Page): Promise<string[]> {
  const found: string[] = []
  for (const name of TRANSITION_BUTTONS) {
    if (await page.getByRole('button', { name, exact: true }).count() > 0) found.push(name)
  }
  return found.sort()
}
