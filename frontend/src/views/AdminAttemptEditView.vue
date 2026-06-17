<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowLeft, Save } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/services/api'
import { formatDuration, modeLabels } from '@/services/labels'
import type {
  AdminAttemptDetailResponse,
  AdminQuestionReviewRow,
  IssueReportStatus,
  QuestionIssueReportResponse
} from '@/types'

const route = useRoute()
const router = useRouter()
const selectedAttempt = ref<AdminAttemptDetailResponse | null>(null)
const editedAnswers = ref<Record<number, string>>({})
const reportNotes = ref<Record<number, string>>({})
const selectedOwnerId = ref<number | null>(null)
const loading = ref(false)
const savingQuestionId = ref<number | null>(null)
const savingOwner = ref(false)
const error = ref('')

onMounted(() => {
  loadAttempt()
})

async function loadAttempt() {
  const attemptId = String(route.params.attemptId)
  loading.value = true
  error.value = ''
  try {
    selectedAttempt.value = await api.get<AdminAttemptDetailResponse>(`/admin/monitoring/attempts/${attemptId}`)
    selectedOwnerId.value = selectedAttempt.value.attempt.userId
    editedAnswers.value = Object.fromEntries(
      selectedAttempt.value.questions.map((question) => [question.questionId, question.answer])
    )
    reportNotes.value = Object.fromEntries(
      selectedAttempt.value.questions.flatMap((question) =>
        question.reports.map((report) => [report.id, report.adminNote ?? ''])
      )
    )
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Тестът не можа да се зареди.'
  } finally {
    loading.value = false
  }
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

function scoreLabel() {
  const attempt = selectedAttempt.value?.attempt
  if (!attempt) {
    return '-'
  }
  if (attempt.score === null) {
    return `${attempt.answeredCount}/${attempt.totalQuestions} отговорени`
  }
  return `${attempt.score}/${attempt.totalQuestions}`
}

function backToMonitoring() {
  const section = typeof route.query.section === 'string' ? route.query.section : 'types'
  router.push({ name: 'adminMonitoring', query: { section } })
}

async function saveAnswer(question: AdminQuestionReviewRow) {
  if (!selectedAttempt.value) {
    return
  }
  savingQuestionId.value = question.questionId
  error.value = ''
  try {
    selectedAttempt.value = await api.put<AdminAttemptDetailResponse>(
      `/admin/monitoring/attempts/${selectedAttempt.value.attempt.attemptId}/answers/${question.questionId}`,
      { answer: editedAnswers.value[question.questionId] ?? '' }
    )
    editedAnswers.value = Object.fromEntries(
      selectedAttempt.value.questions.map((row) => [row.questionId, row.answer])
    )
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Отговорът не беше записан.'
  } finally {
    savingQuestionId.value = null
  }
}

async function saveOwner() {
  if (!selectedAttempt.value || selectedOwnerId.value === null) {
    return
  }
  savingOwner.value = true
  error.value = ''
  try {
    selectedAttempt.value = await api.put<AdminAttemptDetailResponse>(
      `/admin/monitoring/attempts/${selectedAttempt.value.attempt.attemptId}/owner`,
      { childId: selectedOwnerId.value }
    )
    selectedOwnerId.value = selectedAttempt.value.attempt.userId
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Профилът за теста не беше записан.'
  } finally {
    savingOwner.value = false
  }
}

async function updateReport(report: QuestionIssueReportResponse, status: IssueReportStatus) {
  error.value = ''
  try {
    await api.put<QuestionIssueReportResponse>(`/admin/monitoring/issue-reports/${report.id}`, {
      status,
      adminNote: reportNotes.value[report.id] ?? report.adminNote ?? ''
    })
    await loadAttempt()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Докладът не беше обновен.'
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
</script>

<template>
  <section class="page admin-attempt-page">
    <section class="panel attempt-hero">
      <button class="button secondary" type="button" @click="backToMonitoring">
        <ArrowLeft :size="20" />
        <span>Назад към мониторинг</span>
      </button>
      <div>
        <p class="eyebrow">Админ</p>
        <h1>Редакция на тест</h1>
        <p v-if="selectedAttempt" class="muted">
          {{ selectedAttempt.attempt.displayName }} · {{ modeLabels[selectedAttempt.attempt.mode] }} ·
          {{ scoreLabel() }} · оценка {{ formatGrade(selectedAttempt.attempt.grade) }} ·
          {{ formatDuration(selectedAttempt.attempt.durationSeconds) }}
        </p>
      </div>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading" class="panel empty-state">Зареждане на теста...</div>

    <section v-if="selectedAttempt" class="panel monitoring-panel review-panel">
      <div class="panel-header compact">
        <div>
          <h2 class="panel-title">Въпроси и отговори</h2>
          <p class="muted">Завършен: {{ formatDateTime(selectedAttempt.attempt.completedAt) }}</p>
        </div>
        <div class="owner-editor">
          <label class="field owner-field">
            <span>Решавал</span>
            <select v-model.number="selectedOwnerId">
              <option v-for="child in selectedAttempt.children" :key="child.id" :value="child.id">
                {{ child.displayName }}
              </option>
            </select>
          </label>
          <button
            class="mini-button green"
            type="button"
            :disabled="savingOwner || selectedOwnerId === selectedAttempt.attempt.userId"
            @click="saveOwner"
          >
            <Save :size="16" />
            <span>Запази профил</span>
          </button>
        </div>
      </div>

      <div class="question-review-list">
        <article
          v-for="question in selectedAttempt.questions"
          :key="question.questionId"
          class="question-review-row"
          :class="{ correct: question.correct, wrong: !question.correct, reported: question.reports.length > 0 }"
        >
          <div class="question-review-main">
            <span class="question-chip">Задача {{ question.questionId }}</span>
            <strong>{{ question.prompt }}</strong>
            <small v-if="question.sourceMode">{{ modeLabels[question.sourceMode] }}</small>
            <small>Верен отговор: {{ question.publicCorrectAnswer }}</small>
            <div v-if="question.choices.length" class="review-values">
              <span v-for="choice in question.choices" :key="choice">{{ choice }}</span>
            </div>
          </div>
          <div class="question-review-edit">
            <label class="field">
              <span>Отговор</span>
              <textarea v-model="editedAnswers[question.questionId]"></textarea>
            </label>
            <button class="mini-button green" type="button" :disabled="savingQuestionId === question.questionId" @click="saveAnswer(question)">
              <Save :size="16" />
              <span>Запази</span>
            </button>
            <div v-if="question.reports.length" class="inline-reports">
              <strong>Доклади</strong>
              <article v-for="report in question.reports" :key="report.id" class="inline-report-row">
                <p>{{ reportStatusLabel(report.status) }}: {{ report.message }}</p>
                <textarea v-model="reportNotes[report.id]" placeholder="Бележка"></textarea>
                <div>
                  <button class="mini-button green" type="button" @click="updateReport(report, 'RESOLVED')">Решен</button>
                  <button class="mini-button" type="button" @click="updateReport(report, 'DISMISSED')">Няма проблем</button>
                </div>
              </article>
            </div>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.admin-attempt-page {
  display: grid;
  gap: 18px;
}

.attempt-hero {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px;
}

.attempt-hero h1 {
  margin: 2px 0 6px;
  font-size: clamp(2rem, 4vw, 3rem);
}

.monitoring-panel {
  overflow: hidden;
  box-shadow: none;
}

.panel-header.compact {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(36, 48, 74, 0.1);
}

.owner-editor {
  display: flex;
  align-items: end;
  gap: 10px;
}

.owner-field {
  min-width: 220px;
}

.owner-field select {
  min-height: 42px;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 0 12px;
  color: var(--ink);
  background: #ffffff;
  font: inherit;
  font-weight: 900;
}

.empty-state {
  padding: 26px 20px;
  color: var(--muted);
  font-weight: 800;
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

.question-review-list {
  display: grid;
  gap: 12px;
  padding: 0 20px 20px;
}

.question-review-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.58fr);
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  background: #ffffff;
}

.question-review-row.reported {
  border-color: rgba(245, 185, 66, 0.5);
  background: rgba(245, 185, 66, 0.08);
}

.question-review-row.correct {
  border-color: rgba(30, 157, 116, 0.26);
  background: rgba(30, 157, 116, 0.05);
}

.question-review-row.wrong {
  border-color: rgba(217, 75, 75, 0.26);
  background: rgba(217, 75, 75, 0.05);
}

.question-review-main strong,
.question-review-main small {
  display: block;
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

.question-review-edit textarea,
.inline-report-row textarea {
  min-height: 72px;
  resize: vertical;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 10px;
  font: inherit;
}

.inline-reports {
  display: grid;
  gap: 8px;
  border-top: 1px solid rgba(36, 48, 74, 0.08);
  padding-top: 8px;
}

.inline-report-row {
  display: grid;
  gap: 8px;
}

.inline-report-row p {
  margin: 0;
  color: var(--muted);
  font-weight: 800;
}

@media (max-width: 980px) {
  .attempt-hero,
  .panel-header.compact,
  .question-review-row {
    grid-template-columns: 1fr;
  }

  .panel-header.compact,
  .attempt-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .owner-editor {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
