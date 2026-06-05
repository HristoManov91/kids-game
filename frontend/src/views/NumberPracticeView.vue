<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowLeft, Dice5, Image as ImageIcon, ListOrdered, Shuffle, SpellCheck2 } from 'lucide-vue-next'

interface SmallNumber {
  value: number
  word: string
  fruit: string
  fruitName: string
}

const smallNumbers: SmallNumber[] = [
  { value: 0, word: 'нула', fruit: '🧺', fruitName: 'празна кошница' },
  { value: 1, word: 'едно', fruit: '🍎', fruitName: 'ябълка' },
  { value: 2, word: 'две', fruit: '🍐', fruitName: 'круши' },
  { value: 3, word: 'три', fruit: '🍊', fruitName: 'портокала' },
  { value: 4, word: 'четири', fruit: '🍋', fruitName: 'лимона' },
  { value: 5, word: 'пет', fruit: '🍓', fruitName: 'ягоди' },
  { value: 6, word: 'шест', fruit: '🍒', fruitName: 'череши' },
  { value: 7, word: 'седем', fruit: '🍇', fruitName: 'гроздове' },
  { value: 8, word: 'осем', fruit: '🍑', fruitName: 'праскови' },
  { value: 9, word: 'девет', fruit: '🥝', fruitName: 'кивита' },
  { value: 10, word: 'десет', fruit: '🍌', fruitName: 'банана' }
]

const units = ['нула', 'едно', 'две', 'три', 'четири', 'пет', 'шест', 'седем', 'осем', 'девет']
const teens = [
  'десет',
  'единадесет',
  'дванадесет',
  'тринадесет',
  'четиринадесет',
  'петнадесет',
  'шестнадесет',
  'седемнадесет',
  'осемнадесет',
  'деветнадесет'
]
const tens = ['', '', 'двадесет', 'тридесет', 'четиридесет', 'петдесет', 'шестдесет', 'седемдесет', 'осемдесет', 'деветдесет']

const allNumbers = Array.from({ length: 101 }, (_, value) => ({
  value,
  word: numberWord(value)
}))
const allNumberValues = allNumbers.map((item) => item.value)
const numberOrder = ref([...allNumberValues])
const selectedNumber = ref(0)
const selectedSmallNumber = ref(0)
const selectedTab = ref<'FRUITS' | 'LIST' | 'ORAL'>('FRUITS')
const shuffled = ref(false)
const oralRange = ref<'SMALL' | 'HUNDRED'>('SMALL')
const oralNumber = ref(0)
const showOralWord = ref(true)

const numbersByValue = new Map(allNumbers.map((item) => [item.value, item]))
const smallByValue = new Map(smallNumbers.map((item) => [item.value, item]))
const selectedSmall = computed(() => smallByValue.get(selectedSmallNumber.value) ?? smallNumbers[0])
const selectedBig = computed(() => numbersByValue.get(selectedNumber.value) ?? allNumbers[0])
const visibleNumbers = computed(() => numberOrder.value.map((value) => numbersByValue.get(value)).filter(Boolean) as typeof allNumbers)
const oralItem = computed(() => numbersByValue.get(oralNumber.value) ?? allNumbers[0])
const oralSmallItem = computed(() => smallByValue.get(oralNumber.value))

function numberWord(value: number) {
  if (value < 10) {
    return units[value]
  }
  if (value < 20) {
    return teens[value - 10]
  }
  if (value === 100) {
    return 'сто'
  }
  const ten = Math.floor(value / 10)
  const unit = value % 10
  if (unit === 0) {
    return tens[ten]
  }
  return `${tens[ten]} и ${units[unit]}`
}

function fruitItems(number: SmallNumber) {
  if (number.value === 0) {
    return []
  }
  return Array.from({ length: number.value }, (_, index) => `${number.fruit}-${index}`)
}

function selectSmall(value: number) {
  selectedSmallNumber.value = value
}

function selectNumber(value: number) {
  selectedNumber.value = value
}

function showOrderedNumbers() {
  numberOrder.value = [...allNumberValues]
  shuffled.value = false
}

function shuffleNumbers() {
  const next = [...allNumberValues]
  for (let index = next.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1))
    ;[next[index], next[swapIndex]] = [next[swapIndex], next[index]]
  }
  numberOrder.value = next
  shuffled.value = true
}

function nextOralNumber() {
  const max = oralRange.value === 'SMALL' ? 10 : 100
  let next = Math.floor(Math.random() * (max + 1))
  if (max > 0 && next === oralNumber.value) {
    next = (next + 1) % (max + 1)
  }
  oralNumber.value = next
}

function setOralRange(range: 'SMALL' | 'HUNDRED') {
  oralRange.value = range
  oralNumber.value = 0
}
</script>

<template>
  <section class="page numbers-page">
    <div class="numbers-shell">
      <header class="numbers-header">
        <RouterLink class="button secondary back-button" :to="{ name: 'dashboard' }">
          <ArrowLeft :size="19" />
          <span>Назад</span>
        </RouterLink>
        <div>
          <p class="eyebrow">Математика</p>
          <h1>Цифри и числа</h1>
        </div>
        <div class="numbers-count">0-100</div>
      </header>

      <section class="numbers-tabs" aria-label="Вид упражнение">
        <button type="button" :class="{ active: selectedTab === 'FRUITS' }" @click="selectedTab = 'FRUITS'">
          <ImageIcon :size="20" />
          <span>0 до 10</span>
        </button>
        <button type="button" :class="{ active: selectedTab === 'LIST' }" @click="selectedTab = 'LIST'">
          <ListOrdered :size="20" />
          <span>0 до 100</span>
        </button>
        <button type="button" :class="{ active: selectedTab === 'ORAL' }" @click="selectedTab = 'ORAL'">
          <Dice5 :size="20" />
          <span>Устно упражнение</span>
        </button>
      </section>

      <template v-if="selectedTab === 'FRUITS'">
        <section class="number-focus">
          <div class="focus-number">{{ selectedSmall.value }}</div>
          <div class="fruit-board" :class="{ empty: selectedSmall.value === 0 }" aria-hidden="true">
            <span v-if="selectedSmall.value === 0" class="empty-basket">{{ selectedSmall.fruit }}</span>
            <span v-for="item in fruitItems(selectedSmall)" v-else :key="item">{{ selectedSmall.fruit }}</span>
          </div>
          <div class="focus-word">
            <strong>{{ selectedSmall.word }}</strong>
            <span>{{ selectedSmall.value === 0 ? 'няма плодове' : selectedSmall.fruitName }}</span>
          </div>
        </section>

        <div class="small-number-grid">
          <button
            v-for="item in smallNumbers"
            :key="item.value"
            type="button"
            class="small-number-card"
            :class="{ active: selectedSmallNumber === item.value }"
            @click="selectSmall(item.value)"
          >
            <strong>{{ item.value }}</strong>
            <span aria-hidden="true">{{ item.fruit }}</span>
            <small>{{ item.word }}</small>
          </button>
        </div>
      </template>

      <template v-else-if="selectedTab === 'LIST'">
        <section class="big-number-focus">
          <div class="focus-number">{{ selectedBig.value }}</div>
          <div class="big-number-word">{{ selectedBig.word }}</div>
        </section>

        <section class="numbers-toolbar" aria-label="Настройки">
          <button class="tool-button" type="button" :class="{ active: shuffled }" @click="shuffleNumbers">
            <Shuffle :size="20" />
            <span>Разбъркай</span>
          </button>
          <button class="tool-button" type="button" :disabled="!shuffled" @click="showOrderedNumbers">
            <SpellCheck2 :size="20" />
            <span>По ред</span>
          </button>
        </section>

        <div class="hundred-grid">
          <button
            v-for="item in visibleNumbers"
            :key="item.value"
            type="button"
            class="hundred-card"
            :class="{ active: selectedNumber === item.value }"
            @click="selectNumber(item.value)"
          >
            <strong>{{ item.value }}</strong>
          </button>
        </div>
      </template>

      <template v-else>
        <section class="oral-card">
          <div class="oral-range" aria-label="Обхват">
            <button type="button" :class="{ active: oralRange === 'SMALL' }" @click="setOralRange('SMALL')">0 до 10</button>
            <button type="button" :class="{ active: oralRange === 'HUNDRED' }" @click="setOralRange('HUNDRED')">0 до 100</button>
          </div>

          <div class="oral-number">{{ oralNumber }}</div>
          <div v-if="oralRange === 'SMALL' && oralSmallItem" class="oral-fruit-board" aria-hidden="true">
            <span v-if="oralSmallItem.value === 0" class="empty-basket">{{ oralSmallItem.fruit }}</span>
            <span v-for="item in fruitItems(oralSmallItem)" v-else :key="item">{{ oralSmallItem.fruit }}</span>
          </div>
          <div class="oral-word" :class="{ hidden: !showOralWord }">
            {{ showOralWord ? oralItem.word : 'Кажи числото на глас' }}
          </div>

          <div class="oral-actions">
            <button class="button" type="button" @click="nextOralNumber">
              <Dice5 :size="20" />
              <span>Следващо число</span>
            </button>
            <button class="button secondary" type="button" @click="showOralWord = !showOralWord">
              <ImageIcon :size="20" />
              <span>{{ showOralWord ? 'Скрий думата' : 'Покажи думата' }}</span>
            </button>
          </div>
        </section>
      </template>
    </div>
  </section>
</template>

<style scoped>
.numbers-page {
  width: min(1480px, calc(100% - 20px));
}

.numbers-shell {
  display: grid;
  gap: 20px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: clamp(16px, 2.6vw, 28px);
  background: rgba(255, 253, 247, 0.94);
  box-shadow: var(--shadow);
}

.numbers-header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
}

.numbers-header h1,
.numbers-header p {
  margin: 0;
}

.back-button {
  box-shadow: none;
}

.numbers-count {
  display: grid;
  min-width: 76px;
  height: 58px;
  place-items: center;
  border-radius: var(--radius);
  color: #ffffff;
  background: var(--blue);
  font-size: 1.35rem;
  font-weight: 950;
}

.numbers-tabs,
.oral-range {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 6px;
  background: #ffffff;
}

.numbers-tabs button,
.oral-range button {
  display: inline-flex;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: calc(var(--radius) - 2px);
  color: var(--muted);
  background: transparent;
  font-weight: 900;
}

.numbers-tabs button.active,
.oral-range button.active {
  color: #ffffff;
  background: var(--green);
}

.number-focus,
.big-number-focus {
  display: grid;
  grid-template-columns: minmax(120px, 0.7fr) minmax(220px, 1.3fr) minmax(180px, 1fr);
  align-items: center;
  gap: 18px;
  min-height: 210px;
  border: 2px solid rgba(63, 125, 217, 0.2);
  border-radius: var(--radius);
  padding: 18px;
  background: #ffffff;
}

.big-number-focus {
  grid-template-columns: minmax(130px, 0.7fr) minmax(220px, 1.6fr);
}

.focus-number,
.oral-number {
  display: grid;
  min-height: 140px;
  place-items: center;
  border-radius: var(--radius);
  color: #ffffff;
  background: var(--blue);
  font-size: clamp(4.8rem, 12vw, 8.5rem);
  font-weight: 950;
  line-height: 1;
}

.fruit-board,
.oral-fruit-board {
  display: flex;
  min-height: 150px;
  align-content: center;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  border: 1px solid rgba(36, 48, 74, 0.1);
  border-radius: var(--radius);
  padding: 14px;
  background: #fffdf7;
}

.fruit-board span,
.oral-fruit-board span {
  font-size: clamp(2rem, 4.2vw, 3.4rem);
  line-height: 1;
}

.fruit-board.empty,
.oral-fruit-board .empty-basket {
  color: var(--muted);
}

.empty-basket {
  font-size: clamp(4rem, 9vw, 6.5rem) !important;
}

.focus-word,
.big-number-word {
  display: grid;
  gap: 8px;
  min-width: 0;
  color: var(--ink);
  font-size: clamp(1.8rem, 4vw, 3.8rem);
  font-weight: 950;
  line-height: 1.04;
  overflow-wrap: anywhere;
}

.focus-word span {
  color: var(--muted);
  font-size: clamp(1rem, 2vw, 1.4rem);
  font-weight: 850;
}

.small-number-grid,
.hundred-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  gap: 10px;
}

.hundred-grid {
  grid-template-columns: repeat(10, minmax(92px, 1fr));
  overflow-x: auto;
  padding-bottom: 4px;
}

.small-number-card,
.hundred-card {
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

.hundred-card {
  min-height: 88px;
  grid-template-rows: 1fr;
  align-content: center;
  gap: 0;
  padding: 8px 6px;
}

.small-number-card.active,
.hundred-card.active {
  border-color: var(--blue);
  box-shadow: 0 12px 26px rgba(63, 125, 217, 0.18);
}

.small-number-card strong,
.hundred-card strong {
  font-size: clamp(2.2rem, 5vw, 3.9rem);
  font-weight: 950;
  line-height: 1;
}

.hundred-card strong {
  font-size: clamp(1.75rem, 2.7vw, 3.2rem);
  letter-spacing: 0;
}

.small-number-card span {
  font-size: clamp(2.1rem, 5vw, 3.1rem);
  line-height: 1;
}

.small-number-card small {
  width: 100%;
  min-height: 1.2em;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 850;
  line-height: 1.06;
  overflow-wrap: anywhere;
  word-break: normal;
}

.numbers-toolbar {
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

.oral-card {
  display: grid;
  justify-items: center;
  gap: 18px;
  border: 2px solid rgba(30, 157, 116, 0.18);
  border-radius: var(--radius);
  padding: clamp(18px, 3vw, 30px);
  background: #ffffff;
}

.oral-range {
  width: min(520px, 100%);
  grid-template-columns: 1fr 1fr;
}

.oral-number {
  width: min(360px, 100%);
  min-height: 190px;
  font-size: clamp(6rem, 18vw, 12rem);
}

.oral-fruit-board {
  width: min(720px, 100%);
}

.oral-word {
  min-height: 1.15em;
  color: var(--ink);
  font-size: clamp(2rem, 5vw, 4.4rem);
  font-weight: 950;
  text-align: center;
}

.oral-word.hidden {
  color: var(--muted);
  font-size: clamp(1.4rem, 3vw, 2.2rem);
}

.oral-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

@media (max-width: 760px) {
  .numbers-header {
    grid-template-columns: 1fr auto;
  }

  .back-button {
    grid-column: 1 / -1;
    justify-self: start;
  }

  .numbers-tabs {
    grid-template-columns: 1fr;
  }

  .number-focus,
  .big-number-focus {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .small-number-grid {
    grid-template-columns: repeat(auto-fit, minmax(94px, 1fr));
  }

  .hundred-card strong {
    font-size: clamp(1.55rem, 7vw, 2.3rem);
  }
}
</style>
