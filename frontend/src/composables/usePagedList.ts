import { ref, shallowRef } from 'vue'
import type { PageResult } from '@/types/api'

export const DEFAULT_PAGE_SIZE = 10

type FetchPage<T> = (page: number, size: number) => Promise<PageResult<T>>

/**
 * 分页列表状态：首屏加载、加载更多、按 id 去重、失败重试。
 * 每次请求都有序号，reload 之后旧请求的结果会被丢弃，避免快速切换条件时旧数据覆盖新数据。
 */
export function usePagedList<T extends { id: number }>(fetchPage: FetchPage<T>, pageSize = DEFAULT_PAGE_SIZE) {
  const items = shallowRef<T[]>([])
  const total = ref(0)
  const loading = ref(false)
  /** 最近一次请求的错误信息，空字符串表示没有错误 */
  const error = ref('')
  /** 已加载全部数据 */
  const finished = ref(false)

  let nextPage = 1
  let failedPage = 1
  let requestSeq = 0

  async function load(page: number) {
    const seq = ++requestSeq
    loading.value = true
    error.value = ''
    try {
      const result = await fetchPage(page, pageSize)
      if (seq !== requestSeq) return
      if (page === 1) {
        items.value = result.records
      } else {
        const known = new Set(items.value.map((item) => item.id))
        items.value = [...items.value, ...result.records.filter((item) => !known.has(item.id))]
      }
      total.value = result.total
      nextPage = page + 1
      finished.value = result.records.length < pageSize || page * pageSize >= result.total
    } catch (e) {
      if (seq !== requestSeq) return
      failedPage = page
      error.value = e instanceof Error && e.message ? e.message : '加载失败，请稍后重试'
    } finally {
      if (seq === requestSeq) loading.value = false
    }
  }

  /** 从第 1 页重新加载。keepItems 为 true 时保留旧数据直到新数据返回（下拉刷新）；否则立即清空（切换筛选条件）。 */
  function reload(options: { keepItems?: boolean } = {}) {
    if (!options.keepItems) {
      items.value = []
      total.value = 0
    }
    finished.value = false
    return load(1)
  }

  /** 加载下一页；正在加载、已到底或上次失败（需先 retry）时不会发起请求 */
  function loadMore() {
    if (loading.value || finished.value || error.value) return Promise.resolve()
    return load(nextPage)
  }

  /** 重试上次失败的那一页 */
  function retry() {
    return load(failedPage)
  }

  return { items, total, loading, error, finished, reload, loadMore, retry }
}
