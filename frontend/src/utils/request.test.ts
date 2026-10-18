import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const env = vi.hoisted(() => ({ apiBaseUrl: 'http://localhost:8080', isDev: false }))
vi.mock('@/config/env', () => ({ env }))

import { fetchDealDetail, fetchDeals } from '@/api/deal'
import { ApiError, cleanParams, http } from '@/utils/request'

type RequestOptions = {
  url: string
  method: string
  data?: unknown
  success: (res: { statusCode: number; data: unknown }) => void
  fail: (err: { errMsg: string }) => void
}

const uniRequest = vi.fn<(options: RequestOptions) => void>()

function respond(statusCode: number, data: unknown) {
  uniRequest.mockImplementationOnce((options) => options.success({ statusCode, data }))
}

beforeEach(() => {
  env.apiBaseUrl = 'http://localhost:8080'
  env.isDev = true
  vi.stubGlobal('uni', { request: uniRequest })
  vi.spyOn(console, 'log').mockImplementation(() => {})
})

afterEach(() => {
  uniRequest.mockReset()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('request', () => {
  it('成功时只返回 data', async () => {
    respond(200, { code: 0, message: '成功', data: { id: 1 } })
    await expect(http.get('/api/v1/x')).resolves.toEqual({ id: 1 })
    expect(uniRequest.mock.calls[0][0].url).toBe('http://localhost:8080/api/v1/x')
  })

  it('业务错误抛出 ApiError，携带后端 code 和 message', async () => {
    respond(404, { code: 40401, message: '好价已下架或已过期', data: null })
    const error = await http.get('/api/v1/deals/7').catch((e) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ code: 40401, statusCode: 404, message: '好价已下架或已过期' })
  })

  it('HTTP 错误但响应体不是统一格式时提示服务异常', async () => {
    respond(502, '<html>Bad Gateway</html>')
    await expect(http.get('/api/v1/x')).rejects.toMatchObject({ code: -1, statusCode: 502, message: '服务响应异常（HTTP 502）' })
  })

  it('网络失败和超时给出可读提示', async () => {
    uniRequest.mockImplementationOnce((options) => options.fail({ errMsg: 'request:fail timeout' }))
    await expect(http.get('/api/v1/x')).rejects.toMatchObject({ code: -1, message: '网络请求超时，请稍后重试' })
    uniRequest.mockImplementationOnce((options) => options.fail({ errMsg: 'request:fail' }))
    await expect(http.get('/api/v1/x')).rejects.toMatchObject({ code: -1, message: '网络异常，请检查网络后重试' })
  })

  it('未配置 Base URL 时不发请求', async () => {
    env.apiBaseUrl = ''
    await expect(http.get('/api/v1/x')).rejects.toBeInstanceOf(ApiError)
    expect(uniRequest).not.toHaveBeenCalled()
  })

  it('生产环境拒绝非 https 的 Base URL', async () => {
    env.isDev = false
    await expect(http.get('/api/v1/x')).rejects.toMatchObject({ message: '生产环境 VITE_API_BASE_URL 必须使用 https' })
    expect(uniRequest).not.toHaveBeenCalled()
  })

  it('开发环境输出请求日志，生产环境不输出', async () => {
    respond(200, { code: 0, message: '成功', data: null })
    await http.get('/api/v1/x')
    expect(console.log).toHaveBeenCalled()

    vi.mocked(console.log).mockClear()
    env.isDev = false
    env.apiBaseUrl = 'https://api.example.com'
    respond(200, { code: 0, message: '成功', data: null })
    await http.get('/api/v1/x')
    expect(console.log).not.toHaveBeenCalled()
  })
})

describe('cleanParams', () => {
  it('去掉 undefined / null / 空字符串，保留 0 和 false', () => {
    expect(cleanParams({ a: undefined, b: null, c: '', d: 0, e: false, f: 'x' })).toEqual({ d: 0, e: false, f: 'x' })
  })
})

describe('api/deal', () => {
  it('列表查询参数透传到 GET 请求，省略未设置的条件', async () => {
    respond(200, { code: 0, message: '成功', data: { records: [], total: 0, page: 1, size: 10 } })
    await fetchDeals({ page: 2, size: 10, categoryId: undefined, keyword: '耳机' })
    const options = uniRequest.mock.calls[0][0]
    expect(options.url).toBe('http://localhost:8080/api/v1/deals')
    expect(options.method).toBe('GET')
    expect(options.data).toEqual({ page: 2, size: 10, keyword: '耳机' })
  })

  it('详情使用路径参数', async () => {
    respond(200, { code: 0, message: '成功', data: { id: 3 } })
    await fetchDealDetail(3)
    expect(uniRequest.mock.calls[0][0].url).toBe('http://localhost:8080/api/v1/deals/3')
  })
})
