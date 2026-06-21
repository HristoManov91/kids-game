<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { Activity, AlertCircle, BarChart3, CheckCircle2, Gem, Gift, Home, Images, Info, LogOut, Palette, UserCog, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useQuizStore } from '@/stores/quiz'
import { useRewardsStore } from '@/stores/rewards'
import { isAdminUser } from '@/services/admin'

const auth = useAuthStore()
const quiz = useQuizStore()
const rewards = useRewardsStore()
const router = useRouter()
const isLoggedIn = computed(() => Boolean(auth.token))
const isAdmin = computed(() => isAdminUser(auth.user))
const navTooltip = ref<{ label: string, x: number, y: number } | null>(null)
const appToast = computed(() => {
  const raw = rewards.toast as unknown
  if (!raw) {
    return null
  }
  if (typeof raw === 'string') {
    const message = raw.trim()
    return message ? { message, type: 'success' as const } : null
  }
  if (typeof raw === 'object') {
    const payload = raw as { message?: unknown, type?: unknown }
    const message = typeof payload.message === 'string' ? payload.message.trim() : ''
    if (!message) {
      return null
    }
    const type = payload.type === 'error' || payload.type === 'info' || payload.type === 'success'
      ? payload.type
      : 'success'
    return { message, type }
  }
  return null
})

const toastIcon = computed(() => {
  if (appToast.value?.type === 'error') {
    return AlertCircle
  }
  if (appToast.value?.type === 'info') {
    return Info
  }
  return CheckCircle2
})

watch(
  () => auth.token,
  (token) => {
    if (token) {
      quiz.fetchTotalCrystals().catch(() => {
        quiz.totalCrystals = 0
      })
      return
    }
    quiz.totalCrystals = 0
  },
  { immediate: true }
)

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}

function openNavTooltip(event: MouseEvent | FocusEvent, label: string) {
  const target = event.currentTarget as HTMLElement | null
  if (!target) {
    return
  }
  const rect = target.getBoundingClientRect()
  navTooltip.value = {
    label,
    x: rect.left + rect.width / 2,
    y: rect.bottom + 8
  }
}

function closeNavTooltip() {
  navTooltip.value = null
}
</script>

<template>
  <div class="app-shell">
    <transition name="toast-pop">
      <aside v-if="appToast" class="app-toast" :class="`app-toast--${appToast.type}`" role="status" aria-live="polite">
        <component :is="toastIcon" :size="20" />
        <span>{{ appToast.message }}</span>
      </aside>
    </transition>
    <transition name="tooltip-pop">
      <aside
        v-if="navTooltip"
        class="floating-tooltip"
        :style="{ left: `${navTooltip.x}px`, top: `${navTooltip.y}px` }"
      >
        {{ navTooltip.label }}
      </aside>
    </transition>

    <header v-if="isLoggedIn" class="topbar">
      <RouterLink class="brand" to="/app" aria-label="Към началото">
        <span class="brand-mark"><UserRound :size="22" /></span>
        <span>
          <strong>{{ auth.user?.displayName }}</strong>
          <em class="crystal-pill"><Gem :size="14" /> {{ quiz.totalCrystals }} кристала</em>
        </span>
      </RouterLink>

      <nav class="topnav" aria-label="Главна навигация">
        <RouterLink
          class="icon-button"
          to="/account"
          title="Профил"
          aria-label="Профил"
          @mouseenter="openNavTooltip($event, 'Профил')"
          @mouseleave="closeNavTooltip"
          @focus="openNavTooltip($event, 'Профил')"
          @blur="closeNavTooltip"
        >
          <UserCog :size="20" />
        </RouterLink>
        <RouterLink
          class="icon-button"
          to="/app"
          title="Начало"
          aria-label="Начало"
          @mouseenter="openNavTooltip($event, 'Начало')"
          @mouseleave="closeNavTooltip"
          @focus="openNavTooltip($event, 'Начало')"
          @blur="closeNavTooltip"
        >
          <Home :size="20" />
        </RouterLink>
        <RouterLink
          class="icon-button"
          to="/report"
          title="Справка"
          aria-label="Справка"
          @mouseenter="openNavTooltip($event, 'Справка')"
          @mouseleave="closeNavTooltip"
          @focus="openNavTooltip($event, 'Справка')"
          @blur="closeNavTooltip"
        >
          <BarChart3 :size="20" />
        </RouterLink>
        <RouterLink
          class="icon-button"
          to="/rewards/album"
          title="Моят албум"
          aria-label="Моят албум"
          @mouseenter="openNavTooltip($event, 'Моят албум')"
          @mouseleave="closeNavTooltip"
          @focus="openNavTooltip($event, 'Моят албум')"
          @blur="closeNavTooltip"
        >
          <Palette :size="20" />
        </RouterLink>
        <RouterLink
          v-if="isAdmin"
          class="icon-button"
          to="/admin/monitoring"
          title="Мониторинг"
          aria-label="Мониторинг"
          @mouseenter="openNavTooltip($event, 'Мониторинг')"
          @mouseleave="closeNavTooltip"
          @focus="openNavTooltip($event, 'Мониторинг')"
          @blur="closeNavTooltip"
        >
          <Activity :size="20" />
        </RouterLink>
        <RouterLink
          v-if="isAdmin"
          class="icon-button"
          to="/admin/catalog"
          title="Каталог"
          aria-label="Каталог с думи"
          @mouseenter="openNavTooltip($event, 'Каталог с думи')"
          @mouseleave="closeNavTooltip"
          @focus="openNavTooltip($event, 'Каталог с думи')"
          @blur="closeNavTooltip"
        >
          <Images :size="20" />
        </RouterLink>
        <RouterLink
          v-if="isAdmin"
          class="icon-button"
          to="/admin/reward-catalog"
          title="Награди"
          aria-label="Награди"
          @mouseenter="openNavTooltip($event, 'Награди')"
          @mouseleave="closeNavTooltip"
          @focus="openNavTooltip($event, 'Награди')"
          @blur="closeNavTooltip"
        >
          <Gift :size="20" />
        </RouterLink>
        <button
          class="icon-button"
          type="button"
          title="Изход"
          aria-label="Изход"
          @mouseenter="openNavTooltip($event, 'Изход')"
          @mouseleave="closeNavTooltip"
          @focus="openNavTooltip($event, 'Изход')"
          @blur="closeNavTooltip"
          @click="logout"
        >
          <LogOut :size="20" />
        </button>
      </nav>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-toast {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 1200;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  max-width: min(420px, calc(100vw - 24px));
  border: 1px solid rgba(36, 48, 74, 0.14);
  border-radius: 12px;
  padding: 12px 14px;
  color: #ffffff;
  font-weight: 850;
  box-shadow: 0 14px 30px rgba(36, 48, 74, 0.24);
}

.app-toast--success {
  background: linear-gradient(135deg, #1e9d74, #167d5b);
}

.app-toast--info {
  background: linear-gradient(135deg, #3f7dd9, #2c5fb0);
}

.app-toast--error {
  background: linear-gradient(135deg, #d94b4b, #b43a3a);
}

.toast-pop-enter-active,
.toast-pop-leave-active {
  transition: all 180ms ease;
}

.toast-pop-enter-from,
.toast-pop-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}

.floating-tooltip {
  position: fixed;
  z-index: 1300;
  transform: translate(-50%, 0);
  border-radius: 10px;
  padding: 6px 10px;
  font-size: 0.82rem;
  font-weight: 800;
  line-height: 1;
  color: #ffffff;
  white-space: nowrap;
  background: rgba(36, 48, 74, 0.95);
  box-shadow: 0 10px 24px rgba(36, 48, 74, 0.26);
  pointer-events: none;
}

.tooltip-pop-enter-active,
.tooltip-pop-leave-active {
  transition: opacity 140ms ease, transform 140ms ease;
}

.tooltip-pop-enter-from,
.tooltip-pop-leave-to {
  opacity: 0;
  transform: translate(-50%, -6px);
}

.crystal-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--blue);
  font-size: 0.78rem;
  font-style: normal;
  font-weight: 950;
  line-height: 1.1;
}

@media (max-width: 760px) {
  .app-toast {
    top: 10px;
    right: 10px;
    left: 10px;
    max-width: none;
  }
}
</style>
