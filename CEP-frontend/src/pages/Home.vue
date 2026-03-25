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
        <button
          v-if="!isUserLoggedIn"
          class="ghost-btn login-btn"
          @click="openLoginModal"
        >
          登录
        </button>
        <button class="primary-btn" @click="goToPublish">发布闲置</button>
        <button class="ghost-btn message-btn" @click="goToChat">
          <el-icon><ChatDotRound /></el-icon>
          <span>消息</span>
        </button>
        <button
          v-if="isUserLoggedIn"
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

      <!-- 中间推荐区：热门推荐 -->
      <section class="content">
        <!-- 热门推荐区 -->
        <section class="block block--featured">
          <div class="block__header">
            <div>
              <h3 class="section-title">{{ blockTitle }}</h3>
              <p v-if="blockDesc" class="block__desc">{{ blockDesc }}</p>
            </div>
            <div v-if="!isHotMode" class="block__tabs">
              <button
                v-for="option in sortOptions"
                :key="option.id"
                :class="[
                  'tab-btn',
                  isSortActive(option) ? 'tab-btn--active' : '',
                ]"
                @click="applySort(option)"
              >
                {{ option.label }}
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
              @click="goToItemDetail(item.id)"
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
                </div>
              </div>
            </article>
          </div>

          <div v-if="homeError" class="empty-state">{{ homeError }}</div>

          <div
            v-if="!homeError && displayedItems.length === 0"
            class="empty-state"
          >
            暂无匹配物品，请尝试其他关键词或分类。
          </div>
        </section>
      </section>
    </main>

    <div v-if="isAuthModalVisible" class="login-modal-mask">
      <section class="login-modal" @click.stop>
        <div class="login-modal__header">
          <div>
            <h3 class="login-modal__title">{{ authModalTitle }}</h3>
            <p class="login-modal__subtitle">{{ authModalSubtitle }}</p>
          </div>
          <button class="login-modal__close" @click="closeAuthModal">×</button>
        </div>

        <form
          v-if="authModalType === 'login'"
          class="login-form"
          method="post"
          autocomplete="on"
          @submit.prevent="submitLogin"
        >
          <label class="login-form__field">
            <span class="login-form__label">邮箱</span>
            <input
              v-model="loginForm.email"
              class="login-form__input"
              name="username"
              type="email"
              placeholder="请输入邮箱"
              autocomplete="username"
            />
          </label>

          <label class="login-form__field">
            <span class="login-form__label">密码</span>
            <input
              v-model="loginForm.password"
              class="login-form__input"
              name="password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </label>

          <p v-if="loginError" class="login-form__error">{{ loginError }}</p>
          <p v-if="loginSuccess" class="login-form__success">
            {{ loginSuccess }}
          </p>

          <label class="agreement-check">
            <input v-model="loginAgree" type="checkbox" />
            <span>
              我已阅读并同意
              <a href="/terms" target="_blank" rel="noopener noreferrer"
                >《用户条款》</a
              >
              与
              <a href="/privacy" target="_blank" rel="noopener noreferrer"
                >《隐私协议》</a
              >
            </span>
          </label>

          <button type="submit" class="primary-btn login-form__submit">
            登录
          </button>

          <p class="login-form__hint">
            <button
              type="button"
              class="login-form__switch"
              @click="switchToForgotPassword"
            >
              忘记密码
            </button>
          </p>

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

        <form
          v-else-if="authModalType === 'register-profile'"
          class="login-form"
        >
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

          <label class="agreement-check">
            <input v-model="registerAgree" type="checkbox" />
            <span>
              我已阅读并同意
              <a href="/terms" target="_blank" rel="noopener noreferrer"
                >《用户条款》</a
              >
              与
              <a href="/privacy" target="_blank" rel="noopener noreferrer"
                >《隐私协议》</a
              >
            </span>
          </label>

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

        <form v-else-if="authModalType === 'forgot-verify'" class="login-form">
          <label class="login-form__field">
            <span class="login-form__label">邮箱</span>
            <div class="login-form__code-row">
              <input
                v-model="forgotForm.email"
                class="login-form__input"
                type="email"
                placeholder="请输入已注册邮箱"
                @input="onForgotEmailInput"
              />
              <button
                type="button"
                class="ghost-btn login-form__code-btn"
                :disabled="isSendingForgotCode || forgotCodeCountdown > 0"
                @click="sendForgotCode"
              >
                {{ sendForgotCodeButtonText }}
              </button>
            </div>
          </label>

          <label class="login-form__field">
            <span class="login-form__label">验证码</span>
            <div class="login-form__code-row">
              <input
                v-model="forgotForm.code"
                class="login-form__input"
                type="text"
                placeholder="请输入6位数字验证码"
                maxlength="6"
                @input="onForgotCodeInput"
              />
              <button
                type="button"
                class="ghost-btn login-form__code-btn"
                @click="goForgotResetStep"
              >
                校验验证码
              </button>
            </div>
          </label>

          <p v-if="forgotError" class="login-form__error">{{ forgotError }}</p>
          <p v-if="forgotSuccess" class="login-form__success">
            {{ forgotSuccess }}
          </p>

          <p class="login-form__hint">
            已想起密码？
            <button
              type="button"
              class="login-form__switch"
              @click="switchToLogin"
            >
              返回登录
            </button>
          </p>
        </form>

        <form v-else-if="authModalType === 'forgot-reset'" class="login-form">
          <label class="login-form__field">
            <span class="login-form__label">新密码</span>
            <input
              v-model="forgotForm.password"
              class="login-form__input"
              type="password"
              placeholder="8-20位，需包含数字和字母"
            />
          </label>

          <label class="login-form__field">
            <span class="login-form__label">确认新密码</span>
            <input
              v-model="forgotForm.confirmPassword"
              class="login-form__input"
              type="password"
              placeholder="请再次输入新密码"
            />
          </label>

          <p v-if="forgotError" class="login-form__error">{{ forgotError }}</p>
          <p v-if="forgotSuccess" class="login-form__success">
            {{ forgotSuccess }}
          </p>

          <button
            type="button"
            class="primary-btn login-form__submit"
            @click="submitResetPassword"
          >
            重置密码
          </button>

          <p class="login-form__hint">
            <button
              type="button"
              class="login-form__switch"
              @click="backToForgotVerify"
            >
              返回上一步
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
import { ElMessage } from "element-plus";
import { ChatDotRound, UserFilled } from "@element-plus/icons-vue";
import {
  loginUser,
  registerUser,
  resetPassword,
  sendRegisterCode as sendRegisterCodeRequest,
  sendResetPasswordCode,
  verifyRegisterCode,
  verifyResetPasswordCode,
} from "../service/login/loginApiService";
import {
  authState,
  initAuthSession,
  saveAuthSession,
} from "../service/common/authSessionService";
import {
  fetchHomeCategories,
  fetchHomeItems,
  fetchHotItems,
  fetchHotKeywords,
} from "../service/home/homeApiService";

const router = useRouter();
const keyword = ref("");
const searchedKeyword = ref("");
const authModalType = ref("");
const isAuthModalVisible = computed(() => Boolean(authModalType.value));
const isUserLoggedIn = computed(() =>
  Boolean(authState.user && authState.refreshToken)
);
const loginForm = ref({
  email: "",
  password: "",
});
const loginAgree = ref(false);
const forgotForm = ref({
  email: "",
  code: "",
  password: "",
  confirmPassword: "",
});
const registerForm = ref({
  email: "",
  code: "",
  username: "",
  password: "",
  confirmPassword: "",
});
const registerAgree = ref(false);
const loginError = ref("");
const loginSuccess = ref("");
const registerError = ref("");
const registerSuccess = ref("");
const forgotError = ref("");
const forgotSuccess = ref("");
const isSendingCode = ref(false);
const codeCountdown = ref(0);
let codeCountdownTimer = null;
const isSendingForgotCode = ref(false);
const forgotCodeCountdown = ref(0);
let forgotCodeCountdownTimer = null;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const codePattern = /^\d{6}$/;
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/;

const sendCodeButtonText = computed(() => {
  if (isSendingCode.value) return "发送中...";
  if (codeCountdown.value > 0) return `${codeCountdown.value}s后重发`;
  return "发送验证码";
});

const sendForgotCodeButtonText = computed(() => {
  if (isSendingForgotCode.value) return "发送中...";
  if (forgotCodeCountdown.value > 0)
    return `${forgotCodeCountdown.value}s后重发`;
  return "发送验证码";
});

const authModalTitle = computed(() => {
  if (authModalType.value === "login") return "用户登录";
  if (authModalType.value === "register-verify") return "用户注册";
  if (authModalType.value === "register-profile") return "完善注册信息";
  if (authModalType.value === "forgot-verify") return "找回密码";
  if (authModalType.value === "forgot-reset") return "重置密码";
  return "账号中心";
});

const authModalSubtitle = computed(() => {
  if (authModalType.value === "login") return "欢迎回到校园易物";
  if (authModalType.value === "register-verify") return "请先填写邮箱与验证码";
  if (authModalType.value === "register-profile") return "请继续填写个人资料";
  if (authModalType.value === "forgot-verify") return "请输入邮箱并校验验证码";
  if (authModalType.value === "forgot-reset") return "请设置新的登录密码";
  return "";
});

const categories = ref([]);
const hotItems = ref([]);
const hotKeywords = ref([]);
const listItems = ref([]);
const homeError = ref("");
const activeCategoryId = ref(null);
const sortBy = ref("time");
const sortOrder = ref("desc");

const sortOptions = [
  { id: "time-desc", sortBy: "time", sortOrder: "desc", label: "最新发布" },
  { id: "price-asc", sortBy: "price", sortOrder: "asc", label: "价格从低到高" },
  {
    id: "price-desc",
    sortBy: "price",
    sortOrder: "desc",
    label: "价格从高到低",
  },
];

const activeCategory = computed(() =>
  categories.value.find((cat) => cat.id === activeCategoryId.value)
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
  return hotKeywords.value.map((keyword) => keyword.keyword);
});

const displayedItems = computed(() => {
  return isHotMode.value ? hotItems.value : listItems.value;
});

const formatRelativeTime = (createdAt) => {
  const parsed = new Date(createdAt);
  const timestamp = parsed.getTime();
  if (Number.isNaN(timestamp)) {
    return "刚刚";
  }
  const diffMinutes = Math.max(0, Math.floor((Date.now() - timestamp) / 60000));
  if (diffMinutes < 1) return "刚刚";
  if (diffMinutes < 60) return `${diffMinutes}分钟前`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}小时前`;
  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays}天前`;
};

const mapHomeItem = (item) => ({
  id: item.id,
  categoryId: item.categoryId,
  title: item.title,
  price: item.price,
  campus: item.campus,
  desc: item.description,
  time: formatRelativeTime(item.createdAt),
  badge: item.badge,
});

const loadCategories = async () => {
  const responseBody = await fetchHomeCategories();
  categories.value = responseBody.data.map((item) => ({
    id: item.id,
    name: item.name,
    desc: item.description,
    tags: item.tags,
  }));
};

const loadHotKeywords = async () => {
  const responseBody = await fetchHotKeywords(8);
  hotKeywords.value = responseBody.data || [];
};

const loadHotItems = async () => {
  const responseBody = await fetchHotItems(8);
  hotItems.value = (responseBody.data || []).map(mapHomeItem);
};

const loadListItems = async () => {
  const responseBody = await fetchHomeItems({
    keyword: searchedKeyword.value.trim(),
    categoryId: activeCategoryId.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value,
    page: 1,
    size: 12,
  });
  listItems.value = (responseBody.data?.items || []).map(mapHomeItem);
};

const handleSearch = async () => {
  const value = keyword.value.trim();
  searchedKeyword.value = value;
  homeError.value = "";
  if (!value && !activeCategoryId.value) {
    return;
  }
  try {
    await loadListItems();
  } catch (error) {
    homeError.value = error.message || "获取物品列表失败";
  }
};

const quickSearch = async (word) => {
  keyword.value = word;
  searchedKeyword.value = word;
  await handleSearch();
};

const selectCategory = async (id) => {
  activeCategoryId.value = activeCategoryId.value === id ? null : id;
  searchedKeyword.value = "";
  keyword.value = "";
  homeError.value = "";
  if (!activeCategoryId.value) {
    return;
  }
  try {
    await loadListItems();
  } catch (error) {
    homeError.value = error.message || "获取物品列表失败";
  }
};

const isSortActive = (option) =>
  sortBy.value === option.sortBy && sortOrder.value === option.sortOrder;

const applySort = async (option) => {
  sortBy.value = option.sortBy;
  sortOrder.value = option.sortOrder;
  if (isHotMode.value) {
    return;
  }
  homeError.value = "";
  try {
    await loadListItems();
  } catch (error) {
    homeError.value = error.message || "获取物品列表失败";
  }
};

const openLoginModal = () => {
  authModalType.value = "login";
  loginError.value = "";
  loginSuccess.value = "";
};

const openRegisterModal = () => {
  authModalType.value = "register-verify";
  registerError.value = "";
  registerSuccess.value = "";
  registerAgree.value = false;
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

const switchToForgotPassword = () => {
  authModalType.value = "forgot-verify";
  forgotError.value = "";
  forgotSuccess.value = "";
};

const goToProfile = () => {
  if (!isUserLoggedIn.value) {
    openLoginModal();
    return;
  }
  router.push("/profile");
};

const goToPublish = () => {
  router.push("/publish");
};

const goToChat = () => {
  if (!isUserLoggedIn.value) {
    openLoginModal();
    return;
  }
  const resolved = router.resolve("/chat");
  window.open(resolved.href, "_blank");
};

const goToItemDetail = (id) => {
  const resolved = router.resolve(`/item/${id}`);
  window.open(resolved.href, "_blank");
};

const onRegisterEmailInput = () => {
  registerError.value = "";
  registerSuccess.value = "";
};

const onRegisterCodeInput = () => {
  registerError.value = "";
  registerSuccess.value = "";
};

const onForgotEmailInput = () => {
  forgotError.value = "";
  forgotSuccess.value = "";
};

const onForgotCodeInput = () => {
  forgotError.value = "";
  forgotSuccess.value = "";
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

const startForgotCodeCountdown = () => {
  forgotCodeCountdown.value = 60;
  if (forgotCodeCountdownTimer) {
    clearInterval(forgotCodeCountdownTimer);
  }
  forgotCodeCountdownTimer = window.setInterval(() => {
    if (forgotCodeCountdown.value <= 1) {
      clearInterval(forgotCodeCountdownTimer);
      forgotCodeCountdownTimer = null;
      forgotCodeCountdown.value = 0;
      return;
    }
    forgotCodeCountdown.value -= 1;
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
    await sendRegisterCodeRequest(email);
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
    await verifyRegisterCode(email, code);
    authModalType.value = "register-profile";
  } catch (error) {
    registerError.value = error.message || "验证码校验失败";
  }
};

const sendForgotCode = async () => {
  forgotError.value = "";
  forgotSuccess.value = "";
  const email = forgotForm.value.email.trim();

  if (!emailPattern.test(email)) {
    forgotError.value = "邮箱格式不正确";
    return;
  }

  if (isSendingForgotCode.value || forgotCodeCountdown.value > 0) {
    return;
  }

  isSendingForgotCode.value = true;
  try {
    await sendResetPasswordCode(email);
    forgotSuccess.value = "验证码已发送，请查收邮箱";
    startForgotCodeCountdown();
  } catch (error) {
    forgotError.value = error.message || "发送验证码失败";
  } finally {
    isSendingForgotCode.value = false;
  }
};

const goForgotResetStep = async () => {
  forgotError.value = "";
  forgotSuccess.value = "";
  const email = forgotForm.value.email.trim();
  const code = forgotForm.value.code.trim();

  if (!emailPattern.test(email)) {
    forgotError.value = "邮箱格式不正确";
    return;
  }

  if (!codePattern.test(code)) {
    forgotError.value = "验证码格式不正确，请输入6位数字";
    return;
  }

  try {
    await verifyResetPasswordCode(email, code);
    authModalType.value = "forgot-reset";
  } catch (error) {
    forgotError.value = error.message || "验证码校验失败";
  }
};

const backToForgotVerify = () => {
  forgotError.value = "";
  forgotSuccess.value = "";
  authModalType.value = "forgot-verify";
};

const backToRegisterVerify = () => {
  registerError.value = "";
  registerSuccess.value = "";
  authModalType.value = "register-verify";
};

const submitLogin = async () => {
  loginError.value = "";
  loginSuccess.value = "";
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

  if (!loginAgree.value) {
    loginError.value = "请先同意用户条款与隐私协议";
    return;
  }

  try {
    const responseBody = await loginUser(email, password);
    saveAuthSession(responseBody);
    ElMessage.success("登录成功");
    closeAuthModal();
  } catch (error) {
    loginError.value = error.message || "登录失败";
  }
};

const submitResetPassword = async () => {
  forgotError.value = "";
  forgotSuccess.value = "";
  const { email, code, password, confirmPassword } = forgotForm.value;

  if (!emailPattern.test(email.trim())) {
    forgotError.value = "邮箱格式不正确";
    return;
  }

  if (!codePattern.test(code.trim())) {
    forgotError.value = "验证码格式不正确，请输入6位数字";
    return;
  }

  if (!passwordPattern.test(password)) {
    forgotError.value = "密码需为8-20位，且同时包含字母和数字";
    return;
  }

  if (password !== confirmPassword) {
    forgotError.value = "两次输入的密码不一致";
    return;
  }

  try {
    await resetPassword(email.trim(), code.trim(), password);
    loginForm.value.email = email.trim();
    loginForm.value.password = "";
    openLoginModal();
    loginSuccess.value = "密码重置成功，请使用新密码登录";
  } catch (error) {
    forgotError.value = error.message || "重置密码失败";
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

  if (!registerAgree.value) {
    registerError.value = "请先同意用户条款与隐私协议";
    return;
  }

  try {
    await registerUser({
      email: email.trim(),
      code: code.trim(),
      username: username.trim(),
      password,
    });

    const loginResponse = await loginUser(email.trim(), password);
    saveAuthSession(loginResponse);

    registerSuccess.value = "注册并登录成功";
    closeAuthModal();
  } catch (error) {
    registerError.value = error.message || "注册失败";
  }
};

onMounted(async () => {
  await initAuthSession();
  homeError.value = "";
  try {
    await Promise.all([loadCategories(), loadHotItems(), loadHotKeywords()]);
  } catch (error) {
    homeError.value = error.message || "首页数据加载失败";
    ElMessage.error(homeError.value);
  }
});

onBeforeUnmount(() => {
  if (codeCountdownTimer) {
    clearInterval(codeCountdownTimer);
    codeCountdownTimer = null;
  }
  if (forgotCodeCountdownTimer) {
    clearInterval(forgotCodeCountdownTimer);
    forgotCodeCountdownTimer = null;
  }
});
</script>

<style scoped>
.home {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

.message-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-color: #bfdbfe;
  color: #1d4ed8;
  background: #eff6ff;
}

.message-btn :deep(svg) {
  font-size: 15px;
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
  flex: 1;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  align-items: stretch;
  gap: 16px;
  min-height: 0;
  overflow: hidden;
  padding: 16px 40px 12px;
}

.category-nav {
  height: 100%;
  overflow: auto;
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
  min-height: 100%;
  overflow: hidden;
  gap: 16px;
}

.block {
  background: #ffffff;
  border-radius: 16px;
  padding: 14px 16px 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.block--featured {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
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
  align-content: start;
  gap: 10px;
  overflow-y: auto;
  min-height: 0;
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
  cursor: pointer;
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

.agreement-check {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #4b5563;
}

.agreement-check input {
  margin-top: 2px;
}

.agreement-check a {
  color: #2563eb;
  text-decoration: none;
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
