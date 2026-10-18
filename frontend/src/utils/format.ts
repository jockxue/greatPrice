import type { CurrencyCode } from '@/types/deal'

const CURRENCY_SYMBOLS: Record<CurrencyCode, string> = {
  CNY: '¥',
}

/** 后端金额字符串转展示文本，不带货币符号，不经过浮点运算：整数去掉 ".00"，否则原样保留两位。"199.00" → "199"，"89.90" → "89.90" */
export function formatAmount(amount: string): string {
  return amount.endsWith('.00') ? amount.slice(0, -3) : amount
}

/** 未知币种直接显示币种代码 */
export function getCurrencySymbol(currency: CurrencyCode): string {
  return CURRENCY_SYMBOLS[currency] ?? currency
}

/** "99.90", 'CNY' → "¥99.90" */
export function formatPrice(amount: string, currency: CurrencyCode): string {
  return getCurrencySymbol(currency) + formatAmount(amount)
}

const pad = (n: number) => String(n).padStart(2, '0')

/** ISO-8601 → "10-18 09:30"（按设备本地时区）；跨年时带年份；无法解析时原样返回。 */
export function formatDateTime(iso: string, now: Date = new Date()): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return iso
  }
  const monthDay = `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  const prefix = date.getFullYear() === now.getFullYear() ? monthDay : `${date.getFullYear()}-${monthDay}`
  return `${prefix} ${time}`
}
