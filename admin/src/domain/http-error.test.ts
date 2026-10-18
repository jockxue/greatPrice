import { describe, expect, it } from 'vitest'
import { parseApiFailure } from './http-error'

describe('parseApiFailure', () => {
  it('maps network failure and expired login', () => {
    expect(parseApiFailure(undefined, undefined, true)).toMatchObject({ code: -1, unauthorized: false })
    expect(parseApiFailure(401, { code: 40100, message: '未登录或登录已过期' }, false)).toMatchObject({
      unauthorized: true,
      message: '未登录或登录已过期',
    })
    expect(parseApiFailure(400, { code: 40000, message: '当前价格格式不正确' }, false).unauthorized).toBe(false)
  })
})
