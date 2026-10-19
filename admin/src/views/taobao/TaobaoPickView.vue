<template>
  <div class="panel">
    <div class="filters">
      <el-input v-model="keyword" placeholder="搜索关键词" clearable style="width: 220px" @keyup.enter="search" />
      <el-input v-model="startPrice" placeholder="最低价" clearable style="width: 120px" />
      <el-input v-model="endPrice" placeholder="最高价" clearable style="width: 120px" />
      <el-select v-model="sort" clearable placeholder="排序" style="width: 180px">
        <el-option v-for="item in sorts" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="categoryId" placeholder="导入分类" style="width: 160px">
        <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <el-table v-loading="loading" :data="items">
      <el-table-column label="图片" width="88">
        <template #default="{ row }">
          <img v-if="row.imageUrl" :src="row.imageUrl" alt="" class="thumb" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="itemId" label="商品 ID" width="160" />
      <el-table-column label="折扣价" width="110">
        <template #default="{ row }">{{ priceText(row.finalPrice, row.finalPriceStatus) }}</template>
      </el-table-column>
      <el-table-column prop="shopTitle" label="店铺" width="140" />
      <el-table-column label="优惠" min-width="140">
        <template #default="{ row }">{{ row.couponInfo || '接口未返回优惠券' }}</template>
      </el-table-column>
      <el-table-column label="推广链接" width="120">
        <template #default="{ row }">{{ row.promotionLinkGenerated ? '已转链' : '未转链' }}</template>
      </el-table-column>
      <el-table-column label="站内" width="120">
        <template #default="{ row }">{{ row.importedDealStatus || '未导入' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="busy" @click="openDetail(row.itemId)">详情</el-button>
          <el-button link type="primary" :disabled="busy || !row.itemId" @click="link(row)">转链</el-button>
          <el-button link type="primary" :disabled="busy || !row.itemId" @click="importItem(row)">导入</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="输入关键词后再搜索，页面打开不会请求淘宝" /></template>
    </el-table>
    <div class="pager">
      <el-pagination layout="total, prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="changePage" />
    </div>
    <el-dialog v-model="detailVisible" title="商品详情" width="640px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="标题">{{ detail.title || '未返回' }}</el-descriptions-item>
        <el-descriptions-item label="商品 ID">{{ detail.itemId || '未返回' }}</el-descriptions-item>
        <el-descriptions-item label="一口价">{{ priceText(detail.reservePrice, detail.reservePriceStatus) }}</el-descriptions-item>
        <el-descriptions-item label="折扣价">{{ priceText(detail.finalPrice, detail.finalPriceStatus) }}</el-descriptions-item>
        <el-descriptions-item label="活动价">{{ priceText(detail.salePrice, detail.salePriceStatus) }}</el-descriptions-item>
        <el-descriptions-item label="店铺">{{ detail.shopTitle || '未返回' }}</el-descriptions-item>
        <el-descriptions-item label="类目">{{ detail.catLeafName || detail.catName || detail.categoryName || '未返回' }}</el-descriptions-item>
        <el-descriptions-item label="活动">{{ detail.playInfo || detail.presaleText || detail.kuadian || '未返回' }}</el-descriptions-item>
        <el-descriptions-item label="原始链接">{{ detail.itemUrl || '未返回' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { categories, taobaoDetail, taobaoImport, taobaoItems, taobaoPromotion } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Category, TaobaoItem } from '@/api/types'

const sorts = [
  { label: '销量降序', value: 'total_sales_des' },
  { label: '销量升序', value: 'total_sales_asc' },
  { label: '价格降序', value: 'price_des' },
  { label: '价格升序', value: 'price_asc' },
  { label: '佣金比率降序', value: 'tk_rate_des' },
  { label: '匹配分降序', value: 'match_des' },
]

const keyword = ref('')
const startPrice = ref('')
const endPrice = ref('')
const sort = ref('')
const categoryId = ref<number>()
const categoryOptions = ref<Category[]>([])
const items = ref<TaobaoItem[]>([])
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const detailVisible = ref(false)
const detail = ref<TaobaoItem>()

function priceText(value: string | null, status: string) {
  if (status === 'MISSING') return '未返回'
  if (status === 'INVALID') return '金额无效'
  return value || '未返回'
}

async function loadCategories() {
  categoryOptions.value = (await categories()).filter((item) => item.status === 'ENABLED')
}

async function search() {
  if (!keyword.value.trim()) {
    error.value = '请输入关键词'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await taobaoItems({
      keyword: keyword.value.trim(),
      page: page.value,
      size,
      startPrice: startPrice.value.trim() || undefined,
      endPrice: endPrice.value.trim() || undefined,
      sort: sort.value || undefined,
    })
    items.value = data.items
    total.value = data.total
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '搜索失败'
    items.value = []
  } finally {
    loading.value = false
  }
}

function changePage(next: number) {
  page.value = next
  search()
}

async function openDetail(itemId: string | null) {
  if (!itemId || busy.value) return
  busy.value = true
  try {
    const data = await taobaoDetail(itemId)
    detail.value = data.items[0]
    detailVisible.value = true
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '详情加载失败'
  } finally {
    busy.value = false
  }
}

async function link(row: TaobaoItem) {
  if (!row.itemId || busy.value) return
  busy.value = true
  try {
    const data = await taobaoPromotion(row.itemId, row.importedDealId)
    ElMessage.success(data.persisted ? '推广链接已写入草稿' : '已生成链接，但未写入站内好价')
    ElMessage.info(data.clientNote)
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '转链失败'
  } finally {
    busy.value = false
  }
}

async function importItem(row: TaobaoItem) {
  if (!row.itemId || busy.value) return
  if (!categoryId.value) {
    error.value = '请先选择导入分类'
    return
  }
  busy.value = true
  try {
    const data = await taobaoImport(row.itemId, categoryId.value)
    ElMessage.success(data.message)
    row.importedProductId = data.productId
    row.importedDealId = data.dealId
    row.importedDealStatus = data.dealStatus
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '导入失败'
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  try {
    await loadCategories()
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '分类加载失败'
  }
})
</script>

<style scoped>
.thumb { width: 56px; height: 56px; object-fit: cover; }
</style>
