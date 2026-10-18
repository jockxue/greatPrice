<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import CategoryIcon from '@/components/CategoryIcon.vue'
import DealCard from '@/components/DealCard.vue'
import DealImage from '@/components/DealImage.vue'
import DemoNotice from '@/components/DemoNotice.vue'
import PriceText from '@/components/PriceText.vue'
import { getNavBarMetrics } from '@/platform'
import { listCategories, listDeals, listFeaturedDeals } from '@/services/deal'
import { useCategoryStore } from '@/stores/category'
import type { Category, Deal } from '@/types/deal'
import { goCategory, goDealDetail, goSearch } from '@/utils/navigation'

/** 「更多」使用色板中的灰色 */
const MORE_COLOR_SEED = 7

const nav = getNavBarMetrics()
const categoryStore = useCategoryStore()

const loading = ref(true)
const categories = ref<Category[]>([])
const featuredDeals = ref<Deal[]>([])
const deals = ref<Deal[]>([])

onLoad(async () => {
  const [categoryList, featuredList, dealList] = await Promise.all([
    listCategories(),
    listFeaturedDeals(),
    listDeals(),
  ])
  categories.value = categoryList
  featuredDeals.value = featuredList
  deals.value = dealList
  loading.value = false
})

/** id 为 null 表示查看全部分类 */
function openCategory(id: number | null) {
  categoryStore.select(id)
  goCategory()
}
</script>

<template>
  <view class="home">
    <view class="header" :style="{ paddingTop: `${nav.statusBarHeight}px` }">
      <view class="header__bar" :style="{ height: `${nav.navBarHeight}px`, paddingRight: `${nav.capsuleReservedWidth}px` }">
        <text class="header__logo">好价导购</text>
        <text class="header__slogan">发现值得买的好价</text>
      </view>
      <view class="header__search" hover-class="header__search--hover" @click="goSearch()">
        <uni-icons type="search" size="16" color="#999999" />
        <text class="header__search-text">搜索商品名称、品牌或分类</text>
      </view>
    </view>

    <view class="section">
      <DemoNotice />
    </view>

    <view class="section card categories">
      <view
        v-for="category in categories"
        :key="category.id"
        class="categories__item"
        hover-class="categories__item--hover"
        @click="openCategory(category.id)"
      >
        <CategoryIcon :name="category.name" :color-seed="category.id" />
        <text class="categories__name">{{ category.name }}</text>
      </view>
      <view v-if="categories.length" class="categories__item" hover-class="categories__item--hover" @click="openCategory(null)">
        <CategoryIcon name="更多" :color-seed="MORE_COLOR_SEED" />
        <text class="categories__name">更多</text>
      </view>
    </view>

    <view v-if="featuredDeals.length" class="section">
      <view class="section__header">
        <text class="section__title">编辑精选</text>
        <text class="section__subtitle">演示数据</text>
      </view>
      <scroll-view class="featured" scroll-x :show-scrollbar="false">
        <view
          v-for="deal in featuredDeals"
          :key="deal.id"
          class="featured__item"
          hover-class="featured__item--hover"
          @click="goDealDetail(deal.id)"
        >
          <view class="featured__image">
            <DealImage :image="deal.image" :label="deal.brand" :color-seed="deal.categoryId" />
          </view>
          <view class="featured__title">{{ deal.title }}</view>
          <PriceText :price="deal.price" :currency="deal.currency" />
        </view>
      </scroll-view>
    </view>

    <view class="section">
      <view class="section__header">
        <text class="section__title">最新好价</text>
      </view>
      <uni-load-more v-if="loading" status="loading" />
      <view v-for="deal in deals" :key="deal.id" class="feed__item">
        <DealCard :deal="deal" />
      </view>
      <view v-if="!loading" class="feed__end">没有更多了</view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 10;
  padding-bottom: $gp-spacing-base;
  background: linear-gradient(180deg, #e62e2e 0%, #f0503c 100%);
}

.header__bar {
  display: flex;
  align-items: center;
  padding-left: $gp-spacing-base;
}

.header__logo {
  color: #ffffff;
  font-size: $gp-font-size-xl;
  font-weight: 700;
}

.header__slogan {
  margin-left: $gp-spacing-sm;
  color: rgba(255, 255, 255, 0.85);
  font-size: $gp-font-size-sm;
}

.header__search {
  display: flex;
  align-items: center;
  height: 72rpx;
  margin: $gp-spacing-sm $gp-spacing-base 0;
  padding: 0 $gp-spacing-base;
  background-color: #ffffff;
  border-radius: 999rpx;
}

.header__search--hover {
  opacity: 0.9;
}

.header__search-text {
  margin-left: 12rpx;
  color: $gp-text-placeholder;
  font-size: $gp-font-size-base;
}

.section {
  margin: $gp-spacing-base $gp-spacing-base 0;
}

.section__header {
  display: flex;
  align-items: baseline;
  margin-bottom: $gp-spacing-sm;
}

.section__title {
  color: $gp-text-main;
  font-size: $gp-font-size-lg;
  font-weight: 700;
}

.section__subtitle {
  margin-left: $gp-spacing-sm;
  color: $gp-text-placeholder;
  font-size: 22rpx;
}

.card {
  background-color: $gp-bg-card;
  border-radius: $gp-radius-base;
}

.categories {
  display: flex;
  flex-wrap: wrap;
  padding: $gp-spacing-base 0 0;
}

.categories__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 25%;
  margin-bottom: $gp-spacing-base;
}

.categories__item--hover {
  opacity: 0.7;
}

.categories__name {
  margin-top: 12rpx;
  color: $gp-text-main;
  font-size: $gp-font-size-sm;
}

.featured {
  width: 100%;
  white-space: nowrap;
}

.featured__item {
  display: inline-block;
  box-sizing: border-box;
  width: 260rpx;
  margin-right: $gp-spacing-sm;
  padding: $gp-spacing-sm;
  vertical-align: top;
  background-color: $gp-bg-card;
  border-radius: $gp-radius-base;
  white-space: normal;
}

.featured__item:last-child {
  margin-right: 0;
}

.featured__item--hover {
  opacity: 0.85;
}

.featured__image {
  width: 228rpx;
  height: 228rpx;
  border-radius: $gp-radius-base;
  overflow: hidden;
}

.featured__title {
  display: -webkit-box;
  height: 2.9em;
  margin: 12rpx 0 4rpx;
  overflow: hidden;
  color: $gp-text-main;
  font-size: $gp-font-size-sm;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  word-break: break-all;
}

.feed__item {
  margin-bottom: $gp-spacing-sm;
}

.feed__end {
  padding: $gp-spacing-base 0 $gp-spacing-lg;
  color: $gp-text-placeholder;
  font-size: $gp-font-size-sm;
  text-align: center;
}
</style>
