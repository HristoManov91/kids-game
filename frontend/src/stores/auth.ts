import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api, TOKEN_KEY } from '@/services/api'
import type { AuthResponse, UserResponse } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<UserResponse | null>(null)
  const loading = ref(false)

  async function login(username: string, password: string) {
    loading.value = true
    try {
      const response = await api.post<AuthResponse>('/auth/login', { username, password })
      token.value = response.token
      user.value = response.user
      localStorage.setItem(TOKEN_KEY, response.token)
    } finally {
      loading.value = false
    }
  }

  async function register(username: string, password: string, repeatPassword: string) {
    loading.value = true
    try {
      const response = await api.post<AuthResponse>('/auth/register', { username, password, repeatPassword })
      token.value = response.token
      user.value = response.user
      localStorage.setItem(TOKEN_KEY, response.token)
    } finally {
      loading.value = false
    }
  }

  async function loadMe() {
    if (!token.value) {
      return
    }
    user.value = await api.get<UserResponse>('/auth/me')
  }

  async function createChild(username: string, password: string, displayName: string) {
    return api.post<UserResponse>('/auth/children', { username, password, displayName })
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, user, loading, login, register, loadMe, createChild, logout }
})
