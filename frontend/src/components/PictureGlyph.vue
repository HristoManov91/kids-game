<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  image?: string | null
}>()

const focusImage = computed(() => {
  const raw = props.image ?? ''
  if (!raw.startsWith('focus:')) {
    return null
  }
  const parts = raw.slice('focus:'.length).split('|')
  const target = Number(parts.at(-1))
  const symbols = parts.slice(0, -1).filter(Boolean)
  if (!Number.isInteger(target) || target < 0 || target >= symbols.length || symbols.length === 0) {
    return null
  }
  return { symbols, target }
})
</script>

<template>
  <span v-if="focusImage" class="picture-glyph focused-picture">
    <span
      v-for="(symbol, symbolIndex) in focusImage.symbols"
      :key="`${symbol}-${symbolIndex}`"
      class="picture-part"
      :class="{ target: symbolIndex === focusImage.target }"
    >
      {{ symbol }}
    </span>
  </span>
  <span v-else class="picture-glyph">{{ image }}</span>
</template>

<style scoped>
.picture-glyph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.12em;
  line-height: 1;
}

.focused-picture {
  gap: 0.18em;
}

.picture-part {
  display: inline-grid;
  min-width: 1em;
  min-height: 1em;
  place-items: center;
}

.picture-part.target {
  position: relative;
}

.picture-part.target::before {
  position: absolute;
  inset: -0.14em -0.16em;
  content: "";
  border: 0.075em solid #1e9d74;
  border-radius: 999px;
  box-shadow: 0 0 0 0.05em rgba(255, 255, 255, 0.92);
}
</style>
