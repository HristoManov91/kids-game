<script setup lang="ts">
import { Gem, PlusCircle } from 'lucide-vue-next'
import type { RewardCatalogItem } from '@/types'

const props = defineProps<{
  item: RewardCatalogItem
  crystals: number
  busy?: boolean
}>()

defineEmits<{
  add: [itemId: string]
}>()

const canBuy = () => props.crystals >= props.item.price && !props.busy
const isImageAsset = (image: string) => image.startsWith('/') || image.startsWith('http') || image.startsWith('data:image')
const isMarvelAsset = (image: string) => image.includes('cdn.marvel.com/')
</script>

<template>
  <article class="catalog-card" :class="{ disabled: !canBuy() }">
    <div class="item-art" aria-hidden="true">
      <img
        v-if="isImageAsset(item.image)"
        :src="item.image"
        :alt="item.name"
        :class="{ marvel: isMarvelAsset(item.image) }"
        draggable="false"
      >
      <span v-else>{{ item.image }}</span>
    </div>
    <div class="item-body">
      <strong>{{ item.name }}</strong>
      <span class="price">{{ item.price }} <Gem :size="16" /></span>
      <small v-if="crystals < item.price">Не достигат {{ item.price - crystals }}</small>
      <small v-else>{{ item.category }}</small>
    </div>
    <button class="button compact-add" type="button" :disabled="!canBuy()" @click="$emit('add', item.id)">
      <PlusCircle :size="18" />
      <span>Добави</span>
    </button>
  </article>
</template>

<style scoped>
.catalog-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 158px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 10px;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(36, 48, 74, 0.08);
}

.catalog-card.disabled {
  opacity: 0.48;
}

.item-art {
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  border-radius: 14px;
  background: rgba(63, 125, 217, 0.08);
  font-size: clamp(1.85rem, 5vw, 2.35rem);
  line-height: 0.98;
  text-align: center;
  white-space: normal;
}

.item-art img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.item-art img.marvel {
  object-fit: cover;
  object-position: left center;
}

.item-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.item-body strong {
  color: var(--ink);
  font-size: 0.98rem;
  font-weight: 950;
  line-height: 1.08;
  overflow-wrap: anywhere;
}

.price {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--blue);
  font-weight: 950;
}

.item-body small {
  color: var(--muted);
  font-weight: 800;
}

.compact-add {
  grid-column: 1 / -1;
  min-height: 38px;
  padding: 0 12px;
}
</style>
