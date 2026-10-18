/** 分类图标、占位图使用的统一色板（背景浅色 + 前景深色）。 */
const PALETTE = [
  { bg: '#FFF1F0', fg: '#E62E2E' },
  { bg: '#FFF7E6', fg: '#D46B08' },
  { bg: '#F6FFED', fg: '#389E0D' },
  { bg: '#E6F4FF', fg: '#0958D9' },
  { bg: '#F9F0FF', fg: '#722ED1' },
  { bg: '#E6FFFB', fg: '#08979C' },
  { bg: '#FFF0F6', fg: '#C41D7F' },
  { bg: '#F5F5F5', fg: '#595959' },
] as const

export type PaletteColor = (typeof PALETTE)[number]

/** 根据数字（如分类 id）稳定地取一组颜色，同一个 seed 永远得到同一种颜色。 */
export function pickColor(seed: number): PaletteColor {
  const index = ((seed % PALETTE.length) + PALETTE.length) % PALETTE.length
  return PALETTE[index]
}
