<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowLeft, Image as ImageIcon, Shuffle, SpellCheck2 } from 'lucide-vue-next'

interface AlphabetLetter {
  letter: string
  word: string
  image: string
}

const alphabet: AlphabetLetter[] = [
  { letter: 'А', word: 'Автобус', image: '🚌' },
  { letter: 'Б', word: 'Балон', image: '🎈' },
  { letter: 'В', word: 'Влак', image: '🚂' },
  { letter: 'Г', word: 'Грозде', image: '🍇' },
  { letter: 'Д', word: 'Дом', image: '🏠' },
  { letter: 'Е', word: 'Елен', image: '🦌' },
  { letter: 'Ж', word: 'Жаба', image: '🐸' },
  { letter: 'З', word: 'Заек', image: '🐰' },
  { letter: 'И', word: 'Игла', image: '🪡' },
  { letter: 'Й', word: 'Йо-йо', image: '🪀' },
  { letter: 'К', word: 'Котка', image: '🐱' },
  { letter: 'Л', word: 'Лимон', image: '🍋' },
  { letter: 'М', word: 'Мече', image: '🧸' },
  { letter: 'Н', word: 'Нос', image: '👃' },
  { letter: 'О', word: 'Око', image: '👁️' },
  { letter: 'П', word: 'Пате', image: '🐥' },
  { letter: 'Р', word: 'Риба', image: '🐟' },
  { letter: 'С', word: 'Слънце', image: '☀️' },
  { letter: 'Т', word: 'Топка', image: '⚽' },
  { letter: 'У', word: 'Ухо', image: '👂' },
  { letter: 'Ф', word: 'Флаг', image: '🚩' },
  { letter: 'Х', word: 'Хляб', image: '🍞' },
  { letter: 'Ц', word: 'Цвете', image: '🌸' },
  { letter: 'Ч', word: 'Часовник', image: '⏰' },
  { letter: 'Ш', word: 'Шапка', image: '🎩' },
  { letter: 'Щ', word: 'Щурец', image: '🦗' },
  { letter: 'Ъ', word: 'Ъгъл', image: '📐' },
  { letter: 'Ь', word: 'Мек знак', image: '🔤' },
  { letter: 'Ю', word: 'Юрган', image: '🛏️' },
  { letter: 'Я', word: 'Ябълка', image: '🍎' }
]

const orderedLetters = alphabet.map((item) => item.letter)
const letterOrder = ref([...orderedLetters])
const selectedLetter = ref(alphabet[0].letter)
const showPictures = ref(true)
const shuffled = ref(false)

const lettersByKey = new Map(alphabet.map((item) => [item.letter, item]))
const selected = computed(() => lettersByKey.get(selectedLetter.value) ?? alphabet[0])
const visibleLetters = computed(() => letterOrder.value.map((letter) => lettersByKey.get(letter)).filter(Boolean) as AlphabetLetter[])

function selectLetter(letter: string) {
  selectedLetter.value = letter
}

function showAlphabetical() {
  letterOrder.value = [...orderedLetters]
  shuffled.value = false
}

function shuffleLetters() {
  const next = [...orderedLetters]
  for (let index = next.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1))
    ;[next[index], next[swapIndex]] = [next[swapIndex], next[index]]
  }
  letterOrder.value = next
  shuffled.value = true
}
</script>

<template>
  <section class="page alphabet-page">
    <div class="alphabet-shell">
      <header class="alphabet-header">
        <RouterLink class="button secondary back-button" :to="{ name: 'dashboard' }">
          <ArrowLeft :size="19" />
          <span>Назад</span>
        </RouterLink>
        <div>
          <p class="eyebrow">Български</p>
          <h1>Азбука</h1>
        </div>
        <div class="alphabet-count">{{ alphabet.length }}</div>
      </header>

      <section class="alphabet-focus" :class="{ plain: !showPictures }">
        <div class="focus-letter">{{ selected.letter }}</div>
        <div v-if="showPictures" class="focus-picture" aria-hidden="true">{{ selected.image }}</div>
        <div class="focus-word">{{ selected.word }}</div>
      </section>

      <section class="alphabet-toolbar" aria-label="Настройки">
        <button class="tool-button" type="button" :class="{ active: showPictures }" @click="showPictures = !showPictures">
          <ImageIcon :size="20" />
          <span>{{ showPictures ? 'С картинки' : 'Само букви' }}</span>
        </button>
        <button class="tool-button" type="button" :class="{ active: shuffled }" @click="shuffleLetters">
          <Shuffle :size="20" />
          <span>Разбъркай</span>
        </button>
        <button class="tool-button" type="button" :disabled="!shuffled" @click="showAlphabetical">
          <SpellCheck2 :size="20" />
          <span>По ред</span>
        </button>
      </section>

      <div class="alphabet-grid" :class="{ compact: !showPictures }">
        <button
          v-for="item in visibleLetters"
          :key="item.letter"
          type="button"
          class="letter-card"
          :class="{ active: selectedLetter === item.letter }"
          :aria-label="`${item.letter} - ${item.word}`"
          @click="selectLetter(item.letter)"
        >
          <strong>{{ item.letter }}</strong>
          <span v-if="showPictures" aria-hidden="true">{{ item.image }}</span>
          <small v-if="showPictures">{{ item.word }}</small>
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.alphabet-page {
  width: min(1480px, calc(100% - 20px));
}

.alphabet-shell {
  display: grid;
  gap: 20px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: clamp(16px, 2.6vw, 28px);
  background: rgba(255, 253, 247, 0.94);
  box-shadow: var(--shadow);
}

.alphabet-header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
}

.alphabet-header h1,
.alphabet-header p {
  margin: 0;
}

.back-button {
  box-shadow: none;
}

.alphabet-count {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--green);
  font-size: 1.6rem;
  font-weight: 950;
}

.alphabet-focus {
  display: grid;
  grid-template-columns: minmax(94px, 0.8fr) minmax(110px, 1fr) minmax(140px, 1.2fr);
  align-items: center;
  gap: 18px;
  min-height: 176px;
  border: 2px solid rgba(30, 157, 116, 0.2);
  border-radius: var(--radius);
  padding: 18px;
  background: #ffffff;
}

.alphabet-focus.plain {
  grid-template-columns: minmax(120px, 0.8fr) minmax(160px, 1.4fr);
}

.focus-letter {
  display: grid;
  min-height: 128px;
  place-items: center;
  border-radius: var(--radius);
  color: #ffffff;
  background: var(--green);
  font-size: clamp(4.6rem, 12vw, 8rem);
  font-weight: 950;
  line-height: 1;
}

.focus-picture {
  display: grid;
  min-height: 128px;
  place-items: center;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  background: #fffdf7;
  font-size: clamp(4rem, 10vw, 7rem);
}

.focus-word {
  min-width: 0;
  color: var(--ink);
  font-size: clamp(2rem, 5vw, 4.5rem);
  font-weight: 950;
  line-height: 1.04;
  overflow-wrap: anywhere;
}

.alphabet-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tool-button {
  display: inline-flex;
  min-height: 46px;
  align-items: center;
  gap: 9px;
  border: 1px solid rgba(36, 48, 74, 0.14);
  border-radius: var(--radius);
  padding: 0 16px;
  color: var(--ink);
  background: #ffffff;
  font-weight: 850;
}

.tool-button.active {
  border-color: rgba(30, 157, 116, 0.5);
  color: var(--green-dark);
  background: rgba(30, 157, 116, 0.1);
}

.tool-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.alphabet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  gap: 10px;
}

.alphabet-grid.compact {
  grid-template-columns: repeat(auto-fit, minmax(88px, 1fr));
}

.letter-card {
  display: grid;
  min-height: 142px;
  grid-template-rows: auto 1fr auto;
  align-items: center;
  justify-items: center;
  gap: 6px;
  border: 2px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 12px 8px;
  color: var(--ink);
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(36, 48, 74, 0.07);
}

.letter-card.active {
  border-color: var(--green);
  box-shadow: 0 12px 26px rgba(30, 157, 116, 0.18);
}

.letter-card strong {
  font-size: clamp(2.3rem, 6vw, 4rem);
  font-weight: 950;
  line-height: 1;
}

.letter-card span {
  font-size: clamp(2.1rem, 5vw, 3.2rem);
  line-height: 1;
}

.letter-card small {
  width: 100%;
  min-height: 1.2em;
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 850;
  line-height: 1.12;
  overflow-wrap: anywhere;
}

.alphabet-grid.compact .letter-card {
  min-height: 92px;
  grid-template-rows: 1fr;
}

@media (max-width: 760px) {
  .alphabet-header {
    grid-template-columns: 1fr auto;
  }

  .back-button {
    grid-column: 1 / -1;
    justify-self: start;
  }

  .alphabet-focus,
  .alphabet-focus.plain {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .alphabet-grid {
    grid-template-columns: repeat(auto-fit, minmax(94px, 1fr));
  }
}
</style>
