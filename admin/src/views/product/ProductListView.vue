<template>
  <div class="panel">
    <div class="filters">
      <el-input v-model="keyword" placeholder="名称 / 品牌 / 编码" clearable style="width: 220px" />
      <el-select v-model="categoryId" clearable placeholder="分类" style="width: 160px">
        <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="status" clearable placeholder="状态" style="width: 140px">
        <el-option label="启用" value="ACTIVE" />
        <el-option label="停用" value="INACTIVE" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="router.push('/products/create')">新建商品</el-button>
    </div>
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <el-table v-else :data="records" v-loading="loading">
      <el-table-column prop="productCode" label="编码" width="160" />
      <el-table-column prop="title" label="名称" min-width="180" />
      <el-table-column prop="brand" label="品牌" width="120" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ productStatusLabel[row.status] }}</template>
      </el-table-column>
      <el-table-column label="演示" width="80">
        <template #default="{ row }">{{ row.demo ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/products/${row.id}`)">详情</el-button>
          <el-button link type="primary" @click="router.push(`/products/${row.id}/edit`)">编辑</el-button>
          <el-button link type="warning" @click="toggle(row)">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="没有符合条件的商品" /></template>
    </el-table>
    <div class="pager">
      <el-pagination layout="total, prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="changePage" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { categories, deleteProduct, disableProduct, enableProduct, products } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Category, Product } from '@/api/types'
import { productStatusLabel } from '@/domain/labels'

const router = useRouter()
const keyword = ref('')
const categoryId = ref<number | undefined>()
const status = ref<string | undefined>()
const page = ref(1)
const size = 20
const total = ref(0)
const records = ref<Product[]>([])
const categoryOptions = ref<Category[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await products({
      page: page.value,
      size,
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value,
      status: status.value,
    })
    records.value = data.records
    total.value = data.total
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '商品列表加载失败'
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

async function toggle(row: Product) {
  const disabling = row.status === 'ACTIVE'
  try {
    await ElMessageBox.confirm(
      disabling ? '停用后，该商品下已发布的好价不会再出现在用户端。' : '重新启用后，仍处于发布状态的好价会按原规则对用户端可见。',
      disabling ? '停用商品' : '启用商品',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    if (disabling) await disableProduct(row.id)
    else await enableProduct(row.id)
    ElMessage.success('已更新')
    await load()
  } catch (err) {
    if (err instanceof ApiError) ElMessage.error(err.message)
  }
}

async function remove(row: Product) {
  try {
    await ElMessageBox.confirm('删除是逻辑删除，商品不再出现在列表中，关联好价也不会再对用户端公开。', '删除商品', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteProduct(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (err) {
    if (err instanceof ApiError) ElMessage.error(err.message)
  }
}

onMounted(async () => {
  categoryOptions.value = await categories().catch(() => [])
  await load()
})
</script>
