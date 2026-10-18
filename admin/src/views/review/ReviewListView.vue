<template>
  <div class="panel">
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <el-table v-else :data="records" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="商品" min-width="180" />
      <el-table-column prop="currentPrice" label="价格" width="110" />
      <el-table-column label="来源" width="120">
        <template #default="{ row }">{{ labelOf(sourceLabel, row.sourceType) }}</template>
      </el-table-column>
      <el-table-column prop="submitterName" label="提交人" width="120" />
      <el-table-column label="核验" width="160">
        <template #default="{ row }">{{ row.verifiedAt || '未核验' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/reviews/${row.id}`)">审核</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="没有待审核好价" /></template>
    </el-table>
    <div class="pager">
      <el-pagination layout="total, prev, pager, next" :total="total" :page-size="20" :current-page="page" @current-change="changePage" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { reviews } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Deal } from '@/api/types'
import { labelOf, sourceLabel } from '@/domain/labels'

const router = useRouter()
const page = ref(1)
const total = ref(0)
const records = ref<Deal[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await reviews({ page: page.value, size: 20, sortBy: 'CREATED_AT_DESC' })
    records.value = data.records
    total.value = data.total
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '审核列表加载失败'
  } finally {
    loading.value = false
  }
}

function changePage(next: number) {
  page.value = next
  load()
}

onMounted(load)
</script>
