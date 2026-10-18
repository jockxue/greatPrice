import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'

// 单元测试只覆盖不依赖 uni.* 的纯 TS 逻辑（utils、services、mock 数据），
// 因此不加载 uni-app 的 Vite 插件，与 vite.config.ts 相互独立。
export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    include: ['src/**/*.test.ts'],
  },
})
