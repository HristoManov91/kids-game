<script setup lang="ts">
import type { PlacedRewardItem } from '@/types'

defineProps<{
  background: string
  items: PlacedRewardItem[]
  label: string
}>()

function itemStyle(item: PlacedRewardItem) {
  const mirror = item.mirrored ? ' scaleX(-1)' : ''
  return {
    left: `${item.x}%`,
    top: `${item.y}%`,
    transform: `translate(-50%, -50%) rotate(${item.rotation}deg) scale(${item.scale})${mirror}`,
    zIndex: item.zIndex
  }
}

function isImageAsset(image: string) {
  return image.startsWith('/') || image.startsWith('http') || image.startsWith('data:image')
}

function isMarvelAsset(image: string) {
  return image.includes('cdn.marvel.com/')
}

function hideBrokenImage(event: Event) {
  const target = event.target as HTMLImageElement | null
  if (!target) {
    return
  }
  target.style.display = 'none'
}
</script>

<template>
  <div class="scene-preview" :style="{ background }" :aria-label="label">
    <span
      v-for="item in items"
      :key="item.id"
      class="preview-item"
      :style="itemStyle(item)"
      aria-hidden="true"
    >
      <img
        v-if="isImageAsset(item.image)"
        :src="item.image"
        :alt="item.name"
        :class="{ marvel: isMarvelAsset(item.image) }"
        draggable="false"
        @error="hideBrokenImage"
      >
      <span v-else>{{ item.image }}</span>
    </span>
  </div>
</template>

<style scoped>
.scene-preview {
  position: relative;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  border-radius: var(--radius);
  border: 1px solid rgba(36, 48, 74, 0.12);
  box-shadow: inset 0 -34px 60px rgba(255, 255, 255, 0.18);
}

.preview-item {
  position: absolute;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  transform-origin: center;
  font-size: 2rem;
  line-height: 1;
  user-select: none;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
}

.preview-item img.marvel {
  object-fit: cover;
  object-position: left center;
}

.preview-item span {
  pointer-events: none;
}
</style>
