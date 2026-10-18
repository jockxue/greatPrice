/** 金额只做格式校验，不转成 JavaScript number 做运算。 */
export function isMoney(value: string, required = true): boolean {
  const text = value.trim()
  if (!text) {
    return !required
  }
  return /^\d{1,8}(\.\d{1,2})?$/.test(text)
}

export function moneyError(value: string, label: string, required = true): string | null {
  if (!value.trim()) {
    return required ? `${label}不能为空` : null
  }
  if (!isMoney(value, true)) {
    return `${label}须为 0 到 99999999.99，最多两位小数`
  }
  return null
}
