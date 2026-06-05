<script setup lang="ts">
import { Gem } from 'lucide-vue-next'

defineProps<{
  crystals: number
  compact?: boolean
}>()
</script>

<template>
  <span class="reward-balance" :class="{ compact }">
    <Gem :size="compact ? 17 : 22" />
    <strong>{{ crystals }}</strong>
    <span>кристала</span>
  </span>
</template>

<style scoped>
.reward-balance {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid rgba(63, 125, 217, 0.18);
  border-radius: 999px;
  padding: 10px 14px;
  color: var(--blue);
  background: #ffffff;
  font-weight: 950;
  box-shadow: 0 10px 24px rgba(36, 48, 74, 0.08);
}

.reward-balance strong {
  color: var(--ink);
}

.reward-balance.compact {
  padding: 7px 10px;
  font-size: 0.9rem;
}
</style>
