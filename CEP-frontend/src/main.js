import { createApp } from "vue";
import App from "./App.vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "./styles/soft-neo-theme.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import router from "./router";
import { AUTH_EXPIRED_EVENT } from "./api/common/authSessionService";

const app = createApp(App);

Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component);
});

app.use(ElementPlus);
app.use(router);

window.addEventListener(AUTH_EXPIRED_EVENT, () => {
  const currentName = String(router.currentRoute.value?.name || "");
  if (currentName !== "home") {
    router.replace({
      name: "home",
      query: { loginRequired: "1", reason: "expired" },
    });
    return;
  }
  router.replace({
    name: "home",
    query: {
      ...router.currentRoute.value.query,
      loginRequired: "1",
      reason: "expired",
    },
  });
});

app.mount("#app");
