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
              rows="2"
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
import { fetchProfileOverview } from "../service/profile/profileApiService";

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
const isProfilePrefilled = ref(false);

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

const loadReceiverProfile = async () => {
  try {
    const responseBody = await fetchProfileOverview();
    const overview = responseBody?.data || {};
    receiver.value = {
      name:
        (typeof overview.name === "string" && overview.name.trim()) ||
        (typeof overview.username === "string" && overview.username.trim()) ||
        "",
      phone: typeof overview.phone === "string" ? overview.phone.trim() : "",
      address:
        typeof overview.address === "string" ? overview.address.trim() : "",
    };
    isProfilePrefilled.value = true;
  } catch {
    isProfilePrefilled.value = false;
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
    loadReceiverProfile();
  },
  { immediate: true }
);
</script>

<style scoped>
.confirm-page {
  min-height: 100vh;
  background: #f7f8fc;
  padding: 14px 14px 12px;
}

.confirm-main {
  max-width: 780px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
  color: #40376d;
}

.card,
.state-card {
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(134, 123, 199, 0.1);
  padding: 14px;
}

.state-card--error {
  border: 1px solid #fecaca;
}

h2 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #3f3868;
}

.item-row {
  display: flex;
  gap: 12px;
}

.item-cover {
  width: 82px;
  height: 82px;
  border-radius: 12px;
  object-fit: cover;
  background: #ede9ff;
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
  gap: 4px;
}

.item-title {
  margin: 0;
  font-weight: 600;
  font-size: 22px;
  color: #3f3868;
}

.item-price {
  margin: 0;
  color: #7764de;
  font-size: 28px;
  font-weight: 700;
}

.address-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.address-card::before {
  content: "";
  display: block;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, #c7b9ff, #ecd0ff);
  margin-bottom: 2px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field span {
  font-size: 16px;
  color: #5d537f;
}

.field input,
.field textarea {
  border: none;
  border-radius: 12px;
  padding: 8px 12px;
  font-size: 16px;
  line-height: 1.35;
  outline: none;
  background: #faf9ff;
  box-shadow: inset 0 0 0 1px #ddd4ff;
}

.field textarea {
  resize: vertical;
  min-height: 54px;
}

.field input:focus,
.field textarea:focus {
  box-shadow: inset 0 0 0 2px #bfb1f7;
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
  gap: 6px;
  color: #574b7c;
  font-size: 18px;
}

.total-row strong {
  color: #f26f3d;
  font-size: 28px;
}

.primary-btn,
.secondary-btn {
  border-radius: 999px;
  border: none;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn {
  padding: 8px 20px;
  color: #ffffff;
  background: #26ba5f;
  font-size: 18px;
}

.primary-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.secondary-btn {
  padding: 8px 18px;
  color: #61538b;
  border: 1px solid #dcd4ff;
  background: #f6f3ff;
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

