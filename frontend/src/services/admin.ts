import type { UserResponse } from '@/types'

export function isAdminUser(user: UserResponse | null | undefined) {
  return user?.role === 'ADMIN'
}
