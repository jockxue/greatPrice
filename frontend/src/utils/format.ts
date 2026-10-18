import type { CurrencyCode } from '@/types/deal'

const CURRENCY_SYMBOLS: Record<CurrencyCode, string> = {
  CNY: '¥',
}

/** 金额数字转展示文本，不带货币符号：整数不显示小数，否则保留两位。199 → "199"，89.9 → "89.90" */
export function formatAmount(amount: number): string {
  return Number.isInteger(amount) ? String(amount) : amount.toFixed(2)
}

export function getCurrencySymbol(currency: CurrencyCode): string {
  return CURRENCY_SYMBOLS[currency]
}

/** 99.9, 'CNY' → "¥99.90" */
export function formatPrice(amount: number, currency: CurrencyCode): string {
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
