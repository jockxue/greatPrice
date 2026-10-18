<template>
  <div v-loading="loading">
    <div v-if="error" class="state">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button @click="load">重试</el-button>
    </div>
    <template v-else-if="data">
      <div class="panel">
        <el-alert v-if="data.demo" title="演示数据，用户端会明确标注，不能当作真实核验价格。" type="warning" show-icon :closable="false" />
        <el-alert v-if="data.rejectReason" :title="`驳回原因：${data.rejectReason}`" type="error" show-icon :closable="false" style="margin-top: 12px" />
        <el-alert v-if="data.offlineReason" :title="`下架原因：${data.offlineReason}`" type="info" show-icon :closable="false" style="margin-top: 12px" />
        <el-descriptions :column="2" border style="margin-top: 12px">
          <el-descriptions-item label="状态"><StatusTag :status="data.status" /></el-descriptions-item>
          <el-descriptions-item label="演示">{{ data.demo ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="商品">
            <el-button link type="primary" @click="router.push(`/products/${data.productId}`)">{{ data.title }}（{{ data.productCode }}）</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="商品状态">{{ data.productStatus }}</el-descriptions-item>
          <el-descriptions-item label="当前价格">{{ data.currentPrice }} {{ data.currency }}</el-descriptions-item>
          <el-descriptions-item label="参考价格">{{ data.originalPrice || '-' }}</el-descriptions-item>
          <el-descriptions-item label="优惠说明" :span="2">{{ data.dealDescription }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ labelOf(sourceLabel, data.sourceType) }} / {{ data.sourceName }}</el-descriptions-item>
          <el-descriptions-item label="核验时间">{{ data.verifiedAt || '未核验' }}</el-descriptions-item>
          <el-descriptions-item label="购买链接" :span="2">{{ data.purchaseUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="推广链接" :span="2">{{ data.promotionUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始">{{ data.startsAt || '未限定' }}</el-descriptions-item>
          <el-descriptions-item label="结束">{{ data.expiresAt || '未限定' }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ data.submitterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ data.reviewerName || '-' }} {{ data.reviewedAt || '' }}</el-descriptions-item>
        </el-descriptions>
        <div class="form-actions">
          <el-button v-if="actions.canEdit" @click="router.push(`/deals/${data.id}/edit`)">编辑</el-button>
          <el-button v-if="actions.canSubmit" type="primary" :loading="acting" @click="runSubmit">提交审核</el-button>
          <el-button v-if="actions.canApprove" type="success" :loading="acting" @click="runApprove">审核通过</el-button>
          <el-button v-if="actions.canReject" type="danger" :loading="acting" @click="runReject">驳回</el-button>
          <el-button v-if="actions.canRevise" :loading="acting" @click="router.push(`/deals/${data.id}/edit`)">提交变更审核</el-button>
          <el-button v-if="actions.canOffline" type="warning" :loading="acting" @click="runOffline">下架</el-button>
          <el-button v-if="actions.canReopen" :loading="acting" @click="runReopen">重新打开为草稿</el-button>
          <el-button @click="router.push({ path: '/price-history', query: { dealId: data.id, productId: data.productId } })">价格历史</el-button>
        </div>
      </div>
      <div class="panel" style="margin-top: 16px">
        <h3>审核记录</h3>
        <el-table :data="history">
          <el-table-column prop="createdAt" label="时间" width="220" />
          <el-table-column prop="reviewerName" label="审核人" width="140" />
          <el-table-column label="结果" width="120">
            <template #default="{ row }">{{ row.action === 'APPROVE' ? '通过' : '驳回' }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" />
          <template #empty><el-empty description="还没有审核记录" /></template>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { approveDeal, deal, dealReviews, offlineDeal, rejectDeal, reopenDeal, submitDeal } from '@/api/admin'
import { ApiError } from '@/api/http'
import type { Deal, DealReview } from '@/api/types'
import StatusTag from '@/components/StatusTag.vue'
import { dealActions } from '@/domain/deal'
import { labelOf, sourceLabel } from '@/domain/labels'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const data = ref<Deal | null>(null)
const history = ref<DealReview[]>([])
const loading = ref(false)
const acting = ref(false)
const error = ref('')
const actions = computed(() => data.value
  ? dealActions({
      status: data.value.status,
      role: auth.user?.role ?? '',
      submittedBy: data.value.submittedBy,
      selfId: auth.user?.id ?? 0,
    })
  : dealActions({ status: '', role: '', submittedBy: null, selfId: 0 }))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const id = Number(route.params.id)
    data.value = await deal(id)
    history.value = await dealReviews(id)
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function act(work: () => Promise<void>) {
  if (acting.value) return
  acting.value = true
  try {
    await work()
    await load()
  } catch (err) {
    if (err instanceof ApiError) ElMessage.error(err.message)
  } finally {
    acting.value = false
  }
}

function runSubmit() {
  act(async () => {
    await submitDeal(data.value!.id, data.value!.version)
    ElMessage.success('已提交审核')
  })
}

function runApprove() {
  act(async () => {
    await ElMessageBox.confirm('通过后好价会对用户端公开（仍受有效期和商品状态约束）。', '审核通过', { type: 'warning' })
    await approveDeal(data.value!.id, data.value!.version)
    ElMessage.success('已发布')
  })
}

function runReject() {
  act(async () => {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回', {
      inputValidator: (text) => (text && text.trim() ? true : '驳回原因不能为空'),
    })
    await rejectDeal(data.value!.id, data.value!.version, value.trim())
    ElMessage.success('已驳回')
  })
}

function runOffline() {
  act(async () => {
    const { value } = await ElMessageBox.prompt('下架后用户端不可再访问。请填写原因。', '下架', {
      inputValidator: (text) => (text && text.trim() ? true : '下架原因不能为空'),
    })
    await offlineDeal(data.value!.id, data.value!.version, value.trim())
    ElMessage.success('已下架')
  })
}

function runReopen() {
  act(async () => {
    await ElMessageBox.confirm('只会退回草稿，不会直接重新发布。', '重新打开', { type: 'warning' })
    await reopenDeal(data.value!.id, data.value!.version)
    ElMessage.success('已退回草稿')
  })
}

onMounted(load)
</script>
