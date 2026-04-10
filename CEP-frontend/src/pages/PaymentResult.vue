<template>
  <div class="result-page">
    <main class="result-main">
      <section v-if="isLoading" class="result-card">订单状态加载中...</section>
      <section v-else class="result-card">
        <div class="success-icon">✓</div>
        <h1 class="title">支付成功</h1>
        <p class="amount">￥{{ displayPrice }}</p>
        <p class="order-no">订单号：{{ order.orderNo }}</p>
        <p class="redirect-tip">{{ autoRedirectText }}</p>

        <button type="button" class="primary-btn" @click="finishFlow">
          返回首页
        </button>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchTradeOrder } from "../api/payment/paymentApiService";

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
const redirectSeconds = ref(3);
let redirectTimer = null;
let countdownTimer = null;

const orderId = computed(() => {
  const value = Number(route.query.orderId);
  if (!Number.isInteger(value) || value <= 0) {
    return null;
  }
  return value;
});

const returnTo = computed(() => {
  const raw = String(route.query?.returnTo || "").trim();
  if (!raw.startsWith("/")) {
    return null;
  }
  return raw;
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
const autoRedirectText = computed(
  () => `${redirectSeconds.value} 秒后自动返回首页`
);

const clearTimers = () => {
  if (redirectTimer) {
    window.clearTimeout(redirectTimer);
    redirectTimer = null;
  }
  if (countdownTimer) {
    window.clearInterval(countdownTimer);
    countdownTimer = null;
  }
};

const scheduleAutoRedirect = () => {
  clearTimers();
  redirectSeconds.value = 3;
  countdownTimer = window.setInterval(() => {
    if (redirectSeconds.value > 1) {
      redirectSeconds.value -= 1;
    }
  }, 1000);
  redirectTimer = window.setTimeout(() => {
    clearTimers();
    if (returnTo.value) {
      window.location.replace(returnTo.value);
      return;
    }
    router.replace({ name: "home" });
  }, 3000);
};

const loadOrder = async () => {
  if (!orderId.value) {
    router.replace({ name: "home" });
    return;
  }

  isLoading.value = true;
  loadError.value = "";
  try {
    const responseBody = await fetchTradeOrder(orderId.value);
    order.value = responseBody.data || order.value;
    scheduleAutoRedirect();
  } catch (error) {
    router.replace({ name: "home" });
    return;
  } finally {
    isLoading.value = false;
  }
};

const finishFlow = () => {
  clearTimers();
  if (returnTo.value) {
    window.location.replace(returnTo.value);
    return;
  }
  router.replace({ name: "home" });
};

onBeforeUnmount(() => {
  clearTimers();
});

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
  padding: 28px;
}

.result-main {
  width: 100%;
  max-width: 560px;
}

.result-card {
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.12);
  text-align: center;
  padding: 36px 28px;
}

.result-card--error {
  border: 1px solid #fecaca;
}

.success-icon {
  width: 90px;
  height: 90px;
  margin: 0 auto;
  border-radius: 50%;
  background: #22c55e;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  font-weight: 700;
}

.title {
  margin: 16px 0 0;
  font-size: 36px;
  color: #111827;
}

.amount {
  margin: 12px 0 0;
  font-size: 50px;
  font-weight: 700;
  color: #111827;
}

.order-no {
  margin: 10px 0 24px;
  color: #6b7280;
  font-size: 16px;
}

.primary-btn,
.secondary-btn {
  border-radius: 999px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn {
  width: 100%;
  border: none;
  padding: 14px 24px;
  color: #ffffff;
  background: linear-gradient(135deg, #16a34a, #22c55e);
}

.secondary-btn {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 10px 20px;
}
</style>

