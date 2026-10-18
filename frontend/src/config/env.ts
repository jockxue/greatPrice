/** 所有环境变量统一从这里读取，业务代码不要直接访问 import.meta.env。 */
export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? '',
  /** dev:* 命令为 true，build:* 命令为 false */
  isDev: import.meta.env.DEV,
}
