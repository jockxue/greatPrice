import { describe, expect, it } from 'vitest'
import { getBuyUrl, isDemoDeal, listHotKeywords } from '@/services/deal'

describe('getBuyUrl', () => {
  it('优先使用推广链接', () => {
    expect(getBuyUrl({ promotionUrl: 'https://union.example.com/p', purchaseUrl: 'https://item.example.com/1' })).toBe(
      'https://union.example.com/p',
    )
  })

  it('没有推广链接时使用普通购买链接', () => {
    expect(getBuyUrl({ promotionUrl: null, purchaseUrl: 'https://item.example.com/1' })).toBe('https://item.example.com/1')
  })

  it('没有链接时不允许跳转', () => {
    expect(getBuyUrl({ promotionUrl: null, purchaseUrl: null })).toBeUndefined()
    expect(getBuyUrl({ promotionUrl: '', purchaseUrl: '' })).toBeUndefined()
  })

  it('拒绝非 https 或格式不合法的链接', () => {
    for (const url of ['http://item.example.com/1', 'javascript:alert(1)', 'https://', 'https:// bad.com', '//item.example.com']) {
      expect(getBuyUrl({ promotionUrl: null, purchaseUrl: url })).toBeUndefined()
    }
    expect(getBuyUrl({ promotionUrl: 'javascript:alert(1)', purchaseUrl: 'https://item.example.com/1' })).toBe(
      'https://item.example.com/1',
    )
  })
})

describe('isDemoDeal', () => {
  it('只有 DEMO 来源视为演示数据', () => {
    expect(isDemoDeal({ sourceType: 'DEMO' })).toBe(true)
    expect(isDemoDeal({ sourceType: 'EDITOR' })).toBe(false)
  })
})

describe('listHotKeywords', () => {
  it('返回示例热门词的副本', async () => {
    const first = await listHotKeywords()
    first.push('x')
    expect(await listHotKeywords()).not.toContain('x')
  })
})
