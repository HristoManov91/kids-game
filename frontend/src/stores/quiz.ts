import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '@/services/api'
import type {
  CrystalLeaderboardRow,
  CrystalTotalResponse,
  LeaderboardRow,
  QuestionResultResponse,
  QuizAttemptResponse,
  QuizCategory,
  QuizMode,
  QuizSummaryResponse,
  QuizTimeResponse,
  RecentAttemptRow
} from '@/types'

export const useQuizStore = defineStore('quiz', () => {
  const selectedCategory = ref<QuizCategory>('MATH')
  const testPlan = ref<'SINGLE' | 'CUSTOM' | 'ALL'>('SINGLE')
  const selectedMode = ref<QuizMode>('ADDITION')
  const selectedIncludedModes = ref<QuizMode[]>(['ADDITION', 'SUBTRACTION'])
  const difficulty = ref(1)
  const currentAttempt = ref<QuizAttemptResponse | null>(null)
  const summary = ref<QuizSummaryResponse | null>(null)
  const leaderboard = ref<LeaderboardRow[]>([])
  const recentAttempts = ref<RecentAttemptRow[]>([])
  const crystalLeaderboard = ref<CrystalLeaderboardRow[]>([])
  const totalCrystals = ref(0)
  const history = ref<QuizAttemptResponse[]>([])
  const activeAttempts = ref<QuizAttemptResponse[]>([])
  const loading = ref(false)

  const answerMap = computed(() => {
    const map = new Map<number, QuestionResultResponse>()
    currentAttempt.value?.answers.forEach((answer) => map.set(answer.questionId, answer))
    return map
  })

  function selectedModeForQuery() {
    if (testPlan.value === 'CUSTOM') {
      return 'CUSTOM_GROUP'
    }
    if (testPlan.value === 'ALL') {
      return 'ALL_GROUP'
    }
    return selectedMode.value
  }

  async function start() {
    loading.value = true
    try {
      summary.value = null
      const mode = selectedModeForQuery()
      currentAttempt.value = await api.post<QuizAttemptResponse>('/quizzes/start', {
        category: selectedCategory.value,
        mode,
        difficulty: difficulty.value,
        includedModes: testPlan.value === 'CUSTOM' ? selectedIncludedModes.value : []
      })
      return currentAttempt.value
    } finally {
      loading.value = false
    }
  }

  async function fetchAttempt(attemptId: string) {
    currentAttempt.value = await api.get<QuizAttemptResponse>(`/quizzes/${attemptId}`)
    return currentAttempt.value
  }

  async function answer(questionId: number, answerValue: string) {
    if (!currentAttempt.value) {
      throw new Error('Няма активен тест.')
    }
    const result = await api.post<QuestionResultResponse>(`/quizzes/${currentAttempt.value.attemptId}/answer`, {
      questionId,
      answer: answerValue
    })
    const withoutOld = currentAttempt.value.answers.filter((answer) => answer.questionId !== questionId)
    currentAttempt.value = {
      ...currentAttempt.value,
      answers: [...withoutOld, result]
    }
    return result
  }

  async function finish() {
    if (!currentAttempt.value) {
      throw new Error('Няма активен тест.')
    }
    summary.value = await api.post<QuizSummaryResponse>(`/quizzes/${currentAttempt.value.attemptId}/finish`)
    currentAttempt.value = {
      ...currentAttempt.value,
      status: 'COMPLETED',
      score: summary.value.score,
      grade: summary.value.grade,
      medal: summary.value.medal,
      crystals: summary.value.crystals,
      completedAt: summary.value.completedAt,
      answers: summary.value.results
    }
    fetchTotalCrystals().catch(() => {})
    return summary.value
  }

  async function heartbeat(attemptId?: string) {
    const id = attemptId ?? currentAttempt.value?.attemptId
    if (!id) {
      throw new Error('Няма активен тест.')
    }
    const response = await api.post<QuizTimeResponse>(`/quizzes/${id}/heartbeat`)
    if (currentAttempt.value?.attemptId === id) {
      currentAttempt.value = {
        ...currentAttempt.value,
        durationSeconds: response.durationSeconds
      }
    }
    return response
  }

  async function fetchLeaderboard() {
    const params = new URLSearchParams({
      category: selectedCategory.value,
      mode: selectedModeForQuery(),
      difficulty: String(difficulty.value)
    })
    leaderboard.value = await api.get<LeaderboardRow[]>(`/quizzes/leaderboard?${params}`)
  }

  async function fetchRecentAttempts() {
    const params = new URLSearchParams({
      category: selectedCategory.value,
      mode: selectedModeForQuery(),
      difficulty: String(difficulty.value)
    })
    recentAttempts.value = await api.get<RecentAttemptRow[]>(`/quizzes/recent?${params}`)
  }

  async function fetchCrystalLeaderboard() {
    crystalLeaderboard.value = await api.get<CrystalLeaderboardRow[]>('/quizzes/crystals/leaderboard')
  }

  async function fetchTotalCrystals() {
    const response = await api.get<CrystalTotalResponse>('/quizzes/crystals/total')
    totalCrystals.value = response.crystals
  }

  async function fetchHistory() {
    history.value = await api.get<QuizAttemptResponse[]>('/quizzes/history')
  }

  async function fetchActiveAttempts() {
    activeAttempts.value = await api.get<QuizAttemptResponse[]>('/quizzes/active')
  }

  async function cancelAttempt(attemptId: string) {
    await api.post<void>(`/quizzes/${attemptId}/cancel`)
    activeAttempts.value = activeAttempts.value.filter((attempt) => attempt.attemptId !== attemptId)
    if (currentAttempt.value?.attemptId === attemptId) {
      currentAttempt.value = null
    }
  }

  return {
    selectedCategory,
    testPlan,
    selectedMode,
    selectedIncludedModes,
    difficulty,
    currentAttempt,
    summary,
    leaderboard,
    recentAttempts,
    crystalLeaderboard,
    totalCrystals,
    history,
    activeAttempts,
    loading,
    answerMap,
    start,
    fetchAttempt,
    answer,
    finish,
    heartbeat,
    fetchLeaderboard,
    fetchRecentAttempts,
    fetchCrystalLeaderboard,
    fetchTotalCrystals,
    fetchHistory,
    fetchActiveAttempts,
    cancelAttempt
  }
})
