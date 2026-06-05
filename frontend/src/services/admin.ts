import type { UserResponse } from '@/types'

const adminUsernames = new Set(['христо'])

export function isAdminUser(user: UserResponse | null | undefined) {
  return Boolean(user && adminUsernames.has(user.username.trim().toLocaleLowerCase('bg-BG')))
}
