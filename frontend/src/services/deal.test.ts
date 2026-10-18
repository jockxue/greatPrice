import { describe, expect, it } from 'vitest'
import { mockCategories } from '@/mock/categories'
import { mockDeals } from '@/mock/deals'
import { getDealById, listDeals, listFeaturedDeals, searchDeals } from '@/services/deal'

const ids = (deals: { id: number }[]) => deals.map((d) => d.id)

describe('演示数据完整性', () => {
  it('id 唯一', () => {
    expect(new Set(ids(mockDeals)).size).toBe(mockDeals.length)
  })

  it('价格为正数，原价存在时高于现价', () => {
    for (const deal of mockDeals) {
      expect(typeof deal.price).toBe('number')
      expect(deal.price).toBeGreaterThan(0)
      if (deal.originalPrice !== undefined) {
        expect(deal.originalPrice).toBeGreaterThan(deal.price)
      }
    }
  })

  it('分类存在、时间可解析、不包含伪造的购买链接', () => {
    const categoryIds = new Set(mockCategories.map((c) => c.id))
    for (const deal of mockDeals) {
      expect(categoryIds.has(deal.categoryId)).toBe(true)
      expect(Number.isNaN(Date.parse(deal.publishedAt))).toBe(false)
      if (deal.verifiedAt) {
        expect(Number.isNaN(Date.parse(deal.verifiedAt))).toBe(false)
      }
      expect(deal.purchaseUrl).toBeUndefined()
    }
  })
})

describe('listDeals', () => {
  it('不传分类返回全部，按发布时间倒序', async () => {
    const deals = await listDeals()
    expect(deals).toHaveLength(mockDeals.length)
    const times = deals.map((d) => Date.parse(d.publishedAt))
    expect(times).toEqual([...times].sort((a, b) => b - a))
  })

  it('按分类筛选', async () => {
    const deals = await listDeals(1)
    expect(deals.length).toBeGreaterThan(0)
    expect(deals.every((d) => d.categoryId === 1)).toBe(true)
  })

  it('没有商品的分类返回空列表', async () => {
    expect(await listDeals(6)).toEqual([])
  })
})

describe('listFeaturedDeals', () => {
  it('只返回精选', async () => {
    const deals = await listFeaturedDeals()
    expect(deals.length).toBeGreaterThan(0)
    expect(deals.every((d) => d.featured)).toBe(true)
  })
})

describe('getDealById', () => {
  it('找到商品', async () => {
    expect((await getDealById(1001))?.brand).toBe('声途')
  })

  it('不存在时返回 undefined', async () => {
    expect(await getDealById(999999)).toBeUndefined()
  })
})

describe('searchDeals', () => {
  it('匹配商品名称', async () => {
    expect(ids(await searchDeals('耳机'))).toEqual([1001])
  })

  it('匹配品牌', async () => {
    expect(ids(await searchDeals('露颜')).sort()).toEqual([1007, 1008])
  })

  it('匹配分类名称', async () => {
    const deals = await searchDeals('运动')
    expect(deals.length).toBeGreaterThan(0)
    expect(deals.every((d) => d.categoryId === 5)).toBe(true)
  })

  it('忽略大小写和首尾空格', async () => {
    expect(ids(await searchDeals('  air3 '))).toEqual([1001])
  })

  it('无结果返回空列表', async () => {
    expect(await searchDeals('不存在的商品xyz')).toEqual([])
  })

  it('空关键词返回空列表', async () => {
    expect(await searchDeals('   ')).toEqual([])
  })
})
