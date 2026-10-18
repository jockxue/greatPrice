<script setup lang="ts">
import { computed } from 'vue'
import { pickColor } from '@/utils/palette'

/** 分类图标：统一风格的圆角色块 + 分类名首字，不依赖任何远程图标资源。 */
const props = defineProps<{
  name: string
  colorSeed: number
}>()

const color = computed(() => pickColor(props.colorSeed))
</script>

<template>
  <view class="category-icon" :style="{ backgroundColor: color.bg, color: color.fg }">
    <text>{{ name.charAt(0) }}</text>
  </view>
</template>

<style lang="scss" scoped>
.category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88rpx;
  height: 88rpx;
  border-radius: 28rpx;
  font-size: $gp-font-size-lg;
  font-weight: 600;
}
</style>
