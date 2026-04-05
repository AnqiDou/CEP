<template>
  <div class="other-profile-page">
    <main class="other-profile-main">
      <section class="seller-hero card">
        <div class="seller-hero__content">
          <div class="seller-info-wrap">
            <img
              class="seller-avatar"
              :src="seller.avatar"
              :alt="seller.name"
            />
            <div class="seller-meta-wrap">
              <div class="seller-name-row">
                <h1 class="seller-name">{{ seller.name }}</h1>
                <span class="seller-badge"
                  >卖家信用{{ seller.sellerCredit }}</span
                >
                <span class="seller-badge"
                  >买家信用{{ seller.buyerCredit }}</span
                >
              </div>
              <p class="seller-stats">
                {{ seller.fans }}粉丝 ｜ {{ seller.following }}关注
              </p>
            </div>
          </div>

          <button class="follow-btn" type="button" @click="toggleFollow">
            {{ isFollowing ? "已关注" : "关注" }}
          </button>
        </div>

        <div class="seller-tabs">
          <button
            :class="[
              'seller-tab',
              activeTab === 'goods' ? 'seller-tab--active' : '',
            ]"
            type="button"
            @click="activeTab = 'goods'"
          >
            <span class="seller-tab__label">已发布</span>
            <span>{{ seller.items.length }}</span>
          </button>
          <button
            :class="[
              'seller-tab',
              activeTab === 'review' ? 'seller-tab--active' : '',
            ]"
            type="button"
            @click="activeTab = 'review'"
          >
            <span class="seller-tab__label">信用及评价</span>
            <span>{{ seller.reviews.length }}</span>
          </button>
        </div>
      </section>

      <section v-if="activeTab === 'goods'" class="goods-panel">
        <div class="goods-toolbar">
          <div class="goods-status-tabs">
            <button
              :class="[
                'status-chip',
                activeStatus === 'all' ? 'status-chip--active' : '',
              ]"
              type="button"
              @click="activeStatus = 'all'"
            >
              综合
            </button>
            <button
              :class="[
                'status-chip',
                activeStatus === 'onsale' ? 'status-chip--active' : '',
              ]"
              type="button"
              @click="activeStatus = 'onsale'"
            >
              在售{{ onSaleCount }}
            </button>
            <button
              :class="[
                'status-chip',
                activeStatus === 'sold' ? 'status-chip--active' : '',
              ]"
              type="button"
              @click="activeStatus = 'sold'"
            >
              已售出{{ soldCount }}
            </button>
          </div>

          <div class="price-sort-buttons">
            <button
              :class="[
                'sort-btn',
                activeSort === 'price-desc' ? 'sort-btn--active' : '',
              ]"
              type="button"
              @click="activeSort = 'price-desc'"
            >
              高价优先
            </button>
            <button
              :class="[
                'sort-btn',
                activeSort === 'price-asc' ? 'sort-btn--active' : '',
              ]"
              type="button"
              @click="activeSort = 'price-asc'"
            >
              低价优先
            </button>
          </div>
        </div>

        <div class="goods-grid">
          <article
            v-for="goods in displayedGoods"
            :key="goods.id"
            class="goods-card"
            role="button"
            tabindex="0"
            @click="goToItemDetail(goods.id)"
            @keydown.enter="goToItemDetail(goods.id)"
          >
            <img
              :src="goods.image"
              :alt="goods.title"
              class="goods-card__image"
            />
            <div class="goods-card__body">
              <p class="goods-card__title">{{ goods.title }}</p>
              <p class="goods-card__meta">
                <span class="goods-card__price">￥{{ goods.price }}</span>
                <span class="goods-card__status">
                  {{ goods.status === "onsale" ? "在售" : "已售出" }}
                </span>
              </p>
            </div>
          </article>
        </div>

        <div v-if="displayedGoods.length === 0" class="empty-state">
          暂无符合条件的宝贝
        </div>
      </section>

      <section v-else class="review-panel">
        <div class="review-header">
          <h3>信用及评价</h3>
        </div>

        <div class="review-list-scroll">
          <article
            v-for="review in seller.reviews"
            :key="review.id"
            class="review-item"
          >
            <div class="review-item__top">
              <img
                class="review-avatar"
                :src="review.avatar"
                :alt="review.user"
              />
              <div class="review-main">
                <div class="review-user-row">
                  <p class="review-user">{{ review.user }}</p>
                  <span class="review-identity">{{ review.identity }}</span>
                </div>
                <div class="review-content-row">
                  <span class="review-tag">{{
                    getReviewTagText(review.tag)
                  }}</span>
                  <p class="review-content">{{ review.content }}</p>
                </div>
                <p class="review-time">{{ review.time }}</p>
              </div>
            </div>
          </article>

          <div v-if="seller.reviews.length === 0" class="empty-state">
            暂无评价
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  fetchOtherProfileItems,
  fetchOtherProfileOverview,
  fetchOtherProfileReviews,
  followOtherProfile,
  unfollowOtherProfile,
} from "../service/profile/profileApiService";

const route = useRoute();
const router = useRouter();

const activeTab = ref("goods");
const activeStatus = ref("all");
const activeSort = ref("price-desc");
const isLoading = ref(false);
const seller = ref({
  id: null,
  name: "校园卖家",
  avatar: "",
  fans: 0,
  following: 0,
  sellerCredit: "良好",
  buyerCredit: "良好",
  items: [],
  reviews: [],
});
const isFollowing = ref(false);

const sellerName = computed(() => String(route.params.name || "").trim());
const sellerUserId = computed(() => {
  const value = Number(route.query.userId);
  return Number.isInteger(value) && value > 0 ? value : null;
});

const onSaleCount = computed(
  () => seller.value.items.filter((item) => item.status === "onsale").length
);
const soldCount = computed(
  () => seller.value.items.filter((item) => item.status === "sold").length
);

const displayedGoods = computed(() => {
  let list = [...seller.value.items];

  if (activeStatus.value !== "all") {
    list = list.filter((item) => item.status === activeStatus.value);
  }

  if (activeSort.value === "price-desc") {
    list.sort((first, second) => second.price - first.price);
  }

  if (activeSort.value === "price-asc") {
    list.sort((first, second) => first.price - second.price);
  }

  return list;
});

const loadOtherProfile = async () => {
  isLoading.value = true;
  try {
    const params = {
      userId: sellerUserId.value,
      username: sellerName.value,
    };
    const [overviewBody, itemsBody, reviewsBody] = await Promise.all([
      fetchOtherProfileOverview(params),
      fetchOtherProfileItems({ ...params, status: "all", sort: "price-desc" }),
      fetchOtherProfileReviews({ ...params, rating: "all" }),
    ]);

    const overview = overviewBody?.data || {};
    const items = Array.isArray(itemsBody?.data) ? itemsBody.data : [];
    const reviews = Array.isArray(reviewsBody?.data?.reviews)
      ? reviewsBody.data.reviews
      : [];

    seller.value = {
      id: Number.isInteger(overview?.userId)
        ? overview.userId
        : sellerUserId.value,
      name:
        typeof overview?.username === "string" && overview.username.trim()
          ? overview.username.trim()
          : sellerName.value || "校园卖家",
      avatar:
        typeof overview?.avatar === "string" && overview.avatar.trim()
          ? overview.avatar.trim()
          : "",
      fans: Number.isInteger(overview?.fans) ? overview.fans : 0,
      following: Number.isInteger(overview?.following) ? overview.following : 0,
      sellerCredit:
        typeof overview?.sellerCredit === "string" &&
        overview.sellerCredit.trim()
          ? overview.sellerCredit.trim()
          : "良好",
      buyerCredit:
        typeof overview?.buyerCredit === "string" && overview.buyerCredit.trim()
          ? overview.buyerCredit.trim()
          : "良好",
      items: items.map((item) => ({
        id: item?.id ?? null,
        title:
          typeof item?.title === "string" && item.title.trim()
            ? item.title.trim()
            : "未命名宝贝",
        price: Number(item?.price) || 0,
        status: item?.status === "sold" ? "sold" : "onsale",
        image: typeof item?.image === "string" ? item.image : "",
      })),
      reviews: reviews.map((review) => ({
        id: review?.id ?? Date.now(),
        user:
          typeof review?.user === "string" && review.user.trim()
            ? review.user.trim()
            : "校园用户",
        identity:
          typeof review?.identity === "string" && review.identity.trim()
            ? review.identity.trim()
            : "交易方",
        time: typeof review?.time === "string" ? review.time : "",
        tag: typeof review?.tag === "string" ? review.tag : "好评",
        avatar: typeof review?.avatar === "string" ? review.avatar : "",
        content: typeof review?.content === "string" ? review.content : "",
      })),
    };
    isFollowing.value = overview?.followed === true;
  } catch (error) {
    ElMessage.error(error.message || "加载他人主页失败");
  } finally {
    isLoading.value = false;
  }
};

watch(
  () => [route.params.name, route.query.userId],
  () => {
    loadOtherProfile();
  },
  { immediate: true }
);

const toggleFollow = async () => {
  const params = {
    userId: seller.value.id,
    username: seller.value.name,
  };
  try {
    if (isFollowing.value) {
      await unfollowOtherProfile(params);
      isFollowing.value = false;
      seller.value.fans = Math.max(0, seller.value.fans - 1);
      ElMessage.success("已取消关注");
      return;
    }
    await followOtherProfile(params);
    isFollowing.value = true;
    seller.value.fans += 1;
    ElMessage.success("关注成功");
  } catch (error) {
    ElMessage.error(error.message || "关注操作失败");
  }
};

const getReviewTagText = (tag) => (tag === "差评" ? "😞 差评" : "🥰 好评");

const goToItemDetail = (id) => {
  const itemId = Number(id);
  if (!Number.isInteger(itemId) || itemId <= 0) {
    ElMessage.warning("商品信息异常，暂时无法打开详情");
    return;
  }
  const resolved = router.resolve(`/item/${itemId}`);
  window.open(resolved.href, "_blank");
};
</script>

<style scoped>
.other-profile-page {
  min-height: 100vh;
  background: #f7f8fc;
  color: #2c2f45;
}

.other-profile-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

.card {
  background: #ffffff;
  border-radius: 26px;
  box-shadow: 0 14px 38px rgba(140, 124, 240, 0.11);
}

.seller-hero {
  border-radius: 26px 26px 0 0;
  overflow: hidden;
  background: #ffffff;
  position: relative;
}

.seller-hero__content {
  margin-top: 0;
  padding: 18px 24px 18px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.seller-info-wrap {
  display: flex;
  gap: 18px;
}

.seller-avatar {
  width: 108px;
  height: 108px;
  border-radius: 50%;
  border: 5px solid #ffffff;
  box-shadow: 0 8px 20px rgba(140, 124, 240, 0.16);
  object-fit: cover;
}

.seller-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.seller-name {
  margin: 0;
  font-size: 30px;
  line-height: 1.05;
  color: #232640;
}

.seller-badge {
  padding: 4px 12px;
  border-radius: 999px;
  background: #ece6ff;
  font-size: 13px;
  font-weight: 700;
  color: #6f5dd9;
}

.seller-stats {
  margin: 10px 0 0;
  font-size: 16px;
  color: #616587;
}

.follow-btn {
  margin-top: 16px;
  border: none;
  border-radius: 999px;
  padding: 11px 34px;
  background: #8c7cf0;
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 10px 20px rgba(140, 124, 240, 0.28);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.follow-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(140, 124, 240, 0.32);
}

.seller-tabs {
  display: flex;
  gap: 30px;
  border-top: none;
  padding: 16px 24px 0;
  background: transparent;
}

.seller-tab {
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  line-height: 1.2;
  color: #707492;
  font-size: 20px !important;
  padding: 8px 0 16px;
  cursor: pointer;
}

.seller-tab__label {
  color: inherit;
  font-size: inherit;
  line-height: inherit;
}

.seller-tab span {
  font-size: 18px;
}

.seller-tab--active {
  color: #6f5dd9;
  font-weight: 800;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  border-radius: 0 !important;
}

.goods-panel,
.review-panel {
  margin-top: 0;
  padding: 22px;
  background: #fcfbff;
  border-radius: 0 0 26px 26px;
  box-shadow: 0 14px 38px rgba(140, 124, 240, 0.11);
}

.review-panel {
  height: calc(100vh - 230px);
  min-height: 380px;
  display: flex;
  flex-direction: column;
}

.review-list-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.goods-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
  position: relative;
}

.goods-status-tabs {
  display: flex;
  gap: 10px;
}

.status-chip {
  border: 1px solid #ece8ff;
  border-radius: 14px;
  background: #f8f5ff;
  color: #545a80;
  font-size: 15px;
  padding: 9px 16px;
  cursor: pointer;
}

.status-chip--active {
  background: #e9e2ff;
  border-color: #c6b9ff;
  color: #6f5dd9;
  font-weight: 700;
}

.price-sort-buttons {
  display: flex;
  gap: 10px;
}

.sort-btn {
  border: 1px solid #e8e3ff;
  border-radius: 999px;
  padding: 8px 15px;
  font-size: 14px;
  color: #6f5dd9;
  background: #f8f5ff;
  cursor: pointer;
}

.sort-btn--active {
  background: #ece6ff;
  border-color: #c6b9ff;
  font-weight: 700;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.goods-card {
  border-radius: 18px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(140, 124, 240, 0.09);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.goods-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(140, 124, 240, 0.16);
}

.goods-card__image {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  display: block;
}

.goods-card__body {
  padding: 11px;
}

.goods-card__title {
  margin: 0;
  font-size: 15px;
  line-height: 1.35;
  min-height: 44px;
}

.goods-card__meta {
  margin: 6px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-card__price {
  color: #f06f72;
  font-weight: 700;
}

.goods-card__status {
  color: #8085a7;
  font-size: 13px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.review-header h3 {
  margin: 0;
  font-size: 18px;
}

.review-item {
  margin-top: 0;
  border-bottom: 1px solid #efebff;
  padding: 14px 0;
  background: transparent;
}

.review-item:last-child {
  border-bottom: none;
}

.review-item__top {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.review-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.review-main {
  flex: 1;
}

.review-user-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-identity {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #f1ecff;
  color: #6d5bb1;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
}

.review-user {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}

.review-time {
  margin: 6px 0 0;
  color: #8085a7;
  font-size: 12px;
}

.review-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #ece6ff;
  color: #6f5dd9;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 9px;
}

.review-content-row {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-content {
  margin: 0;
  color: #4d5378;
  line-height: 1.55;
}

.empty-state {
  padding: 14px 0;
  text-align: center;
  color: #8489ab;
}

@media (max-width: 1120px) {
  .goods-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .other-profile-main {
    padding: 14px;
  }

  .seller-hero__content {
    margin-top: 0;
    flex-direction: column;
  }

  .seller-avatar {
    width: 80px;
    height: 80px;
  }

  .seller-name {
    font-size: 24px;
  }

  .seller-stats {
    font-size: 14px;
  }

  .follow-btn {
    font-size: 16px;
    padding: 10px 22px;
  }

  .seller-tab {
    font-size: 18px;
  }

  .seller-tab span {
    font-size: 16px;
  }

  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
