<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import {
  AlertTriangle,
  BarChart3,
  CalendarDays,
  CircleAlert,
  Eye,
  Filter,
  RotateCcw,
  Search,
  Target,
  ThumbsDown,
  ThumbsUp
} from 'lucide-vue-next'
import { api } from '@/services/api'
import { isAdminUser } from '@/services/admin'
import { bulgarianPrimitiveModes, categoryLabels, formatDuration, logicPrimitiveModes, mathPrimitiveModes, modeLabels, primitiveModes } from '@/services/labels'
import { useAuthStore } from '@/stores/auth'
import type { ChildReportResponse, QuizCategory, QuizMode, ReportAttemptRow, ReportFocusArea, UserResponse } from '@/types'

const auth = useAuthStore()
const fromDate = ref(today())
const toDate = ref(today())
const category = ref<'ALL' | QuizCategory>('ALL')
const selectedModes = ref<QuizMode[]>([])
const difficulty = ref<'ALL' | number>('ALL')
const children = ref<UserResponse[]>([])
const selectedChildId = ref<number | null>(null)
const report = ref<ChildReportResponse | null>(null)
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = ref(10)
const modeSelect = ref<HTMLDetailsElement | null>(null)
const isAdminReport = computed(() => isAdminUser(auth.user))

const modeOptions = computed<QuizMode[]>(() => {
  if (category.value === 'MATH') {
    return mathPrimitiveModes
  }
  if (category.value === 'BULGARIAN') {
    return bulgarianPrimitiveModes
  }
  if (category.value === 'LOGIC') {
    return logicPrimitiveModes
  }
  return primitiveModes
})

const guidanceAreas = computed(() => report.value?.focusAreas ?? [])
const guidanceGroups = computed(() => {
  const groups = new Map<QuizCategory, ReportFocusArea[]>()
  for (const area of guidanceAreas.value) {
    groups.set(area.category, [...(groups.get(area.category) ?? []), area])
  }
  return Array.from(groups.entries()).map(([groupCategory, areas]) => ({
    category: groupCategory,
    title: categoryLabels[groupCategory],
    areas
  }))
})
const averageDurationSeconds = computed(() => {
  if (!report.value?.completedAttempts) {
    return 0
  }
  return Math.round(report.value.totalDurationSeconds / report.value.completedAttempts)
})
const totalPages = computed(() => {
  const total = report.value?.attempts.length ?? 0
  return Math.max(1, Math.ceil(total / pageSize.value))
})
const pagedAttempts = computed(() => {
  const attempts = report.value?.attempts ?? []
  const start = (page.value - 1) * pageSize.value
  return attempts.slice(start, start + pageSize.value)
})
const pageStart = computed(() => {
  if (!report.value?.attempts.length) {
    return 0
  }
  return (page.value - 1) * pageSize.value + 1
})
const pageEnd = computed(() => {
  const total = report.value?.attempts.length ?? 0
  return Math.min(total, page.value * pageSize.value)
})
const selectedModesLabel = computed(() => {
  if (selectedModes.value.length === 0) {
    return 'Всички'
  }
  if (selectedModes.value.length <= 2) {
    return selectedModes.value.map((selectedMode) => modeLabels[selectedMode]).join(', ')
  }
  return `${selectedModes.value.length} избрани`
})

watch(pageSize, () => {
  page.value = 1
})

watch(category, () => {
  const allowedModes = modeOptions.value
  selectedModes.value = selectedModes.value.filter((selectedMode) => allowedModes.includes(selectedMode))
})

watch(totalPages, (nextTotal) => {
  if (page.value > nextTotal) {
    page.value = nextTotal
  }
})

onMounted(async () => {
  document.addEventListener('click', closeModeSelectOnOutside)
  await loadChildrenIfNeeded()
  await loadReport()
})

onUnmounted(() => {
  document.removeEventListener('click', closeModeSelectOnOutside)
})

async function loadReport() {
  loading.value = true
  error.value = ''
  try {
    await loadChildrenIfNeeded()
    if (isAdminReport.value && selectedChildId.value === null) {
      report.value = null
      error.value = 'Изберете детски профил.'
      return
    }
    const params = new URLSearchParams({
      from: fromDate.value,
      to: toDate.value
    })
    if (category.value !== 'ALL') {
      params.set('category', category.value)
    }
    selectedModes.value.forEach((selectedMode) => params.append('modes', selectedMode))
    if (difficulty.value !== 'ALL') {
      params.set('difficulty', String(difficulty.value))
    }
    const endpoint = isAdminReport.value
      ? `/reports/children/${selectedChildId.value}/attempts?${params}`
      : `/reports/me/attempts?${params}`
    report.value = await api.get<ChildReportResponse>(endpoint)
    page.value = 1
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Справката не беше заредена.'
  } finally {
    loading.value = false
  }
}

async function loadChildrenIfNeeded() {
  if (!isAdminReport.value || children.value.length > 0) {
    return
  }
  children.value = await api.get<UserResponse[]>('/reports/children')
  selectedChildId.value = children.value[0]?.id ?? null
}

function today() {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function formatGrade(value: string | number | null) {
  if (value === null) {
    return '-'
  }
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return String(value)
  }
  if (Number.isInteger(numberValue)) {
    return String(numberValue)
  }
  return numberValue.toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
}

function guidanceGrade(area: ReportFocusArea) {
  return Number(area.averageGrade)
}

function guidanceIcon(area: ReportFocusArea) {
  const grade = guidanceGrade(area)
  if (grade > 5) {
    return ThumbsUp
  }
  if (grade >= 4.5) {
    return AlertTriangle
  }
  if (grade >= 4) {
    return CircleAlert
  }
  return ThumbsDown
}

function guidanceLabel(area: ReportFocusArea) {
  const grade = guidanceGrade(area)
  if (grade > 5) {
    return 'Всичко е наред'
  }
  if (grade >= 4.5) {
    return 'Под внимание'
  }
  if (grade >= 4) {
    return 'Още малко тренировки'
  }
  return 'Специално внимание'
}

function guidanceClass(area: ReportFocusArea) {
  const grade = guidanceGrade(area)
  if (grade > 5) {
    return 'excellent'
  }
  if (grade >= 4.5) {
    return 'watch'
  }
  if (grade >= 4) {
    return 'training'
  }
  return 'attention'
}

function formatDateTime(value: string | null) {
  if (!value) {
    return 'активен'
  }
  return new Intl.DateTimeFormat('bg-BG', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function resultLabel(attempt: ReportAttemptRow) {
  return attempt.status === 'COMPLETED'
    ? `${attempt.correct}/${attempt.totalQuestions} · оценка ${formatGrade(attempt.grade)} · +${attempt.crystals ?? 0} кристала`
    : `${attempt.correct} верни · активен`
}

function attemptHref(attempt: ReportAttemptRow) {
  if (isAdminReport.value) {
    return `/admin/monitoring/attempts/${attempt.attemptId}`
  }
  return attempt.status === 'COMPLETED' ? `/result/${attempt.attemptId}` : `/quiz/${attempt.attemptId}`
}

function toggleMode(nextMode: QuizMode) {
  if (selectedModes.value.includes(nextMode)) {
    selectedModes.value = selectedModes.value.filter((selectedMode) => selectedMode !== nextMode)
    return
  }
  selectedModes.value = [...selectedModes.value, nextMode]
}

function clearModes() {
  selectedModes.value = []
}

function clearFilters() {
  const todayValue = today()
  fromDate.value = todayValue
  toDate.value = todayValue
  category.value = 'ALL'
  selectedModes.value = []
  difficulty.value = 'ALL'
  report.value = null
  error.value = ''
  page.value = 1
  pageSize.value = 10
  if (modeSelect.value) {
    modeSelect.value.open = false
  }
}

function closeModeSelectOnOutside(event: MouseEvent) {
  const target = event.target
  if (!(target instanceof Node) || !modeSelect.value || modeSelect.value.contains(target)) {
    return
  }
  modeSelect.value.open = false
}

function openNativePicker(event: Event) {
  const control = event.currentTarget as (HTMLInputElement | HTMLSelectElement) & { showPicker?: () => void }
  control.focus()
  try {
    control.showPicker?.()
  } catch {
    // Some browsers only allow the native picker for specific controls.
  }
}

function previousPage() {
  page.value = Math.max(1, page.value - 1)
}

function nextPage() {
  page.value = Math.min(totalPages.value, page.value + 1)
}
</script>

<template>
  <section class="panel report-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">Моя справка</p>
        <h2 class="panel-title">Преглед на решени тестове</h2>
      </div>
      <BarChart3 :size="25" />
    </div>

    <div class="report-filters" :class="{ 'report-filters--admin': isAdminReport }">
      <label v-if="isAdminReport" class="field child-field">
        <span>Профил</span>
        <select v-model.number="selectedChildId" @click="openNativePicker">
          <option v-for="child in children" :key="child.id" :value="child.id">{{ child.displayName }}</option>
        </select>
      </label>
      <label class="field">
        <span>От дата</span>
        <input v-model="fromDate" type="date" @click="openNativePicker" />
      </label>
      <label class="field">
        <span>До дата</span>
        <input v-model="toDate" type="date" @click="openNativePicker" />
      </label>
      <label class="field">
        <span>Предмет</span>
        <select v-model="category" @click="openNativePicker">
          <option value="ALL">Всички</option>
          <option value="MATH">Математика</option>
          <option value="BULGARIAN">Български</option>
          <option value="LOGIC">Логика</option>
        </select>
      </label>
      <div class="field mode-filter">
        <span>Подкатегории</span>
        <details ref="modeSelect" class="multi-select">
          <summary>
            <span>{{ selectedModesLabel }}</span>
          </summary>
          <div class="multi-select-menu">
            <label class="mode-option all-option">
              <input type="checkbox" :checked="selectedModes.length === 0" @change="clearModes" />
              <span>Всички</span>
            </label>
            <label v-for="option in modeOptions" :key="option" class="mode-option">
              <input
                type="checkbox"
                :checked="selectedModes.includes(option)"
                @change="toggleMode(option)"
              />
              <span>{{ modeLabels[option] }}</span>
            </label>
          </div>
        </details>
      </div>
      <label class="field">
        <span>Ниво</span>
        <select v-model="difficulty" @click="openNativePicker">
          <option value="ALL">Всички</option>
          <option v-for="level in 10" :key="level" :value="level">{{ level }}</option>
        </select>
      </label>
      <div class="report-actions">
        <button class="button report-search" type="button" :disabled="loading" @click="loadReport">
          <Search :size="19" />
          <span>Покажи</span>
        </button>
        <button class="button secondary clear-filters" type="button" :disabled="loading" @click="clearFilters">
          <RotateCcw :size="18" />
          <span>Изчисти</span>
        </button>
      </div>
    </div>

    <div v-if="error" class="error report-error">{{ error }}</div>

    <div v-if="report" class="report-content">
      <div class="report-summary">
        <div>
          <CalendarDays :size="22" />
          <span>Тестове</span>
          <strong>{{ report.completedAttempts }}/{{ report.totalAttempts }}</strong>
        </div>
        <div>
          <Target :size="22" />
          <span>Верни</span>
          <strong>{{ report.totalCorrect }}/{{ report.totalCorrect + report.totalWrong }}</strong>
        </div>
        <div>
          <Filter :size="22" />
          <span>Средна оценка</span>
          <strong>{{ formatGrade(report.averageGrade) }}</strong>
        </div>
        <div>
          <BarChart3 :size="22" />
          <span>Средно време</span>
          <strong>{{ formatDuration(averageDurationSeconds) }}</strong>
        </div>
      </div>

      <div class="attention-panel">
        <h3>Насоки по категории</h3>
        <div v-if="guidanceAreas.length === 0" class="empty-state">Няма достатъчно завършени тестове за анализ.</div>
        <section v-for="group in guidanceGroups" :key="group.category" class="attention-group">
          <h4 v-if="category === 'ALL'">{{ group.title }}</h4>
          <div
            v-for="area in group.areas"
            :key="`${area.category}-${area.mode}`"
            class="attention-row"
            :class="guidanceClass(area)"
          >
            <span class="guidance-icon" aria-hidden="true">
              <component :is="guidanceIcon(area)" :size="23" :stroke-width="2.7" />
            </span>
            <span>
              <b>{{ modeLabels[area.mode] }}</b>
              <small>{{ guidanceLabel(area) }}</small>
            </span>
            <strong>Оценка {{ formatGrade(area.averageGrade) }}</strong>
            <small>{{ area.accuracyPercent }}% · {{ area.wrong }} грешни от {{ area.total }}</small>
          </div>
        </section>
      </div>

      <div class="attempts-panel">
        <div class="attempts-heading">
          <h3>Решени тестове</h3>
          <label class="page-size-control">
            <span>На страница</span>
            <select v-model.number="pageSize" @click="openNativePicker">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </label>
        </div>
        <div v-if="report.attempts.length === 0" class="empty-state">Няма тестове за избрания период.</div>
        <a
          v-for="attempt in pagedAttempts"
          :key="attempt.attemptId"
          class="attempt-report-row"
          :href="attemptHref(attempt)"
          target="_blank"
          rel="noopener"
        >
          <span>
            <b>{{ modeLabels[attempt.mode] }}</b>
            <small>{{ formatDateTime(attempt.completedAt ?? attempt.startedAt) }} · Ниво {{ attempt.difficulty }}</small>
          </span>
          <strong>{{ resultLabel(attempt) }}</strong>
          <small>{{ formatDuration(attempt.durationSeconds) }}</small>
          <Eye :size="18" />
        </a>
        <div v-if="report.attempts.length > 0" class="pagination-bar">
          <span>{{ pageStart }}-{{ pageEnd }} от {{ report.attempts.length }}</span>
          <div>
            <button type="button" :disabled="page <= 1" @click="previousPage">Назад</button>
            <strong>{{ page }}/{{ totalPages }}</strong>
            <button type="button" :disabled="page >= totalPages" @click="nextPage">Напред</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.report-panel {
  display: grid;
  gap: 18px;
}

.report-filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 0.7fr)) minmax(150px, 0.75fr) minmax(280px, 1.6fr) minmax(100px, 0.5fr) auto;
  gap: 14px 12px;
  padding: 0 20px 10px;
  align-items: end;
}

.report-filters--admin {
  grid-template-columns: minmax(150px, 0.8fr) repeat(2, minmax(140px, 0.7fr)) minmax(150px, 0.75fr) minmax(260px, 1.4fr) minmax(100px, 0.5fr) auto;
}

.child-field {
  min-width: 0;
}

.mode-filter {
  position: relative;
  min-width: 0;
}

.multi-select {
  position: relative;
}

.multi-select summary {
  display: flex;
  min-height: 46px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 0 13px;
  color: var(--ink);
  background: #ffffff;
  cursor: pointer;
  font-weight: 400;
  list-style: none;
}

.multi-select summary::-webkit-details-marker {
  display: none;
}

.multi-select summary::after {
  content: '▾';
  color: var(--muted);
  font-size: 0.85rem;
}

.multi-select[open] summary {
  border-color: var(--blue);
  box-shadow: 0 0 0 3px rgba(63, 125, 217, 0.16);
}

.multi-select-menu {
  position: absolute;
  z-index: 20;
  top: calc(100% + 6px);
  right: 0;
  left: 0;
  display: grid;
  max-height: 280px;
  overflow: auto;
  gap: 4px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 6px;
  background: #ffffff;
  box-shadow: var(--shadow);
}

.mode-option {
  display: flex;
  min-height: 36px;
  align-items: center;
  gap: 8px;
  border-radius: calc(var(--radius) - 2px);
  padding: 0 8px;
  color: var(--ink);
  cursor: pointer;
  font-size: 1rem;
  font-weight: 400;
}

.mode-option:hover {
  background: rgba(63, 125, 217, 0.08);
}

.mode-option input {
  width: 17px;
  height: 17px;
  accent-color: var(--blue);
}

.all-option {
  color: var(--blue);
}

.report-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(112px, auto));
  gap: 8px;
}

.report-search,
.clear-filters {
  min-height: 48px;
}

.report-error {
  margin: 10px 20px 0;
}

.report-content {
  display: grid;
  gap: 16px;
  padding: 0 20px 20px;
}

.report-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.report-summary div {
  display: grid;
  gap: 6px;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 14px;
  background: #ffffff;
}

.report-summary svg {
  color: var(--blue);
}

.report-summary span,
.attention-row small,
.attempt-report-row small {
  color: var(--muted);
  font-weight: 800;
}

.report-summary strong {
  font-size: 1.45rem;
}

.attention-panel,
.attempts-panel {
  display: grid;
  gap: 10px;
}

.attention-group {
  display: grid;
  gap: 8px;
  border-top: 2px solid rgba(36, 48, 74, 0.1);
  padding-top: 10px;
}

.attention-group:first-of-type {
  border-top: 0;
  padding-top: 0;
}

.attention-panel h3,
.attempts-panel h3 {
  margin: 0;
  font-size: 1rem;
}

.attention-group h4 {
  margin: 0;
  border-radius: var(--radius);
  padding: 9px 12px;
  color: var(--green-dark);
  background: rgba(30, 157, 116, 0.09);
  font-size: 0.95rem;
}

.attempts-heading,
.pagination-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 0.88rem;
  font-weight: 800;
}

.page-size-control select {
  min-height: 36px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 0 10px;
  color: var(--ink);
  background: #ffffff;
  font-weight: 900;
}

.attention-row,
.attempt-report-row {
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 12px;
  background: #ffffff;
}

.attention-row {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  border-left-width: 8px;
  border-left-style: solid;
}

.attention-row b,
.attempt-report-row b {
  font-weight: 950;
}

.attention-row > span:not(.guidance-icon) {
  display: grid;
  gap: 3px;
}

.guidance-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 50%;
}

.attention-row.excellent {
  border-color: rgba(30, 157, 116, 0.3);
  border-left-color: #1e9d74;
  background: #f0fbf5;
}

.attention-row.excellent .guidance-icon {
  color: #137b5b;
  background: rgba(30, 157, 116, 0.12);
}

.attention-row.watch {
  border-color: rgba(245, 185, 66, 0.42);
  border-left-color: #f0b429;
  background: #fff9df;
}

.attention-row.watch .guidance-icon {
  color: #a87905;
  background: rgba(240, 180, 41, 0.16);
}

.attention-row.training {
  border-color: rgba(230, 132, 38, 0.36);
  border-left-color: #e68426;
  background: #fff5e8;
}

.attention-row.training .guidance-icon {
  color: #a5530f;
  background: rgba(230, 132, 38, 0.14);
}

.attention-row.attention {
  border-color: rgba(242, 107, 94, 0.32);
  border-left-color: #e65d50;
  background: #fff2ef;
}

.attention-row.attention .guidance-icon {
  color: #b9342b;
  background: rgba(230, 93, 80, 0.12);
}

.attempt-report-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  gap: 12px;
  align-items: center;
  width: 100%;
  color: var(--ink);
  text-align: left;
}

.attempt-report-row.active {
  border-color: var(--blue);
  box-shadow: 0 0 0 3px rgba(63, 125, 217, 0.12);
}

.attempt-report-row:hover,
.attempt-report-row:focus-visible {
  border-color: rgba(63, 125, 217, 0.42);
  box-shadow: 0 0 0 3px rgba(63, 125, 217, 0.12);
}

.attempt-report-row > span {
  display: grid;
  gap: 3px;
}

.pagination-bar {
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.7);
}

.pagination-bar span {
  color: var(--muted);
  font-weight: 800;
}

.pagination-bar div {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pagination-bar button {
  min-height: 34px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 0 10px;
  color: var(--ink);
  background: #ffffff;
  font-weight: 850;
}

.pagination-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

@media (max-width: 1180px) {
  .report-filters,
  .report-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .report-filters,
  .report-summary,
  .attention-row,
  .attempt-report-row {
    grid-template-columns: 1fr;
  }
}
</style>
