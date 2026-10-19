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
  expired: boolean
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
  lastExpiryRun: JobRun | null
}

export interface JobRun {
  jobName: string
  status: string
  processedCount: number
  failedCount: number
  message: string | null
  startedAt: string
  finishedAt: string
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

export interface TaobaoStatus {
  enabled: boolean
  manualSyncEnabled: boolean
  appKeyConfigured: boolean
  appKeyMasked: string | null
  appSecretConfigured: boolean
  siteIdConfigured: boolean
  adzoneIdConfigured: boolean
  sessionConfigured: boolean
  gatewayHost: string | null
  searchConfigured: boolean
  detailConfigured: boolean
  promotionConfigured: boolean
  lastTestStatus: string | null
  lastTestMessage: string | null
  lastTestErrorCode: string | null
  lastTestAt: string | null
}

export interface TaobaoItem {
  platform: string
  apiMethod: string
  itemId: string | null
  title: string | null
  imageUrl: string | null
  shopTitle: string | null
  reservePrice: string | null
  reservePriceStatus: string
  finalPrice: string | null
  finalPriceStatus: string
  salePrice: string | null
  salePriceStatus: string
  couponId: string | null
  couponInfo: string | null
  couponStart: string | null
  couponEnd: string | null
  itemUrl: string | null
  platformClickUrl: string | null
  promotionLinkGenerated: boolean
  categoryName: string | null
  suggestedCategoryId: number | null
  importedProductId: number | null
  importedDealId: number | null
  importedDealStatus: string | null
  volume: string | null
  userType: string | null
  catName: string | null
  catLeafName: string | null
  playInfo: string | null
  presaleText: string | null
  kuadian: string | null
}

export interface TaobaoSearchResult {
  apiMethod: string
  requestId: string | null
  total: number
  items: TaobaoItem[]
}

export interface TaobaoPromotion {
  apiMethod: string
  requestId: string | null
  itemId: string
  promotionUrl: string
  linkField: string
  couponInfo: string | null
  couponStart: string | null
  couponEnd: string | null
  persisted: boolean
  dealId: number | null
  miniProgramCompatible: boolean
  clientNote: string
}

export interface TaobaoImportResult {
  productOutcome: string
  productId: number
  productCode: string | null
  dealOutcome: string
  dealId: number | null
  dealStatus: string | null
  message: string
}

export interface TaobaoProbe {
  state: string
  apiMethod: string
  errorCode: string | null
  message: string | null
  requestId: string | null
  retryable: boolean
}

export interface TaobaoTestResult {
  search: TaobaoProbe
  detail: TaobaoProbe
  promotion: TaobaoProbe
}

export interface TaobaoJob {
  id: number
  jobName: string
  status: string
  requestedCount: number
  processedCount: number
  failedCount: number
  errorCode: string | null
  retryable: boolean
  message: string | null
  startedAt: string | null
  finishedAt: string | null
}
