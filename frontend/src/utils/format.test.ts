import { describe, expect, it } from 'vitest'
import { formatAmount, formatDateTime, formatPrice } from '@/utils/format'

describe('formatAmount', () => {
  it('整数不显示小数', () => {
    expect(formatAmount('199.00')).toBe('199')
    expect(formatAmount('0.00')).toBe('0')
    expect(formatAmount('1000.00')).toBe('1000')
  })

  it('小数原样保留两位', () => {
    expect(formatAmount('89.90')).toBe('89.90')
    expect(formatAmount('35.50')).toBe('35.50')
    expect(formatAmount('0.30')).toBe('0.30')
    expect(formatAmount('100.05')).toBe('100.05')
  })
})

describe('formatPrice', () => {
  it('带人民币符号', () => {
    expect(formatPrice('299.00', 'CNY')).toBe('¥299')
    expect(formatPrice('49.90', 'CNY')).toBe('¥49.90')
  })
})

describe('formatDateTime', () => {
  const now = new Date(2026, 9, 18, 12, 0)

  it('同一年显示 月-日 时:分（设备本地时区）', () => {
    const iso = new Date(2026, 9, 18, 9, 5).toISOString()
    expect(formatDateTime(iso, now)).toBe('10-18 09:05')
  })

  it('跨年显示年份', () => {
    const iso = new Date(2025, 11, 31, 23, 59).toISOString()
    expect(formatDateTime(iso, now)).toBe('2025-12-31 23:59')
  })

  it('无法解析时原样返回', () => {
    expect(formatDateTime('not-a-date', now)).toBe('not-a-date')
  })
})
