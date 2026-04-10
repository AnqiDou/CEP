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
            <img
              v-if="sellerAvatar"
              class="seller-avatar seller-avatar--image"
              :src="sellerAvatar"
              :alt="item.publisher.name"
            />
            <div v-else class="seller-avatar">{{ sellerInitial }}</div>
            <div class="seller-main">
              <p class="seller-name">{{ item.publisher.name }}</p>
              <p class="seller-meta">{{ sellerCreditLevel }}</p>
            </div>
          </div>
          <button class="seller-home-btn" type="button" @click="goToSellerHome">
            TA的主页
          </button>
        </section>

        <section class="detail-top card">
          <div
            :class="[
              'gallery-wrap',
              hasMultiplePhotos ? '' : 'gallery-wrap--single',
            ]"
          >
            <div v-if="hasMultiplePhotos" class="gallery-side">
              <div
                ref="galleryListRef"
                class="gallery-list soft-panel"
                @scroll="handleGalleryListScroll"
              >
                <button
                  v-for="(photo, index) in item.photos"
                  :key="`${photo}-${index}`"
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
              <button
                v-if="galleryCanScroll"
                class="gallery-scroll-btn"
                type="button"
                @click="scrollGalleryList"
              >
                {{ galleryAtBottom ? "回到顶部" : "向下查看更多" }}
              </button>
            </div>

            <div class="gallery-main-box soft-panel">
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
            <div class="summary-hero">
              <div class="summary-head-row">
                <h1 class="summary-title">{{ item.title }}</h1>
                <div class="summary-price-row">
                  <span class="summary-price">￥{{ displayPrice }}</span>
                </div>
              </div>
            </div>

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
                聊天咨询
              </button>
              <button class="primary-btn" type="button" @click="applyTrade">
                申请交易
              </button>
              <button
                class="danger-btn"
                type="button"
                @click="openReportDialog"
              >
                举报商品
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

    <section
      v-if="showReportDialog"
      class="report-dialog-mask"
      @click.self="closeReportDialog"
    >
      <div class="report-dialog card">
        <h3>举报该商品</h3>
        <p class="report-dialog-desc">
          请选择问题类型并补充说明，我们会尽快审核处理。
        </p>

        <div class="report-field">
          <label class="report-label" for="report-type-select">问题类型</label>
          <select
            id="report-type-select"
            v-model="reportType"
            class="toolbar-select"
          >
            <option value="PROHIBITED_CONTACT">违规联系方式</option>
            <option value="COUNTERFEIT">疑似假货</option>
            <option value="FRAUD_RISK">欺诈风险</option>
            <option value="OTHER">其他</option>
          </select>
        </div>

        <div class="report-field">
          <label class="report-label" for="report-content-input"
            >详细描述</label
          >
          <textarea
            id="report-content-input"
            v-model.trim="reportContent"
            class="notice-input"
            placeholder="请描述举报原因（至少5个字）"
          ></textarea>
        </div>

        <div class="report-actions">
          <button
            class="secondary-btn"
            type="button"
            @click="closeReportDialog"
          >
            取消
          </button>
          <button class="danger-btn" type="button" @click="submitReport">
            提交举报
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { Star } from "@element-plus/icons-vue";
import {
  addItemFavorite,
  fetchItemDetail,
  fetchItemFavoriteStatus,
  removeItemFavorite,
  reportItem,
} from "../api/item-detail/itemDetailApiService";

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
    avatar: "",
    credit: "良好",
  },
});

const item = ref(createEmptyItem());
const isLoading = ref(false);
const loadError = ref("");
const hasMultiplePhotos = computed(
  () => Array.isArray(item.value?.photos) && item.value.photos.length > 1
);
const sellerInitial = computed(() =>
  (item.value?.publisher?.name || "校").slice(0, 1)
);
const sellerAvatar = computed(() =>
  normalizeOptionalText(item.value?.publisher?.avatar)
);
const sellerCreditLevel = computed(() => {
  const creditText = normalizeOptionalText(item.value?.publisher?.credit);
  if (!creditText) return "卖家信用良好";
  return creditText.includes("信用") ? creditText : `卖家信用${creditText}`;
});

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
  const publisher = detail?.publisher || {};
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
      id: publisher?.id ?? null,
      name:
        typeof publisher?.name === "string" && publisher.name.trim()
          ? publisher.name.trim()
          : "校园用户",
      avatar:
        typeof publisher?.avatar === "string" && publisher.avatar.trim()
          ? publisher.avatar.trim()
          : "",
      credit:
        typeof publisher?.credit === "string" && publisher.credit.trim()
          ? publisher.credit.trim()
          : "良好",
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

const featureCards = computed(() => [
  {
    icon: "🌸",
    title: "成色状态",
    subtitle: item.value.usageDuration || "信息已完善",
    progress: item.value.usageDuration ? 76 : 62,
  },
  {
    icon: "🫧",
    title: "描述完整度",
    subtitle: item.value.description ? "细节清晰" : "基础信息",
    progress: item.value.description ? 88 : 58,
  },
  {
    icon: "🍯",
    title: "卖家信用",
    subtitle: sellerCreditLevel.value,
    progress: 82,
  },
]);

const activePhoto = ref("");
const isFavorite = ref(false);
const showReportDialog = ref(false);
const reportType = ref("PROHIBITED_CONTACT");
const reportContent = ref("");
const galleryListRef = ref(null);
const galleryCanScroll = ref(false);
const galleryAtBottom = ref(false);

const updateGalleryScrollState = () => {
  const container = galleryListRef.value;
  if (!container) {
    galleryCanScroll.value = false;
    galleryAtBottom.value = false;
    return;
  }
  const maxScrollTop = Math.max(
    0,
    container.scrollHeight - container.clientHeight
  );
  galleryCanScroll.value = maxScrollTop > 4;
  galleryAtBottom.value =
    maxScrollTop <= 4 || container.scrollTop >= maxScrollTop - 2;
};

const handleGalleryListScroll = () => {
  updateGalleryScrollState();
};

const scrollGalleryList = () => {
  const container = galleryListRef.value;
  if (!container) return;
  const step = Math.max(150, Math.floor(container.clientHeight * 0.75));
  const maxScrollTop = Math.max(
    0,
    container.scrollHeight - container.clientHeight
  );
  const nextTop = galleryAtBottom.value
    ? 0
    : Math.min(container.scrollTop + step, maxScrollTop);
  container.scrollTo({ top: nextTop, behavior: "smooth" });
};

const loadFavoriteStatus = async () => {
  const itemId = Number(route.params.id);
  if (!Number.isInteger(itemId) || itemId <= 0) {
    isFavorite.value = false;
    return;
  }

  try {
    const responseBody = await fetchItemFavoriteStatus(itemId);
    isFavorite.value = Boolean(responseBody?.data?.favorite);
  } catch {
    isFavorite.value = false;
  }
};

watch(
  () => route.params.id,
  () => {
    loadItemDetail();
    loadFavoriteStatus();
  },
  { immediate: true }
);

watch(
  () => [hasMultiplePhotos.value, item.value.photos.length],
  async () => {
    await nextTick();
    updateGalleryScrollState();
  },
  { immediate: true }
);

onMounted(() => {
  window.addEventListener("resize", updateGalleryScrollState);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", updateGalleryScrollState);
});

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
      sellerUserId: item.value.publisher.id
        ? String(item.value.publisher.id)
        : undefined,
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
    query: item.value.publisher.id
      ? {
          userId: String(item.value.publisher.id),
        }
      : undefined,
  });
  window.open(resolved.href, "_blank");
};

const toggleFavorite = async () => {
  const itemId = Number(item.value.id);
  if (!Number.isInteger(itemId) || itemId <= 0) {
    ElMessage.warning("当前物品不可收藏");
    return;
  }

  try {
    if (isFavorite.value) {
      await removeItemFavorite(itemId);
      isFavorite.value = false;
      ElMessage.success("已取消收藏");
      return;
    }

    await addItemFavorite(itemId);
    isFavorite.value = true;
    ElMessage.success("收藏成功");
  } catch (error) {
    ElMessage.error(error.message || "收藏操作失败");
  }
};

const openReportDialog = () => {
  if (!item.value.id) {
    ElMessage.warning("当前物品不可举报");
    return;
  }
  showReportDialog.value = true;
};

const closeReportDialog = () => {
  showReportDialog.value = false;
  reportType.value = "PROHIBITED_CONTACT";
  reportContent.value = "";
};

const submitReport = async () => {
  const itemId = Number(item.value.id);
  if (!Number.isInteger(itemId) || itemId <= 0) {
    ElMessage.warning("当前物品不可举报");
    return;
  }
  if (!reportContent.value || reportContent.value.length < 5) {
    ElMessage.warning("举报内容至少5个字");
    return;
  }
  try {
    await reportItem(itemId, {
      reportType: reportType.value,
      content: reportContent.value,
    });
    ElMessage.success("举报已提交");
    closeReportDialog();
  } catch (error) {
    ElMessage.error(error.message || "举报提交失败");
  }
};
</script>

<style scoped>
.item-detail-page {
  min-height: 100vh;
  background: #f8f8fc;
  color: #4a4464;
}

.detail-main {
  max-width: 1220px;
  margin: 0 auto;
  padding: 12px 30px 20px;
}

.card {
  border-radius: 26px;
  background: #fcfbff;
  box-shadow: 0 16px 42px rgba(156, 140, 192, 0.14);
}

.soft-panel {
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(165, 149, 201, 0.12);
}

.soft-gradient-panel {
  border-radius: 24px;
  padding: 20px 22px;
  background: linear-gradient(135deg, #c6b9ff 0%, #dbc8ff 45%, #f2dff7 100%);
  box-shadow: 0 14px 26px rgba(180, 160, 223, 0.22);
}

.state-card {
  margin-bottom: 20px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.state-card--error {
  border: 1px solid #f5d6de;
}

.state-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.state-desc {
  margin: 0;
  color: #756f8f;
}

.seller-bar {
  margin-bottom: 20px;
  padding: 18px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.seller-home-btn {
  border: none;
  border-radius: 999px;
  background: #efeaff;
  color: #5f4ca2;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
}

.seller-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e2d7ff, #f5e9ff);
  color: #6f56bf;
  font-size: 20px;
  font-weight: 600;
}

.seller-avatar--image {
  object-fit: cover;
}

.seller-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.seller-meta {
  margin: 4px 0 0;
  color: #7f789f;
  font-size: 13px;
}

.detail-top {
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 18px;
  align-items: stretch;
  min-height: clamp(420px, calc(100vh - 300px), 560px);
  height: clamp(420px, calc(100vh - 300px), 560px);
}

.gallery-wrap {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  gap: 14px;
  min-height: 0;
  height: 100%;
}

.gallery-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  max-height: clamp(360px, calc(100vh - 360px), 520px);
}

.gallery-wrap--single {
  grid-template-columns: minmax(0, 1fr);
}

.gallery-main-box {
  border-radius: 22px;
  overflow: hidden;
  padding: 10px;
  min-height: 0;
}

.gallery-main {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 16px;
}

.gallery-empty {
  width: 100%;
  aspect-ratio: 4 / 3;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9f98bb;
  font-size: 16px;
  background: #f6f2ff;
  border-radius: 16px;
}

.gallery-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  flex: 1;
  min-height: 0;
  max-height: none;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: #c6b8f6 transparent;
}

.gallery-list::-webkit-scrollbar {
  width: 7px;
}

.gallery-list::-webkit-scrollbar-thumb {
  background: #c6b8f6;
  border-radius: 999px;
}

.gallery-list::-webkit-scrollbar-track {
  background: transparent;
}

.gallery-scroll-btn {
  border: none;
  border-radius: 999px;
  background: #efeaff;
  color: #5f4ca2;
  font-size: 12px;
  font-weight: 600;
  height: 30px;
  cursor: pointer;
}

.gallery-thumb {
  border: 2px solid transparent;
  border-radius: 14px;
  overflow: hidden;
  padding: 0;
  background: #f8f5ff;
  cursor: pointer;
}

.gallery-thumb img {
  width: 100%;
  display: block;
  aspect-ratio: 3 / 4;
  object-fit: cover;
}

.gallery-thumb--active {
  border-color: #b8a2ff;
  box-shadow: 0 8px 18px rgba(184, 162, 255, 0.35);
}

.summary {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
}

.summary-head-row {
  width: 100%;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.summary-tip {
  margin: 0;
  font-size: 12px;
  color: #65589d;
}

.summary-title {
  margin: 0;
  min-width: 0;
  flex: 1;
  font-size: 30px;
  line-height: 1.25;
  color: #2f2950;
}

.summary-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6d6298;
}

.summary-price-row {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  flex-shrink: 0;
  gap: 8px;
}

.summary-price {
  margin: 0;
  font-size: 42px;
  font-weight: 600;
  color: #6a57b3;
}

.feature-card {
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(166, 151, 203, 0.14);
  padding: 12px;
}

.feature-icon {
  margin: 0;
  font-size: 20px;
}

.feature-title {
  margin: 4px 0 0;
  font-size: 14px;
  font-weight: 600;
  color: #473f6c;
}

.feature-subtitle {
  margin: 4px 0 8px;
  font-size: 12px;
  color: #867fad;
}

.feature-track {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: #ece5ff;
  overflow: hidden;
}

.feature-progress {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: #b29bff;
}

.summary-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #4f496f;
  font-size: 14px;
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
  width: 76px;
  flex-shrink: 0;
  color: #8e86b0;
}

.summary-meta__value {
  color: #514b70;
  font-size: 15px;
  line-height: 1.65;
}

.summary-meta__row--desc .summary-meta__value {
  font-size: 16px;
  line-height: 1.72;
}

.summary-scroll {
  overflow: visible;
  border-radius: 18px;
  padding: 12px 14px;
  background: #f7f3ff;
}

.summary-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.primary-btn,
.secondary-btn {
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn {
  border: none;
  padding: 11px 28px;
  color: #ffffff;
  background: #8f7ff0;
  box-shadow: 0 10px 18px rgba(143, 127, 240, 0.35);
}

.secondary-btn {
  border: none;
  padding: 11px 24px;
  color: #605092;
  background: #efeaff;
}

.danger-btn {
  border: none;
  border-radius: 999px;
  padding: 11px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  background: #fee2e2;
  color: #b91c1c;
  box-shadow: inset 0 0 0 1px #fecaca;
}

.report-dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(80, 64, 132, 0.22);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.report-dialog {
  width: min(92vw, 520px);
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border: 1px solid #ece3ff;
  background: linear-gradient(145deg, #fcfaff 0%, #f7f3ff 55%, #f3f8ff 100%);
  box-shadow: 0 22px 50px rgba(143, 127, 190, 0.2);
}

.report-dialog h3 {
  margin: 0;
  font-size: 20px;
  color: #41366d;
}

.report-dialog-desc {
  margin: -4px 0 2px;
  font-size: 13px;
  line-height: 1.6;
  color: #776ea0;
}

.report-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.report-label {
  font-size: 13px;
  font-weight: 600;
  color: #61548f;
}

.report-dialog .toolbar-select,
.report-dialog .notice-input {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  display: block;
  border: 1px solid #ddd2ff;
  border-radius: 14px;
  padding: 11px 14px;
  font-size: 14px;
  color: #4e4573;
  background: #ffffff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(164, 146, 210, 0.1);
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.report-dialog .toolbar-select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  cursor: pointer;
  background-image: linear-gradient(45deg, transparent 50%, #8f80d8 50%),
    linear-gradient(135deg, #8f80d8 50%, transparent 50%),
    linear-gradient(145deg, #ffffff 0%, #f6f2ff 100%);
  background-position: calc(100% - 18px) calc(50% - 2px),
    calc(100% - 12px) calc(50% - 2px), 0 0;
  background-size: 6px 6px, 6px 6px, 100% 100%;
  background-repeat: no-repeat;
  padding-right: 36px;
}

.report-dialog .toolbar-select option {
  background: #f8f4ff;
  color: #4f4675;
}

.report-dialog .toolbar-select option:checked {
  background: #d7c8ff;
  color: #2f2950;
}

.report-dialog .notice-input {
  min-height: 110px;
  line-height: 1.55;
  resize: vertical;
}

.report-dialog .toolbar-select:focus,
.report-dialog .notice-input:focus {
  border-color: #b7a4ff;
  box-shadow: 0 0 0 4px rgba(183, 164, 255, 0.18),
    0 10px 22px rgba(162, 144, 210, 0.18);
}

.report-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.favorite-icon-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: #f2ecff;
  color: #8379ae;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
}

.favorite-icon-btn--active {
  background: #ffeecf;
  color: #ca8a03;
}

@media (max-width: 980px) {
  .detail-top {
    grid-template-columns: minmax(0, 1fr);
    min-height: auto;
    height: auto;
  }

  .summary-title {
    font-size: 30px;
  }

  .summary-price {
    font-size: 36px;
  }
}

@media (max-width: 680px) {
  .detail-main {
    padding: 12px 14px 16px;
  }

  .gallery-wrap {
    grid-template-columns: minmax(0, 1fr);
  }

  .gallery-list {
    order: 2;
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    max-height: none;
    overflow: visible;
  }

  .gallery-side {
    max-height: none;
  }

  .gallery-scroll-btn {
    display: none;
  }

  .summary-actions {
    flex-wrap: wrap;
  }

  .seller-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

