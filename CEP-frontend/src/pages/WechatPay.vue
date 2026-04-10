<template>
  <div class="wechat-page">
    <main class="wechat-main">
      <header class="wechat-head">
        <div class="wechat-logo">微信支付</div>
      </header>

      <section v-if="isLoading" class="wechat-card">订单信息加载中...</section>
      <section v-else-if="loadError" class="wechat-card wechat-card--error">
        <p>{{ loadError }}</p>
        <button type="button" class="outline-btn" @click="loadOrder">
          重试
        </button>
      </section>

      <section v-else class="wechat-card">
        <p class="pay-item-name">{{ order.itemTitle }}</p>
        <p class="pay-order-no">订单号：{{ order.orderNo }}</p>
        <p class="pay-amount">￥{{ displayPrice }}</p>

        <button
          type="button"
          class="confirm-btn"
          :disabled="isPaying"
          @click="confirmPay"
        >
          {{ isPaying ? "支付处理中..." : "确认支付" }}
        </button>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import {
  buildMessageWebSocketUrl,
  createOrGetDirectConversation,
} from "../service/chat/chatApiService";
import {
  fetchTradeOrder,
  markTradeOrderPaid,
} from "../service/payment/paymentApiService";
import { fetchBoughtOrderContact } from "../service/profile/profileApiService";

const TRADE_REMINDER_PREFIX = "[TRADE_REMINDER]";

const route = useRoute();
const router = useRouter();

const order = ref({
  id: null,
  orderNo: "",
  itemTitle: "",
  amount: 0,
  status: "",
});

const isLoading = ref(false);
const loadError = ref("");
const isPaying = ref(false);

const orderId = computed(() => {
  const value = Number(route.query.orderId);
  if (!Number.isInteger(value) || value <= 0) {
    return null;
  }
  return value;
});

const toNumber = (value, fallback = 0) => {
  const converted = Number(value);
  return Number.isFinite(converted) ? converted : fallback;
};

const formatCurrency = (value) => {
  const fixed = toNumber(value, 0).toFixed(2);
  return fixed.endsWith(".00") ? fixed.slice(0, -3) : fixed;
};

const displayPrice = computed(() => formatCurrency(order.value.amount));

const sleep = (milliseconds) =>
  new Promise((resolve) => {
    setTimeout(resolve, milliseconds);
  });

const buildTradeReminderText = ({
  type,
  orderId,
  itemTitle,
  content,
  actionText,
  targetMenu,
}) =>
  `${TRADE_REMINDER_PREFIX}${JSON.stringify({
    type: String(type || "TRADE_REMINDER").trim() || "TRADE_REMINDER",
    orderId: Number(orderId) > 0 ? Number(orderId) : null,
    itemTitle: String(itemTitle || "").trim(),
    content: String(content || "").trim(),
    actionText: String(actionText || "去处理").trim() || "去处理",
    targetMenu: String(targetMenu || "").trim(),
  })}`;

const sendConversationTextBySocket = async (conversationId, text) => {
  const wsUrl = await buildMessageWebSocketUrl();
  await new Promise((resolve, reject) => {
    const socket = new WebSocket(wsUrl);
    let settled = false;

    const settle = (error) => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timeout);
      if (socket.readyState === WebSocket.OPEN) {
        socket.close();
      }
      if (error) {
        reject(error);
        return;
      }
      resolve();
    };

    const timeout = window.setTimeout(() => {
      settle(new Error("消息提醒发送超时"));
    }, 6000);

    socket.onopen = () => {
      try {
        socket.send(
          JSON.stringify({
            action: "SEND_MESSAGE",
            conversationId: Number(conversationId),
            text,
            imageUrl: "",
          })
        );
        window.setTimeout(() => settle(), 120);
      } catch (error) {
        settle(error instanceof Error ? error : new Error("消息提醒发送失败"));
      }
    };

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event?.data || "{}");
        if (String(payload?.eventType || "").toUpperCase() === "ERROR") {
          settle(new Error(payload?.message || "消息提醒发送失败"));
        }
      } catch {
        // ignore invalid payload
      }
    };

    socket.onerror = () => {
      settle(new Error("消息提醒发送失败"));
    };

    socket.onclose = () => {
      if (!settled) {
        settle();
      }
    };
  });
};

const notifySellerOrderPaid = async (paidOrder) => {
  const orderId = Number(paidOrder?.id || 0);
  if (!orderId) return;

  const contactBody = await fetchBoughtOrderContact(orderId);
  const contact = contactBody?.data || {};
  const peerUserId = Number(contact.peerUserId || 0);
  const itemId = Number(contact.itemId || paidOrder?.itemId || 0);
  if (!peerUserId || !itemId) {
    throw new Error("交易联系人信息无效，无法发送支付提醒");
  }

  const conversationBody = await createOrGetDirectConversation({
    peerUserId,
    itemId,
  });
  const rawConversation = conversationBody?.data || {};
  const conversationId = Number(
    rawConversation.conversationId || rawConversation.id || 0
  );
  if (!conversationId) {
    throw new Error("会话信息无效，无法发送支付提醒");
  }

  const reminderText = buildTradeReminderText({
    type: "BUYER_PAID_PENDING_CONFIRMATION",
    orderId,
    itemTitle: paidOrder?.itemTitle || contact.itemTitle || "",
    content: "买家已完成支付，订单已进入待确认，请确认是否已交付物品。",
    actionText: "确认已交付物品",
    targetMenu: "trade-sold",
  });
  await sendConversationTextBySocket(conversationId, reminderText);
};

const loadOrder = async () => {
  if (!orderId.value) {
    loadError.value = "订单信息无效";
    return;
  }

  isLoading.value = true;
  loadError.value = "";
  try {
    const responseBody = await fetchTradeOrder(orderId.value);
    order.value = responseBody.data || order.value;
  } catch (error) {
    loadError.value = error.message || "获取订单失败";
  } finally {
    isLoading.value = false;
  }
};

const confirmPay = async () => {
  if (!order.value.id) {
    ElMessage.warning("订单信息无效");
    return;
  }

  isPaying.value = true;
  try {
    await sleep(1200);
    const responseBody = await markTradeOrderPaid(order.value.id);
    const paidOrder = responseBody?.data;

    if (!paidOrder?.id) {
      throw new Error("支付失败，请重试");
    }

    await notifySellerOrderPaid(paidOrder).catch((error) => {
      ElMessage.warning(error.message || "支付成功，但卖家提醒发送失败");
    });

    const resolved = router.resolve({
      name: "payment-result",
      query: {
        orderId: String(paidOrder.id),
      },
    });
    window.open(resolved.href, "_blank");
  } catch (error) {
    ElMessage.error(error.message || "支付失败，请重试");
  } finally {
    isPaying.value = false;
  }
};

watch(
  () => route.query.orderId,
  () => {
    loadOrder();
  },
  { immediate: true }
);
</script>

<style scoped>
.wechat-page {
  min-height: 100vh;
  background: #ededed;
  padding: 30px 20px;
}

.wechat-main {
  max-width: 580px;
  margin: 0 auto;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.2);
  background: #ffffff;
}

.wechat-head {
  background: #07c160;
  color: #ffffff;
  padding: 20px;
}

.wechat-logo {
  font-size: 26px;
  font-weight: 700;
}

.wechat-card {
  padding: 30px 24px;
  text-align: center;
}

.wechat-card--error {
  border-top: 1px solid #fecaca;
}

.pay-item-name {
  margin: 0;
  font-size: 26px;
  color: #111827;
}

.pay-order-no {
  margin: 10px 0 0;
  color: #6b7280;
  font-size: 16px;
}

.pay-amount {
  margin: 20px 0 30px;
  font-size: 52px;
  font-weight: 700;
  color: #111827;
}

.confirm-btn,
.outline-btn {
  border-radius: 999px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
}

.confirm-btn {
  width: 100%;
  border: none;
  padding: 14px 22px;
  background: #07c160;
  color: #ffffff;
}

.confirm-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.outline-btn {
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #374151;
  padding: 10px 20px;
}
</style>

