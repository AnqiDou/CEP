import { createRouter, createWebHistory } from "vue-router";
import Home from "../pages/Home.vue";
import Profile from "../pages/Profile.vue";
import Publish from "../pages/Publish.vue";
import ItemDetail from "../pages/ItemDetail.vue";
import Chat from "../pages/Chat.vue";
import OtherProfile from "../pages/OtherProfile.vue";
import Terms from "../pages/Terms.vue";
import Privacy from "../pages/Privacy.vue";
import {
  authState,
  initAuthSession,
} from "../service/common/authSessionService";

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
      path: "/publish",
      name: "publish",
      component: Publish,
    },
    {
      path: "/item/:id",
      name: "item-detail",
      component: ItemDetail,
    },
    {
      path: "/chat",
      name: "chat",
      component: Chat,
    },
    {
      path: "/user/:name",
      name: "other-profile",
      component: OtherProfile,
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
