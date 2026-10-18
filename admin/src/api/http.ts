import axios, { type AxiosResponse } from 'axios'
import { env } from '@/config/env'
import { parseApiFailure } from '@/domain/http-error'

export class ApiError extends Error {
  constructor(
    message: string,
    readonly code: number,
    readonly status?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export const http = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
  timeout: 15000,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

let onUnauthorized = () => {}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

http.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as { code?: number; message?: string }
    if (body && typeof body.code === 'number' && body.code !== 0) {
      const failure = parseApiFailure(response.status, body, false)
      if (failure.unauthorized) {
        onUnauthorized()
      }
      return Promise.reject(new ApiError(failure.message, failure.code, response.status))
    }
    return response
  },
  (error) => {
    const failure = parseApiFailure(error.response?.status, error.response?.data, !error.response)
    if (failure.unauthorized) {
      onUnauthorized()
    }
    return Promise.reject(new ApiError(failure.message, failure.code, error.response?.status))
  },
)

export async function unwrap<T>(promise: Promise<AxiosResponse<{ data: T }>>): Promise<T> {
  const response = await promise
  return response.data.data
}
