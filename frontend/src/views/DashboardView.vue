<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  BookOpen,
  Brain,
  Calculator,
  Divide,
  Equal,
  Eye,
  Globe2,
  HelpCircle,
  Image as ImageIcon,
  Keyboard,
  Layers,
  Lock,
  GalleryHorizontalEnd,
  Gem,
  MessageSquareText,
  Minus,
  Palette,
  PlayCircle,
  Plus,
  Boxes,
  Search,
  Shuffle,
  SpellCheck2,
  Trash2,
  X as MultiplyIcon
} from 'lucide-vue-next'
import { useQuizStore } from '@/stores/quiz'
import { api } from '@/services/api'
import {
  bulgarianPrimitiveModes,
  categoryLabels,
  formatDuration,
  levelRange,
  logicPrimitiveModes,
  mathPrimitiveModes,
  modeLabels,
  modeShortLabels
} from '@/services/labels'
import type { QuizCategory, QuizMode } from '@/types'

const router = useRouter()
const quiz = useQuizStore()
const suggestionOpen = ref(false)
const suggestionMessage = ref('')
const suggestionSending = ref(false)
const suggestionSuccess = ref('')
const suggestionError = ref('')
const suggestionTarget = ref<'GENERAL' | 'SPECIFIC'>('SPECIFIC')
const suggestionCategory = ref<QuizCategory>(quiz.selectedCategory)
const suggestionSelectedMode = ref<QuizMode>(quiz.selectedMode)
const suggestionDifficulty = ref(quiz.difficulty)

const subjectOptions = [
  { category: 'MATH' as QuizCategory, title: 'Математика', label: 'Числа и знаци', icon: Calculator, enabled: true },
  { category: 'BULGARIAN' as QuizCategory, title: 'Български', label: 'Букви и думи', icon: BookOpen, enabled: true },
  { category: 'LOGIC' as QuizCategory, title: 'Логика', label: 'Откриване и мислене', icon: Brain, enabled: true },
  { category: 'MATH' as QuizCategory, title: 'Околен свят', label: 'Скоро', icon: Globe2, enabled: false }
]

const mathModeOptions = [
  { mode: 'ADDITION' as QuizMode, icon: Plus, accent: 'green' },
  { mode: 'SUBTRACTION' as QuizMode, icon: Minus, accent: 'coral' },
  { mode: 'MIXED' as QuizMode, icon: Shuffle, accent: 'blue' },
  { mode: 'MULTIPLICATION' as QuizMode, icon: MultiplyIcon, accent: 'green' },
  { mode: 'DIVISION' as QuizMode, icon: Divide, accent: 'coral' },
  { mode: 'MULTIPLICATION_DIVISION' as QuizMode, icon: Shuffle, accent: 'blue' },
  { mode: 'UNKNOWN_ADDITION' as QuizMode, icon: HelpCircle, accent: 'violet' },
  { mode: 'UNKNOWN_SUBTRACTION' as QuizMode, icon: HelpCircle, accent: 'amber' },
  { mode: 'UNKNOWN_MIXED' as QuizMode, icon: HelpCircle, accent: 'blue' },
  { mode: 'COMPARE' as QuizMode, icon: Equal, accent: 'ink' }
]

const bulgarianModeOptions = [
  { mode: 'WORD_LETTERS' as QuizMode, icon: Shuffle, accent: 'green' },
  { mode: 'WORD_SYLLABLES' as QuizMode, icon: Layers, accent: 'amber' },
  { mode: 'WORD_TYPING' as QuizMode, icon: Keyboard, accent: 'blue' },
  { mode: 'WORD_PICTURE' as QuizMode, icon: ImageIcon, accent: 'violet' },
  { mode: 'WORD_MISSING_LETTER' as QuizMode, icon: HelpCircle, accent: 'coral' },
  { mode: 'WORD_FIRST_LETTER_GROUP' as QuizMode, icon: Boxes, accent: 'green' },
  { mode: 'WORD_WRONG_LETTER' as QuizMode, icon: SpellCheck2, accent: 'amber' }
]

const logicModeOptions = [
  { mode: 'FIND_OBJECT' as QuizMode, icon: Search, accent: 'green' },
  { mode: 'SPOT_DIFFERENCES' as QuizMode, icon: Eye, accent: 'blue' },
  { mode: 'MEMORY_PAIRS' as QuizMode, icon: GalleryHorizontalEnd, accent: 'violet' },
  { mode: 'PATTERN_SEQUENCE' as QuizMode, icon: Boxes, accent: 'amber' }
]

const suggestionCategoryOptions = [
  { category: 'MATH' as QuizCategory, label: categoryLabels.MATH },
  { category: 'BULGARIAN' as QuizCategory, label: categoryLabels.BULGARIAN },
  { category: 'LOGIC' as QuizCategory, label: categoryLabels.LOGIC }
]

const modeOptions = computed(() => {
  if (quiz.selectedCategory === 'BULGARIAN') {
    return bulgarianModeOptions
  }
  if (quiz.selectedCategory === 'LOGIC') {
    return logicModeOptions
  }
  return mathModeOptions
})
const primitiveForCategory = computed(() =>
  quiz.selectedCategory === 'BULGARIAN'
    ? bulgarianPrimitiveModes
    : quiz.selectedCategory === 'LOGIC'
    ? logicPrimitiveModes
    : mathPrimitiveModes
)
const groupedOptions = computed(() => modeOptions.value.filter((option) => primitiveForCategory.value.includes(option.mode)))
const selectedRange = computed(() => {
  if (quiz.selectedCategory === 'LOGIC' && quiz.selectedMode === 'MEMORY_PAIRS') {
    return `${Math.min(12, Math.max(3, quiz.difficulty + 2))} двойки`
  }
  if (quiz.selectedCategory === 'LOGIC' && quiz.selectedMode === 'PATTERN_SEQUENCE') {
    return `${Math.min(13, Math.max(4, quiz.difficulty + 3))} фигури`
  }
  return levelRange(quiz.difficulty, quiz.selectedCategory)
})
const selectedTaskLabel = computed(() => {
  if (quiz.selectedCategory === 'MATH') {
    return '20 задачи'
  }
  if (quiz.selectedCategory === 'LOGIC') {
    return quiz.selectedMode === 'MEMORY_PAIRS' ? '1 игра' : '5 задачи'
  }
  return '10 задачи'
})
const planOptions = computed(() => quiz.selectedCategory === 'LOGIC' ? ['SINGLE'] : ['SINGLE', 'CUSTOM', 'ALL'])
const suggestionModeOptions = computed(() => taskOptionsForCategory(suggestionCategory.value))
const canStart = computed(() => quiz.testPlan !== 'CUSTOM' || quiz.selectedIncludedModes.length >= 2)

watch(
  () => quiz.selectedCategory,
  (category) => {
    const defaults = category === 'BULGARIAN'
      ? bulgarianPrimitiveModes
      : category === 'LOGIC'
      ? logicPrimitiveModes
      : mathPrimitiveModes
    if (!defaults.includes(quiz.selectedMode)) {
      quiz.selectedMode = defaults[0]
    }
    quiz.selectedIncludedModes = defaults.slice(0, Math.min(2, defaults.length))
    if (!planOptions.value.includes(quiz.testPlan)) {
      quiz.testPlan = 'SINGLE'
    }
  },
  { immediate: true }
)

watch(
  () => suggestionCategory.value,
  (category) => {
    const allowedModes = taskOptionsForCategory(category).map((option) => option.mode)
    if (!allowedModes.includes(suggestionSelectedMode.value)) {
      suggestionSelectedMode.value = allowedModes[0]
    }
  }
)

watch(
  () => [quiz.selectedCategory, quiz.selectedMode, quiz.difficulty] as const,
  () => {
    if (!suggestionOpen.value) {
      prepareSuggestionContext()
    }
  }
)

watch(
  () => [quiz.selectedCategory, quiz.testPlan, quiz.selectedMode, quiz.difficulty] as const,
  () => {
    quiz.fetchRecentAttempts().catch(() => {
      quiz.recentAttempts = []
    })
  },
  { immediate: true }
)

onMounted(() => {
  quiz.fetchActiveAttempts().catch(() => {
    quiz.activeAttempts = []
  })
  quiz.fetchCrystalLeaderboard().catch(() => {
    quiz.crystalLeaderboard = []
  })
  quiz.fetchTotalCrystals().catch(() => {
    quiz.totalCrystals = 0
  })
})

async function startQuiz() {
  if (!canStart.value) {
    return
  }
  const attempt = await quiz.start()
  router.push({ name: 'quiz', params: { attemptId: attempt.attemptId } })
}

function toggleIncluded(mode: QuizMode) {
  if (quiz.selectedIncludedModes.includes(mode)) {
    quiz.selectedIncludedModes = quiz.selectedIncludedModes.filter((selected) => selected !== mode)
    return
  }
  quiz.selectedIncludedModes = [...quiz.selectedIncludedModes, mode]
}

function answeredCount(attemptId: string) {
  return quiz.activeAttempts.find((attempt) => attempt.attemptId === attemptId)?.answers.length ?? 0
}

function taskOptionsForCategory(category: QuizCategory) {
  if (category === 'BULGARIAN') {
    return bulgarianModeOptions
  }
  if (category === 'LOGIC') {
    return logicModeOptions
  }
  return mathModeOptions
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('bg-BG', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function formatGrade(value: string | number | null) {
  if (value === null || value === undefined) {
    return '-'
  }
  return Number(value).toLocaleString('bg-BG', { maximumFractionDigits: 2 })
}

function prepareSuggestionContext() {
  suggestionCategory.value = quiz.selectedCategory
  const categoryOptions = taskOptionsForCategory(quiz.selectedCategory)
  const currentModeIsAvailable = categoryOptions.some((option) => option.mode === quiz.selectedMode)
  suggestionSelectedMode.value = currentModeIsAvailable ? quiz.selectedMode : categoryOptions[0].mode
  suggestionDifficulty.value = quiz.difficulty
}

function toggleSuggestionForm() {
  if (!suggestionOpen.value) {
    prepareSuggestionContext()
    suggestionSuccess.value = ''
    suggestionError.value = ''
  }
  suggestionOpen.value = !suggestionOpen.value
}

async function cancelAttempt(attemptId: string) {
  await quiz.cancelAttempt(attemptId)
}

async function submitSuggestion() {
  if (!suggestionMessage.value.trim()) {
    suggestionError.value = 'Напиши кратко предложение.'
    return
  }
  suggestionSending.value = true
  suggestionError.value = ''
  suggestionSuccess.value = ''
  try {
    const payload = suggestionTarget.value === 'GENERAL'
      ? { message: suggestionMessage.value.trim() }
      : {
          category: suggestionCategory.value,
          mode: suggestionSelectedMode.value,
          difficulty: suggestionDifficulty.value,
          message: suggestionMessage.value.trim()
        }
    await api.post('/suggestions', payload)
    suggestionSuccess.value = 'Предложението е изпратено.'
    suggestionMessage.value = ''
    suggestionOpen.value = false
  } catch (err) {
    suggestionError.value = err instanceof Error ? err.message : 'Предложението не беше изпратено.'
  } finally {
    suggestionSending.value = false
  }
}
</script>

<template>
  <section class="page dashboard-page">
    <div class="dashboard-grid">
      <section class="panel setup-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Предмет</p>
            <h1 class="panel-title">Избор на тест</h1>
          </div>
        </div>

        <div class="subject-grid">
          <button
            v-for="subject in subjectOptions"
            :key="subject.title"
            type="button"
            class="subject-card"
            :class="{ active: subject.enabled && quiz.selectedCategory === subject.category, disabled: !subject.enabled }"
            :disabled="!subject.enabled"
            @click="quiz.selectedCategory = subject.category"
          >
            <component :is="subject.icon" :size="24" />
            <span>{{ subject.title }}</span>
            <small>{{ subject.label }}</small>
            <Lock v-if="!subject.enabled" class="lock-icon" :size="16" />
          </button>
        </div>

        <div class="category-band">
          <span>{{ selectedTaskLabel }}</span>
          <span>Проверка сега или на финала</span>
          <span v-if="quiz.testPlan === 'CUSTOM'">Без класация</span>
          <span v-else>Кристали за всеки завършен тест</span>
        </div>

        <div class="section-block">
          <RouterLink class="alphabet-link reward-link" :to="{ name: 'rewardAlbum' }">
            <Palette :size="30" />
            <span>
              <strong>Моят албум</strong>
              <small>Създай картини с кристали</small>
            </span>
            <ArrowRight :size="21" />
          </RouterLink>
        </div>

        <div v-if="quiz.selectedCategory === 'MATH'" class="section-block">
          <RouterLink class="alphabet-link" :to="{ name: 'numbers' }">
            <Calculator :size="30" />
            <span>
              <strong>Цифри и числа</strong>
              <small>Устно упражнение без оценка</small>
            </span>
            <ArrowRight :size="21" />
          </RouterLink>
        </div>

        <div v-if="quiz.selectedCategory === 'BULGARIAN'" class="section-block">
          <RouterLink class="alphabet-link" :to="{ name: 'alphabet' }">
            <BookOpen :size="30" />
            <span>
              <strong>Азбука</strong>
              <small>Устно упражнение без оценка</small>
            </span>
            <ArrowRight :size="21" />
          </RouterLink>
        </div>

        <div class="section-block">
          <h2>Вид тест</h2>
          <div class="plan-tabs" :class="{ single: planOptions.length === 1 }" role="tablist" aria-label="Вид тест">
            <button v-if="planOptions.includes('SINGLE')" type="button" :class="{ active: quiz.testPlan === 'SINGLE' }" @click="quiz.testPlan = 'SINGLE'">
              Единичен
            </button>
            <button v-if="planOptions.includes('CUSTOM')" type="button" :class="{ active: quiz.testPlan === 'CUSTOM' }" @click="quiz.testPlan = 'CUSTOM'">
              Тест по избор
            </button>
            <button v-if="planOptions.includes('ALL')" type="button" :class="{ active: quiz.testPlan === 'ALL' }" @click="quiz.testPlan = 'ALL'">
              Всички
            </button>
          </div>
        </div>

        <div class="section-block">
          <h2>{{ quiz.testPlan === 'CUSTOM' ? 'Категории в теста по избор' : 'Тип задачи' }}</h2>
          <div v-if="quiz.testPlan === 'SINGLE'" class="mode-grid">
            <button
              v-for="option in modeOptions"
              :key="option.mode"
              type="button"
              class="mode-card"
              :class="[{ active: quiz.selectedMode === option.mode }, option.accent]"
              @click="quiz.selectedMode = option.mode"
            >
              <component :is="option.icon" :size="24" />
              <span>{{ modeLabels[option.mode] }}</span>
              <small>{{ modeShortLabels[option.mode] }}</small>
            </button>
          </div>
          <div v-else-if="quiz.testPlan === 'CUSTOM'" class="mode-grid">
            <button
              v-for="option in groupedOptions"
              :key="option.mode"
              type="button"
              class="mode-card compact"
              :class="[{ active: quiz.selectedIncludedModes.includes(option.mode) }, option.accent]"
              @click="toggleIncluded(option.mode)"
            >
              <component :is="option.icon" :size="24" />
              <span>{{ modeLabels[option.mode] }}</span>
              <small>{{ quiz.selectedIncludedModes.includes(option.mode) ? 'Включено' : 'Изключено' }}</small>
            </button>
          </div>
          <div v-else class="all-group">
            <Layers :size="34" />
            <div>
              <strong>Групов тест от всички категории</strong>
              <p class="muted">Има обща класация и отделни точки по тип задача.</p>
            </div>
          </div>
          <p v-if="quiz.testPlan === 'CUSTOM' && !canStart" class="helper-warning">
            Избери поне две категории.
          </p>
        </div>

        <div class="section-block difficulty-block">
          <div>
            <h2>Трудност</h2>
            <p class="muted">
              Ниво {{ quiz.difficulty }} ·
              {{ quiz.selectedCategory === 'MATH' ? 'числа от' : '' }}
              {{ selectedRange }}
            </p>
          </div>
          <input v-model.number="quiz.difficulty" type="range" min="1" max="10" step="1" />
          <div class="level-row" aria-label="Избор на трудност">
            <button
              v-for="level in 10"
              :key="level"
              type="button"
              :class="{ active: quiz.difficulty === level }"
              @click="quiz.difficulty = level"
            >
              {{ level }}
            </button>
          </div>
        </div>

        <button class="button start-button" type="button" :disabled="quiz.loading || !canStart" @click="startQuiz">
          <ArrowRight :size="21" />
          <span>Стартирай тест</span>
        </button>
      </section>

      <aside class="side-column">
        <section v-if="quiz.activeAttempts.length > 0" class="panel active-tests-panel">
          <div class="panel-header">
            <h2 class="panel-title">Незавършени тестове</h2>
            <PlayCircle :size="23" />
          </div>
          <div class="active-tests-list">
            <div v-for="attempt in quiz.activeAttempts" :key="attempt.attemptId" class="active-test-row">
              <div>
                <strong>{{ modeLabels[attempt.mode] }}</strong>
                <span>Ниво {{ attempt.difficulty }} · {{ answeredCount(attempt.attemptId) }}/{{ attempt.totalQuestions }}</span>
                <small>{{ formatDuration(attempt.durationSeconds) }}</small>
              </div>
              <div class="active-test-actions">
                <RouterLink
                  class="button compact-button"
                  :to="{ name: 'quiz', params: { attemptId: attempt.attemptId } }"
                >
                  <PlayCircle :size="18" />
                  <span>Продължи</span>
                </RouterLink>
                <button class="button secondary compact-button" type="button" @click="cancelAttempt(attempt.attemptId)">
                  <Trash2 :size="18" />
                  <span>Откажи</span>
                </button>
              </div>
            </div>
          </div>
        </section>

        <section id="recent-results" class="panel leaderboard-panel">
          <div class="panel-header">
            <h2 class="panel-title">Моите последни 10</h2>
            <Gem :size="24" />
          </div>
          <div class="leaderboard-list">
            <div v-if="quiz.recentAttempts.length === 0" class="empty-state">Още нямаш решени тестове за този избор.</div>
            <RouterLink
              v-for="row in quiz.recentAttempts"
              :key="row.attemptId"
              class="leaderboard-row recent-row"
              :to="{ name: 'result', params: { attemptId: row.attemptId } }"
            >
              <strong>+{{ row.crystals }}</strong>
              <span>{{ row.score }}/{{ row.totalQuestions }} · оценка {{ formatGrade(row.grade) }}</span>
              <b>{{ formatDuration(row.durationSeconds) }}</b>
              <small>{{ modeLabels[row.mode] }} · Ниво {{ row.difficulty }} · {{ formatDateTime(row.completedAt) }}</small>
            </RouterLink>
          </div>
        </section>

        <section class="panel leaderboard-panel">
          <div class="panel-header">
            <h2 class="panel-title">Обща класация</h2>
            <Gem :size="24" />
          </div>
          <div class="leaderboard-list">
            <div v-if="quiz.crystalLeaderboard.length === 0" class="empty-state">Още няма обща класация.</div>
            <div v-for="row in quiz.crystalLeaderboard" :key="row.userId" class="leaderboard-row">
              <strong>{{ row.rank }}</strong>
              <span>{{ row.displayName }}</span>
              <b>+{{ row.crystals }}</b>
              <small>{{ row.completedAttempts }} теста</small>
            </div>
          </div>
        </section>

        <section class="panel suggestion-panel" :class="{ open: suggestionOpen }">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">Предложения</h2>
            </div>
            <MessageSquareText :size="24" />
          </div>
          <button class="button secondary suggestion-toggle" type="button" @click="toggleSuggestionForm">
            <MessageSquareText :size="19" />
            <span>{{ suggestionOpen ? 'Затвори' : 'Дай предложение' }}</span>
          </button>
          <form v-if="suggestionOpen" class="suggestion-form" @submit.prevent="submitSuggestion">
            <div class="suggestion-scope" role="tablist" aria-label="Вид предложение">
              <button type="button" :class="{ active: suggestionTarget === 'GENERAL' }" @click="suggestionTarget = 'GENERAL'">
                Общо
              </button>
              <button type="button" :class="{ active: suggestionTarget === 'SPECIFIC' }" @click="suggestionTarget = 'SPECIFIC'">
                За задача
              </button>
            </div>
            <div v-if="suggestionTarget === 'SPECIFIC'" class="suggestion-fields">
              <label class="field">
                <span>Предмет</span>
                <select v-model="suggestionCategory">
                  <option v-for="subject in suggestionCategoryOptions" :key="subject.category" :value="subject.category">
                    {{ subject.label }}
                  </option>
                </select>
              </label>
              <label class="field">
                <span>Подкатегория</span>
                <select v-model="suggestionSelectedMode">
                  <option v-for="option in suggestionModeOptions" :key="option.mode" :value="option.mode">
                    {{ modeLabels[option.mode] }}
                  </option>
                </select>
              </label>
              <label class="field">
                <span>Ниво</span>
                <select v-model.number="suggestionDifficulty">
                  <option v-for="level in 10" :key="level" :value="level">Ниво {{ level }}</option>
                </select>
              </label>
            </div>
            <label class="field">
              <span>Какво да подобрим?</span>
              <textarea
                v-model="suggestionMessage"
                maxlength="1500"
                placeholder="Например: повече предмети на по-високите нива или нов тип задача."
              ></textarea>
            </label>
            <button class="button" type="submit" :disabled="suggestionSending">
              <ArrowRight :size="19" />
              <span>Изпрати</span>
            </button>
          </form>
          <p v-if="suggestionSuccess" class="success-note">{{ suggestionSuccess }}</p>
          <p v-if="suggestionError" class="error">{{ suggestionError }}</p>
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  align-items: start;
}

.setup-panel {
  display: grid;
  gap: 24px;
  padding-bottom: 22px;
}

.eyebrow {
  margin: 0 0 4px;
  color: var(--green-dark);
  font-size: 0.82rem;
  font-weight: 900;
  text-transform: uppercase;
}

.category-band {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 20px;
}

.category-band span {
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: 999px;
  padding: 8px 12px;
  background: #ffffff;
  color: var(--muted);
  font-weight: 800;
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  padding: 0 20px;
}

.subject-card {
  position: relative;
  display: grid;
  min-height: 94px;
  gap: 7px;
  justify-items: start;
  border: 2px solid transparent;
  border-radius: var(--radius);
  padding: 14px;
  color: var(--ink);
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(36, 48, 74, 0.08);
  text-align: left;
}

.subject-card.active {
  border-color: var(--blue);
}

.subject-card.disabled {
  color: var(--muted);
  background: rgba(255, 255, 255, 0.72);
}

.subject-card span {
  font-weight: 950;
}

.subject-card small {
  color: var(--muted);
  font-weight: 800;
}

.lock-icon {
  position: absolute;
  top: 12px;
  right: 12px;
}

.section-block {
  display: grid;
  gap: 14px;
  padding: 0 20px;
}

.section-block h2 {
  margin: 0;
  font-size: 1.05rem;
}

.alphabet-link {
  display: grid;
  min-height: 86px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  border: 2px solid rgba(30, 157, 116, 0.24);
  border-radius: var(--radius);
  padding: 16px;
  color: var(--green-dark);
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(36, 48, 74, 0.08);
}

.alphabet-link span {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.alphabet-link strong {
  color: var(--ink);
  font-size: 1.2rem;
}

.alphabet-link small {
  color: var(--muted);
  font-weight: 800;
}

.plan-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 6px;
  background: #ffffff;
}

.plan-tabs.single {
  grid-template-columns: 1fr;
}

.plan-tabs button {
  min-height: 42px;
  border: 0;
  border-radius: calc(var(--radius) - 2px);
  color: var(--muted);
  background: transparent;
  font-weight: 900;
}

.plan-tabs button.active {
  color: #ffffff;
  background: var(--blue);
}

.mode-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.mode-card {
  display: grid;
  min-height: 132px;
  grid-template-rows: auto 1fr auto;
  gap: 9px;
  align-content: start;
  justify-items: start;
  border: 2px solid transparent;
  border-radius: var(--radius);
  padding: 15px;
  color: var(--ink);
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(36, 48, 74, 0.08);
  text-align: left;
}

.mode-card span {
  font-weight: 900;
  line-height: 1.15;
}

.mode-card small {
  color: var(--muted);
  font-weight: 800;
}

.mode-card.active {
  border-color: currentColor;
  transform: translateY(-2px);
}

.mode-card.compact {
  min-height: 112px;
}

.mode-card.green {
  color: var(--green-dark);
}

.mode-card.coral {
  color: var(--coral);
}

.mode-card.blue {
  color: var(--blue);
}

.mode-card.violet {
  color: var(--violet);
}

.mode-card.amber {
  color: #a66700;
}

.mode-card.ink {
  color: var(--ink);
}

.difficulty-block {
  gap: 12px;
}

.difficulty-block p {
  margin: 4px 0 0;
}

.all-group {
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid rgba(63, 125, 217, 0.18);
  border-radius: var(--radius);
  padding: 16px;
  background: #ffffff;
}

.all-group strong {
  display: block;
  margin-bottom: 4px;
}

.all-group p {
  margin: 0;
}

.helper-warning {
  margin: 0;
  border-radius: var(--radius);
  padding: 10px 12px;
  color: #8a4e00;
  background: rgba(245, 185, 66, 0.18);
  font-weight: 800;
}

input[type='range'] {
  width: 100%;
  accent-color: var(--green);
}

.level-row {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 6px;
}

.level-row button {
  display: grid;
  min-height: 28px;
  place-items: center;
  border: 0;
  border-radius: var(--radius);
  background: rgba(63, 125, 217, 0.1);
  color: var(--muted);
  font-weight: 900;
  transition:
    transform 160ms ease,
    background 160ms ease,
    color 160ms ease;
}

.level-row button:hover,
.level-row button:focus-visible {
  transform: translateY(-1px);
  background: rgba(63, 125, 217, 0.18);
  outline: none;
}

.level-row button.active {
  color: #ffffff;
  background: var(--blue);
}

.start-button {
  justify-self: start;
  margin-left: 20px;
}

.side-column {
  display: grid;
  gap: 20px;
}

.suggestion-panel {
  display: grid;
  gap: 14px;
}

.suggestion-panel .panel-header {
  padding-bottom: 0;
}

.suggestion-toggle {
  justify-self: start;
  margin-left: 20px;
}

.suggestion-panel:not(.open) .suggestion-toggle {
  margin-bottom: 20px;
}

.suggestion-form {
  display: grid;
  gap: 12px;
  padding: 0 20px 18px;
}

.suggestion-scope {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 5px;
  background: #ffffff;
}

.suggestion-scope button {
  min-height: 38px;
  border: 0;
  border-radius: calc(var(--radius) - 2px);
  color: var(--muted);
  background: transparent;
  font-weight: 900;
}

.suggestion-scope button.active {
  color: #ffffff;
  background: var(--green);
}

.suggestion-fields {
  display: grid;
  gap: 10px;
}

.suggestion-form textarea {
  min-height: 120px;
  resize: vertical;
}

.suggestion-panel .success-note,
.suggestion-panel .error {
  margin: 0 20px 18px;
}

.leaderboard-list,
.active-tests-list {
  display: grid;
  gap: 10px;
  padding: 16px 20px 20px;
}

.leaderboard-row,
.active-test-row {
  display: grid;
  gap: 10px;
  align-items: center;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 10px;
  background: #ffffff;
}

.leaderboard-row {
  grid-template-columns: minmax(42px, auto) minmax(0, 1fr) auto;
  color: var(--ink);
  text-decoration: none;
}

.active-test-row {
  grid-template-columns: minmax(0, 1fr);
}

.active-test-row > div:first-child {
  display: grid;
  gap: 4px;
}

.active-test-row strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 1rem;
}

.active-test-row > div:first-child span,
.active-test-row > div:first-child small {
  color: var(--muted);
  font-weight: 800;
}

.active-test-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.compact-button {
  min-height: 40px;
  padding: 0 12px;
  text-decoration: none;
}

.compact-button span {
  color: inherit;
}

.leaderboard-row strong {
  display: grid;
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  place-items: center;
  border-radius: 999px;
  color: #ffffff;
  background: var(--amber);
}

.leaderboard-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 800;
}

.leaderboard-row small {
  grid-column: 2 / 4;
  color: var(--muted);
  font-weight: 700;
}

.empty-state,
.success {
  border-radius: var(--radius);
  padding: 12px;
  color: var(--muted);
  background: rgba(63, 125, 217, 0.09);
  font-weight: 700;
}

.success {
  color: #126f55;
  background: #def7e9;
}

@media (max-width: 960px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .active-tests-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 680px) {
  .mode-grid,
  .subject-grid,
  .side-column {
    grid-template-columns: 1fr;
  }

  .mode-card {
    min-height: 104px;
  }
}
</style>
