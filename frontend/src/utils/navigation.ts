export const ROUTES = {
  home: '/pages/index/index',
  category: '/pages/category/index',
  mine: '/pages/mine/index',
  search: '/pages/search/index',
  dealDetail: '/pages/deal/detail',
  submit: '/pages/submit/index',
} as const

export function goDealDetail(id: number) {
  uni.navigateTo({ url: `${ROUTES.dealDetail}?id=${id}` })
}

export function goSearch(keyword?: string) {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
  uni.navigateTo({ url: ROUTES.search + query })
}

/** TabBar 页面只能用 switchTab 跳转，且不能带参数 */
export function goHome() {
  uni.switchTab({ url: ROUTES.home })
}

export function goCategory() {
  uni.switchTab({ url: ROUTES.category })
}
