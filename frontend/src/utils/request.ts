import { env } from '@/config/env'
import { API_ERROR_CODE, type ApiResponse } from '@/types/api'

const DEFAULT_TIMEOUT = 10000

/** 网络失败时 code 为 -1；否则为后端返回的业务错误码。 */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly code: number,
    readonly statusCode?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

type RequestData = UniNamespace.RequestOptions['data']
export type QueryParams = Record<string, string | number | boolean | null | undefined>

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: RequestData
  header?: Record<string, string>
  timeout?: number
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  return typeof value === 'object' && value !== null && 'code' in value
}

/** 去掉值为 undefined / null / 空字符串的查询参数，避免小程序把它们序列化成 "undefined" */
export function cleanParams(params?: QueryParams): Record<string, string | number | boolean> | undefined {
  if (!params) return undefined
  const result: Record<string, string | number | boolean> = {}
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      result[key] = value
    }
  }
  return result
}

function log(...args: unknown[]) {
  if (env.isDev) {
    console.log('[request]', ...args)
  }
}

/**
 * 统一请求入口：拼接 API 根地址、解析统一响应体，成功时只返回 data，失败时抛出 ApiError。
 * 不在这里弹 toast，由调用方决定如何提示用户。
 */
export function request<T>(options: RequestOptions): Promise<T> {
  if (!env.apiBaseUrl) {
    return Promise.reject(new ApiError('VITE_API_BASE_URL 未配置，请检查 frontend/.env.* 文件', API_ERROR_CODE.NETWORK))
  }
  if (!env.isDev && !env.apiBaseUrl.startsWith('https://')) {
    return Promise.reject(new ApiError('生产环境 VITE_API_BASE_URL 必须使用 https', API_ERROR_CODE.NETWORK))
  }
  const method = options.method ?? 'GET'
  const url = env.apiBaseUrl + options.url
  const startedAt = Date.now()
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url,
      method,
      data: options.data,
      header: { 'Content-Type': 'application/json', ...options.header },
      timeout: options.timeout ?? DEFAULT_TIMEOUT,
      success: (res) => {
        const body = res.data
        log(method, url, options.data ?? '', `→ HTTP ${res.statusCode}`, `${Date.now() - startedAt}ms`, body)
        if (!isApiResponse(body)) {
          reject(new ApiError(`服务响应异常（HTTP ${res.statusCode}）`, API_ERROR_CODE.NETWORK, res.statusCode))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body.data as T)
          return
        }
        reject(new ApiError(body.message || `请求失败（HTTP ${res.statusCode}）`, body.code, res.statusCode))
      },
      fail: (err) => {
        log(method, url, options.data ?? '', '→ 失败', err.errMsg)
        const message = /timeout/i.test(err.errMsg ?? '') ? '网络请求超时，请稍后重试' : '网络异常，请检查网络后重试'
        reject(new ApiError(message, API_ERROR_CODE.NETWORK))
      },
    })
  })
}

export const http = {
  get: <T>(url: string, params?: QueryParams) => request<T>({ url, method: 'GET', data: cleanParams(params) }),
  post: <T>(url: string, data?: RequestData) => request<T>({ url, method: 'POST', data }),
  put: <T>(url: string, data?: RequestData) => request<T>({ url, method: 'PUT', data }),
  delete: <T>(url: string, data?: RequestData) => request<T>({ url, method: 'DELETE', data }),
}
