import { env } from '@/config/env'
import type { ApiResponse } from '@/types/api'

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

/**
 * 统一请求入口：拼接 API 根地址、解析统一响应体，成功时只返回 data，失败时抛出 ApiError。
 * 不在这里弹 toast，由调用方决定如何提示用户。
 */
export function request<T>(options: RequestOptions): Promise<T> {
  if (!env.apiBaseUrl) {
    return Promise.reject(new ApiError('VITE_API_BASE_URL 未配置，请检查 frontend/.env.* 文件', -1))
  }
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: env.apiBaseUrl + options.url,
      method: options.method ?? 'GET',
      data: options.data,
      header: { 'Content-Type': 'application/json', ...options.header },
      timeout: options.timeout ?? DEFAULT_TIMEOUT,
      success: (res) => {
        const body = res.data
        if (!isApiResponse(body)) {
          reject(new ApiError(`响应格式错误（HTTP ${res.statusCode}）`, -1, res.statusCode))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body.data as T)
          return
        }
        reject(new ApiError(body.message || `请求失败（HTTP ${res.statusCode}）`, body.code, res.statusCode))
      },
      fail: (err) => {
        reject(new ApiError(err.errMsg || '网络异常，请稍后重试', -1))
      },
    })
  })
}

export const http = {
  get: <T>(url: string, data?: RequestData) => request<T>({ url, method: 'GET', data }),
  post: <T>(url: string, data?: RequestData) => request<T>({ url, method: 'POST', data }),
  put: <T>(url: string, data?: RequestData) => request<T>({ url, method: 'PUT', data }),
  delete: <T>(url: string, data?: RequestData) => request<T>({ url, method: 'DELETE', data }),
}
