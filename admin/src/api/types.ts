export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface Profile {
  id: number
  username: string
  displayName: string
  role: 'ADMIN' | 'OPERATOR'
}

export interface Product {
  id: number
  productCode: string
  title: string
  brand: string | null
  imageUrl: string | null
  categoryId: number
  categoryName: string | null
  specification: string | null
  description: string | null
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string | null
  updatedAt: string | null
  demo: boolean
}

export interface Category {
  id: number
  name: string
  code: string
  status: string
}

export interface Deal {
  id: number
  productId: number
  productCode: string
  productStatus: string
  productDeleted: boolean
  title: string
  brand: string | null
  imageUrl: string | null
  categoryId: number
  categoryName: string | null
  specification: string | null
  description: string | null
  currentPrice: string
  originalPrice: string | null
  currency: string
  dealDescription: string
  sourceType: string
  sourceName: string
  purchaseUrl: string | null
  promotionUrl: string | null
  featured: boolean
  startsAt: string | null
  expiresAt: string | null
  verifiedAt: string | null
  status: string
  publishedAt: string | null
  createdAt: string | null
  updatedAt: string | null
  rejectReason: string | null
  offlineReason: string | null
  submittedBy: number | null
  submitterName: string | null
  reviewedBy: number | null
  reviewerName: string | null
  reviewedAt: string | null
  version: number
  demo: boolean
}

export interface DealReview {
  id: number
  dealId: number
  action: string
  reviewerId: number
  reviewerName: string
  reason: string | null
  createdAt: string
}

export interface Dashboard {
  productCount: number
  publishedCount: number
  pendingCount: number
  offlineCount: number
  expiredCount: number
  recentDeals: Deal[]
}

export interface PriceHistory {
  id: number
  productId: number
  productCode: string
  productTitle: string
  dealId: number | null
  price: string
  currency: string
  sourceType: string
  sourceName: string
  verificationStatus: string
  recordedAt: string
  verifiedBy: number | null
  verifierName: string | null
  note: string | null
}

export interface AuditLog {
  id: number
  actorId: number | null
  actorName: string
  action: string
  resourceType: string
  resourceId: number | null
  detail: string | null
  createdAt: string
}

export interface AdminUser {
  id: number
  username: string
  displayName: string
  role: string
  status: string
  createdAt: string | null
}

export interface DealWrite {
  productId: number
  currentPrice: string
  originalPrice: string | null
  currency: string
  dealDescription: string
  sourceType: string
  sourceName: string
  purchaseUrl: string | null
  promotionUrl: string | null
  startsAt: string | null
  expiresAt: string | null
  version?: number
}
