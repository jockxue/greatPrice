<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import DealImage from '@/components/DealImage.vue'
import DemoNotice from '@/components/DemoNotice.vue'
import EmptyState from '@/components/EmptyState.vue'
import PriceText from '@/components/PriceText.vue'
import { showToast } from '@/platform'
import { getDealById, listCategories } from '@/services/deal'
import type { Deal } from '@/types/deal'
import { formatDateTime } from '@/utils/format'
import { goHome } from '@/utils/navigation'

const loading = ref(true)
const deal = ref<Deal>()
const categoryName = ref('')

const infoRows = computed(() => {
  if (!deal.value) return []
  return [
    { label: '商品来源', value: deal.value.sourceName },
    { label: '发布时间', value: formatDateTime(deal.value.publishedAt) },
    { label: '核验时间', value: deal.value.verifiedAt ? formatDateTime(deal.value.verifiedAt) : '暂未核验' },
  ]
})

onLoad(async (query) => {
  const id = Number(query?.id)
  if (Number.isInteger(id) && id > 0) {
    const [found, categories] = await Promise.all([getDealById(id), listCategories()])
    deal.value = found
    categoryName.value = categories.find((c) => c.id === found?.categoryId)?.name ?? ''
  }
  loading.value = false
})

/** 第二阶段不跳转任何外部链接；购买链接由第三阶段后端生成后再接入 */
function buy() {
  showToast('购买链接将在后续阶段接入')
}
</script>

<template>
  <view>
    <uni-load-more v-if="loading" status="loading" />

    <EmptyState
      v-else-if="!deal"
      icon="info"
      title="好价不存在或已下架"
      description="可能是链接有误，或该好价已被移除"
      action-text="返回首页"
      @action="goHome"
    />

    <view v-else class="detail">
      <view class="detail__image">
        <DealImage :image="deal.image" :label="deal.brand" :color-seed="deal.categoryId" />
      </view>

      <view class="card">
        <PriceText :price="deal.price" :original-price="deal.originalPrice" :currency="deal.currency" size="lg" />
        <view class="detail__title">{{ deal.title }}</view>
        <view class="detail__chips">
          <text class="chip">{{ deal.brand }}</text>
          <text v-if="categoryName" class="chip">{{ categoryName }}</text>
          <text v-for="tag in deal.tags" :key="tag" class="chip chip--primary">{{ tag }}</text>
        </view>
      </view>

      <view class="detail__notice">
        <DemoNotice />
      </view>

      <view class="card">
        <view class="card__title">优惠说明</view>
        <view class="card__text">{{ deal.dealDescription }}</view>
      </view>

      <view v-if="deal.specs?.length" class="card">
        <view class="card__title">规格与适用条件</view>
        <view v-for="spec in deal.specs" :key="spec.label" class="row">
          <text class="row__label">{{ spec.label }}</text>
          <text class="row__value">{{ spec.value }}</text>
        </view>
      </view>

      <view class="card">
        <view class="card__title">信息来源</view>
        <view v-for="row in infoRows" :key="row.label" class="row">
          <text class="row__label">{{ row.label }}</text>
          <text class="row__value">{{ row.value }}</text>
        </view>
        <view class="card__hint">价格和优惠可能随时变化，请以商城下单页面为准</view>
      </view>

      <view class="action-bar">
        <view class="action-bar__inner">
          <view class="action-bar__source">
            <text class="action-bar__label">来自</text>
            <text class="action-bar__name">{{ deal.sourceName }}</text>
          </view>
          <view class="action-bar__button" hover-class="action-bar__button--hover" @click="buy">去购买</view>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
$action-bar-height: 112rpx;

.detail {
  /* 为底部固定操作栏和 iPhone 底部安全区留出空间 */
  padding-bottom: calc(#{$action-bar-height} + #{$gp-spacing-base} + constant(safe-area-inset-bottom));
  padding-bottom: calc(#{$action-bar-height} + #{$gp-spacing-base} + env(safe-area-inset-bottom));
}

.detail__image {
  width: 100%;
  height: 560rpx;
}

.detail__title {
  margin-top: $gp-spacing-sm;
  color: $gp-text-main;
  font-size: $gp-font-size-lg;
  font-weight: 600;
  line-height: 1.5;
  word-break: break-all;
}

.detail__chips {
  display: flex;
  flex-wrap: wrap;
  margin-top: $gp-spacing-sm;
}

.chip {
  margin: 0 $gp-spacing-sm 8rpx 0;
  padding: 4rpx 16rpx;
  background-color: $gp-bg-page;
  border-radius: 6rpx;
  color: $gp-text-secondary;
  font-size: 22rpx;
}

.chip--primary {
  background-color: #fff1f0;
  color: $gp-color-primary;
}

.detail__notice {
  margin: $gp-spacing-base $gp-spacing-base 0;
}

.card {
  margin: $gp-spacing-base $gp-spacing-base 0;
  padding: $gp-spacing-base;
  background-color: $gp-bg-card;
  border-radius: $gp-radius-base;
}

.card__title {
  margin-bottom: $gp-spacing-sm;
  color: $gp-text-main;
  font-size: $gp-font-size-base;
  font-weight: 600;
}

.card__text {
  color: $gp-text-secondary;
  font-size: $gp-font-size-base;
  line-height: 1.6;
}

.card__hint {
  margin-top: $gp-spacing-sm;
  color: $gp-text-placeholder;
  font-size: 22rpx;
}

.row {
  display: flex;
  padding: 8rpx 0;
  font-size: $gp-font-size-base;
}

.row__label {
  flex-shrink: 0;
  width: 160rpx;
  color: $gp-text-placeholder;
}

.row__value {
  flex: 1;
  min-width: 0;
  color: $gp-text-main;
  word-break: break-all;
}

.action-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
  background-color: $gp-bg-card;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06);
}

.action-bar__inner {
  display: flex;
  align-items: center;
  height: $action-bar-height;
  padding: 0 $gp-spacing-base;
}

.action-bar__source {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.action-bar__label {
  color: $gp-text-placeholder;
  font-size: 22rpx;
}

.action-bar__name {
  overflow: hidden;
  color: $gp-text-main;
  font-size: $gp-font-size-sm;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.action-bar__button {
  flex-shrink: 0;
  margin-left: $gp-spacing-base;
  padding: 18rpx 72rpx;
  background-color: $gp-color-primary;
  border-radius: 999rpx;
  color: #ffffff;
  font-size: $gp-font-size-lg;
  font-weight: 600;
}

.action-bar__button--hover {
  opacity: 0.85;
}
</style>
