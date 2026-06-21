<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Mail, Save } from 'lucide-vue-next'
import { api } from '@/services/api'

interface AccountEmailResponse { email: string | null }

const email = ref('')
const loading = ref(false)
const error = ref('')
const message = ref('')

async function submit() {
  error.value = ''
  message.value = ''
  loading.value = true
  try {
    const response = await api.put<AccountEmailResponse>('/auth/email', { email: email.value })
    email.value = response.email ?? ''
    message.value = 'Email адресът е запазен.'
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Email адресът не беше запазен.'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const response = await api.get<AccountEmailResponse>('/auth/email')
    email.value = response.email ?? ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Профилът не беше зареден.'
  }
})
</script>

<template>
  <main class="page account-page">
    <form class="panel account-panel" @submit.prevent="submit">
      <Mail :size="38" class="account-icon" />
      <h1>Моят профил</h1>
      <p class="muted">Този email се използва само когато поискаш нова парола.</p>

      <div v-if="error" class="error">{{ error }}</div>
      <div v-if="message" class="success">{{ message }}</div>

      <label class="field">
        <span>Email за възстановяване</span>
        <input v-model.trim="email" type="email" autocomplete="email" required maxlength="254" />
      </label>
      <button class="button" type="submit" :disabled="loading">
        <Save :size="20" />
        <span>Запази email</span>
      </button>
    </form>
  </main>
</template>

<style scoped>
.account-page { display: grid; place-items: start center; }
.account-panel { display: grid; width: min(100%, 520px); gap: 16px; padding: 28px; }
.account-icon { color: var(--green); }
h1, p { margin: 0; }
.success { border-radius: var(--radius); padding: 12px 14px; color: #126348; background: #def7e9; font-weight: 700; }
</style>
