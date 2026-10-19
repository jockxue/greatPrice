<template>
  <div>
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <template v-else>
      <div class="stats" v-loading="loading">
        <div class="stat"><span>商品总数</span><b>{{ data?.productCount ?? '-' }}</b></div>
        <div class="stat"><span>已发布且未过期</span><b>{{ data?.publishedCount ?? '-' }}</b></div>
        <div class="stat"><span>待审核</span><b>{{ data?.pendingCount ?? '-' }}</b></div>
        <div class="stat"><span>已下架</span><b>{{ data?.offlineCount ?? '-' }}</b></div>
        <div class="stat"><span>已过期</span><b>{{ data?.expiredCount ?? '-' }}</b></div>
      </div>
      <div class="panel" style="margin-bottom: 16px">
        <h3>自动过期任务</h3>
        <el-empty v-if="!data?.lastExpiryRun" description="尚未执行" />
        <el-descriptions v-else :column="4" border>
          <el-descriptions-item label="最近执行">{{ data.lastExpiryRun.finishedAt }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ data.lastExpiryRun.status === 'SUCCESS' ? '成功' : '失败' }}</el-descriptions-item>
          <el-descriptions-item label="处理数量">{{ data.lastExpiryRun.processedCount }}</el-descriptions-item>
          <el-descriptions-item label="失败批次">{{ data.lastExpiryRun.failedCount }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="panel">
        <h3>最近更新的好价</h3>
        <el-table :data="data?.recentDeals ?? []" v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="商品" min-width="180" />
          <el-table-column prop="currentPrice" label="价格" width="120" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }"><StatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="演示" width="90">
            <template #default="{ row }">{{ row.demo ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(`/deals/${row.id}`)">查看</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无好价" /></template>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dashboard } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Dashboard } from '@/api/types'
import StatusTag from '@/components/StatusTag.vue'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const data = ref<Dashboard | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await dashboard()
  } catch (err) {
    data.value = null
    error.value = err instanceof ApiError ? err.message : '统计加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
