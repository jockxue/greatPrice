<script setup lang="ts">
import { computed, ref } from 'vue'
import { pickColor } from '@/utils/palette'

/** 商品图片。无图或加载失败时显示统一风格的占位区域；尺寸由父元素决定，图片按 aspectFill 裁剪不变形。 */
const props = defineProps<{
  image?: string
  /** 占位区域显示的文字，通常为品牌名 */
  label: string
  /** 决定占位颜色，通常为分类 id */
  colorSeed: number
}>()

const loadFailed = ref(false)
const color = computed(() => pickColor(props.colorSeed))
const showImage = computed(() => !!props.image && !loadFailed.value)
</script>

<template>
  <view class="deal-image" :style="{ backgroundColor: color.bg }">
    <image v-if="showImage" class="deal-image__img" :src="image" mode="aspectFill" lazy-load @error="loadFailed = true" />
    <view v-else class="deal-image__placeholder">
      <text class="deal-image__label" :style="{ color: color.fg }">{{ label }}</text>
      <text class="deal-image__hint">演示图</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.deal-image {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.deal-image__img {
  width: 100%;
  height: 100%;
}

.deal-image__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.deal-image__label {
  max-width: 90%;
  overflow: hidden;
  font-size: $gp-font-size-lg;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.deal-image__hint {
  margin-top: 4rpx;
  color: $gp-text-placeholder;
  font-size: 20rpx;
}
</style>
