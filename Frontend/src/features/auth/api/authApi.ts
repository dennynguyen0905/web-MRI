import { api } from '@/shared/api/client'
import type { CurrentUserDto, LoginResponseDto } from '@/shared/api/dto'
import { toAppUser } from '@/shared/api/mappers'
import type { AppUser } from '@/shared/lib/types'

export interface LoginResult {
  accessToken: string
  expiresIn: number
  user: AppUser
}

/** `POST /auth/login` (openapi `login`). */
export async function login(username: string, password: string): Promise<LoginResult> {
  const dto = await api.request<LoginResponseDto>({
    method: 'POST',
    path: '/auth/login',
    body: { username, password },
  })
  return { accessToken: dto.access_token, expiresIn: dto.expires_in, user: toAppUser(dto.user) }
}

/** `POST /auth/logout`. Lỗi mạng ở đây không chặn việc dọn phiên phía client. */
export async function logout(): Promise<void> {
  await api.request<void>({ method: 'POST', path: '/auth/logout' })
}

/** `GET /auth/me` — dùng để khôi phục phiên sau khi tải lại trang. */
export async function fetchCurrentUser(signal?: AbortSignal): Promise<AppUser> {
  const dto = await api.request<CurrentUserDto>({ method: 'GET', path: '/auth/me', signal })
  return toAppUser(dto)
}
