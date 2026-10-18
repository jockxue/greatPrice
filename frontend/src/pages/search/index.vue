<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import DealCard from '@/components/DealCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { showToast } from '@/platform'
import { listHotKeywords, searchDeals } from '@/services/deal'
import type { Deal } from '@/types/deal'

const keyword = ref('')
/** 已执行搜索的关键词；为空表示尚未搜索，显示热门搜索 */
const searchedKeyword = ref('')
const results = ref<Deal[]>([])
const hotKeywords = ref<string[]>([])
const autoFocus = ref(false)

onLoad((query) => {
  listHotKeywords().then((list) => {
    hotKeywords.value = list
  })
  const initial = query?.keyword ? decodeURIComponent(query.keyword) : ''
  if (initial) {
    keyword.value = initial
    doSearch()
  } else {
    autoFocus.value = true
  }
})

async function doSearch() {
  const value = keyword.value.trim()
  if (!value) {
    showToast('请输入搜索关键词')
    return
  }
  results.value = await searchDeals(value)
  searchedKeyword.value = value
}

function searchHotKeyword(value: string) {
  keyword.value = value
  doSearch()
}

function clearKeyword() {
  keyword.value = ''
  searchedKeyword.value = ''
  results.value = []
}
</script>

<template>
  <view class="search-page">
    <view class="search-bar">
      <view class="search-bar__box">
        <uni-icons type="search" size="16" color="#999999" />
        <input
          v-model="keyword"
          class="search-bar__input"
          placeholder="搜索商品名称、品牌或分类"
          placeholder-class="search-bar__placeholder"
          confirm-type="search"
          :focus="autoFocus"
          @confirm="doSearch"
        />
        <view v-if="keyword" class="search-bar__clear" @click="clearKeyword">
          <uni-icons type="clear" size="18" color="#C0C0C0" />
        </view>
      </view>
      <view class="search-bar__button" hover-class="search-bar__button--hover" @click="doSearch">搜索</view>
    </view>

    <view v-if="!searchedKeyword" class="hot">
      <view class="hot__header">
        <text class="hot__title">热门搜索</text>
        <text class="hot__tag">示例</text>
      </view>
      <view class="hot__list">
        <view
          v-for="item in hotKeywords"
          :key="item"
          class="hot__item"
          hover-class="hot__item--hover"
          @click="searchHotKeyword(item)"
        >
          {{ item }}
        </view>
      </view>
    </view>

    <view v-else class="results">
      <template v-if="results.length">
        <view class="results__summary">找到 {{ results.length }} 条与“{{ searchedKeyword }}”相关的好价</view>
        <view v-for="deal in results" :key="deal.id" class="results__item">
          <DealCard :deal="deal" />
        </view>
      </template>
      <EmptyState
        v-else
        icon="search"
        title="没有找到相关好价"
        :description="`没有与“${searchedKeyword}”匹配的商品，换个关键词试试`"
        action-text="清空重新搜索"
        @action="clearKeyword"
      />
    </view>
  </view>
</template>

<style lang="scss" scoped>
.search-bar {
  position: sticky;
  top: var(--window-top);
  z-index: 10;
  display: flex;
  align-items: center;
  padding: $gp-spacing-sm $gp-spacing-base;
  background-color: $gp-bg-card;
}

.search-bar__box {
  display: flex;
  flex: 1;
  align-items: center;
  min-width: 0;
  height: 72rpx;
  padding: 0 $gp-spacing-base;
  background-color: $gp-bg-page;
  border-radius: 999rpx;
}

.search-bar__input {
  flex: 1;
  min-width: 0;
  margin-left: 12rpx;
  font-size: $gp-font-size-base;
}

.search-bar__placeholder {
  color: $gp-text-placeholder;
}

.search-bar__clear {
  display: flex;
  align-items: center;
  padding-left: $gp-spacing-sm;
}

.search-bar__button {
  flex-shrink: 0;
  margin-left: $gp-spacing-sm;
  padding: 12rpx 28rpx;
  background-color: $gp-color-primary;
  border-radius: 999rpx;
  color: #ffffff;
  font-size: $gp-font-size-base;
}

.search-bar__button--hover {
  opacity: 0.85;
}

.hot {
  padding: $gp-spacing-base;
}

.hot__header {
  display: flex;
  align-items: center;
  margin-bottom: $gp-spacing-sm;
}

.hot__title {
  color: $gp-text-main;
  font-size: $gp-font-size-base;
  font-weight: 600;
}

.hot__tag {
  margin-left: $gp-spacing-sm;
  padding: 0 10rpx;
  background-color: #fff7e6;
  border-radius: 6rpx;
  color: #d46b08;
  font-size: 20rpx;
}

.hot__list {
  display: flex;
  flex-wrap: wrap;
}

.hot__item {
  margin: 0 $gp-spacing-sm $gp-spacing-sm 0;
  padding: 10rpx 28rpx;
  background-color: $gp-bg-card;
  border-radius: 999rpx;
  color: $gp-text-secondary;
  font-size: $gp-font-size-sm;
}

.hot__item--hover {
  background-color: #eeeeee;
}

.results {
  padding: 0 $gp-spacing-base;
}

.results__summary {
  padding: $gp-spacing-base 0 $gp-spacing-sm;
  color: $gp-text-secondary;
  font-size: $gp-font-size-sm;
}

.results__item {
  margin-bottom: $gp-spacing-sm;
}
</style>
