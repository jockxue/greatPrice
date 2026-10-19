import { createRouter, createWebHistory } from 'vue-router'
import { redirectTarget } from '@/domain/deal'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true, title: '登录' } },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '仪表盘' } },
        { path: 'products', component: () => import('@/views/product/ProductListView.vue'), meta: { title: '商品管理' } },
        { path: 'products/create', component: () => import('@/views/product/ProductFormView.vue'), meta: { title: '新建商品' } },
        { path: 'products/:id', component: () => import('@/views/product/ProductDetailView.vue'), meta: { title: '商品详情' } },
        { path: 'products/:id/edit', component: () => import('@/views/product/ProductFormView.vue'), meta: { title: '编辑商品' } },
        { path: 'deals', component: () => import('@/views/deal/DealListView.vue'), meta: { title: '好价管理' } },
        { path: 'deals/create', component: () => import('@/views/deal/DealFormView.vue'), meta: { title: '录入好价' } },
        { path: 'deals/:id', component: () => import('@/views/deal/DealDetailView.vue'), meta: { title: '好价详情' } },
        { path: 'deals/:id/edit', component: () => import('@/views/deal/DealFormView.vue'), meta: { title: '编辑好价' } },
        { path: 'reviews', component: () => import('@/views/review/ReviewListView.vue'), meta: { title: '审核管理' } },
        { path: 'reviews/:id', component: () => import('@/views/deal/DealDetailView.vue'), meta: { title: '审核详情' } },
        { path: 'price-history', component: () => import('@/views/price/PriceHistoryView.vue'), meta: { title: '价格核验' } },
        { path: 'taobao', component: () => import('@/views/taobao/TaobaoStatusView.vue'), meta: { title: '淘宝联盟' } },
        { path: 'taobao/pick', component: () => import('@/views/taobao/TaobaoPickView.vue'), meta: { title: '淘宝选品' } },
        { path: 'taobao/jobs', component: () => import('@/views/taobao/TaobaoJobView.vue'), meta: { title: '同步记录' } },
        { path: 'audit-logs', component: () => import('@/views/audit/AuditLogView.vue'), meta: { title: '操作审计', requiresAdmin: true } },
        { path: 'users', component: () => import('@/views/user/UserListView.vue'), meta: { title: '账号管理', requiresAdmin: true } },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.loaded) {
    await auth.fetchMe()
  }
  const target = redirectTarget(Boolean(to.meta.public), auth.user?.role ?? null, Boolean(to.meta.requiresAdmin))
  if (target === '/login') {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (target) {
    return target
  }
  if (to.path === '/login' && auth.user) {
    return '/dashboard'
  }
  return true
})

export default router
