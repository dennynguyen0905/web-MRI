import { createHttpClient, type HttpClient } from './http'
import { createMockClient } from './mock/mockClient'
import { getAccessToken, notifyUnauthenticated } from './session'

/**
 * Base URL là đường dẫn tương đối cùng origin (`doc/14` mục 3.2): nginx của `ris-frontend`
 * proxy `/api/` sang backend nên không cần cấu hình CORS. Khi `npm run dev`, proxy trong
 * `vite.config.ts` thay vai trò nginx.
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

/** `mock` (mặc định) chạy mock server trong trình duyệt; `live` gọi backend thật. */
export const API_MODE: 'mock' | 'live' = import.meta.env.VITE_API_MODE === 'live' ? 'live' : 'mock'

export const isMockMode = API_MODE === 'mock'

export const api: HttpClient = isMockMode
  ? createMockClient()
  : createHttpClient({
    baseUrl: API_BASE_URL,
    getAccessToken,
    onUnauthenticated: notifyUnauthenticated,
  })
