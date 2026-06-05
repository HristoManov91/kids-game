import type { QuizCategory, QuizMode } from '@/types'

export const modeLabels: Record<QuizMode, string> = {
  ADDITION: 'Събиране',
  SUBTRACTION: 'Изваждане',
  MIXED: 'Събиране и изваждане',
  UNKNOWN_ADDITION: 'Неизвестно със събиране',
  UNKNOWN_SUBTRACTION: 'Неизвестно с изваждане',
  UNKNOWN_MIXED: 'Неизвестно със събиране и изваждане',
  MULTIPLICATION: 'Умножение',
  DIVISION: 'Делене',
  MULTIPLICATION_DIVISION: 'Умножение и делене',
  COMPARE: 'По-голямо, по-малко, равно',
  WORD_LETTERS: 'Подреди букви',
  WORD_SYLLABLES: 'Подреди срички',
  WORD_TYPING: 'Напиши дума',
  WORD_PICTURE: 'Намери картинка',
  WORD_MISSING_LETTER: 'Липсваща буква',
  WORD_FIRST_LETTER_GROUP: 'Групирай по първа буква',
  WORD_WRONG_LETTER: 'Открий грешната буква',
  FIND_OBJECT: 'Открий предмета',
  SPOT_DIFFERENCES: 'Открий разликите',
  MEMORY_PAIRS: 'Запомни двойките',
  PATTERN_SEQUENCE: 'Подреди модела',
  CUSTOM_GROUP: 'Тест по избор',
  ALL_GROUP: 'Всички'
}

export const modeShortLabels: Record<QuizMode, string> = {
  ADDITION: 'Плюс',
  SUBTRACTION: 'Минус',
  MIXED: 'Смесени',
  UNKNOWN_ADDITION: '? +',
  UNKNOWN_SUBTRACTION: '? -',
  UNKNOWN_MIXED: '? + -',
  MULTIPLICATION: 'x',
  DIVISION: ':',
  MULTIPLICATION_DIVISION: 'x :',
  COMPARE: '< = >',
  WORD_LETTERS: 'Букви',
  WORD_SYLLABLES: 'Срички',
  WORD_TYPING: 'Писане',
  WORD_PICTURE: 'Картинка',
  WORD_MISSING_LETTER: 'Буква',
  WORD_FIRST_LETTER_GROUP: 'Кошници',
  WORD_WRONG_LETTER: 'Поправи',
  FIND_OBJECT: 'Предмет',
  SPOT_DIFFERENCES: 'Картинки',
  MEMORY_PAIRS: 'Памет',
  PATTERN_SEQUENCE: 'Модел',
  CUSTOM_GROUP: 'Избор',
  ALL_GROUP: 'Всички'
}

export const categoryLabels: Record<QuizCategory, string> = {
  MATH: 'Математика',
  BULGARIAN: 'Български',
  LOGIC: 'Логика'
}

export function levelRange(difficulty: number, category: QuizCategory = 'MATH') {
  if (category === 'LOGIC') {
    if (difficulty >= 9) {
      return 'много детайли'
    }
    if (difficulty >= 4) {
      return 'повече детайли'
    }
    if (difficulty >= 2) {
      return 'средна трудност'
    }
    return 'лесна логика'
  }
  if (category === 'BULGARIAN') {
    const labels = [
      'много лесни думи',
      'лесни думи',
      'кратки думи',
      'познати думи',
      'малко по-дълги думи',
      'думи със срички',
      'по-дълги думи',
      'две кратки думи',
      'по-сложни думи',
      'най-предизвикателни думи'
    ]
    return labels[difficulty - 1] ?? 'подбрани думи'
  }
  return `0 до ${difficulty * 10}`
}

export function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

export const primitiveModes: QuizMode[] = [
  'ADDITION',
  'SUBTRACTION',
  'MULTIPLICATION',
  'DIVISION',
  'UNKNOWN_ADDITION',
  'UNKNOWN_SUBTRACTION',
  'COMPARE',
  'WORD_LETTERS',
  'WORD_SYLLABLES',
  'WORD_TYPING',
  'WORD_PICTURE',
  'WORD_MISSING_LETTER',
  'WORD_FIRST_LETTER_GROUP',
  'WORD_WRONG_LETTER',
  'FIND_OBJECT',
  'SPOT_DIFFERENCES',
  'MEMORY_PAIRS',
  'PATTERN_SEQUENCE'
]

export const mathPrimitiveModes: QuizMode[] = [
  'ADDITION',
  'SUBTRACTION',
  'MULTIPLICATION',
  'DIVISION',
  'UNKNOWN_ADDITION',
  'UNKNOWN_SUBTRACTION',
  'COMPARE'
]

export const bulgarianPrimitiveModes: QuizMode[] = [
  'WORD_LETTERS',
  'WORD_SYLLABLES',
  'WORD_TYPING',
  'WORD_PICTURE',
  'WORD_MISSING_LETTER',
  'WORD_FIRST_LETTER_GROUP',
  'WORD_WRONG_LETTER'
]

export const logicPrimitiveModes: QuizMode[] = [
  'FIND_OBJECT',
  'SPOT_DIFFERENCES',
  'MEMORY_PAIRS',
  'PATTERN_SEQUENCE'
]
