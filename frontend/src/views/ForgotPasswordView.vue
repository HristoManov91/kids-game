<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { KeyRound, Mail } from 'lucide-vue-next'
import { api } from '@/services/api'
import type { MessageResponse } from '@/types'

const email = ref('')
const loading = ref(false)
const error = ref('')
const message = ref('')

async function submit() {
  error.value = ''
  message.value = ''
  loading.value = true
  try {
    const response = await api.post<MessageResponse>('/auth/forgot-password', { email: email.value })
    message.value = response.message
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Заявката не беше успешна.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <form class="auth-panel panel" @submit.prevent="submit">
      <KeyRound class="auth-icon" :size="42" />
      <h1>Забравена парола</h1>
      <p class="muted">Въведи email-а от регистрацията. Ще получиш линк, валиден 30 минути.</p>

      <div v-if="error" class="error">{{ error }}</div>
      <div v-if="message" class="success">{{ message }}</div>

      <label class="field">
        <span>Email</span>
        <input v-model.trim="email" type="email" autocomplete="email" required maxlength="254" />
      </label>

      <button class="button" type="submit" :disabled="loading">
        <Mail :size="20" />
        <span>Изпрати линк</span>
      </button>
      <RouterLink class="secondary-link" to="/login">Обратно към входа</RouterLink>
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

.auth-panel {
  display: grid;
  width: 100%;
  gap: 16px;
  padding: 28px;
}

.auth-icon { color: var(--green); }
h1, p { margin: 0; }
.secondary-link { justify-self: center; color: var(--blue); font-weight: 900; }
.success { border-radius: var(--radius); padding: 12px 14px; color: #126348; background: #def7e9; font-weight: 700; }
</style>
