import type { PageQuery } from '@/types/api'

/** ISO 4217 货币代码，首期只支持人民币。 */
export type CurrencyCode = 'CNY'

/** 信息来源类型；DEMO 为演示数据，界面上需要明确提示 */
export type DealSourceType = 'DEMO' | 'EDITOR' | 'USER_SUBMISSION' | 'AFFILIATE'

/** GET /api/v1/categories 列表项 */
export interface Category {
  id: number
  name: string
  code: string
  /** 图标 URL；未配置时为 null，界面显示文字图标 */
  icon: string | null
}

/** 服务端排序白名单，缺省为 LATEST（发布时间倒序） */
export type DealSortBy = 'LATEST' | 'PRICE_ASC' | 'PRICE_DESC'

/** GET /api/v1/deals 查询参数 */
export interface DealQuery extends PageQuery {
  categoryId?: number
  keyword?: string
  /** true 时只返回编辑精选 */
  featured?: boolean
  sortBy?: DealSortBy
}

/**
 * 好价列表项，与后端 DealSummaryResponse 对应。
 * 金额为保留两位小数的字符串（如 "89.90"），时间为带时区偏移的 ISO-8601 字符串，可空字段为 null。
 */
export interface DealSummary {
  id: number
  title: string
  brand: string | null
  imageUrl: string | null
  categoryId: number
  categoryName: string | null
  currentPrice: string
  /** 参考价，仅用于展示，不代表经过核验的优惠幅度 */
  originalPrice: string | null
  currency: CurrencyCode
  dealDescription: string
  sourceType: DealSourceType
  sourceName: string
  expiresAt: string | null
  /** 最近核验时间；未核验时为 null */
  verifiedAt: string | null
  publishedAt: string
}

/** GET /api/v1/deals/{id}，与后端 DealDetailResponse 对应 */
export interface DealDetail extends DealSummary {
  productId: number
  specification: string | null
  description: string | null
  startsAt: string | null
  /** 普通购买链接，后端只返回合法的 https 地址 */
  purchaseUrl: string | null
  /** 推广链接，由后端生成；前端不得拼接或伪造 */
  promotionUrl: string | null
}
