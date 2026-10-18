import { http, unwrap } from '@/api/http'
import type {
  AdminUser,
  AuditLog,
  Category,
  Dashboard,
  Deal,
  DealReview,
  DealWrite,
  PageResult,
  PriceHistory,
  Product,
  Profile,
} from '@/api/types'

export function ensureCsrf() {
  return unwrap<null>(http.get('/api/admin/v1/auth/csrf'))
}

export function login(username: string, password: string) {
  return unwrap<Profile>(http.post('/api/admin/v1/auth/login', { username, password }))
}

export function logout() {
  return unwrap<null>(http.post('/api/admin/v1/auth/logout'))
}

export function me() {
  return unwrap<Profile>(http.get('/api/admin/v1/auth/me'))
}

export function dashboard() {
  return unwrap<Dashboard>(http.get('/api/admin/v1/dashboard'))
}

export function categories() {
  return unwrap<Category[]>(http.get('/api/admin/v1/categories'))
}

export function products(params: Record<string, unknown>) {
  return unwrap<PageResult<Product>>(http.get('/api/admin/v1/products', { params }))
}

export function product(id: number) {
  return unwrap<Product>(http.get(`/api/admin/v1/products/${id}`))
}

export function createProduct(body: Record<string, unknown>) {
  return unwrap<Product>(http.post('/api/admin/v1/products', body))
}

export function updateProduct(id: number, body: Record<string, unknown>) {
  return unwrap<Product>(http.put(`/api/admin/v1/products/${id}`, body))
}

export function disableProduct(id: number) {
  return unwrap<Product>(http.post(`/api/admin/v1/products/${id}/disable`))
}

export function enableProduct(id: number) {
  return unwrap<Product>(http.post(`/api/admin/v1/products/${id}/enable`))
}

export function deleteProduct(id: number) {
  return unwrap<null>(http.delete(`/api/admin/v1/products/${id}`))
}

export function deals(params: Record<string, unknown>) {
  return unwrap<PageResult<Deal>>(http.get('/api/admin/v1/deals', { params }))
}

export function reviews(params: Record<string, unknown>) {
  return unwrap<PageResult<Deal>>(http.get('/api/admin/v1/reviews', { params }))
}

export function deal(id: number) {
  return unwrap<Deal>(http.get(`/api/admin/v1/deals/${id}`))
}

export function dealReviews(id: number) {
  return unwrap<DealReview[]>(http.get(`/api/admin/v1/deals/${id}/reviews`))
}

export function createDeal(body: DealWrite) {
  return unwrap<Deal>(http.post('/api/admin/v1/deals', body))
}

export function updateDeal(id: number, body: DealWrite) {
  return unwrap<Deal>(http.put(`/api/admin/v1/deals/${id}`, body))
}

export function submitDeal(id: number, version: number) {
  return unwrap<Deal>(http.post(`/api/admin/v1/deals/${id}/submit`, { version }))
}

export function approveDeal(id: number, version: number) {
  return unwrap<Deal>(http.post(`/api/admin/v1/deals/${id}/approve`, { version }))
}

export function rejectDeal(id: number, version: number, reason: string) {
  return unwrap<Deal>(http.post(`/api/admin/v1/deals/${id}/reject`, { version, reason }))
}

export function offlineDeal(id: number, version: number, reason: string) {
  return unwrap<Deal>(http.post(`/api/admin/v1/deals/${id}/offline`, { version, reason }))
}

export function reviseDeal(id: number, body: DealWrite) {
  return unwrap<Deal>(http.post(`/api/admin/v1/deals/${id}/revise`, body))
}

export function reopenDeal(id: number, version: number) {
  return unwrap<Deal>(http.post(`/api/admin/v1/deals/${id}/reopen`, { version }))
}

export function priceHistory(params: Record<string, unknown>) {
  return unwrap<PageResult<PriceHistory>>(http.get('/api/admin/v1/price-history', { params }))
}

export function verifyPrice(body: Record<string, unknown>) {
  return unwrap<PriceHistory>(http.post('/api/admin/v1/price-history', body))
}

export function auditLogs(params: Record<string, unknown>) {
  return unwrap<PageResult<AuditLog>>(http.get('/api/admin/v1/audit-logs', { params }))
}

export function users() {
  return unwrap<AdminUser[]>(http.get('/api/admin/v1/users'))
}

export function createUser(body: Record<string, unknown>) {
  return unwrap<AdminUser>(http.post('/api/admin/v1/users', body))
}

export function disableUser(id: number) {
  return unwrap<AdminUser>(http.post(`/api/admin/v1/users/${id}/disable`))
}

export function enableUser(id: number) {
  return unwrap<AdminUser>(http.post(`/api/admin/v1/users/${id}/enable`))
}
