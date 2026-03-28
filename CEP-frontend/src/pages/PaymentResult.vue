<template>
  <div class="result-page">
    <main class="result-main">
      <section v-if="isLoading" class="result-card">订单状态加载中...</section>
      <section v-else-if="loadError" class="result-card result-card--error">
        <p>{{ loadError }}</p>
        <button type="button" class="secondary-btn" @click="loadOrder">
          重试
        </button>
      </section>

      <section v-else class="result-card">
        <div class="success-icon">✓</div>
        <h1 class="title">支付成功</h1>
        <p class="amount">￥{{ displayPrice }}</p>
        <p class="order-no">订单号：{{ order.orderNo }}</p>

        <button type="button" class="primary-btn" @click="finishFlow">
          完成
        </button>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchTradeOrder } from "../service/payment/paymentApiService";

const route = useRoute();
const router = useRouter();

const order = ref({
  id: null,
  orderNo: "",
  amount: 0,
  itemId: null,
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
    loadError.value = error.message || "获取订单状态失败";
  } finally {
    isLoading.value = false;
  }
};

const finishFlow = () => {
  if (order.value.itemId) {
    router.replace({
      name: "item-detail",
      params: { id: String(order.value.itemId) },
    });
    return;
  }
  router.replace({ name: "home" });
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
.result-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #ecfdf5 0%, #f0fdf4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.result-main {
  width: 100%;
  max-width: 420px;
}

.result-card {
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.12);
  text-align: center;
  padding: 28px 22px;
}

.result-card--error {
  border: 1px solid #fecaca;
}

.success-icon {
  width: 74px;
  height: 74px;
  margin: 0 auto;
  border-radius: 50%;
  background: #22c55e;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
}

.title {
  margin: 16px 0 0;
  font-size: 28px;
  color: #111827;
}

.amount {
  margin: 12px 0 0;
  font-size: 38px;
  font-weight: 700;
  color: #111827;
}

.order-no {
  margin: 10px 0 24px;
  color: #6b7280;
  font-size: 13px;
}

.primary-btn,
.secondary-btn {
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn {
  width: 100%;
  border: none;
  padding: 12px 20px;
  color: #ffffff;
  background: linear-gradient(135deg, #16a34a, #22c55e);
}

.secondary-btn {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 8px 18px;
}
</style>

