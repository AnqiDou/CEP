import { createRouter, createWebHistory } from "vue-router";
import Home from "../pages/Home.vue";
import Profile from "../pages/Profile.vue";
import Terms from "../pages/Terms.vue";
import Privacy from "../pages/Privacy.vue";
import { authState, initAuthSession } from "../auth/session";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: Home,
    },
    {
      path: "/profile",
      name: "profile",
      component: Profile,
    },
    {
      path: "/terms",
      name: "terms",
      component: Terms,
    },
    {
      path: "/privacy",
      name: "privacy",
      component: Privacy,
    },
  ],
});

router.beforeEach(async (to) => {
  await initAuthSession();

  if (to.name === "profile" && !authState.user) {
    return { name: "home" };
  }

  return true;
});

export default router;
