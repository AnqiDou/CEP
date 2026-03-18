<template>
  <div class="home">
    <!-- 顶部导航：平台入口 + 核心功能引导 -->
    <header class="home-header">
      <div class="home-header__left">
        <div class="logo">校园易物</div>
        <div class="tagline">校园闲置 · 安全易物</div>
      </div>
      <div class="home-header__center">
        <div class="search-bar">
          <input
            v-model="keyword"
            class="search-bar__input"
            type="text"
            placeholder="搜索闲置好物 / 关键字"
          />
          <button class="search-bar__btn" @click="handleSearch">搜索</button>
        </div>
        <div class="search-suggest">
          <span class="search-suggest__label">大家都在搜：</span>
          <button
            v-for="hot in hotKeywords"
            :key="hot"
            class="search-suggest__item"
            @click="quickSearch(hot)"
          >
            {{ hot }}
          </button>
        </div>
      </div>
      <div class="home-header__right">
        <button class="ghost-btn login-btn" @click="openLoginModal">
          登录
        </button>
        <button class="primary-btn">发布闲置</button>
        <button class="ghost-btn">我的订单</button>
      </div>
    </header>

    <main class="home-main">
      <!-- 左侧分类导航 -->
      <aside class="category-nav">
        <h3 class="section-title">分类导航</h3>
        <ul class="category-list">
          <li
            v-for="cat in categories"
            :key="cat.id"
            :class="[
              'category-item',
              activeCategoryId === cat.id ? 'category-item--active' : '',
            ]"
            @click="selectCategory(cat.id)"
          >
            <div class="category-item__name">
              {{ cat.name }}
            </div>
            <div class="category-item__desc">
              {{ cat.desc }}
            </div>
          </li>
        </ul>
      </aside>

      <!-- 中间推荐区：banner + 热门推荐 -->
      <section class="content">
        <div class="banner">
          <div class="banner__text">
            <h2>让校园闲置物品重新流动起来</h2>
            <p>支持当面交易 · 校园认证 · 信息真实可靠</p>
            <div class="banner__actions">
              <button class="primary-btn">立即发布</button>
              <button class="ghost-btn">浏览更多</button>
            </div>
          </div>
          <div class="banner__stats">
            <div class="stats-item">
              <div class="stats-item__num">{{ stats.items }}</div>
              <div class="stats-item__label">在售闲置</div>
            </div>
            <div class="stats-item">
              <div class="stats-item__num">{{ stats.users }}</div>
              <div class="stats-item__label">活跃同学</div>
            </div>
            <div class="stats-item">
              <div class="stats-item__num">{{ stats.deals }}</div>
              <div class="stats-item__label">成交订单</div>
            </div>
          </div>
        </div>

        <!-- 热门推荐区 -->
        <section class="block">
          <div class="block__header">
            <h3 class="section-title">热门推荐</h3>
            <div class="block__tabs">
              <button
                v-for="tab in hotTabs"
                :key="tab.id"
                :class="[
                  'tab-btn',
                  activeHotTabId === tab.id ? 'tab-btn--active' : '',
                ]"
                @click="activeHotTabId = tab.id"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>
          <div class="card-grid">
            <article
              v-for="item in filteredHotItems"
              :key="item.id"
              class="item-card"
            >
              <div class="item-card__thumb">
                <span class="item-card__badge" v-if="item.badge">
                  {{ item.badge }}
                </span>
              </div>
              <div class="item-card__body">
                <h4 class="item-card__title" :title="item.title">
                  {{ item.title }}
                </h4>
                <div class="item-card__meta">
                  <span class="item-card__price">￥{{ item.price }}</span>
                  <span class="item-card__origin">{{ item.campus }}</span>
                </div>
                <p class="item-card__desc">
                  {{ item.desc }}
                </p>
                <div class="item-card__footer">
                  <span class="item-card__time">{{ item.time }}</span>
                  <button class="text-btn">去看看</button>
                </div>
              </div>
            </article>
          </div>
        </section>

        <!-- 分类浏览区 -->
        <section class="block">
          <div class="block__header">
            <h3 class="section-title">分类浏览</h3>
          </div>
          <div class="category-section-grid">
            <div
              v-for="cat in categories"
              :key="cat.id"
              class="category-section"
            >
              <div class="category-section__header">
                <h4>{{ cat.name }}</h4>
                <span class="category-section__more">查看该分类 &gt;</span>
              </div>
              <div class="category-section__tags">
                <button
                  v-for="tag in cat.tags"
                  :key="tag"
                  class="tag-btn"
                  @click="quickSearch(tag)"
                >
                  {{ tag }}
                </button>
              </div>
            </div>
          </div>
        </section>
      </section>

      <!-- 右侧辅助区：公告 / 安全提示等 -->
      <aside class="side-panel">
        <section class="side-card">
          <h3 class="side-card__title">平台公告</h3>
          <ul class="side-card__list">
            <li>支持校园一卡通 / 校园邮箱认证</li>
            <li>建议当面交易，注意财物安全</li>
            <li>严禁发布违法违规、虚假信息</li>
          </ul>
        </section>

        <section class="side-card">
          <h3 class="side-card__title">安全交易小贴士</h3>
          <ul class="side-card__list side-card__list--ordered">
            <li>优先选择线下当面验货、当面交易</li>
            <li>勿轻信低价诱惑，警惕私下转账</li>
            <li>保留聊天记录与交易凭证</li>
          </ul>
        </section>
      </aside>
    </main>

    <div
      v-if="isLoginModalVisible"
      class="login-modal-mask"
      @click="closeLoginModal"
    >
      <section class="login-modal" @click.stop>
        <div class="login-modal__header">
          <div>
            <h3 class="login-modal__title">用户登录</h3>
            <p class="login-modal__subtitle">欢迎回到校园易物</p>
          </div>
          <button class="login-modal__close" @click="closeLoginModal">×</button>
        </div>

        <form class="login-form">
          <label class="login-form__field">
            <span class="login-form__label">用户名</span>
            <input
              v-model="loginForm.username"
              class="login-form__input"
              type="text"
              placeholder="请输入用户名"
            />
          </label>

          <label class="login-form__field">
            <span class="login-form__label">密码</span>
            <input
              v-model="loginForm.password"
              class="login-form__input"
              type="password"
              placeholder="请输入密码"
            />
          </label>

          <label class="login-form__agreement">
            <input v-model="loginForm.agreement" type="checkbox" />
            <span>我已阅读并同意平台服务条款与隐私政策</span>
          </label>

          <button type="button" class="primary-btn login-form__submit">
            登录
          </button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";

const keyword = ref("");
const isLoginModalVisible = ref(false);
const loginForm = ref({
  username: "",
  password: "",
  agreement: false,
});

const hotKeywords = ["考研资料", "四六级", "平板", "耳机", "代步车"];

const stats = {
  items: "1.2k+",
  users: "3.5k+",
  deals: "980+",
};

const categories = [
  {
    id: "book",
    name: "学习教材 / 书籍",
    desc: "教材、考研资料、课外读物",
    tags: ["高数教材", "英语四级", "考研政治", "专业书籍"],
  },
  {
    id: "digital",
    name: "数码电子",
    desc: "手机、电脑、平板、耳机",
    tags: ["平板", "二手手机", "无线耳机", "键盘鼠标"],
  },
  {
    id: "life",
    name: "生活用品",
    desc: "宿舍、电器、日用百货",
    tags: ["台灯", "收纳盒", "水杯", "小风扇"],
  },
  {
    id: "sport",
    name: "运动出行",
    desc: "球类、健身、代步工具",
    tags: ["篮球", "自行车", "滑板", "瑜伽垫"],
  },
  {
    id: "other",
    name: "其他闲置",
    desc: "手办、乐器、票券等",
    tags: ["手办", "吉他", "门票", "乐高"],
  },
];

const hotTabs = [
  { id: "today", label: "今日热门" },
  { id: "book", label: "爆款教材" },
  { id: "digital", label: "数码精选" },
];

const hotItems = [
  {
    id: 1,
    tabId: "today",
    title: "22级高数教材+辅导书全套",
    price: 35,
    campus: "东校区",
    desc: "九成新，无笔记，适合低年级同学使用。",
    time: "1小时前",
    badge: "热门",
  },
  {
    id: 2,
    tabId: "today",
    title: "iPad 9 64G + 原装笔",
    price: 1200,
    campus: "本部",
    desc: "用于记笔记，电池健康良好，附带原装包装盒。",
    time: "2小时前",
    badge: "急售",
  },
  {
    id: 3,
    tabId: "book",
    title: "考研政治核心考点精讲",
    price: 20,
    campus: "南校区",
    desc: "基本全新，少量划线，已顺利上岸转让。",
    time: "3小时前",
    badge: "",
  },
  {
    id: 4,
    tabId: "digital",
    title: "降噪无线蓝牙耳机",
    price: 80,
    campus: "本部",
    desc: "音质不错，适合通勤与自习使用。",
    time: "半小时前",
    badge: "精选",
  },
];

const activeCategoryId = ref(categories[0]?.id || "");
const activeHotTabId = ref(hotTabs[0]?.id || "today");

const filteredHotItems = computed(() =>
  hotItems.filter((item) => item.tabId === activeHotTabId.value)
);

const handleSearch = () => {
  const value = keyword.value.trim();
  if (!value) return;
  // 目前先简单控制台输出，后续可对接搜索结果页
  // eslint-disable-next-line no-console
  console.log("搜索关键字：", value);
};

const quickSearch = (word) => {
  keyword.value = word;
  handleSearch();
};

const selectCategory = (id) => {
  activeCategoryId.value = id;
};

const openLoginModal = () => {
  isLoginModalVisible.value = true;
};

const closeLoginModal = () => {
  isLoginModalVisible.value = false;
};
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: #f5f7fb;
  color: #1f2933;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
    sans-serif;
}

.home-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: 260px 1fr 260px;
  align-items: center;
  padding: 16px 40px;
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.06);
}

.home-header__left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #2563eb;
}

.tagline {
  font-size: 12px;
  color: #6b7280;
}

.home-header__center {
  padding: 0 24px;
}

.search-bar {
  display: flex;
  border-radius: 999px;
  background: #f3f4f6;
  padding: 3px;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.4);
}

.search-bar__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 8px 14px 8px 18px;
  font-size: 14px;
}

.search-bar__input::placeholder {
  color: #9ca3af;
}

.search-bar__btn {
  min-width: 88px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.1s ease, background 0.2s ease;
}

.search-bar__btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.32);
}

.search-suggest {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.search-suggest__label {
  color: #9ca3af;
}

.search-suggest__item {
  border: none;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  background: #e5edff;
  color: #2563eb;
  cursor: pointer;
}

.home-header__right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
}

.login-btn {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.primary-btn {
  border: none;
  border-radius: 999px;
  padding: 7px 18px;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.33);
}

.ghost-btn {
  border-radius: 999px;
  padding: 7px 16px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
}

.home-main {
  display: grid;
  grid-template-columns: 240px minmax(0, 1.6fr) 260px;
  gap: 16px;
  padding: 18px 40px 40px;
}

.category-nav {
  background: #ffffff;
  border-radius: 14px;
  padding: 14px 12px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.category-item {
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.1s ease;
}

.category-item:hover {
  background: #f3f4ff;
  transform: translateX(2px);
}

.category-item--active {
  background: #e0ebff;
}

.category-item__name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
}

.category-item__desc {
  font-size: 12px;
  color: #6b7280;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.banner {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 16px;
  padding: 18px 20px;
  border-radius: 18px;
  background: radial-gradient(circle at top left, #e0f2fe, #eff6ff);
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.22);
}

.banner__text h2 {
  font-size: 20px;
  margin-bottom: 6px;
}

.banner__text p {
  font-size: 13px;
  color: #4b5563;
}

.banner__actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.banner__stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-item {
  text-align: center;
}

.stats-item__num {
  font-size: 18px;
  font-weight: 700;
  color: #1d4ed8;
}

.stats-item__label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.block {
  background: #ffffff;
  border-radius: 16px;
  padding: 14px 16px 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.block__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.block__tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  border-radius: 999px;
  border: 1px solid transparent;
  padding: 4px 10px;
  font-size: 12px;
  background: #f3f4f6;
  color: #4b5563;
  cursor: pointer;
}

.tab-btn--active {
  background: #e0ebff;
  border-color: #2563eb;
  color: #1d4ed8;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.item-card {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
}

.item-card__thumb {
  width: 90px;
  border-radius: 10px;
  background: linear-gradient(135deg, #bfdbfe, #e5e7eb);
  position: relative;
}

.item-card__badge {
  position: absolute;
  top: 6px;
  left: 6px;
  padding: 2px 6px;
  font-size: 10px;
  border-radius: 999px;
  background: #f97316;
  color: #ffffff;
}

.item-card__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.item-card__title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.item-card__price {
  font-size: 15px;
  font-weight: 700;
  color: #ef4444;
}

.item-card__origin {
  font-size: 12px;
  color: #6b7280;
}

.item-card__desc {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
}

.item-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #9ca3af;
}

.text-btn {
  border: none;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
}

.category-section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.category-section {
  padding: 10px;
  border-radius: 10px;
  background: #f9fafb;
}

.category-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.category-section__header h4 {
  font-size: 13px;
}

.category-section__more {
  font-size: 11px;
  color: #2563eb;
}

.category-section__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-btn {
  border: none;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 11px;
  background: #e5edff;
  color: #2563eb;
  cursor: pointer;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 12px 12px 10px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.side-card__title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}

.side-card__list {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  color: #4b5563;
}

.side-card__list--ordered {
  list-style: decimal;
}

.login-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(4px);
}

.login-modal {
  width: min(100%, 420px);
  border-radius: 20px;
  padding: 22px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  box-shadow: 0 24px 60px rgba(37, 99, 235, 0.18);
  border: 1px solid rgba(191, 219, 254, 0.9);
}

.login-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.login-modal__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1d4ed8;
}

.login-modal__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.login-modal__close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #e5edff;
  color: #2563eb;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.login-form__field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-form__label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.login-form__input {
  height: 44px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  padding: 0 14px;
  background: #f8fbff;
  font-size: 14px;
  color: #1f2933;
  outline: none;
}

.login-form__input:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.16);
}

.login-form__agreement {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #4b5563;
}

.login-form__agreement input {
  margin-top: 2px;
  accent-color: #2563eb;
}

.login-form__submit {
  width: 100%;
  margin-top: 4px;
  padding: 11px 18px;
}

@media (max-width: 1024px) {
  .home-header {
    grid-template-columns: 1fr;
    row-gap: 8px;
  }

  .home-header__center {
    order: 3;
    padding: 0;
  }

  .home-main {
    grid-template-columns: minmax(0, 1fr);
  }

  .side-panel {
    order: 3;
  }

  .home-header__right {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 640px) {
  .login-modal {
    padding: 18px;
    border-radius: 16px;
  }
}
</style>
