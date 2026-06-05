<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Eraser, Gem, Pencil, Save, Search, Trash2 } from 'lucide-vue-next'
import { api } from '@/services/api'
import type { AdminRewardCatalogItem, DeleteRewardCatalogItemResponse, RewardCatalogResponse, RewardTheme } from '@/types'

interface RewardItemForm {
  themeIds: string[]
  category: string
  name: string
  price: string
  imageUrl: string
  defaultScale: number
  minScale: number
  maxScale: number
}

const items = ref<AdminRewardCatalogItem[]>([])
const themes = ref<RewardTheme[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')
const search = ref('')
const themeFilter = ref('ALL')
const categoryFilter = ref('ALL')
const currentPage = ref(1)
const pageSize = ref(12)
const selectedFile = ref<File | null>(null)
const selectedFilePreview = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const editingItemId = ref<string | null>(null)
const form = reactive<RewardItemForm>(emptyForm())
const defaultItemScale = 1
const minItemScale = 0.35
const maxItemScale = 2.5

const themeOptions = computed(() => themes.value)
const formTitle = computed(() => editingItemId.value ? 'Редакция на предмет' : 'Нов предмет')
const selectedThemes = computed(() => themes.value.filter((theme) => form.themeIds.includes(theme.id)))
const formCategoryOptions = computed(() => {
  const categories = new Set<string>()
  selectedThemes.value.forEach((theme) => theme.categories.forEach((category) => categories.add(category)))
  return categories.size > 0 ? [...categories] : categoryOptions.value
})
const previewImage = computed(() => selectedFilePreview.value || form.imageUrl)
const categoryOptions = computed(() => {
  const categories = new Set(items.value.map((item) => item.category).filter(Boolean))
  return [...categories].sort((a, b) => a.localeCompare(b, 'bg-BG'))
})
const filteredItems = computed(() => {
  const query = search.value.trim().toLocaleLowerCase('bg-BG')
  return items.value.filter((item) => {
    const matchesQuery =
      !query ||
      item.name.toLocaleLowerCase('bg-BG').includes(query) ||
      item.category.toLocaleLowerCase('bg-BG').includes(query) ||
      item.id.toLocaleLowerCase('bg-BG').includes(query)
    const matchesTheme = themeFilter.value === 'ALL' || itemThemeIds(item).includes(themeFilter.value)
    const matchesCategory = categoryFilter.value === 'ALL' || item.category === categoryFilter.value
    return matchesQuery && matchesTheme && matchesCategory
  })
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / pageSize.value)))
const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})
const pageStart = computed(() => filteredItems.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1)
const pageEnd = computed(() => Math.min(currentPage.value * pageSize.value, filteredItems.value.length))

onMounted(load)
onBeforeUnmount(clearSelectedFilePreview)

watch(themeOptions, (value) => {
  if (form.themeIds.length === 0 && value.length > 0) {
    form.themeIds = [value[0].id]
  }
})

watch([search, themeFilter, categoryFilter, pageSize], () => {
  currentPage.value = 1
})

watch(formCategoryOptions, (options) => {
  if (options.length > 0 && !options.includes(form.category)) {
    form.category = options[0]
  }
})

watch(totalPages, (value) => {
  if (currentPage.value > value) {
    currentPage.value = value
  }
})

function emptyForm(): RewardItemForm {
  return {
    themeIds: ['forest-meadow'],
    category: 'Предмети',
    name: '',
    price: '10',
    imageUrl: '',
    defaultScale: defaultItemScale,
    minScale: minItemScale,
    maxScale: maxItemScale
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [catalog, adminItems] = await Promise.all([
      api.get<RewardCatalogResponse>('/rewards/catalog'),
      api.get<AdminRewardCatalogItem[]>('/admin/reward-catalog')
    ])
    themes.value = catalog.themes
    items.value = adminItems
    if (form.themeIds.length === 0 && catalog.themes.length > 0) {
      form.themeIds = [catalog.themes[0].id]
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Каталогът не се зареди.'
  } finally {
    loading.value = false
  }
}

function edit(item: AdminRewardCatalogItem) {
  editingItemId.value = item.id
  form.themeIds = itemThemeIds(item)
  form.category = item.category
  form.name = item.name
  form.price = String(item.price)
  form.imageUrl = item.image
  form.defaultScale = item.defaultScale
  form.minScale = item.minScale
  form.maxScale = item.maxScale
  selectedFile.value = null
  clearSelectedFilePreview()
  if (fileInput.value) {
    fileInput.value.value = ''
  }
  success.value = ''
  error.value = ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function resetForm() {
  editingItemId.value = null
  Object.assign(form, emptyForm())
  if (themeOptions.value.length > 0) {
    form.themeIds = [themeOptions.value[0].id]
  }
  selectedFile.value = null
  clearSelectedFilePreview()
  if (fileInput.value) {
    fileInput.value.value = ''
  }
  error.value = ''
  success.value = ''
}

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  selectedFile.value = target.files?.[0] ?? null
  clearSelectedFilePreview()
  if (selectedFile.value) {
    selectedFilePreview.value = URL.createObjectURL(selectedFile.value)
  }
}

function onPriceInput(event: Event) {
  const target = event.target as HTMLInputElement
  let value = target.value.replace(/[^\d,]/g, '')
  const commaIndex = value.indexOf(',')

  if (commaIndex !== -1) {
    const whole = value.slice(0, commaIndex)
    const decimal = value.slice(commaIndex + 1).replace(/,/g, '').slice(0, 2)
    value = `${whole},${decimal}`
  }

  form.price = value
  target.value = value
}

async function save() {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const validationError = validateForm()
    if (validationError) {
      error.value = validationError
      return
    }
    const body = new FormData()
    form.themeIds.forEach((themeId) => body.append('themeIds', themeId))
    body.append('themeId', form.themeIds[0] ?? '')
    body.append('category', form.category)
    body.append('name', form.name.trim())
    body.append('price', normalizedPrice())
    body.append('defaultScale', String(form.defaultScale))
    body.append('minScale', String(form.minScale))
    body.append('maxScale', String(form.maxScale))
    if (selectedFile.value) {
      body.append('file', selectedFile.value)
    }

    if (editingItemId.value) {
      await api.putForm<AdminRewardCatalogItem>(`/admin/reward-catalog/${editingItemId.value}`, body)
      success.value = 'Предметът е обновен.'
    } else {
      await api.postForm<AdminRewardCatalogItem>('/admin/reward-catalog', body)
      success.value = 'Предметът е добавен.'
      resetForm()
    }
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Предметът не беше запазен.'
  } finally {
    saving.value = false
  }
}

async function remove(item: AdminRewardCatalogItem) {
  if (!window.confirm(`Да премахна ли "${item.name}"? Ако е използван, кристалите ще се върнат.`)) {
    return
  }
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const response = await api.delete<DeleteRewardCatalogItemResponse>(`/admin/reward-catalog/${item.id}`)
    success.value = `Премахнато: ${response.itemName}. Върнати: ${response.refundedCrystals} кристала.`
    if (editingItemId.value === item.id) {
      resetForm()
    }
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Предметът не беше премахнат.'
  } finally {
    saving.value = false
  }
}

function themeName(themeId: string) {
  return themes.value.find((theme) => theme.id === themeId)?.name ?? themeId
}

function itemThemeIds(item: AdminRewardCatalogItem) {
  return item.themeIds?.length ? item.themeIds : [item.themeId]
}

function itemThemeNames(item: AdminRewardCatalogItem) {
  return itemThemeIds(item).map(themeName).join(', ')
}

function isImageAsset(image: string) {
  return image.startsWith('/') || image.startsWith('http') || image.startsWith('data:image') || image.startsWith('blob:')
}

function previousPage() {
  currentPage.value = Math.max(1, currentPage.value - 1)
}

function nextPage() {
  currentPage.value = Math.min(totalPages.value, currentPage.value + 1)
}

function normalizedPrice() {
  return form.price.trim()
}

function validateForm() {
  if (form.themeIds.length === 0) {
    return 'Избери поне един свят.'
  }
  if (!form.category) {
    return 'Избери категория.'
  }
  const name = form.name.trim()
  if (!name) {
    return 'Въведи име.'
  }
  const duplicated = items.value.some((item) =>
    item.id !== editingItemId.value &&
    itemThemeIds(item).some((themeId) => form.themeIds.includes(themeId)) &&
    item.name.trim().toLocaleLowerCase('bg-BG') === name.toLocaleLowerCase('bg-BG')
  )
  if (duplicated) {
    return 'Вече има предмет с това име в избрания свят.'
  }
  const price = normalizedPrice()
  if (!/^\d+(,\d{1,2})?$/.test(price)) {
    return 'Цената може да съдържа само цифри и десетична запетая.'
  }
  const priceNumber = Number(price.replace(',', '.'))
  if (!Number.isFinite(priceNumber) || priceNumber <= 0) {
    return 'Цената трябва да е по-голяма от 0.'
  }
  if (!Number.isInteger(priceNumber)) {
    return 'Цената е в цели кристали. Може например 10 или 10,00.'
  }
  if (!selectedFile.value) {
    return 'Качи картинка от компютър.'
  }
  return ''
}

function clearSelectedFilePreview() {
  if (selectedFilePreview.value) {
    URL.revokeObjectURL(selectedFilePreview.value)
    selectedFilePreview.value = ''
  }
}
</script>

<template>
  <section class="page admin-reward-page">
    <section class="panel hero-panel">
      <div>
        <h1>Наградни предмети</h1>
      </div>
    </section>

    <div class="admin-reward-layout">
      <section class="panel form-panel">
        <div class="panel-header">
          <h2 class="panel-title">{{ formTitle }}</h2>
        </div>

        <div class="preview-box">
          <img v-if="isImageAsset(previewImage)" :src="previewImage" :alt="form.name || 'Предмет'" draggable="false">
          <span v-else>{{ previewImage || '🖼️' }}</span>
        </div>

        <div class="form-grid">
          <div class="wide-field form-field">
            <span>Светове</span>
            <div class="theme-multi-picker" role="group" aria-label="Светове за предмета">
              <label
                v-for="theme in themeOptions"
                :key="theme.id"
                class="theme-choice"
                :class="{ active: form.themeIds.includes(theme.id) }"
              >
                <input v-model="form.themeIds" type="checkbox" :value="theme.id">
                <span>{{ theme.name }}</span>
              </label>
            </div>
          </div>
          <label>
            <span>Категория</span>
            <select v-model="form.category" required>
              <option v-for="category in formCategoryOptions" :key="category" :value="category">{{ category }}</option>
            </select>
          </label>
          <label>
            <span>Име</span>
            <input v-model.trim="form.name" maxlength="120" required>
          </label>
          <label>
            <span>Цена</span>
            <input v-model.trim="form.price" inputmode="decimal" required placeholder="Например: 30" @input="onPriceInput">
          </label>
          <label class="wide-field file-field">
            <span>Качи картинка от компютър</span>
            <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/webp" required @change="onFileChange">
            <small v-if="selectedFile">{{ selectedFile.name }}</small>
          </label>
        </div>

        <p v-if="error" class="message error-message">{{ error }}</p>
        <p v-if="success" class="message success-message">{{ success }}</p>

        <div class="form-actions">
          <button class="button" type="button" :disabled="saving" @click="save">
            <Save :size="20" />
            <span>Запази</span>
          </button>
          <button class="button secondary" type="button" :disabled="saving" @click="resetForm">
            <Eraser :size="20" />
            <span>Изчисти</span>
          </button>
        </div>
      </section>

      <section class="panel list-panel">
        <div class="panel-header">
          <h2 class="panel-title">Каталог</h2>
          <span v-if="loading">Зарежда...</span>
        </div>

        <div class="filters-row">
          <label class="search-field">
            <Search :size="18" />
            <input v-model="search" placeholder="Търси предмет">
          </label>
          <select v-model="themeFilter" class="field-control">
            <option value="ALL">Всички светове</option>
            <option v-for="theme in themes" :key="theme.id" :value="theme.id">{{ theme.name }}</option>
          </select>
          <select v-model="categoryFilter" class="field-control">
            <option value="ALL">Всички категории</option>
            <option v-for="category in categoryOptions" :key="category" :value="category">{{ category }}</option>
          </select>
        </div>

        <div class="reward-items-grid">
          <article v-for="item in paginatedItems" :key="item.id" class="reward-admin-card">
            <div class="admin-item-art">
              <img v-if="isImageAsset(item.image)" :src="item.image" :alt="item.name" draggable="false">
              <span v-else>{{ item.image }}</span>
            </div>
            <div class="admin-item-body">
              <strong>{{ item.name }}</strong>
              <span>{{ itemThemeNames(item) }} · {{ item.category }}</span>
              <span class="admin-price-line">
                <span class="price-chip">{{ item.price }} <Gem :size="15" /></span>
                <span>използван {{ item.usedCount }} пъти</span>
              </span>
            </div>
            <div class="admin-card-actions">
              <button class="icon-button tooltip-button" type="button" title="Редактирай" aria-label="Редактирай" data-tooltip="Редактирай" @click="edit(item)">
                <Pencil :size="18" />
                <span class="sr-only">Редактирай</span>
              </button>
              <button class="icon-button danger tooltip-button" type="button" title="Премахни" aria-label="Премахни" data-tooltip="Премахни" @click="remove(item)">
                <Trash2 :size="18" />
                <span class="sr-only">Премахни</span>
              </button>
            </div>
          </article>
        </div>

        <div class="pagination-row">
          <span>{{ pageStart }}-{{ pageEnd }} от {{ filteredItems.length }}</span>
          <label class="page-size-field">
            <span>На страница</span>
            <select v-model.number="pageSize" class="field-control">
              <option :value="12">12</option>
              <option :value="24">24</option>
              <option :value="48">48</option>
            </select>
          </label>
          <div class="pagination-actions">
            <button class="button secondary small-button" type="button" :disabled="currentPage === 1" @click="previousPage">Назад</button>
            <strong>{{ currentPage }}/{{ totalPages }}</strong>
            <button class="button secondary small-button" type="button" :disabled="currentPage === totalPages" @click="nextPage">Напред</button>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.admin-reward-page {
  display: grid;
  gap: 18px;
  width: min(1480px, calc(100% - 20px));
}

.hero-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;
}

.hero-panel h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(2.2rem, 5vw, 4.2rem);
  line-height: 0.98;
}

.admin-reward-layout {
  display: grid;
  grid-template-columns: minmax(380px, 440px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.form-panel,
.list-panel {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.preview-box {
  display: grid;
  min-height: 190px;
  place-items: center;
  border: 1px dashed rgba(36, 48, 74, 0.22);
  border-radius: var(--radius);
  background: #ffffff;
  font-size: 5rem;
}

.preview-box img,
.admin-item-art img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-grid label,
.form-grid .form-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.form-grid input,
.form-grid select,
.field-control {
  width: 100%;
  min-width: 0;
  min-height: 48px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: 12px;
  padding: 0 14px;
  background: #ffffff;
  color: var(--ink);
  font: inherit;
  font-weight: 800;
  outline: none;
  box-shadow: 0 1px 0 rgba(36, 48, 74, 0.05);
  transition: border-color 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.form-grid input:focus,
.form-grid select:focus,
.field-control:focus,
.search-field:focus-within {
  border-color: rgba(63, 125, 217, 0.72);
  box-shadow: 0 0 0 4px rgba(63, 125, 217, 0.12);
}

.form-grid input::placeholder,
.search-field input::placeholder {
  color: rgba(107, 119, 145, 0.82);
  font-weight: 750;
}

.form-grid > label > span,
.form-grid > .form-field > span,
.file-field small {
  color: var(--muted);
  font-weight: 850;
}

.theme-multi-picker {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 8px;
}

.theme-choice {
  position: relative;
  display: grid;
  align-items: center;
  justify-items: center;
  min-height: 46px;
  border: 2px solid rgba(36, 48, 74, 0.14);
  border-radius: 12px;
  padding: 0 14px;
  background: #ffffff;
  color: var(--ink);
  text-align: center;
  font-weight: 850;
  cursor: pointer;
  user-select: none;
  transition: border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease, color 0.16s ease;
}

.theme-choice.active {
  border-color: rgba(31, 158, 118, 0.78);
  background: #eefaf3;
  color: var(--green-dark);
  box-shadow: 0 0 0 3px rgba(31, 158, 118, 0.1);
}

.theme-choice:focus-within {
  border-color: rgba(63, 125, 217, 0.72);
  box-shadow: 0 0 0 4px rgba(63, 125, 217, 0.12);
}

.theme-choice input {
  position: absolute;
  inset: 0;
  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 0;
  border: 0;
  margin: 0;
  padding: 0;
  opacity: 0;
  cursor: pointer;
}

.theme-choice span {
  overflow-wrap: anywhere;
  pointer-events: none;
}

.wide-field {
  grid-column: 1 / -1;
}

.file-field input {
  border-style: dashed;
  min-height: 62px;
  padding: 13px;
}

.file-field input::file-selector-button {
  margin-right: 10px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: 10px;
  padding: 9px 12px;
  color: var(--ink);
  background: #ffffff;
  font: inherit;
  font-weight: 850;
}

.form-actions,
.filters-row,
.admin-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.message {
  margin: 0;
  border-radius: var(--radius);
  padding: 12px;
  font-weight: 850;
}

.error-message {
  color: #8a3900;
  background: rgba(245, 185, 66, 0.22);
}

.success-message {
  color: #126f55;
  background: #def7e9;
}

.search-field {
  display: flex;
  min-width: min(360px, 100%);
  flex: 1 1 260px;
  align-items: center;
  gap: 8px;
  min-height: 48px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: 12px;
  padding: 0 12px;
  background: #ffffff;
  box-shadow: 0 1px 0 rgba(36, 48, 74, 0.05);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.search-field input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--ink);
  font: inherit;
  font-weight: 800;
  outline: none;
  box-shadow: none;
}

.filters-row .field-control {
  flex: 1 1 210px;
  max-width: 260px;
}

.reward-items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 280px), 1fr));
  gap: 12px;
}

.reward-admin-card {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 12px;
  background: #ffffff;
}

.admin-item-art {
  display: grid;
  width: 82px;
  height: 82px;
  place-items: center;
  border-radius: 14px;
  background: rgba(63, 125, 217, 0.08);
  font-size: 2.6rem;
  line-height: 1;
  text-align: center;
}

.admin-item-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.admin-item-body strong,
.admin-item-body small,
.admin-item-body span {
  overflow-wrap: anywhere;
}

.admin-item-body strong {
  color: var(--ink);
  font-size: 1.05rem;
  font-weight: 950;
}

.admin-item-body span,
.admin-item-body small {
  color: var(--muted);
  font-weight: 800;
}

.admin-price-line,
.price-chip {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.price-chip {
  border-radius: 999px;
  padding: 4px 8px;
  background: rgba(63, 125, 217, 0.1);
  color: var(--blue);
  font-weight: 950;
}

.admin-card-actions {
  grid-column: 1 / -1;
  justify-content: flex-end;
}

.pagination-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid rgba(36, 48, 74, 0.1);
  padding-top: 14px;
  color: var(--muted);
  font-weight: 850;
}

.page-size-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-size-field .field-control {
  min-height: 42px;
  min-width: 82px;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.small-button {
  min-height: 42px;
  padding: 0 16px;
}

.icon-button.danger {
  color: var(--danger);
}

.tooltip-button {
  position: relative;
}

.tooltip-button::after {
  position: absolute;
  right: 50%;
  bottom: calc(100% + 8px);
  z-index: 5;
  transform: translateX(50%) translateY(4px);
  border-radius: 8px;
  padding: 6px 9px;
  background: var(--ink);
  color: #ffffff;
  content: attr(data-tooltip);
  font-size: 0.78rem;
  font-weight: 850;
  line-height: 1;
  opacity: 0;
  pointer-events: none;
  white-space: nowrap;
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.tooltip-button:hover::after,
.tooltip-button:focus-visible::after {
  transform: translateX(50%) translateY(0);
  opacity: 1;
}

.sr-only {
  position: absolute;
  overflow: hidden;
  width: 1px;
  height: 1px;
  margin: -1px;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .admin-reward-layout,
  .hero-panel {
    grid-template-columns: 1fr;
    display: grid;
  }

  .filters-row .field-control {
    max-width: none;
  }
}

@media (max-width: 640px) {
  .admin-reward-page {
    width: min(100% - 12px, 1480px);
  }

  .hero-panel,
  .form-panel,
  .list-panel {
    padding: 14px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .preview-box {
    min-height: 150px;
  }

  .filters-row,
  .pagination-row,
  .pagination-actions,
  .page-size-field {
    align-items: stretch;
    width: 100%;
  }

  .search-field,
  .filters-row .field-control,
  .page-size-field .field-control,
  .pagination-actions .button {
    width: 100%;
  }

  .reward-admin-card {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .admin-item-art {
    width: 72px;
    height: 72px;
  }
}
</style>
