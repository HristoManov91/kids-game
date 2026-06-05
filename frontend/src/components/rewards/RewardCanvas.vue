<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { FlipHorizontal, RotateCcw, RotateCw, Trash2, ZoomIn, ZoomOut } from 'lucide-vue-next'
import type { AlbumPictureDetail, PlacedRewardItem } from '@/types'

const props = defineProps<{
  picture: AlbumPictureDetail
  selectedItemId: string | null
}>()

const emit = defineEmits<{
  select: [itemId: string | null]
  move: [itemId: string, x: number, y: number]
  resize: [itemId: string, direction: 'up' | 'down']
  rotate: [itemId: string, direction: 'left' | 'right']
  mirror: [itemId: string]
  remove: [itemId: string]
}>()

const canvasRef = ref<HTMLElement | null>(null)
const dragState = ref<{ itemId: string, offsetX: number, offsetY: number } | null>(null)
const selectedItem = computed(() => props.picture.placedItems.find((item) => item.id === props.selectedItemId) ?? null)
const selectedToolStyle = computed(() => {
  if (!selectedItem.value) {
    return undefined
  }
  return {
    left: `${clamp(selectedItem.value.x, 9, 91)}%`,
    top: `${clamp(selectedItem.value.y - 12, 8, 92)}%`
  }
})

function itemStyle(item: PlacedRewardItem) {
  const mirror = item.mirrored ? ' scaleX(-1)' : ''
  return {
    left: `${item.x}%`,
    top: `${item.y}%`,
    transform: `translate(-50%, -50%) rotate(${item.rotation}deg) scale(${item.scale})${mirror}`,
    zIndex: item.zIndex
  }
}

function startDrag(event: PointerEvent, item: PlacedRewardItem) {
  event.preventDefault()
  event.stopPropagation()
  const point = pointInCanvas(event)
  dragState.value = {
    itemId: item.id,
    offsetX: item.x - point.x,
    offsetY: item.y - point.y
  }
  emit('select', item.id)
  window.addEventListener('pointermove', moveDrag)
  window.addEventListener('pointerup', stopDrag)
  window.addEventListener('pointercancel', stopDrag)
}

function moveDrag(event: PointerEvent) {
  if (!dragState.value) {
    return
  }
  const point = pointInCanvas(event)
  emit(
    'move',
    dragState.value.itemId,
    clamp(point.x + dragState.value.offsetX, 0, 100),
    clamp(point.y + dragState.value.offsetY, 0, 100)
  )
}

function stopDrag() {
  dragState.value = null
  window.removeEventListener('pointermove', moveDrag)
  window.removeEventListener('pointerup', stopDrag)
  window.removeEventListener('pointercancel', stopDrag)
}

function pointInCanvas(event: PointerEvent) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) {
    return { x: 50, y: 50 }
  }
  return {
    x: ((event.clientX - rect.left) / rect.width) * 100,
    y: ((event.clientY - rect.top) / rect.height) * 100
  }
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value))
}

function isImageAsset(image: string) {
  return image.startsWith('/') || image.startsWith('http') || image.startsWith('data:image')
}

onUnmounted(stopDrag)
</script>

<template>
  <div
    ref="canvasRef"
    class="reward-canvas"
    :style="{ background: picture.backgroundImage }"
    @pointerdown.self="emit('select', null)"
  >
    <button
      v-for="item in picture.placedItems"
      :key="item.id"
      type="button"
      class="placed-item"
      :class="{ selected: selectedItemId === item.id }"
      :style="itemStyle(item)"
      :aria-label="item.name"
      @pointerdown="startDrag($event, item)"
      @click.stop="emit('select', item.id)"
    >
      <img v-if="isImageAsset(item.image)" :src="item.image" :alt="item.name" draggable="false">
      <span v-else>{{ item.image }}</span>
    </button>

    <div
      v-if="selectedItem"
      class="floating-tools"
      :style="selectedToolStyle"
      role="toolbar"
      aria-label="Инструменти за избран предмет"
      @pointerdown.stop
      @click.stop
    >
      <button type="button" class="floating-tool" title="По-голямо" @click="emit('resize', selectedItem.id, 'up')">
        <ZoomIn :size="16" />
      </button>
      <button type="button" class="floating-tool" title="По-малко" @click="emit('resize', selectedItem.id, 'down')">
        <ZoomOut :size="16" />
      </button>
      <button type="button" class="floating-tool" title="Завърти наляво" @click="emit('rotate', selectedItem.id, 'left')">
        <RotateCcw :size="16" />
      </button>
      <button type="button" class="floating-tool" title="Завърти надясно" @click="emit('rotate', selectedItem.id, 'right')">
        <RotateCw :size="16" />
      </button>
      <button type="button" class="floating-tool" title="Огледално" @click="emit('mirror', selectedItem.id)">
        <FlipHorizontal :size="16" />
      </button>
      <button type="button" class="floating-tool floating-tool-danger" title="Премахни" @click="emit('remove', selectedItem.id)">
        <Trash2 :size="16" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.reward-canvas {
  position: relative;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  min-height: 420px;
  border: 3px solid rgba(36, 48, 74, 0.12);
  border-radius: 10px;
  box-shadow:
    inset 0 -80px 100px rgba(255, 255, 255, 0.18),
    0 16px 32px rgba(36, 48, 74, 0.12);
  touch-action: none;
}

.placed-item {
  position: absolute;
  display: grid;
  width: 94px;
  height: 94px;
  place-items: center;
  border: 0;
  border-radius: 18px;
  padding: 0;
  background: transparent;
  font-size: clamp(2.45rem, 4.1vw, 4.8rem);
  line-height: 0.98;
  text-align: center;
  transform-origin: center;
  cursor: grab;
  user-select: none;
  white-space: normal;
  touch-action: none;
}

.placed-item img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
}

.placed-item span {
  pointer-events: none;
}

.placed-item:active {
  cursor: grabbing;
}

.placed-item.selected {
  outline: 4px solid rgba(63, 125, 217, 0.8);
  outline-offset: 8px;
  background: rgba(255, 255, 255, 0.42);
}

.floating-tools {
  position: absolute;
  z-index: 999;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transform: translate(-50%, -100%);
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: 12px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 10px 24px rgba(36, 48, 74, 0.18);
}

.floating-tool {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: 8px;
  color: var(--ink);
  background: #ffffff;
}

.floating-tool:hover {
  border-color: rgba(26, 161, 119, 0.42);
  color: var(--green-dark);
}

.floating-tool-danger {
  color: var(--danger);
}

@media (max-width: 760px) {
  .reward-canvas {
    min-height: 300px;
  }

  .placed-item {
    width: 68px;
    height: 68px;
  }

  .floating-tools {
    gap: 4px;
    padding: 5px;
  }

  .floating-tool {
    width: 28px;
    height: 28px;
  }
}
</style>
