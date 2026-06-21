<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LogIn, Sparkles, UserPlus } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const showDemoLogins = import.meta.env.VITE_SHOW_DEMO_LOGINS === 'true'
const username = ref('')
const password = ref('')
const error = ref('')

async function submit() {
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/app'
    router.push(redirect)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Неуспешен вход.'
  }
}

function fillDemo(nextUsername: string, nextPassword: string) {
  username.value = nextUsername
  password.value = nextPassword
}
</script>

<template>
  <section class="login-page">
    <div class="login-copy">
      <div class="login-badge">
        <Sparkles :size="18" />
        <span>Учене с награди</span>
      </div>
      <h1>Задачки за малки шампиони</h1>
      <p>Решавай задачи, печели кристали и създавай собствени албуми с картинки.</p>
    </div>

    <form class="login-panel panel" @submit.prevent="submit">
      <h2>Вход</h2>
      <p class="muted">Влез с профил или създай нов акаунт.</p>

      <div v-if="error" class="error">{{ error }}</div>

      <label class="field">
        <span>Акаунт</span>
        <input v-model.trim="username" autocomplete="username" required />
      </label>

      <label class="field">
        <span>Парола</span>
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>

      <RouterLink class="forgot-link" to="/forgot-password">Забравена парола?</RouterLink>

      <button class="button" type="submit" :disabled="auth.loading">
        <LogIn :size="20" />
        <span>Влез</span>
      </button>

      <RouterLink class="button secondary register-button" to="/register">
        <UserPlus :size="20" />
        <span>Създай нов акаунт</span>
      </RouterLink>

      <div v-if="showDemoLogins" class="demo-logins" aria-label="Бърз избор на локален профил">
        <button type="button" @click="fillDemo('mila', 'mila123')">Мила</button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 420px);
  align-items: center;
  gap: clamp(24px, 6vw, 72px);
  width: min(1240px, calc(100% - 20px));
  margin: 0 auto;
  padding: 40px 0;
}

.login-copy {
  display: grid;
  gap: 18px;
}

.login-badge {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 9px 13px;
  color: #13634e;
  background: #def7e9;
  font-weight: 800;
}

h1 {
  max-width: 680px;
  margin: 0;
  font-size: clamp(2.3rem, 7vw, 5rem);
  line-height: 1;
}

.login-copy p {
  max-width: 520px;
  margin: 0;
  color: var(--muted);
  font-size: clamp(1.05rem, 2vw, 1.35rem);
}

.login-panel {
  display: grid;
  gap: 16px;
  padding: 26px;
}

.login-panel h2,
.login-panel p {
  margin: 0;
}

.demo-logins {
  display: grid;
  gap: 10px;
}

.demo-logins button {
  min-height: 40px;
  border: 1px solid rgba(36, 48, 74, 0.14);
  border-radius: var(--radius);
  color: var(--ink);
  background: #ffffff;
  font-weight: 800;
}

.register-button {
  width: 100%;
  text-decoration: none;
}

.forgot-link {
  justify-self: end;
  margin-top: -8px;
  color: var(--blue);
  font-weight: 800;
}

@media (max-width: 800px) {
  .login-page {
    grid-template-columns: 1fr;
    align-content: center;
  }

  h1 {
    font-size: clamp(2.2rem, 13vw, 3.9rem);
  }
}
</style>
