/** 与后端 com.greatprice.common.api.ApiResponse 对应，code 为 0 表示成功。 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** 与后端 com.greatprice.common.api.PageResult 对应，page 从 1 开始。 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface PageQuery {
  page?: number
  size?: number
}

/** 与后端 ErrorCode 对应的错误码；-1 为前端网络或解析错误 */
export const API_ERROR_CODE = {
  NETWORK: -1,
  NOT_FOUND: 40400,
  DEAL_UNAVAILABLE: 40401,
} as const
