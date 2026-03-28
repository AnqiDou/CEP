<template>
  <div class="review-page">
    <main class="review-main">
      <section v-if="isLoading" class="review-card">评价信息加载中...</section>

      <section v-else-if="loadError" class="review-card review-card--error">
        <p>{{ loadError }}</p>
        <button type="button" class="ghost-btn" @click="loadReviewDetail">
          重试
        </button>
      </section>

      <section v-else class="review-card">
        <header class="review-head">
          <h1 class="review-title">交易评价</h1>
          <p class="review-subtitle">你的反馈会影响对方信用评分</p>
        </header>

        <div class="item-row">
          <img
            v-if="detail.itemCover"
            :src="detail.itemCover"
            alt="商品图片"
            class="item-cover"
          />
          <div class="item-text">
            <p class="item-title">{{ detail.itemTitle }}</p>
            <p class="item-user">
              评价对象：{{ detail.targetUserName }}（{{ roleLabel }}）
            </p>
          </div>
        </div>

        <div class="rating-row">
          <button
            type="button"
            :class="[
              'rating-btn',
              rating === 'good' ? 'rating-btn--active-good' : '',
            ]"
            :disabled="!detail.canSubmit || isSubmitting"
            @click="rating = 'good'"
          >
            好评
          </button>
          <button
            type="button"
            :class="[
              'rating-btn',
              rating === 'bad' ? 'rating-btn--active-bad' : '',
            ]"
            :disabled="!detail.canSubmit || isSubmitting"
            @click="rating = 'bad'"
          >
            差评
          </button>
        </div>

        <textarea
          v-model="content"
          class="review-input"
          :disabled="!detail.canSubmit || isSubmitting"
          placeholder="可选：补充本次交易体验（最多300字）"
          maxlength="300"
        />

        <div class="actions">
          <button type="button" class="ghost-btn" @click="goBackToChat">
            返回聊天
          </button>
          <button
            type="button"
            class="primary-btn"
            :disabled="!detail.canSubmit || isSubmitting"
            @click="submitReview"
          >
            {{
              detail.canSubmit
                ? isSubmitting
                  ? "提交中..."
                  : "提交评价"
                : "已评价"
            }}
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import {
  fetchReviewOrderDetail,
  submitTradeReview,
} from "../service/review/reviewApiService";

const route = useRoute();
const router = useRouter();

const isLoading = ref(false);
const loadError = ref("");
const isSubmitting = ref(false);

const detail = ref({
  orderId: null,
  itemTitle: "",
  itemCover: "",
  targetUserName: "校园用户",
  targetRole: "seller",
  status: "PENDING",
  canSubmit: false,
});

const rating = ref("good");
const content = ref("");

const orderId = computed(() => {
  const value = Number(route.query.orderId);
  if (!Number.isInteger(value) || value <= 0) {
    return null;
  }
  return value;
});

const roleLabel = computed(() =>
  detail.value.targetRole === "seller" ? "卖家" : "买家"
);

const loadReviewDetail = async () => {
  if (!orderId.value) {
    loadError.value = "订单参数无效";
    return;
  }

  isLoading.value = true;
  loadError.value = "";
  try {
    const responseBody = await fetchReviewOrderDetail(orderId.value);
    detail.value = responseBody?.data || detail.value;
  } catch (error) {
    loadError.value = error.message || "加载评价信息失败";
  } finally {
    isLoading.value = false;
  }
};

const submitReview = async () => {
  if (!orderId.value) {
    ElMessage.warning("订单参数无效");
    return;
  }

  if (!detail.value.canSubmit) {
    ElMessage.info("该订单已评价");
    return;
  }

  isSubmitting.value = true;
  try {
    await submitTradeReview(orderId.value, {
      rating: rating.value,
      content: content.value,
    });
    ElMessage.success("评价提交成功");
    await loadReviewDetail();
  } catch (error) {
    ElMessage.error(error.message || "提交评价失败");
  } finally {
    isSubmitting.value = false;
  }
};

const goBackToChat = () => {
  router.replace({ name: "chat" });
};

watch(
  () => route.query.orderId,
  () => {
    loadReviewDetail();
  },
  { immediate: true }
);
</script>

<style scoped>
.review-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eff6ff 0%, #f8fbff 100%);
  padding: 28px 14px;
}

.review-main {
  max-width: 720px;
  margin: 0 auto;
}

.review-card {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.12);
  padding: 24px;
}

.review-card--error {
  border: 1px solid #fecaca;
}

.review-head {
  margin-bottom: 16px;
}

.review-title {
  margin: 0;
  color: #111827;
  font-size: 28px;
}

.review-subtitle {
  margin: 8px 0 0;
  color: #6b7280;
}

.item-row {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
}

.item-cover {
  width: 76px;
  height: 76px;
  border-radius: 10px;
  object-fit: cover;
}

.item-title {
  margin: 0;
  font-size: 17px;
  color: #111827;
}

.item-user {
  margin: 8px 0 0;
  color: #1d4ed8;
}

.rating-row {
  margin-top: 16px;
  display: flex;
  gap: 10px;
}

.rating-btn {
  flex: 1;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  padding: 10px;
  cursor: pointer;
  font-size: 15px;
}

.rating-btn--active-good {
  border-color: #16a34a;
  background: #f0fdf4;
  color: #15803d;
}

.rating-btn--active-bad {
  border-color: #dc2626;
  background: #fef2f2;
  color: #b91c1c;
}

.review-input {
  margin-top: 14px;
  width: 100%;
  min-height: 124px;
  border-radius: 12px;
  border: 1px solid #dbeafe;
  padding: 12px;
  resize: vertical;
  font-size: 14px;
  color: #1f2937;
}

.actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.primary-btn,
.ghost-btn {
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  padding: 10px 18px;
  cursor: pointer;
}

.primary-btn {
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}

.ghost-btn {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}
</style>

