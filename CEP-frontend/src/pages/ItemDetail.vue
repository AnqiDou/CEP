<template>
  <div class="item-detail-page">
    <main class="detail-main">
      <section class="seller-bar card">
        <div class="seller-info">
          <div class="seller-avatar">{{ item.publisher.name.slice(0, 1) }}</div>
          <div>
            <p class="seller-name">{{ item.publisher.name }}</p>
            <p class="seller-meta">
              {{ item.publisher.college }} · {{ item.publisher.campus }} · 信用
              {{ item.publisher.credit }}
            </p>
          </div>
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
            <img :src="activePhoto" :alt="item.title" class="gallery-main" />
          </div>
        </div>

        <div class="summary">
          <div class="summary-price-row">
            <span class="summary-price">￥{{ item.price }}</span>
            <span class="summary-origin-price">原价￥{{ originPrice }}</span>
          </div>
          <h1 class="summary-title">{{ item.title }}</h1>

          <div class="summary-scroll">
            <div class="summary-meta">
              <div class="summary-meta__row">
                <span class="summary-meta__label">物品名称</span>
                <span class="summary-meta__value">{{ item.title }}</span>
              </div>
              <div class="summary-meta__row">
                <span class="summary-meta__label">分类</span>
                <span class="summary-meta__value">{{ item.category }}</span>
              </div>
              <div class="summary-meta__row">
                <span class="summary-meta__label">购买时间</span>
                <span class="summary-meta__value">{{ item.purchaseDate }}</span>
              </div>
              <div class="summary-meta__row">
                <span class="summary-meta__label">使用时长</span>
                <span class="summary-meta__value">{{
                  item.usageDuration
                }}</span>
              </div>
              <div class="summary-meta__row">
                <span class="summary-meta__label">交易地点</span>
                <span class="summary-meta__value">{{ item.location }}</span>
              </div>
              <div class="summary-meta__row">
                <span class="summary-meta__label">发布时间</span>
                <span class="summary-meta__value">{{ item.publishTime }}</span>
              </div>
              <div class="summary-meta__row">
                <span class="summary-meta__label">成色</span>
                <span class="summary-meta__value">{{ item.condition }}</span>
              </div>
              <div class="summary-meta__row">
                <span class="summary-meta__label">配件</span>
                <span class="summary-meta__value">{{ item.accessories }}</span>
              </div>
              <div class="summary-meta__row summary-meta__row--desc">
                <span class="summary-meta__label">描述</span>
                <span class="summary-meta__value">{{ item.description }}</span>
              </div>
              <div class="summary-meta__row summary-meta__row--desc">
                <span class="summary-meta__label">说明</span>
                <span class="summary-meta__value">{{ item.detailNote }}</span>
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
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { Star } from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();

const createMockPhoto = (label, color) =>
  `data:image/svg+xml;utf8,${encodeURIComponent(
    `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 900 620'>
      <defs>
        <linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>
          <stop offset='0%' stop-color='${color}'/>
          <stop offset='100%' stop-color='#e2e8f0'/>
        </linearGradient>
      </defs>
      <rect width='900' height='620' fill='url(#g)'/>
      <text x='450' y='320' text-anchor='middle' font-size='56' fill='#1e3a8a' font-family='Arial'>${label}</text>
    </svg>`
  )}`;

const itemMap = {
  1: {
    title: "22级高数教材+辅导书全套",
    price: 35,
    category: "学习教材 / 书籍",
    purchaseDate: "2025-09-01",
    usageDuration: "1 学期",
    location: "东校区",
    publishTime: "1小时前",
    description: "教材整套转让，笔记较少，适合低年级同学开学入手。",
    condition: "9 成新",
    accessories: "教材 3 本、辅导资料 2 本",
    detailNote: "支持当面验货，不支持站外转账。",
    photos: [
      createMockPhoto("高数教材-图1", "#bfdbfe"),
      createMockPhoto("高数教材-图2", "#93c5fd"),
      createMockPhoto("高数教材-图3", "#c7d2fe"),
    ],
    publisher: {
      name: "王同学",
      college: "计算机学院",
      campus: "东校区",
      credit: 4.8,
      note: "近三个月成交 12 单，回复及时。",
    },
  },
  2: {
    title: "iPad 9 64G + 原装笔",
    price: 1200,
    category: "数码电子",
    purchaseDate: "2024-03-12",
    usageDuration: "12 个月",
    location: "本部",
    publishTime: "2小时前",
    description: "自用平板，学习记笔记为主，屏幕无划痕，电池健康良好。",
    condition: "95 新",
    accessories: "原盒、充电器、手写笔",
    detailNote: "可现场检查序列号与功能。",
    photos: [
      createMockPhoto("iPad-图1", "#a5b4fc"),
      createMockPhoto("iPad-图2", "#bfdbfe"),
      createMockPhoto("iPad-图3", "#c4b5fd"),
    ],
    publisher: {
      name: "李同学",
      college: "信息工程学院",
      campus: "本部",
      credit: 4.9,
      note: "认证用户，历史评价良好。",
    },
  },
  201: {
    title: "自用台灯（护眼款）",
    price: 49,
    category: "生活用品",
    purchaseDate: "2025-02-20",
    usageDuration: "6 个月",
    location: "南校区",
    publishTime: "今天",
    description: "支持三档亮度调节，宿舍与自习室都适用。",
    condition: "9 成新",
    accessories: "原装电源线",
    detailNote: "面交优先，价格可小刀。",
    photos: [
      createMockPhoto("台灯-图1", "#dbeafe"),
      createMockPhoto("台灯-图2", "#bfdbfe"),
      createMockPhoto("台灯-图3", "#c7d2fe"),
    ],
    publisher: {
      name: "陈同学",
      college: "外语学院",
      campus: "南校区",
      credit: 4.7,
      note: "近期在线时间稳定，可快速回复。",
    },
  },
  202: {
    title: "英语六级备考资料",
    price: 20,
    category: "学习教材 / 书籍",
    purchaseDate: "2025-06-10",
    usageDuration: "1 学期",
    location: "本部",
    publishTime: "昨天",
    description: "包含真题、词汇与写作模板，资料齐全。",
    condition: "9 成新",
    accessories: "真题册、词汇书、笔记复印件",
    detailNote: "适合冲刺备考阶段同学。",
    photos: [
      createMockPhoto("六级-图1", "#bfdbfe"),
      createMockPhoto("六级-图2", "#c7d2fe"),
      createMockPhoto("六级-图3", "#dbeafe"),
    ],
    publisher: {
      name: "赵同学",
      college: "外语学院",
      campus: "本部",
      credit: 4.7,
      note: "可分享备考经验。",
    },
  },
  203: {
    title: "二手羽毛球拍",
    price: 68,
    category: "运动出行",
    purchaseDate: "2024-11-02",
    usageDuration: "10 个月",
    location: "东校区",
    publishTime: "2天前",
    description: "拍线刚换，手感轻快，适合日常练习。",
    condition: "85 新",
    accessories: "拍套",
    detailNote: "支持当面试打。",
    photos: [createMockPhoto("球拍-图1", "#93c5fd")],
    publisher: {
      name: "刘同学",
      college: "体育学院",
      campus: "东校区",
      credit: 4.6,
      note: "周末可约球场面交。",
    },
  },
  204: {
    title: "宿舍收纳柜",
    price: 35,
    category: "生活用品",
    purchaseDate: "2025-03-20",
    usageDuration: "5 个月",
    location: "本部",
    publishTime: "1周前",
    description: "容量大，可拆装，搬宿舍方便。",
    condition: "9 成新",
    accessories: "原装螺丝件",
    detailNote: "建议自提。",
    photos: [
      createMockPhoto("收纳柜-图1", "#bfdbfe"),
      createMockPhoto("收纳柜-图2", "#dbeafe"),
      createMockPhoto("收纳柜-图3", "#c7d2fe"),
    ],
    publisher: {
      name: "周同学",
      college: "经管学院",
      campus: "本部",
      credit: 4.8,
      note: "可当天完成交易。",
    },
  },
  205: {
    title: "罗技机械键盘 K845",
    price: 188,
    category: "数码电子",
    purchaseDate: "2024-09-12",
    usageDuration: "1 年",
    location: "图书馆南门",
    publishTime: "今天",
    description: "按键回弹好，灯效正常，办公学习两用。",
    condition: "9 成新",
    accessories: "原包装盒",
    detailNote: "可现场测试按键。",
    photos: [
      createMockPhoto("键盘-图1", "#a5b4fc"),
      createMockPhoto("键盘-图2", "#bfdbfe"),
      createMockPhoto("键盘-图3", "#c4b5fd"),
    ],
    publisher: {
      name: "陈同学",
      college: "信息学院",
      campus: "本部",
      credit: 4.9,
      note: "回复及时，支持面交。",
    },
  },
  206: {
    title: "高等数学教材（同济版）",
    price: 26,
    category: "学习教材 / 书籍",
    purchaseDate: "2025-02-18",
    usageDuration: "1 学期",
    location: "二食堂门口",
    publishTime: "明天约见",
    description: "书本干净整洁，适合低年级课程使用。",
    condition: "9 成新",
    accessories: "配套习题册",
    detailNote: "可议价。",
    photos: [
      createMockPhoto("教材-图1", "#bfdbfe"),
      createMockPhoto("教材-图2", "#93c5fd"),
      createMockPhoto("教材-图3", "#dbeafe"),
    ],
    publisher: {
      name: "赵同学",
      college: "外语学院",
      campus: "本部",
      credit: 4.7,
      note: "可配合课余时间面交。",
    },
  },
};

const fallbackItem = {
  title: "校园闲置物品",
  price: 99,
  category: "其他闲置",
  purchaseDate: "2025-10-01",
  usageDuration: "8 个月",
  location: "本部",
  publishTime: "刚刚",
  description: "当前为前端演示详情页，后端暂未接入。",
  condition: "9 成新",
  accessories: "以实际交易信息为准",
  detailNote: "支持当面交易，注意安全。",
  photos: [
    createMockPhoto("闲置-图1", "#bfdbfe"),
    createMockPhoto("闲置-图2", "#93c5fd"),
    createMockPhoto("闲置-图3", "#c7d2fe"),
  ],
  publisher: {
    name: "校园用户",
    college: "未知学院",
    campus: "本部",
    credit: 4.6,
    note: "仅前端演示数据。",
  },
};

const item = computed(() => {
  const id = Number(route.params.id);
  return itemMap[id] || fallbackItem;
});
const hasMultiplePhotos = computed(() => item.value.photos.length > 1);
const originPrice = computed(() =>
  Math.max(item.value.price + 40, item.value.price * 2)
);

const activePhoto = ref(item.value.photos[0]);
const isFavorite = ref(false);

watch(
  () => route.params.id,
  () => {
    activePhoto.value = item.value.photos[0];
    isFavorite.value = false;
  }
);

const applyTrade = () => {
  ElMessage.success("已发起交易申请（仅前端演示）");
};

const startChat = () => {
  const resolved = router.resolve({
    path: "/chat",
    query: {
      itemTitle: item.value.title,
      sellerName: item.value.publisher.name,
    },
  });
  window.open(resolved.href, "_blank");
};

const toggleFavorite = () => {
  isFavorite.value = !isFavorite.value;
  ElMessage.success(isFavorite.value ? "已收藏" : "已取消收藏");
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
