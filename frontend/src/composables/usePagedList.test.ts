import { describe, expect, it, vi } from 'vitest'
import { usePagedList } from '@/composables/usePagedList'
import type { PageResult } from '@/types/api'

type Item = { id: number }

const page = (ids: number[], total: number, pageNo = 1, size = 2): PageResult<Item> => ({
  records: ids.map((id) => ({ id })),
  total,
  page: pageNo,
  size,
})

/** 手动控制返回时机的 Promise，用来模拟慢请求 */
function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

const ids = (items: Item[]) => items.map((i) => i.id)

describe('usePagedList', () => {
  it('首屏加载期间 loading 为 true，完成后填充数据', async () => {
    const d = deferred<PageResult<Item>>()
    const list = usePagedList<Item>(() => d.promise, 2)
    const pending = list.reload()
    expect(list.loading.value).toBe(true)
    d.resolve(page([1, 2], 3))
    await pending
    expect(list.loading.value).toBe(false)
    expect(ids(list.items.value)).toEqual([1, 2])
    expect(list.total.value).toBe(3)
    expect(list.finished.value).toBe(false)
  })

  it('加载更多按页追加，到最后一页后不再请求', async () => {
    const fetchPage = vi
      .fn()
      .mockResolvedValueOnce(page([1, 2], 3))
      .mockResolvedValueOnce(page([3], 3, 2))
    const list = usePagedList<Item>(fetchPage, 2)
    await list.reload()
    await list.loadMore()
    expect(fetchPage).toHaveBeenLastCalledWith(2, 2)
    expect(ids(list.items.value)).toEqual([1, 2, 3])
    expect(list.finished.value).toBe(true)
    await list.loadMore()
    expect(fetchPage).toHaveBeenCalledTimes(2)
  })

  it('翻页期间有新数据插入导致记录重复时按 id 去重', async () => {
    const fetchPage = vi
      .fn()
      .mockResolvedValueOnce(page([5, 4], 5))
      .mockResolvedValueOnce(page([4, 3], 5, 2))
    const list = usePagedList<Item>(fetchPage, 2)
    await list.reload()
    await list.loadMore()
    expect(ids(list.items.value)).toEqual([5, 4, 3])
  })

  it('加载中重复触发 loadMore 只发一次请求', async () => {
    const d = deferred<PageResult<Item>>()
    const fetchPage = vi.fn().mockResolvedValueOnce(page([1, 2], 4)).mockReturnValueOnce(d.promise)
    const list = usePagedList<Item>(fetchPage, 2)
    await list.reload()
    const first = list.loadMore()
    list.loadMore()
    list.loadMore()
    d.resolve(page([3, 4], 4, 2))
    await first
    expect(fetchPage).toHaveBeenCalledTimes(2)
    expect(ids(list.items.value)).toEqual([1, 2, 3, 4])
  })

  it('切换条件后，先发出的慢请求结果被丢弃', async () => {
    const slow = deferred<PageResult<Item>>()
    const fast = deferred<PageResult<Item>>()
    const fetchPage = vi.fn().mockReturnValueOnce(slow.promise).mockReturnValueOnce(fast.promise)
    const list = usePagedList<Item>(fetchPage, 2)
    const first = list.reload()
    const second = list.reload()
    fast.resolve(page([20], 1))
    await second
    slow.resolve(page([10, 11], 2))
    await first
    expect(ids(list.items.value)).toEqual([20])
    expect(list.loading.value).toBe(false)
  })

  it('旧请求失败也不会影响新结果', async () => {
    const slow = deferred<PageResult<Item>>()
    const fetchPage = vi.fn().mockReturnValueOnce(slow.promise).mockResolvedValueOnce(page([20], 1))
    const list = usePagedList<Item>(fetchPage, 2)
    const first = list.reload()
    await list.reload()
    slow.reject(new Error('网络异常'))
    await first
    expect(list.error.value).toBe('')
    expect(ids(list.items.value)).toEqual([20])
  })

  it('空结果直接标记为已到底', async () => {
    const list = usePagedList<Item>(async () => page([], 0), 2)
    await list.reload()
    expect(list.items.value).toEqual([])
    expect(list.finished.value).toBe(true)
  })

  it('首屏失败记录错误，重试成功后清除', async () => {
    const fetchPage = vi.fn().mockRejectedValueOnce(new Error('网络异常，请检查网络后重试')).mockResolvedValueOnce(page([1], 1))
    const list = usePagedList<Item>(fetchPage, 2)
    await list.reload()
    expect(list.error.value).toBe('网络异常，请检查网络后重试')
    expect(list.loading.value).toBe(false)
    await list.retry()
    expect(list.error.value).toBe('')
    expect(ids(list.items.value)).toEqual([1])
  })

  it('加载更多失败后保留已有数据，需重试失败的那一页', async () => {
    const fetchPage = vi
      .fn()
      .mockResolvedValueOnce(page([1, 2], 3))
      .mockRejectedValueOnce(new Error('超时'))
      .mockResolvedValueOnce(page([3], 3, 2))
    const list = usePagedList<Item>(fetchPage, 2)
    await list.reload()
    await list.loadMore()
    expect(list.error.value).toBe('超时')
    expect(ids(list.items.value)).toEqual([1, 2])

    await list.loadMore()
    expect(fetchPage).toHaveBeenCalledTimes(2)

    await list.retry()
    expect(fetchPage).toHaveBeenLastCalledWith(2, 2)
    expect(ids(list.items.value)).toEqual([1, 2, 3])
  })

  it('reload 默认立即清空旧数据，keepItems 时保留到新数据返回', async () => {
    const d = deferred<PageResult<Item>>()
    const fetchPage = vi.fn().mockResolvedValueOnce(page([1], 1)).mockReturnValueOnce(d.promise).mockResolvedValueOnce(page([3], 1))
    const list = usePagedList<Item>(fetchPage, 2)
    await list.reload()

    const refreshing = list.reload({ keepItems: true })
    expect(ids(list.items.value)).toEqual([1])
    d.resolve(page([2], 1))
    await refreshing
    expect(ids(list.items.value)).toEqual([2])

    const switching = list.reload()
    expect(list.items.value).toEqual([])
    await switching
    expect(ids(list.items.value)).toEqual([3])
  })
})
