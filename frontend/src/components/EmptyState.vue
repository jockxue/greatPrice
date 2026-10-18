<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    /** uni-icons 图标名 */
    icon?: string
    /** 不传则不显示按钮 */
    actionText?: string
  }>(),
  { icon: 'info' },
)

defineEmits<{
  action: []
}>()
</script>

<template>
  <view class="empty">
    <uni-icons :type="icon" size="56" color="#C8C8C8" />
    <text class="empty__title">{{ title }}</text>
    <text v-if="description" class="empty__desc">{{ description }}</text>
    <view v-if="actionText" class="empty__action" hover-class="empty__action--hover" @click="$emit('action')">
      {{ actionText }}
    </view>
  </view>
</template>

<style lang="scss" scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 96rpx $gp-spacing-lg;
  text-align: center;
}

.empty__title {
  margin-top: $gp-spacing-base;
  color: $gp-text-main;
  font-size: $gp-font-size-lg;
  font-weight: 600;
}

.empty__desc {
  margin-top: $gp-spacing-sm;
  color: $gp-text-secondary;
  font-size: $gp-font-size-sm;
}

.empty__action {
  margin-top: $gp-spacing-lg;
  padding: 14rpx 48rpx;
  border: 1px solid $gp-color-primary;
  border-radius: 999rpx;
  color: $gp-color-primary;
  font-size: $gp-font-size-base;
}

.empty__action--hover {
  background-color: rgba(230, 46, 46, 0.08);
}
</style>
