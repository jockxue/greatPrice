import { mockHotKeywords } from '@/mock/search'
import type { DealDetail, DealSummary } from '@/types/deal'

/**
 * 好价相关的前端业务规则。列表、详情、分类数据直接调用 @/api，这里只放需要判断或组合的逻辑。
 */

const HTTPS_URL = /^https:\/\/[^\s/?#]+[^\s]*$/

/** 演示数据需要在界面上明确提示 */
export function isDemoDeal(deal: Pick<DealSummary, 'sourceType'>): boolean {
  return deal.sourceType === 'DEMO'
}

/** 优先使用后端配置的推广链接，其次普通购买链接；都没有或不是合法 https 地址时返回 undefined，不允许跳转 */
export function getBuyUrl(deal: Pick<DealDetail, 'promotionUrl' | 'purchaseUrl'>): string | undefined {
  return [deal.promotionUrl, deal.purchaseUrl].find((url): url is string => !!url && HTTPS_URL.test(url))
}

/** 热门搜索词暂无后端接口，使用标注为“示例”的演示数据 */
export async function listHotKeywords(): Promise<string[]> {
  return [...mockHotKeywords]
}
