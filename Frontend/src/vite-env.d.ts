/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** `mock` (mặc định) hoặc `live`. */
  readonly VITE_API_MODE?: string
  /** Mặc định `/api/v1`, cùng origin — xem `shared/api/client.ts`. */
  readonly VITE_API_BASE_URL?: string
  /** `true` thì access token sống qua lần tải lại trang (nợ kỹ thuật, xem `shared/api/session.ts`). */
  readonly VITE_PERSIST_SESSION?: string
  /** Base URL của OHIF Viewer để mở deep-link (IN-23). */
  readonly VITE_OHIF_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
