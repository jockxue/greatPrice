export type HealthStatus = 'UP' | 'DOWN'

/** GET /api/v1/health */
export interface HealthInfo {
  status: HealthStatus
  database: HealthStatus
  time: string
}
