<template>
  <view class="demo-notice">
    <uni-icons type="info" size="14" color="#D46B08" />
    <text class="demo-notice__text">演示数据：商品、价格与优惠均为虚构，仅用于界面展示</text>
  </view>
</template>

<style lang="scss" scoped>
.demo-notice {
  display: flex;
  align-items: center;
  padding: 12rpx $gp-spacing-base;
  background-color: #fff7e6;
  border-radius: $gp-radius-base;
}

.demo-notice__text {
  flex: 1;
  min-width: 0;
  margin-left: 8rpx;
  color: #d46b08;
  font-size: 22rpx;
}
</style>
