<template>
  <div class="method-page">
    <main class="method-main">
      <h1 class="page-title">选择支付方式</h1>

      <section v-if="isLoading" class="state-card">订单信息加载中...</section>
      <section v-else-if="loadError" class="state-card state-card--error">
        <p>{{ loadError }}</p>
        <button type="button" class="secondary-btn" @click="loadOrder">
          重试
        </button>
      </section>

      <template v-else>
        <section class="card order-card">
          <h2>订单信息</h2>
          <div class="order-row">
            <span>商品</span>
            <strong>{{ order.itemTitle }}</strong>
          </div>
          <div class="order-row">
            <span>订单号</span>
            <strong>{{ order.orderNo }}</strong>
          </div>
          <div class="order-row">
            <span>待支付金额</span>
            <strong class="price">￥{{ displayPrice }}</strong>
          </div>
        </section>

        <section class="card channel-card">
          <h2>支付方式</h2>
          <label class="channel-option channel-option--active">
            <span class="radio"></span>
            <span>微信支付</span>
          </label>
        </section>

        <button type="button" class="pay-btn" @click="goPay">立即支付</button>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { fetchTradeOrder } from "../service/payment/paymentApiService";

const route = useRoute();
const router = useRouter();

const order = ref({
  id: null,
  orderNo: "",
  itemTitle: "",
  amount: 0,
});

const isLoading = ref(false);
const loadError = ref("");

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

const goPay = () => {
  if (!order.value.id) {
    ElMessage.warning("订单信息无效");
    return;
  }

  router.push({
    name: "wechat-pay",
    query: { orderId: String(order.value.id) },
  });
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
.method-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 30px 20px;
}

.method-main {
  max-width: 940px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-title {
  margin: 0;
  font-size: 32px;
}

.card,
.state-card {
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  padding: 20px;
}

.state-card--error {
  border: 1px solid #fecaca;
}

h2 {
  margin: 0 0 14px;
  font-size: 24px;
}

.order-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #4b5563;
  font-size: 17px;
}

.order-row strong {
  color: #111827;
  text-align: right;
}

.order-row .price {
  color: #f97316;
  font-size: 30px;
}

.channel-option {
  border-radius: 14px;
  border: 1px solid #d1fae5;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #ecfdf5;
  color: #065f46;
  font-size: 18px;
}

.radio {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.2);
}

.pay-btn,
.secondary-btn {
  border-radius: 999px;
  border: none;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
}

.pay-btn {
  width: 100%;
  padding: 14px 22px;
  color: #ffffff;
  background: linear-gradient(135deg, #16a34a, #22c55e);
}

.secondary-btn {
  padding: 10px 20px;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
}
</style>

