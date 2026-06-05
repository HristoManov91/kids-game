<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Activity,
  AlertTriangle,
  BarChart3,
  ClipboardCheck,
  Eye,
  MessageSquareText,
  ShieldCheck,
  Timer,
  Trophy,
  Users
} from 'lucide-vue-next'
import { api } from '@/services/api'
import { categoryLabels, formatDuration, modeLabels } from '@/services/labels'
import type {
  AdminAttemptMonitorRow,
  AdminModeMonitorRow,
  AdminMonitoringResponse,
  QuizCategory,
  QuizMode,
  IssueReportStatus,
  QuestionIssueReportResponse,
  TaskSuggestionResponse
} from '@/types'

type SectionId = 'overview' | 'load' | 'types' | 'active' | 'reports' | 'suggestions' | 'users'

const router = useRouter()
const route = useRoute()
const monitoring = ref<AdminMonitoringResponse | null>(null)
const reportNotes = ref<Record<number, string>>({})
const suggestionNotes = ref<Record<number, string>>({})
const loading = ref(false)
const error = ref('')
const fromDate = ref(todayInputValue())
const toDate = ref(todayInputValue())
const activeSection = ref<SectionId>(sectionFromQuery(route.query.section))
const expandedModeKey = ref('')
const typeCategoryFilter = ref<QuizCategory | 'ALL'>('ALL')
const typeModeFilter = ref<QuizMode | 'ALL'>('ALL')
const modePage = ref(1)
const modePageSize = ref(5)
const usersPage = ref(1)
const usersPageSize = ref(10)

const generatedAt = computed(() => monitoring.value ? formatDateTime(monitoring.value.generatedAt) : '')
const hasActiveAttempts = computed(() => Boolean(monitoring.value?.active.length))
const periodNoun = computed(() => {
  return fromDate.value === toDate.value ? 'деня' : 'периода'
})
const periodRangeLabel = computed(() => {
  if (!monitoring.value) {
    return ''
  }
  const start = new Date(monitoring.value.periodStart)
  const end = new Date(new Date(monitoring.value.periodEnd).getTime() - 1)
  if (monitoring.value.fromDate === monitoring.value.toDate) {
    return formatDateOnly(start)
  }
  return `${formatDateOnly(start)} - ${formatDateOnly(end)}`
})
const maxUsageCount = computed(() => {
  const buckets = monitoring.value?.todayUsage ?? []
  return Math.max(1, ...buckets.map((bucket) => Math.max(bucket.started, bucket.completed)))
})
const selectedMode = computed(() => {
  if (!expandedModeKey.value) {
    return null
  }
  return filteredModes.value.find((mode) => modeKey(mode) === expandedModeKey.value) ?? null
})
const filteredModes = computed(() => {
  const modes = monitoring.value?.todayModes ?? []
  return modes.filter((mode) => {
    const categoryMatches = typeCategoryFilter.value === 'ALL' || mode.category === typeCategoryFilter.value
    const modeMatches = typeModeFilter.value === 'ALL' || mode.mode === typeModeFilter.value
    return categoryMatches && modeMatches
  })
})
const modeOptions = computed(() => {
  const modes = monitoring.value?.todayModes ?? []
  return modes
    .filter((mode) => typeCategoryFilter.value === 'ALL' || mode.category === typeCategoryFilter.value)
    .map((mode) => mode.mode)
    .filter((mode, index, all) => all.indexOf(mode) === index)
})
const selectedModeAttempts = computed(() => selectedMode.value?.attempts ?? [])
const pagedModeAttempts = computed(() => paginate(selectedModeAttempts.value, modePage.value, modePageSize.value))
const modePageCount = computed(() => pageCount(selectedModeAttempts.value.length, modePageSize.value))
const pagedUsers = computed(() => paginate(monitoring.value?.users ?? [], usersPage.value, usersPageSize.value))
const usersPageCount = computed(() => pageCount(monitoring.value?.users.length ?? 0, usersPageSize.value))

const navItems = computed(() => [
  { id: 'overview' as SectionId, label: 'Обобщение' },
  { id: 'load' as SectionId, label: 'Натоварване' },
  { id: 'types' as SectionId, label: 'Тестове' },
  { id: 'active' as SectionId, label: 'Активни' },
  { id: 'reports' as SectionId, label: 'Доклади' },
  { id: 'suggestions' as SectionId, label: 'Предложения' },
  { id: 'users' as SectionId, label: 'Акаунти' }
])

onMounted(() => {
  loadMonitoring()
})

async function loadMonitoring() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({
      from: fromDate.value,
      to: toDate.value
    })
    monitoring.value = await api.get<AdminMonitoringResponse>(`/admin/monitoring?${params}`)
    usersPage.value = 1
    modePage.value = 1
    expandedModeKey.value = ''
    reportNotes.value = {
      ...Object.fromEntries(monitoring.value.recentReports.map((report) => [report.id, report.adminNote ?? ''])),
      ...reportNotes.value
    }
    suggestionNotes.value = {
      ...Object.fromEntries(monitoring.value.recentSuggestions.map((suggestion) => [suggestion.id, suggestion.adminNote ?? ''])),
      ...suggestionNotes.value
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Мониторингът не можа да се зареди.'
  } finally {
    loading.value = false
  }
}

function todayInputValue() {
  const date = new Date()
  const offset = date.getTimezoneOffset()
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 10)
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'няма'
  }
  return new Intl.DateTimeFormat('bg-BG', {
    dateStyle: 'short',
    timeStyle: 'short'
  }).format(new Date(value))
}

function formatDateOnly(value: Date) {
  return new Intl.DateTimeFormat('bg-BG', {
    dateStyle: 'medium'
  }).format(value)
}

function formatGrade(value: string | null | undefined) {
  if (!value) {
    return '-'
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return value
  }
  return numeric.toFixed(2).replace(/\.00$/, '')
}

function scoreLabel(attempt: AdminAttemptMonitorRow) {
  if (attempt.score === null) {
    return `${attempt.answeredCount}/${attempt.totalQuestions} отговорени`
  }
  return `${attempt.score}/${attempt.totalQuestions}`
}

function modeKey(mode: Pick<AdminModeMonitorRow, 'category' | 'mode'>) {
  return `${mode.category}-${mode.mode}`
}

function toggleMode(mode: AdminModeMonitorRow) {
  const key = modeKey(mode)
  expandedModeKey.value = expandedModeKey.value === key ? '' : key
  modePage.value = 1
}

function resetTypeFilters() {
  typeModeFilter.value = 'ALL'
  collapseTypeDetails()
}

function collapseTypeDetails() {
  expandedModeKey.value = ''
  modePage.value = 1
}

function pageCount(total: number, size: number) {
  return Math.max(1, Math.ceil(total / size))
}

function paginate<T>(items: T[], page: number, size: number) {
  const start = (page - 1) * size
  return items.slice(start, start + size)
}

function previousModePage() {
  modePage.value = Math.max(1, modePage.value - 1)
}

function nextModePage() {
  modePage.value = Math.min(modePageCount.value, modePage.value + 1)
}

function previousUsersPage() {
  usersPage.value = Math.max(1, usersPage.value - 1)
}

function nextUsersPage() {
  usersPage.value = Math.min(usersPageCount.value, usersPage.value + 1)
}

function editAttempt(attemptId: string) {
  router.push({ name: 'adminAttemptEdit', params: { attemptId }, query: { section: activeSection.value } })
}

function sectionFromQuery(value: unknown): SectionId {
  const section = Array.isArray(value) ? value[0] : value
  if (
    section === 'overview' ||
    section === 'load' ||
    section === 'types' ||
    section === 'active' ||
    section === 'reports' ||
    section === 'suggestions' ||
    section === 'users'
  ) {
    return section
  }
  return 'overview'
}

async function updateReport(report: QuestionIssueReportResponse, status: IssueReportStatus) {
  error.value = ''
  try {
    await api.put<QuestionIssueReportResponse>(`/admin/monitoring/issue-reports/${report.id}`, {
      status,
      adminNote: reportNotes.value[report.id] ?? report.adminNote ?? ''
    })
    await loadMonitoring()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Докладът не беше обновен.'
  }
}

async function updateSuggestion(suggestion: TaskSuggestionResponse, status: IssueReportStatus) {
  error.value = ''
  try {
    await api.put<TaskSuggestionResponse>(`/admin/monitoring/suggestions/${suggestion.id}`, {
      status,
      adminNote: suggestionNotes.value[suggestion.id] ?? suggestion.adminNote ?? ''
    })
    await loadMonitoring()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Предложението не беше обновено.'
  }
}

function reportStatusLabel(status: IssueReportStatus) {
  if (status === 'RESOLVED') {
    return 'Решен'
  }
  if (status === 'DISMISSED') {
    return 'Няма проблем'
  }
  return 'Отворен'
}

function suggestionTargetLabel(suggestion: TaskSuggestionResponse) {
  if (!suggestion.category) {
    return 'Общо предложение'
  }
  const parts = [categoryLabels[suggestion.category]]
  if (suggestion.mode) {
    parts.push(modeLabels[suggestion.mode])
  }
  if (suggestion.difficulty) {
    parts.push(`Ниво ${suggestion.difficulty}`)
  }
  return parts.join(' · ')
}
</script>

<template>
  <section class="page admin-monitoring-page">
    <section class="panel monitoring-hero">
      <div>
        <p class="eyebrow">Админ мониторинг</p>
        <h1>Наблюдение на приложението</h1>
        <p class="muted">
          Видимо само за Христо. Период: {{ periodRangeLabel || 'зареждане' }}.
        </p>
      </div>
      <div class="period-controls">
        <label>
          <span>От дата</span>
          <input v-model="fromDate" type="date" @change="loadMonitoring" />
        </label>
        <label>
          <span>До дата</span>
          <input v-model="toDate" type="date" @change="loadMonitoring" />
        </label>
      </div>
    </section>

    <p v-if="error" class="error">{{ error }}</p>

    <template v-if="monitoring">
      <div class="monitoring-shell">
        <aside class="panel monitoring-nav">
          <button
            v-for="item in navItems"
            :key="item.id"
            type="button"
            :class="{ active: activeSection === item.id }"
            @click="activeSection = item.id"
          >
            <span>{{ item.label }}</span>
          </button>
        </aside>

        <div class="monitoring-content">
          <section v-if="activeSection === 'overview'" class="section-stack">
            <div class="monitoring-stats">
              <article class="metric-card">
                <Users :size="30" />
                <span>Акаунти</span>
                <strong>{{ monitoring.totalUsers }}</strong>
              </article>
              <article class="metric-card">
                <Activity :size="30" />
                <span>Стартирани за {{ periodNoun }}</span>
                <strong>{{ monitoring.startedToday }}</strong>
              </article>
              <article class="metric-card">
                <ClipboardCheck :size="30" />
                <span>Завършени за {{ periodNoun }}</span>
                <strong>{{ monitoring.completedToday }}</strong>
              </article>
              <article class="metric-card" :class="{ warn: hasActiveAttempts }">
                <Activity :size="30" />
                <span>Активни тестове</span>
                <strong>{{ monitoring.activeAttempts }}</strong>
              </article>
              <article class="metric-card">
                <Users :size="30" />
                <span>Активни акаунти за {{ periodNoun }}</span>
                <strong>{{ monitoring.activeUsersToday }}</strong>
              </article>
              <article class="metric-card">
                <Trophy :size="30" />
                <span>Средна оценка за {{ periodNoun }}</span>
                <strong>{{ formatGrade(monitoring.averageGradeToday) }}</strong>
              </article>
              <article class="metric-card" :class="{ warn: monitoring.openIssueReports > 0 }">
                <AlertTriangle :size="30" />
                <span>Отворени доклади</span>
                <strong>{{ monitoring.openIssueReports }}</strong>
              </article>
              <article class="metric-card" :class="{ warn: monitoring.openSuggestions > 0 }">
                <MessageSquareText :size="30" />
                <span>Отворени предложения</span>
                <strong>{{ monitoring.openSuggestions }}</strong>
              </article>
            </div>

            <section class="panel monitoring-panel">
              <div class="panel-header compact">
                <div>
                  <h2 class="panel-title">По предмети</h2>
                  <p class="muted">Завършени тестове и средна оценка за избрания период.</p>
                </div>
                <Trophy :size="23" />
              </div>

              <div class="category-monitor">
                <article v-for="category in monitoring.categories" :key="category.category">
                  <span>{{ categoryLabels[category.category] }}</span>
                  <strong>{{ category.completedAttempts }}</strong>
                  <small>средна оценка {{ formatGrade(category.averageGrade) }}</small>
                </article>
              </div>
            </section>

            <section class="panel monitoring-panel">
              <div class="panel-header compact">
                <div>
                  <h2 class="panel-title">От началото</h2>
                  <p class="muted">Обща статистика за всички записани данни в приложението.</p>
                </div>
                <BarChart3 :size="23" />
              </div>
              <div class="monitoring-stats all-time-stats">
                <article class="metric-card">
                  <span>Всички стартирани тестове</span>
                  <strong>{{ monitoring.totalAttempts }}</strong>
                </article>
                <article class="metric-card">
                  <span>Всички завършени тестове</span>
                  <strong>{{ monitoring.completedAttempts }}</strong>
                </article>
                <article class="metric-card">
                  <span>Средна оценка общо</span>
                  <strong>{{ formatGrade(monitoring.allTimeAverageGrade) }}</strong>
                </article>
                <article class="metric-card">
                  <span>Средно време общо</span>
                  <strong>{{ formatDuration(monitoring.allTimeAverageDurationSeconds) }}</strong>
                </article>
              </div>
              <div class="category-monitor">
                <article v-for="category in monitoring.allTimeCategories" :key="category.category">
                  <span>{{ categoryLabels[category.category] }}</span>
                  <strong>{{ category.completedAttempts }}</strong>
                  <small>средна оценка {{ formatGrade(category.averageGrade) }}</small>
                </article>
              </div>
            </section>
          </section>

          <section v-if="activeSection === 'load'" class="panel monitoring-panel">
            <div class="panel-header compact">
              <div>
                <h2 class="panel-title">Натоварване за {{ periodNoun }}</h2>
                <p class="muted">Стартирани и завършени тестове за {{ periodRangeLabel }}.</p>
              </div>
              <Activity :size="23" />
            </div>
            <div class="usage-chart">
              <article v-for="bucket in monitoring.todayUsage" :key="bucket.label" class="usage-bar">
                <span>{{ bucket.label }}</span>
                <div>
                  <b class="started" :style="{ height: `${Math.max(4, (bucket.started / maxUsageCount) * 88)}px` }"></b>
                  <b class="completed" :style="{ height: `${Math.max(4, (bucket.completed / maxUsageCount) * 88)}px` }"></b>
                </div>
                <small>{{ bucket.started }}/{{ bucket.completed }}</small>
              </article>
            </div>
            <p class="chart-legend">
              <span></span> стартирани <b></b> завършени · средно време {{ formatDuration(monitoring.averageDurationTodaySeconds) }} ·
              най-бърз {{ formatDuration(monitoring.fastestDurationTodaySeconds) }} · най-бавен {{ formatDuration(monitoring.slowestDurationTodaySeconds) }}
            </p>
          </section>

          <section v-if="activeSection === 'types'" class="section-stack">
            <section class="panel monitoring-panel">
              <div class="panel-header compact">
                <div>
                  <h2 class="panel-title">Тестове · {{ filteredModes.length }}</h2>
                  <p class="muted">Резюме за избрания период. Разтвори ред, за да видиш конкретните тестове.</p>
                </div>
                <BarChart3 :size="23" />
              </div>

              <div class="table-toolbar filters-toolbar">
                <label>
                  <span>Предмет</span>
                  <select v-model="typeCategoryFilter" @change="resetTypeFilters">
                    <option value="ALL">Всички</option>
                    <option value="MATH">Математика</option>
                    <option value="BULGARIAN">Български</option>
                    <option value="LOGIC">Логика</option>
                  </select>
                </label>
                <label>
                  <span>Подкатегория</span>
                  <select v-model="typeModeFilter" @change="collapseTypeDetails">
                    <option value="ALL">Всички</option>
                    <option v-for="mode in modeOptions" :key="mode" :value="mode">{{ modeLabels[mode] }}</option>
                  </select>
                </label>
              </div>

              <div v-if="filteredModes.length === 0" class="empty-state">Няма завършени тестове за избраните филтри.</div>
              <div v-else class="monitor-table mode-table">
                <div class="monitor-table-head">
                  <span>Предмет</span>
                  <span>Подкатегория</span>
                  <span>Брой</span>
                  <span>Средна оценка</span>
                  <span>Средно време</span>
                  <span>Най-бърз</span>
                  <span>Най-бавен</span>
                  <span></span>
                </div>
                <template v-for="mode in filteredModes" :key="modeKey(mode)">
                  <div class="monitor-table-row">
                    <span>{{ categoryLabels[mode.category] }}</span>
                    <span>
                      <strong>{{ modeLabels[mode.mode] }}</strong>
                    </span>
                    <span>{{ mode.completedAttempts }}</span>
                    <span>{{ formatGrade(mode.averageGrade) }}</span>
                    <span>{{ formatDuration(mode.averageDurationSeconds) }}</span>
                    <span>{{ formatDuration(mode.fastestDurationSeconds) }}</span>
                    <span>{{ formatDuration(mode.slowestDurationSeconds) }}</span>
                    <span>
                      <button class="mini-button" type="button" @click="toggleMode(mode)">
                        {{ expandedModeKey === modeKey(mode) ? 'Скрий' : 'Покажи' }}
                      </button>
                    </span>
                  </div>
                  <div v-if="expandedModeKey === modeKey(mode)" class="mode-detail-row">
                    <div class="mode-detail-header">
                      <strong>{{ modeLabels[mode.mode] }}</strong>
                      <span>{{ mode.attempts.length }} теста за {{ periodNoun }}</span>
                      <label>
                        <span>На страница</span>
                        <select v-model.number="modePageSize" @change="modePage = 1">
                          <option :value="5">5</option>
                          <option :value="10">10</option>
                        </select>
                      </label>
                    </div>
                    <div class="monitor-table nested-table">
                      <div class="monitor-table-head">
                        <span>Дете</span>
                        <span>Резултат</span>
                        <span>Оценка</span>
                        <span>Време</span>
                        <span>Действие</span>
                      </div>
                      <div v-for="attempt in pagedModeAttempts" :key="attempt.attemptId" class="monitor-table-row">
                        <span>
                          <strong>{{ attempt.displayName }}</strong>
                          <small>{{ formatDateTime(attempt.completedAt) }}</small>
                        </span>
                        <span>{{ scoreLabel(attempt) }}</span>
                        <span>{{ formatGrade(attempt.grade) }}</span>
                        <span>{{ formatDuration(attempt.durationSeconds) }}</span>
                        <span>
                          <button class="mini-button" type="button" @click="editAttempt(attempt.attemptId)">
                            <Eye :size="16" />
                            <span>Редактирай тест</span>
                          </button>
                        </span>
                      </div>
                    </div>
                    <div class="pagination-row">
                      <button class="mini-button" type="button" :disabled="modePage === 1" @click="previousModePage">Назад</button>
                      <strong>{{ modePage }}/{{ modePageCount }}</strong>
                      <button class="mini-button" type="button" :disabled="modePage === modePageCount" @click="nextModePage">Напред</button>
                    </div>
                  </div>
                </template>
              </div>
            </section>
          </section>

          <section v-if="activeSection === 'active'" class="panel monitoring-panel">
            <div class="panel-header compact">
              <div>
                <h2 class="panel-title">Активни тестове</h2>
                <p class="muted">Кой решава в момента или има незавършен тест.</p>
              </div>
              <AlertTriangle v-if="hasActiveAttempts" :size="23" />
              <ShieldCheck v-else :size="23" />
            </div>

            <div v-if="monitoring.active.length === 0" class="empty-state">Няма активни тестове.</div>
            <div v-else class="monitor-list">
              <article v-for="attempt in monitoring.active" :key="attempt.attemptId" class="monitor-row">
                <div>
                  <strong>{{ attempt.displayName }}</strong>
                  <span>{{ categoryLabels[attempt.category] }} · {{ modeLabels[attempt.mode] }} · Ниво {{ attempt.difficulty }}</span>
                </div>
                <div class="monitor-row-meta">
                  <b>{{ scoreLabel(attempt) }}</b>
                  <small><Timer :size="16" /> {{ formatDuration(attempt.durationSeconds) }}</small>
                  <small>{{ formatDateTime(attempt.startedAt) }}</small>
                  <button class="mini-button" type="button" @click="editAttempt(attempt.attemptId)">
                    <Eye :size="16" />
                    <span>Редактирай тест</span>
                  </button>
                </div>
              </article>
            </div>
          </section>

          <section v-if="activeSection === 'reports'" class="panel monitoring-panel">
            <div class="panel-header compact">
              <div>
                <h2 class="panel-title">Доклади за нередности</h2>
                <p class="muted">Последните доклади от задачите.</p>
              </div>
              <AlertTriangle :size="23" />
            </div>

            <div v-if="monitoring.recentReports.length === 0" class="empty-state">Няма докладвани нередности.</div>
            <div v-else class="report-list">
              <article v-for="report in monitoring.recentReports" :key="report.id" class="report-row" :class="report.status.toLowerCase()">
                <div>
                  <strong>{{ reportStatusLabel(report.status) }} · {{ report.displayName }}</strong>
                  <span>{{ modeLabels[report.mode] }} · задача {{ report.questionId }} · {{ formatDateTime(report.createdAt) }}</span>
                  <p>{{ report.message }}</p>
                  <small>{{ report.questionPrompt }}</small>
                </div>
                <div class="report-actions">
                  <textarea v-model="reportNotes[report.id]" placeholder="Бележка за обработката"></textarea>
                  <button class="mini-button" type="button" @click="editAttempt(report.attemptId)">
                    <Eye :size="16" />
                    <span>Редактирай тест</span>
                  </button>
                  <button class="mini-button green" type="button" @click="updateReport(report, 'RESOLVED')">Решен</button>
                  <button class="mini-button" type="button" @click="updateReport(report, 'DISMISSED')">Няма проблем</button>
                </div>
              </article>
            </div>
          </section>

          <section v-if="activeSection === 'suggestions'" class="panel monitoring-panel">
            <div class="panel-header compact">
              <div>
                <h2 class="panel-title">Предложения за развитие</h2>
                <p class="muted">Общи идеи или предложения към конкретен предмет, задача и ниво.</p>
              </div>
              <MessageSquareText :size="23" />
            </div>

            <div v-if="monitoring.recentSuggestions.length === 0" class="empty-state">Няма изпратени предложения.</div>
            <div v-else class="report-list">
              <article
                v-for="suggestion in monitoring.recentSuggestions"
                :key="suggestion.id"
                class="report-row"
                :class="suggestion.status.toLowerCase()"
              >
                <div>
                  <strong>{{ reportStatusLabel(suggestion.status) }} · {{ suggestion.displayName }}</strong>
                  <span>
                    {{ suggestionTargetLabel(suggestion) }} ·
                    {{ formatDateTime(suggestion.createdAt) }}
                  </span>
                  <p>{{ suggestion.message }}</p>
                </div>
                <div class="report-actions">
                  <textarea v-model="suggestionNotes[suggestion.id]" placeholder="Бележка за предложението"></textarea>
                  <button class="mini-button green" type="button" @click="updateSuggestion(suggestion, 'RESOLVED')">Прието</button>
                  <button class="mini-button" type="button" @click="updateSuggestion(suggestion, 'DISMISSED')">Остави за по-късно</button>
                </div>
              </article>
            </div>
          </section>

          <section v-if="activeSection === 'users'" class="panel monitoring-panel">
            <div class="panel-header compact">
              <div>
                <h2 class="panel-title">Акаунти</h2>
                <p class="muted">Общ поглед по потребители.</p>
              </div>
              <Users :size="23" />
            </div>

            <div class="table-toolbar">
              <label>
                <span>На страница</span>
                <select v-model.number="usersPageSize" @change="usersPage = 1">
                  <option :value="10">10</option>
                  <option :value="20">20</option>
                </select>
              </label>
            </div>
            <div class="monitor-table users-table">
              <div class="monitor-table-head">
                <span>Име</span>
                <span>Тестове</span>
                <span>Активни</span>
                <span>Средна оценка</span>
                <span>Последна активност</span>
              </div>
              <div v-for="user in pagedUsers" :key="user.userId" class="monitor-table-row">
                <span>
                  <strong>{{ user.displayName }}</strong>
                  <small>{{ user.username }}</small>
                </span>
                <span>{{ user.completedAttempts }}/{{ user.totalAttempts }}</span>
                <span>{{ user.activeAttempts }}</span>
                <span>{{ formatGrade(user.averageGrade) }}</span>
                <span>{{ formatDateTime(user.lastActivityAt) }}</span>
              </div>
            </div>
            <div class="pagination-row">
              <button class="mini-button" type="button" :disabled="usersPage === 1" @click="previousUsersPage">Назад</button>
              <strong>{{ usersPage }}/{{ usersPageCount }}</strong>
              <button class="mini-button" type="button" :disabled="usersPage === usersPageCount" @click="nextUsersPage">Напред</button>
            </div>
          </section>

          <p class="monitoring-updated">Последно обновяване: {{ generatedAt }}</p>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.admin-monitoring-page,
.section-stack {
  display: grid;
  gap: 18px;
}

.monitoring-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
}

.monitoring-hero h1 {
  margin: 2px 0 6px;
  font-size: clamp(2rem, 4vw, 3rem);
}

.period-controls,
.table-toolbar,
.pagination-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.period-controls label,
.table-toolbar label {
  display: grid;
  gap: 5px;
  min-width: 140px;
  color: var(--muted);
  font-weight: 800;
}

.period-controls input,
.table-toolbar select {
  width: 100%;
  min-height: 42px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 0 12px;
  color: var(--ink);
  background: #ffffff;
  font: inherit;
  font-weight: 800;
}

.monitoring-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.monitoring-nav {
  position: sticky;
  top: 14px;
  display: grid;
  gap: 8px;
  padding: 12px;
}

.monitoring-nav button {
  display: block;
  min-height: 44px;
  border: 1px solid transparent;
  border-radius: var(--radius);
  padding: 0 12px;
  color: var(--ink);
  background: transparent;
  font: inherit;
  font-weight: 900;
  text-align: left;
}

.monitoring-nav button.active {
  border-color: rgba(30, 157, 116, 0.3);
  color: var(--green-dark);
  background: rgba(30, 157, 116, 0.09);
}

.monitoring-content {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.monitoring-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
}

.metric-card {
  display: grid;
  gap: 8px;
  min-height: 132px;
  padding: 18px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  background: rgba(255, 253, 247, 0.95);
}

.metric-card svg {
  color: var(--blue);
}

.metric-card.warn svg {
  color: var(--amber);
}

.metric-card span,
.metric-card small {
  color: var(--muted);
  font-weight: 800;
}

.metric-card strong {
  font-size: clamp(2rem, 4vw, 3rem);
}

.monitoring-panel {
  overflow: hidden;
  box-shadow: none;
}

.panel-header.compact {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(36, 48, 74, 0.1);
}

.panel-header.compact svg {
  color: var(--green);
  flex: 0 0 auto;
}

.panel-header.compact .muted {
  margin: 4px 0 0;
}

.monitor-list {
  display: grid;
}

.monitor-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  padding: 16px 20px;
  border-top: 1px solid rgba(36, 48, 74, 0.08);
}

.monitor-row:first-child {
  border-top: 0;
}

.monitor-row strong,
.monitor-row span {
  display: block;
}

.monitor-row span,
.monitor-row small,
.monitor-table small {
  color: var(--muted);
  font-weight: 700;
}

.monitor-row-meta {
  display: grid;
  justify-items: end;
  gap: 3px;
  text-align: right;
}

.monitor-row-meta small {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.mini-button {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 0 10px;
  color: var(--ink);
  background: #ffffff;
  font-weight: 900;
}

.mini-button.green {
  border-color: rgba(30, 157, 116, 0.34);
  color: var(--green-dark);
}

.mini-button:disabled {
  opacity: 0.5;
}

.empty-state {
  padding: 26px 20px;
  color: var(--muted);
  font-weight: 800;
}

.monitor-table {
  display: grid;
  padding: 0 20px 20px;
}

.monitor-table-head,
.monitor-table-row {
  display: grid;
  gap: 12px;
  align-items: center;
  padding: 13px 0;
  border-bottom: 1px solid rgba(36, 48, 74, 0.08);
}

.monitor-table-head {
  color: var(--muted);
  font-weight: 900;
}

.monitor-table-row span:first-child {
  display: grid;
}

.mode-table .monitor-table-head,
.mode-table .monitor-table-row {
  grid-template-columns: 0.95fr 1.25fr 0.5fr 0.75fr 0.75fr 0.7fr 0.7fr 0.7fr;
}

.completed-table .monitor-table-head,
.completed-table .monitor-table-row {
  grid-template-columns: 1.25fr 1.25fr 0.8fr 0.7fr 0.7fr;
}

.nested-table {
  padding: 0;
}

.nested-table .monitor-table-head,
.nested-table .monitor-table-row {
  grid-template-columns: 1.3fr 0.8fr 0.7fr 0.7fr 0.7fr;
}

.users-table .monitor-table-head,
.users-table .monitor-table-row {
  grid-template-columns: 1.4fr 0.7fr 0.7fr 0.9fr 1fr;
}

.table-toolbar {
  padding: 14px 20px 0;
}

.filters-toolbar {
  justify-content: flex-start;
}

.mode-detail-row {
  display: grid;
  grid-column: 1 / -1;
  gap: 12px;
  margin: 0 0 8px;
  border: 1px solid rgba(30, 157, 116, 0.2);
  border-radius: var(--radius);
  padding: 14px;
  background: rgba(30, 157, 116, 0.05);
}

.mode-detail-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mode-detail-header span,
.mode-detail-header label {
  color: var(--muted);
  font-weight: 800;
}

.mode-detail-header label {
  display: grid;
  gap: 4px;
  min-width: 110px;
}

.mode-detail-header select {
  min-height: 38px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 0 10px;
  color: var(--ink);
  background: #ffffff;
  font: inherit;
  font-weight: 800;
}

.pagination-row {
  justify-content: center;
  padding: 0 20px 20px;
}

.usage-chart {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(34px, 1fr));
  gap: 6px;
  align-items: end;
  padding: 0 20px 12px;
  overflow-x: auto;
}

.usage-bar {
  display: grid;
  min-width: 34px;
  gap: 6px;
  justify-items: center;
}

.usage-bar > div {
  display: flex;
  height: 96px;
  align-items: end;
  gap: 3px;
}

.usage-bar b {
  display: block;
  width: 8px;
  border-radius: 5px 5px 0 0;
}

.usage-bar .started {
  background: var(--blue);
}

.usage-bar .completed {
  background: var(--green);
}

.usage-bar span,
.usage-bar small,
.chart-legend {
  color: var(--muted);
  font-size: 0.78rem;
  font-weight: 800;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 0 20px 18px;
}

.chart-legend span,
.chart-legend b {
  width: 16px;
  height: 8px;
  border-radius: 999px;
}

.chart-legend span {
  background: var(--blue);
}

.chart-legend b {
  background: var(--green);
}

.report-list,
.question-review-list {
  display: grid;
  gap: 12px;
  padding: 0 20px 20px;
}

.report-row,
.question-review-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.58fr);
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  background: #ffffff;
}

.report-row.open,
.question-review-row.reported {
  border-color: rgba(245, 185, 66, 0.5);
  background: rgba(245, 185, 66, 0.08);
}

.report-row.resolved,
.question-review-row.correct {
  border-color: rgba(30, 157, 116, 0.26);
}

.report-row.dismissed,
.question-review-row.wrong {
  border-color: rgba(217, 75, 75, 0.26);
}

.report-row strong,
.report-row span,
.report-row small,
.question-review-main strong,
.question-review-main small {
  display: block;
}

.report-row p {
  margin: 8px 0;
  font-weight: 800;
}

.report-actions {
  display: flex;
  flex-wrap: wrap;
  align-content: start;
  gap: 8px;
}

.report-actions textarea {
  width: 100%;
  min-height: 68px;
  resize: vertical;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 10px;
  font: inherit;
}

.review-panel {
  border-color: rgba(63, 125, 217, 0.28);
}

.question-review-row.correct {
  background: rgba(30, 157, 116, 0.05);
}

.question-review-row.wrong {
  background: rgba(217, 75, 75, 0.05);
}

.question-chip {
  display: inline-flex;
  width: fit-content;
  margin-bottom: 8px;
  border-radius: 999px;
  padding: 5px 10px;
  color: #ffffff;
  background: var(--green);
  font-weight: 900;
}

.review-values {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.review-values span {
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 4px 8px;
  background: rgba(255, 253, 247, 0.9);
  font-weight: 800;
}

.question-review-edit {
  display: grid;
  align-content: start;
  gap: 10px;
}

.question-review-edit textarea {
  min-height: 72px;
  resize: vertical;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 10px;
  font: inherit;
}

.inline-reports {
  display: grid;
  gap: 4px;
  border-top: 1px solid rgba(36, 48, 74, 0.08);
  padding-top: 8px;
}

.inline-reports p {
  margin: 0;
  color: var(--muted);
  font-weight: 800;
}

.category-monitor {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 0 20px 20px;
}

.category-monitor article {
  display: grid;
  gap: 6px;
  padding: 16px;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  background: #ffffff;
}

.category-monitor span,
.category-monitor small {
  color: var(--muted);
  font-weight: 800;
}

.category-monitor strong {
  font-size: 2rem;
}

.monitoring-updated {
  margin: 0;
  color: var(--muted);
  font-weight: 800;
  text-align: right;
}

@media (max-width: 1100px) {
  .monitoring-shell {
    grid-template-columns: 1fr;
  }

  .monitoring-nav {
    position: static;
    display: flex;
    overflow-x: auto;
  }

  .monitoring-nav button {
    flex: 0 0 auto;
  }
}

@media (max-width: 980px) {
  .monitoring-stats,
  .category-monitor {
    grid-template-columns: 1fr;
  }

  .monitor-row,
  .monitor-table-head,
  .monitor-table-row,
  .mode-table .monitor-table-head,
  .mode-table .monitor-table-row,
  .completed-table .monitor-table-head,
  .completed-table .monitor-table-row,
  .users-table .monitor-table-head,
  .users-table .monitor-table-row,
  .nested-table .monitor-table-head,
  .nested-table .monitor-table-row,
  .report-row,
  .question-review-row {
    grid-template-columns: 1fr;
  }

  .monitor-row-meta {
    justify-items: start;
    text-align: left;
  }
}

@media (max-width: 680px) {
  .monitoring-hero {
    align-items: stretch;
    grid-template-columns: 1fr;
  }

  .period-controls {
    justify-content: stretch;
  }

  .period-controls label,
  .period-controls button {
    width: 100%;
  }
}
</style>
