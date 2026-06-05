<script setup lang="ts">
import { onMounted } from 'vue'
import { Eye, ImagePlus, Images, Trash2 } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import RewardBalanceBadge from '@/components/rewards/RewardBalanceBadge.vue'
import RewardScenePreview from '@/components/rewards/RewardScenePreview.vue'
import { useRewardsStore } from '@/stores/rewards'

const rewards = useRewardsStore()
const router = useRouter()

onMounted(() => {
  rewards.bootstrap().catch(() => {
    rewards.showToast('Албумът не се зареди. Опитай пак.', 'error')
  })
})

async function deletePicture(pictureId: string) {
  if (!window.confirm('Да изтрием ли тази картина? Кристалите за предметите не се връщат.')) {
    return
  }
  await rewards.deletePicture(pictureId)
}

function itemCountLabel(count: number) {
  return count === 1 ? '1 предмет' : `${count} предмета`
}
</script>

<template>
  <section class="page album-page">
    <section class="panel album-hero">
      <div>
        <p class="eyebrow">Албум</p>
        <h1>Моят албум</h1>
        <p>Събирай кристали, добавяй предмети и прави свои светове.</p>
      </div>
      <div class="album-hero-actions">
        <RewardBalanceBadge :crystals="rewards.balance.available" />
        <RouterLink class="button" :to="{ name: 'rewardAlbumNew' }">
          <ImagePlus :size="22" />
          <span>Нова картина</span>
        </RouterLink>
      </div>
    </section>

    <section class="panel album-list-panel">
      <div class="panel-header">
        <h2 class="panel-title">Моите картини</h2>
        <Images :size="24" />
      </div>

      <div v-if="rewards.pictures.length === 0" class="empty-album">
        <strong>Още няма картини.</strong>
        <RouterLink class="button" :to="{ name: 'rewardAlbumNew' }">
          <ImagePlus :size="20" />
          <span>Създай първата</span>
        </RouterLink>
      </div>

      <div v-else class="album-grid">
        <article v-for="picture in rewards.pictures" :key="picture.id" class="album-card">
          <RewardScenePreview :background="picture.backgroundImage" :items="picture.previewItems" :label="picture.name" />
          <div class="album-card-body">
            <div>
              <strong>{{ picture.name }}</strong>
              <span>{{ picture.themeName }} · {{ itemCountLabel(picture.itemCount) }}</span>
            </div>
            <div class="album-card-actions">
              <button class="button compact-button" type="button" @click="router.push({ name: 'rewardAlbumEdit', params: { pictureId: picture.id } })">
                <Eye :size="18" />
                <span>Отвори</span>
              </button>
              <button class="button secondary compact-button danger-action" type="button" @click="deletePicture(picture.id)">
                <Trash2 :size="18" />
                <span>Изтрий</span>
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.album-page {
  display: grid;
  gap: 20px;
}

.album-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
}

.album-hero h1,
.album-hero p {
  margin: 0;
}

.album-hero h1 {
  font-size: clamp(2.2rem, 6vw, 4.2rem);
  line-height: 1;
}

.album-hero p:not(.eyebrow) {
  margin-top: 10px;
  color: var(--muted);
  font-weight: 800;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--green-dark);
  font-weight: 950;
  text-transform: uppercase;
}

.album-hero-actions {
  display: grid;
  gap: 12px;
  justify-items: end;
}

.album-hero-actions a {
  text-decoration: none;
}

.album-list-panel {
  padding-bottom: 22px;
}

.empty-album {
  display: grid;
  justify-items: center;
  gap: 16px;
  padding: 48px 20px;
  color: var(--muted);
  text-align: center;
}

.empty-album strong {
  color: var(--ink);
  font-size: 1.5rem;
}

.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  padding: 0 20px 20px;
}

.album-card {
  display: grid;
  gap: 12px;
  border: 1px solid rgba(36, 48, 74, 0.12);
  border-radius: var(--radius);
  padding: 12px;
  background: #ffffff;
  box-shadow: 0 12px 26px rgba(36, 48, 74, 0.08);
}

.album-card-body {
  display: grid;
  gap: 12px;
}

.album-card-body > div:first-child strong,
.album-card-body > div:first-child span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.album-card-body > div:first-child strong {
  color: var(--ink);
  font-size: 1.15rem;
  font-weight: 950;
}

.album-card-body > div:first-child span {
  color: var(--muted);
  font-weight: 800;
}

.album-card-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.compact-button {
  min-height: 40px;
  padding: 0 12px;
  font-weight: 900;
  text-decoration: none;
}

.danger-action {
  color: var(--danger);
}

@media (max-width: 760px) {
  .album-hero {
    display: grid;
  }

  .album-hero-actions {
    justify-items: start;
  }
}
</style>
