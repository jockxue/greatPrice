<template>
  <div class="panel" v-loading="loading">
    <el-alert v-if="current?.demo" title="演示数据会在用户端标明，不能写入真实价格历史。" type="warning" show-icon :closable="false" />
    <el-alert v-if="current?.rejectReason" :title="`驳回原因：${current.rejectReason}`" type="error" show-icon :closable="false" style="margin: 12px 0" />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width: 760px; margin-top: 12px">
      <el-form-item label="商品" prop="productId">
        <el-select v-model="form.productId" filterable remote :remote-method="searchProducts" :loading="searching" style="width: 100%" placeholder="搜索商品名称或编码">
          <el-option v-for="item in productOptions" :key="item.id" :label="`${item.title}（${item.productCode}）`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="当前价格" prop="currentPrice">
        <el-input v-model="form.currentPrice" placeholder="例如 19.90" />
      </el-form-item>
      <el-form-item label="参考价格" prop="originalPrice">
        <el-input v-model="form.originalPrice" placeholder="可空，不据此计算优惠幅度" />
      </el-form-item>
      <el-form-item label="优惠说明" prop="dealDescription">
        <el-input v-model="form.dealDescription" type="textarea" :rows="3" maxlength="500" />
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="form.sourceType" style="width: 100%">
          <el-option v-for="(label, value) in sourceLabel" :key="value" :label="label" :value="value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源名称" prop="sourceName">
        <el-input v-model="form.sourceName" maxlength="64" />
      </el-form-item>
      <el-form-item label="购买链接" prop="purchaseUrl">
        <el-input v-model="form.purchaseUrl" maxlength="1024" />
      </el-form-item>
      <el-form-item label="推广链接" prop="promotionUrl">
        <el-input v-model="form.promotionUrl" maxlength="1024" />
      </el-form-item>
      <el-form-item label="开始时间" prop="startsAt">
        <el-date-picker v-model="form.startsAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="可空" />
      </el-form-item>
      <el-form-item label="结束时间" prop="expiresAt">
        <el-date-picker v-model="form.expiresAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="可空" />
      </el-form-item>
      <div class="form-actions">
        <el-button v-if="current?.status !== 'PUBLISHED'" type="primary" :loading="submitting" @click="save(false)">保存草稿</el-button>
        <el-button v-if="current?.status !== 'PUBLISHED'" type="success" :loading="submitting" @click="save(true)">保存并提交审核</el-button>
        <el-button v-else type="warning" :loading="submitting" @click="save(false)">提交变更审核</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDeal, deal, products, reviseDeal, submitDeal, updateDeal } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Deal, DealWrite, Product } from '@/api/types'
import { fieldErrors, fromApiTime, toApiTime } from '@/domain/deal'
import { moneyError } from '@/domain/money'
import { sourceLabel } from '@/domain/labels'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const searching = ref(false)
const current = ref<Deal | null>(null)
const productOptions = ref<Product[]>([])
const editingId = computed(() => (route.params.id ? Number(route.params.id) : null))
const form = reactive({
  productId: undefined as number | undefined,
  currentPrice: '',
  originalPrice: '',
  dealDescription: '',
  sourceType: 'EDITOR',
  sourceName: '',
  purchaseUrl: '',
  promotionUrl: '',
  startsAt: '',
  expiresAt: '',
})

const rules: FormRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  currentPrice: [{
    validator: (_rule, value, callback) => {
      const message = moneyError(String(value ?? ''), '当前价格', true)
      callback(message ? new Error(message) : undefined)
    },
    trigger: 'blur',
  }],
  originalPrice: [{
    validator: (_rule, value, callback) => {
      const message = moneyError(String(value ?? ''), '参考价格', false)
      callback(message ? new Error(message) : undefined)
    },
    trigger: 'blur',
  }],
  dealDescription: [{ required: true, message: '请填写优惠说明', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  sourceName: [{ required: true, message: '请填写来源名称', trigger: 'blur' }],
}

function payload(version?: number): DealWrite {
  return {
    productId: form.productId as number,
    currentPrice: form.currentPrice.trim(),
    originalPrice: form.originalPrice.trim() || null,
    currency: 'CNY',
    dealDescription: form.dealDescription.trim(),
    sourceType: form.sourceType,
    sourceName: form.sourceName.trim(),
    purchaseUrl: form.purchaseUrl.trim() || null,
    promotionUrl: form.promotionUrl.trim() || null,
    startsAt: toApiTime(form.startsAt),
    expiresAt: toApiTime(form.expiresAt),
    version,
  }
}

async function searchProducts(keyword: string) {
  searching.value = true
  try {
    const data = await products({ page: 1, size: 20, keyword, status: 'ACTIVE' })
    productOptions.value = data.records
  } finally {
    searching.value = false
  }
}

async function save(submit: boolean) {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || submitting.value) return
  submitting.value = true
  try {
    let saved: Deal
    if (!editingId.value) {
      saved = await createDeal(payload())
    } else if (current.value?.status === 'PUBLISHED') {
      saved = await reviseDeal(editingId.value, payload(current.value.version))
      ElMessage.success('已提交变更审核，好价暂不对用户端展示')
      router.push(`/deals/${saved.id}`)
      return
    } else {
      saved = await updateDeal(editingId.value, payload(current.value?.version))
    }
    if (submit) {
      saved = await submitDeal(saved.id, saved.version)
      ElMessage.success('已提交审核')
    } else {
      ElMessage.success('已保存草稿')
    }
    router.push(`/deals/${saved.id}`)
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '保存失败')
    const message = error instanceof ApiError ? error.message : ''
    const fields = fieldErrors(message)
    Object.entries(fields).forEach(([prop, text]) => {
      formRef.value?.validateField(prop).catch(() => undefined)
      if (text) ElMessage.warning(`${prop}: ${text}`)
    })
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!editingId.value) return
  loading.value = true
  try {
    const data = await deal(editingId.value)
    if (data.status === 'OFFLINE' || data.status === 'EXPIRED') {
      ElMessage.warning('已下架或已过期的好价不能直接编辑')
      router.replace(`/deals/${data.id}`)
      return
    }
    current.value = data
    form.productId = data.productId
    form.currentPrice = data.currentPrice
    form.originalPrice = data.originalPrice ?? ''
    form.dealDescription = data.dealDescription
    form.sourceType = data.sourceType
    form.sourceName = data.sourceName
    form.purchaseUrl = data.purchaseUrl ?? ''
    form.promotionUrl = data.promotionUrl ?? ''
    form.startsAt = fromApiTime(data.startsAt)
    form.expiresAt = fromApiTime(data.expiresAt)
    productOptions.value = [{
      id: data.productId,
      productCode: data.productCode,
      title: data.title,
      brand: data.brand,
      imageUrl: data.imageUrl,
      categoryId: data.categoryId,
      categoryName: data.categoryName,
      specification: data.specification,
      description: data.description,
      status: data.productStatus as 'ACTIVE' | 'INACTIVE',
      createdAt: null,
      updatedAt: null,
      demo: data.demo,
    }]
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
})
</script>
