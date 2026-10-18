<script setup lang="ts">
import { computed } from 'vue'
import type { CurrencyCode } from '@/types/deal'
import { formatAmount, formatPrice, getCurrencySymbol } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    /** 后端金额字符串，如 "89.90" */
    price: string
    currency: CurrencyCode
    originalPrice?: string | null
    size?: 'md' | 'lg'
  }>(),
  { size: 'md' },
)

/** 参考价高于现价时才显示划线价；Number 只用于比较大小，展示仍使用原字符串 */
const originalText = computed(() =>
  props.originalPrice && Number(props.originalPrice) > Number(props.price)
    ? formatPrice(props.originalPrice, props.currency)
    : '',
)
</script>

<template>
  <view class="price" :class="`price--${size}`">
    <text class="price__symbol">{{ getCurrencySymbol(currency) }}</text>
    <text class="price__amount">{{ formatAmount(price) }}</text>
    <text v-if="originalText" class="price__original">{{ originalText }}</text>
  </view>
</template>

<style lang="scss" scoped>
.price {
  display: flex;
  align-items: baseline;
  color: $gp-color-price;
  white-space: nowrap;
}

.price__symbol {
  font-size: $gp-font-size-sm;
  font-weight: 600;
}

.price__amount {
  margin-left: 2rpx;
  font-size: $gp-font-size-xl;
  font-weight: 700;
}

.price__original {
  margin-left: $gp-spacing-sm;
  color: $gp-text-placeholder;
  font-size: $gp-font-size-sm;
  text-decoration: line-through;
}

.price--lg {
  .price__symbol {
    font-size: $gp-font-size-lg;
  }

  .price__amount {
    font-size: 56rpx;
  }

  .price__original {
    font-size: $gp-font-size-base;
  }
}
</style>
