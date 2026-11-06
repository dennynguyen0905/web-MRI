import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

/**
 * Chặn route khi chưa đăng nhập (IN-13): chưa có phiên thì chuyển về `/login` và nhớ
 * đường dẫn đang muốn vào để đăng nhập xong quay lại đúng chỗ.
 *
 * Đây chỉ là lớp giao diện. Backend vẫn phải trả 401/403 cho mọi endpoint (`doc/06` mục 1).
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth()
  const location = useLocation()

  if (initializing) {
    return (
      <div className="flex h-full items-center justify-center text-[12px] text-ink-4">
        Đang kiểm tra phiên đăng nhập…
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }

  return <>{children}</>
}
