import { fetchHealth } from '@/api/system'

export type BackendStatus = 'ok' | 'database-down' | 'unreachable'

/** 用于开发阶段确认前后端与数据库是否连通。 */
export async function getBackendStatus(): Promise<BackendStatus> {
  try {
    const health = await fetchHealth()
    return health.database === 'UP' ? 'ok' : 'database-down'
  } catch {
    return 'unreachable'
  }
}
