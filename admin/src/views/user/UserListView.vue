<template>
  <div class="panel">
    <div class="filters">
      <el-button type="primary" @click="visible = true">新建账号</el-button>
    </div>
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <el-table v-else :data="records" v-loading="loading">
      <el-table-column prop="username" label="用户名" width="160" />
      <el-table-column prop="displayName" label="显示名称" width="160" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="createdAt" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="disable(row)">停用</el-button>
          <el-button v-else link type="primary" @click="enable(row)">启用</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无账号" /></template>
    </el-table>

    <el-dialog v-model="visible" title="新建账号" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="显示名称" prop="displayName"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="运营" value="OPERATOR" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { createUser, disableUser, enableUser, users } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { AdminUser } from '@/api/types'

const records = ref<AdminUser[]>([])
const loading = ref(false)
const error = ref('')
const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '', displayName: '', role: 'OPERATOR' })
const rules: FormRules = {
  username: [{ required: true, min: 3, message: '至少 3 位字母、数字、下划线或中划线', trigger: 'blur' }],
  password: [{ required: true, min: 10, message: '密码至少 10 位', trigger: 'blur' }],
  displayName: [{ required: true, message: '请填写显示名称', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    records.value = await users()
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '账号加载失败'
  } finally {
    loading.value = false
  }
}

async function create() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || submitting.value) return
  submitting.value = true
  try {
    await createUser({ ...form, username: form.username.trim(), displayName: form.displayName.trim() })
    ElMessage.success('已创建')
    visible.value = false
    form.password = ''
    await load()
  } catch (err) {
    ElMessage.error(err instanceof ApiError ? err.message : '创建失败')
  } finally {
    submitting.value = false
  }
}

async function disable(row: AdminUser) {
  try {
    await ElMessageBox.confirm(`停用后 ${row.username} 将无法继续访问后台。`, '停用账号', { type: 'warning' })
  } catch {
    return
  }
  try {
    await disableUser(row.id)
    ElMessage.success('已停用')
    await load()
  } catch (err) {
    if (err instanceof ApiError) ElMessage.error(err.message)
  }
}

async function enable(row: AdminUser) {
  try {
    await enableUser(row.id)
    ElMessage.success('已启用')
    await load()
  } catch (err) {
    if (err instanceof ApiError) ElMessage.error(err.message)
  }
}

onMounted(load)
</script>
