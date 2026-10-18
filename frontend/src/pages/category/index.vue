<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import DealCard from '@/components/DealCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listCategories, listDeals } from '@/services/deal'
import { useCategoryStore } from '@/stores/category'
import type { Category, Deal } from '@/types/deal'
import { goHome } from '@/utils/navigation'

const categoryStore = useCategoryStore()
const { selectedId } = storeToRefs(categoryStore)

const categories = ref<Category[]>([])
const deals = ref<Deal[]>([])
const loading = ref(true)

const currentName = computed(
  () => categories.value.find((c) => c.id === selectedId.value)?.name ?? '全部好价',
)

listCategories().then((list) => {
  categories.value = list
})

/** 首页通过 store 切换分类后切到本页，watch 会自动刷新列表 */
watch(
  selectedId,
  async (id) => {
    loading.value = true
    deals.value = await listDeals(id ?? undefined)
    loading.value = false
  },
  { immediate: true },
)
</script>

<template>
  <view class="category-page">
    <scroll-view class="sidebar" scroll-y>
      <view
        class="sidebar__item"
        :class="{ 'sidebar__item--active': selectedId === null }"
        @click="categoryStore.select(null)"
      >
        全部
      </view>
      <view
        v-for="category in categories"
        :key="category.id"
        class="sidebar__item"
        :class="{ 'sidebar__item--active': selectedId === category.id }"
        @click="categoryStore.select(category.id)"
      >
        {{ category.name }}
      </view>
    </scroll-view>

    <!-- key 随分类变化，切换分类时列表回到顶部 -->
    <scroll-view :key="selectedId ?? 'all'" class="content" scroll-y>
      <view class="content__header">
        <text class="content__title">{{ currentName }}</text>
        <text v-if="!loading" class="content__count">共 {{ deals.length }} 条</text>
      </view>
      <uni-load-more v-if="loading" status="loading" />
      <template v-else>
        <view v-for="deal in deals" :key="deal.id" class="content__item">
          <DealCard :deal="deal" compact />
        </view>
        <EmptyState
          v-if="deals.length === 0"
          icon="shop"
          title="该分类暂无好价"
          description="换个分类看看，或回首页逛逛"
          action-text="回首页逛逛"
          @action="goHome"
        />
      </template>
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.category-page {
  display: flex;
  /* --window-top / --window-bottom 由 uni-app 提供：H5 为导航栏和 TabBar 高度，小程序中为 0 */
  height: calc(100vh - var(--window-top) - var(--window-bottom));
}

.sidebar {
  flex-shrink: 0;
  width: 180rpx;
  height: 100%;
  background-color: #f8f8f8;
}

.sidebar__item {
  position: relative;
  padding: 28rpx 0;
  color: $gp-text-secondary;
  font-size: $gp-font-size-base;
  text-align: center;
}

.sidebar__item--active {
  background-color: $gp-bg-card;
  color: $gp-color-primary;
  font-weight: 600;

  &::before {
    position: absolute;
    top: 50%;
    left: 0;
    width: 6rpx;
    height: 32rpx;
    margin-top: -16rpx;
    background-color: $gp-color-primary;
    border-radius: 0 6rpx 6rpx 0;
    content: '';
  }
}

.content {
  flex: 1;
  height: 100%;
  min-width: 0;
}

.content__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: $gp-spacing-base $gp-spacing-base $gp-spacing-sm;
}

.content__title {
  color: $gp-text-main;
  font-size: $gp-font-size-lg;
  font-weight: 700;
}

.content__count {
  color: $gp-text-placeholder;
  font-size: $gp-font-size-sm;
}

.content__item {
  margin: 0 $gp-spacing-sm $gp-spacing-sm;
}
</style>
