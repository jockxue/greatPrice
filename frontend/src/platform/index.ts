/**
 * 平台适配层：页面和业务代码通过这里调用与平台相关的能力。
 * 需要区分微信小程序 / H5 / App 的实现时，在本目录内使用条件编译（#ifdef MP-WEIXIN 等），不要散落到页面中。
 */

export function showToast(title: string) {
  uni.showToast({ title, icon: 'none' })
}

/**
 * 打开站外购买链接，调用方需先确认是合法的 https 地址。
 * H5 新开窗口；小程序不能直接打开外部网页（web-view 只能打开已配置且归属本主体的业务域名），改为复制链接。
 */
export function openExternalUrl(url: string) {
  // #ifdef H5
  window.open(url, '_blank', 'noopener')
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: url,
    success: () => {
      uni.showModal({
        title: '链接已复制',
        content: '请粘贴到浏览器或对应购物 App 中打开，价格以下单页面为准',
        showCancel: false,
        confirmText: '知道了',
      })
    },
  })
  // #endif
}

export interface NavBarMetrics {
  /** 系统状态栏高度（px），自定义导航栏需要为其留白 */
  statusBarHeight: number
  /** 导航栏内容区高度（px），微信小程序中与右上角胶囊按钮垂直对齐 */
  navBarHeight: number
  /** 右侧需要为胶囊按钮预留的宽度（px），非小程序为 0 */
  capsuleReservedWidth: number
}

const DEFAULT_NAV_BAR_HEIGHT = 44

/** 用于 navigationStyle: custom 的页面计算自定义导航栏尺寸。 */
export function getNavBarMetrics(): NavBarMetrics {
  const { statusBarHeight = 0, windowWidth } = uni.getWindowInfo()
  const metrics: NavBarMetrics = {
    statusBarHeight,
    navBarHeight: DEFAULT_NAV_BAR_HEIGHT,
    capsuleReservedWidth: 0,
  }
  // #ifdef MP-WEIXIN
  const capsule = uni.getMenuButtonBoundingClientRect()
  if (capsule && capsule.height) {
    metrics.navBarHeight = (capsule.top - statusBarHeight) * 2 + capsule.height
    metrics.capsuleReservedWidth = windowWidth - capsule.left
  }
  // #endif
  return metrics
}
