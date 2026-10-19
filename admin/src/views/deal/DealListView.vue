<template>
  <div class="panel">
    <div class="filters">
      <el-input v-model="keyword" placeholder="商品名称 / 编码 / 好价 ID" clearable style="width: 240px" />
      <el-select v-model="categoryId" clearable placeholder="分类" style="width: 150px">
        <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="status" clearable placeholder="状态" style="width: 140px">
        <el-option v-for="(label, value) in dealStatusLabel" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="sourceType" clearable placeholder="来源" style="width: 140px">
        <el-option v-for="(label, value) in sourceLabel" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="sortBy" style="width: 170px">
        <el-option label="创建时间倒序" value="CREATED_AT_DESC" />
        <el-option label="创建时间正序" value="CREATED_AT_ASC" />
        <el-option label="发布时间倒序" value="PUBLISHED_AT_DESC" />
        <el-option label="发布时间正序" value="PUBLISHED_AT_ASC" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="router.push('/deals/create')">录入好价</el-button>
    </div>
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <el-table v-else :data="records" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="商品" min-width="180" />
      <el-table-column prop="productCode" label="编码" width="140" />
      <el-table-column prop="currentPrice" label="价格" width="110" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="结束时间" min-width="170">
        <template #default="{ row }">{{ row.expiresAt || '未设置' }}</template>
      </el-table-column>
      <el-table-column label="到期" width="90">
        <template #default="{ row }">{{ row.expired ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="来源" width="110">
        <template #default="{ row }">{{ labelOf(sourceLabel, row.sourceType) }}</template>
      </el-table-column>
      <el-table-column label="演示" width="80">
        <template #default="{ row }">{{ row.demo ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/deals/${row.id}`)">详情</el-button>
          <el-button v-if="row.status === 'DRAFT' || row.status === 'PENDING_REVIEW'" link type="primary" @click="router.push(`/deals/${row.id}/edit`)">编辑</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="没有符合条件的好价" /></template>
    </el-table>
    <div class="pager">
      <el-pagination layout="total, prev, pager, next" :total="total" :page-size="20" :current-page="page" @current-change="changePage" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { categories, deals } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Category, Deal } from '@/api/types'
import StatusTag from '@/components/StatusTag.vue'
import { dealStatusLabel, labelOf, sourceLabel } from '@/domain/labels'

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const categoryId = ref<number | undefined>()
const status = ref<string | undefined>()
const sourceType = ref<string | undefined>()
const sortBy = ref('CREATED_AT_DESC')
const productId = ref<number | undefined>(route.query.productId ? Number(route.query.productId) : undefined)
const page = ref(1)
const total = ref(0)
const records = ref<Deal[]>([])
const categoryOptions = ref<Category[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await deals({
      page: page.value,
      size: 20,
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value,
      status: status.value,
      sourceType: sourceType.value,
      sortBy: sortBy.value,
      productId: productId.value,
    })
    records.value = data.records
    total.value = data.total
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '好价列表加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function changePage(next: number) {
  page.value = next
  load()
}

watch(() => route.query.productId, (value) => {
  productId.value = value ? Number(value) : undefined
  search()
})

onMounted(async () => {
  categoryOptions.value = await categories().catch(() => [])
  await load()
})
</script>
