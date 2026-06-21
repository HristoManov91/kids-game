<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { KeyRound } from 'lucide-vue-next'
import { api } from '@/services/api'
import type { MessageResponse } from '@/types'

const route = useRoute()
const token = computed(() => typeof route.query.token === 'string' ? route.query.token : '')
const password = ref('')
const repeatPassword = ref('')
const loading = ref(false)
const error = ref(token.value ? '' : 'Линкът няма валиден token. Поискай нов линк.')
const message = ref('')

async function submit() {
  error.value = ''
  message.value = ''
  if (!token.value) {
    error.value = 'Линкът няма валиден token. Поискай нов линк.'
    return
  }
  if (password.value !== repeatPassword.value) {
    error.value = 'Паролите не съвпадат.'
    return
  }
  loading.value = true
  try {
    const response = await api.post<MessageResponse>('/auth/reset-password', {
      token: token.value,
      password: password.value,
      repeatPassword: repeatPassword.value
    })
    message.value = response.message
    password.value = ''
    repeatPassword.value = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Паролата не беше сменена.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <form class="auth-panel panel" @submit.prevent="submit">
      <KeyRound class="auth-icon" :size="42" />
      <h1>Нова парола</h1>
      <p class="muted">Избери нова парола с поне 4 символа.</p>

      <div v-if="error" class="error">{{ error }}</div>
      <div v-if="message" class="success">{{ message }}</div>

      <label class="field">
        <span>Нова парола</span>
        <input v-model="password" type="password" autocomplete="new-password" required minlength="4" maxlength="120" />
      </label>
      <label class="field">
        <span>Повтори паролата</span>
        <input v-model="repeatPassword" type="password" autocomplete="new-password" required minlength="4" maxlength="120" />
      </label>

      <button class="button" type="submit" :disabled="loading || !token || Boolean(message)">
        <KeyRound :size="20" />
        <span>Смени паролата</span>
      </button>
      <RouterLink class="secondary-link" :to="message ? '/login' : '/forgot-password'">
        {{ message ? 'Към входа' : 'Поискай нов линк' }}
      </RouterLink>
    </form>
  </section>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  width: min(100% - 20px, 460px);
  margin: 0 auto;
  padding: 40px 0;
}

.auth-panel { display: grid; width: 100%; gap: 16px; padding: 28px; }
.auth-icon { color: var(--green); }
h1, p { margin: 0; }
.secondary-link { justify-self: center; color: var(--blue); font-weight: 900; }
.success { border-radius: var(--radius); padding: 12px 14px; color: #126348; background: #def7e9; font-weight: 700; }
</style>
