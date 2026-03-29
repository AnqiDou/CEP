import { createRouter, createWebHistory } from "vue-router";
import Home from "../pages/Home.vue";
import Profile from "../pages/Profile.vue";
import Publish from "../pages/Publish.vue";
import ItemDetail from "../pages/ItemDetail.vue";
import Chat from "../pages/Chat.vue";
import OtherProfile from "../pages/OtherProfile.vue";
import Terms from "../pages/Terms.vue";
import Privacy from "../pages/Privacy.vue";
import ConfirmOrder from "../pages/ConfirmOrder.vue";
import PaymentMethod from "../pages/PaymentMethod.vue";
import WechatPay from "../pages/WechatPay.vue";
import PaymentResult from "../pages/PaymentResult.vue";
import TradeReview from "../pages/TradeReview.vue";
import AdminDashboard from "../pages/AdminDashboard.vue";
import {
  authState,
  initAuthSession,
} from "../service/common/authSessionService";

const ADMIN_EMAIL = "3299166215@qq.com";

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
      meta: {
        requiresAuth: true,
      },
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
    {
      path: "/trade/confirm",
      name: "confirm-order",
      component: ConfirmOrder,
    },
    {
      path: "/trade/pay-method",
      name: "payment-method",
      component: PaymentMethod,
    },
    {
      path: "/trade/wechat-pay",
      name: "wechat-pay",
      component: WechatPay,
    },
    {
      path: "/trade/result",
      name: "payment-result",
      component: PaymentResult,
    },
    {
      path: "/trade/review",
      name: "trade-review",
      component: TradeReview,
    },
    {
      path: "/admin",
      name: "admin-dashboard",
      component: AdminDashboard,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
  ],
});

router.beforeEach(async (to) => {
  await initAuthSession();

  const currentEmail = (authState.user?.email || "").trim().toLowerCase();
  const isAdmin = currentEmail === ADMIN_EMAIL;

  if (isAdmin && to.name !== "admin-dashboard") {
    return { name: "admin-dashboard" };
  }

  if (to.meta?.requiresAuth && !authState.user) {
    return { name: "home" };
  }

  if (to.meta?.requiresAdmin) {
    if (currentEmail !== ADMIN_EMAIL) {
      return { name: "home" };
    }
  }

  if (to.name === "profile" && !authState.user) {
    return { name: "home" };
  }

  return true;
});

export default router;
