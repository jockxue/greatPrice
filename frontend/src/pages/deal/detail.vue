<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchDealDetail } from '@/api/deal'
import DealImage from '@/components/DealImage.vue'
import DemoNotice from '@/components/DemoNotice.vue'
import EmptyState from '@/components/EmptyState.vue'
import PriceText from '@/components/PriceText.vue'
import { openExternalUrl, showToast } from '@/platform'
import { getBuyUrl, isDemoDeal } from '@/services/deal'
import { API_ERROR_CODE } from '@/types/api'
import type { DealDetail } from '@/types/deal'
import { formatDateTime } from '@/utils/format'
import { goHome } from '@/utils/navigation'
import { ApiError } from '@/utils/request'

type LoadState = 'loading' | 'ready' | 'not-found' | 'unavailable' | 'error'

const state = ref<LoadState>('loading')
const errorMessage = ref('')
const deal = ref<DealDetail>()
let dealId = 0

const buyUrl = computed(() => (deal.value ? getBuyUrl(deal.value) : undefined))
const isDemo = computed(() => !!deal.value && isDemoDeal(deal.value))

const validityText = computed(() => {
  const d = deal.value
  if (!d || (!d.startsAt && !d.expiresAt)) return '未注明，以商城页面为准'
  if (d.startsAt && d.expiresAt) return `${formatDateTime(d.startsAt)} 至 ${formatDateTime(d.expiresAt)}`
  return d.expiresAt ? `截至 ${formatDateTime(d.expiresAt)}` : `${formatDateTime(d.startsAt as string)} 起`
})

const infoRows = computed(() => {
  if (!deal.value) return []
  return [
    { label: '商品来源', value: deal.value.sourceName },
    { label: '促销有效期', value: validityText.value },
    { label: '发布时间', value: formatDateTime(deal.value.publishedAt) },
    { label: '核验时间', value: deal.value.verifiedAt ? formatDateTime(deal.value.verifiedAt) : '暂未核验' },
  ]
})

async function load() {
  if (!Number.isInteger(dealId) || dealId <= 0) {
    state.value = 'not-found'
    return
  }
  state.value = 'loading'
  try {
    deal.value = await fetchDealDetail(dealId)
    state.value = 'ready'
  } catch (e) {
    const code = e instanceof ApiError ? e.code : API_ERROR_CODE.NETWORK
    if (code === API_ERROR_CODE.NOT_FOUND) {
      state.value = 'not-found'
    } else if (code === API_ERROR_CODE.DEAL_UNAVAILABLE) {
      state.value = 'unavailable'
    } else {
      errorMessage.value = e instanceof Error ? e.message : '加载失败，请稍后重试'
      state.value = 'error'
    }
  }
}

onLoad((query) => {
  dealId = Number(query?.id)
  load()
})

/** 只有后端返回了合法的 https 链接才允许跳转；前端不拼接、不伪造任何购买或推广链接 */
function buy() {
  if (!buyUrl.value) {
    showToast('该好价暂未提供购买链接')
    return
  }
  openExternalUrl(buyUrl.value)
}
</script>

<template>
  <view>
    <uni-load-more v-if="state === 'loading'" status="loading" />

    <EmptyState
      v-else-if="state === 'not-found'"
      icon="info"
      title="好价不存在"
      description="可能是链接有误，或该好价已被移除"
      action-text="返回首页"
      @action="goHome"
    />

    <EmptyState
      v-else-if="state === 'unavailable'"
      icon="info"
      title="好价已下架或已过期"
      description="该优惠已结束，去首页看看其他好价吧"
      action-text="返回首页"
      @action="goHome"
    />

    <EmptyState
      v-else-if="state === 'error' || !deal"
      icon="info"
      title="加载失败"
      :description="errorMessage"
      action-text="重新加载"
      @action="load"
    />

    <view v-else class="detail">
      <view class="detail__image">
        <DealImage :image="deal.imageUrl" :label="deal.brand || deal.title" :color-seed="deal.categoryId" />
      </view>

      <view class="card">
        <PriceText :price="deal.currentPrice" :original-price="deal.originalPrice" :currency="deal.currency" size="lg" />
        <view class="detail__title">{{ deal.title }}</view>
        <view v-if="deal.brand || deal.categoryName" class="detail__chips">
          <text v-if="deal.brand" class="chip">{{ deal.brand }}</text>
          <text v-if="deal.categoryName" class="chip">{{ deal.categoryName }}</text>
        </view>
      </view>

      <view v-if="isDemo" class="detail__notice">
        <DemoNotice />
      </view>

      <view class="card">
        <view class="card__title">优惠说明</view>
        <view class="card__text">{{ deal.dealDescription }}</view>
      </view>

      <view v-if="deal.specification" class="card">
        <view class="card__title">规格</view>
        <view class="card__text">{{ deal.specification }}</view>
      </view>

      <view v-if="deal.description" class="card">
        <view class="card__title">商品描述</view>
        <view class="card__text">{{ deal.description }}</view>
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
          <view
            class="action-bar__button"
            :class="{ 'action-bar__button--disabled': !buyUrl }"
            :hover-class="buyUrl ? 'action-bar__button--hover' : 'none'"
            @click="buy"
          >
            {{ buyUrl ? '去购买' : '暂无购买链接' }}
          </view>
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

.action-bar__button--disabled {
  padding: 18rpx 48rpx;
  background-color: #cccccc;
  font-size: $gp-font-size-base;
}
</style>
