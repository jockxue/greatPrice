<script setup lang="ts">
/** 分页列表底部状态：加载中 / 加载失败可重试 / 没有更多 / 加载更多。列表为空且不在加载时不显示，由页面展示空状态或错误状态。 */
defineProps<{
  loading: boolean
  error: string
  finished: boolean
  hasItems: boolean
}>()

defineEmits<{
  retry: []
  more: []
}>()
</script>

<template>
  <view v-if="loading || hasItems" class="list-footer">
    <uni-load-more v-if="loading" status="loading" />
    <view v-else-if="error" class="list-footer__text list-footer__text--error" @click="$emit('retry')">加载失败，点击重试</view>
    <view v-else-if="finished" class="list-footer__text">没有更多了</view>
    <view v-else class="list-footer__text" @click="$emit('more')">上拉或点击加载更多</view>
  </view>
</template>

<style lang="scss" scoped>
.list-footer__text {
  padding: $gp-spacing-base 0 $gp-spacing-lg;
  color: $gp-text-placeholder;
  font-size: $gp-font-size-sm;
  text-align: center;
}

.list-footer__text--error {
  color: $gp-color-primary;
}
</style>
