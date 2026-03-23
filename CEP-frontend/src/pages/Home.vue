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
            v-for="hot in dynamicHotKeywords"
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
        <button
          class="ghost-btn profile-btn"
          @click="goToProfile"
          aria-label="进入个人主页"
        >
          <el-icon><UserFilled /></el-icon>
        </button>
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
        <section class="block block--featured">
          <div class="block__header">
            <div>
              <h3 class="section-title">{{ blockTitle }}</h3>
              <p v-if="blockDesc" class="block__desc">{{ blockDesc }}</p>
            </div>
            <div v-if="isHotMode" class="block__tabs">
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

          <div v-if="activeCategory" class="display-words">
            <span class="display-words__label">显示词：</span>
            <button
              v-for="word in activeCategory.tags"
              :key="word"
              class="tag-btn"
              @click="quickSearch(word)"
            >
              {{ word }}
            </button>
          </div>

          <div :class="['card-grid', isHotMode ? 'card-grid--featured' : '']">
            <article
              v-for="item in displayedItems"
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

          <div v-if="displayedItems.length === 0" class="empty-state">
            暂无匹配物品，请尝试其他关键词或分类。
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

    <div v-if="isAuthModalVisible" class="login-modal-mask">
      <section class="login-modal" @click.stop>
        <div class="login-modal__header">
          <div>
            <h3 class="login-modal__title">
              {{ authModalType === "login" ? "用户登录" : "用户注册" }}
            </h3>
            <p class="login-modal__subtitle">
              {{
                authModalType === "login"
                  ? "欢迎回到校园易物"
                  : authModalType === "register-verify"
                  ? "请先填写邮箱与验证码"
                  : "请继续填写个人资料"
              }}
            </p>
          </div>
          <button class="login-modal__close" @click="closeAuthModal">×</button>
        </div>

        <form v-if="authModalType === 'login'" class="login-form">
          <label class="login-form__field">
            <span class="login-form__label">邮箱</span>
            <input
              v-model="loginForm.email"
              class="login-form__input"
              type="email"
              placeholder="请输入邮箱"
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

          <p v-if="loginError" class="login-form__error">{{ loginError }}</p>

          <button
            type="button"
            class="primary-btn login-form__submit"
            @click="submitLogin"
          >
            登录
          </button>

          <p class="login-form__hint">
            还未注册？
            <button
              type="button"
              class="login-form__switch"
              @click="switchToRegister"
            >
              去注册
            </button>
          </p>
        </form>

        <form
          v-else-if="authModalType === 'register-verify'"
          class="login-form"
        >
          <label class="login-form__field">
            <span class="login-form__label">邮箱</span>
            <div class="login-form__code-row">
              <input
                v-model="registerForm.email"
                class="login-form__input"
                type="email"
                placeholder="请输入邮箱"
                @input="onRegisterEmailInput"
              />
              <button
                type="button"
                class="ghost-btn login-form__code-btn"
                :disabled="isSendingCode || codeCountdown > 0"
                @click="sendRegisterCode"
              >
                {{ sendCodeButtonText }}
              </button>
            </div>
          </label>

          <label class="login-form__field">
            <span class="login-form__label">验证码</span>
            <div class="login-form__code-row">
              <input
                v-model="registerForm.code"
                class="login-form__input"
                type="text"
                placeholder="请输入6位数字验证码"
                maxlength="6"
                @input="onRegisterCodeInput"
              />
              <button
                type="button"
                class="ghost-btn login-form__code-btn"
                @click="goRegisterProfileStep"
              >
                校验验证码
              </button>
            </div>
          </label>

          <p v-if="registerError" class="login-form__error">
            {{ registerError }}
          </p>

          <p class="login-form__hint">
            已有账号？
            <button
              type="button"
              class="login-form__switch"
              @click="switchToLogin"
            >
              去登录
            </button>
          </p>
        </form>

        <form v-else class="login-form">
          <label class="login-form__field">
            <span class="login-form__label">用户名（选填）</span>
            <input
              v-model="registerForm.username"
              class="login-form__input"
              type="text"
              placeholder="请输入用户名（可留空）"
            />
          </label>

          <label class="login-form__field">
            <span class="login-form__label">密码</span>
            <input
              v-model="registerForm.password"
              class="login-form__input"
              type="password"
              placeholder="8-20位，需包含数字和字母"
            />
          </label>

          <label class="login-form__field">
            <span class="login-form__label">确认密码</span>
            <input
              v-model="registerForm.confirmPassword"
              class="login-form__input"
              type="password"
              placeholder="请再次输入密码"
            />
          </label>

          <p v-if="registerError" class="login-form__error">
            {{ registerError }}
          </p>
          <p v-if="registerSuccess" class="login-form__success">
            {{ registerSuccess }}
          </p>

          <button
            type="button"
            class="primary-btn login-form__submit"
            @click="submitRegister"
          >
            注册
          </button>

          <p class="login-form__hint">
            <button
              type="button"
              class="login-form__switch"
              @click="backToRegisterVerify"
            >
              返回上一步
            </button>
          </p>

          <p class="login-form__hint">
            已有账号？
            <button
              type="button"
              class="login-form__switch"
              @click="switchToLogin"
            >
              去登录
            </button>
          </p>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { UserFilled } from "@element-plus/icons-vue";

const AUTH_API_BASE = "/api/auth";
const router = useRouter();

const keyword = ref("");
const searchedKeyword = ref("");
const authModalType = ref("");
const isAuthModalVisible = computed(() => Boolean(authModalType.value));
const loginForm = ref({
  email: "",
  password: "",
});
const registerForm = ref({
  email: "",
  code: "",
  username: "",
  password: "",
  confirmPassword: "",
});
const loginError = ref("");
const registerError = ref("");
const registerSuccess = ref("");
const isSendingCode = ref(false);
const codeCountdown = ref(0);
let codeCountdownTimer = null;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const codePattern = /^\d{6}$/;
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/;

const sendCodeButtonText = computed(() => {
  if (isSendingCode.value) return "发送中...";
  if (codeCountdown.value > 0) return `${codeCountdown.value}s后重发`;
  return "发送验证码";
});

const hotKeywordPool = [
  "考研资料",
  "四六级",
  "平板",
  "耳机",
  "代步车",
  "自行车",
  "羽毛球拍",
  "台灯",
  "收纳盒",
  "二手手机",
];
const keywordCursor = ref(0);
let keywordTimer = null;

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
    categoryId: "book",
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
    categoryId: "digital",
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
    categoryId: "book",
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
    categoryId: "digital",
    title: "降噪无线蓝牙耳机",
    price: 80,
    campus: "本部",
    desc: "音质不错，适合通勤与自习使用。",
    time: "半小时前",
    badge: "精选",
  },
  {
    id: 5,
    tabId: "today",
    categoryId: "life",
    title: "宿舍护眼台灯（可调色温）",
    price: 28,
    campus: "东校区",
    desc: "灯光柔和，支持三档调节，自习必备。",
    time: "刚刚",
    badge: "新品",
  },
  {
    id: 6,
    tabId: "today",
    categoryId: "sport",
    title: "九成新山地自行车",
    price: 360,
    campus: "本部",
    desc: "通勤代步稳定，车况良好，可当面试骑。",
    time: "45分钟前",
    badge: "热销",
  },
  {
    id: 7,
    tabId: "digital",
    categoryId: "digital",
    title: "机械键盘青轴 87键",
    price: 99,
    campus: "南校区",
    desc: "手感清脆，带灯效，送拔键器。",
    time: "2小时前",
    badge: "精选",
  },
  {
    id: 8,
    tabId: "book",
    categoryId: "other",
    title: "吉他入门教程 + 民谣谱合集",
    price: 26,
    campus: "本部",
    desc: "包含多本教材，适合零基础上手。",
    time: "1天前",
    badge: "超值",
  },
];

const activeCategoryId = ref("");
const activeHotTabId = ref(hotTabs[0]?.id || "today");

const filteredHotItems = computed(() =>
  hotItems.filter((item) => item.tabId === activeHotTabId.value)
);

const activeCategory = computed(() =>
  categories.find((cat) => cat.id === activeCategoryId.value)
);

const isHotMode = computed(
  () => !activeCategoryId.value && !searchedKeyword.value.trim()
);

const blockTitle = computed(() => {
  if (activeCategory.value) return `${activeCategory.value.name} · 分类物品`;
  if (searchedKeyword.value.trim())
    return `搜索结果：${searchedKeyword.value.trim()}`;
  return "热门推荐";
});

const blockDesc = computed(() => {
  if (activeCategory.value) return "已按所选分类展示对应物品与显示词";
  if (searchedKeyword.value.trim()) return "已根据关键词筛选相关物品";
  return "未搜索或未选择分类时，默认展示热门推荐";
});

const dynamicHotKeywords = computed(() => {
  const size = 5;
  const start = keywordCursor.value % hotKeywordPool.length;
  return Array.from({ length: size }, (_, idx) => {
    const offset = (start + idx) % hotKeywordPool.length;
    return hotKeywordPool[offset];
  });
});

const displayedItems = computed(() => {
  let source = isHotMode.value
    ? filteredHotItems.value
    : activeCategoryId.value
    ? hotItems.filter((item) => item.categoryId === activeCategoryId.value)
    : hotItems;

  const word = searchedKeyword.value.trim().toLowerCase();
  if (!word) return source;

  source = source.filter((item) => {
    const catName =
      categories.find((cat) => cat.id === item.categoryId)?.name || "";
    return [item.title, item.desc, catName]
      .join(" ")
      .toLowerCase()
      .includes(word);
  });

  return source;
});

const handleSearch = () => {
  const value = keyword.value.trim();
  searchedKeyword.value = value;
  if (!value) return;
  // 目前先简单控制台输出，后续可对接搜索结果页
  // eslint-disable-next-line no-console
  console.log("搜索关键字：", value);
};

const quickSearch = (word) => {
  keyword.value = word;
  searchedKeyword.value = word;
  handleSearch();
};

const selectCategory = (id) => {
  activeCategoryId.value = activeCategoryId.value === id ? "" : id;
  searchedKeyword.value = "";
  keyword.value = "";
};

const openLoginModal = () => {
  authModalType.value = "login";
  loginError.value = "";
};

const openRegisterModal = () => {
  authModalType.value = "register-verify";
  registerError.value = "";
  registerSuccess.value = "";
};

const closeAuthModal = () => {
  authModalType.value = "";
};

const switchToRegister = () => {
  openRegisterModal();
};

const switchToLogin = () => {
  openLoginModal();
};

const goToProfile = () => {
  router.push("/profile");
};

const onRegisterEmailInput = () => {
  registerError.value = "";
  registerSuccess.value = "";
};

const onRegisterCodeInput = () => {
  registerError.value = "";
  registerSuccess.value = "";
};

const postJson = async (url, payload) => {
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    const body = await response.json();
    if (!response.ok || body.success === false) {
      throw new Error(body.message || "请求失败");
    }
    return body;
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error("无法连接后端服务，请确认后端已启动且前端通过 Vite 访问");
    }
    throw error;
  }
};

const startCodeCountdown = () => {
  codeCountdown.value = 60;
  if (codeCountdownTimer) {
    clearInterval(codeCountdownTimer);
  }
  codeCountdownTimer = window.setInterval(() => {
    if (codeCountdown.value <= 1) {
      clearInterval(codeCountdownTimer);
      codeCountdownTimer = null;
      codeCountdown.value = 0;
      return;
    }
    codeCountdown.value -= 1;
  }, 1000);
};

const sendRegisterCode = async () => {
  registerError.value = "";
  registerSuccess.value = "";
  const email = registerForm.value.email.trim();

  if (!emailPattern.test(email)) {
    registerError.value = "邮箱格式不正确";
    return;
  }

  if (isSendingCode.value || codeCountdown.value > 0) {
    return;
  }

  isSendingCode.value = true;
  try {
    await postJson(`${AUTH_API_BASE}/send-register-code`, { email });
    registerSuccess.value = "验证码已发送，请查收邮箱";
    startCodeCountdown();
  } catch (error) {
    registerError.value = error.message || "发送验证码失败";
  } finally {
    isSendingCode.value = false;
  }
};

const goRegisterProfileStep = async () => {
  registerError.value = "";
  registerSuccess.value = "";
  const email = registerForm.value.email.trim();
  const code = registerForm.value.code.trim();

  if (!emailPattern.test(email)) {
    registerError.value = "邮箱格式不正确";
    return;
  }

  if (!codePattern.test(code)) {
    registerError.value = "验证码格式不正确，请输入6位数字";
    return;
  }

  try {
    await postJson(`${AUTH_API_BASE}/verify-register-code`, { email, code });
    authModalType.value = "register-profile";
  } catch (error) {
    registerError.value = error.message || "验证码校验失败";
  }
};

const backToRegisterVerify = () => {
  registerError.value = "";
  registerSuccess.value = "";
  authModalType.value = "register-verify";
};

const submitLogin = async () => {
  loginError.value = "";
  const email = loginForm.value.email.trim();
  const password = loginForm.value.password;

  if (!emailPattern.test(email)) {
    loginError.value = "邮箱格式不正确";
    return;
  }

  if (!password) {
    loginError.value = "请输入密码";
    return;
  }

  try {
    await postJson(`${AUTH_API_BASE}/login`, { email, password });
    closeAuthModal();
  } catch (error) {
    loginError.value = error.message || "登录失败";
  }
};

const submitRegister = async () => {
  registerError.value = "";
  registerSuccess.value = "";
  const { email, code, username, password, confirmPassword } =
    registerForm.value;

  if (!emailPattern.test(email.trim())) {
    registerError.value = "邮箱格式不正确";
    return;
  }

  if (!codePattern.test(code.trim())) {
    registerError.value = "验证码格式不正确，请输入6位数字";
    return;
  }

  if (!passwordPattern.test(password)) {
    registerError.value = "密码需为8-20位，且同时包含字母和数字";
    return;
  }

  if (password !== confirmPassword) {
    registerError.value = "两次输入的密码不一致";
    return;
  }

  try {
    await postJson(`${AUTH_API_BASE}/register`, {
      email: email.trim(),
      code: code.trim(),
      username: username.trim(),
      password,
    });

    await postJson(`${AUTH_API_BASE}/login`, {
      email: email.trim(),
      password,
    });

    registerSuccess.value = "注册并登录成功";
    closeAuthModal();
  } catch (error) {
    registerError.value = error.message || "注册失败";
  }
};

onMounted(() => {
  keywordTimer = window.setInterval(() => {
    keywordCursor.value += 1;
  }, 2600);
});

onBeforeUnmount(() => {
  if (keywordTimer) {
    clearInterval(keywordTimer);
    keywordTimer = null;
  }
  if (codeCountdownTimer) {
    clearInterval(codeCountdownTimer);
    codeCountdownTimer = null;
  }
});
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
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 24px;
}

.search-bar {
  width: min(100%, 680px);
  max-width: 680px;
  margin: 0 auto;
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
  width: min(100%, 680px);
  margin: 6px auto 0;
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

.home-header__right .ghost-btn,
.home-header__right .primary-btn {
  white-space: nowrap;
  flex-shrink: 0;
}

.profile-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border-radius: 50%;
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

.block--featured {
  min-height: 420px;
}

.block__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.block__desc {
  margin: -2px 0 0;
  font-size: 12px;
  color: #6b7280;
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

.card-grid--featured {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.item-card {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
}

.item-card__thumb {
  width: 104px;
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

.display-words {
  display: flex;
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.display-words__label {
  font-size: 12px;
  color: #6b7280;
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

.empty-state {
  margin-top: 12px;
  padding: 24px 12px;
  border-radius: 12px;
  text-align: center;
  color: #6b7280;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
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

.login-form__submit {
  width: 100%;
  margin-top: 4px;
  padding: 11px 18px;
}

.login-form__hint {
  margin: 0;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}

.login-form__switch {
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  cursor: pointer;
}

.login-form__error {
  margin: -4px 0 0;
  font-size: 12px;
  color: #dc2626;
}

.login-form__success {
  margin: -4px 0 0;
  font-size: 12px;
  color: #16a34a;
}

.login-form__code-row {
  display: flex;
  gap: 8px;
}

.login-form__code-row .login-form__input {
  flex: 1;
}

.login-form__code-btn {
  min-width: 84px;
  padding: 0 14px;
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
