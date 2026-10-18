// 生成 TabBar 图标（81x81 PNG，线性风格），输出到 src/static/tabbar/。
// 只依赖 Node 内置模块；修改形状或颜色后执行：node scripts/generate-tabbar-icons.mjs
import { mkdirSync, writeFileSync } from 'node:fs'
import { deflateSync } from 'node:zlib'

const SIZE = 81
const SUPERSAMPLE = 4
const STROKE = 0.075
const COLORS = { normal: [0x99, 0x99, 0x99], active: [0xe6, 0x2e, 0x2e] }
const OUT_DIR = new URL('../src/static/tabbar/', import.meta.url)

const segmentDistance = (px, py, [ax, ay], [bx, by]) => {
  const dx = bx - ax
  const dy = by - ay
  const t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
  return Math.hypot(px - ax - t * dx, py - ay - t * dy)
}

const polyline = (points) => (x, y) => {
  let min = Infinity
  for (let i = 0; i < points.length - 1; i++) min = Math.min(min, segmentDistance(x, y, points[i], points[i + 1]))
  return min
}

const circle = (cx, cy, r) => (x, y) => Math.abs(Math.hypot(x - cx, y - cy) - r)

/** 圆角矩形轮廓到点的距离 */
const roundRect = (x0, y0, x1, y1, r) => (x, y) => {
  const cx = (x0 + x1) / 2
  const cy = (y0 + y1) / 2
  const qx = Math.abs(x - cx) - ((x1 - x0) / 2 - r)
  const qy = Math.abs(y - cy) - ((y1 - y0) / 2 - r)
  const outside = Math.hypot(Math.max(qx, 0), Math.max(qy, 0))
  return Math.abs(outside + Math.min(Math.max(qx, qy), 0) - r)
}

const ICONS = {
  home: [
    polyline([[0.16, 0.47], [0.5, 0.18], [0.84, 0.47]]),
    polyline([[0.26, 0.4], [0.26, 0.82], [0.74, 0.82], [0.74, 0.4]]),
    polyline([[0.43, 0.82], [0.43, 0.62], [0.57, 0.62], [0.57, 0.82]]),
  ],
  category: [
    roundRect(0.17, 0.17, 0.45, 0.45, 0.07),
    roundRect(0.55, 0.17, 0.83, 0.45, 0.07),
    roundRect(0.17, 0.55, 0.45, 0.83, 0.07),
    roundRect(0.55, 0.55, 0.83, 0.83, 0.07),
  ],
  mine: [circle(0.5, 0.35, 0.15), roundRect(0.2, 0.6, 0.8, 1.05, 0.22)],
}

/** mine 的身体轮廓只保留上半部分，形成肩部 */
const CLIP_BOTTOM = { mine: 0.84 }

function coverage(shapes, clipBottom, px, py) {
  let hits = 0
  for (let sy = 0; sy < SUPERSAMPLE; sy++) {
    for (let sx = 0; sx < SUPERSAMPLE; sx++) {
      const x = (px + (sx + 0.5) / SUPERSAMPLE) / SIZE
      const y = (py + (sy + 0.5) / SUPERSAMPLE) / SIZE
      if (clipBottom !== undefined && y > clipBottom) continue
      if (shapes.some((d) => d(x, y) <= STROKE / 2)) hits++
    }
  }
  return hits / (SUPERSAMPLE * SUPERSAMPLE)
}

const CRC_TABLE = Array.from({ length: 256 }, (_, n) => {
  let c = n
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
  return c >>> 0
})

const crc32 = (buf) => {
  let c = 0xffffffff
  for (const b of buf) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}

const chunk = (type, data) => {
  const len = Buffer.alloc(4)
  len.writeUInt32BE(data.length)
  const body = Buffer.concat([Buffer.from(type), data])
  const crc = Buffer.alloc(4)
  crc.writeUInt32BE(crc32(body))
  return Buffer.concat([len, body, crc])
}

function encodePng(rgba) {
  const header = Buffer.alloc(13)
  header.writeUInt32BE(SIZE, 0)
  header.writeUInt32BE(SIZE, 4)
  header[8] = 8 // bit depth
  header[9] = 6 // RGBA
  const raw = Buffer.alloc((SIZE * 4 + 1) * SIZE)
  for (let y = 0; y < SIZE; y++) rgba.copy(raw, y * (SIZE * 4 + 1) + 1, y * SIZE * 4, (y + 1) * SIZE * 4)
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(raw)),
    chunk('IEND', Buffer.alloc(0)),
  ])
}

mkdirSync(OUT_DIR, { recursive: true })
for (const [name, shapes] of Object.entries(ICONS)) {
  const alpha = new Float64Array(SIZE * SIZE)
  for (let y = 0; y < SIZE; y++) for (let x = 0; x < SIZE; x++) alpha[y * SIZE + x] = coverage(shapes, CLIP_BOTTOM[name], x, y)
  for (const [variant, [r, g, b]] of Object.entries(COLORS)) {
    const rgba = Buffer.alloc(SIZE * SIZE * 4)
    alpha.forEach((a, i) => rgba.set([r, g, b, Math.round(a * 255)], i * 4))
    const file = `${name}${variant === 'active' ? '-active' : ''}.png`
    writeFileSync(new URL(file, OUT_DIR), encodePng(rgba))
    console.log(`generated src/static/tabbar/${file}`)
  }
}
