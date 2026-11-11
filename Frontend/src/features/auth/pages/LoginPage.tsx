import { type FormEvent, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { isMockMode } from '@/shared/api/client'
import { ApiError } from '@/shared/api/http'
import { DEMO_PASSWORD, USERS } from '@/shared/api/mock/seed'
import { ROLE_META } from '@/shared/lib/auth'
import { Button, Checkbox, Input, cx } from '@/shared/ui/controls'
import { Lock, Plus, User, Warning } from '@/shared/ui/Icon'
import { useAuth } from '../hooks/useAuth'

/** Màn đăng nhập (IN-13). Gọi `POST /auth/login` qua `features/auth/api`. */
export function LoginPage() {
  const { user, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/worklist'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [remember, setRemember] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  // Đã đăng nhập mà mở lại /login thì quay về chỗ đang làm dở.
  useEffect(() => {
    if (user) navigate(from, { replace: true })
  }, [user, from, navigate])

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await signIn(username, password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : 'Không gọi được máy chủ. Kiểm tra lại kết nối hoặc cấu hình VITE_API_BASE_URL.',
      )
    } finally {
      setBusy(false)
    }
  }

  const quickFill = (name: string) => {
    setUsername(name)
    setPassword(DEMO_PASSWORD)
    setError(null)
  }

  return (
    <div className="flex h-full items-center justify-center overflow-auto bg-app p-6">
      <div className="w-full max-w-[760px] desktop:max-w-[860px]">
        <div className="mb-5 flex items-center justify-center gap-2.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 text-white">
            <Plus size={22} strokeWidth={2.4} />
          </div>
          <div className="leading-tight">
            <div className="text-[18px] font-bold tracking-tight text-ink">VR-RIS PACS</div>
            <div className="text-[10px] font-medium uppercase tracking-wider text-ink-4">RIS tích hợp PACS</div>
          </div>
        </div>

        <div className="grid gap-3 md:grid-cols-[320px_1fr] desktop:gap-4 desktop:grid-cols-[360px_1fr]">
          <form onSubmit={submit} className="card p-5 desktop:p-6">
            <h1 className="mb-0.5 text-[14px] font-semibold text-ink">Đăng nhập</h1>
            <p className="mb-4 text-[11.5px] text-ink-4">Dùng tài khoản được cấp trong hệ thống.</p>

            <label className="mb-2.5 block">
              <div className="mb-1 text-[11.5px] text-ink-3">Tên đăng nhập</div>
              <div className="relative">
                <User size={13} className="pointer-events-none absolute left-2.5 top-1/2 z-10 -translate-y-1/2 text-ink-4" />
                <Input
                  autoFocus
                  autoComplete="username"
                  placeholder="Nhập tên đăng nhập"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  className="h-8 pl-7"
                />
              </div>
            </label>

            <label className="mb-3 block">
              <div className="mb-1 text-[11.5px] text-ink-3">Mật khẩu</div>
              <div className="relative">
                <Lock size={13} className="pointer-events-none absolute left-2.5 top-1/2 z-10 -translate-y-1/2 text-ink-4" />
                <Input
                  type="password"
                  autoComplete="current-password"
                  placeholder="Nhập mật khẩu"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  className="h-8 pl-7"
                />
              </div>
            </label>

            <div className="mb-3 flex items-center justify-between">
              <Checkbox checked={remember} onChange={setRemember} label={<span className="text-ink-3">Ghi nhớ đăng nhập</span>} />
              <button type="button" className="text-[11.5px] text-brand-600 hover:underline">Quên mật khẩu?</button>
            </div>

            {error && (
              <div className="mb-3 flex items-start gap-1.5 rounded-md border border-[#f7cfcf] bg-[#fdf2f2] px-2.5 py-2 text-[11.5px] text-[#c81e1e]">
                <Warning size={13} className="mt-px shrink-0" />
                {error}
              </div>
            )}

            <Button type="submit" size="md" block disabled={busy}>
              {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
            </Button>
          </form>

          <div className="card p-5 desktop:p-6">
            <h2 className="mb-0.5 text-[13px] font-semibold text-ink">Tài khoản demo</h2>
            <p className="mb-3 text-[11.5px] text-ink-4">
              7 tài khoản seed của <code className="rounded bg-line-soft px-1 py-0.5 font-mono text-[11px] text-ink-2">V7</code>/<code className="rounded bg-line-soft px-1 py-0.5 font-mono text-[11px] text-ink-2">V8</code>.
              Mật khẩu chung: <code className="rounded bg-line-soft px-1 py-0.5 font-mono text-[11px] text-ink-2">{DEMO_PASSWORD}</code>
            </p>

            <div className="space-y-1.5">
              {USERS.map(u => (
                <button
                  key={u.id}
                  type="button"
                  onClick={() => quickFill(u.username)}
                  className={cx(
                    'flex w-full items-center gap-2.5 rounded-md border px-2.5 py-2 text-left transition-colors',
                    username === u.username
                      ? 'border-brand-300 bg-brand-50'
                      : 'border-line bg-white hover:bg-line-soft',
                  )}
                >
                  <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-brand-600 text-[11px] font-bold text-white">
                    {u.full_name.slice(0, 1)}
                  </span>
                  <span className="min-w-0 flex-1 leading-tight">
                    <span className="block truncate text-[12px] font-semibold text-ink">{u.full_name}</span>
                    <span className="block truncate text-[11px] text-ink-4">
                      {u.username} · {u.roles.map(r => ROLE_META[r].label).join(', ')}
                    </span>
                  </span>
                </button>
              ))}
            </div>

            <p className="mt-3 text-[11px] leading-relaxed text-ink-4">
              {isMockMode
                ? 'Đang chạy mock server trong trình duyệt (VITE_API_MODE=mock). Đặt VITE_API_MODE=live để gọi backend thật.'
                : 'Đang gọi backend thật. Danh sách trên chỉ để gợi ý tên đăng nhập.'}
              {' '}Quyền thật do backend chặn ở tầng API (IN-14), giao diện chỉ ẩn/khóa nút.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
