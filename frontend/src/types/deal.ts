/** ISO 4217 货币代码，首期只支持人民币。 */
export type CurrencyCode = 'CNY'

export interface Category {
  id: number
  name: string
}

export interface DealSpec {
  label: string
  value: string
}

/**
 * 好价信息。字段与第三阶段后端接口保持一致：
 * 价格为数字（元），时间为带时区偏移的 ISO-8601 字符串。
 */
export interface Deal {
  id: number
  title: string
  brand: string
  /** 商品主图 URL；缺省时界面显示统一占位图 */
  image?: string
  price: number
  originalPrice?: number
  currency: CurrencyCode
  categoryId: number
  sourceName: string
  dealDescription: string
  /** 编辑最近一次核验价格的时间；未核验时缺省 */
  verifiedAt?: string
  publishedAt: string
  /** 购买链接，由后端生成；前端不得拼接或伪造 */
  purchaseUrl?: string
  tags?: string[]
  /** 规格或适用条件 */
  specs?: DealSpec[]
  /** 是否进入首页精选 */
  featured?: boolean
}
