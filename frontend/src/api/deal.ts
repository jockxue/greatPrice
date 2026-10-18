import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { DealDetail, DealQuery, DealSummary } from '@/types/deal'

export function fetchDeals(query: DealQuery = {}) {
  return http.get<PageResult<DealSummary>>('/api/v1/deals', { ...query })
}

/** 不存在时抛出 code 40400，已下架或已过期时抛出 code 40401 */
export function fetchDealDetail(id: number) {
  return http.get<DealDetail>(`/api/v1/deals/${id}`)
}
