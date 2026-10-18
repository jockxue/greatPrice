<script setup lang="ts">
import { computed } from 'vue'
import DealImage from '@/components/DealImage.vue'
import PriceText from '@/components/PriceText.vue'
import type { DealSummary } from '@/types/deal'
import { formatDateTime } from '@/utils/format'
import { goDealDetail } from '@/utils/navigation'

const props = defineProps<{
  deal: DealSummary
  /** 窄列表（如分类页右侧）使用更小的图片 */
  compact?: boolean
}>()

const timeText = computed(() =>
  props.deal.verifiedAt
    ? `${formatDateTime(props.deal.verifiedAt)} 核验`
    : `${formatDateTime(props.deal.publishedAt)} 发布`,
)
</script>

<template>
  <view class="deal-card" :class="{ 'deal-card--compact': compact }" hover-class="deal-card--hover" @click="goDealDetail(deal.id)">
    <view class="deal-card__image">
      <DealImage :image="deal.imageUrl" :label="deal.brand || deal.title" :color-seed="deal.categoryId" />
    </view>
    <view class="deal-card__body">
      <view class="deal-card__title">{{ deal.title }}</view>
      <view class="deal-card__desc">{{ deal.dealDescription }}</view>
      <view class="deal-card__price">
        <PriceText :price="deal.currentPrice" :original-price="deal.originalPrice" :currency="deal.currency" />
      </view>
      <view class="deal-card__meta">
        <text class="deal-card__source">{{ deal.sourceName }}</text>
        <text class="deal-card__time">{{ timeText }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.deal-card {
  display: flex;
  padding: $gp-spacing-base;
  background-color: $gp-bg-card;
  border-radius: $gp-radius-base;
}

.deal-card--hover {
  opacity: 0.85;
}

.deal-card__image {
  flex-shrink: 0;
  width: 200rpx;
  height: 200rpx;
  border-radius: $gp-radius-base;
  overflow: hidden;
}

.deal-card--compact {
  padding: $gp-spacing-sm;

  .deal-card__image {
    width: 160rpx;
    height: 160rpx;
  }

  .deal-card__body {
    margin-left: $gp-spacing-sm;
  }
}

.deal-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  margin-left: $gp-spacing-base;
}

.deal-card__title {
  display: -webkit-box;
  overflow: hidden;
  color: $gp-text-main;
  font-size: $gp-font-size-base;
  font-weight: 500;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  word-break: break-all;
}

.deal-card__desc {
  margin-top: 8rpx;
  overflow: hidden;
  color: $gp-text-secondary;
  font-size: $gp-font-size-sm;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.deal-card__price {
  margin-top: auto;
  padding-top: 8rpx;
}

.deal-card__meta {
  display: flex;
  justify-content: space-between;
  margin-top: 4rpx;
  color: $gp-text-placeholder;
  font-size: 22rpx;
}

.deal-card__source {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.deal-card__time {
  flex-shrink: 0;
  margin-left: $gp-spacing-sm;
}
</style>
