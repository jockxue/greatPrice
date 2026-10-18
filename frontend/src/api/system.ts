import { http } from '@/utils/request'
import type { HealthInfo } from '@/types/system'

export function fetchHealth() {
  return http.get<HealthInfo>('/api/v1/health')
}
