<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ArrowRight, Images } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import RewardBalanceBadge from '@/components/rewards/RewardBalanceBadge.vue'
import { useRewardsStore } from '@/stores/rewards'

const rewards = useRewardsStore()
const router = useRouter()
const selectedThemeId = ref('')
const pictureName = ref('')
const saving = ref(false)

const selectedTheme = computed(() => rewards.themes.find((theme) => theme.id === selectedThemeId.value) ?? rewards.themes[0])

function isAssetThumbnail(thumbnail: string) {
  return thumbnail.startsWith('/') || thumbnail.startsWith('http') || thumbnail.startsWith('data:image')
}

function isMarvelAsset(image: string) {
  return image.includes('cdn.marvel.com/')
}

onMounted(async () => {
  await rewards.bootstrap()
  if (!selectedThemeId.value && rewards.themes.length > 0) {
    selectedThemeId.value = rewards.themes[0].id
  }
})

watch(selectedTheme, (theme) => {
  if (theme && !pictureName.value.trim()) {
    pictureName.value = theme.defaultPictureName
  }
})

async function createPicture() {
  if (!selectedTheme.value || saving.value) {
    return
  }
  saving.value = true
  try {
    const created = await rewards.createPicture(selectedTheme.value.id, pictureName.value)
    router.push({ name: 'rewardAlbumEdit', params: { pictureId: created.id } })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="page create-picture-page">
    <section class="panel create-header">
      <div>
        <p class="eyebrow">Нова картина</p>
        <h1>Избери свят</h1>
      </div>
      <RewardBalanceBadge :crystals="rewards.balance.available" />
    </section>

    <section class="panel create-panel">
      <div class="theme-grid">
        <button
          v-for="theme in rewards.themes"
          :key="theme.id"
          type="button"
          class="theme-card"
          :class="{ active: selectedThemeId === theme.id }"
          @click="selectedThemeId = theme.id"
        >
          <span class="theme-art" :style="{ background: theme.backgroundImage }">
            <img
              v-if="isAssetThumbnail(theme.thumbnailImage)"
              :src="theme.thumbnailImage"
              :alt="theme.name"
              :class="{ marvel: isMarvelAsset(theme.thumbnailImage) }"
            />
            <span v-else>{{ theme.thumbnailImage }}</span>
          </span>
          <strong>{{ theme.name }}</strong>
          <small>{{ theme.description }}</small>
        </button>
      </div>

      <form class="name-panel" @submit.prevent="createPicture">
        <Images :size="42" />
        <label class="field">
          <span>Име на картината</span>
          <input v-model.trim="pictureName" maxlength="40" :placeholder="selectedTheme?.defaultPictureName" />
        </label>
        <button class="button" type="submit" :disabled="saving || !selectedTheme">
          <ArrowRight :size="21" />
          <span>Отвори редактора</span>
        </button>
      </form>
    </section>
  </section>
</template>

<style scoped>
.create-picture-page {
  display: grid;
  gap: 20px;
}

.create-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;
}

.create-header h1,
.create-header p {
  margin: 0;
}

.create-header h1 {
  font-size: clamp(2.2rem, 6vw, 4rem);
  line-height: 1;
}

.eyebrow {
  color: var(--green-dark);
  font-weight: 950;
  text-transform: uppercase;
}

.create-panel {
  display: grid;
  gap: 22px;
  padding: 22px;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 14px;
}

.theme-card {
  display: grid;
  gap: 10px;
  justify-items: start;
  border: 3px solid transparent;
  border-radius: var(--radius);
  padding: 12px;
  color: var(--ink);
  background: #ffffff;
  box-shadow: 0 10px 22px rgba(36, 48, 74, 0.08);
  text-align: left;
}

.theme-card.active {
  border-color: var(--blue);
}

.theme-art {
  display: grid;
  width: 100%;
  aspect-ratio: 16 / 9;
  place-items: center;
  border-radius: 10px;
  overflow: hidden;
  font-size: 3.2rem;
}

.theme-art img {
  width: min(74%, 150px);
  height: min(80%, 116px);
  object-fit: contain;
}

.theme-art img.marvel {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: left center;
}

.theme-card strong {
  font-size: 1.1rem;
  font-weight: 950;
}

.theme-card small {
  color: var(--muted);
  font-weight: 800;
}

.name-panel {
  display: grid;
  grid-template-columns: auto minmax(220px, 1fr) auto;
  gap: 16px;
  align-items: end;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 18px;
  background: rgba(255, 255, 255, 0.78);
}

@media (max-width: 780px) {
  .create-header,
  .name-panel {
    display: grid;
  }
}
</style>
