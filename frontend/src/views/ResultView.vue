<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck, Gem, Home, RotateCcw, Sparkles, Target } from 'lucide-vue-next'
import MascotFigure from '@/components/MascotFigure.vue'
import PictureGlyph from '@/components/PictureGlyph.vue'
import { useQuizStore } from '@/stores/quiz'
import { formatDuration, modeLabels } from '@/services/labels'
import type { QuestionResponse, QuestionResultResponse, QuizSummaryResponse } from '@/types'

const route = useRoute()
const router = useRouter()
const quiz = useQuizStore()

const attemptId = computed(() => String(route.params.attemptId))
const attempt = computed(() => quiz.currentAttempt)
const result = computed<QuizSummaryResponse | null>(() => {
  if (quiz.summary?.attemptId === attemptId.value) {
    return quiz.summary
  }
  if (attempt.value?.attemptId === attemptId.value && attempt.value.status === 'COMPLETED') {
    return {
      attemptId: attempt.value.attemptId,
      category: attempt.value.category,
      mode: attempt.value.mode,
      includedModes: attempt.value.includedModes,
      difficulty: attempt.value.difficulty,
      leaderboardEligible: attempt.value.leaderboardEligible,
      score: attempt.value.score ?? 0,
      totalQuestions: attempt.value.totalQuestions,
      grade: attempt.value.grade ?? '2.00',
      medal: attempt.value.medal ?? 'KEEP_GOING',
      crystals: attempt.value.crystals ?? 0,
      completedAt: attempt.value.completedAt ?? '',
      durationSeconds: attempt.value.durationSeconds,
      results: attempt.value.answers,
      scoreBreakdown: attempt.value.scoreBreakdown
    }
  }
  return null
})

const questionById = computed(() => {
  const map = new Map<number, QuestionResponse>()
  attempt.value?.questions.forEach((question) => map.set(question.id, question))
  return map
})

const percentage = computed(() => {
  if (!result.value) {
    return 0
  }
  return Math.round((result.value.score / result.value.totalQuestions) * 100)
})
const crystalTitle = computed(() => (result.value?.crystals === 1 ? 'кристал' : 'кристала'))
const resultRows = computed(() => result.value?.results ?? [])
const awardMood = computed(() => (percentage.value >= 50 ? 'proud' : 'thinking'))

interface PatternSummaryToken {
  key: string
  id: string
  shape: string
  color: string
  label: string
}

onMounted(async () => {
  if (!attempt.value || attempt.value.attemptId !== attemptId.value) {
    await quiz.fetchAttempt(attemptId.value)
  }
  if (attempt.value?.status !== 'COMPLETED') {
    router.replace({ name: 'quiz', params: { attemptId: attemptId.value } })
    return
  }
  quiz.selectedCategory = attempt.value.category
  quiz.testPlan = attempt.value.mode === 'CUSTOM_GROUP' ? 'CUSTOM' : attempt.value.mode === 'ALL_GROUP' ? 'ALL' : 'SINGLE'
  if (attempt.value.mode !== 'CUSTOM_GROUP' && attempt.value.mode !== 'ALL_GROUP') {
    quiz.selectedMode = attempt.value.mode
  }
  quiz.difficulty = attempt.value.difficulty
  quiz.fetchRecentAttempts().catch(() => {
    quiz.recentAttempts = []
  })
  quiz.fetchTotalCrystals().catch(() => {})
})

function promptParts(prompt: string | undefined) {
  return (prompt ?? '').split('?')
}

function isWordQuestion(question: QuestionResponse | undefined) {
  return question?.kind === 'LETTER_ORDER'
    || question?.kind === 'SYLLABLE_ORDER'
    || question?.kind === 'WORD_TYPING'
    || question?.sourceMode === 'WORD_WRONG_LETTER'
}

function tokenFor(answer: QuestionResultResponse) {
  return answer.correct ? answer.answer : answer.correctAnswer
}

function isGroupingQuestion(question: QuestionResponse | undefined) {
  return question?.kind === 'GROUPING'
}

function isSpotDifferencesQuestion(question: QuestionResponse | undefined) {
  return question?.kind === 'SPOT_DIFFERENCES'
}

function isFindObjectQuestion(question: QuestionResponse | undefined) {
  return question?.sourceMode === 'FIND_OBJECT'
}

function isMemoryPairsQuestion(question: QuestionResponse | undefined) {
  return question?.kind === 'MEMORY_PAIRS'
}

function isPatternSequenceQuestion(question: QuestionResponse | undefined) {
  return question?.kind === 'PATTERN_SEQUENCE'
}

function isSudokuQuestion(question: QuestionResponse | undefined) {
  return question?.kind === 'SUDOKU'
}

function findObjectLabel(question: QuestionResponse | undefined, id: string) {
  const item = question?.answerSlots
    .map((slot) => slot.split('|'))
    .find((parts) => parts[0] === 'I' && parts[1] === id)
  return item?.[8] ?? item?.[7] ?? id
}

function countListAnswer(answer: string) {
  return answer.split(';').map((part) => part.trim()).filter(Boolean).length
}

function memoryField(answer: string | undefined, field: string) {
  const prefix = `${field}=`
  return (answer ?? '').split('|').find((part) => part.startsWith(prefix))?.replace(prefix, '') ?? ''
}

function memoryPairCount(question: QuestionResponse | undefined, answer: QuestionResultResponse) {
  const fromAnswer = Number(memoryField(answer.answer, 'pairs'))
  if (Number.isFinite(fromAnswer) && fromAnswer > 0) {
    return fromAnswer
  }
  const header = question?.answerSlots.find((slot) => slot.startsWith('M|'))
  const parts = header?.split('|') ?? []
  return Number(parts[2]) || 0
}

function patternSequenceTokens(question: QuestionResponse | undefined): PatternSummaryToken[] {
  return question?.answerSlots
    .map((slot, index) => ({ parts: slot.split('|'), index }))
    .filter(({ parts }) => parts[0] === 'S' && parts.length >= 5)
    .map(({ parts, index }) => ({
      key: `${parts[1]}-${index}`,
      id: parts[1],
      shape: parts[2],
      color: parts[3],
      label: parts[4]
    })) ?? []
}

function patternTokenStyle(token: PatternSummaryToken) {
  return {
    '--pattern-color': token.color
  }
}

function patternSequenceLabels(question: QuestionResponse | undefined) {
  return patternSequenceTokens(question)
    .map((token) => token.label)
    .join(' → ')
}

function hasPatternSequence(question: QuestionResponse | undefined) {
  return patternSequenceTokens(question).length > 0
}

function patternSequenceFallback(question: QuestionResponse | undefined) {
  return patternSequenceLabels(question) || 'виж правилния модел'
}

function patternSequenceAria(question: QuestionResponse | undefined) {
  return patternSequenceLabels(question) || 'Правилен модел'
}

function spotCorrectCount(answer: QuestionResultResponse) {
  const correct = new Set(answer.correctAnswer.split(';').map((part) => part.trim()).filter(Boolean))
  return answer.answer.split(';').map((part) => part.trim()).filter((part) => correct.has(part)).length
}

function spotWrongCount(answer: QuestionResultResponse) {
  const correct = new Set(answer.correctAnswer.split(';').map((part) => part.trim()).filter(Boolean))
  return answer.answer.split(';').map((part) => part.trim()).filter((part) => part && !correct.has(part)).length
}

function groupingImageByWord(question: QuestionResponse | undefined) {
  const images = new Map<string, string>()
  question?.choices.forEach((choice) => {
    const { image, word } = parseGroupingChoice(choice)
    if (word?.trim()) {
      images.set(word.trim().toUpperCase(), image)
    }
  })
  return images
}

function parseGroupingChoice(choice: string) {
  const separatorIndex = choice.lastIndexOf('|')
  if (separatorIndex < 0) {
    return { image: choice, word: choice }
  }
  return {
    image: choice.slice(0, separatorIndex),
    word: choice.slice(separatorIndex + 1)
  }
}

function groupingSummaryItems(question: QuestionResponse | undefined, answer: QuestionResultResponse) {
  const source = answer.correct ? answer.answer : answer.correctAnswer
  const images = groupingImageByWord(question)
  return source
    .split(';')
    .map((part) => part.trim())
    .filter((part) => part.includes('='))
    .map((part) => {
      const [word, letter] = part.split('=', 2)
      return {
        key: `${word}-${letter}`,
        word: word.trim(),
        image: images.get(word.trim().toUpperCase()) ?? '',
        letter: letter.trim()
      }
    })
}

function answerSummaryLabel(question: QuestionResponse | undefined, answer: QuestionResultResponse) {
  if (!answer.answer) {
    return 'без отговор'
  }
  if (isGroupingQuestion(question)) {
    return answer.correct ? 'подредено правилно' : 'виж правилната подредба'
  }
  if (isSpotDifferencesQuestion(question)) {
    const correctCount = spotCorrectCount(answer)
    const wrongCount = spotWrongCount(answer)
    return wrongCount
      ? `правилни ${correctCount} от ${countListAnswer(answer.correctAnswer)} · грешни ${wrongCount}`
      : `правилни ${correctCount} от ${countListAnswer(answer.correctAnswer)}`
  }
  if (isFindObjectQuestion(question)) {
    return answer.correct
      ? `намери ${findObjectLabel(question, answer.correctAnswer)}`
      : `трябваше ${findObjectLabel(question, answer.correctAnswer)}`
  }
  if (isMemoryPairsQuestion(question)) {
    const attempts = memoryField(answer.answer, 'attempts') || '0'
    return `открити ${memoryPairCount(question, answer)} двойки · ${attempts} опита`
  }
  if (isPatternSequenceQuestion(question)) {
    return answer.correct ? 'моделът е подреден' : 'виж правилния модел'
  }
  if (isSudokuQuestion(question)) {
    return answer.correct ? 'судокуто е решено' : 'виж правилното судоку'
  }
  return answer.answer
}

function sudokuRows(answer: QuestionResultResponse) {
  const source = answer.correct ? answer.answer : answer.correctAnswer
  const rows = source
    .split('/')
    .map((row) => row.trim().replace(/[^0-9]/g, ''))
    .filter(Boolean)
  if (rows.length) {
    return rows
  }
  const normalized = source.replace(/[^0-9]/g, '')
  const size = normalized.length === 16 ? 4 : normalized.length === 81 ? 9 : 0
  if (!size) {
    return []
  }
  return Array.from({ length: size }, (_, row) => normalized.slice(row * size, row * size + size))
}

function wordSummaryToken(question: QuestionResponse | undefined, answer: QuestionResultResponse) {
  if (question?.sourceMode === 'WORD_WRONG_LETTER') {
    return question.speechText ?? answer.correctAnswer
  }
  return answer.correctAnswer
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

</script>

<template>
  <section class="page result-page">
    <div v-if="result" class="result-layout">
      <section class="panel result-main">
        <div class="crystal-visual">
          <Gem :size="62" />
          <Sparkles class="crystal-sparkle" :size="28" />
        </div>
        <p class="muted">{{ modeLabels[result.mode] }} · Ниво {{ result.difficulty }}</p>
        <h1>+{{ result.crystals }}</h1>
        <h2 class="visual-title">{{ crystalTitle }}</h2>
        <div class="score-line">
          <span :style="{ width: `${percentage}%` }"></span>
        </div>
        <div class="result-stats">
          <div>
            <span>Оценка</span>
            <strong>{{ result.grade }}</strong>
          </div>
          <div class="award-stat">
            <MascotFigure :mood="awardMood" small />
            <span>Верни</span>
            <strong>{{ result.score }}/{{ result.totalQuestions }}</strong>
          </div>
          <div>
            <span>Време</span>
            <strong>{{ formatDuration(result.durationSeconds) }}</strong>
          </div>
        </div>
        <div v-if="result.scoreBreakdown.length > 1" class="breakdown-grid" aria-label="Точки по категории">
          <div v-for="item in result.scoreBreakdown" :key="item.mode">
            <span>{{ modeLabels[item.mode] }}</span>
            <strong>{{ item.correct }}/{{ item.total }}</strong>
          </div>
        </div>
        <div class="result-actions">
          <button class="button" type="button" @click="router.push({ name: 'dashboard' })">
            <RotateCcw :size="20" />
            <span>Нов тест</span>
          </button>
          <button class="button secondary" type="button" @click="router.push({ name: 'dashboard' })">
            <Home :size="20" />
            <span>Начало</span>
          </button>
        </div>
      </section>

      <aside class="result-side">
        <section class="panel mistakes-panel">
          <div class="panel-header">
            <h2 class="panel-title">Резюме</h2>
          </div>
          <div class="mistake-list">
            <div v-for="answer in resultRows" :key="answer.questionId" class="mistake-row" :class="{ correct: answer.correct, wrong: !answer.correct }">
              <template v-if="isWordQuestion(questionById.get(answer.questionId))">
                <PictureGlyph class="summary-word-image" :image="questionById.get(answer.questionId)?.image" aria-hidden="true" />
                <span class="summary-prompt">
                  <span>{{ questionById.get(answer.questionId)?.prompt }}</span>
                  <b class="summary-token" :class="{ correct: answer.correct, wrong: !answer.correct }">
                    {{ wordSummaryToken(questionById.get(answer.questionId), answer) }}
                  </b>
                </span>
              </template>
              <div v-else-if="isGroupingQuestion(questionById.get(answer.questionId))" class="summary-grouping">
                <span class="summary-prompt">{{ questionById.get(answer.questionId)?.prompt }}</span>
                <ul class="summary-grouping-list" :class="{ correct: answer.correct, wrong: !answer.correct }">
                  <li v-for="item in groupingSummaryItems(questionById.get(answer.questionId), answer)" :key="item.key">
                    <PictureGlyph class="summary-grouping-image" :image="item.image || item.word" :aria-label="item.word" />
                    <span class="summary-grouping-word">{{ item.word }}</span>
                    <small aria-hidden="true">→</small>
                    <b>{{ item.letter }}</b>
                  </li>
                </ul>
              </div>
              <span v-else-if="isSpotDifferencesQuestion(questionById.get(answer.questionId))" class="summary-prompt">
                <span>{{ questionById.get(answer.questionId)?.prompt }}</span>
                <b class="summary-token" :class="{ correct: answer.correct, wrong: !answer.correct }">
                  {{ spotCorrectCount(answer) }}/{{ countListAnswer(answer.correctAnswer) }}
                </b>
              </span>
              <span v-else-if="isFindObjectQuestion(questionById.get(answer.questionId))" class="summary-prompt">
                <span>{{ questionById.get(answer.questionId)?.prompt }}</span>
                <b class="summary-token" :class="{ correct: answer.correct, wrong: !answer.correct }">
                  {{ findObjectLabel(questionById.get(answer.questionId), answer.correct ? answer.answer : answer.correctAnswer) }}
                </b>
              </span>
              <span v-else-if="isMemoryPairsQuestion(questionById.get(answer.questionId))" class="summary-prompt">
                <span>{{ questionById.get(answer.questionId)?.prompt }}</span>
                <b class="summary-token" :class="{ correct: answer.correct, wrong: !answer.correct }">
                  {{ memoryField(answer.answer, 'attempts') || '0' }} опита
                </b>
              </span>
              <div v-else-if="isPatternSequenceQuestion(questionById.get(answer.questionId))" class="summary-pattern">
                <span class="summary-prompt">{{ questionById.get(answer.questionId)?.prompt }}</span>
                <div
                  v-if="hasPatternSequence(questionById.get(answer.questionId))"
                  class="summary-pattern-row"
                  :class="{ correct: answer.correct, wrong: !answer.correct }"
                  :aria-label="patternSequenceAria(questionById.get(answer.questionId))"
                >
                  <span
                    v-for="token in patternSequenceTokens(questionById.get(answer.questionId))"
                    :key="token.key"
                    class="summary-pattern-token"
                    :class="token.shape"
                    :style="patternTokenStyle(token)"
                    :aria-label="token.label"
                  ></span>
                </div>
                <b v-else class="summary-token" :class="{ correct: answer.correct, wrong: !answer.correct }">
                  {{ patternSequenceFallback(questionById.get(answer.questionId)) }}
                </b>
              </div>
              <div v-else-if="isSudokuQuestion(questionById.get(answer.questionId))" class="summary-sudoku">
                <span class="summary-prompt">{{ questionById.get(answer.questionId)?.prompt }}</span>
                <div class="summary-sudoku-grid" :class="{ correct: answer.correct, wrong: !answer.correct }">
                  <span v-for="(row, rowIndex) in sudokuRows(answer)" :key="`${answer.questionId}-${rowIndex}`">{{ row }}</span>
                </div>
              </div>
              <span v-else class="summary-prompt">
                <template v-for="(part, partIndex) in promptParts(questionById.get(answer.questionId)?.prompt)" :key="partIndex">
                  <span>{{ part }}</span>
                  <b
                    v-if="partIndex === 0"
                    class="summary-token"
                    :class="{ correct: answer.correct, wrong: !answer.correct }"
                  >
                    {{ tokenFor(answer) }}
                  </b>
                </template>
              </span>
              <small>
                <CircleCheck v-if="answer.correct" :size="16" />
                <Target v-else :size="16" />
                {{ answerSummaryLabel(questionById.get(answer.questionId), answer) }}
              </small>
            </div>
          </div>
        </section>

        <section class="panel leaderboard-panel">
          <div class="panel-header">
            <h2 class="panel-title">Моите последни 10</h2>
            <Gem :size="23" />
          </div>
          <div class="leaderboard-list">
            <div v-if="quiz.recentAttempts.length === 0" class="empty-state">Още нямаш други решени тестове за този избор.</div>
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
      </aside>
    </div>
  </section>
</template>

<style scoped>
.result-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 20px;
  align-items: start;
}

.result-main {
  display: grid;
  justify-items: center;
  gap: 18px;
  padding: 34px 24px;
  text-align: center;
}

.result-main p,
.result-main h1 {
  margin: 0;
}

.result-main h1 {
  font-size: clamp(4rem, 12vw, 8rem);
  line-height: 1;
}

.visual-title {
  margin: -8px 0 0;
  font-size: clamp(1.35rem, 4vw, 2rem);
}

.crystal-visual {
  position: relative;
  display: grid;
  width: 118px;
  height: 118px;
  place-items: center;
  border-radius: 32px;
  color: #ffffff;
  background: linear-gradient(135deg, #39d7f2, #3f7dd9 52%, #8b5cf6);
  box-shadow: 0 18px 34px rgba(63, 125, 217, 0.28);
}

.crystal-sparkle {
  position: absolute;
  top: 10px;
  right: 10px;
  color: #fff7b0;
}

.score-line {
  width: min(420px, 100%);
  height: 14px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(36, 48, 74, 0.1);
}

.score-line span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--coral), var(--green));
}

.result-stats {
  display: grid;
  width: min(760px, 100%);
  grid-template-columns: minmax(160px, 0.9fr) minmax(300px, 1.35fr) minmax(160px, 0.9fr);
  gap: 12px;
}

.result-stats > div {
  display: grid;
  gap: 4px;
  min-height: 142px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 16px;
  background: #ffffff;
  align-content: center;
}

.result-stats .award-stat {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  justify-items: start;
  text-align: left;
  column-gap: 12px;
  overflow: hidden;
}

.award-stat .mascot {
  grid-row: span 2;
  flex: 0 0 auto;
}

.award-stat strong {
  max-width: 100%;
  overflow-wrap: anywhere;
  font-size: clamp(1.25rem, 2.5vw, 1.75rem);
  line-height: 1.05;
}

.result-stats span {
  color: var(--muted);
  font-weight: 800;
}

.result-stats strong {
  font-size: 1.35rem;
}

.breakdown-grid {
  display: grid;
  width: min(620px, 100%);
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.breakdown-grid div {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 11px;
  background: #ffffff;
}

.breakdown-grid span {
  color: var(--muted);
  font-size: 0.88rem;
  font-weight: 800;
}

.result-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.result-side {
  display: grid;
  gap: 20px;
}

.mistake-list,
.leaderboard-list {
  display: grid;
  gap: 10px;
  padding: 16px 20px 20px;
}

.mistake-row,
.leaderboard-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 10px;
  background: #ffffff;
}

.leaderboard-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 850;
}

.mistake-row.correct {
  border-color: rgba(30, 157, 116, 0.3);
  background: rgba(30, 157, 116, 0.06);
}

.mistake-row.wrong {
  border-color: rgba(217, 75, 75, 0.25);
  background: rgba(217, 75, 75, 0.05);
}

.summary-prompt {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.18em;
  font-size: 1.05rem;
  font-weight: 900;
}

.summary-word-image {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border-radius: var(--radius);
  background: #ffffff;
  font-size: 2.2rem;
}

.summary-token {
  display: inline-grid;
  min-width: 1.4em;
  place-items: center;
  border-radius: 6px;
  padding: 2px 7px;
  color: #ffffff;
}

.summary-token.correct {
  background: var(--green);
}

.summary-token.wrong {
  background: var(--danger);
}

.summary-pattern {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.summary-pattern-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  border-radius: 8px;
  padding: 7px;
}

.summary-pattern-row.correct {
  background: rgba(30, 157, 116, 0.08);
}

.summary-pattern-row.wrong {
  background: rgba(217, 75, 75, 0.08);
}

.summary-pattern-token {
  --pattern-color: var(--green);
  display: block;
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  background: var(--pattern-color);
  box-shadow: inset 0 -3px 0 rgba(0, 0, 0, 0.12);
}

.summary-pattern-token.circle {
  border-radius: 50%;
}

.summary-pattern-token.square {
  border-radius: 5px;
}

.summary-pattern-token.triangle {
  width: 0;
  height: 0;
  border-right: 13px solid transparent;
  border-bottom: 25px solid var(--pattern-color);
  border-left: 13px solid transparent;
  background: transparent;
  box-shadow: none;
}

.summary-pattern-token.diamond {
  border-radius: 5px;
  transform: rotate(45deg) scale(0.86);
}

.summary-sudoku {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.summary-sudoku-grid {
  display: grid;
  width: fit-content;
  gap: 2px;
  border-radius: 8px;
  padding: 7px;
  background: rgba(36, 48, 74, 0.05);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 0.92rem;
  font-weight: 950;
  letter-spacing: 0;
}

.summary-sudoku-grid.correct {
  color: var(--green-dark);
  background: rgba(30, 157, 116, 0.08);
}

.summary-sudoku-grid.wrong {
  color: var(--danger);
  background: rgba(217, 75, 75, 0.08);
}

.summary-grouping {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.summary-grouping-list {
  display: grid;
  gap: 5px;
  margin: 0 0 0 12px;
  padding: 0;
  list-style: none;
}

.summary-grouping-list li {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 6px;
  min-width: 0;
  border-radius: 7px;
  padding: 5px 7px;
  color: var(--ink);
  background: rgba(255, 255, 255, 0.68);
  font-size: clamp(0.78rem, 2.2vw, 0.92rem);
  font-weight: 900;
  line-height: 1.05;
}

.summary-grouping-image {
  display: grid;
  min-width: 0;
  min-height: 38px;
  place-items: center;
  border-radius: 7px;
  background: #ffffff;
  font-size: clamp(1.65rem, 5.5vw, 2.2rem);
  line-height: 1;
}

.summary-grouping-word {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--ink);
  font-size: clamp(0.78rem, 2.1vw, 0.92rem);
  font-weight: 950;
  line-height: 1.05;
  text-align: left;
}

.summary-grouping-list li > span:not(.summary-grouping-image) {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
  text-align: left;
}

.summary-grouping-list li > small {
  color: var(--muted);
  font-weight: 900;
}

.summary-grouping-list li > b {
  display: grid;
  min-width: 28px;
  min-height: 26px;
  place-items: center;
  border-radius: 6px;
  color: #ffffff;
  font-size: 0.86rem;
  line-height: 1;
}

.summary-grouping-list.correct li > b {
  background: var(--green);
}

.summary-grouping-list.wrong li > b {
  background: var(--danger);
}

.mistake-row small {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--muted);
  font-weight: 800;
}

.leaderboard-row {
  grid-template-columns: minmax(42px, auto) minmax(0, 1fr) auto;
  color: var(--ink);
  text-decoration: none;
}

.leaderboard-row strong {
  display: grid;
  min-width: 30px;
  height: 28px;
  padding: 0 8px;
  place-items: center;
  border-radius: 999px;
  color: #ffffff;
  background: var(--amber);
}

.leaderboard-row small {
  grid-column: 2 / 4;
  color: var(--muted);
  font-weight: 750;
}

.empty-state {
  border-radius: var(--radius);
  padding: 12px;
  color: var(--muted);
  background: rgba(63, 125, 217, 0.09);
  font-weight: 800;
}

@media (max-width: 900px) {
  .result-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .result-stats {
    grid-template-columns: 1fr;
  }
}
</style>
