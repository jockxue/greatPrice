<template>
  <div v-loading="loading">
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <template v-else-if="data">
      <div class="panel">
        <el-alert v-if="data.demo" title="这是演示商品，不能把它写成真实价格历史。" type="warning" show-icon :closable="false" />
        <el-descriptions :column="2" border style="margin-top: 12px">
          <el-descriptions-item label="编码">{{ data.productCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ productStatusLabel[data.status] }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ data.title }}</el-descriptions-item>
          <el-descriptions-item label="品牌">{{ data.brand || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ data.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="图片">{{ data.imageUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="规格" :span="2">{{ data.specification || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ data.description || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="form-actions">
          <el-button type="primary" @click="router.push(`/products/${data.id}/edit`)">编辑</el-button>
          <el-button @click="router.push({ path: '/deals', query: { productId: data.id } })">查看关联好价</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { product } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Product } from '@/api/types'
import { productStatusLabel } from '@/domain/labels'

const route = useRoute()
const router = useRouter()
const data = ref<Product | null>(null)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await product(Number(route.params.id))
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
