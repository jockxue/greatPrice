<template>
  <div class="panel">
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <template v-else>
      <el-descriptions v-loading="loading" :column="2" border>
        <el-descriptions-item label="接入启用">{{ status?.enabled ? '已启用' : '未启用' }}</el-descriptions-item>
        <el-descriptions-item label="允许手动同步">{{ status?.manualSyncEnabled ? '已开启' : '未开启' }}</el-descriptions-item>
        <el-descriptions-item label="App Key">{{ status?.appKeyConfigured ? status.appKeyMasked : '未配置' }}</el-descriptions-item>
        <el-descriptions-item label="App Secret">{{ status?.appSecretConfigured ? '已配置' : '未配置' }}</el-descriptions-item>
        <el-descriptions-item label="siteId">{{ yes(status?.siteIdConfigured) }}</el-descriptions-item>
        <el-descriptions-item label="adzoneId">{{ yes(status?.adzoneIdConfigured) }}</el-descriptions-item>
        <el-descriptions-item label="session">{{ status?.sessionConfigured ? '已配置' : '未配置' }}</el-descriptions-item>
        <el-descriptions-item label="网关主机">{{ status?.gatewayHost || '未配置' }}</el-descriptions-item>
        <el-descriptions-item label="商品搜索">{{ ready(status?.searchConfigured) }}</el-descriptions-item>
        <el-descriptions-item label="商品详情">{{ ready(status?.detailConfigured) }}</el-descriptions-item>
        <el-descriptions-item label="推广链接">{{ ready(status?.promotionConfigured) }}</el-descriptions-item>
        <el-descriptions-item label="最近测试">{{ status?.lastTestAt || '尚未测试' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="status?.lastTestMessage"
        style="margin-top: 16px"
        :type="status.lastTestStatus === 'SUCCESS' ? 'success' : 'warning'"
        :title="`${status.lastTestStatus || ''} ${status.lastTestErrorCode || ''} ${status.lastTestMessage}`"
        show-icon
        :closable="false"
      />
      <p class="hint">配置完整只表示后端拿到了参数，不表示开放平台已经授权。测试会真实调用接口。</p>
      <el-button type="primary" :loading="testing" :disabled="loading" @click="runTest">测试连接</el-button>
      <el-descriptions v-if="testResult" :column="1" border style="margin-top: 16px">
        <el-descriptions-item label="商品搜索">{{ probeText(testResult.search) }}</el-descriptions-item>
        <el-descriptions-item label="商品详情">{{ probeText(testResult.detail) }}</el-descriptions-item>
        <el-descriptions-item label="推广链接">{{ probeText(testResult.promotion) }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { taobaoStatus, taobaoTest } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { TaobaoProbe, TaobaoStatus, TaobaoTestResult } from '@/api/types'

const status = ref<TaobaoStatus>()
const testResult = ref<TaobaoTestResult>()
const loading = ref(false)
const testing = ref(false)
const error = ref('')

function yes(value: boolean | undefined) {
  return value ? '已配置' : '未配置'
}

function ready(value: boolean | undefined) {
  return value ? '配置完整，尚未用本次结果证明已授权' : '未配置'
}

function probeText(probe: TaobaoProbe) {
  return `${probe.state} ${probe.apiMethod} ${probe.errorCode || ''} ${probe.message || ''} ${probe.requestId || ''}`.trim()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    status.value = await taobaoStatus()
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '状态加载失败'
  } finally {
    loading.value = false
  }
}

async function runTest() {
  testing.value = true
  try {
    testResult.value = await taobaoTest()
    await load()
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '连接测试失败'
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.hint { color: #667085; font-size: 13px; }
</style>
