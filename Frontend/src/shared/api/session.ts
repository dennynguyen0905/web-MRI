/**
 * Nơi giữ access token.
 *
 * Quyết định ❓ ở `doc/09` mục 1.2 chưa chốt. Mặc định theo **đề xuất của tài liệu**:
 * access token nằm trong biến JS (bộ nhớ), refresh token do backend đặt trong cookie HttpOnly
 * (`credentials: 'include'` ở `http.ts` đã sẵn sàng cho việc đó). Token trong bộ nhớ thì
 * XSS không đọc được, đổi lại tải lại trang là mất phiên — đúng như tài liệu mô tả.
 *
 * Nếu nhóm chốt dùng localStorage/sessionStorage cho nhanh thì bật `VITE_PERSIST_SESSION=true`
 * và **phải** ghi vào nợ kỹ thuật tuần 8: khi đó XSS đọc được token.
 */
const PERSIST = import.meta.env.VITE_PERSIST_SESSION === 'true'
const STORAGE_KEY = 'vr-ris.access-token'

let accessToken: string | null = PERSIST ? readStored() : null

function readStored(): string | null {
  try {
    return sessionStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
  if (!PERSIST) return
  try {
    if (token) sessionStorage.setItem(STORAGE_KEY, token)
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    // Trình duyệt chặn storage (chế độ riêng tư) — vẫn chạy được với token trong bộ nhớ.
  }
}

/** true khi phiên có thể sống qua lần tải lại trang; dùng để quyết định có gọi `/auth/me` không. */
export const sessionIsPersisted = PERSIST

type UnauthenticatedHandler = () => void

let onUnauthenticated: UnauthenticatedHandler = () => {}

/** Tầng auth đăng ký hàm dọn phiên + chuyển về trang login khi API trả 401. */
export function setUnauthenticatedHandler(handler: UnauthenticatedHandler): void {
  onUnauthenticated = handler
}

export function notifyUnauthenticated(): void {
  onUnauthenticated()
}
