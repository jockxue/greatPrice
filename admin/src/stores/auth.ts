import { defineStore } from 'pinia'
import { ensureCsrf, login as loginApi, logout as logoutApi, me } from '@/api/admin'
import type { Profile } from '@/api/types'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as Profile | null,
    loaded: false,
  }),
  actions: {
    async fetchMe() {
      try {
        this.user = await me()
      } catch {
        this.user = null
      } finally {
        this.loaded = true
      }
    },
    async login(username: string, password: string) {
      await ensureCsrf()
      this.user = await loginApi(username, password)
      this.loaded = true
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.user = null
      }
    },
    clear() {
      this.user = null
      this.loaded = true
    },
  },
})
