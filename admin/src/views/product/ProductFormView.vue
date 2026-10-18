<template>
  <div class="panel" v-loading="loading">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 720px">
      <el-form-item label="商品编码" prop="productCode">
        <el-input v-model="form.productCode" maxlength="64" />
      </el-form-item>
      <el-form-item label="商品名称" prop="title">
        <el-input v-model="form.title" maxlength="200" />
      </el-form-item>
      <el-form-item label="品牌" prop="brand">
        <el-input v-model="form.brand" maxlength="64" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="选择启用中的分类" style="width: 100%">
          <el-option v-for="item in enabledCategories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="图片 URL" prop="imageUrl">
        <el-input v-model="form.imageUrl" maxlength="512" placeholder="http 或 https" />
      </el-form-item>
      <el-form-item label="规格" prop="specification">
        <el-input v-model="form.specification" maxlength="500" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" maxlength="5000" />
      </el-form-item>
      <div class="form-actions">
        <el-button type="primary" :loading="submitting" @click="save">保存</el-button>
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
import { categories, createProduct, product, updateProduct } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Category } from '@/api/types'
import { fieldErrors } from '@/domain/deal'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const categoryOptions = ref<Category[]>([])
const editingId = computed(() => (route.params.id ? Number(route.params.id) : null))
const enabledCategories = computed(() => categoryOptions.value.filter((item) => item.status === 'ENABLED'))
const form = reactive({
  productCode: '',
  title: '',
  brand: '',
  categoryId: undefined as number | undefined,
  imageUrl: '',
  specification: '',
  description: '',
})
const rules: FormRules = {
  productCode: [{ required: true, message: '请填写商品编码', trigger: 'blur' }],
  title: [{ required: true, message: '请填写商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

function payload() {
  return {
    productCode: form.productCode.trim(),
    title: form.title.trim(),
    brand: form.brand.trim() || null,
    categoryId: form.categoryId,
    imageUrl: form.imageUrl.trim() || null,
    specification: form.specification.trim() || null,
    description: form.description.trim() || null,
  }
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || submitting.value) return
  submitting.value = true
  try {
    const saved = editingId.value
      ? await updateProduct(editingId.value, payload())
      : await createProduct(payload())
    ElMessage.success('已保存')
    router.push(`/products/${saved.id}`)
  } catch (error) {
    const message = error instanceof ApiError ? error.message : '保存失败'
    const fields = fieldErrors(message)
    if (Object.keys(fields).length > 0) {
      formRef.value?.validate(() => undefined)
    }
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  categoryOptions.value = await categories()
  if (!editingId.value) return
  loading.value = true
  try {
    const data = await product(editingId.value)
    form.productCode = data.productCode
    form.title = data.title
    form.brand = data.brand ?? ''
    form.categoryId = data.categoryId
    form.imageUrl = data.imageUrl ?? ''
    form.specification = data.specification ?? ''
    form.description = data.description ?? ''
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
})
</script>
