import { http } from '@/utils/request'
import type { Category } from '@/types/deal'

/** 只返回启用的分类，已按 sort_order 排序 */
export function fetchCategories() {
  return http.get<Category[]>('/api/v1/categories')
}
