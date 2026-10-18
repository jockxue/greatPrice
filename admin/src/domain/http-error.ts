export interface ApiFailure {
  message: string
  code: number
  unauthorized: boolean
}

export function parseApiFailure(
  status: number | undefined,
  data: { code?: number; message?: string } | undefined,
  network: boolean,
): ApiFailure {
  if (network || data == null) {
    return { message: '网络异常，请稍后重试', code: -1, unauthorized: false }
  }
  const code = data.code ?? -1
  return {
    message: data.message || '请求失败',
    code,
    unauthorized: status === 401 || code === 40100,
  }
}
