import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/http'

export interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: ApiError | null
}

/**
 * Hook gọi API dùng chung. Nhóm chưa chốt TanStack Query (README 8.2–8.3) nên tạm dùng
 * hook tự viết — đủ cho worklist và các màn hiện có. Nếu sau này đưa TanStack Query vào,
 * chỉ các hook trong `features/<ten>/hooks` phải sửa, component không đụng tới.
 */
export function useAsync<T>(
  run: (signal: AbortSignal) => Promise<T>,
  deps: unknown[],
  options: { enabled?: boolean } = {},
): AsyncState<T> & { reload: () => void } {
  const enabled = options.enabled ?? true
  const [state, setState] = useState<AsyncState<T>>({ data: null, loading: enabled, error: null })
  const [nonce, setNonce] = useState(0)

  // Giữ bản mới nhất của `run` mà không đưa nó vào deps: `run` là closure được tạo lại mỗi lần
  // render, đưa vào deps sẽ gọi API vô hạn. Cập nhật trong effect (không gán lúc render) để
  // không vi phạm luật "không đụng ref khi đang render" của React.
  const runRef = useRef(run)
  useEffect(() => {
    runRef.current = run
  })

  useEffect(() => {
    if (!enabled) {
      setState({ data: null, loading: false, error: null })
      return
    }
    const controller = new AbortController()
    let alive = true
    setState(s => ({ ...s, loading: true, error: null }))

    runRef.current(controller.signal)
      .then(data => {
        if (alive) setState({ data, loading: false, error: null })
      })
      .catch((err: unknown) => {
        if (!alive || controller.signal.aborted) return
        const error = err instanceof ApiError
          ? err
          : new ApiError(0, 'NETWORK_ERROR', err instanceof Error ? err.message : 'Không gọi được máy chủ.')
        setState({ data: null, loading: false, error })
      })

    return () => {
      alive = false
      controller.abort()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, enabled, nonce])

  const reload = useCallback(() => setNonce(n => n + 1), [])
  return { ...state, reload }
}
