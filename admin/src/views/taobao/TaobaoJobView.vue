<template>
  <div class="panel">
    <div class="filters">
      <el-select v-model="jobName" clearable placeholder="任务类型" style="width: 200px">
        <el-option v-for="item in names" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="status" clearable placeholder="结果" style="width: 140px">
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <el-table v-else :data="records" v-loading="loading">
      <el-table-column prop="finishedAt" label="结束时间" width="200" />
      <el-table-column prop="jobName" label="任务" width="180" />
      <el-table-column prop="status" label="结果" width="100" />
      <el-table-column prop="requestedCount" label="请求" width="80" />
      <el-table-column prop="processedCount" label="成功" width="80" />
      <el-table-column prop="failedCount" label="失败" width="80" />
      <el-table-column prop="errorCode" label="错误码" width="120" />
      <el-table-column prop="message" label="说明" min-width="220" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button v-if="row.retryable && row.status === 'FAILED'" link type="primary" :disabled="busyId === row.id" @click="retry(row.id)">
            重试
          </el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无同步记录" /></template>
    </el-table>
    <div class="pager">
      <el-pagination layout="total, prev, pager, next" :total="total" :page-size="20" :current-page="page" @current-change="changePage" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { taobaoJobs, taobaoRetry } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { TaobaoJob } from '@/api/types'

const names = ['TAOBAO_SEARCH', 'TAOBAO_ITEM_INFO', 'TAOBAO_PROMOTION', 'TAOBAO_IMPORT', 'TAOBAO_REFRESH', 'TAOBAO_API_TEST']
const jobName = ref('')
const status = ref('')
const page = ref(1)
const total = ref(0)
const records = ref<TaobaoJob[]>([])
const loading = ref(false)
const error = ref('')
const busyId = ref<number>()

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await taobaoJobs({
      page: page.value,
      size: 20,
      jobName: jobName.value || undefined,
      status: status.value || undefined,
    })
    records.value = data.records
    total.value = data.total
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '同步记录加载失败'
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

async function retry(id: number) {
  busyId.value = id
  try {
    const data = await taobaoRetry(id)
    ElMessage.success(data.status === 'SUCCESS' ? '重试已完成' : data.message || '重试未成功')
    await load()
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '重试失败'
  } finally {
    busyId.value = undefined
  }
}

onMounted(load)
</script>
