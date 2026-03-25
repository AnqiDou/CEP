<template>
  <div class="other-profile-page">
    <main class="other-profile-main">
      <section class="seller-hero card">
        <div class="seller-cover"></div>

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
                <span class="seller-badge">卖家信用较好</span>
                <span class="seller-badge">买家信用较好</span>
              </div>
              <p class="seller-stats">
                {{ seller.city }} ｜ {{ seller.fans }}粉丝 ｜
                {{ seller.following }}关注
              </p>
              <p class="seller-intro">{{ seller.bio }}</p>
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
            宝贝 <span>{{ seller.items.length }}</span>
          </button>
          <button
            :class="[
              'seller-tab',
              activeTab === 'review' ? 'seller-tab--active' : '',
            ]"
            type="button"
            @click="activeTab = 'review'"
          >
            信用及评价 <span>{{ seller.reviews.length }}</span>
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
              <p class="review-user">{{ review.user }}</p>
              <p class="review-content">
                <span class="review-tag">{{
                  getReviewTagText(review.tag)
                }}</span>
                {{ review.content }}
              </p>
              <p class="review-time">{{ review.time }}</p>
            </div>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const createMockPhoto = (label, color) =>
  `data:image/svg+xml;utf8,${encodeURIComponent(
    `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 900 900'>
      <defs>
        <linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>
          <stop offset='0%' stop-color='${color}'/>
          <stop offset='100%' stop-color='#e2e8f0'/>
        </linearGradient>
      </defs>
      <rect width='900' height='900' fill='url(#g)'/>
      <text x='450' y='460' text-anchor='middle' font-size='56' fill='#1e3a8a' font-family='Arial'>${label}</text>
    </svg>`
  )}`;

const sellerMap = {
  王同学: {
    name: "王同学",
    avatar: createMockPhoto("王", "#bfdbfe"),
    city: "浙江省",
    fans: 64,
    following: 5,
    bio: "校园闲置爱好者，主打教材和生活好物，支持当面验货交易。",
    credit: 4.8,
    items: [
      {
        id: "g1",
        title: "22级高数教材+辅导书全套",
        price: 35,
        status: "onsale",
        image: createMockPhoto("教材", "#dbeafe"),
      },
      {
        id: "g2",
        title: "英语六级听力专项",
        price: 15,
        status: "onsale",
        image: createMockPhoto("六级", "#c7d2fe"),
      },
      {
        id: "g3",
        title: "二手羽毛球拍",
        price: 68,
        status: "sold",
        image: createMockPhoto("球拍", "#93c5fd"),
      },
      {
        id: "g4",
        title: "宿舍收纳套盒",
        price: 39,
        status: "onsale",
        image: createMockPhoto("收纳", "#bfdbfe"),
      },
      {
        id: "g5",
        title: "保温杯（九成新）",
        price: 20,
        status: "sold",
        image: createMockPhoto("水杯", "#a5b4fc"),
      },
    ],
    reviews: [
      {
        id: "r1",
        user: "李同学",
        time: "2天前",
        tag: "好评",
        avatar: createMockPhoto("李", "#93c5fd"),
        content: "回复快，面交准时，物品和描述一致。",
      },
      {
        id: "r2",
        user: "张同学",
        time: "1周前",
        tag: "好评",
        avatar: createMockPhoto("张", "#bfdbfe"),
        content: "卖家很好沟通，交易过程顺利。",
      },
      {
        id: "r3",
        user: "陈同学",
        time: "2周前",
        tag: "差评",
        avatar: createMockPhoto("陈", "#a5b4fc"),
        content: "整体不错，建议提前确认具体地点。",
      },
    ],
  },
};

const fallbackSeller = {
  name: "校园卖家",
  avatar: createMockPhoto("卖", "#bfdbfe"),
  city: "本地",
  fans: 18,
  following: 3,
  bio: "仅前端演示数据，后端暂未接入。",
  credit: 4.7,
  items: [
    {
      id: "f1",
      title: "校园闲置物品",
      price: 99,
      status: "onsale",
      image: createMockPhoto("闲置", "#dbeafe"),
    },
  ],
  reviews: [
    {
      id: "fr1",
      user: "平台用户",
      time: "刚刚",
      tag: "好评",
      avatar: createMockPhoto("平", "#93c5fd"),
      content: "演示数据：卖家沟通友好。",
    },
  ],
};

const activeTab = ref("goods");
const activeStatus = ref("all");
const activeSort = ref("price-desc");
const isFollowing = ref(false);

const sellerName = computed(() => String(route.params.name || "").trim());
const seller = computed(
  () =>
    sellerMap[sellerName.value] || {
      ...fallbackSeller,
      name: sellerName.value || fallbackSeller.name,
    }
);

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

const toggleFollow = () => {
  isFollowing.value = !isFollowing.value;
};

const getReviewTagText = (tag) => (tag === "差评" ? "😞 差评" : "🥰 好评");
</script>

<style scoped>
.other-profile-page {
  min-height: 100vh;
  background: #f5f7fb;
  color: #1f2933;
}

.other-profile-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 14px 20px 28px;
}

.card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.seller-hero {
  border-radius: 16px 16px 0 0;
  overflow: hidden;
  background: linear-gradient(90deg, #eff6ff 0%, #dbeafe 100%);
}

.seller-cover {
  height: 140px;
  background: transparent;
}

.seller-hero__content {
  margin-top: -86px;
  padding: 0 18px 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.seller-info-wrap {
  display: flex;
  gap: 16px;
}

.seller-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 4px solid #ffffff;
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
}

.seller-badge {
  padding: 3px 10px;
  border-radius: 999px;
  background: #dbeafe;
  font-size: 13px;
  font-weight: 700;
  color: #1d4ed8;
}

.seller-stats {
  margin: 8px 0 0;
  font-size: 16px;
  color: #475569;
}

.seller-intro {
  margin: 8px 0 0;
  max-width: 920px;
  font-size: 16px;
  color: #374151;
}

.follow-btn {
  margin-top: 14px;
  border: 1px solid #2563eb;
  border-radius: 999px;
  padding: 10px 30px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
}

.seller-tabs {
  display: flex;
  gap: 26px;
  border-top: 1px solid #e5e7eb;
  padding: 14px 18px 0;
  background: #ffffff;
}

.seller-tab {
  border: none;
  background: transparent;
  color: #475569;
  font-size: 20px;
  padding: 8px 0 14px;
  cursor: pointer;
}

.seller-tab span {
  font-size: 18px;
}

.seller-tab--active {
  color: #1d4ed8;
  font-weight: 800;
  border-bottom: 4px solid #2563eb;
}

.goods-panel,
.review-panel {
  margin-top: 0;
  padding: 16px;
  background: #ffffff;
  border-radius: 0 0 16px 16px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.goods-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  position: relative;
}

.goods-status-tabs {
  display: flex;
  gap: 10px;
}

.status-chip {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f3f4f6;
  color: #1f2937;
  font-size: 15px;
  padding: 8px 14px;
  cursor: pointer;
}

.status-chip--active {
  background: #e5edff;
  border-color: #93c5fd;
  color: #1d4ed8;
  font-weight: 700;
}

.price-sort-buttons {
  display: flex;
  gap: 10px;
}

.sort-btn {
  border: 1px solid #dbeafe;
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 14px;
  color: #1d4ed8;
  background: #ffffff;
  cursor: pointer;
}

.sort-btn--active {
  background: #e5edff;
  border-color: #93c5fd;
  font-weight: 700;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.goods-card {
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
}

.goods-card__image {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  display: block;
}

.goods-card__body {
  padding: 8px;
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
  color: #ef4444;
  font-weight: 700;
}

.goods-card__status {
  color: #6b7280;
  font-size: 13px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-header h3 {
  margin: 0;
  font-size: 18px;
}

.review-item {
  margin-top: 0;
  border-bottom: 1px solid #e5e7eb;
  padding: 12px 0;
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

.review-user {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}

.review-time {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.review-tag {
  margin-right: 8px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #e5edff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 9px;
}

.review-content {
  margin: 8px 0 0;
  color: #374151;
  line-height: 1.55;
}

.empty-state {
  padding: 14px 0;
  text-align: center;
  color: #6b7280;
}

@media (max-width: 1120px) {
  .goods-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .other-profile-main {
    padding: 12px;
  }

  .seller-hero__content {
    margin-top: -72px;
    flex-direction: column;
  }

  .seller-avatar {
    width: 80px;
    height: 80px;
  }

  .seller-name {
    font-size: 24px;
  }

  .seller-stats,
  .seller-intro {
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
