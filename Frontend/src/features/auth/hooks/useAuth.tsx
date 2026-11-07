import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError } from '@/shared/api/http'
import { sessionIsPersisted, setAccessToken, setUnauthenticatedHandler } from '@/shared/api/session'
import type { AppUser } from '@/shared/lib/types'
import * as authApi from '../api/authApi'

interface AuthContextValue {
  user: AppUser | null
  /** true trong lúc kiểm tra phiên cũ lúc khởi động — chưa biết đã đăng nhập hay chưa. */
  initializing: boolean
  signIn: (username: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  /** Thông báo 403 gần nhất, để màn hình hiện "không đủ quyền" mà không đăng xuất. */
  forbiddenMessage: string | null
  reportForbidden: (message: string) => void
  clearForbidden: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AppUser | null>(null)
  const [initializing, setInitializing] = useState(sessionIsPersisted)
  const [forbiddenMessage, setForbiddenMessage] = useState<string | null>(null)

  // 401 từ bất kỳ request nào: dọn phiên. `RequireAuth` sẽ tự chuyển về trang login.
  useEffect(() => {
    setUnauthenticatedHandler(() => {
      setAccessToken(null)
      setUser(null)
    })
    return () => setUnauthenticatedHandler(() => {})
  }, [])

  // Chỉ thử khôi phục phiên khi token được giữ lại qua lần tải trang; mặc định token nằm
  // trong bộ nhớ nên tải lại là về trang login, đúng như `doc/09` mục 1.2 mô tả.
  useEffect(() => {
    if (!sessionIsPersisted) return
    const controller = new AbortController()
    authApi.fetchCurrentUser(controller.signal)
      .then(setUser)
      .catch(() => setAccessToken(null))
      .finally(() => setInitializing(false))
    return () => controller.abort()
  }, [])

  const signIn = useCallback(async (username: string, password: string) => {
    const result = await authApi.login(username, password)
    setAccessToken(result.accessToken)
    setUser(result.user)
    setForbiddenMessage(null)
  }, [])

  const signOut = useCallback(async () => {
    try {
      await authApi.logout()
    } catch (err) {
      // Đăng xuất phía máy chủ hỏng thì vẫn dọn phiên phía client.
      if (!(err instanceof ApiError)) throw err
    } finally {
      setAccessToken(null)
      setUser(null)
      setForbiddenMessage(null)
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    initializing,
    signIn,
    signOut,
    forbiddenMessage,
    reportForbidden: setForbiddenMessage,
    clearForbidden: () => setForbiddenMessage(null),
  }), [user, initializing, signIn, signOut, forbiddenMessage])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phải nằm trong <AuthProvider>')
  return ctx
}

/** Người dùng đang đăng nhập; chỉ gọi trong vùng đã qua `RequireAuth`. */
export function useCurrentUser(): AppUser {
  const { user } = useAuth()
  if (!user) throw new Error('useCurrentUser chỉ dùng được sau khi đã đăng nhập')
  return user
}
