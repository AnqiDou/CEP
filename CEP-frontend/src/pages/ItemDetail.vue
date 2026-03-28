<template>
  <div class="item-detail-page">
    <main class="detail-main">
      <section v-if="isLoading" class="state-card card">
        <p class="state-title">详情加载中...</p>
      </section>

      <section v-else-if="loadError" class="state-card card state-card--error">
        <p class="state-title">加载失败</p>
        <p class="state-desc">{{ loadError }}</p>
        <button class="secondary-btn" type="button" @click="loadItemDetail">
          重试
        </button>
      </section>

      <template v-else>
        <section class="seller-bar card">
          <div class="seller-info">
            <div class="seller-avatar">{{ sellerInitial }}</div>
            <div>
              <p class="seller-name">{{ item.publisher.name }}</p>
              <p class="seller-meta">
                {{ item.publisher.college }} · {{ item.publisher.campus }} ·
                信用
                {{ item.publisher.credit }}
              </p>
            </div>
            <button
              class="seller-home-btn"
              type="button"
              @click="goToSellerHome"
            >
              TA的主页
            </button>
          </div>
          <p class="seller-note">{{ item.publisher.note }}</p>
        </section>

        <section class="detail-top card">
          <div
            :class="[
              'gallery-wrap',
              hasMultiplePhotos ? '' : 'gallery-wrap--single',
            ]"
          >
            <div v-if="hasMultiplePhotos" class="gallery-list">
              <button
                v-for="(photo, index) in item.photos"
                :key="photo"
                :class="[
                  'gallery-thumb',
                  activePhoto === photo ? 'gallery-thumb--active' : '',
                ]"
                type="button"
                @click="activePhoto = photo"
              >
                <img :src="photo" :alt="`${item.title}-图片${index + 1}`" />
              </button>
            </div>

            <div class="gallery-main-box">
              <img
                v-if="activePhoto"
                :src="activePhoto"
                :alt="item.title"
                class="gallery-main"
              />
              <div v-else class="gallery-empty">暂无图片</div>
            </div>
          </div>

          <div class="summary">
            <div class="summary-price-row">
              <span class="summary-price">￥{{ displayPrice }}</span>
            </div>
            <h1 class="summary-title">{{ item.title }}</h1>

            <div class="summary-scroll">
              <div class="summary-meta">
                <div
                  v-for="row in detailRows"
                  :key="row.label"
                  :class="[
                    'summary-meta__row',
                    row.multiline ? 'summary-meta__row--desc' : '',
                  ]"
                >
                  <span class="summary-meta__label">{{ row.label }}</span>
                  <span class="summary-meta__value">{{ row.value }}</span>
                </div>
              </div>
            </div>

            <div class="summary-actions">
              <button class="secondary-btn" type="button" @click="startChat">
                聊天
              </button>
              <button class="primary-btn" type="button" @click="applyTrade">
                申请交易
              </button>
              <button
                :class="[
                  'favorite-icon-btn',
                  isFavorite ? 'favorite-icon-btn--active' : '',
                ]"
                type="button"
                @click="toggleFavorite"
                aria-label="收藏"
              >
                <el-icon><Star /></el-icon>
              </button>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { Star } from "@element-plus/icons-vue";
import { fetchItemDetail } from "../service/item-detail/itemDetailApiService";

const route = useRoute();
const router = useRouter();

const createEmptyItem = () => ({
  id: null,
  title: "",
  price: 0,
  category: "",
  purchaseDate: "",
  usageDuration: "",
  description: "",
  photos: [],
  publisher: {
    id: null,
    name: "校园用户",
    college: "未填写学院",
    campus: "未填写",
    credit: 4.5,
    note: "该用户暂未完善个人简介",
  },
});

const item = ref(createEmptyItem());
const isLoading = ref(false);
const loadError = ref("");
const hasMultiplePhotos = computed(() => item.value.photos.length > 1);
const sellerInitial = computed(() =>
  (item.value.publisher.name || "校").slice(0, 1)
);

const toNumber = (value, fallback = 0) => {
  const converted = Number(value);
  return Number.isFinite(converted) ? converted : fallback;
};

const PLACEHOLDER_VALUES = new Set([
  "未填写",
  "暂无",
  "暂无描述",
  "null",
  "undefined",
  "-",
]);

const normalizeOptionalText = (value) => {
  if (typeof value !== "string") {
    return "";
  }
  const trimmed = value.trim();
  if (!trimmed || PLACEHOLDER_VALUES.has(trimmed)) {
    return "";
  }
  return trimmed;
};

const formatCurrency = (value) => {
  const fixed = toNumber(value, 0).toFixed(2);
  return fixed.endsWith(".00") ? fixed.slice(0, -3) : fixed;
};

const formatDate = (value) => {
  if (!value) {
    return "";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return typeof value === "string" ? value.trim() : "";
  }
  const year = parsed.getFullYear();
  const month = String(parsed.getMonth() + 1).padStart(2, "0");
  const day = String(parsed.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const mapItemDetail = (detail) => {
  const photos = Array.isArray(detail?.photos)
    ? detail.photos
        .filter((photo) => typeof photo === "string")
        .map((photo) => photo.trim())
        .filter(Boolean)
    : [];

  const price = toNumber(detail?.price, 0);
  return {
    id: detail?.id ?? null,
    title:
      typeof detail?.title === "string" && detail.title.trim()
        ? detail.title.trim()
        : "未命名物品",
    price,
    category:
      typeof detail?.categoryName === "string" && detail.categoryName.trim()
        ? detail.categoryName.trim()
        : "其他",
    purchaseDate: formatDate(detail?.purchaseDate),
    usageDuration: normalizeOptionalText(detail?.usageDuration),
    description: normalizeOptionalText(detail?.description),
    photos,
    publisher: {
      id: detail?.publisher?.id ?? null,
      name:
        typeof detail?.publisher?.name === "string" &&
        detail.publisher.name.trim()
          ? detail.publisher.name.trim()
          : "校园用户",
      college:
        typeof detail?.publisher?.college === "string" &&
        detail.publisher.college.trim()
          ? detail.publisher.college.trim()
          : "未填写学院",
      campus:
        typeof detail?.publisher?.campus === "string" &&
        detail.publisher.campus.trim()
          ? detail.publisher.campus.trim()
          : "未填写",
      credit: Number(toNumber(detail?.publisher?.credit, 4.5).toFixed(1)),
      note:
        typeof detail?.publisher?.note === "string" &&
        detail.publisher.note.trim()
          ? detail.publisher.note.trim()
          : "该用户暂未完善个人简介",
    },
  };
};

const loadItemDetail = async () => {
  const itemId = Number(route.params.id);
  if (!Number.isInteger(itemId) || itemId <= 0) {
    loadError.value = "物品 ID 无效";
    item.value = createEmptyItem();
    activePhoto.value = "";
    return;
  }

  isLoading.value = true;
  loadError.value = "";

  try {
    const responseBody = await fetchItemDetail(itemId);
    item.value = mapItemDetail(responseBody.data);
    activePhoto.value = item.value.photos[0] || "";
  } catch (error) {
    item.value = createEmptyItem();
    activePhoto.value = "";
    loadError.value = error.message || "获取物品详情失败";
  } finally {
    isLoading.value = false;
  }
};

const displayPrice = computed(() => formatCurrency(item.value.price));

const detailRows = computed(() => {
  const rows = [
    { label: "物品名称", value: item.value.title },
    { label: "分类", value: item.value.category },
    { label: "购买时间", value: item.value.purchaseDate },
    { label: "使用时长", value: item.value.usageDuration },
    { label: "描述", value: item.value.description, multiline: true },
  ];

  return rows.filter((row) => normalizeOptionalText(row.value));
});

const activePhoto = ref("");
const isFavorite = ref(false);

watch(
  () => route.params.id,
  () => {
    loadItemDetail();
    isFavorite.value = false;
  },
  { immediate: true }
);

const applyTrade = () => {
  if (!item.value.id) {
    ElMessage.warning("当前物品不可交易");
    return;
  }
  router.push({
    name: "confirm-order",
    query: {
      itemId: String(item.value.id),
    },
  });
};

const startChat = () => {
  if (!item.value.id) {
    ElMessage.warning("当前物品不可聊天");
    return;
  }
  const resolved = router.resolve({
    path: "/chat",
    query: {
      itemId: String(item.value.id),
      itemTitle: item.value.title,
      sellerName: item.value.publisher.name,
    },
  });
  window.open(resolved.href, "_blank");
};

const goToSellerHome = () => {
  if (!item.value.publisher.name) {
    ElMessage.warning("卖家信息暂不可用");
    return;
  }
  const resolved = router.resolve({
    name: "other-profile",
    params: {
      name: item.value.publisher.name,
    },
  });
  window.open(resolved.href, "_blank");
};

const toggleFavorite = () => {
  isFavorite.value = !isFavorite.value;
  ElMessage.info("收藏功能即将上线");
};
</script>

<style scoped>
.item-detail-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4ff 0%, #f8fbff 36%, #f3f6fb 100%);
  color: #1f2937;
}

.detail-main {
  max-width: 1160px;
  margin: 0 auto;
  padding-left: 24px;
  padding-right: 24px;
}

.card {
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.state-card {
  margin-bottom: 14px;
  padding: 22px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.state-card--error {
  border: 1px solid #fecaca;
}

.state-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.state-desc {
  margin: 0;
  color: #6b7280;
}

.detail-main {
  padding-top: 24px;
  padding-bottom: 28px;
}

.seller-bar {
  margin-bottom: 14px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seller-home-btn {
  margin-left: auto;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
}

.seller-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #bfdbfe, #93c5fd);
  color: #1d4ed8;
  font-size: 20px;
  font-weight: 700;
}

.seller-name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.seller-meta,
.seller-note {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.detail-top {
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 18px;
  align-items: stretch;
}

.gallery-wrap {
  display: grid;
  grid-template-columns: 126px minmax(0, 1fr);
  gap: 12px;
}

.gallery-wrap--single {
  grid-template-columns: minmax(0, 1fr);
}

.gallery-main-box {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #dbeafe;
  background: #ffffff;
}

.gallery-main {
  width: 100%;
  aspect-ratio: 4 / 5;
  object-fit: cover;
}

.gallery-empty {
  width: 100%;
  aspect-ratio: 4 / 5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 16px;
  background: #f8fbff;
}

.gallery-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.gallery-thumb {
  border: 1px solid #dbeafe;
  border-radius: 10px;
  overflow: hidden;
  padding: 0;
  background: #ffffff;
  cursor: pointer;
}

.gallery-thumb img {
  width: 100%;
  display: block;
  aspect-ratio: 3 / 4;
  object-fit: cover;
}

.gallery-thumb--active {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.summary-title {
  margin: 8px 0 0;
  font-size: 38px;
  line-height: 1.25;
  color: #111827;
}

.summary-price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.summary-price {
  margin: 0;
  font-size: 46px;
  font-weight: 700;
  color: #f97316;
}

.summary-origin-price {
  color: #9ca3af;
  font-size: 18px;
  text-decoration: line-through;
}

.summary-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #374151;
  font-size: 13px;
}

.summary-meta__row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.summary-meta__row--desc {
  align-items: flex-start;
}

.summary-meta__label {
  width: 72px;
  flex-shrink: 0;
  color: #6b7280;
}

.summary-meta__value {
  color: #1f2937;
  line-height: 1.6;
}

.summary-scroll {
  margin-top: 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  border-radius: 12px;
  border: 1px solid #dbeafe;
  padding: 10px 12px;
  background: #f8fbff;
}

.summary-actions {
  margin-top: auto;
  padding-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.primary-btn,
.secondary-btn,
.ghost-btn {
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn {
  border: none;
  padding: 10px 28px;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.3);
}

.secondary-btn {
  border: 1px solid #bfdbfe;
  padding: 9px 24px;
  color: #1d4ed8;
  background: #eff6ff;
}

.favorite-icon-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #6b7280;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
}

.favorite-icon-btn--active {
  border-color: #f59e0b;
  background: #fff7ed;
  color: #f59e0b;
}

@media (max-width: 980px) {
  .detail-top {
    grid-template-columns: minmax(0, 1fr);
  }

  .summary-title {
    font-size: 30px;
  }

  .summary-price {
    font-size: 40px;
  }
}

@media (max-width: 680px) {
  .detail-main {
    padding-left: 14px;
    padding-right: 14px;
  }

  .gallery-wrap {
    grid-template-columns: minmax(0, 1fr);
  }

  .gallery-list {
    order: 2;
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .seller-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
