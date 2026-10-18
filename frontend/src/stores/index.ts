import { createPinia } from 'pinia'

/**
 * 只有确实需要跨页面共享的状态（如登录用户）才放进 store，单页面状态用组件内 ref 即可。
 * 新 store 按业务放在本目录，例如 stores/user.ts。
 */
export const pinia = createPinia()
