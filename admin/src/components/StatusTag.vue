<template>
  <el-tag :type="dealStatusType[status] ?? 'info'" effect="plain">{{ labelOf(dealStatusLabel, status) }}</el-tag>
</template>

<script setup lang="ts">
import { dealStatusLabel, dealStatusType, labelOf } from '@/domain/labels'

defineProps<{ status: string }>()
</script>
