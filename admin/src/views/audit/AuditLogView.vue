<template>
  <div class="panel">
    <div class="filters">
      <el-input v-model="action" placeholder="动作，如 DEAL_APPROVE" clearable style="width: 220px" />
      <el-input v-model="resourceType" placeholder="资源，如 DEAL" clearable style="width: 160px" />
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <el-table v-else :data="records" v-loading="loading">
      <el-table-column prop="createdAt" label="时间" width="200" />
      <el-table-column prop="actorName" label="操作人" width="140" />
      <el-table-column prop="action" label="动作" width="160" />
      <el-table-column prop="resourceType" label="资源" width="140" />
      <el-table-column prop="resourceId" label="资源 ID" width="110" />
      <el-table-column prop="detail" label="说明" min-width="200" />
      <template #empty><el-empty description="暂无审计记录" /></template>
    </el-table>
    <div class="pager">
      <el-pagination layout="total, prev, pager, next" :total="total" :page-size="20" :current-page="page" @current-change="changePage" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { auditLogs } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { AuditLog } from '@/api/types'

const action = ref('')
const resourceType = ref('')
const page = ref(1)
const total = ref(0)
const records = ref<AuditLog[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await auditLogs({
      page: page.value,
      size: 20,
      action: action.value.trim() || undefined,
      resourceType: resourceType.value.trim() || undefined,
    })
    records.value = data.records
    total.value = data.total
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '审计记录加载失败'
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

onMounted(load)
</script>
