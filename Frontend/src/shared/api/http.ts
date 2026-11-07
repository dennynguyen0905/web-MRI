/**
 * Lớp HTTP dùng chung. Component **không** gọi thẳng fetch/Axios — chỉ đi qua các hàm
 * trong `features/<ten>/api` rồi tới hook (`doc/15` mục 7).
 *
 * Hiện cài bằng `fetch` để không thêm dependency khi nhóm chưa chốt Axios hay
 * fetch/TanStack Query (README 8.2–8.3, câu hỏi còn mở). Muốn đổi sang Axios thì viết một
 * `HttpClient` khác trong file này và đổi `createHttpClient`, phần còn lại không phải sửa.
 */

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export type QueryValue = string | number | boolean | null | undefined | (string | number)[]

export interface HttpRequest {
  method: HttpMethod
  path: string
  query?: Record<string, QueryValue>
  body?: unknown
  signal?: AbortSignal
}

export interface HttpClient {
  request<T>(req: HttpRequest): Promise<T>
  /** Dùng cho endpoint trả file (PDF phiếu in). */
  requestBlob(req: HttpRequest): Promise<{ blob: Blob; filename: string }>
}

/** Lỗi đã chuẩn hóa theo schema `Error` của openapi. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: { field?: string; issue?: string }[]

  constructor(status: number, code: string, message: string, details: { field?: string; issue?: string }[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }

  /** 401: chưa đăng nhập hoặc token hết hạn → quay về trang login (`doc/06` mục 8). */
  get isUnauthenticated() {
    return this.status === 401
  }

  /** 403: sai quyền → báo không đủ quyền, **không** đăng xuất (`doc/06` mục 8). */
  get isForbidden() {
    return this.status === 403
  }
}

export interface HttpClientOptions {
  baseUrl: string
  /** Lấy access token hiện tại; trả `null` khi chưa đăng nhập. */
  getAccessToken: () => string | null
  /** Gọi khi gặp 401, để tầng auth dọn phiên và chuyển về trang login. */
  onUnauthenticated?: () => void
}

export function buildQueryString(query: Record<string, QueryValue> | undefined): string {
  if (!query) return ''
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value == null || value === '') continue
    // openapi dùng style=form, explode=true: mảng lặp lại tên tham số.
    if (Array.isArray(value)) {
      for (const v of value) params.append(key, String(v))
    } else {
      params.append(key, String(value))
    }
  }
  const s = params.toString()
  return s ? `?${s}` : ''
}

function filenameFrom(disposition: string | null, fallback: string): string {
  if (!disposition) return fallback
  const star = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (star) return decodeURIComponent(star[1])
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain ? plain[1] : fallback
}

export function createHttpClient(options: HttpClientOptions): HttpClient {
  const { baseUrl, getAccessToken, onUnauthenticated } = options

  async function send(req: HttpRequest, accept: string): Promise<Response> {
    const token = getAccessToken()
    const headers: Record<string, string> = { Accept: accept }
    if (token) headers.Authorization = `Bearer ${token}`
    if (req.body !== undefined) headers['Content-Type'] = 'application/json'

    const res = await fetch(`${baseUrl}${req.path}${buildQueryString(req.query)}`, {
      method: req.method,
      headers,
      body: req.body === undefined ? undefined : JSON.stringify(req.body),
      signal: req.signal,
      credentials: 'include', // để refresh token dạng cookie HttpOnly đi kèm khi backend làm xong
    })

    if (res.ok) return res

    if (res.status === 401) onUnauthenticated?.()

    let code = `HTTP_${res.status}`
    let message = res.statusText || 'Yêu cầu không thành công'
    let details: { field?: string; issue?: string }[] = []
    try {
      const body = await res.json()
      if (body && typeof body === 'object') {
        code = body.code ?? code
        message = body.message ?? message
        details = Array.isArray(body.details) ? body.details : []
      }
    } catch {
      // Phản hồi không phải JSON (ví dụ nginx trả HTML) — giữ nguyên thông điệp mặc định.
    }
    throw new ApiError(res.status, code, message, details)
  }

  return {
    async request<T>(req: HttpRequest): Promise<T> {
      const res = await send(req, 'application/json')
      if (res.status === 204) return undefined as T
      return (await res.json()) as T
    },

    async requestBlob(req: HttpRequest) {
      const res = await send(req, 'application/pdf, application/octet-stream')
      return {
        blob: await res.blob(),
        filename: filenameFrom(res.headers.get('Content-Disposition'), 'tai-ve.pdf'),
      }
    },
  }
}
