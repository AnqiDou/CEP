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
const PUBLIC_ROUTE_NAMES = new Set(["home", "terms", "privacy"]);

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: Home,
      meta: {
        title: "校园易物平台",
      },
    },
    {
      path: "/search",
      name: "search",
      component: Home,
      meta: {
        title: "搜索结果",
      },
    },
    {
      path: "/profile",
      name: "profile",
      component: Profile,
      meta: {
        requiresAuth: true,
        title: "个人中心",
      },
    },
    {
      path: "/publish",
      name: "publish",
      component: Publish,
      meta: {
        title: "发布闲置",
      },
    },
    {
      path: "/item/:id",
      name: "item-detail",
      component: ItemDetail,
      meta: {
        title: "商品详情",
      },
    },
    {
      path: "/chat",
      name: "chat",
      component: Chat,
      meta: {
        title: "聊天消息",
      },
    },
    {
      path: "/user/:name",
      name: "other-profile",
      component: OtherProfile,
      meta: {
        title: "用户主页",
      },
    },
    {
      path: "/terms",
      name: "terms",
      component: Terms,
      meta: {
        title: "用户协议",
      },
    },
    {
      path: "/privacy",
      name: "privacy",
      component: Privacy,
      meta: {
        title: "隐私政策",
      },
    },
    {
      path: "/trade/confirm",
      name: "confirm-order",
      component: ConfirmOrder,
      meta: {
        title: "确认订单",
      },
    },
    {
      path: "/trade/pay-method",
      name: "payment-method",
      component: PaymentMethod,
      meta: {
        title: "选择支付方式",
      },
    },
    {
      path: "/trade/wechat-pay",
      name: "wechat-pay",
      component: WechatPay,
      meta: {
        title: "微信支付",
      },
    },
    {
      path: "/trade/result",
      name: "payment-result",
      component: PaymentResult,
      meta: {
        title: "支付结果",
      },
    },
    {
      path: "/trade/review",
      name: "trade-review",
      component: TradeReview,
      meta: {
        title: "交易评价",
      },
    },
    {
      path: "/admin",
      name: "admin-dashboard",
      component: AdminDashboard,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
        title: "管理后台",
      },
    },
  ],
});

router.beforeEach(async (to) => {
  await initAuthSession();

  const isLoggedIn = Boolean(authState.user && authState.refreshToken);
  const isPublicRoute = PUBLIC_ROUTE_NAMES.has(String(to.name || ""));

  if (!isLoggedIn && !isPublicRoute) {
    return {
      name: "home",
      query: {
        loginRequired: "1",
        from: to.fullPath,
      },
    };
  }

  if (!isLoggedIn && to.name === "home" && to.query?.opsView === "list") {
    return {
      name: "home",
      query: {
        loginRequired: "1",
      },
    };
  }

  const currentEmail = (authState.user?.email || "").trim().toLowerCase();
  const isAdmin = currentEmail === ADMIN_EMAIL;

  if (isAdmin && to.name !== "admin-dashboard") {
    return { name: "admin-dashboard" };
  }

  if (to.meta?.requiresAdmin) {
    if (currentEmail !== ADMIN_EMAIL) {
      return { name: "home" };
    }
  }

  return true;
});

router.afterEach((to) => {
  const pageTitle = to.meta?.title;
  document.title =
    typeof pageTitle === "string" && pageTitle.trim()
      ? pageTitle
      : "校园易物平台";
});

export default router;
