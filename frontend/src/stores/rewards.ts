import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/services/api'
import { useQuizStore } from '@/stores/quiz'
import type {
  AlbumPictureDetail,
  AlbumPictureSummary,
  PlacedRewardItem,
  PurchaseRewardItemResponse,
  RemoveRewardItemResponse,
  RewardBalanceResponse,
  RewardCatalogItem,
  RewardCatalogResponse,
  RewardTheme
} from '@/types'

export const useRewardsStore = defineStore('rewards', () => {
  type RewardToastType = 'success' | 'info' | 'error'

  const themes = ref<RewardTheme[]>([])
  const items = ref<RewardCatalogItem[]>([])
  const pictures = ref<AlbumPictureSummary[]>([])
  const currentPicture = ref<AlbumPictureDetail | null>(null)
  const balance = ref<RewardBalanceResponse>({ earned: 0, spent: 0, available: 0 })
  const loading = ref(false)
  const toast = ref<{ message: string, type: RewardToastType } | null>(null)
  let toastTimer: number | null = null

  const itemsById = computed(() => new Map(items.value.map((item) => [item.id, item])))
  const themesById = computed(() => new Map(themes.value.map((theme) => [theme.id, theme])))

  async function bootstrap() {
    loading.value = true
    try {
      await Promise.all([fetchCatalog(), fetchBalance(), fetchPictures()])
    } finally {
      loading.value = false
    }
  }

  async function fetchCatalog() {
    const response = await api.get<RewardCatalogResponse>('/rewards/catalog')
    themes.value = response.themes
    items.value = response.items
  }

  async function fetchBalance() {
    balance.value = await api.get<RewardBalanceResponse>('/rewards/balance')
    useQuizStore().totalCrystals = balance.value.available
  }

  async function fetchPictures() {
    pictures.value = await api.get<AlbumPictureSummary[]>('/rewards/pictures')
  }

  async function createPicture(themeId: string, name: string) {
    currentPicture.value = await api.post<AlbumPictureDetail>('/rewards/pictures', {
      themeId,
      name: name.trim()
    })
    await fetchPictures()
    return currentPicture.value
  }

  async function fetchPicture(pictureId: string) {
    currentPicture.value = await api.get<AlbumPictureDetail>(`/rewards/pictures/${pictureId}`)
    return currentPicture.value
  }

  async function savePicture(options?: { toastMessage?: string | null }) {
    if (!currentPicture.value) {
      throw new Error('Няма отворена картина.')
    }
    currentPicture.value = await api.put<AlbumPictureDetail>(`/rewards/pictures/${currentPicture.value.id}`, {
      name: currentPicture.value.name,
      placedItems: currentPicture.value.placedItems
    })
    await fetchBalance()
    await fetchPictures()
    const toastMessage = options?.toastMessage === undefined ? 'Картината е запазена.' : options.toastMessage
    if (toastMessage) {
      showToast(toastMessage, 'success')
    }
    return currentPicture.value
  }

  async function purchaseItem(itemId: string) {
    if (!currentPicture.value) {
      throw new Error('Отвори картина, за да добавяш предмети.')
    }
    const response = await api.post<PurchaseRewardItemResponse>(`/rewards/pictures/${currentPicture.value.id}/items`, {
      itemId
    })
    balance.value = response.balance
    useQuizStore().totalCrystals = response.balance.available
    currentPicture.value = response.picture
    await fetchPictures()
    showToast(response.message)
    return response.placedItem
  }

  async function deletePicture(pictureId: string) {
    await api.delete<void>(`/rewards/pictures/${pictureId}`)
    pictures.value = pictures.value.filter((picture) => picture.id !== pictureId)
    if (currentPicture.value?.id === pictureId) {
      currentPicture.value = null
    }
  }

  function setPictureName(name: string) {
    if (!currentPicture.value) {
      return
    }
    currentPicture.value = {
      ...currentPicture.value,
      name: name.slice(0, 40)
    }
  }

  function moveItem(itemId: string, x: number, y: number) {
    updatePlacedItem(itemId, (item) => ({
      ...item,
      x: clamp(x, 0, 100),
      y: clamp(y, 0, 100)
    }))
  }

  async function removeItem(itemId: string) {
    if (!currentPicture.value) {
      throw new Error('Отвори картина, за да махаш предмети.')
    }
    const response = await api.delete<RemoveRewardItemResponse>(`/rewards/pictures/${currentPicture.value.id}/items/${itemId}`)
    balance.value = response.balance
    useQuizStore().totalCrystals = response.balance.available
    currentPicture.value = response.picture
    await fetchPictures()
    showToast(response.message, response.refundedCrystals > 0 ? 'success' : 'info')
    return response
  }

  function resizeItem(itemId: string, direction: 'up' | 'down') {
    const placed = currentPicture.value?.placedItems.find((item) => item.id === itemId)
    if (!placed) {
      return
    }
    const catalogItem = itemsById.value.get(placed.catalogItemId)
    const min = catalogItem?.minScale ?? 0.35
    const max = catalogItem?.maxScale ?? 3
    const delta = direction === 'up' ? 0.15 : -0.15
    updatePlacedItem(itemId, (item) => ({
      ...item,
      scale: clamp(Number((item.scale + delta).toFixed(2)), min, max)
    }))
  }

  function rotateItem(itemId: string, direction: 'left' | 'right') {
    const delta = direction === 'right' ? 10 : -10
    updatePlacedItem(itemId, (item) => ({
      ...item,
      rotation: clamp(item.rotation + delta, -180, 180)
    }))
  }

  function toggleMirror(itemId: string) {
    updatePlacedItem(itemId, (item) => ({
      ...item,
      mirrored: !item.mirrored
    }))
  }

  function bringForward(itemId: string) {
    shiftZIndex(itemId, 1)
  }

  function sendBackward(itemId: string) {
    shiftZIndex(itemId, -1)
  }

  function itemsForTheme(themeId: string) {
    return items.value.filter((item) => (item.themeIds?.length ? item.themeIds : [item.themeId]).includes(themeId))
  }

  function themeById(themeId: string) {
    return themesById.value.get(themeId)
  }

  function showToast(message: string, type: RewardToastType = 'success') {
    const normalizedMessage = message.trim()
    if (!normalizedMessage) {
      return
    }
    toast.value = { message: normalizedMessage, type }
    if (toastTimer !== null) {
      window.clearTimeout(toastTimer)
    }
    toastTimer = window.setTimeout(() => {
      if (toast.value?.message === normalizedMessage) {
        toast.value = null
      }
      toastTimer = null
    }, 5000)
  }

  function updatePlacedItem(itemId: string, updater: (item: PlacedRewardItem) => PlacedRewardItem) {
    if (!currentPicture.value) {
      return
    }
    currentPicture.value = {
      ...currentPicture.value,
      placedItems: currentPicture.value.placedItems.map((item) => item.id === itemId ? updater(item) : item)
    }
  }

  function shiftZIndex(itemId: string, delta: number) {
    if (!currentPicture.value) {
      return
    }
    const itemsCopy = currentPicture.value.placedItems
      .map((item) => ({ ...item }))
      .sort((a, b) => a.zIndex - b.zIndex)
    const index = itemsCopy.findIndex((item) => item.id === itemId)
    if (index < 0) {
      return
    }
    const targetIndex = clamp(index + delta, 0, itemsCopy.length - 1)
    const [selected] = itemsCopy.splice(index, 1)
    itemsCopy.splice(targetIndex, 0, selected)
    currentPicture.value = {
      ...currentPicture.value,
      placedItems: itemsCopy.map((item, index) => ({
        ...item,
        zIndex: index + 1
      }))
    }
  }

  function clamp(value: number, min: number, max: number) {
    return Math.max(min, Math.min(max, value))
  }

  return {
    themes,
    items,
    pictures,
    currentPicture,
    balance,
    loading,
    toast,
    itemsById,
    bootstrap,
    fetchCatalog,
    fetchBalance,
    fetchPictures,
    createPicture,
    fetchPicture,
    savePicture,
    purchaseItem,
    deletePicture,
    setPictureName,
    moveItem,
    removeItem,
    resizeItem,
    rotateItem,
    toggleMirror,
    bringForward,
    sendBackward,
    itemsForTheme,
    themeById,
    showToast
  }
})
