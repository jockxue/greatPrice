export const dealStatusLabel: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  PUBLISHED: '已发布',
  OFFLINE: '已下架',
  EXPIRED: '已过期',
}

export const dealStatusType: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  PUBLISHED: 'success',
  OFFLINE: 'danger',
  EXPIRED: 'info',
}

export const sourceLabel: Record<string, string> = {
  DEMO: '演示',
  EDITOR: '编辑发布',
  USER_SUBMISSION: '用户爆料',
  AFFILIATE: '推广联盟',
}

export const productStatusLabel: Record<string, string> = {
  ACTIVE: '启用',
  INACTIVE: '停用',
}

export function labelOf(map: Record<string, string>, value: string | null | undefined) {
  if (!value) return '-'
  return map[value] ?? value
}
