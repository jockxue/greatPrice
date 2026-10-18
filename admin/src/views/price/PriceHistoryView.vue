<template>
  <div class="panel">
    <div class="filters">
      <el-input v-model="productId" placeholder="商品 ID" style="width: 140px" />
      <el-input v-model="dealId" placeholder="好价 ID" style="width: 140px" />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="openVerify">记录核验</el-button>
    </div>
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <el-table v-else :data="records" v-loading="loading">
      <el-table-column prop="recordedAt" label="核验时间" width="200" />
      <el-table-column prop="productTitle" label="商品" min-width="160" />
      <el-table-column prop="productCode" label="编码" width="140" />
      <el-table-column prop="dealId" label="好价" width="90" />
      <el-table-column prop="price" label="价格" width="110" />
      <el-table-column label="来源" width="140">
        <template #default="{ row }">{{ labelOf(sourceLabel, row.sourceType) }} / {{ row.sourceName }}</template>
      </el-table-column>
      <el-table-column prop="verifierName" label="核验人" width="120" />
      <el-table-column prop="note" label="说明" min-width="160" />
      <template #empty><el-empty description="请按商品或好价查询核验记录" /></template>
    </el-table>
    <div class="pager">
      <el-pagination layout="total, prev, pager, next" :total="total" :page-size="20" :current-page="page" @current-change="changePage" />
    </div>

    <el-dialog v-model="visible" title="记录本次核验" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商品 ID" prop="productId"><el-input v-model="form.productId" /></el-form-item>
        <el-form-item label="好价 ID" prop="dealId"><el-input v-model="form.dealId" placeholder="可空" /></el-form-item>
        <el-form-item label="价格" prop="price"><el-input v-model="form.price" placeholder="例如 19.90" /></el-form-item>
        <el-form-item label="来源" prop="sourceType">
          <el-select v-model="form.sourceType" style="width: 100%">
            <el-option label="编辑发布" value="EDITOR" />
            <el-option label="用户爆料" value="USER_SUBMISSION" />
            <el-option label="推广联盟" value="AFFILIATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源名称" prop="sourceName"><el-input v-model="form.sourceName" /></el-form-item>
        <el-form-item label="说明" prop="note"><el-input v-model="form.note" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitVerify">保存核验</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { priceHistory, verifyPrice } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { PriceHistory } from '@/api/types'
import { moneyError } from '@/domain/money'
import { labelOf, sourceLabel } from '@/domain/labels'

const route = useRoute()
const productId = ref(route.query.productId ? String(route.query.productId) : '')
const dealId = ref(route.query.dealId ? String(route.query.dealId) : '')
const page = ref(1)
const total = ref(0)
const records = ref<PriceHistory[]>([])
const loading = ref(false)
const error = ref('')
const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ productId: '', dealId: '', price: '', sourceType: 'EDITOR', sourceName: '', note: '' })
const rules: FormRules = {
  productId: [{ required: true, message: '请填写商品 ID', trigger: 'blur' }],
  price: [{
    validator: (_rule, value, callback) => {
      const message = moneyError(String(value ?? ''), '核验价格', true)
      callback(message ? new Error(message) : undefined)
    },
    trigger: 'blur',
  }],
  sourceType: [{ required: true, message: '请选择来源', trigger: 'change' }],
  sourceName: [{ required: true, message: '请填写来源名称', trigger: 'blur' }],
}

async function load() {
  if (!productId.value.trim() && !dealId.value.trim()) {
    records.value = []
    total.value = 0
    error.value = ''
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await priceHistory({
      page: page.value,
      size: 20,
      productId: productId.value.trim() || undefined,
      dealId: dealId.value.trim() || undefined,
    })
    records.value = data.records
    total.value = data.total
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '核验记录加载失败'
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

function openVerify() {
  form.productId = productId.value
  form.dealId = dealId.value
  visible.value = true
}

async function submitVerify() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || submitting.value) return
  submitting.value = true
  try {
    await verifyPrice({
      productId: Number(form.productId),
      dealId: form.dealId.trim() ? Number(form.dealId) : null,
      price: form.price.trim(),
      sourceType: form.sourceType,
      sourceName: form.sourceName.trim(),
      note: form.note.trim() || null,
    })
    ElMessage.success('已记录核验')
    visible.value = false
    productId.value = form.productId
    dealId.value = form.dealId
    await load()
  } catch (err) {
    ElMessage.error(err instanceof ApiError ? err.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
