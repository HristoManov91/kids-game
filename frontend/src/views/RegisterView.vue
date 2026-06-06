<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { UserPlus, Sparkles } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const username = ref('')
const password = ref('')
const repeatPassword = ref('')
const error = ref('')

async function submit() {
  error.value = ''
  if (password.value !== repeatPassword.value) {
    error.value = 'Паролите не съвпадат.'
    return
  }
  try {
    await auth.register(username.value, password.value, repeatPassword.value)
    router.push({ name: 'dashboard' })
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Регистрацията не беше успешна.'
  }
}
</script>

<template>
  <section class="login-page">
    <div class="login-copy">
      <div class="login-badge">
        <Sparkles :size="18" />
        <span>Нов профил</span>
      </div>
      <h1>Създай акаунт</h1>
      <p>Нужни са само акаунт и парола. След това започваш веднага.</p>
    </div>

    <form class="login-panel panel" @submit.prevent="submit">
      <h2>Регистрация</h2>
      <p class="muted">Акаунтът трябва да е между 3 и 60 символа. Паролата трябва да е между 4 и 120 символа.</p>

      <div v-if="error" class="error">{{ error }}</div>

      <label class="field">
        <span>Акаунт</span>
        <input v-model.trim="username" autocomplete="username" required minlength="3" maxlength="60" />
      </label>

      <label class="field">
        <span>Парола</span>
        <input v-model="password" type="password" autocomplete="new-password" required minlength="4" maxlength="120" />
      </label>

      <label class="field">
        <span>Повтори парола</span>
        <input v-model="repeatPassword" type="password" autocomplete="new-password" required minlength="4" maxlength="120" />
      </label>

      <button class="button" type="submit" :disabled="auth.loading">
        <UserPlus :size="20" />
        <span>Създай акаунт</span>
      </button>

      <RouterLink class="secondary-link" to="/login">Вече имам акаунт</RouterLink>
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

.secondary-link {
  justify-self: center;
  color: var(--blue);
  font-weight: 900;
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
