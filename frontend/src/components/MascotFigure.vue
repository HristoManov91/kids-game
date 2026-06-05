<script setup lang="ts">
defineProps<{
  mood: 'thinking' | 'happy' | 'sad' | 'proud'
  small?: boolean
}>()
</script>

<template>
  <div class="mascot" :class="[mood, { small }]" aria-hidden="true">
    <div class="spark one"></div>
    <div class="spark two"></div>
    <div class="head">
      <div class="hair"></div>
      <div class="eyes">
        <span></span>
        <span></span>
      </div>
      <div class="mouth"></div>
      <div v-if="mood === 'thinking'" class="question-dot">?</div>
      <div v-if="mood === 'proud'" class="badge-star"></div>
    </div>
    <div class="body">
      <span class="arm left"></span>
      <span class="shirt"></span>
      <span class="arm right"></span>
    </div>
  </div>
</template>

<style scoped>
.mascot {
  position: relative;
  display: grid;
  width: 132px;
  height: 150px;
  place-items: center;
}

.mascot.small {
  width: 76px;
  height: 86px;
}

.head {
  position: relative;
  z-index: 2;
  display: grid;
  width: 92px;
  height: 92px;
  place-items: center;
  border: 4px solid rgba(36, 48, 74, 0.14);
  border-radius: 50%;
  background: #ffe1b7;
  box-shadow: inset 0 -9px rgba(205, 123, 69, 0.12);
}

.small .head {
  width: 54px;
  height: 54px;
  border-width: 3px;
}

.hair {
  position: absolute;
  top: -4px;
  left: 18px;
  width: 54px;
  height: 22px;
  border-radius: 999px 999px 8px 8px;
  background: #3b2d2b;
  transform: rotate(-4deg);
}

.small .hair {
  left: 10px;
  width: 34px;
  height: 14px;
}

.eyes {
  display: flex;
  gap: 20px;
  transform: translateY(2px);
}

.small .eyes {
  gap: 12px;
}

.eyes span {
  width: 10px;
  height: 13px;
  border-radius: 50%;
  background: #24304a;
}

.small .eyes span {
  width: 7px;
  height: 8px;
}

.mouth {
  position: absolute;
  bottom: 22px;
  width: 32px;
  height: 15px;
  border: 4px solid #24304a;
  border-top: 0;
  border-radius: 0 0 999px 999px;
}

.small .mouth {
  bottom: 13px;
  width: 19px;
  height: 9px;
  border-width: 3px;
}

.sad .mouth {
  bottom: 16px;
  border-top: 4px solid #24304a;
  border-bottom: 0;
  border-radius: 999px 999px 0 0;
}

.thinking .mouth {
  width: 17px;
  height: 6px;
  border: 0;
  border-radius: 999px;
  background: #24304a;
}

.body {
  position: absolute;
  bottom: 3px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: 112px;
  height: 66px;
}

.small .body {
  width: 66px;
  height: 38px;
}

.shirt {
  width: 72px;
  height: 58px;
  border-radius: 28px 28px 14px 14px;
  background: var(--blue);
  box-shadow: inset 0 -8px rgba(36, 48, 74, 0.12);
}

.small .shirt {
  width: 42px;
  height: 34px;
  border-radius: 18px 18px 9px 9px;
}

.arm {
  width: 20px;
  height: 54px;
  border-radius: 999px;
  background: #ffe1b7;
}

.small .arm {
  width: 12px;
  height: 30px;
}

.arm.left {
  transform: rotate(28deg) translate(6px, 4px);
}

.arm.right {
  transform: rotate(-28deg) translate(-6px, 4px);
}

.happy .arm.left,
.proud .arm.left {
  transform: rotate(-122deg) translate(-4px, -8px);
}

.happy .arm.right,
.proud .arm.right {
  transform: rotate(122deg) translate(4px, -8px);
}

.sad .shirt {
  background: var(--coral);
}

.thinking .shirt {
  background: var(--amber);
}

.proud .shirt {
  background: var(--green);
}

.question-dot {
  position: absolute;
  top: -18px;
  right: -12px;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--blue);
  font-weight: 950;
}

.small .question-dot {
  top: -12px;
  right: -8px;
  width: 22px;
  height: 22px;
  font-size: 0.8rem;
}

.badge-star {
  position: absolute;
  top: -17px;
  right: -15px;
  width: 34px;
  height: 34px;
  background: var(--amber);
  clip-path: polygon(50% 0, 62% 34%, 98% 35%, 68% 56%, 79% 91%, 50% 70%, 21% 91%, 32% 56%, 2% 35%, 38% 34%);
}

.small .badge-star {
  top: -11px;
  right: -9px;
  width: 22px;
  height: 22px;
}

.spark {
  position: absolute;
  border-radius: 50%;
  background: rgba(245, 185, 66, 0.36);
}

.spark.one {
  top: 14px;
  left: 10px;
  width: 20px;
  height: 20px;
}

.spark.two {
  right: 8px;
  bottom: 36px;
  width: 14px;
  height: 14px;
  background: rgba(30, 157, 116, 0.24);
}

.small .spark {
  display: none;
}
</style>
