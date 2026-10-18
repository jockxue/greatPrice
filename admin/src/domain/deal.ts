export interface DealActionInput {
  status: string
  role: string
  submittedBy: number | null
  selfId: number
}

export interface DealActions {
  canEdit: boolean
  canSubmit: boolean
  canApprove: boolean
  canReject: boolean
  canOffline: boolean
  canRevise: boolean
  canReopen: boolean
}

export function dealActions(input: DealActionInput): DealActions {
  const admin = input.role === 'ADMIN'
  const own = input.submittedBy != null && input.submittedBy === input.selfId
  const pending = input.status === 'PENDING_REVIEW'
  return {
    canEdit: input.status === 'DRAFT' || pending,
    canSubmit: input.status === 'DRAFT',
    canApprove: admin && pending && !own,
    canReject: admin && pending && !own,
    canOffline: admin && input.status === 'PUBLISHED',
    canRevise: input.status === 'PUBLISHED',
    canReopen: input.status === 'OFFLINE' || input.status === 'EXPIRED',
  }
}

export function isDemo(sourceType: string, productCode: string | null | undefined): boolean {
  return sourceType === 'DEMO' || (productCode != null && productCode.startsWith('DEMO-'))
}

export function toApiTime(value: string): string | null {
  const text = value.trim()
  if (!text) {
    return null
  }
  return `${text.replace(' ', 'T')}+08:00`
}

export function fromApiTime(value: string | null): string {
  if (!value) {
    return ''
  }
  return value.slice(0, 19).replace('T', ' ')
}

export function fieldErrors(message: string): Record<string, string> {
  const result: Record<string, string> = {}
  for (const part of message.split('; ')) {
    const index = part.indexOf(': ')
    if (index > 0) {
      result[part.slice(0, index)] = part.slice(index + 2)
    }
  }
  return result
}

export function redirectTarget(isPublic: boolean, role: string | null, requiresAdmin: boolean): string | null {
  if (isPublic) {
    return null
  }
  if (!role) {
    return '/login'
  }
  if (requiresAdmin && role !== 'ADMIN') {
    return '/dashboard'
  }
  return null
}
