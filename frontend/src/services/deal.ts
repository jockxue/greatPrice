import { mockCategories } from '@/mock/categories'
import { mockDeals } from '@/mock/deals'
import { mockHotKeywords } from '@/mock/search'
import type { Category, Deal } from '@/types/deal'

/**
 * 好价业务服务。页面只调用这里的函数，不直接引用 mock。
 * 第二阶段由演示数据实现，函数均返回 Promise；第三阶段把函数体替换为 @/api 中的接口调用，
 * 列表类函数届时需增加分页参数并返回 PageResult。
 */

const byPublishedDesc = (a: Deal, b: Deal) => Date.parse(b.publishedAt) - Date.parse(a.publishedAt)

export async function listCategories(): Promise<Category[]> {
  return [...mockCategories]
}

/** 不传 categoryId 时返回全部，按发布时间倒序。 */
export async function listDeals(categoryId?: number): Promise<Deal[]> {
  return mockDeals
    .filter((deal) => categoryId === undefined || deal.categoryId === categoryId)
    .sort(byPublishedDesc)
}

export async function listFeaturedDeals(): Promise<Deal[]> {
  return mockDeals.filter((deal) => deal.featured).sort(byPublishedDesc)
}

export async function getDealById(id: number): Promise<Deal | undefined> {
  return mockDeals.find((deal) => deal.id === id)
}

/** 关键词匹配商品名称、品牌、分类名称，忽略大小写和首尾空格；空关键词返回空列表。 */
export async function searchDeals(keyword: string): Promise<Deal[]> {
  const normalized = keyword.trim().toLowerCase()
  if (!normalized) {
    return []
  }
  const categoryNames = new Map(mockCategories.map((c) => [c.id, c.name]))
  return mockDeals
    .filter((deal) =>
      [deal.title, deal.brand, categoryNames.get(deal.categoryId) ?? ''].some((text) =>
        text.toLowerCase().includes(normalized),
      ),
    )
    .sort(byPublishedDesc)
}

export async function listHotKeywords(): Promise<string[]> {
  return [...mockHotKeywords]
}
