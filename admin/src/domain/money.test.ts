import { describe, expect, it } from 'vitest'
import { isMoney, moneyError } from './money'

describe('money', () => {
  it('accepts decimal strings and rejects calculation-prone values', () => {
    expect(isMoney('0')).toBe(true)
    expect(isMoney('19.90')).toBe(true)
    expect(isMoney('99999999.99')).toBe(true)
    expect(isMoney('')).toBe(false)
    expect(isMoney('', false)).toBe(true)
    expect(isMoney('-1')).toBe(false)
    expect(isMoney('1.239')).toBe(false)
    expect(isMoney('10.1.1')).toBe(false)
    expect(moneyError('1.239', '当前价格')).toContain('最多两位小数')
  })
})
