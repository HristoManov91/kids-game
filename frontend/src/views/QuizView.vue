<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AlertTriangle, ArrowLeft, ArrowRight, Check, Clock, Flag } from 'lucide-vue-next'
import MascotFigure from '@/components/MascotFigure.vue'
import PictureGlyph from '@/components/PictureGlyph.vue'
import { useQuizStore } from '@/stores/quiz'
import { api } from '@/services/api'
import { formatDuration, levelRange, modeLabels } from '@/services/labels'
import type { QuestionResponse } from '@/types'

const route = useRoute()
const router = useRouter()
const quiz = useQuizStore()

const index = ref(0)
const typedAnswer = ref('')
const error = ref('')
const issueError = ref('')
const issueSuccess = ref('')
const issueMessage = ref('')
const issueFormOpen = ref(false)
const checking = ref(false)
const issueSubmitting = ref(false)
const finishing = ref(false)
const draftAnswers = ref<Record<number, string>>({})
const builderAnswers = ref<Record<number, BuilderSlot[]>>({})
const activeBuilderSlots = ref<Record<number, number>>({})
const groupingAnswers = ref<Record<number, Record<string, string>>>({})
const spotDifferenceAnswers = ref<Record<number, string[]>>({})
const memoryFlippedCards = ref<Record<number, string[]>>({})
const memoryMatchedPairs = ref<Record<number, string[]>>({})
const memoryAttempts = ref<Record<number, number>>({})
const memoryPreviewing = ref<Record<number, boolean>>({})
const memoryPreviewRemaining = ref<Record<number, number>>({})
const memoryPreviewSeen = ref<Record<number, boolean>>({})
const memoryLocked = ref<Record<number, boolean>>({})
const patternAnswers = ref<Record<number, (PatternToken | null)[]>>({})
const activePatternSlots = ref<Record<number, number>>({})
const patternPreviewing = ref<Record<number, boolean>>({})
const patternPreviewRemaining = ref<Record<number, number>>({})
const patternPreviewSeen = ref<Record<number, boolean>>({})
const sudokuAnswers = ref<Record<number, string[]>>({})
const activeSudokuCells = ref<Record<number, number>>({})
const selectedGroupingWord = ref('')
const now = ref(Date.now())
const activeBaseSeconds = ref(0)
const activeStartedAt = ref<number | null>(null)
let timer: number | undefined
let heartbeatTimer: number | undefined
let memoryPreviewTimer: number | undefined
let memoryMismatchTimer: number | undefined
let patternPreviewTimer: number | undefined
let memoryPreviewQuestionId: number | null = null
let memoryMismatchQuestionId: number | null = null
let patternPreviewQuestionId: number | null = null
let draggedTokenId = ''
let draggedGroupingWord = ''

interface BuilderToken {
  id: string
  text: string
}

type BuilderSlot = BuilderToken | null

interface GroupingItem {
  key: string
  image: string
  word: string
}

interface SpotDifferenceObject {
  key: string
  selectionKey: string
  side: 'L' | 'R'
  id: string
  x: number
  y: number
  emoji: string
  size: number
  differenceId: string
}

interface SpotDifferenceScene {
  name: string
  theme: string
  left: SpotDifferenceObject[]
  right: SpotDifferenceObject[]
  differenceIds: string[]
}

interface FindObjectItem {
  key: string
  id: string
  x: number
  y: number
  emoji: string
  size: number
  label: string
  targetText: string
}

interface FindObjectScene {
  name: string
  theme: string
  objects: FindObjectItem[]
}

interface MemoryCard {
  key: string
  cardId: string
  pairId: string
  emoji: string
  label: string
}

interface MemoryGame {
  previewSeconds: number
  pairCount: number
  perfectMistakes: number
  cards: MemoryCard[]
}

interface PatternToken {
  key: string
  id: string
  shape: string
  color: string
  label: string
}

interface PatternGame {
  previewSeconds: number
  length: number
  sequence: PatternToken[]
  choices: PatternToken[]
}

interface SudokuCell {
  key: string
  row: number
  col: number
  index: number
  value: string
  given: boolean
}

interface SudokuGame {
  size: number
  boxSize: number
  givenCount: number
  cells: SudokuCell[]
  choices: string[]
}

const BULGARIAN_REPLACEMENT_DISTRACTORS = [
  'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'К', 'Л', 'М',
  'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Я'
]

const attemptId = computed(() => String(route.params.attemptId))
const attempt = computed(() => quiz.currentAttempt)
const questions = computed(() => attempt.value?.questions ?? [])
const hasMultipleQuestions = computed(() => (questions.value.length || attempt.value?.totalQuestions || 0) > 1)
const currentQuestion = computed(() => questions.value[index.value] ?? null)
const currentAnswer = computed(() => {
  const question = currentQuestion.value
  return question ? quiz.answerMap.get(question.id) : undefined
})
const questionCompletion = computed(() => questions.value.map((question) => isQuestionAnswered(question)))
const answeredCount = computed(() => questionCompletion.value.filter(Boolean).length)
const unansweredQuestionIndexes = computed(() => questionCompletion.value
  .map((answered, questionIndex) => answered ? -1 : questionIndex)
  .filter((questionIndex) => questionIndex >= 0))
const progressPercent = computed(() => {
  const total = questions.value.length || attempt.value?.totalQuestions || 20
  return Math.round((answeredCount.value / total) * 100)
})
const progressLabel = computed(() => {
  return `${answeredCount.value}/${questions.value.length || attempt.value?.totalQuestions || 20}`
})
const promptParts = computed(() => currentQuestion.value?.prompt.split('?') ?? ['', ''])
const isWordQuestion = computed(() => isWordKind(currentQuestion.value))
const isBuilderQuestion = computed(() => isBuilderKind(currentQuestion.value))
const isGroupingQuestion = computed(() => isGroupingKind(currentQuestion.value))
const isFindObjectQuestion = computed(() => isFindObjectKind(currentQuestion.value))
const isSpotDifferencesQuestion = computed(() => isSpotDifferencesKind(currentQuestion.value))
const isMemoryPairsQuestion = computed(() => isMemoryPairsKind(currentQuestion.value))
const isPatternSequenceQuestion = computed(() => isPatternSequenceKind(currentQuestion.value))
const isSudokuQuestion = computed(() => isSudokuKind(currentQuestion.value))
const isPictureChoiceQuestion = computed(() => currentQuestion.value?.sourceMode === 'WORD_PICTURE')
const isWrongLetterQuestionCurrent = computed(() => isWrongLetterQuestion(currentQuestion.value))
const groupingItems = computed(() => (currentQuestion.value ? groupingQuestionItems(currentQuestion.value) : []))
const groupingLetters = computed(() => currentQuestion.value?.answerSlots ?? [])
const groupingAssignment = computed(() => {
  const question = currentQuestion.value
  return question ? groupingAnswers.value[question.id] ?? {} : {}
})
const availableGroupingItems = computed(() => groupingItems.value.filter((item) => !groupingAssignment.value[item.word]))
const spotScene = computed(() => currentQuestion.value ? parseSpotDifferenceScene(currentQuestion.value) : emptySpotDifferenceScene())
const findObjectScene = computed(() => currentQuestion.value ? parseFindObjectScene(currentQuestion.value) : emptyFindObjectScene())
const findObjectTarget = computed(() => findObjectTargetItem(currentQuestion.value))
const memoryGame = computed(() => currentQuestion.value ? parseMemoryGame(currentQuestion.value) : emptyMemoryGame())
const patternGame = computed(() => currentQuestion.value ? parsePatternGame(currentQuestion.value) : emptyPatternGame())
const sudokuGame = computed(() => currentQuestion.value ? parseSudokuGame(currentQuestion.value) : emptySudokuGame())
const patternSlots = computed(() => {
  const question = currentQuestion.value
  return question ? patternAnswers.value[question.id] ?? emptyPatternSlots(question) : []
})
const sudokuCells = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return sudokuGame.value.cells
  }
  const values = sudokuAnswers.value[question.id] ?? emptySudokuValues(question)
  return sudokuGame.value.cells.map((cell) => ({
    ...cell,
    value: cell.given ? cell.value : values[cell.index] ?? ''
  }))
})
const memoryFlipped = computed(() => {
  const question = currentQuestion.value
  return question ? memoryFlippedCards.value[question.id] ?? [] : []
})
const memoryMatched = computed(() => {
  const question = currentQuestion.value
  return question ? memoryMatchedPairs.value[question.id] ?? [] : []
})
const memoryAttemptsCount = computed(() => {
  const question = currentQuestion.value
  return question ? memoryAttempts.value[question.id] ?? 0 : 0
})
const memoryPreviewActive = computed(() => {
  const question = currentQuestion.value
  return question ? memoryPreviewing.value[question.id] ?? false : false
})
const memoryPreviewSecondsLeft = computed(() => {
  const question = currentQuestion.value
  return question ? memoryPreviewRemaining.value[question.id] ?? memoryGame.value.previewSeconds : memoryGame.value.previewSeconds
})
const memoryGridColumns = computed(() => {
  const cards = memoryGame.value.cards.length
  if (cards <= 8) {
    return 4
  }
  if (cards <= 12) {
    return 4
  }
  return 6
})
const memoryIntroVisible = computed(() => {
  const question = currentQuestion.value
  return Boolean(
    question
    && isMemoryPairsKind(question)
    && !currentAnswer.value
    && !memoryPreviewSeen.value[question.id]
    && !isMemorySolved(question)
  )
})
const patternPreviewActive = computed(() => {
  const question = currentQuestion.value
  return question ? patternPreviewing.value[question.id] ?? false : false
})
const patternPreviewSecondsLeft = computed(() => {
  const question = currentQuestion.value
  return question ? patternPreviewRemaining.value[question.id] ?? patternGame.value.previewSeconds : patternGame.value.previewSeconds
})
const patternIntroVisible = computed(() => {
  const question = currentQuestion.value
  return Boolean(
    question
    && isPatternSequenceKind(question)
    && !currentAnswer.value
    && !patternPreviewSeen.value[question.id]
  )
})
const memoryMistakesUsed = computed(() => Math.max(0, memoryAttemptsCount.value - memoryGame.value.pairCount))
const primaryActionLabel = computed(() => (isMemoryPairsQuestion.value ? 'Запиши резултат' : 'Провери сега'))
const selectedSpotDifferences = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return []
  }
  return spotDifferenceAnswers.value[question.id] ?? parseSpotDifferenceAnswer(currentAnswer.value?.answer ?? typedAnswer.value)
})
const selectedCorrectSpotDifferences = computed(() => {
  const correct = new Set(spotScene.value.differenceIds)
  return selectedSpotDifferences.value.filter((selection) => correct.has(selection)).length
})
const selectedWrongSpotDifferences = computed(() => {
  const correct = new Set(spotScene.value.differenceIds)
  return selectedSpotDifferences.value.filter((selection) => !correct.has(selection)).length
})
const builderSlots = computed(() => {
  const question = currentQuestion.value
  return question ? builderAnswers.value[question.id] ?? emptyBuilderSlots(question) : []
})
const builderLayout = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return []
  }
  const slots = builderSlots.value
  let slotIndex = 0
  return answerLayoutTokens(question).map((answerSlot, layoutIndex) => {
    if (isGapSlot(answerSlot)) {
      return {
        key: `${question.id}-gap-${layoutIndex}`,
        type: 'gap' as const
      }
    }
    const currentSlotIndex = slotIndex++
    return {
      key: `${question.id}-slot-${currentSlotIndex}`,
      type: 'slot' as const,
      slotIndex: currentSlotIndex,
      expected: answerSlot,
      token: slots[currentSlotIndex] ?? null,
      active: activeBuilderIndex(question) === currentSlotIndex
    }
  })
})
const availableTokens = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return []
  }
  const usedIds = new Set(builderSlots.value.filter((token): token is BuilderToken => Boolean(token)).map((token) => token.id))
  return question.choices
    .map((text, tokenIndex) => ({ id: `${question.id}-${tokenIndex}-${text}`, text }))
    .filter((token) => !usedIds.has(token.id))
})
const answerInputMode = computed(() => (currentQuestion.value?.kind === 'NUMERIC' ? 'numeric' : 'text'))
const answerLabel = computed(() => (currentQuestion.value?.kind === 'WORD_TYPING' ? 'Дума' : 'Отговор'))
const difficultyCaption = computed(() => {
  if (attempt.value?.category === 'LOGIC' && isMemoryPairsQuestion.value) {
    return `${memoryGame.value.pairCount} двойки`
  }
  if (attempt.value?.category === 'LOGIC' && isPatternSequenceQuestion.value) {
    return `${patternGame.value.length} фигури`
  }
  if (attempt.value?.category === 'LOGIC' && isSudokuQuestion.value) {
    return `${sudokuGame.value.size}x${sudokuGame.value.size} · ${sudokuGame.value.givenCount} подсказки`
  }
  return attempt.value ? levelRange(attempt.value.difficulty, attempt.value.category) : ''
})
const elapsedSeconds = computed(() => {
  const localSeconds = activeStartedAt.value ? Math.floor((now.value - activeStartedAt.value) / 1000) : 0
  return Math.max(0, activeBaseSeconds.value + localSeconds)
})
const mascotMood = computed(() => {
  if (!currentAnswer.value) {
    return 'thinking'
  }
  return currentAnswer.value.correct ? 'happy' : 'sad'
})

watch(
  currentQuestion,
  () => {
    clearMemoryTimers()
    clearPatternTimers()
    const question = currentQuestion.value
    if (!question) {
      typedAnswer.value = ''
    } else if (isBuilderKind(question)) {
      ensureBuilderSlots(question)
      const builtAnswer = builderAnswerText(question)
      typedAnswer.value = currentAnswer.value?.answer ?? (builtAnswer || draftAnswers.value[question.id] || '')
    } else if (isGroupingKind(question)) {
      ensureGroupingAnswer(question)
      typedAnswer.value = currentAnswer.value?.answer ?? groupingAnswerText(question)
    } else if (isSpotDifferencesKind(question)) {
      const answer = currentAnswer.value?.answer ?? draftAnswers.value[question.id] ?? ''
      typedAnswer.value = answer
      spotDifferenceAnswers.value = {
        ...spotDifferenceAnswers.value,
        [question.id]: parseSpotDifferenceAnswer(answer)
      }
    } else if (isMemoryPairsKind(question)) {
      ensureMemoryState(question)
      typedAnswer.value = currentAnswer.value?.answer ?? draftAnswers.value[question.id] ?? ''
    } else if (isPatternSequenceKind(question)) {
      ensurePatternState(question)
      seedPatternFromAnswer(question, currentAnswer.value?.answer ?? draftAnswers.value[question.id] ?? '')
      typedAnswer.value = currentAnswer.value?.answer ?? patternAnswerText(question)
    } else if (isSudokuKind(question)) {
      ensureSudokuState(question)
      seedSudokuFromAnswer(question, currentAnswer.value?.answer ?? draftAnswers.value[question.id] ?? '')
      typedAnswer.value = currentAnswer.value?.answer ?? sudokuAnswerText(question)
    } else {
      typedAnswer.value = currentAnswer.value?.answer ?? draftAnswers.value[question.id] ?? ''
    }
    selectedGroupingWord.value = ''
    error.value = ''
    issueError.value = ''
    issueSuccess.value = ''
    issueMessage.value = ''
    issueFormOpen.value = false
  },
  { immediate: true }
)

watch(typedAnswer, (value) => {
  if (!currentQuestion.value || currentAnswer.value) {
    return
  }
  draftAnswers.value = {
    ...draftAnswers.value,
    [currentQuestion.value.id]: value
  }
})

onMounted(async () => {
  if (!attempt.value || attempt.value.attemptId !== attemptId.value) {
    await quiz.fetchAttempt(attemptId.value)
  }
  attempt.value?.answers.forEach((answer) => {
    draftAnswers.value[answer.questionId] = answer.answer
  })
  if (attempt.value?.status === 'COMPLETED') {
    router.replace({ name: 'result', params: { attemptId: attemptId.value } })
    return
  }
  activeBaseSeconds.value = attempt.value?.durationSeconds ?? 0
  seedBuildersFromDrafts()
  const firstOpen = questions.value.findIndex((question) => !quiz.answerMap.has(question.id))
  index.value = firstOpen >= 0 ? firstOpen : 0
  await syncActiveTimeIfNeeded(true)
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
  heartbeatTimer = window.setInterval(() => {
    if (shouldTrackActiveTime()) {
      void syncActiveTime(true)
    } else if (activeStartedAt.value) {
      void syncActiveTime(false)
    }
  }, 5000)
  window.addEventListener('keydown', handleGlobalKeydown)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
  if (heartbeatTimer) {
    window.clearInterval(heartbeatTimer)
  }
  clearMemoryTimers()
  clearPatternTimers()
  window.removeEventListener('keydown', handleGlobalKeydown)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (activeStartedAt.value || shouldTrackActiveTime()) {
    void syncActiveTime(false)
  }
})

async function syncActiveTimeIfNeeded(keepRunning: boolean) {
  if (!shouldTrackActiveTime()) {
    activeStartedAt.value = null
    return
  }
  await syncActiveTime(keepRunning)
}

async function syncActiveTime(keepRunning: boolean) {
  if (!attempt.value || attempt.value.status !== 'ACTIVE') {
    return
  }
  try {
    const response = await quiz.heartbeat(attemptId.value)
    activeBaseSeconds.value = response.durationSeconds
    activeStartedAt.value = keepRunning && document.visibilityState === 'visible' ? Date.now() : null
    now.value = Date.now()
  } catch {
    activeStartedAt.value = keepRunning && document.visibilityState === 'visible' ? (activeStartedAt.value ?? Date.now()) : null
  }
}

function handleVisibilityChange() {
  if (shouldTrackActiveTime()) {
    void syncActiveTime(document.visibilityState === 'visible')
  } else if (activeStartedAt.value) {
    void syncActiveTime(false)
  }
}

function shouldTrackActiveTime() {
  if (!attempt.value || attempt.value.status !== 'ACTIVE' || document.visibilityState !== 'visible') {
    return false
  }
  const question = currentQuestion.value
  if (isPatternSequenceKind(question)) {
    return Boolean(
      question
      && patternPreviewSeen.value[question.id]
      && !patternPreviewing.value[question.id]
      && !currentAnswer.value
    )
  }
  if (!isMemoryPairsKind(question)) {
    return true
  }
  return Boolean(
    question
    && memoryPreviewSeen.value[question.id]
    && !memoryPreviewing.value[question.id]
    && !isMemorySolved(question)
    && !currentAnswer.value
  )
}

async function submitAnswer() {
  if (!currentQuestion.value || currentAnswer.value) {
    return
  }
  if (isGroupingKind(currentQuestion.value) && !isGroupingComplete(currentQuestion.value)) {
    error.value = 'Постави всички картинки в кошниците.'
    return
  }
  if (isMemoryPairsKind(currentQuestion.value) && !isMemorySolved(currentQuestion.value)) {
    error.value = 'Отвори всички двойки, после запиши резултата.'
    return
  }
  if (isPatternSequenceKind(currentQuestion.value) && patternIntroVisible.value) {
    error.value = 'Първо стартирай прегледа на модела.'
    return
  }
  if (isPatternSequenceKind(currentQuestion.value) && patternPreviewActive.value) {
    error.value = 'Изчакай моделът да се скрие, после го подреди.'
    return
  }
  if (!isDraftAnswerComplete(currentQuestion.value, typedAnswer.value)) {
    if (isWrongLetterQuestion(currentQuestion.value)) {
      error.value = 'Избери грешната буква и с коя буква да я смениш.'
    } else {
      error.value = 'Въведи отговор.'
    }
    return
  }

  checking.value = true
  error.value = ''
  try {
    await quiz.answer(currentQuestion.value.id, typedAnswer.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Отговорът не беше проверен.'
  } finally {
    checking.value = false
  }
}

async function saveCurrentAnswerIfReady() {
  const question = currentQuestion.value
  if (!question || currentAnswer.value) {
    return true
  }

  const answer = typedAnswer.value
  draftAnswers.value = { ...draftAnswers.value, [question.id]: answer }
  if (!isDraftAnswerComplete(question, answer)) {
    return true
  }

  checking.value = true
  error.value = ''
  try {
    await quiz.answer(question.id, answer)
    return true
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Отговорът не беше записан.'
    return false
  } finally {
    checking.value = false
  }
}

function choose(value: string) {
  if (!currentAnswer.value) {
    typedAnswer.value = value
  }
}

function isDraftAnswerComplete(question: QuestionResponse, answer: string | undefined) {
  if (isMemoryPairsKind(question)) {
    return isMemorySolved(question) || Boolean(answer?.trim().toUpperCase().startsWith('SOLVED'))
  }
  if (isPatternSequenceKind(question)) {
    return isPatternComplete(question)
  }
  if (isSudokuKind(question)) {
    return isSudokuComplete(question)
  }
  if (isSpotDifferencesKind(question)) {
    return parseSpotDifferenceAnswer(answer).length >= parseSpotDifferenceScene(question).differenceIds.length
  }
  if (!answer?.trim()) {
    return false
  }
  if (isWrongLetterQuestion(question)) {
    const [wrongLetter, replacementLetter] = splitWrongLetterAnswer(answer)
    return Boolean(wrongLetter && replacementLetter)
  }
  return true
}

function isQuestionAnswered(question: QuestionResponse) {
  if (quiz.answerMap.has(question.id)) {
    return true
  }
  if (isSpotDifferencesKind(question)) {
    const selected = spotDifferenceAnswers.value[question.id] ?? parseSpotDifferenceAnswer(draftAnswers.value[question.id])
    return selected.length >= parseSpotDifferenceScene(question).differenceIds.length
  }
  if (isMemoryPairsKind(question)) {
    return isMemorySolved(question)
  }
  if (isSudokuKind(question)) {
    return isSudokuComplete(question)
  }
  return isDraftAnswerComplete(question, draftAnswers.value[question.id])
}

function isWordKind(question: QuestionResponse | null | undefined) {
  return question?.kind === 'LETTER_ORDER'
    || question?.kind === 'SYLLABLE_ORDER'
    || question?.kind === 'WORD_TYPING'
    || isWrongLetterQuestion(question)
}

function isBuilderKind(question: QuestionResponse | null | undefined) {
  return question?.kind === 'LETTER_ORDER' || question?.kind === 'SYLLABLE_ORDER'
}

function isGroupingKind(question: QuestionResponse | null | undefined) {
  return question?.kind === 'GROUPING'
}

function isFindObjectKind(question: QuestionResponse | null | undefined) {
  return question?.sourceMode === 'FIND_OBJECT'
}

function isSpotDifferencesKind(question: QuestionResponse | null | undefined) {
  return question?.kind === 'SPOT_DIFFERENCES'
}

function isMemoryPairsKind(question: QuestionResponse | null | undefined) {
  return question?.kind === 'MEMORY_PAIRS'
}

function isPatternSequenceKind(question: QuestionResponse | null | undefined) {
  return question?.kind === 'PATTERN_SEQUENCE'
}

function isSudokuKind(question: QuestionResponse | null | undefined) {
  return question?.kind === 'SUDOKU'
}

function emptySudokuGame(): SudokuGame {
  return {
    size: 4,
    boxSize: 2,
    givenCount: 0,
    cells: [],
    choices: []
  }
}

function parseSudokuGame(question: QuestionResponse): SudokuGame {
  const game = emptySudokuGame()
  const givens = new Map<number, string>()
  question.answerSlots.forEach((slot) => {
    const parts = slot.split('|')
    if (parts[0] === 'G') {
      game.size = Number(parts[1]) || 4
      game.boxSize = Number(parts[2]) || 2
      game.givenCount = Number(parts[3]) || 0
      return
    }
    if (parts[0] !== 'C') {
      return
    }
    const row = Number(parts[1])
    const col = Number(parts[2])
    const value = parts[3] ?? ''
    if (Number.isInteger(row) && Number.isInteger(col) && value) {
      givens.set(row * game.size + col, value)
    }
  })
  game.choices = question.choices.length
    ? question.choices
    : Array.from({ length: game.size }, (_, choiceIndex) => String(choiceIndex + 1))
  game.cells = Array.from({ length: game.size * game.size }, (_, cellIndex) => {
    const row = Math.floor(cellIndex / game.size)
    const col = cellIndex % game.size
    const value = givens.get(cellIndex) ?? ''
    return {
      key: `${question.id}-sudoku-${row}-${col}`,
      row,
      col,
      index: cellIndex,
      value,
      given: Boolean(value)
    }
  })
  return game
}

function emptySudokuValues(question: QuestionResponse) {
  return parseSudokuGame(question).cells.map((cell) => cell.value)
}

function ensureSudokuState(question: QuestionResponse) {
  if (!sudokuAnswers.value[question.id]) {
    sudokuAnswers.value = {
      ...sudokuAnswers.value,
      [question.id]: emptySudokuValues(question)
    }
  }
  const currentActive = activeSudokuCells.value[question.id]
  const game = parseSudokuGame(question)
  if (!Number.isInteger(currentActive) || currentActive < 0 || currentActive >= game.size * game.size || game.cells[currentActive]?.given) {
    const firstEditable = game.cells.find((cell) => !cell.given)?.index ?? 0
    activeSudokuCells.value = {
      ...activeSudokuCells.value,
      [question.id]: firstEditable
    }
  }
}

function seedSudokuFromAnswer(question: QuestionResponse, answer: string) {
  ensureSudokuState(question)
  const normalized = (answer ?? '').replace(/[^0-9]/g, '')
  if (!normalized) {
    return
  }
  const game = parseSudokuGame(question)
  const values = emptySudokuValues(question)
  Array.from(normalized).slice(0, values.length).forEach((value, cellIndex) => {
    if (!game.cells[cellIndex]?.given) {
      values[cellIndex] = value
    }
  })
  sudokuAnswers.value = {
    ...sudokuAnswers.value,
    [question.id]: values
  }
}

function sudokuAnswerText(question: QuestionResponse) {
  return (sudokuAnswers.value[question.id] ?? emptySudokuValues(question)).join('')
}

function updateSudokuAnswer(question: QuestionResponse, values: string[]) {
  sudokuAnswers.value = {
    ...sudokuAnswers.value,
    [question.id]: values
  }
  const answer = values.join('')
  typedAnswer.value = answer
  draftAnswers.value = {
    ...draftAnswers.value,
    [question.id]: answer
  }
}

function selectSudokuCell(cell: SudokuCell) {
  if (!currentQuestion.value || currentAnswer.value || cell.given) {
    return
  }
  activeSudokuCells.value = {
    ...activeSudokuCells.value,
    [currentQuestion.value.id]: cell.index
  }
}

function setSudokuValue(value: string) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value) {
    return
  }
  ensureSudokuState(question)
  const activeIndex = activeSudokuCells.value[question.id]
  const game = parseSudokuGame(question)
  const cell = game.cells[activeIndex]
  if (!cell || cell.given) {
    return
  }
  const values = [...(sudokuAnswers.value[question.id] ?? emptySudokuValues(question))]
  values[activeIndex] = value
  updateSudokuAnswer(question, values)
  const nextCell = game.cells.find((candidate) => !candidate.given && !values[candidate.index] && candidate.index > activeIndex)
    ?? game.cells.find((candidate) => !candidate.given && !values[candidate.index])
  if (nextCell) {
    activeSudokuCells.value = {
      ...activeSudokuCells.value,
      [question.id]: nextCell.index
    }
  }
}

function clearSudokuCell() {
  const question = currentQuestion.value
  if (!question || currentAnswer.value) {
    return
  }
  const activeIndex = activeSudokuCells.value[question.id]
  const game = parseSudokuGame(question)
  if (game.cells[activeIndex]?.given) {
    return
  }
  const values = [...(sudokuAnswers.value[question.id] ?? emptySudokuValues(question))]
  values[activeIndex] = ''
  updateSudokuAnswer(question, values)
}

function isSudokuComplete(question: QuestionResponse) {
  const values = sudokuAnswers.value[question.id] ?? emptySudokuValues(question)
  return values.length === parseSudokuGame(question).size ** 2 && values.every((value) => Boolean(value))
}

function sudokuCellClass(cell: SudokuCell) {
  const question = currentQuestion.value
  const active = Boolean(question && activeSudokuCells.value[question.id] === cell.index && !currentAnswer.value)
  const correctValue = sudokuCorrectValue(cell.index)
  return {
    given: cell.given,
    active,
    filled: Boolean(cell.value),
    correct: Boolean(currentAnswer && cell.value && correctValue && cell.value === correctValue),
    wrong: Boolean(currentAnswer && cell.value && correctValue && cell.value !== correctValue)
  }
}

function sudokuCellStyle(cell: SudokuCell) {
  const size = sudokuGame.value.size
  const box = sudokuGame.value.boxSize
  return {
    borderTopWidth: cell.row % box === 0 ? '3px' : '1px',
    borderLeftWidth: cell.col % box === 0 ? '3px' : '1px',
    borderRightWidth: cell.col === size - 1 ? '3px' : '1px',
    borderBottomWidth: cell.row === size - 1 ? '3px' : '1px'
  }
}

function sudokuCorrectRows() {
  const answer = currentAnswer.value?.correctAnswer ?? ''
  return answer
    .split('/')
    .map((row) => row.trim().replace(/[^0-9]/g, ''))
    .filter(Boolean)
}

function sudokuCorrectValue(cellIndex: number) {
  const rows = sudokuCorrectRows()
  if (!rows.length) {
    return ''
  }
  const size = rows[0]?.length ?? sudokuGame.value.size
  const row = Math.floor(cellIndex / size)
  const col = cellIndex % size
  return rows[row]?.[col] ?? ''
}

function emptyMemoryGame(): MemoryGame {
  return {
    previewSeconds: 5,
    pairCount: 0,
    perfectMistakes: 0,
    cards: []
  }
}

function parseMemoryGame(question: QuestionResponse): MemoryGame {
  const game = emptyMemoryGame()
  question.answerSlots.forEach((slot, slotIndex) => {
    const parts = slot.split('|')
    if (parts[0] === 'M') {
      game.previewSeconds = Number(parts[1]) || 5
      game.pairCount = Number(parts[2]) || 0
      game.perfectMistakes = Number(parts[3]) || 0
      return
    }
    if (parts[0] !== 'C') {
      return
    }
    const [, cardId, pairId, emoji, label = ''] = parts
    if (!cardId || !pairId || !emoji) {
      return
    }
    game.cards.push({
      key: `${question.id}-${cardId}-${slotIndex}`,
      cardId,
      pairId,
      emoji,
      label
    })
  })
  if (!game.pairCount) {
    game.pairCount = new Set(game.cards.map((card) => card.pairId)).size
  }
  return game
}

function ensureMemoryState(question: QuestionResponse) {
  if (!memoryFlippedCards.value[question.id]) {
    memoryFlippedCards.value = { ...memoryFlippedCards.value, [question.id]: [] }
  }
  if (!memoryMatchedPairs.value[question.id]) {
    memoryMatchedPairs.value = { ...memoryMatchedPairs.value, [question.id]: [] }
  }
  if (!Number.isFinite(memoryAttempts.value[question.id])) {
    memoryAttempts.value = { ...memoryAttempts.value, [question.id]: 0 }
  }
}

function clearMemoryTimers() {
  if (memoryPreviewTimer) {
    window.clearInterval(memoryPreviewTimer)
    memoryPreviewTimer = undefined
    if (memoryPreviewQuestionId != null) {
      memoryPreviewing.value = { ...memoryPreviewing.value, [memoryPreviewQuestionId]: false }
      memoryPreviewRemaining.value = { ...memoryPreviewRemaining.value, [memoryPreviewQuestionId]: 0 }
      memoryPreviewQuestionId = null
    }
  }
  if (memoryMismatchTimer) {
    window.clearTimeout(memoryMismatchTimer)
    memoryMismatchTimer = undefined
    if (memoryMismatchQuestionId != null) {
      memoryFlippedCards.value = { ...memoryFlippedCards.value, [memoryMismatchQuestionId]: [] }
      memoryLocked.value = { ...memoryLocked.value, [memoryMismatchQuestionId]: false }
      memoryMismatchQuestionId = null
    }
  }
}

function startMemoryPreview(question: QuestionResponse) {
  if (currentAnswer.value || memoryPreviewSeen.value[question.id] || isMemorySolved(question)) {
    return
  }
  activeStartedAt.value = null
  const seconds = parseMemoryGame(question).previewSeconds
  memoryPreviewSeen.value = { ...memoryPreviewSeen.value, [question.id]: true }
  memoryPreviewing.value = { ...memoryPreviewing.value, [question.id]: true }
  memoryPreviewRemaining.value = { ...memoryPreviewRemaining.value, [question.id]: seconds }
  memoryPreviewQuestionId = question.id
  memoryPreviewTimer = window.setInterval(() => {
    const remaining = (memoryPreviewRemaining.value[question.id] ?? seconds) - 1
    memoryPreviewRemaining.value = { ...memoryPreviewRemaining.value, [question.id]: Math.max(0, remaining) }
    if (remaining <= 0) {
      if (memoryPreviewTimer) {
        window.clearInterval(memoryPreviewTimer)
        memoryPreviewTimer = undefined
      }
      memoryPreviewing.value = { ...memoryPreviewing.value, [question.id]: false }
      memoryPreviewQuestionId = null
      void syncActiveTimeIfNeeded(true)
    }
  }, 1000)
}

function startMemoryPairsGame() {
  const question = currentQuestion.value
  if (!question || !isMemoryPairsKind(question)) {
    return
  }
  startMemoryPreview(question)
}

function memoryMistakeText(game = memoryGame.value) {
  if (game.perfectMistakes === 1) {
    return '1 грешка'
  }
  return `${game.perfectMistakes} грешки`
}

function memoryRemainingMistakeText() {
  const remaining = Math.max(0, memoryGame.value.perfectMistakes - memoryMistakesUsed.value)
  if (remaining === 1) {
    return 'остава 1 грешка за перфектен резултат'
  }
  if (remaining > 1) {
    return `остават ${remaining} грешки за перфектен резултат`
  }
  return 'следващите грешки вече намаляват резултата'
}

function memoryCardById(question: QuestionResponse, cardId: string) {
  return parseMemoryGame(question).cards.find((card) => card.cardId === cardId)
}

function isMemoryCardOpen(card: MemoryCard) {
  if (memoryPreviewActive.value || currentAnswer.value) {
    return true
  }
  return memoryMatched.value.includes(card.pairId) || memoryFlipped.value.includes(card.cardId)
}

function isMemorySolved(question: QuestionResponse | null | undefined) {
  if (!question || !isMemoryPairsKind(question)) {
    return false
  }
  const game = parseMemoryGame(question)
  return game.pairCount > 0 && (memoryMatchedPairs.value[question.id] ?? []).length >= game.pairCount
}

function memoryAnswerText(question: QuestionResponse) {
  const game = parseMemoryGame(question)
  const attempts = memoryAttempts.value[question.id] ?? 0
  return `SOLVED|attempts=${attempts}|pairs=${game.pairCount}`
}

function updateMemoryAnswer(question: QuestionResponse) {
  const answer = memoryAnswerText(question)
  typedAnswer.value = answer
  draftAnswers.value = {
    ...draftAnswers.value,
    [question.id]: answer
  }
  if (activeStartedAt.value) {
    void syncActiveTime(false)
  }
}

function flipMemoryCard(card: MemoryCard) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value || memoryPreviewActive.value || memoryLocked.value[question.id]) {
    return
  }
  ensureMemoryState(question)
  if (memoryMatched.value.includes(card.pairId) || memoryFlipped.value.includes(card.cardId)) {
    return
  }
  const currentFlipped = memoryFlippedCards.value[question.id] ?? []
  if (currentFlipped.length >= 2) {
    return
  }
  const nextFlipped = [...currentFlipped, card.cardId]
  memoryFlippedCards.value = {
    ...memoryFlippedCards.value,
    [question.id]: nextFlipped
  }
  if (nextFlipped.length < 2) {
    return
  }

  const [firstCardId, secondCardId] = nextFlipped
  const firstCard = memoryCardById(question, firstCardId)
  const secondCard = memoryCardById(question, secondCardId)
  memoryAttempts.value = {
    ...memoryAttempts.value,
    [question.id]: (memoryAttempts.value[question.id] ?? 0) + 1
  }
  if (firstCard && secondCard && firstCard.pairId === secondCard.pairId) {
    const matched = [...new Set([...(memoryMatchedPairs.value[question.id] ?? []), firstCard.pairId])]
    memoryMatchedPairs.value = {
      ...memoryMatchedPairs.value,
      [question.id]: matched
    }
    memoryFlippedCards.value = {
      ...memoryFlippedCards.value,
      [question.id]: []
    }
    if (matched.length >= parseMemoryGame(question).pairCount) {
      updateMemoryAnswer(question)
    }
    return
  }

  memoryLocked.value = { ...memoryLocked.value, [question.id]: true }
  const questionId = question.id
  memoryMismatchQuestionId = questionId
  memoryMismatchTimer = window.setTimeout(() => {
    memoryFlippedCards.value = { ...memoryFlippedCards.value, [questionId]: [] }
    memoryLocked.value = { ...memoryLocked.value, [questionId]: false }
    memoryMismatchTimer = undefined
    memoryMismatchQuestionId = null
  }, 850)
}

function memoryCardClass(card: MemoryCard) {
  return {
    open: isMemoryCardOpen(card),
    matched: memoryMatched.value.includes(card.pairId),
    flipped: memoryFlipped.value.includes(card.cardId)
  }
}

function memoryAttemptsFromAnswer(answer: string | undefined) {
  const value = (answer ?? '').split('|').find((part) => part.startsWith('attempts='))
  return value ? value.replace('attempts=', '') : String(memoryAttemptsCount.value)
}

function emptyPatternGame(): PatternGame {
  return {
    previewSeconds: 10,
    length: 0,
    sequence: [],
    choices: []
  }
}

function parsePatternGame(question: QuestionResponse): PatternGame {
  const game = emptyPatternGame()
  question.answerSlots.forEach((slot, slotIndex) => {
    const parts = slot.split('|')
    if (parts[0] === 'P') {
      game.previewSeconds = Number(parts[1]) || 10
      game.length = Number(parts[2]) || 0
      return
    }
    if (parts[0] !== 'S') {
      return
    }
    const token = patternTokenFromParts(question, parts, slotIndex)
    if (token) {
      game.sequence.push(token)
    }
  })
  game.choices = question.choices
    .map((choice, choiceIndex) => patternTokenFromParts(question, choice.split('|'), choiceIndex))
    .filter((token): token is PatternToken => Boolean(token))
  if (!game.length) {
    game.length = game.sequence.length
  }
  return game
}

function patternTokenFromParts(question: QuestionResponse, parts: string[], index: number): PatternToken | null {
  if (parts.length < 5) {
    return null
  }
  const [, id, shape, color, label = ''] = parts
  if (!id || !shape || !color) {
    return null
  }
  return {
    key: `${question.id}-${id}-${index}-${parts[0]}`,
    id,
    shape,
    color,
    label
  }
}

function emptyPatternSlots(question: QuestionResponse): (PatternToken | null)[] {
  return Array.from({ length: parsePatternGame(question).length }, () => null as PatternToken | null)
}

function ensurePatternState(question: QuestionResponse) {
  if (!patternAnswers.value[question.id]) {
    patternAnswers.value = { ...patternAnswers.value, [question.id]: emptyPatternSlots(question) }
  }
  if (!Number.isFinite(activePatternSlots.value[question.id])) {
    activePatternSlots.value = { ...activePatternSlots.value, [question.id]: 0 }
  }
}

function clearPatternTimers() {
  if (patternPreviewTimer) {
    window.clearInterval(patternPreviewTimer)
    patternPreviewTimer = undefined
    if (patternPreviewQuestionId != null) {
      patternPreviewing.value = { ...patternPreviewing.value, [patternPreviewQuestionId]: false }
      patternPreviewRemaining.value = { ...patternPreviewRemaining.value, [patternPreviewQuestionId]: 0 }
      patternPreviewQuestionId = null
    }
  }
}

function startPatternPreview(question: QuestionResponse) {
  if (currentAnswer.value || patternPreviewSeen.value[question.id]) {
    return
  }
  activeStartedAt.value = null
  const seconds = parsePatternGame(question).previewSeconds
  patternPreviewSeen.value = { ...patternPreviewSeen.value, [question.id]: true }
  patternPreviewing.value = { ...patternPreviewing.value, [question.id]: true }
  patternPreviewRemaining.value = { ...patternPreviewRemaining.value, [question.id]: seconds }
  patternPreviewQuestionId = question.id
  patternPreviewTimer = window.setInterval(() => {
    const remaining = (patternPreviewRemaining.value[question.id] ?? seconds) - 1
    patternPreviewRemaining.value = { ...patternPreviewRemaining.value, [question.id]: Math.max(0, remaining) }
    if (remaining <= 0) {
      if (patternPreviewTimer) {
        window.clearInterval(patternPreviewTimer)
        patternPreviewTimer = undefined
      }
      patternPreviewing.value = { ...patternPreviewing.value, [question.id]: false }
      patternPreviewQuestionId = null
      void syncActiveTimeIfNeeded(true)
    }
  }, 1000)
}

function startPatternSequenceGame() {
  const question = currentQuestion.value
  if (!question || !isPatternSequenceKind(question)) {
    return
  }
  startPatternPreview(question)
}

function patternSlotIndex(question: QuestionResponse) {
  const slots = patternAnswers.value[question.id] ?? []
  const current = activePatternSlots.value[question.id] ?? 0
  if (!slots[current]) {
    return Math.min(current, Math.max(0, slots.length - 1))
  }
  const emptyIndex = slots.findIndex((slot) => !slot)
  return emptyIndex >= 0 ? emptyIndex : Math.max(0, slots.length - 1)
}

function choosePatternToken(token: PatternToken) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value || patternPreviewActive.value || patternIntroVisible.value) {
    return
  }
  ensurePatternState(question)
  const slots = [...(patternAnswers.value[question.id] ?? emptyPatternSlots(question))]
  const slotIndex = patternSlotIndex(question)
  slots[slotIndex] = token
  const nextEmpty = slots.findIndex((slot, index) => index > slotIndex && !slot)
  activePatternSlots.value = {
    ...activePatternSlots.value,
    [question.id]: nextEmpty >= 0 ? nextEmpty : Math.min(slotIndex + 1, Math.max(0, slots.length - 1))
  }
  updatePatternAnswer(question, slots)
}

function clearPatternSlot(slotIndex: number) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value) {
    return
  }
  const slots = [...(patternAnswers.value[question.id] ?? emptyPatternSlots(question))]
  slots[slotIndex] = null
  activePatternSlots.value = { ...activePatternSlots.value, [question.id]: slotIndex }
  updatePatternAnswer(question, slots)
}

function updatePatternAnswer(question: QuestionResponse, slots: (PatternToken | null)[]) {
  patternAnswers.value = { ...patternAnswers.value, [question.id]: slots }
  const answer = patternAnswerText(question)
  typedAnswer.value = answer
  draftAnswers.value = {
    ...draftAnswers.value,
    [question.id]: answer
  }
}

function patternAnswerText(question: QuestionResponse) {
  return (patternAnswers.value[question.id] ?? [])
    .map((token) => token?.id ?? '')
    .join(',')
}

function seedPatternFromAnswer(question: QuestionResponse, answer: string) {
  if (!answer.trim()) {
    return
  }
  const tokensById = new Map(parsePatternGame(question).choices.map((token) => [token.id, token]))
  const slots: (PatternToken | null)[] = answer.split(',').map((id) => tokensById.get(id.trim()) ?? null)
  if (slots.length) {
    const paddedSlots: (PatternToken | null)[] = [
      ...slots,
      ...Array.from({ length: Math.max(0, parsePatternGame(question).length - slots.length) }, () => null as PatternToken | null)
    ].slice(0, parsePatternGame(question).length)
    patternAnswers.value = {
      ...patternAnswers.value,
      [question.id]: paddedSlots
    }
  }
}

function isPatternComplete(question: QuestionResponse) {
  return (patternAnswers.value[question.id] ?? []).length === parsePatternGame(question).length
    && (patternAnswers.value[question.id] ?? []).every(Boolean)
}

function patternCorrectText(question: QuestionResponse | null | undefined) {
  if (!question) {
    return ''
  }
  return parsePatternGame(question).sequence.map((token) => token.label).join(' → ')
}

function patternTokenStyle(token: PatternToken) {
  return {
    '--pattern-color': token.color
  }
}

function emptyFindObjectScene(): FindObjectScene {
  return {
    name: '',
    theme: 'room',
    objects: []
  }
}

function parseFindObjectScene(question: QuestionResponse): FindObjectScene {
  const scene = emptyFindObjectScene()
  question.answerSlots.forEach((slot, slotIndex) => {
    const parts = slot.split('|')
    if (parts[0] === 'F') {
      scene.name = parts[1] ?? ''
      scene.theme = parts[2] ?? 'room'
      return
    }
    if (parts[0] !== 'I') {
      return
    }
    const [, id, x, y, emoji, size, label = id, targetText = label] = parts
    scene.objects.push({
      key: `${question.id}-${id}-${slotIndex}`,
      id,
      x: Number(x),
      y: Number(y),
      emoji,
      size: Number(size),
      label,
      targetText
    })
  })
  return scene
}

function chooseFindObject(item: FindObjectItem) {
  if (currentAnswer.value) {
    return
  }
  typedAnswer.value = item.id
}

function findObjectLabel(question: QuestionResponse | null | undefined, id: string) {
  if (!question) {
    return id
  }
  return parseFindObjectScene(question).objects.find((item) => item.id === id)?.targetText ?? id
}

function findObjectTargetItem(question: QuestionResponse | null | undefined) {
  if (!question) {
    return null
  }
  const scene = parseFindObjectScene(question)
  return scene.objects.find((item) => item.label === question.speechText || item.targetText === question.speechText) ?? null
}

function findObjectClass(item: FindObjectItem) {
  if (!currentAnswer.value) {
    return {
      selected: typedAnswer.value === item.id
    }
  }
  const correctId = currentAnswer.value.correctAnswer
  return {
    correct: item.id === correctId,
    wrong: typedAnswer.value === item.id && item.id !== correctId
  }
}

function findObjectMarker(item: FindObjectItem) {
  if (!currentAnswer.value) {
    return typedAnswer.value === item.id ? '✓' : ''
  }
  if (item.id === currentAnswer.value.correctAnswer) {
    return '✓'
  }
  if (typedAnswer.value === item.id) {
    return '×'
  }
  return ''
}

function emptySpotDifferenceScene(): SpotDifferenceScene {
  return {
    name: '',
    theme: 'park',
    left: [],
    right: [],
    differenceIds: []
  }
}

function parseSpotDifferenceScene(question: QuestionResponse): SpotDifferenceScene {
  const scene = emptySpotDifferenceScene()
  const differences = new Set<string>()
  question.answerSlots.forEach((slot, slotIndex) => {
    const parts = slot.split('|')
    if (parts[0] === 'S') {
      scene.name = parts[1] ?? ''
      scene.theme = parts[2] ?? 'park'
      return
    }
    if (parts[0] !== 'O') {
      return
    }
    const [, side, id, x, y, emoji, size, differenceId = ''] = parts
    if (side !== 'L' && side !== 'R') {
      return
    }
    const item: SpotDifferenceObject = {
      key: `${question.id}-${side}-${id}-${slotIndex}`,
      selectionKey: differenceId || `N:${side}:${id}`,
      side,
      id,
      x: Number(x),
      y: Number(y),
      emoji,
      size: Number(size),
      differenceId
    }
    if (differenceId) {
      differences.add(differenceId)
    }
    if (side === 'L') {
      scene.left.push(item)
    } else {
      scene.right.push(item)
    }
  })
  scene.differenceIds = [...differences].sort()
  return scene
}

function parseSpotDifferenceAnswer(answer: string | undefined) {
  return [...new Set((answer ?? '')
    .split(';')
    .map((part) => part.trim())
    .filter(Boolean))]
    .sort()
}

function serializeSpotDifferenceAnswer(ids: string[]) {
  return [...new Set(ids)].sort().join(';')
}

function spotAnswerProgress(question: QuestionResponse, answer: string | undefined) {
  const correct = new Set(parseSpotDifferenceScene(question).differenceIds)
  const selected = parseSpotDifferenceAnswer(answer)
  const correctSelections = selected.filter((selection) => correct.has(selection)).length
  const wrongSelections = selected.filter((selection) => !correct.has(selection)).length
  return Math.max(0, Math.min(correct.size, correctSelections - wrongSelections))
}

function spotDraftProgress(question: QuestionResponse, answer: string | undefined) {
  const total = parseSpotDifferenceScene(question).differenceIds.length
  return Math.min(total, parseSpotDifferenceAnswer(answer).length)
}

function toggleSpotDifference(item: SpotDifferenceObject) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value) {
    return
  }
  const selected = new Set(spotDifferenceAnswers.value[question.id] ?? parseSpotDifferenceAnswer(typedAnswer.value))
  if (selected.has(item.selectionKey)) {
    selected.delete(item.selectionKey)
  } else {
    selected.add(item.selectionKey)
  }
  const answer = serializeSpotDifferenceAnswer([...selected])
  spotDifferenceAnswers.value = {
    ...spotDifferenceAnswers.value,
    [question.id]: parseSpotDifferenceAnswer(answer)
  }
  typedAnswer.value = answer
}

function spotObjectClass(item: SpotDifferenceObject) {
  const selected = selectedSpotDifferences.value.includes(item.selectionKey)
  if (!currentAnswer.value) {
    return {
      selected
    }
  }
  const correctIds = parseSpotDifferenceAnswer(currentAnswer.value.correctAnswer)
  return {
    correct: selected && correctIds.includes(item.differenceId),
    wrong: selected && !correctIds.includes(item.selectionKey),
    missed: Boolean(item.differenceId) && correctIds.includes(item.differenceId) && !selected
  }
}

function spotMarker(item: SpotDifferenceObject) {
  if (!currentAnswer.value) {
    return selectedSpotDifferences.value.includes(item.selectionKey) ? '○' : ''
  }
  const correctIds = parseSpotDifferenceAnswer(currentAnswer.value.correctAnswer)
  const selected = selectedSpotDifferences.value.includes(item.selectionKey)
  if (selected && correctIds.includes(item.differenceId)) {
    return '✓'
  }
  if (selected) {
    return '×'
  }
  return correctIds.includes(item.differenceId) ? '!' : ''
}

function isMissingLetterQuestion(question: QuestionResponse | null | undefined) {
  return question?.sourceMode === 'WORD_MISSING_LETTER'
}

function isWrongLetterQuestion(question: QuestionResponse | null | undefined) {
  return question?.sourceMode === 'WORD_WRONG_LETTER'
}

function wordPromptText(question: QuestionResponse) {
  if (isMissingLetterQuestion(question) && currentAnswer.value) {
    return question.speechText ?? question.prompt
  }
  return question.prompt
}

function wrongLetterDisplayText(question: QuestionResponse) {
  return currentAnswer.value ? question.speechText ?? question.prompt : question.prompt
}

function wrongLetterParts(question: QuestionResponse) {
  return Array.from(wrongLetterDisplayText(question)).map((letter, letterIndex) => ({
    key: `${question.id}-${letterIndex}-${letter}`,
    letter,
    gap: !letter.trim()
  }))
}

function wrongLetterInstruction(question: QuestionResponse) {
  if (currentAnswer.value) {
    return `Правилната дума е ${question.speechText ?? question.prompt}`
  }
  return selectedWrongLetter()
    ? 'С коя буква да я сменим?'
    : 'Коя буква е объркана?'
}

function splitWrongLetterAnswer(answer: string) {
  const [wrongLetter = '', replacementLetter = ''] = answer.split('=', 2)
  return [wrongLetter.trim(), replacementLetter.trim()]
}

function selectedWrongLetter() {
  return splitWrongLetterAnswer(typedAnswer.value)[0]
}

function selectedReplacementLetter() {
  return splitWrongLetterAnswer(typedAnswer.value)[1]
}

function bgLetter(letter: string | null | undefined) {
  return (letter ?? '').trim().toLocaleUpperCase('bg-BG')
}

function wordLetters(text: string | null | undefined) {
  return Array.from((text ?? '').toLocaleUpperCase('bg-BG'))
    .filter((letter) => Boolean(letter.trim()) && letter !== '-')
}

function expectedWrongLetterReplacement(question: QuestionResponse) {
  const promptLetters = wordLetters(question.prompt)
  const answerLetters = wordLetters(question.speechText ?? question.prompt)
  const size = Math.min(promptLetters.length, answerLetters.length)
  for (let letterIndex = 0; letterIndex < size; letterIndex += 1) {
    if (promptLetters[letterIndex] !== answerLetters[letterIndex]) {
      return answerLetters[letterIndex]
    }
  }
  return ''
}

function wrongLetterReplacementChoices(question: QuestionResponse) {
  const selected = bgLetter(selectedWrongLetter())
  const correct = expectedWrongLetterReplacement(question)
  const distractors: string[] = []

  const addDistractor = (letter: string) => {
    const normalized = bgLetter(letter)
    if (!normalized || normalized === correct || (normalized === selected && normalized !== correct) || distractors.includes(normalized)) {
      return
    }
    distractors.push(normalized)
  }

  question.choices.forEach(addDistractor)
  BULGARIAN_REPLACEMENT_DISTRACTORS.forEach(addDistractor)

  if (!correct) {
    return distractors.slice(0, 3)
  }

  const choices = distractors.slice(0, 2)
  choices.splice(question.id % (choices.length + 1), 0, correct)
  return choices.slice(0, 3)
}

function chooseWrongLetter(letter: string) {
  if (currentAnswer.value) {
    return
  }
  typedAnswer.value = bgLetter(letter)
}

function chooseReplacementLetter(letter: string) {
  if (!currentQuestion.value || currentAnswer.value || !selectedWrongLetter()) {
    return
  }
  typedAnswer.value = `${selectedWrongLetter()}=${bgLetter(letter)}`
}

function wrongLetterFeedbackText(question: QuestionResponse | null, answer: string) {
  const [wrongLetter, replacementLetter] = splitWrongLetterAnswer(answer)
  const correction = wrongLetter && replacementLetter
    ? `${wrongLetter} се сменя с ${replacementLetter}`
    : answer
  return `${correction}, думата е ${question?.speechText ?? question?.prompt ?? ''}`
}

function wrongFeedbackIntro(question: QuestionResponse | null) {
  if (question?.sourceMode === 'WORD_PICTURE') {
    return 'Почти! Правилната картинка е'
  }
  if (question?.sourceMode === 'WORD_MISSING_LETTER') {
    return question.answerSlots.length > 1 ? 'Почти! Липсващите букви са' : 'Почти! Липсващата буква е'
  }
  if (question?.sourceMode === 'WORD_WRONG_LETTER') {
    return 'Почти! Поправката е'
  }
  if (isGroupingKind(question)) {
    return 'Почти! Виж правилните кошници:'
  }
  if (isSpotDifferencesKind(question)) {
    return 'Почти! Виж отбелязаните разлики.'
  }
  if (isFindObjectKind(question)) {
    return 'Почти! Верният предмет е'
  }
  if (isMemoryPairsKind(question)) {
    return 'Опитай да отвориш всички двойки.'
  }
  if (isPatternSequenceKind(question)) {
    return 'Почти! Правилният модел е'
  }
  if (isSudokuKind(question)) {
    return 'Почти! Правилното судоку е'
  }
  if (isWordKind(question)) {
    return 'Почти! Виж как се подрежда думата:'
  }
  return 'Почти! Верният отговор е'
}

function feedbackAnswerText(question: QuestionResponse | null, answer: string) {
  if (isWrongLetterQuestion(question)) {
    return wrongLetterFeedbackText(question, answer)
  }
  if (isSpotDifferencesKind(question)) {
    return 'Зелените знаци показват разликите'
  }
  if (isFindObjectKind(question)) {
    return findObjectLabel(question, answer)
  }
  if (isMemoryPairsKind(question)) {
    return 'Всички двойки трябва да са отворени'
  }
  if (isPatternSequenceKind(question)) {
    return patternCorrectText(question)
  }
  if (isSudokuKind(question)) {
    return answer
  }
  if (!isGroupingKind(question)) {
    return answer
  }
  return answer
    .split(';')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => part.replace('=', ' → '))
    .join('\n')
}

function answerLayoutTokens(question: QuestionResponse) {
  if (question.answerSlots?.length) {
    return question.answerSlots
  }
  if (question.kind === 'LETTER_ORDER' && question.speechText) {
    return Array.from(question.speechText.toUpperCase()).map((letter) => (letter.trim() ? letter : ' '))
  }
  return question.choices
}

function answerTokenSlots(question: QuestionResponse) {
  return answerLayoutTokens(question).filter((slot) => !isGapSlot(slot))
}

function isGapSlot(slot: string) {
  return !slot.trim()
}

function builderSlotWidth(slot: string | undefined) {
  const length = Math.max(1, Array.from(slot ?? '').length)
  return Math.max(58, 30 + length * 22)
}

function emptyBuilderSlots(question: QuestionResponse): BuilderSlot[] {
  return Array.from({ length: answerTokenSlots(question).length }, () => null)
}

function ensureBuilderSlots(question: QuestionResponse) {
  const expectedCount = answerTokenSlots(question).length
  const existing = builderAnswers.value[question.id]
  if (existing?.length === expectedCount) {
    return existing
  }
  const next = Array.from({ length: expectedCount }, (_, slotIndex) => existing?.[slotIndex] ?? null)
  builderAnswers.value = {
    ...builderAnswers.value,
    [question.id]: next
  }
  return next
}

function activeBuilderIndex(question: QuestionResponse) {
  const slots = builderAnswers.value[question.id] ?? emptyBuilderSlots(question)
  const savedIndex = activeBuilderSlots.value[question.id]
  if (Number.isInteger(savedIndex) && savedIndex >= 0 && savedIndex < slots.length) {
    return savedIndex
  }
  const firstEmpty = slots.findIndex((slot) => !slot)
  return firstEmpty >= 0 ? firstEmpty : Math.max(0, slots.length - 1)
}

function setActiveBuilderSlot(question: QuestionResponse, slotIndex: number) {
  activeBuilderSlots.value = {
    ...activeBuilderSlots.value,
    [question.id]: slotIndex
  }
}

function builderAnswerText(question: QuestionResponse) {
  const slots = builderAnswers.value[question.id]
  return slots?.filter((token): token is BuilderToken => Boolean(token)).map((token) => token.text).join('') ?? ''
}

function updateBuilderAnswer(question: QuestionResponse, slots: BuilderSlot[]) {
  builderAnswers.value = {
    ...builderAnswers.value,
    [question.id]: slots
  }
  const answer = slots.filter((token): token is BuilderToken => Boolean(token)).map((token) => token.text).join('')
  typedAnswer.value = answer
  draftAnswers.value = {
    ...draftAnswers.value,
    [question.id]: answer
  }
}

function placeToken(token: BuilderToken) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value) {
    return
  }
  const slots = ensureBuilderSlots(question)
  let targetSlot = activeBuilderIndex(question)
  if (slots[targetSlot]) {
    targetSlot = nextOpenSlot(slots, targetSlot)
  }
  if (targetSlot < 0) {
    return
  }
  const next = [...slots]
  next[targetSlot] = token
  updateBuilderAnswer(question, next)
  const followingOpenSlot = nextOpenSlot(next, targetSlot + 1)
  setActiveBuilderSlot(question, followingOpenSlot >= 0 ? followingOpenSlot : targetSlot)
}

function removePlacedToken(slotIndex: number) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value) {
    return
  }
  const next = [...ensureBuilderSlots(question)]
  next[slotIndex] = null
  updateBuilderAnswer(question, next)
  setActiveBuilderSlot(question, slotIndex)
}

function handleBuilderSlotClick(slotIndex: number | undefined) {
  const question = currentQuestion.value
  if (!question || typeof slotIndex !== 'number') {
    return
  }
  if (builderSlots.value[slotIndex]) {
    removePlacedToken(slotIndex)
  } else {
    setActiveBuilderSlot(question, slotIndex)
  }
}

function nextOpenSlot(slots: BuilderSlot[], startIndex: number) {
  for (let slotIndex = Math.max(0, startIndex); slotIndex < slots.length; slotIndex++) {
    if (!slots[slotIndex]) {
      return slotIndex
    }
  }
  for (let slotIndex = 0; slotIndex < Math.max(0, startIndex); slotIndex++) {
    if (!slots[slotIndex]) {
      return slotIndex
    }
  }
  return -1
}

function startDrag(token: BuilderToken) {
  draggedTokenId = token.id
}

function dropToken() {
  if (!draggedTokenId) {
    return
  }
  const token = availableTokens.value.find((candidate) => candidate.id === draggedTokenId)
  if (token) {
    placeToken(token)
  }
  draggedTokenId = ''
}

function seedBuildersFromDrafts() {
  for (const question of questions.value) {
    if (!isBuilderKind(question) || builderAnswers.value[question.id]) {
      continue
    }
    const emptySlots = emptyBuilderSlots(question)
    const savedAnswer = draftAnswers.value[question.id]
    if (!savedAnswer) {
      builderAnswers.value[question.id] = emptySlots
      continue
    }
    const tokens = question.choices.map((text, tokenIndex) => ({ id: `${question.id}-${tokenIndex}-${text}`, text }))
    const savedTokens = savedBuilderTokens(question, savedAnswer)
    const used = new Set<string>()
    const restored = [...emptySlots]
    savedTokens.slice(0, restored.length).forEach((savedToken, slotIndex) => {
      const token = tokens.find((candidate) => candidate.text === savedToken && !used.has(candidate.id))
      if (token) {
        restored[slotIndex] = token
        used.add(token.id)
      }
    })
    builderAnswers.value[question.id] = restored
    const firstEmpty = restored.findIndex((slot) => !slot)
    if (firstEmpty >= 0) {
      activeBuilderSlots.value[question.id] = firstEmpty
    }
  }
}

function savedBuilderTokens(question: QuestionResponse, savedAnswer: string) {
  const correctTokens = answerTokenSlots(question)
  const normalizedSavedAnswer = savedAnswer.replace(/\s+/g, '').toUpperCase()
  if (correctTokens.join('').toUpperCase() === normalizedSavedAnswer) {
    return correctTokens
  }
  if (question.kind === 'LETTER_ORDER') {
    return Array.from(normalizedSavedAnswer)
  }
  return segmentAnswerFromChoices(normalizedSavedAnswer, question.choices) ?? []
}

function segmentAnswerFromChoices(answer: string, choices: string[]) {
  const counts = new Map<string, number>()
  choices.forEach((choice) => counts.set(choice, (counts.get(choice) ?? 0) + 1))
  const candidates = [...new Set(choices)].sort((left, right) => right.length - left.length)
  return segmentAnswer(answer, candidates, counts)
}

function segmentAnswer(answer: string, candidates: string[], counts: Map<string, number>): string[] | null {
  if (!answer) {
    return []
  }
  for (const candidate of candidates) {
    const available = counts.get(candidate) ?? 0
    if (!available || !answer.startsWith(candidate)) {
      continue
    }
    counts.set(candidate, available - 1)
    const tail = segmentAnswer(answer.slice(candidate.length), candidates, counts)
    counts.set(candidate, available)
    if (tail) {
      return [candidate, ...tail]
    }
  }
  return null
}

function groupingQuestionItems(question: QuestionResponse): GroupingItem[] {
  if (!isGroupingKind(question)) {
    return []
  }
  return question.choices.map((choice) => {
    const { image, word } = parseGroupingChoice(choice)
    return {
      key: word,
      image,
      word
    }
  })
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

function ensureGroupingAnswer(question: QuestionResponse) {
  if (!isGroupingKind(question) || groupingAnswers.value[question.id]) {
    return
  }
  const savedAnswer = currentAnswer.value?.answer ?? draftAnswers.value[question.id] ?? ''
  groupingAnswers.value = {
    ...groupingAnswers.value,
    [question.id]: parseGroupingAnswer(savedAnswer)
  }
}

function parseGroupingAnswer(answer: string) {
  return answer
    .split(';')
    .map((part) => part.trim())
    .filter((part) => part.includes('='))
    .reduce<Record<string, string>>((assignments, part) => {
      const [word, letter] = part.split('=', 2)
      if (word?.trim() && letter?.trim()) {
        assignments[word.trim().toUpperCase()] = letter.trim().toUpperCase()
      }
      return assignments
    }, {})
}

function groupingAnswerText(question: QuestionResponse) {
  const assignments = groupingAnswers.value[question.id] ?? {}
  return Object.entries(assignments)
    .filter(([, letter]) => Boolean(letter))
    .map(([word, letter]) => `${word}=${letter}`)
    .sort()
    .join(';')
}

function updateGroupingAnswer(question: QuestionResponse, assignments: Record<string, string>) {
  groupingAnswers.value = {
    ...groupingAnswers.value,
    [question.id]: assignments
  }
  const answer = groupingAnswerText(question)
  typedAnswer.value = answer
  draftAnswers.value = {
    ...draftAnswers.value,
    [question.id]: answer
  }
}

function groupingItemsForLetter(letter: string) {
  return groupingItems.value.filter((item) => groupingAssignment.value[item.word] === letter)
}

function selectGroupingItem(item: GroupingItem) {
  if (currentAnswer.value) {
    return
  }
  selectedGroupingWord.value = selectedGroupingWord.value === item.word ? '' : item.word
}

function assignGroupingItem(letter: string) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value || !selectedGroupingWord.value) {
    return
  }
  updateGroupingAnswer(question, {
    ...(groupingAnswers.value[question.id] ?? {}),
    [selectedGroupingWord.value]: letter
  })
  selectedGroupingWord.value = ''
}

function removeGroupingItem(word: string) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value) {
    return
  }
  const next = { ...(groupingAnswers.value[question.id] ?? {}) }
  delete next[word]
  updateGroupingAnswer(question, next)
  selectedGroupingWord.value = word
}

function startGroupingDrag(item: GroupingItem) {
  draggedGroupingWord = item.word
}

function dropGroupingItem(letter: string) {
  const question = currentQuestion.value
  if (!question || currentAnswer.value || !draggedGroupingWord) {
    return
  }
  updateGroupingAnswer(question, {
    ...(groupingAnswers.value[question.id] ?? {}),
    [draggedGroupingWord]: letter
  })
  draggedGroupingWord = ''
  selectedGroupingWord.value = ''
}

function isGroupingComplete(question: QuestionResponse) {
  const assignments = groupingAnswers.value[question.id] ?? {}
  return groupingQuestionItems(question).every((item) => Boolean(assignments[item.word]))
}

async function goToQuestion(nextIndex: number) {
  if (checking.value || finishing.value) {
    return
  }
  if (!(await saveCurrentAnswerIfReady())) {
    return
  }
  if (nextIndex >= 0 && nextIndex < questions.value.length) {
    index.value = nextIndex
  }
}

async function previousQuestion() {
  await goToQuestion(index.value - 1)
}

async function nextQuestion() {
  await goToQuestion(index.value + 1)
}

async function handleEnter() {
  await submitAnswer()
}

function isInteractiveKeyboardTarget(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) {
    return false
  }
  const tagName = target.tagName.toLowerCase()
  return target.isContentEditable || ['input', 'textarea', 'select', 'button', 'a'].includes(tagName)
}

async function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey || isInteractiveKeyboardTarget(event.target)) {
    return
  }
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    await previousQuestion()
  } else if (event.key === 'ArrowRight') {
    event.preventDefault()
    await nextQuestion()
  } else if (event.key === 'Enter') {
    event.preventDefault()
    await submitAnswer()
  }
}

async function finishQuiz() {
  if (checking.value || finishing.value) {
    return
  }
  if (!(await saveCurrentAnswerIfReady())) {
    return
  }
  const unanswered = unansweredQuestionIndexes.value
  if (unanswered.length > 0) {
    const shouldFinish = window.confirm(`Има ${unanswered.length} задачи без отговор. Искаш ли да завършиш теста въпреки това?`)
    if (!shouldFinish) {
      index.value = unanswered[0]
      error.value = 'Продължи от първата задача без отговор.'
      return
    }
  }
  finishing.value = true
  try {
    if (activeStartedAt.value || shouldTrackActiveTime()) {
      await syncActiveTime(false)
    }
    if (currentQuestion.value && !currentAnswer.value) {
      draftAnswers.value = { ...draftAnswers.value, [currentQuestion.value.id]: typedAnswer.value }
    }
    for (const question of questions.value) {
      const answer = draftAnswers.value[question.id]
      if (isDraftAnswerComplete(question, answer) && !quiz.answerMap.has(question.id)) {
        await quiz.answer(question.id, answer)
      }
    }
    await quiz.finish()
    router.push({ name: 'result', params: { attemptId: attemptId.value } })
  } finally {
    finishing.value = false
  }
}

async function submitIssueReport() {
  if (!currentQuestion.value) {
    return
  }
  issueSubmitting.value = true
  issueError.value = ''
  issueSuccess.value = ''
  try {
    await api.post(`/quizzes/${attemptId.value}/questions/${currentQuestion.value.id}/issue-reports`, {
      message: issueMessage.value
    })
    issueSuccess.value = 'Докладът е изпратен.'
    issueMessage.value = ''
    issueFormOpen.value = false
  } catch (err) {
    issueError.value = err instanceof Error ? err.message : 'Докладът не беше изпратен.'
  } finally {
    issueSubmitting.value = false
  }
}
</script>

<template>
  <section class="page quiz-page">
    <div v-if="attempt && currentQuestion" class="quiz-layout">
      <section class="panel question-panel">
        <div class="quiz-meta">
          <span>{{ modeLabels[attempt.mode] }}</span>
          <span>Ниво {{ attempt.difficulty }}</span>
          <span>{{ difficultyCaption }}</span>
          <span class="time-pill"><Clock :size="16" /> {{ formatDuration(elapsedSeconds) }}</span>
        </div>

        <div class="progress-wrap" aria-label="Напредък">
          <div class="progress-line">
            <span :style="{ width: `${progressPercent}%` }"></span>
          </div>
          <strong>{{ progressLabel }}</strong>
        </div>

        <div v-if="hasMultipleQuestions" class="question-jump-list" aria-label="Навигация по задачи">
          <button
            v-for="(question, questionIndex) in questions"
            :key="question.id"
            type="button"
            :class="{ current: questionIndex === index, answered: questionCompletion[questionIndex] }"
            :aria-label="`Задача ${questionIndex + 1}${questionCompletion[questionIndex] ? ' отговорена' : ' без отговор'}`"
            @click="goToQuestion(questionIndex)"
          >
            {{ questionIndex + 1 }}
          </button>
        </div>

        <div class="question-body">
          <p v-if="hasMultipleQuestions" class="question-number">Задача {{ index + 1 }}</p>
          <template v-if="isFindObjectQuestion">
            <h1 class="find-object-title">
              <span>Намери</span>
              <span
                v-if="findObjectTarget"
                class="find-object-target"
                :aria-label="`Търси ${findObjectTarget.label}`"
                role="img"
              >
                {{ findObjectTarget.emoji }}
              </span>
              <span v-else>предмета</span>
            </h1>
            <p class="find-object-counter">
              <template v-if="currentAnswer">
                {{ currentAnswer.correct ? 'Откри го!' : 'Виж къде е верният предмет.' }}
              </template>
              <template v-else>
                Разгледай картинката и натисни предмета.
              </template>
            </p>
            <section class="find-object-scene" :class="findObjectScene.theme" :aria-label="findObjectScene.name || 'Стая'">
              <button
                v-for="item in findObjectScene.objects"
                :key="item.key"
                type="button"
                class="find-object-item"
                :class="findObjectClass(item)"
                :style="{ left: `${item.x}%`, top: `${item.y}%`, fontSize: `${item.size}px` }"
                :aria-label="`Предмет ${item.label}`"
                :disabled="Boolean(currentAnswer)"
                @click="chooseFindObject(item)"
              >
                <span aria-hidden="true">{{ item.emoji }}</span>
                <b v-if="findObjectMarker(item)">{{ findObjectMarker(item) }}</b>
              </button>
            </section>
          </template>

          <template v-else-if="isSpotDifferencesQuestion">
            <h1 class="spot-title">{{ currentQuestion.prompt }}</h1>
            <p class="spot-counter">
              <template v-if="currentAnswer">
                Правилни {{ selectedCorrectSpotDifferences }}/{{ spotScene.differenceIds.length }}
                <span v-if="selectedWrongSpotDifferences"> · грешни {{ selectedWrongSpotDifferences }}</span>
              </template>
              <template v-else>
                Маркирани {{ selectedSpotDifferences.length }} · търси {{ spotScene.differenceIds.length }}
              </template>
            </p>
            <div class="spot-game" :class="spotScene.theme">
              <section class="spot-picture" aria-label="Първа картинка">
                <span class="spot-label">1</span>
                <button
                  v-for="item in spotScene.left"
                  :key="item.key"
                  type="button"
                  class="spot-object"
                  :class="spotObjectClass(item)"
                  :style="{ left: `${item.x}%`, top: `${item.y}%`, fontSize: `${item.size}px` }"
                  :disabled="Boolean(currentAnswer)"
                  @click="toggleSpotDifference(item)"
                >
                  <span aria-hidden="true">{{ item.emoji }}</span>
                  <b v-if="spotMarker(item)">{{ spotMarker(item) }}</b>
                </button>
              </section>
              <section class="spot-picture" aria-label="Втора картинка">
                <span class="spot-label">2</span>
                <button
                  v-for="item in spotScene.right"
                  :key="item.key"
                  type="button"
                  class="spot-object"
                  :class="spotObjectClass(item)"
                  :style="{ left: `${item.x}%`, top: `${item.y}%`, fontSize: `${item.size}px` }"
                  :disabled="Boolean(currentAnswer)"
                  @click="toggleSpotDifference(item)"
                >
                  <span aria-hidden="true">{{ item.emoji }}</span>
                  <b v-if="spotMarker(item)">{{ spotMarker(item) }}</b>
                </button>
              </section>
            </div>
          </template>

          <template v-else-if="isMemoryPairsQuestion">
            <h1 class="memory-title">{{ currentQuestion.prompt }}</h1>
            <p v-if="!memoryIntroVisible" class="memory-counter">
              <template v-if="memoryPreviewActive">
                Запомни картите: {{ memoryPreviewSecondsLeft }} сек.
              </template>
              <template v-else-if="isMemorySolved(currentQuestion)">
                Всички двойки са открити · опити {{ memoryAttemptsCount }}
              </template>
              <template v-else>
                Открити {{ memoryMatched.length }}/{{ memoryGame.pairCount }} · опити {{ memoryAttemptsCount }} · {{ memoryRemainingMistakeText() }}
              </template>
            </p>
            <section v-if="memoryIntroVisible" class="memory-intro">
              <div>
                <strong>{{ memoryGame.previewSeconds }} сек.</strong>
                <span>за преглед на картите</span>
              </div>
              <div>
                <strong>{{ memoryMistakeText() }}</strong>
                <span>са допустими за максимален резултат</span>
              </div>
              <button class="button memory-start-button" type="button" @click="startMemoryPairsGame">
                <span>Започни прегледа</span>
              </button>
            </section>
            <section
              v-else
              class="memory-grid"
              :style="{ gridTemplateColumns: `repeat(${memoryGridColumns}, minmax(0, 1fr))` }"
              aria-label="Карти за памет"
            >
              <button
                v-for="card in memoryGame.cards"
                :key="card.key"
                type="button"
                class="memory-card"
                :class="memoryCardClass(card)"
                :disabled="Boolean(currentAnswer) || memoryPreviewActive || memoryLocked[currentQuestion.id] || memoryMatched.includes(card.pairId)"
                :aria-label="isMemoryCardOpen(card) ? `Карта ${card.label}` : 'Обърната карта'"
                @click="flipMemoryCard(card)"
              >
                <span v-if="isMemoryCardOpen(card)" class="memory-face">{{ card.emoji }}</span>
                <span v-else class="memory-back">?</span>
              </button>
            </section>
          </template>

          <template v-else-if="isPatternSequenceQuestion">
            <h1 class="memory-title">{{ currentQuestion.prompt }}</h1>
            <p v-if="!patternIntroVisible" class="memory-counter">
              <template v-if="patternPreviewActive">
                Запомни модела: {{ patternPreviewSecondsLeft }} сек.
              </template>
              <template v-else-if="currentAnswer">
                {{ currentAnswer.correct ? 'Моделът е подреден правилно.' : 'Виж правилната подредба.' }}
              </template>
              <template v-else>
                Подреди {{ patternGame.length }} фигури в същия ред.
              </template>
            </p>
            <section v-if="patternIntroVisible" class="memory-intro pattern-intro">
              <div>
                <strong>{{ patternGame.previewSeconds }} сек.</strong>
                <span>за преглед на модела</span>
              </div>
              <div>
                <strong>{{ patternGame.length }} фигури</strong>
                <span>трябва да се подредят в същия ред</span>
              </div>
              <button class="button memory-start-button" type="button" @click="startPatternSequenceGame">
                <span>Започни прегледа</span>
              </button>
            </section>
            <section v-else class="pattern-game" :class="{ answered: currentAnswer }">
              <div v-if="patternPreviewActive || currentAnswer" class="pattern-preview-row" aria-label="Модел за запомняне">
                <span
                  v-for="(token, tokenIndex) in patternGame.sequence"
                  :key="`${token.key}-preview-${tokenIndex}`"
                  class="pattern-token display"
                  :class="token.shape"
                  :style="patternTokenStyle(token)"
                  :aria-label="token.label"
                ></span>
              </div>

              <div v-if="!patternPreviewActive" class="pattern-slots" aria-label="Твоят модел">
                <button
                  v-for="(token, slotIndex) in patternSlots"
                  :key="`${currentQuestion.id}-pattern-slot-${slotIndex}`"
                  type="button"
                  class="pattern-slot"
                  :class="{ filled: Boolean(token), active: activePatternSlots[currentQuestion.id] === slotIndex && !currentAnswer }"
                  :disabled="Boolean(currentAnswer)"
                  @click="clearPatternSlot(slotIndex)"
                >
                  <span
                    v-if="token"
                    class="pattern-token display"
                    :class="token.shape"
                    :style="patternTokenStyle(token)"
                    :aria-label="token.label"
                  ></span>
                </button>
              </div>

              <div v-if="!patternPreviewActive && !currentAnswer" class="pattern-bank" aria-label="Фигури за избор">
                <button
                  v-for="token in patternGame.choices"
                  :key="token.key"
                  type="button"
                  class="pattern-choice"
                  :aria-label="token.label"
                  @click="choosePatternToken(token)"
                >
                  <span
                    class="pattern-token display"
                    :class="token.shape"
                    :style="patternTokenStyle(token)"
                    aria-hidden="true"
                  ></span>
                </button>
              </div>
            </section>
          </template>

          <template v-else-if="isSudokuQuestion">
            <h1 class="sudoku-title">{{ currentQuestion.prompt }}</h1>
            <p class="sudoku-counter">
              <template v-if="currentAnswer">
                {{ currentAnswer.correct ? 'Судокуто е решено правилно.' : 'Виж правилната подредба.' }}
              </template>
              <template v-else>
                Попълни празните клетки с числата от 1 до {{ sudokuGame.size }}.
              </template>
            </p>
            <section
              class="sudoku-game"
              :class="{ compact: sudokuGame.size === 4, answered: currentAnswer }"
              :style="{ '--sudoku-size': sudokuGame.size }"
              aria-label="Судоку дъска"
            >
              <div class="sudoku-grid">
                <button
                  v-for="cell in sudokuCells"
                  :key="cell.key"
                  type="button"
                  class="sudoku-cell"
                  :class="sudokuCellClass(cell)"
                  :style="sudokuCellStyle(cell)"
                  :disabled="Boolean(currentAnswer) || cell.given"
                  :aria-label="`Ред ${cell.row + 1}, колона ${cell.col + 1}`"
                  @click="selectSudokuCell(cell)"
                >
                  {{ currentAnswer && !cell.given ? (sudokuCorrectValue(cell.index) || cell.value) : cell.value }}
                </button>
              </div>
              <div v-if="!currentAnswer" class="sudoku-pad" aria-label="Числа за судоку">
                <button
                  v-for="choice in sudokuGame.choices"
                  :key="choice"
                  type="button"
                  @click="setSudokuValue(choice)"
                >
                  {{ choice }}
                </button>
                <button type="button" class="sudoku-clear" @click="clearSudokuCell">Изчисти</button>
              </div>
            </section>
          </template>

          <template v-else-if="isGroupingQuestion">
            <h1 class="word-title">{{ currentQuestion.prompt }}</h1>
            <div class="grouping-game">
              <div class="grouping-bank" aria-label="Картинки за групиране">
                <button
                  v-for="item in availableGroupingItems"
                  :key="item.key"
                  type="button"
                  class="grouping-card"
                  :class="{ selected: selectedGroupingWord === item.word }"
                  draggable="true"
                  :disabled="Boolean(currentAnswer)"
                  @dragstart="startGroupingDrag(item)"
                  @click="selectGroupingItem(item)"
                >
                  <PictureGlyph :image="item.image" aria-hidden="true" />
                  <b v-if="currentAnswer">{{ item.word }}</b>
                </button>
              </div>

              <div class="grouping-baskets">
                <section
                  v-for="letter in groupingLetters"
                  :key="letter"
                  class="grouping-basket"
                  :class="{ target: selectedGroupingWord && !currentAnswer }"
                  @dragover.prevent
                  @drop.prevent="dropGroupingItem(letter)"
                  @click="assignGroupingItem(letter)"
                >
                  <strong>{{ letter }}</strong>
                  <div>
                    <button
                      v-for="item in groupingItemsForLetter(letter)"
                      :key="item.key"
                      type="button"
                      class="grouping-card small"
                      :disabled="Boolean(currentAnswer)"
                      @click.stop="removeGroupingItem(item.word)"
                    >
                      <PictureGlyph :image="item.image" aria-hidden="true" />
                      <b v-if="currentAnswer">{{ item.word }}</b>
                    </button>
                  </div>
                </section>
              </div>
            </div>
          </template>

          <template v-else-if="isWordQuestion">
            <div class="word-card">
              <PictureGlyph class="word-image" :image="currentQuestion.image" aria-hidden="true" />
            </div>
            <template v-if="isWrongLetterQuestionCurrent">
              <p class="wrong-letter-question">{{ wrongLetterInstruction(currentQuestion) }}</p>
              <div
                class="wrong-letter-word"
                :class="{ answered: currentAnswer, correct: currentAnswer?.correct, wrong: currentAnswer && !currentAnswer.correct }"
                aria-label="Дума с една объркана буква"
              >
                <template v-for="part in wrongLetterParts(currentQuestion)" :key="part.key">
                  <span v-if="part.gap" class="wrong-letter-space" aria-hidden="true"></span>
                  <button
                    v-else
                    type="button"
                    :class="{ selected: selectedWrongLetter() === part.letter && !currentAnswer }"
                    :disabled="Boolean(currentAnswer)"
                    :aria-label="`Избери буква ${part.letter}`"
                    @click="chooseWrongLetter(part.letter)"
                  >
                    {{ part.letter }}
                  </button>
                </template>
              </div>
              <div v-if="selectedWrongLetter() && !currentAnswer" class="wrong-letter-replacements">
                <p>
                  <span>{{ selectedWrongLetter() }}</span>
                  <small aria-hidden="true">→</small>
                </p>
                <div>
                  <button
                    v-for="choice in wrongLetterReplacementChoices(currentQuestion)"
                    :key="choice"
                    type="button"
                    :class="{ selected: selectedReplacementLetter() === choice }"
                    @click="chooseReplacementLetter(choice)"
                  >
                    {{ choice }}
                  </button>
                </div>
              </div>
            </template>
            <template v-else>
              <h1
                class="word-title"
                :class="{ answered: currentAnswer && isMissingLetterQuestion(currentQuestion), correct: currentAnswer?.correct, wrong: currentAnswer && !currentAnswer.correct }"
              >
                {{ wordPromptText(currentQuestion) }}
              </h1>

              <div v-if="currentAnswer && !isMissingLetterQuestion(currentQuestion)" class="word-answer" :class="{ correct: currentAnswer.correct, wrong: !currentAnswer.correct }">
                {{ currentAnswer.correct ? currentAnswer.correctAnswer : currentAnswer.correctAnswer }}
              </div>

              <div v-else-if="isBuilderQuestion" class="word-builder">
                <div class="drop-zone" @dragover.prevent @drop.prevent="dropToken">
                  <template v-for="slot in builderLayout" :key="slot.key">
                    <span v-if="slot.type === 'gap'" class="word-gap" aria-hidden="true"></span>
                    <button
                      v-else
                      type="button"
                      class="builder-slot"
                      :class="{ filled: Boolean(slot.token), empty: !slot.token, active: slot.active }"
                      :style="{ width: `${builderSlotWidth(slot.expected)}px` }"
                      :aria-label="slot.token ? `Махни ${slot.token.text}` : 'Празно място за буква или сричка'"
                      @click="handleBuilderSlotClick(slot.slotIndex)"
                    >
                      <span v-if="slot.token">{{ slot.token.text }}</span>
                    </button>
                  </template>
                </div>
                <div class="letter-bank">
                  <button
                    v-for="token in availableTokens"
                    :key="token.id"
                    type="button"
                    class="letter-token"
                    draggable="true"
                    @dragstart="startDrag(token)"
                    @click="placeToken(token)"
                  >
                    {{ token.text }}
                  </button>
                </div>
              </div>

              <label v-else class="field answer-field word-input">
                <span>{{ answerLabel }}</span>
                <input
                  v-model="typedAnswer"
                  inputmode="text"
                  autocomplete="off"
                  :readonly="Boolean(currentAnswer)"
                  @keyup.enter="handleEnter"
                />
              </label>
            </template>
          </template>

          <template v-else>
            <h1 class="prompt-line">
              <template v-if="currentAnswer">
                <span>{{ promptParts[0] }}</span>
                <span class="answer-token" :class="{ correct: currentAnswer.correct, wrong: !currentAnswer.correct }">
                  {{ currentAnswer.correct ? currentAnswer.answer : currentAnswer.correctAnswer }}
                </span>
                <span>{{ promptParts.slice(1).join('?') }}</span>
              </template>
              <template v-else>
                {{ currentQuestion.prompt }}
              </template>
            </h1>

            <div v-if="currentQuestion.kind === 'CHOICE'" class="choice-row" :class="{ picture: isPictureChoiceQuestion }">
              <button
                v-for="choice in currentQuestion.choices"
                :key="choice"
                type="button"
                :class="{ selected: typedAnswer === choice }"
                :aria-label="isPictureChoiceQuestion ? `Избери картинка ${choice}` : choice"
                :disabled="Boolean(currentAnswer)"
                @click="choose(choice)"
              >
                <PictureGlyph v-if="isPictureChoiceQuestion" :image="choice" aria-hidden="true" />
                <template v-else>{{ choice }}</template>
              </button>
            </div>

            <label v-else class="field answer-field">
              <span>{{ answerLabel }}</span>
              <input
                v-model="typedAnswer"
                :inputmode="answerInputMode"
                autocomplete="off"
                :readonly="Boolean(currentAnswer)"
                @keyup.enter="handleEnter"
              />
            </label>
          </template>

          <div v-if="error" class="error">{{ error }}</div>
        </div>

        <div class="quiz-actions">
          <button
            class="button"
            type="button"
            :disabled="checking || Boolean(currentAnswer) || (isMemoryPairsQuestion && !isMemorySolved(currentQuestion)) || patternIntroVisible || patternPreviewActive"
            @click="submitAnswer"
          >
            <Check :size="20" />
            <span>{{ primaryActionLabel }}</span>
          </button>
          <button
            class="button secondary"
            type="button"
            :disabled="checking || finishing || index <= 0"
            @click="previousQuestion"
          >
            <ArrowLeft :size="20" />
            <span>Предишна</span>
          </button>
          <button
            class="button secondary"
            type="button"
            :disabled="checking || finishing || index >= questions.length - 1"
            @click="nextQuestion"
          >
            <ArrowRight :size="20" />
            <span>Следваща</span>
          </button>
          <button class="button coral" type="button" :disabled="finishing || (isPatternSequenceQuestion && patternPreviewActive)" @click="finishQuiz">
            <Flag :size="20" />
            <span>Край на теста</span>
          </button>
          <button class="button secondary report-button" type="button" @click="issueFormOpen = !issueFormOpen">
            <AlertTriangle :size="20" />
            <span>Докладвай нередност</span>
          </button>
        </div>

        <form v-if="issueFormOpen" class="issue-form" @submit.prevent="submitIssueReport">
          <label class="field">
            <span>Какво не е наред?</span>
            <textarea
              v-model="issueMessage"
              maxlength="1000"
              placeholder="Например: картинката не отговаря на думата или верният отговор изглежда грешен."
            ></textarea>
          </label>
          <div class="issue-actions">
            <button class="button" type="submit" :disabled="issueSubmitting">
              <Check :size="18" />
              <span>Изпрати</span>
            </button>
            <button class="button secondary" type="button" @click="issueFormOpen = false">
              Откажи
            </button>
          </div>
        </form>
        <p v-if="issueSuccess" class="success-note">{{ issueSuccess }}</p>
        <p v-if="issueError" class="error">{{ issueError }}</p>
      </section>

      <aside class="feedback-panel panel" :class="{ correct: currentAnswer?.correct, wrong: currentAnswer && !currentAnswer.correct }">
        <div v-if="currentAnswer?.correct" class="fireworks" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
        </div>
        <MascotFigure :mood="mascotMood" />
        <template v-if="currentAnswer">
          <h2>{{ currentAnswer.correct ? (isMemoryPairsQuestion ? 'Готово!' : 'Правилно!') : 'Почти!' }}</h2>
          <p v-if="currentAnswer.correct">
            <template v-if="isMemoryPairsQuestion">
              Тестът е успешно завършен с {{ memoryAttemptsFromAnswer(currentAnswer.answer) }} опита.
            </template>
            <template v-else-if="isPatternSequenceQuestion">
              Моделът е подреден правилно.
            </template>
            <template v-else>
              Точката е записана.
            </template>
          </p>
          <p v-else>
            {{ wrongFeedbackIntro(currentQuestion) }}
            <strong>{{ feedbackAnswerText(currentQuestion, currentAnswer.correctAnswer) }}</strong>
          </p>
        </template>
        <template v-else>
          <h2>Помисли спокойно</h2>
          <p>Можеш да провериш сега или да продължиш напред и да се отчете накрая.</p>
        </template>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.quiz-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: stretch;
}

.question-panel {
  display: grid;
  gap: 22px;
  padding: 22px;
}

.quiz-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quiz-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  padding: 8px 12px;
  color: var(--muted);
  background: #ffffff;
  font-weight: 800;
}

.time-pill {
  color: var(--blue) !important;
}

.progress-wrap {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.progress-line {
  height: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(36, 48, 74, 0.1);
}

.progress-line span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--green), var(--amber));
  transition: width 220ms ease;
}

.question-jump-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(34px, 1fr));
  gap: 6px;
}

.question-jump-list button {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid rgba(36, 48, 74, 0.14);
  border-radius: 8px;
  color: var(--muted);
  background: #ffffff;
  font-weight: 900;
}

.question-jump-list button.answered {
  color: #ffffff;
  background: var(--green);
}

.question-jump-list button.current {
  border-color: var(--blue);
  box-shadow: 0 0 0 3px rgba(63, 125, 217, 0.18);
}

.question-body {
  display: grid;
  gap: 18px;
  min-height: 320px;
  align-content: center;
  justify-items: center;
  text-align: center;
}

.question-number {
  margin: 0;
  color: var(--green-dark);
  font-weight: 900;
}

h1 {
  margin: 0;
  font-size: clamp(3rem, 9vw, 6.3rem);
  line-height: 1;
}

.prompt-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.18em;
}

.answer-token {
  display: inline-grid;
  min-width: 1.2em;
  place-items: center;
  border-radius: var(--radius);
  padding: 0.02em 0.14em;
  color: #ffffff;
}

.answer-token.correct {
  background: var(--green);
}

.answer-token.wrong {
  background: var(--danger);
}

.find-object-title {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 18px;
  font-size: clamp(2rem, 5vw, 3.2rem);
}

.find-object-target {
  display: inline-grid;
  width: 1.55em;
  height: 1.55em;
  place-items: center;
  border: 4px solid rgba(30, 157, 116, 0.34);
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 12px 26px rgba(30, 157, 116, 0.18);
  font-size: 0.95em;
  line-height: 1;
}

.find-object-counter {
  margin: -8px 0 0;
  color: var(--muted);
  font-size: 1.05rem;
  font-weight: 900;
}

.find-object-scene {
  position: relative;
  overflow: hidden;
  width: min(760px, 100%);
  min-height: clamp(340px, 48vw, 520px);
  border: 2px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  background:
    linear-gradient(180deg, rgba(255, 247, 224, 0.95) 0 66%, rgba(231, 205, 176, 0.96) 66% 100%),
    repeating-linear-gradient(90deg, rgba(36, 48, 74, 0.06) 0 1px, transparent 1px 58px);
  box-shadow: 0 16px 34px rgba(36, 48, 74, 0.1);
}

.find-object-scene::before {
  content: '';
  position: absolute;
  left: 8%;
  right: 8%;
  bottom: 28%;
  height: 9px;
  border-radius: 999px;
  background: rgba(160, 116, 78, 0.28);
}

.find-object-scene::after {
  content: '';
  position: absolute;
  top: 9%;
  left: 42%;
  width: 20%;
  height: 18%;
  border: 4px solid rgba(63, 125, 217, 0.18);
  border-radius: var(--radius);
  background: linear-gradient(135deg, rgba(223, 246, 255, 0.92), rgba(255, 255, 255, 0.78));
}

.find-object-scene.park {
  background:
    linear-gradient(180deg, rgba(201, 237, 255, 0.95) 0 42%, rgba(192, 232, 157, 0.96) 42% 100%),
    repeating-linear-gradient(90deg, rgba(36, 48, 74, 0.05) 0 1px, transparent 1px 64px);
}

.find-object-scene.park::before {
  bottom: 18%;
  background: rgba(70, 142, 78, 0.25);
}

.find-object-scene.park::after {
  border-color: rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.58);
}

.find-object-scene.beach {
  background:
    linear-gradient(180deg, rgba(190, 235, 255, 0.95) 0 38%, rgba(114, 205, 229, 0.85) 38% 56%, rgba(250, 221, 158, 0.96) 56% 100%),
    repeating-linear-gradient(90deg, rgba(36, 48, 74, 0.04) 0 1px, transparent 1px 62px);
}

.find-object-scene.beach::before {
  bottom: 41%;
  background: rgba(255, 255, 255, 0.52);
}

.find-object-scene.beach::after {
  display: none;
}

.find-object-item {
  position: absolute;
  z-index: 2;
  display: grid;
  width: 1.24em;
  height: 1.24em;
  place-items: center;
  border: 3px solid transparent;
  border-radius: 50%;
  padding: 0;
  background: transparent;
  line-height: 1;
  transform: translate(-50%, -50%);
}

.find-object-item:hover:not(:disabled),
.find-object-item.selected {
  border-color: var(--blue);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 0 0 6px rgba(63, 125, 217, 0.16);
}

.find-object-item.correct {
  border-color: var(--green);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 0 0 6px rgba(30, 157, 116, 0.18);
}

.find-object-item.wrong {
  border-color: var(--danger);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 0 0 6px rgba(217, 75, 75, 0.16);
}

.find-object-item:disabled {
  cursor: default;
}

.find-object-item b {
  position: absolute;
  right: -8px;
  bottom: -8px;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--green);
  font-size: 0.85rem;
  font-weight: 950;
}

.find-object-item.wrong b {
  background: var(--danger);
}

.spot-title {
  font-size: clamp(1.9rem, 5vw, 3.1rem);
}

.spot-counter {
  margin: -8px 0 0;
  color: var(--muted);
  font-size: 1.05rem;
  font-weight: 900;
}

.spot-game {
  display: grid;
  width: min(980px, 100%);
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.spot-picture {
  position: relative;
  overflow: hidden;
  min-height: clamp(260px, 35vw, 390px);
  border: 2px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  background:
    linear-gradient(180deg, #dff4ff 0 52%, #ddf6d2 52% 100%);
  box-shadow: 0 12px 28px rgba(36, 48, 74, 0.08);
}

.spot-game.beach .spot-picture {
  background:
    linear-gradient(180deg, #dff6ff 0 46%, #bdeaff 46% 64%, #ffeab4 64% 100%);
}

.spot-game.room .spot-picture {
  background:
    linear-gradient(180deg, #fff4d8 0 68%, #f0d5ba 68% 100%);
}

.spot-game.garden .spot-picture {
  background:
    linear-gradient(180deg, #def4ff 0 50%, #ccefbf 50% 100%);
}

.spot-label {
  position: absolute;
  z-index: 2;
  top: 10px;
  left: 10px;
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--blue);
  font-weight: 950;
}

.spot-object {
  position: absolute;
  display: grid;
  width: 1.2em;
  height: 1.2em;
  place-items: center;
  border: 3px solid transparent;
  border-radius: 50%;
  padding: 0;
  background: transparent;
  line-height: 1;
  transform: translate(-50%, -50%);
}

.spot-object:hover:not(:disabled),
.spot-object.selected {
  border-color: var(--blue);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 0 0 5px rgba(63, 125, 217, 0.16);
}

.spot-object.correct {
  border-color: var(--green);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 0 0 5px rgba(30, 157, 116, 0.16);
}

.spot-object.wrong {
  border-color: var(--danger);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 0 0 5px rgba(217, 75, 75, 0.13);
}

.spot-object.missed {
  border-color: var(--amber);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 0 0 5px rgba(245, 185, 66, 0.16);
}

.spot-object:disabled {
  cursor: default;
}

.spot-object b {
  position: absolute;
  right: -8px;
  bottom: -8px;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--green);
  font-size: 0.85rem;
  font-weight: 950;
}

.spot-object.missed b {
  background: var(--amber);
}

.spot-object.wrong b {
  background: var(--danger);
}

.memory-title {
  font-size: clamp(2rem, 5vw, 3.3rem);
}

.memory-counter {
  margin: -8px 0 0;
  color: var(--muted);
  font-size: 1.08rem;
  font-weight: 900;
}

.memory-intro {
  display: grid;
  width: min(620px, 100%);
  gap: 14px;
  justify-items: center;
  border: 2px solid rgba(30, 157, 116, 0.16);
  border-radius: var(--radius);
  padding: 22px;
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 255, 255, 0.94), transparent 30%),
    linear-gradient(135deg, rgba(30, 157, 116, 0.1), rgba(245, 185, 66, 0.12));
  box-shadow: 0 12px 26px rgba(36, 48, 74, 0.08);
}

.memory-intro > div {
  display: grid;
  width: min(280px, 100%);
  gap: 4px;
  justify-items: center;
  border-radius: var(--radius);
  padding: 14px;
  background: #ffffff;
}

.memory-intro strong {
  color: var(--ink);
  font-size: clamp(1.8rem, 5vw, 2.6rem);
  line-height: 1;
}

.memory-intro > div span {
  color: var(--muted);
  font-weight: 850;
}

.memory-start-button {
  min-width: 260px;
  color: #ffffff;
}

.memory-start-button span {
  color: inherit;
  font-weight: 950;
}

.memory-grid {
  display: grid;
  width: min(760px, 100%);
  gap: 12px;
}

.memory-card {
  display: grid;
  min-height: clamp(82px, 9vw, 116px);
  place-items: center;
  border: 2px solid rgba(36, 48, 74, 0.13);
  border-radius: var(--radius);
  color: var(--ink);
  background:
    linear-gradient(135deg, rgba(63, 125, 217, 0.1), rgba(30, 157, 116, 0.1)),
    #ffffff;
  box-shadow: 0 8px 18px rgba(36, 48, 74, 0.08);
  transition: transform 160ms ease, border-color 160ms ease, background 160ms ease;
}

.memory-card:hover:not(:disabled) {
  border-color: var(--blue);
  transform: translateY(-2px);
}

.memory-card.open {
  border-color: rgba(63, 125, 217, 0.28);
  background: #ffffff;
}

.memory-card.matched {
  border-color: var(--green);
  background: rgba(30, 157, 116, 0.1);
  box-shadow: 0 0 0 4px rgba(30, 157, 116, 0.12);
}

.memory-card:disabled {
  cursor: default;
}

.memory-face,
.memory-back {
  display: grid;
  place-items: center;
  line-height: 1;
}

.memory-face {
  font-size: clamp(2.7rem, 7vw, 4.4rem);
}

.memory-back {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  color: #ffffff;
  background: var(--blue);
  font-size: 2rem;
  font-weight: 950;
}

.pattern-game {
  display: grid;
  width: min(820px, 100%);
  gap: 18px;
  justify-items: center;
}

.pattern-preview-row,
.pattern-slots,
.pattern-bank {
  display: flex;
  width: 100%;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.pattern-preview-row {
  border: 2px dashed rgba(30, 157, 116, 0.22);
  border-radius: var(--radius);
  padding: 18px;
  background: rgba(30, 157, 116, 0.07);
}

.pattern-slots {
  border-radius: var(--radius);
  padding: 14px;
  background: rgba(255, 255, 255, 0.55);
}

.pattern-slot,
.pattern-choice {
  display: grid;
  width: clamp(58px, 8vw, 76px);
  height: clamp(58px, 8vw, 76px);
  place-items: center;
  border-radius: var(--radius);
  background: #ffffff;
}

.pattern-slot {
  border: 2px dashed rgba(36, 48, 74, 0.18);
}

.pattern-slot.filled {
  border-style: solid;
  border-color: rgba(30, 157, 116, 0.28);
}

.pattern-slot.active {
  border-color: var(--blue);
  box-shadow: 0 0 0 4px rgba(63, 125, 217, 0.12);
}

.pattern-choice {
  border: 2px solid rgba(36, 48, 74, 0.13);
  box-shadow: 0 8px 18px rgba(36, 48, 74, 0.08);
  transition: transform 160ms ease, border-color 160ms ease;
}

.pattern-choice:hover {
  border-color: var(--blue);
  transform: translateY(-2px);
}

.sudoku-title {
  font-size: clamp(2.1rem, 6vw, 3.6rem);
}

.sudoku-counter {
  margin: -8px 0 0;
  color: var(--muted);
  font-size: 1.05rem;
  font-weight: 900;
}

.sudoku-game {
  display: grid;
  width: min(720px, 100%);
  gap: 16px;
  justify-items: center;
}

.sudoku-grid {
  display: grid;
  inline-size: min(100%, 560px);
  max-inline-size: 100%;
  aspect-ratio: 1;
  grid-template-columns: repeat(var(--sudoku-size), minmax(0, 1fr));
  overflow: hidden;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 16px 34px rgba(36, 48, 74, 0.12);
}

.sudoku-game.compact .sudoku-grid {
  inline-size: min(100%, 420px);
}

.sudoku-cell {
  display: grid;
  min-width: 0;
  min-height: 0;
  place-items: center;
  border-color: rgba(36, 48, 74, 0.5);
  border-style: solid;
  color: var(--ink);
  background: #ffffff;
  font-size: clamp(1rem, 3.8vw, 2.1rem);
  font-weight: 950;
  line-height: 1;
  transition: background 150ms ease, box-shadow 150ms ease, color 150ms ease;
}

.sudoku-game.compact .sudoku-cell {
  font-size: clamp(1.8rem, 8vw, 3.2rem);
}

.sudoku-cell.given {
  background: rgba(63, 125, 217, 0.09);
  color: var(--blue);
}

.sudoku-cell.active {
  background: rgba(245, 185, 66, 0.2);
  box-shadow: inset 0 0 0 4px rgba(245, 185, 66, 0.38);
}

.sudoku-cell.correct {
  color: var(--green-dark);
  background: rgba(30, 157, 116, 0.1);
}

.sudoku-cell.wrong {
  color: var(--danger);
  background: rgba(238, 92, 92, 0.1);
}

.sudoku-pad {
  display: flex;
  max-width: min(100%, 560px);
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.sudoku-pad button {
  display: grid;
  min-width: 48px;
  min-height: 48px;
  place-items: center;
  border: 2px solid rgba(36, 48, 74, 0.13);
  border-radius: 8px;
  color: var(--ink);
  background: #ffffff;
  font-size: 1.1rem;
  font-weight: 950;
}

.sudoku-pad button:hover {
  border-color: var(--blue);
}

.sudoku-pad .sudoku-clear {
  min-width: 96px;
  color: var(--muted);
  font-size: 0.98rem;
}

.pattern-token {
  --pattern-color: var(--green);
  display: block;
  width: clamp(34px, 5vw, 48px);
  height: clamp(34px, 5vw, 48px);
  background: var(--pattern-color);
  box-shadow: inset 0 -5px 0 rgba(0, 0, 0, 0.12), 0 6px 14px rgba(36, 48, 74, 0.12);
}

.pattern-token.circle {
  border-radius: 50%;
}

.pattern-token.square {
  border-radius: 10px;
}

.pattern-token.triangle {
  width: 0;
  height: 0;
  border-right: clamp(21px, 3vw, 30px) solid transparent;
  border-bottom: clamp(38px, 5.4vw, 54px) solid var(--pattern-color);
  border-left: clamp(21px, 3vw, 30px) solid transparent;
  background: transparent;
  box-shadow: 0 6px 14px rgba(36, 48, 74, 0.12);
}

.pattern-token.diamond {
  border-radius: 8px;
  transform: rotate(45deg) scale(0.86);
}

.word-card {
  position: relative;
  display: grid;
  width: min(250px, 72vw);
  aspect-ratio: 1.18;
  place-items: center;
  border: 2px solid rgba(63, 125, 217, 0.16);
  border-radius: var(--radius);
  background:
    radial-gradient(circle at 24% 22%, rgba(255, 255, 255, 0.95), transparent 28%),
    linear-gradient(135deg, #f4fbff, #fff7e3);
  box-shadow: 0 16px 34px rgba(36, 48, 74, 0.1);
}

.word-image {
  font-size: clamp(5.4rem, 16vw, 8.2rem);
  line-height: 1;
}

.word-title {
  font-size: clamp(1.7rem, 5vw, 2.7rem);
}

.wrong-letter-question {
  margin: 0;
  color: var(--muted);
  font-size: clamp(1.1rem, 3vw, 1.45rem);
  font-weight: 900;
}

.wrong-letter-word {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  border-radius: var(--radius);
  padding: 8px 12px 12px;
}

.wrong-letter-word button {
  display: grid;
  min-width: 54px;
  min-height: 62px;
  place-items: center;
  border: 0;
  border-bottom: 5px solid rgba(36, 48, 74, 0.24);
  border-radius: 0;
  color: var(--ink);
  background: #ffffff;
  font-size: clamp(2rem, 7vw, 3.5rem);
  font-weight: 950;
  line-height: 1;
}

.wrong-letter-word button:hover,
.wrong-letter-word button.selected {
  color: #ffffff;
  border-bottom-color: var(--green-dark);
  background: var(--green);
}

.wrong-letter-word.answered button {
  color: #ffffff;
  pointer-events: none;
}

.wrong-letter-word.answered.correct button {
  border-bottom-color: var(--green-dark);
  background: var(--green);
}

.wrong-letter-word.answered.wrong button {
  border-bottom-color: #d94940;
  background: var(--danger);
}

.wrong-letter-space {
  width: 22px;
  min-height: 62px;
}

.wrong-letter-replacements {
  display: grid;
  gap: 10px;
  justify-items: center;
}

.wrong-letter-replacements p {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: var(--muted);
  font-weight: 950;
}

.wrong-letter-replacements p span {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: var(--radius);
  color: #ffffff;
  background: var(--danger);
  font-size: 1.4rem;
}

.wrong-letter-replacements div {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.wrong-letter-replacements button {
  display: grid;
  min-width: 62px;
  min-height: 62px;
  place-items: center;
  border: 2px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  color: var(--ink);
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(36, 48, 74, 0.08);
  font-size: clamp(1.65rem, 5vw, 2.25rem);
  font-weight: 950;
}

.wrong-letter-replacements button:hover,
.wrong-letter-replacements button.selected {
  color: #ffffff;
  border-color: var(--green-dark);
  background: var(--green);
}

.word-title.answered {
  border-radius: var(--radius);
  padding: 4px 14px 8px;
}

.word-title.answered.correct {
  color: var(--green-dark);
  background: rgba(33, 160, 122, 0.1);
}

.word-title.answered.wrong {
  color: var(--danger);
  background: rgba(248, 111, 100, 0.1);
}

.word-builder {
  display: grid;
  width: min(720px, 100%);
  gap: 16px;
}

.drop-zone,
.letter-bank {
  display: flex;
  min-height: 74px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: var(--radius);
}

.drop-zone {
  column-gap: 12px;
  row-gap: 18px;
  padding: 10px 12px 14px;
  background: rgba(255, 255, 255, 0.48);
}

.letter-bank {
  padding: 4px;
}

.letter-token,
.builder-slot {
  display: grid;
  min-width: 58px;
  min-height: 58px;
  place-items: center;
}

.letter-token {
  border-radius: var(--radius);
  border: 2px solid rgba(36, 48, 74, 0.12);
  padding: 0 13px;
  color: var(--ink);
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(36, 48, 74, 0.08);
  font-size: clamp(1.45rem, 5vw, 2.15rem);
  font-weight: 950;
}

.letter-token:hover {
  border-color: var(--blue);
  transform: translateY(-1px);
}

.builder-slot {
  border: 0;
  border-bottom: 5px solid rgba(36, 48, 74, 0.22);
  border-radius: 0;
  color: var(--green-dark);
  background: transparent;
  font-size: clamp(1.55rem, 5vw, 2.25rem);
  font-weight: 950;
  line-height: 1;
  white-space: nowrap;
}

.builder-slot:hover,
.builder-slot.active {
  border-bottom-color: var(--blue);
  background: rgba(63, 125, 217, 0.08);
}

.builder-slot.filled {
  border-bottom-color: var(--green);
}

.word-gap {
  width: 26px;
  min-height: 58px;
}

.word-answer {
  border-radius: var(--radius);
  padding: 14px 20px;
  color: #ffffff;
  font-size: clamp(2rem, 7vw, 4rem);
  font-weight: 950;
  line-height: 1;
}

.word-answer.correct {
  background: var(--green);
}

.word-answer.wrong {
  background: var(--danger);
}

.answer-field {
  width: min(320px, 100%);
  text-align: left;
}

.answer-field input {
  height: 64px;
  font-size: 2rem;
  font-weight: 900;
  text-align: center;
}

.word-input {
  width: min(520px, 100%);
}

.word-input input {
  text-transform: uppercase;
}

.choice-row {
  display: grid;
  grid-template-columns: repeat(3, 88px);
  gap: 12px;
  justify-content: center;
}

.choice-row button {
  aspect-ratio: 1;
  border: 2px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  color: var(--ink);
  background: #ffffff;
  font-size: 2.4rem;
  font-weight: 950;
}

.choice-row button.selected {
  border-color: var(--blue);
  color: #ffffff;
  background: var(--blue);
}

.choice-row.picture {
  grid-template-columns: repeat(3, minmax(108px, 132px));
  gap: 16px;
}

.choice-row.picture button {
  font-size: clamp(3.3rem, 10vw, 5rem);
  line-height: 1;
}

.choice-row button:disabled {
  cursor: not-allowed;
}

.grouping-game {
  display: grid;
  width: min(820px, 100%);
  gap: 18px;
}

.grouping-bank,
.grouping-baskets {
  display: grid;
  gap: 12px;
}

.grouping-bank {
  grid-template-columns: repeat(3, minmax(120px, 1fr));
}

.grouping-baskets {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.grouping-card {
  display: grid;
  min-height: 112px;
  place-items: center;
  gap: 4px;
  border: 2px solid rgba(36, 48, 74, 0.14);
  border-radius: var(--radius);
  padding: 10px;
  color: var(--ink);
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(36, 48, 74, 0.07);
}

.grouping-card span {
  font-size: clamp(2.8rem, 8vw, 4.1rem);
  line-height: 1;
}

.grouping-card b {
  font-size: 1rem;
}

.grouping-card.selected {
  border-color: var(--blue);
  background: rgba(63, 125, 217, 0.1);
}

.grouping-card.small {
  min-height: 82px;
  box-shadow: none;
}

.grouping-card.small span {
  font-size: clamp(2.2rem, 6vw, 3rem);
}

.grouping-basket {
  display: grid;
  min-height: 210px;
  grid-template-rows: auto 1fr;
  gap: 10px;
  border: 2px dashed rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 12px;
  background: rgba(255, 255, 255, 0.65);
}

.grouping-basket.target {
  border-color: var(--blue);
  background: rgba(63, 125, 217, 0.08);
}

.grouping-basket > strong {
  display: grid;
  width: 58px;
  height: 58px;
  margin: 0 auto;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--green);
  font-size: 2rem;
}

.grouping-basket > div {
  display: grid;
  gap: 8px;
  align-content: start;
}

.quiz-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.report-button {
  color: var(--danger);
}

.issue-form {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(217, 75, 75, 0.22);
  border-radius: var(--radius);
  background: rgba(217, 75, 75, 0.06);
}

.issue-form textarea {
  min-height: 92px;
  resize: vertical;
  border: 1px solid rgba(36, 48, 74, 0.16);
  border-radius: var(--radius);
  padding: 12px;
  color: var(--ink);
  background: #ffffff;
  font: inherit;
}

.issue-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.success-note {
  margin: 0;
  color: var(--green-dark);
  font-weight: 900;
}

.feedback-panel {
  position: relative;
  overflow: hidden;
  display: grid;
  min-height: 300px;
  align-content: center;
  justify-items: center;
  gap: 12px;
  padding: 22px;
  text-align: center;
}

.feedback-panel h2,
.feedback-panel p {
  margin: 0;
}

.feedback-panel p {
  color: var(--muted);
  font-size: 1.05rem;
  font-weight: 700;
}

.feedback-panel strong {
  display: block;
  color: var(--ink);
  white-space: pre-line;
  font-size: clamp(1.1rem, 2.2vw, 1.45rem);
  line-height: 1.25;
}

.fireworks {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.fireworks span {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: pop 850ms ease-out infinite;
}

.fireworks span:nth-child(1) {
  top: 18%;
  left: 18%;
  background: var(--amber);
}

.fireworks span:nth-child(2) {
  top: 20%;
  right: 20%;
  background: var(--green);
  animation-delay: 120ms;
}

.fireworks span:nth-child(3) {
  right: 28%;
  bottom: 22%;
  background: var(--blue);
  animation-delay: 240ms;
}

.fireworks span:nth-child(4) {
  bottom: 24%;
  left: 24%;
  background: var(--coral);
  animation-delay: 360ms;
}

@keyframes pop {
  0% {
    opacity: 0;
    transform: scale(0.4);
  }
  38% {
    opacity: 1;
    transform: scale(1.8);
  }
  100% {
    opacity: 0;
    transform: scale(3.4);
  }
}

@media (max-width: 860px) {
  .quiz-layout {
    grid-template-columns: 1fr;
  }

  .feedback-panel {
    min-height: 220px;
  }
}

@media (max-width: 520px) {
  .question-panel {
    padding: 16px;
  }

  h1 {
    font-size: clamp(2.6rem, 16vw, 4.5rem);
  }

  .choice-row {
    grid-template-columns: repeat(3, minmax(64px, 82px));
  }

  .choice-row.picture {
    grid-template-columns: repeat(3, minmax(82px, 1fr));
  }

  .grouping-bank,
  .grouping-baskets,
  .spot-game {
    grid-template-columns: 1fr;
  }

  .grouping-basket {
    min-height: 160px;
  }

  .letter-token,
  .builder-slot {
    min-width: 48px;
    min-height: 50px;
  }

  .word-gap {
    width: 18px;
    min-height: 50px;
  }

  .quiz-actions {
    display: grid;
  }
}
</style>
