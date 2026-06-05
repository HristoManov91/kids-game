<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Check, Eraser, Pencil, Plus, RefreshCw, Search, Sparkles, Trash2, X } from 'lucide-vue-next'
import { api } from '@/services/api'
import { emojiGroups } from '@/services/emojiLibrary'
import { levelRange } from '@/services/labels'
import type { QuizCategory, WordCatalogEntry, WordCatalogSuggestion } from '@/types'

interface CatalogForm {
  id: number | null
  category: QuizCategory
  word: string
  image: string
  syllablesText: string
  difficulty: number
  active: boolean
}

const entries = ref<WordCatalogEntry[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')
const search = ref('')
const levelFilter = ref('ALL')
const statusFilter = ref('ACTIVE')
const page = ref(1)
const perPage = ref(10)
const form = reactive<CatalogForm>(emptyForm())
const filteredEntries = computed(() => {
  const query = search.value.trim().toLowerCase()
  return entries.value.filter((entry) => {
    const matchesQuery =
      !query ||
      entry.word.toLowerCase().includes(query) ||
      entry.image.includes(query) ||
      entry.syllables.join('').toLowerCase().includes(query)
    const matchesLevel = levelFilter.value === 'ALL' || entry.difficulty === Number(levelFilter.value)
    const matchesStatus =
      statusFilter.value === 'ALL' ||
      (statusFilter.value === 'ACTIVE' && entry.active) ||
      (statusFilter.value === 'INACTIVE' && !entry.active)
    return matchesQuery && matchesLevel && matchesStatus
  })
})

const formTitle = computed(() => (form.id ? 'Редакция' : 'Нова дума'))
const formLetters = computed(() =>
  Array.from(form.word.trim().toLocaleUpperCase('bg-BG').replace(/[\s-]/g, ''))
)
const imageUsage = computed(() => {
  const usage = new Map<string, number>()
  for (const entry of entries.value) {
    usage.set(entry.image, (usage.get(entry.image) ?? 0) + 1)
  }
  return usage
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredEntries.value.length / perPage.value)))
const pagedEntries = computed(() => {
  const currentPage = Math.min(page.value, totalPages.value)
  const start = (currentPage - 1) * perPage.value
  return filteredEntries.value.slice(start, start + perPage.value)
})
const pageStart = computed(() => (filteredEntries.value.length === 0 ? 0 : (Math.min(page.value, totalPages.value) - 1) * perPage.value + 1))
const pageEnd = computed(() => Math.min(filteredEntries.value.length, Math.min(page.value, totalPages.value) * perPage.value))

onMounted(() => {
  void loadEntries()
})

watch([search, levelFilter, statusFilter, perPage], () => {
  page.value = 1
})

watch(totalPages, (value) => {
  if (page.value > value) {
    page.value = value
  }
})

function emptyForm(): CatalogForm {
  return {
    id: null,
    category: 'BULGARIAN',
    word: '',
    image: '',
    syllablesText: '',
    difficulty: 1,
    active: true
  }
}

async function loadEntries() {
  loading.value = true
  error.value = ''
  try {
    entries.value = await api.get<WordCatalogEntry[]>('/admin/word-catalog')
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Каталогът не беше зареден.'
  } finally {
    loading.value = false
  }
}

function editEntry(entry: WordCatalogEntry) {
  form.id = entry.id
  form.category = entry.category
  form.word = entry.word
  form.image = entry.image
  form.syllablesText = entry.syllables.join(', ')
  form.difficulty = entry.difficulty
  form.active = entry.active
  success.value = ''
  error.value = ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function resetForm() {
  Object.assign(form, emptyForm())
  success.value = ''
  error.value = ''
}

function clearFormValues() {
  form.word = ''
  form.image = ''
  form.syllablesText = ''
  form.difficulty = 1
  form.active = true
  success.value = ''
  error.value = ''
}

function chooseEmoji(image: string) {
  form.image = image
  if (imageUsageCount(image) > 0) {
    search.value = image
    levelFilter.value = 'ALL'
    statusFilter.value = 'ALL'
    page.value = 1
  }
}

function imageUsageCount(image: string) {
  return imageUsage.value.get(image) ?? 0
}

function emojiTitle(image: string, groupTitle: string) {
  const count = imageUsageCount(image)
  return count > 0 ? `${groupTitle} · вече добавена ${count} пъти` : groupTitle
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('bg-BG', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(new Date(value))
}

function accuracyLabel(entry: WordCatalogEntry) {
  if (entry.usedCount === 0) {
    return 'няма още опити'
  }
  return `${Math.round((entry.correctCount / entry.usedCount) * 100)}% позната`
}

async function suggest() {
  if (!form.word.trim()) {
    error.value = 'Въведи дума за предложение.'
    return
  }
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const response = await api.post<WordCatalogSuggestion>('/admin/word-catalog/suggest', {
      category: form.category,
      word: form.word
    })
    form.word = response.word
    form.syllablesText = response.suggestedSyllables.join(', ')
    form.difficulty = response.suggestedDifficulty
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не можах да предложа срички.'
  } finally {
    saving.value = false
  }
}

async function saveEntry() {
  saving.value = true
  error.value = ''
  success.value = ''
  const payload = {
    category: form.category,
    word: form.word,
    image: form.image,
    syllables: splitSyllables(form.syllablesText),
    difficulty: form.difficulty,
    active: form.active
  }
  try {
    if (form.id) {
      await api.put<WordCatalogEntry>(`/admin/word-catalog/${form.id}`, payload)
      success.value = 'Записът е обновен.'
    } else {
      await api.post<WordCatalogEntry>('/admin/word-catalog', payload)
      success.value = 'Думата е добавена.'
    }
    resetForm()
    await loadEntries()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Записът не беше запазен.'
  } finally {
    saving.value = false
  }
}

async function removeEntry(entry: WordCatalogEntry) {
  if (!window.confirm(`Да премахна ли "${entry.word}"?`)) {
    return
  }
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    await api.delete<void>(`/admin/word-catalog/${entry.id}`)
    if (form.id === entry.id) {
      resetForm()
    }
    await loadEntries()
    success.value = 'Думата е премахната.'
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Думата не беше премахната.'
  } finally {
    saving.value = false
  }
}

function splitSyllables(value: string) {
  return value
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}
</script>

<template>
  <section class="page admin-page">
    <div class="admin-layout">
      <section class="panel editor-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Админ</p>
            <h1 class="panel-title">{{ formTitle }}</h1>
          </div>
          <button class="icon-button" type="button" title="Ново" @click="resetForm">
            <Plus :size="20" />
          </button>
        </div>

        <form class="catalog-form" @submit.prevent="saveEntry">
          <label class="field">
            <span>Предмет</span>
            <select v-model="form.category">
              <option value="BULGARIAN">Български</option>
            </select>
          </label>

          <label class="field">
            <span>Дума</span>
            <input v-model="form.word" autocomplete="off" />
          </label>

          <div v-if="formLetters.length > 0" class="letters-preview">
            <span>Букви (автоматично)</span>
            <div>
              <b v-for="(letter, letterIndex) in formLetters" :key="`${letter}-${letterIndex}`">
                {{ letter }}
              </b>
            </div>
          </div>

          <label class="field image-field">
            <span>Картинка</span>
            <input v-model="form.image" autocomplete="off" />
          </label>

          <div class="emoji-picker" aria-label="Избор на картинка">
            <section v-for="group in emojiGroups" :key="group.title" class="emoji-group">
              <h3>{{ group.title }}</h3>
              <div class="emoji-grid">
                <button
                  v-for="emoji in group.items"
                  :key="`${group.title}-${emoji}`"
                  type="button"
                  :class="{ active: form.image === emoji, used: imageUsageCount(emoji) > 0 }"
                  :title="emojiTitle(emoji, group.title)"
                  :aria-label="emojiTitle(emoji, group.title)"
                  @click="chooseEmoji(emoji)"
                >
                  <span class="emoji-symbol">{{ emoji }}</span>
                  <span v-if="imageUsageCount(emoji) > 0" class="emoji-used-mark">
                    {{ imageUsageCount(emoji) }}
                  </span>
                </button>
              </div>
            </section>
          </div>

          <label class="field">
            <span>Срички</span>
            <input v-model="form.syllablesText" autocomplete="off" />
          </label>

          <label class="field">
            <span>Ниво</span>
            <select v-model.number="form.difficulty">
              <option v-for="level in 10" :key="level" :value="level">
                {{ level }} · {{ levelRange(level, form.category) }}
              </option>
            </select>
          </label>

          <label class="check-row">
            <input v-model="form.active" type="checkbox" />
            <span>Активна дума</span>
          </label>

          <div class="form-actions">
            <button class="button secondary" type="button" :disabled="saving" @click="suggest">
              <Sparkles :size="18" />
              <span>Предложи срички</span>
            </button>
            <button class="button" type="submit" :disabled="saving">
              <Check :size="18" />
              <span>Запази</span>
            </button>
            <button class="button secondary" type="button" :disabled="saving" @click="clearFormValues">
              <Eraser :size="18" />
              <span>Изчисти</span>
            </button>
            <button v-if="form.id" class="button secondary" type="button" :disabled="saving" @click="resetForm">
              <X :size="18" />
              <span>Откажи</span>
            </button>
          </div>
        </form>

        <div v-if="error" class="error">{{ error }}</div>
        <div v-if="success" class="success">{{ success }}</div>
      </section>

      <section class="panel catalog-panel">
        <div class="panel-header catalog-header">
          <div>
            <p class="eyebrow">Каталог</p>
            <h2 class="panel-title">Думи и картинки</h2>
          </div>
          <button class="icon-button" type="button" title="Обнови" :disabled="loading" @click="loadEntries">
            <RefreshCw :size="20" />
          </button>
        </div>

        <div class="filter-bar">
          <label class="field search-field">
            <span>Търсене</span>
            <div class="search-box">
              <Search :size="18" />
              <input v-model="search" autocomplete="off" placeholder="дума или картинка" />
            </div>
          </label>
          <label class="field">
            <span>Ниво</span>
            <select v-model="levelFilter">
              <option value="ALL">Всички</option>
              <option v-for="level in 10" :key="level" :value="String(level)">Ниво {{ level }}</option>
            </select>
          </label>
          <label class="field">
            <span>Статус</span>
            <select v-model="statusFilter">
              <option value="ACTIVE">Активни</option>
              <option value="INACTIVE">Скрити</option>
              <option value="ALL">Всички</option>
            </select>
          </label>
        </div>

        <div class="catalog-toolbar">
          <div class="catalog-count">
            {{ pageStart }}-{{ pageEnd }} от {{ filteredEntries.length }} · общо {{ entries.length }}
          </div>
          <label class="field per-page-field">
            <span>На страница</span>
            <select v-model.number="perPage">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </label>
        </div>

        <div class="catalog-list">
          <article v-for="entry in pagedEntries" :key="entry.id" class="catalog-row" :class="{ inactive: !entry.active }">
            <div class="entry-image">{{ entry.image }}</div>
            <div class="entry-main">
              <strong>{{ entry.word }}</strong>
              <span>{{ entry.syllables.join(' · ') }}</span>
            </div>
            <div class="entry-meta">
              <b>Ниво {{ entry.difficulty }}</b>
              <small>{{ levelRange(entry.difficulty, entry.category) }}</small>
            </div>
            <div class="entry-stats">
              <small>Добавена: {{ formatDate(entry.createdAt) }}</small>
              <b>{{ entry.usedCount }} пъти</b>
              <span>{{ entry.correctCount }} верни · {{ entry.wrongCount }} грешни</span>
              <small>{{ accuracyLabel(entry) }}</small>
            </div>
            <div class="row-actions">
              <button class="icon-button" type="button" title="Редактирай" @click="editEntry(entry)">
                <Pencil :size="18" />
              </button>
              <button class="icon-button danger-icon" type="button" title="Премахни" @click="removeEntry(entry)">
                <Trash2 :size="18" />
              </button>
            </div>
          </article>
        </div>

        <div class="pager">
          <button class="button secondary" type="button" :disabled="page <= 1" @click="page -= 1">
            Назад
          </button>
          <strong>{{ page }}/{{ totalPages }}</strong>
          <button class="button secondary" type="button" :disabled="page >= totalPages" @click="page += 1">
            Напред
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 390px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.editor-panel,
.catalog-panel {
  display: grid;
  gap: 18px;
}

.catalog-form,
.filter-bar {
  display: grid;
  gap: 14px;
  padding: 0 20px 20px;
}

.catalog-form {
  grid-template-columns: 1fr;
}

.image-field input {
  font-size: 1.45rem;
  text-align: center;
}

.letters-preview {
  display: grid;
  gap: 7px;
}

.letters-preview > span {
  color: var(--muted);
  font-size: 0.92rem;
  font-weight: 800;
}

.letters-preview div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.letters-preview b {
  display: grid;
  min-width: 34px;
  min-height: 34px;
  place-items: center;
  border: 1px dashed rgba(36, 48, 74, 0.18);
  border-radius: calc(var(--radius) - 2px);
  color: var(--ink);
  background: #ffffff;
}

.emoji-picker {
  display: grid;
  max-height: 300px;
  overflow: auto;
  gap: 12px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 8px;
  background: #ffffff;
}

.emoji-group {
  display: grid;
  gap: 7px;
}

.emoji-group h3 {
  position: sticky;
  z-index: 1;
  top: -8px;
  margin: 0;
  padding: 5px 2px;
  color: var(--muted);
  background: #ffffff;
  font-size: 0.82rem;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 6px;
}

.emoji-grid button {
  position: relative;
  display: grid;
  min-height: 34px;
  place-items: center;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: calc(var(--radius) - 2px);
  background: #fffaf0;
  font-size: 1.25rem;
}

.emoji-grid button.active,
.emoji-grid button:hover {
  border-color: var(--blue);
  background: #eef5ff;
}

.emoji-grid button.used {
  border-color: rgba(30, 157, 116, 0.55);
  background: #edf9f3;
  box-shadow: inset 0 0 0 1px rgba(30, 157, 116, 0.18);
}

.emoji-grid button.used:hover,
.emoji-grid button.used.active {
  border-color: var(--green);
  background: #ddf7eb;
}

.emoji-symbol {
  line-height: 1;
}

.emoji-used-mark {
  position: absolute;
  top: -5px;
  right: -5px;
  display: grid;
  min-width: 18px;
  height: 18px;
  place-items: center;
  border: 2px solid #ffffff;
  border-radius: 999px;
  color: #ffffff;
  background: var(--green);
  font-size: 0.68rem;
  font-weight: 950;
  line-height: 1;
}

.check-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--muted);
  font-weight: 800;
}

.check-row input {
  width: 20px;
  height: 20px;
  accent-color: var(--green);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.success {
  margin: 0 20px 20px;
  border-radius: var(--radius);
  padding: 12px 14px;
  color: #126f55;
  background: #ddf7eb;
  font-weight: 800;
}

.editor-panel .error {
  margin: 0 20px 20px;
}

.catalog-header {
  padding-bottom: 0;
}

.filter-bar {
  grid-template-columns: minmax(240px, 1fr) 160px 150px;
  align-items: end;
  padding-bottom: 0;
}

.search-box {
  position: relative;
}

.search-box svg {
  position: absolute;
  top: 50%;
  left: 12px;
  color: var(--muted);
  transform: translateY(-50%);
}

.search-box input {
  padding-left: 40px;
}

.catalog-toolbar,
.pager {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
  padding: 0 20px;
}

.catalog-count {
  color: var(--muted);
  font-weight: 800;
}

.per-page-field {
  width: 150px;
}

.catalog-list {
  display: grid;
  gap: 10px;
  padding: 0 20px;
}

.pager {
  padding-bottom: 20px;
}

.pager strong {
  font-size: 1.1rem;
}

.catalog-row {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr) 160px 220px auto;
  gap: 14px;
  align-items: center;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 12px;
  background: #ffffff;
}

.catalog-row.inactive {
  opacity: 0.55;
}

.entry-image {
  display: grid;
  min-height: 62px;
  place-items: center;
  border-radius: var(--radius);
  background: linear-gradient(135deg, #f4fbff, #fff7e3);
  font-size: 2rem;
  line-height: 1;
}

.entry-main,
.entry-meta,
.entry-stats {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.entry-main strong {
  overflow: hidden;
  font-size: 1.2rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-main span,
.entry-meta small,
.entry-stats small,
.entry-stats span {
  color: var(--muted);
  font-weight: 750;
}

.entry-meta b,
.entry-stats b {
  color: var(--green-dark);
}

.row-actions {
  display: inline-flex;
  gap: 8px;
}

.danger-icon {
  color: var(--danger);
}

@media (max-width: 1040px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .filter-bar,
  .catalog-toolbar,
  .pager,
  .catalog-row {
    grid-template-columns: 1fr;
  }

  .catalog-toolbar,
  .pager {
    display: grid;
    align-items: stretch;
  }

  .per-page-field {
    width: 100%;
  }

  .entry-image {
    min-height: 86px;
    font-size: 3rem;
  }

  .row-actions {
    justify-content: stretch;
  }

  .row-actions .icon-button {
    width: 100%;
  }
}
</style>
