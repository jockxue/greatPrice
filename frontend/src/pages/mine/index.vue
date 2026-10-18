<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { showToast } from '@/platform'
import { getBackendStatus, type BackendStatus } from '@/services/system'
import { ROUTES } from '@/utils/navigation'

const statusText: Record<BackendStatus, string> = {
  ok: '正常',
  'database-down': '已连接，数据库不可用',
  unreachable: '无法连接',
}

const backendStatus = ref<BackendStatus>()

onShow(async () => {
  backendStatus.value = await getBackendStatus()
})

function comingSoon(feature: string) {
  showToast(`${feature}将在后续阶段开放`)
}

function openSubmit() {
  uni.navigateTo({ url: ROUTES.submit })
}

function showDemoInfo() {
  uni.showModal({
    title: '关于演示数据',
    content: '当前版本的商品、品牌、商城名称、价格和优惠均为虚构的演示数据，仅用于界面展示，不代表任何真实商品行情。',
    showCancel: false,
    confirmText: '知道了',
  })
}
</script>

<template>
  <view class="mine">
    <view class="profile" hover-class="profile--hover" @click="comingSoon('登录')">
      <view class="profile__avatar">
        <uni-icons type="person" size="32" color="#FFFFFF" />
      </view>
      <view class="profile__info">
        <text class="profile__name">点击登录</text>
        <text class="profile__desc">登录后可同步收藏与爆料</text>
      </view>
      <uni-icons type="right" size="16" color="#C0C0C0" />
    </view>

    <view class="group">
      <uni-list :border="false">
        <uni-list-item title="我的收藏" show-arrow clickable @click="comingSoon('收藏')" />
        <uni-list-item title="我要爆料" show-arrow clickable @click="openSubmit" />
      </uni-list>
    </view>

    <view class="group">
      <uni-list :border="false">
        <uni-list-item title="关于演示数据" show-arrow clickable @click="showDemoInfo" />
        <uni-list-item title="后端服务状态" :right-text="backendStatus ? statusText[backendStatus] : '检查中…'" />
      </uni-list>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.mine {
  padding-bottom: $gp-spacing-lg;
}

.profile {
  display: flex;
  align-items: center;
  padding: $gp-spacing-lg $gp-spacing-base;
  background: linear-gradient(180deg, #e62e2e 0%, #f0503c 100%);
}

.profile--hover {
  opacity: 0.95;
}

.profile__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 112rpx;
  height: 112rpx;
  background-color: rgba(255, 255, 255, 0.25);
  border-radius: 50%;
}

.profile__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  margin-left: $gp-spacing-base;
}

.profile__name {
  color: #ffffff;
  font-size: $gp-font-size-lg;
  font-weight: 600;
}

.profile__desc {
  margin-top: 4rpx;
  color: rgba(255, 255, 255, 0.85);
  font-size: $gp-font-size-sm;
}

.group {
  margin: $gp-spacing-base $gp-spacing-base 0;
  border-radius: $gp-radius-base;
  overflow: hidden;
}
</style>
