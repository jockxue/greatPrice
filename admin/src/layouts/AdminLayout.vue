<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand">好价导购后台</div>
      <router-link class="menu-item" to="/dashboard">仪表盘</router-link>
      <router-link class="menu-item" to="/products">商品管理</router-link>
      <router-link class="menu-item" to="/deals">好价管理</router-link>
      <router-link class="menu-item" to="/reviews">审核管理</router-link>
      <router-link class="menu-item" to="/price-history">价格核验</router-link>
      <router-link class="menu-item" to="/taobao">淘宝联盟</router-link>
      <router-link class="menu-item" to="/taobao/pick">淘宝选品</router-link>
      <router-link class="menu-item" to="/taobao/jobs">同步记录</router-link>
      <router-link v-if="auth.user?.role === 'ADMIN'" class="menu-item" to="/audit-logs">操作审计</router-link>
      <router-link v-if="auth.user?.role === 'ADMIN'" class="menu-item" to="/users">账号管理</router-link>
    </aside>
    <section class="main">
      <header class="topbar">
        <div class="crumb">管理后台 / {{ title }}</div>
        <div class="user-box">
          <span>{{ auth.user?.displayName }} · {{ auth.user?.role === 'ADMIN' ? '管理员' : '运营' }}</span>
          <el-button link type="primary" @click="onLogout">退出登录</el-button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const title = computed(() => String(route.meta.title ?? ''))

async function onLogout() {
  await auth.logout()
  router.push('/login')
}
</script>
