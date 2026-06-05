import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import DashboardView from '@/views/DashboardView.vue'
import QuizView from '@/views/QuizView.vue'
import ResultView from '@/views/ResultView.vue'
import ReportView from '@/views/ReportView.vue'
import AdminCatalogView from '@/views/AdminCatalogView.vue'
import AdminMonitoringView from '@/views/AdminMonitoringView.vue'
import AdminAttemptEditView from '@/views/AdminAttemptEditView.vue'
import AdminRewardCatalogView from '@/views/AdminRewardCatalogView.vue'
import AlphabetView from '@/views/AlphabetView.vue'
import NumberPracticeView from '@/views/NumberPracticeView.vue'
import RewardAlbumPage from '@/views/rewards/RewardAlbumPage.vue'
import RewardCreatePicturePage from '@/views/rewards/RewardCreatePicturePage.vue'
import RewardPictureEditorPage from '@/views/rewards/RewardPictureEditorPage.vue'
import { isAdminUser } from '@/services/admin'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/app' },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/app', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
    { path: '/alphabet', name: 'alphabet', component: AlphabetView, meta: { requiresAuth: true } },
    { path: '/numbers', name: 'numbers', component: NumberPracticeView, meta: { requiresAuth: true } },
    { path: '/rewards/album', name: 'rewardAlbum', component: RewardAlbumPage, meta: { requiresAuth: true } },
    { path: '/rewards/album/new', name: 'rewardAlbumNew', component: RewardCreatePicturePage, meta: { requiresAuth: true } },
    { path: '/rewards/album/:pictureId', name: 'rewardAlbumEdit', component: RewardPictureEditorPage, meta: { requiresAuth: true } },
    { path: '/report', name: 'report', component: ReportView, meta: { requiresAuth: true } },
    { path: '/admin/monitoring', name: 'adminMonitoring', component: AdminMonitoringView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/monitoring/attempts/:attemptId', name: 'adminAttemptEdit', component: AdminAttemptEditView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/catalog', name: 'adminCatalog', component: AdminCatalogView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/reward-catalog', name: 'adminRewardCatalog', component: AdminRewardCatalogView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/quiz/:attemptId', name: 'quiz', component: QuizView, meta: { requiresAuth: true } },
    { path: '/result/:attemptId', name: 'result', component: ResultView, meta: { requiresAuth: true } }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (auth.token && !auth.user) {
    try {
      await auth.loadMe()
    } catch {
      auth.logout()
    }
  }

  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && !isAdminUser(auth.user)) {
    return { name: 'dashboard' }
  }

  if ((to.name === 'login' || to.name === 'register') && auth.token) {
    return { name: 'dashboard' }
  }
})

export default router
