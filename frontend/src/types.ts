export type Role = 'PARENT' | 'CHILD'

export type QuizCategory = 'MATH' | 'BULGARIAN' | 'LOGIC'

export type QuizMode =
  | 'ADDITION'
  | 'SUBTRACTION'
  | 'MIXED'
  | 'UNKNOWN_ADDITION'
  | 'UNKNOWN_SUBTRACTION'
  | 'UNKNOWN_MIXED'
  | 'MULTIPLICATION'
  | 'DIVISION'
  | 'MULTIPLICATION_DIVISION'
  | 'COMPARE'
  | 'WORD_LETTERS'
  | 'WORD_SYLLABLES'
  | 'WORD_TYPING'
  | 'WORD_PICTURE'
  | 'WORD_MISSING_LETTER'
  | 'WORD_FIRST_LETTER_GROUP'
  | 'WORD_WRONG_LETTER'
  | 'FIND_OBJECT'
  | 'SPOT_DIFFERENCES'
  | 'MEMORY_PAIRS'
  | 'PATTERN_SEQUENCE'
  | 'CUSTOM_GROUP'
  | 'ALL_GROUP'

export type QuestionKind = 'NUMERIC' | 'CHOICE' | 'LETTER_ORDER' | 'SYLLABLE_ORDER' | 'WORD_TYPING' | 'GROUPING' | 'SPOT_DIFFERENCES' | 'MEMORY_PAIRS' | 'PATTERN_SEQUENCE'
export type AttemptStatus = 'ACTIVE' | 'COMPLETED'
export type Medal = 'DIAMOND_CUP' | 'GOLD_CUP' | 'SILVER_MEDAL' | 'BRONZE_STAR' | 'KEEP_GOING'
export type IssueReportStatus = 'OPEN' | 'RESOLVED' | 'DISMISSED'

export interface UserResponse {
  id: number
  username: string
  displayName: string
  role: Role
}

export interface AuthResponse {
  token: string
  user: UserResponse
}

export interface QuestionResponse {
  id: number
  kind: QuestionKind
  prompt: string
  image: string | null
  speechText: string | null
  answerSlots: string[]
  choices: string[]
  sourceMode: QuizMode | null
}

export interface QuestionResultResponse {
  questionId: number
  answer: string
  correct: boolean
  correctAnswer: string
  answeredAt: string
}

export interface QuizAttemptResponse {
  attemptId: string
  category: QuizCategory
  mode: QuizMode
  includedModes: QuizMode[]
  difficulty: number
  leaderboardEligible: boolean
  status: AttemptStatus
  questions: QuestionResponse[]
  answers: QuestionResultResponse[]
  score: number | null
  totalQuestions: number
  grade: string | null
  medal: Medal | null
  crystals: number | null
  startedAt: string
  completedAt: string | null
  durationSeconds: number
  scoreBreakdown: ScoreBreakdownResponse[]
}

export interface QuizSummaryResponse {
  attemptId: string
  category: QuizCategory
  mode: QuizMode
  includedModes: QuizMode[]
  difficulty: number
  leaderboardEligible: boolean
  score: number
  totalQuestions: number
  grade: string
  medal: Medal
  crystals: number
  completedAt: string
  durationSeconds: number
  results: QuestionResultResponse[]
  scoreBreakdown: ScoreBreakdownResponse[]
}

export interface QuizTimeResponse {
  durationSeconds: number
}

export interface ScoreBreakdownResponse {
  mode: QuizMode
  correct: number
  total: number
}

export interface LeaderboardRow {
  rank: number
  userId: number
  displayName: string
  attemptId: string
  score: number
  totalQuestions: number
  grade: string
  medal: Medal
  crystals: number
  durationSeconds: number
  completedAt: string
}

export interface RecentAttemptRow {
  attemptId: string
  category: QuizCategory
  mode: QuizMode
  difficulty: number
  score: number
  totalQuestions: number
  grade: string
  crystals: number
  durationSeconds: number
  completedAt: string
}

export interface CrystalLeaderboardRow {
  rank: number
  userId: number
  displayName: string
  crystals: number
  completedAttempts: number
}

export interface CrystalTotalResponse {
  crystals: number
}

export interface RewardTheme {
  id: string
  name: string
  description: string
  backgroundImage: string
  thumbnailImage: string
  categories: string[]
  defaultPictureName: string
}

export interface RewardCatalogItem {
  id: string
  themeId: string
  themeIds: string[]
  category: string
  name: string
  price: number
  image: string
  defaultScale: number
  minScale: number
  maxScale: number
}

export interface RewardCatalogResponse {
  themes: RewardTheme[]
  items: RewardCatalogItem[]
}

export interface AdminRewardCatalogItem extends RewardCatalogItem {
  active: boolean
  usedCount: number
  createdAt: string
  updatedAt: string
}

export interface DeleteRewardCatalogItemResponse {
  itemId: string
  itemName: string
  removedPlacedItems: number
  refundedPurchases: number
  refundedCrystals: number
}

export interface RewardBalanceResponse {
  earned: number
  spent: number
  available: number
}

export interface PlacedRewardItem {
  id: string
  catalogItemId: string
  name: string
  image: string
  x: number
  y: number
  scale: number
  rotation: number
  mirrored?: boolean
  zIndex: number
  createdAt: string
}

export interface AlbumPictureSummary {
  id: string
  name: string
  themeId: string
  themeName: string
  backgroundImage: string
  itemCount: number
  previewItems: PlacedRewardItem[]
  createdAt: string
  updatedAt: string
}

export interface AlbumPictureDetail {
  id: string
  name: string
  themeId: string
  themeName: string
  backgroundImage: string
  placedItems: PlacedRewardItem[]
  createdAt: string
  updatedAt: string
}

export interface PurchaseRewardItemResponse {
  balance: RewardBalanceResponse
  picture: AlbumPictureDetail
  placedItem: PlacedRewardItem
  message: string
}

export interface RemoveRewardItemResponse {
  balance: RewardBalanceResponse
  picture: AlbumPictureDetail
  refundedCrystals: number
  message: string
}

export interface ReportAttemptRow {
  attemptId: string
  category: QuizCategory
  mode: QuizMode
  includedModes: QuizMode[]
  difficulty: number
  status: AttemptStatus
  score: number | null
  totalQuestions: number
  correct: number
  wrong: number
  unanswered: number
  grade: string | null
  medal: Medal | null
  crystals: number | null
  startedAt: string
  completedAt: string | null
  durationSeconds: number
}

export interface ReportFocusArea {
  category: QuizCategory
  mode: QuizMode
  correct: number
  total: number
  wrong: number
  accuracyPercent: number
  averageGrade: string
}

export interface ChildReportResponse {
  child: UserResponse
  from: string
  to: string
  totalAttempts: number
  completedAttempts: number
  totalCorrect: number
  totalWrong: number
  totalDurationSeconds: number
  averageGrade: string
  averageAccuracyPercent: number
  focusAreas: ReportFocusArea[]
  attempts: ReportAttemptRow[]
}

export interface ReportQuestionRow {
  questionId: number
  kind: QuestionKind
  sourceMode: QuizMode | null
  prompt: string
  answer: string
  correctAnswer: string
  correct: boolean
}

export interface ReportAttemptDetailResponse {
  attempt: ReportAttemptRow
  questions: ReportQuestionRow[]
}

export interface AdminMonitoringResponse {
  generatedAt: string
  fromDate: string
  toDate: string
  periodStart: string
  periodEnd: string
  totalUsers: number
  childUsers: number
  totalAttempts: number
  startedToday: number
  activeAttempts: number
  staleActiveAttempts: number
  completedAttempts: number
  completedToday: number
  activeUsersToday: number
  averageGradeToday: string | null
  averageDurationTodaySeconds: number
  fastestDurationTodaySeconds: number
  slowestDurationTodaySeconds: number
  allTimeAverageGrade: string | null
  allTimeAverageDurationSeconds: number
  openIssueReports: number
  openSuggestions: number
  users: AdminUserMonitorRow[]
  active: AdminAttemptMonitorRow[]
  recentCompleted: AdminAttemptMonitorRow[]
  todayCompleted: AdminAttemptMonitorRow[]
  categories: AdminCategoryMonitorRow[]
  allTimeCategories: AdminCategoryMonitorRow[]
  todayModes: AdminModeMonitorRow[]
  todayUsage: AdminUsageBucketRow[]
  recentReports: QuestionIssueReportResponse[]
  recentSuggestions: TaskSuggestionResponse[]
}

export interface AdminUserMonitorRow {
  userId: number
  username: string
  displayName: string
  activeAttempts: number
  completedAttempts: number
  totalAttempts: number
  averageGrade: string | null
  lastActivityAt: string | null
}

export interface AdminAttemptMonitorRow {
  attemptId: string
  userId: number
  displayName: string
  category: QuizCategory
  mode: QuizMode
  difficulty: number
  status: AttemptStatus
  leaderboardEligible: boolean
  score: number | null
  totalQuestions: number
  grade: string | null
  startedAt: string
  completedAt: string | null
  durationSeconds: number
  answeredCount: number
}

export interface AdminCategoryMonitorRow {
  category: QuizCategory
  completedAttempts: number
  averageGrade: string | null
  averageScorePercent: number
}

export interface AdminModeMonitorRow {
  category: QuizCategory
  mode: QuizMode
  completedAttempts: number
  averageGrade: string | null
  averageDurationSeconds: number
  fastestDurationSeconds: number
  slowestDurationSeconds: number
  averageScorePercent: number
  attempts: AdminAttemptMonitorRow[]
}

export interface AdminUsageBucketRow {
  label: string
  started: number
  completed: number
}

export interface QuestionIssueReportResponse {
  id: number
  attemptId: string
  userId: number
  displayName: string
  category: QuizCategory
  mode: QuizMode
  difficulty: number
  questionId: number
  questionPrompt: string
  message: string
  status: IssueReportStatus
  adminNote: string | null
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
}

export interface TaskSuggestionResponse {
  id: number
  userId: number
  displayName: string
  category: QuizCategory | null
  mode: QuizMode | null
  difficulty: number | null
  message: string
  status: IssueReportStatus
  adminNote: string | null
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
}

export interface AdminAttemptDetailResponse {
  attempt: AdminAttemptMonitorRow
  questions: AdminQuestionReviewRow[]
}

export interface AdminQuestionReviewRow {
  questionId: number
  kind: QuestionKind
  sourceMode: QuizMode | null
  prompt: string
  image: string | null
  speechText: string | null
  answerSlots: string[]
  choices: string[]
  expectedAnswer: string
  publicCorrectAnswer: string
  answer: string
  correct: boolean
  answeredAt: string | null
  reports: QuestionIssueReportResponse[]
}

export interface WordCatalogEntry {
  id: number
  category: QuizCategory
  word: string
  image: string
  letters: string[]
  syllables: string[]
  difficulty: number
  active: boolean
  createdAt: string
  updatedAt: string
  usedCount: number
  correctCount: number
  wrongCount: number
}

export interface WordCatalogSuggestion {
  category: QuizCategory
  word: string
  letters: string[]
  suggestedSyllables: string[]
  suggestedDifficulty: number
}
