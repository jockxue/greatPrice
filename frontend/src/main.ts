import { createSSRApp } from "vue";
import * as Pinia from "pinia";
import App from "./App.vue";
import { pinia } from "./stores";

export function createApp() {
  const app = createSSRApp(App);
  app.use(pinia);
  return {
    app,
    // uni-app 要求返回 Pinia，用于 H5 SSR 等场景
    Pinia,
  };
}
