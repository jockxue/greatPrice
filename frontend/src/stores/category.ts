import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 分类页是 TabBar 页面，uni.switchTab 不能携带参数，
 * 因此首页点击分类时通过此 store 传递选中的分类。null 表示「全部」。
 */
export const useCategoryStore = defineStore('category', () => {
  const selectedId = ref<number | null>(null)

  function select(id: number | null) {
    selectedId.value = id
  }

  return { selectedId, select }
})
