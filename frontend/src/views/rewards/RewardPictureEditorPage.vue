<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ArrowLeft, ChevronLeft, ChevronRight, FlipHorizontal, RotateCcw, RotateCw, Save, Trash2, ZoomIn, ZoomOut } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import RewardBalanceBadge from '@/components/rewards/RewardBalanceBadge.vue'
import RewardCanvas from '@/components/rewards/RewardCanvas.vue'
import RewardCatalogItemCard from '@/components/rewards/RewardCatalogItemCard.vue'
import { useRewardsStore } from '@/stores/rewards'

const route = useRoute()
const router = useRouter()
const rewards = useRewardsStore()
const selectedItemId = ref<string | null>(null)
const selectedCategory = ref('Всички')
const saving = ref(false)
const autosaving = ref(false)
const buying = ref('')
const deleting = ref(false)
const catalogScroller = ref<HTMLElement | null>(null)
const lastSavedSnapshot = ref('')
let autosaveTimer: number | null = null

const pictureId = computed(() => String(route.params.pictureId))
const picture = computed(() => rewards.currentPicture)
const themeItems = computed(() => picture.value ? rewards.itemsForTheme(picture.value.themeId) : [])
const categories = computed(() => ['Всички', ...Array.from(new Set(themeItems.value.map((item) => item.category)))])
const catalogItems = computed(() =>
  selectedCategory.value === 'Всички'
    ? themeItems.value
    : themeItems.value.filter((item) => item.category === selectedCategory.value)
)
const selectedItem = computed(() => picture.value?.placedItems.find((item) => item.id === selectedItemId.value) ?? null)
const hasUnsavedChanges = computed(() => {
  if (!picture.value) {
    return false
  }
  return buildSnapshot(picture.value) !== lastSavedSnapshot.value
})

onMounted(async () => {
  await rewards.bootstrap()
  await rewards.fetchPicture(pictureId.value)
  markSavedSnapshot()
  startAutosaveTimer()
  resetCatalogScroll()
})

onUnmounted(stopAutosaveTimer)

watch(selectedCategory, resetCatalogScroll)

async function save() {
  if (saving.value || autosaving.value) {
    return
  }
  saving.value = true
  try {
    await rewards.savePicture()
    markSavedSnapshot()
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Картината не се запази.'
    rewards.showToast(message, 'error')
  } finally {
    saving.value = false
  }
}

async function purchase(itemId: string) {
  buying.value = itemId
  try {
    const placed = await rewards.purchaseItem(itemId)
    selectedItemId.value = placed.id
    markSavedSnapshot()
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Не успяхме да добавим предмета.'
    rewards.showToast(message, 'error')
  } finally {
    buying.value = ''
  }
}

async function removeSelected(itemId?: string) {
  const targetItemId = itemId ?? selectedItem.value?.id
  if (!targetItemId || deleting.value) {
    return
  }
  deleting.value = true
  try {
    await rewards.removeItem(targetItemId)
    selectedItemId.value = null
    markSavedSnapshot()
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Не успяхме да махнем предмета.'
    rewards.showToast(message, 'error')
  } finally {
    deleting.value = false
  }
}

function scrollCatalog(direction: 'left' | 'right') {
  const scroller = catalogScroller.value
  if (!scroller) {
    return
  }
  const distance = Math.max(260, scroller.clientWidth * 0.85)
  scroller.scrollBy({
    left: direction === 'left' ? -distance : distance,
    behavior: 'smooth'
  })
}

function resetCatalogScroll() {
  window.requestAnimationFrame(() => {
    if (catalogScroller.value) {
      catalogScroller.value.scrollLeft = 0
    }
  })
}

function buildSnapshot(value: NonNullable<typeof picture.value>) {
  return JSON.stringify({
    name: value.name,
    placedItems: value.placedItems.map((item) => ({
      id: item.id,
      x: Number(item.x.toFixed(3)),
      y: Number(item.y.toFixed(3)),
      scale: Number(item.scale.toFixed(3)),
      rotation: Number(item.rotation.toFixed(3)),
      mirrored: Boolean(item.mirrored),
      zIndex: item.zIndex
    }))
  })
}

function markSavedSnapshot() {
  lastSavedSnapshot.value = picture.value ? buildSnapshot(picture.value) : ''
}

function startAutosaveTimer() {
  stopAutosaveTimer()
  autosaveTimer = window.setInterval(() => {
    void autosave()
  }, 60_000)
}

function stopAutosaveTimer() {
  if (autosaveTimer !== null) {
    window.clearInterval(autosaveTimer)
    autosaveTimer = null
  }
}

async function autosave() {
  if (!picture.value || saving.value || autosaving.value || !hasUnsavedChanges.value) {
    return
  }
  autosaving.value = true
  try {
    await rewards.savePicture({ toastMessage: 'Автоматично запазено.' })
    markSavedSnapshot()
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Автоматичното запазване не беше успешно.'
    rewards.showToast(message, 'error')
  } finally {
    autosaving.value = false
  }
}
</script>

<template>
  <section class="page reward-editor-page">
    <template v-if="picture">
      <section class="panel editor-toolbar">
        <div class="title-area">
          <button class="button secondary back-button" type="button" @click="router.push({ name: 'rewardAlbum' })">
            <ArrowLeft :size="20" />
            <span>Албум</span>
          </button>
          <div class="name-row">
            <span class="theme-chip">{{ picture.themeName }}</span>
            <label class="name-input-wrap">
              <input :value="picture.name" maxlength="40" @input="rewards.setPictureName(($event.target as HTMLInputElement).value)" />
            </label>
          </div>
        </div>
        <div class="toolbar-actions">
          <RewardBalanceBadge :crystals="rewards.balance.available" compact />
          <button class="button save-button" type="button" :disabled="saving || autosaving" @click="save">
            <Save :size="20" />
            <span>{{ autosaving ? 'Запазване...' : 'Запази' }}</span>
          </button>
        </div>
      </section>

      <div class="editor-layout">
        <section class="panel canvas-panel">
          <RewardCanvas
            :picture="picture"
            :selected-item-id="selectedItemId"
            @select="selectedItemId = $event"
            @move="rewards.moveItem"
            @resize="rewards.resizeItem"
            @rotate="rewards.rotateItem"
            @mirror="rewards.toggleMirror"
            @remove="removeSelected"
          />

          <div class="item-tools">
            <strong v-if="selectedItem">Инструменти: {{ selectedItem.name }}</strong>
            <strong v-else>Инструменти</strong>
            <div class="tool-buttons">
              <button
                type="button"
                class="tool-button"
                :disabled="!selectedItem"
                title="По-голямо"
                aria-label="По-голямо"
                data-tooltip="По-голямо"
                @click="selectedItem && rewards.resizeItem(selectedItem.id, 'up')"
              >
                <ZoomIn :size="20" />
              </button>
              <button
                type="button"
                class="tool-button"
                :disabled="!selectedItem"
                title="По-малко"
                aria-label="По-малко"
                data-tooltip="По-малко"
                @click="selectedItem && rewards.resizeItem(selectedItem.id, 'down')"
              >
                <ZoomOut :size="20" />
              </button>
              <button
                type="button"
                class="tool-button"
                :disabled="!selectedItem"
                title="Завърти наляво"
                aria-label="Завърти наляво"
                data-tooltip="Завърти наляво"
                @click="selectedItem && rewards.rotateItem(selectedItem.id, 'left')"
              >
                <RotateCcw :size="20" />
              </button>
              <button
                type="button"
                class="tool-button"
                :disabled="!selectedItem"
                title="Завърти надясно"
                aria-label="Завърти надясно"
                data-tooltip="Завърти надясно"
                @click="selectedItem && rewards.rotateItem(selectedItem.id, 'right')"
              >
                <RotateCw :size="20" />
              </button>
              <button
                type="button"
                class="tool-button"
                :disabled="!selectedItem"
                title="Огледално"
                aria-label="Огледално"
                data-tooltip="Огледално"
                @click="selectedItem && rewards.toggleMirror(selectedItem.id)"
              >
                <FlipHorizontal :size="20" />
              </button>
              <button
                type="button"
                class="tool-button danger"
                :disabled="!selectedItem || deleting"
                title="Премахни"
                aria-label="Премахни"
                data-tooltip="Премахни"
                @click="removeSelected()"
              >
                <Trash2 :size="20" />
              </button>
            </div>
          </div>

        </section>

        <section class="panel catalog-panel">
          <div class="panel-header">
            <h2 class="panel-title">Предмети</h2>
          </div>
          <div class="category-tabs">
            <button
              v-for="category in categories"
              :key="category"
              type="button"
              :class="{ active: selectedCategory === category }"
              @click="selectedCategory = category"
            >
              {{ category }}
            </button>
          </div>
          <div class="catalog-carousel">
            <button class="catalog-arrow" type="button" title="Предишни предмети" @click="scrollCatalog('left')">
              <ChevronLeft :size="28" />
            </button>
            <div ref="catalogScroller" class="catalog-strip" aria-label="Каталог с предмети">
              <RewardCatalogItemCard
                v-for="item in catalogItems"
                :key="item.id"
                :item="item"
                :crystals="rewards.balance.available"
                :busy="Boolean(buying)"
                @add="purchase"
              />
            </div>
            <button class="catalog-arrow" type="button" title="Следващи предмети" @click="scrollCatalog('right')">
              <ChevronRight :size="28" />
            </button>
          </div>
        </section>
      </div>
    </template>
  </section>
</template>

<style scoped>
.reward-editor-page {
  display: grid;
  gap: 18px;
  width: min(1480px, calc(100% - 20px));
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
}

.title-area,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.back-button,
.save-button {
  min-height: 46px;
  text-decoration: none;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: min(520px, 48vw);
}

.theme-chip {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid rgba(26, 161, 119, 0.26);
  border-radius: 12px;
  color: var(--green-dark);
  background: rgba(26, 161, 119, 0.1);
  font-size: 1rem;
  font-weight: 900;
}

.name-input-wrap {
  flex: 1 1 auto;
}

.name-input-wrap input {
  flex: 1 1 auto;
  width: 100%;
  min-height: 46px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 0 13px;
  color: var(--ink);
  background: #ffffff;
  font-size: 1.15rem;
  font-weight: 900;
  outline: none;
}

.name-input-wrap input:focus {
  border-color: var(--blue);
  box-shadow: 0 0 0 3px rgba(63, 125, 217, 0.16);
}

.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.canvas-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.item-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 12px;
  background: #ffffff;
}

.item-tools strong {
  color: var(--ink);
  font-weight: 950;
}

.tool-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tool-button {
  position: relative;
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border: 1px solid rgba(36, 48, 74, 0.14);
  border-radius: 12px;
  color: var(--ink);
  background: #ffffff;
}

.tool-button::after {
  content: attr(data-tooltip);
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  transform: translateX(-50%);
  white-space: nowrap;
  border-radius: 8px;
  padding: 6px 8px;
  color: #ffffff;
  background: rgba(36, 48, 74, 0.95);
  font-size: 0.78rem;
  font-weight: 800;
  opacity: 0;
  pointer-events: none;
  transition: opacity 120ms ease;
}

.tool-button:hover::after,
.tool-button:focus-visible::after {
  opacity: 1;
}

.tool-button:disabled {
  opacity: 1;
}

.tool-button.danger {
  color: var(--danger);
}

.catalog-panel {
  display: grid;
  gap: 12px;
  overflow: hidden;
  padding-bottom: 0;
}

.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 0 18px 4px;
}

.category-tabs button {
  min-height: 38px;
  flex: 0 0 auto;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: 999px;
  padding: 0 12px;
  color: var(--muted);
  background: #ffffff;
  font-weight: 900;
}

.category-tabs button.active {
  color: #ffffff;
  background: var(--green);
}

.catalog-carousel {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 54px;
  gap: 10px;
  align-items: stretch;
  padding: 0 18px 18px;
}

.catalog-arrow {
  display: grid;
  min-height: 158px;
  place-items: center;
  border: 1px solid rgba(36, 48, 74, 0.14);
  border-radius: var(--radius);
  color: var(--ink);
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(36, 48, 74, 0.06);
}

.catalog-arrow:hover {
  border-color: rgba(26, 161, 119, 0.42);
  color: var(--green-dark);
}

.catalog-strip {
  display: grid;
  grid-auto-columns: minmax(220px, 240px);
  grid-auto-flow: column;
  gap: 12px;
  min-width: 0;
  overflow-x: hidden;
  padding: 4px 2px 8px;
  scroll-behavior: smooth;
}

@media (max-width: 760px) {
  .editor-toolbar,
  .title-area,
  .toolbar-actions,
  .item-tools {
    display: grid;
    justify-items: stretch;
  }

  .name-row {
    min-width: 0;
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  .theme-chip {
    justify-content: center;
  }

  .catalog-carousel {
    grid-template-columns: 44px minmax(0, 1fr) 44px;
    padding-inline: 12px;
  }

  .catalog-arrow {
    min-height: 146px;
  }

  .catalog-strip {
    grid-auto-columns: minmax(190px, 78vw);
  }
}
</style>
