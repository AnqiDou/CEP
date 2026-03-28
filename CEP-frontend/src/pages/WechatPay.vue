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
  fetchTradeOrder,
  markTradeOrderPaid,
} from "../service/payment/paymentApiService";

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

    router.replace({
      name: "payment-result",
      query: { orderId: String(paidOrder.id) },
    });
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
  padding: 24px 14px;
}

.wechat-main {
  max-width: 460px;
  margin: 0 auto;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.2);
  background: #ffffff;
}

.wechat-head {
  background: #07c160;
  color: #ffffff;
  padding: 16px;
}

.wechat-logo {
  font-size: 20px;
  font-weight: 700;
}

.wechat-card {
  padding: 24px 20px;
  text-align: center;
}

.wechat-card--error {
  border-top: 1px solid #fecaca;
}

.pay-item-name {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.pay-order-no {
  margin: 10px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.pay-amount {
  margin: 16px 0 26px;
  font-size: 40px;
  font-weight: 700;
  color: #111827;
}

.confirm-btn,
.outline-btn {
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.confirm-btn {
  width: 100%;
  border: none;
  padding: 12px 20px;
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
  padding: 8px 18px;
}
</style>

