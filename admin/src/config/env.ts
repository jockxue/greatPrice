const raw = import.meta.env.VITE_API_BASE_URL ?? ''

export const env = {
  apiBaseUrl: raw.replace(/\/$/, ''),
  isDev: import.meta.env.DEV,
}
