<template>
  <div class="confirm-page">
    <main class="confirm-main">
      <h1 class="page-title">确认订单</h1>

      <section v-if="isLoading" class="state-card">订单信息加载中...</section>
      <section v-else-if="loadError" class="state-card state-card--error">
        <p>{{ loadError }}</p>
        <button type="button" class="secondary-btn" @click="loadItem">
          重试
        </button>
      </section>

      <template v-else>
        <section class="card item-card">
          <h2>商品信息</h2>
          <div class="item-row">
            <img
              v-if="item.photo"
              :src="item.photo"
              :alt="item.title"
              class="item-cover"
            />
            <div v-else class="item-cover item-cover--empty">暂无图片</div>
            <div class="item-info">
              <p class="item-title">{{ item.title }}</p>
              <p class="item-price">￥{{ displayPrice }}</p>
            </div>
          </div>
        </section>

        <section class="card address-card">
          <h2>收货信息</h2>
          <label class="field">
            <span>收货人</span>
            <input
              v-model="receiver.name"
              type="text"
              placeholder="请输入收货人姓名"
            />
          </label>
          <label class="field">
            <span>联系电话</span>
            <input
              v-model="receiver.phone"
              type="tel"
              placeholder="请输入联系电话"
            />
          </label>
          <label class="field">
            <span>收货地址</span>
            <textarea
              v-model="receiver.address"
              rows="3"
              placeholder="请输入详细收货地址"
            />
          </label>
        </section>

        <section class="card submit-card">
          <div class="total-row">
            <span>订单金额</span>
            <strong>￥{{ displayPrice }}</strong>
          </div>
          <button
            type="button"
            class="primary-btn"
            :disabled="isSubmitting"
            @click="submitOrder"
          >
            {{ isSubmitting ? "提交中..." : "提交订单" }}
          </button>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { fetchItemDetail } from "../service/item-detail/itemDetailApiService";
import { createTradeOrder } from "../service/payment/paymentApiService";

const route = useRoute();
const router = useRouter();

const item = ref({
  id: null,
  title: "",
  price: 0,
  photo: "",
});

const receiver = ref({
  name: "",
  phone: "",
  address: "",
});

const isLoading = ref(false);
const loadError = ref("");
const isSubmitting = ref(false);

const toNumber = (value, fallback = 0) => {
  const converted = Number(value);
  return Number.isFinite(converted) ? converted : fallback;
};

const formatCurrency = (value) => {
  const fixed = toNumber(value, 0).toFixed(2);
  return fixed.endsWith(".00") ? fixed.slice(0, -3) : fixed;
};

const itemId = computed(() => {
  const value = Number(route.query.itemId);
  if (!Number.isInteger(value) || value <= 0) {
    return null;
  }
  return value;
});

const displayPrice = computed(() => formatCurrency(item.value.price));

const loadItem = async () => {
  if (!itemId.value) {
    loadError.value = "商品信息无效";
    return;
  }

  isLoading.value = true;
  loadError.value = "";

  try {
    const responseBody = await fetchItemDetail(itemId.value);
    const detail = responseBody.data || {};
    item.value = {
      id: detail.id ?? itemId.value,
      title:
        typeof detail.title === "string" && detail.title.trim()
          ? detail.title.trim()
          : "未命名物品",
      price: toNumber(detail.price, 0),
      photo:
        Array.isArray(detail.photos) && detail.photos.length > 0
          ? String(detail.photos[0])
          : "",
    };
  } catch (error) {
    loadError.value = error.message || "获取商品信息失败";
  } finally {
    isLoading.value = false;
  }
};

const submitOrder = async () => {
  if (!item.value.id) {
    ElMessage.warning("商品信息无效");
    return;
  }

  if (!receiver.value.name.trim()) {
    ElMessage.warning("请填写收货人姓名");
    return;
  }
  if (!receiver.value.phone.trim()) {
    ElMessage.warning("请填写联系电话");
    return;
  }
  if (!receiver.value.address.trim()) {
    ElMessage.warning("请填写收货地址");
    return;
  }

  isSubmitting.value = true;
  try {
    const responseBody = await createTradeOrder({
      itemId: item.value.id,
      receiverName: receiver.value.name.trim(),
      receiverPhone: receiver.value.phone.trim(),
      receiverAddress: receiver.value.address.trim(),
    });

    const orderId = responseBody?.data?.id;
    if (!orderId) {
      throw new Error("创建订单失败");
    }

    ElMessage.success("订单提交成功");
    router.push({
      name: "payment-method",
      query: { orderId: String(orderId) },
    });
  } catch (error) {
    ElMessage.error(error.message || "提交订单失败");
  } finally {
    isSubmitting.value = false;
  }
};

watch(
  () => route.query.itemId,
  () => {
    loadItem();
  },
  { immediate: true }
);
</script>

<style scoped>
.confirm-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 20px 14px 28px;
}

.confirm-main {
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 24px;
}

.card,
.state-card {
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  padding: 16px;
}

.state-card--error {
  border: 1px solid #fecaca;
}

h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.item-row {
  display: flex;
  gap: 12px;
}

.item-cover {
  width: 96px;
  height: 96px;
  border-radius: 10px;
  object-fit: cover;
  background: #eef2ff;
}

.item-cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}

.item-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.item-title {
  margin: 0;
  font-weight: 600;
}

.item-price {
  margin: 0;
  color: #f97316;
  font-size: 22px;
  font-weight: 700;
}

.address-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field span {
  font-size: 14px;
  color: #4b5563;
}

.field input,
.field textarea {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
}

.field textarea {
  resize: vertical;
}

.field input:focus,
.field textarea:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.2);
}

.submit-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.total-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: #374151;
}

.total-row strong {
  color: #f97316;
  font-size: 24px;
}

.primary-btn,
.secondary-btn {
  border-radius: 999px;
  border: none;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn {
  padding: 10px 28px;
  color: #ffffff;
  background: linear-gradient(135deg, #16a34a, #22c55e);
}

.primary-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.secondary-btn {
  padding: 8px 18px;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
}

@media (max-width: 640px) {
  .submit-card {
    flex-direction: column;
    align-items: stretch;
  }

  .primary-btn {
    width: 100%;
  }
}
</style>

