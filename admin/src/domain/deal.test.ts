import { describe, expect, it } from 'vitest'
import { dealActions, fieldErrors, isDemo, redirectTarget, toApiTime } from './deal'

describe('deal rules', () => {
  it('keeps review and publish on an admin who did not submit', () => {
    const actions = dealActions({ status: 'PENDING_REVIEW', role: 'OPERATOR', submittedBy: 2, selfId: 2 })
    expect(actions.canApprove).toBe(false)
    expect(actions.canSubmit).toBe(false)
    expect(actions.canEdit).toBe(true)

    const admin = dealActions({ status: 'PENDING_REVIEW', role: 'ADMIN', submittedBy: 2, selfId: 9 })
    expect(admin.canApprove).toBe(true)
    expect(admin.canReject).toBe(true)

    const self = dealActions({ status: 'PENDING_REVIEW', role: 'ADMIN', submittedBy: 9, selfId: 9 })
    expect(self.canApprove).toBe(false)
  })

  it('does not treat offline as directly publishable', () => {
    const actions = dealActions({ status: 'OFFLINE', role: 'ADMIN', submittedBy: 1, selfId: 2 })
    expect(actions.canOffline).toBe(false)
    expect(actions.canReopen).toBe(true)
    expect(actions.canEdit).toBe(false)
  })

  it('marks demo data and maps field errors', () => {
    expect(isDemo('DEMO', 'P-1')).toBe(true)
    expect(isDemo('EDITOR', 'DEMO-1')).toBe(true)
    expect(isDemo('EDITOR', 'P-1')).toBe(false)
    expect(fieldErrors('currentPrice: 不能为空; sourceType: 来源类型不能为空')).toEqual({
      currentPrice: '不能为空',
      sourceType: '来源类型不能为空',
    })
    expect(toApiTime('2026-10-18 09:30:00')).toBe('2026-10-18T09:30:00+08:00')
    expect(toApiTime('  ')).toBeNull()
  })

  it('sends anonymous users to login and operators away from admin pages', () => {
    expect(redirectTarget(true, null, false)).toBeNull()
    expect(redirectTarget(false, null, false)).toBe('/login')
    expect(redirectTarget(false, 'OPERATOR', true)).toBe('/dashboard')
    expect(redirectTarget(false, 'ADMIN', true)).toBeNull()
  })
})
