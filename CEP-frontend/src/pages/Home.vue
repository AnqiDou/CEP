<template>
  <div ref="homeScrollRef" class="home" @scroll.passive="handleHomeScroll">
    <!-- 顶部导航 -->
    <header class="home-header">
      <div class="home-header__left">
        <div class="logo">校园易物</div>
      </div>

      <div class="home-header__center">
        <div class="search-bar home-header__search">
          <input
            v-model="keyword"
            class="search-bar__input"
            type="text"
            :placeholder="searchPlaceholder"
            @keyup.enter="handleSearch"
          />
          <button class="search-bar__btn" @click="handleSearch">搜索</button>
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
        <button class="ghost-btn support-btn" @click="goToSupport">客服</button>
        <button class="primary-btn" @click="goToPublish">发布闲置</button>
        <button class="ghost-btn message-btn" @click="goToChat">
          <span
            v-if="isUserLoggedIn && unreadMessageCount > 0"
            class="message-btn__badge"
          >
            {{ unreadMessageCount > 99 ? "99+" : unreadMessageCount }}
          </span>
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

    <section v-if="visibleHomeNotice" class="home-notice" role="status">
      <div class="home-notice__content">
        <span class="home-notice__label">平台公告</span>
        <span class="home-notice__text">{{ visibleHomeNotice.content }}</span>
      </div>
      <button
        type="button"
        class="home-notice__close"
        aria-label="关闭公告"
        @click="closeHomeNotice"
      >
        ×
      </button>
    </section>

    <section v-if="!isSearchListOnlyMode" class="home-hero">
      <aside v-if="!isOpsListOnlyMode" class="home-ops">
        <div class="home-ops__layout">
          <article class="ops-card ops-card--benefit" @click="goToOpsItem(0)">
            <div class="ops-benefit__content">
              <h4>{{ opsCards.benefit.title }}</h4>
              <p>{{ opsCards.benefit.desc }}</p>
            </div>
            <div class="ops-benefit__poster" aria-hidden="true">
              <span class="ops-benefit__layer ops-benefit__layer--back"></span>
              <span class="ops-benefit__layer ops-benefit__layer--mid"></span>
              <span class="ops-benefit__layer ops-benefit__layer--front">
                <span class="ops-benefit__poster-text">福利<br />放送</span>
              </span>
            </div>
            <button
              type="button"
              class="ops-benefit__cta"
              @click.stop="goToOpsItem(0)"
            >
              去看看
            </button>
          </article>

          <div class="home-ops__season-list">
            <article
              v-for="(season, index) in opsCards.seasons"
              :key="season.id"
              class="ops-card ops-card--season"
              @click="goToOpsItem(index + 1)"
            >
              <div class="ops-card__main">
                <h4>{{ season.title }}</h4>
                <p>{{ season.desc }}</p>
              </div>
              <div class="ops-item-preview" v-if="seasonPreviewItems[index]">
                <div class="ops-item-preview__thumb">
                  <img
                    v-if="seasonPreviewItems[index].photoUrl"
                    :src="seasonPreviewItems[index].photoUrl"
                    :alt="seasonPreviewItems[index].title || '季节活动商品'"
                  />
                  <span v-else>好物</span>
                </div>
                <p class="ops-item-preview__title">
                  {{ seasonPreviewItems[index].title || "精选单品" }}
                </p>
              </div>
            </article>
          </div>
        </div>
      </aside>

      <section v-if="!isOpsListOnlyMode" class="home-hero-showcase">
        <div class="home-hero-showcase__carousel-wrap">
          <el-carousel
            class="home-hero-showcase__carousel"
            height="100%"
            trigger="click"
            :autoplay="true"
            @change="handleHeroCarouselChange"
          >
            <el-carousel-item
              v-for="(item, index) in heroCarouselItems"
              :key="item.id || index"
            >
              <div
                :class="[
                  'home-hero-showcase__slide',
                  item.detailId ? '' : 'home-hero-showcase__slide--disabled',
                ]"
                @click="goToHeroItemDetail(item)"
              >
                <img
                  v-if="item.photoUrl"
                  :src="item.photoUrl"
                  :alt="item.title || '福利商品轮播图'"
                  class="home-hero-showcase__image"
                />
                <div v-else class="home-hero-showcase__image-placeholder">
                  暂无图片
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>

        <article class="home-hero-showcase__info">
          <h4 class="home-hero-showcase__title">{{ heroCurrentItem.title }}</h4>
          <p class="home-hero-showcase__price">￥{{ heroCurrentItem.price }}</p>
          <p class="home-hero-showcase__desc">{{ heroCurrentItem.desc }}</p>
        </article>
      </section>
    </section>

    <main class="home-main">
      <nav
        v-if="!isOpsListOnlyMode && !isSearchListOnlyMode"
        class="category-tabs"
        aria-label="分类导航"
      >
        <button
          v-for="cat in categories"
          :key="cat.id"
          type="button"
          :class="[
            'category-pill',
            activeCategoryId === cat.id ? 'category-pill--active' : '',
          ]"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
      </nav>

      <section class="content">
        <section class="block block--featured">
          <header
            v-if="!isOpsListOnlyMode && !isSearchListOnlyMode"
            class="block-header"
          >
            <h3 class="section-title">🔥 {{ blockTitle }}</h3>
            <div class="sort-actions">
              <button
                v-for="option in sortOptions"
                :key="option.id"
                class="sort-btn"
                :class="isSortActive(option) ? 'sort-btn--active' : ''"
                type="button"
                @click="applySort(option)"
              >
                {{ option.label }}
              </button>
            </div>
          </header>

          <div ref="cardGridRef" class="card-grid">
            <article
              v-for="item in displayedItems"
              :key="item.id"
              class="item-card"
              @click="goToItemDetail(item.id)"
            >
              <div class="item-card__thumb">
                <img
                  v-if="item.photoUrl"
                  :src="item.photoUrl"
                  alt="物品图片"
                  class="item-card__thumb-image"
                  loading="lazy"
                />
                <span class="item-card__badge" v-if="item.badge">
                  {{ item.badge }}
                </span>
              </div>
              <div class="item-card__body">
                <h4 class="item-card__title" :title="item.title">
                  {{ item.title }}
                </h4>
                <p v-if="item.desc" class="item-card__desc" :title="item.desc">
                  {{ item.desc }}
                </p>
                <div class="item-card__price-row">
                  <span class="item-card__price">￥{{ item.price }}</span>
                </div>
                <div class="item-card__seller">
                  <el-avatar
                    :size="24"
                    :src="item.sellerAvatarUrl"
                    class="item-card__seller-avatar"
                  >
                    <el-icon><UserFilled /></el-icon>
                  </el-avatar>
                  <span class="item-card__seller-name">{{
                    item.sellerName
                  }}</span>
                  <span class="item-card__credit-level"
                    >卖家信用{{ item.sellerCredit }}</span
                  >
                </div>
              </div>
            </article>
          </div>

          <div v-if="displayedItems.length > 0" class="list-status">
            <span v-if="isLoadingMore">加载中...</span>
            <span v-else-if="!hasMoreItems">已经到底了</span>
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
        <div class="login-modal__panel">
          <div class="login-modal__header">
            <div>
              <h3 class="login-modal__title">{{ authModalTitle }}</h3>
              <p class="login-modal__subtitle">{{ authModalSubtitle }}</p>
            </div>
            <button class="login-modal__close" @click="closeAuthModal">
              ×
            </button>
          </div>

          <form
            v-if="authModalType === 'login'"
            class="login-form"
            method="post"
            :autocomplete="loginAutofillEnabled ? 'on' : 'off'"
            @submit.prevent="submitLogin"
          >
            <section class="login-form__group">
              <label class="login-form__field">
                <span class="login-form__label">邮箱</span>
                <input
                  v-model="loginForm.email"
                  class="login-form__input"
                  name="username"
                  type="email"
                  placeholder="请输入邮箱"
                  :autocomplete="loginAutofillEnabled ? 'username' : 'off'"
                  @focus="enableLoginAutofill"
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
                  :autocomplete="
                    loginAutofillEnabled ? 'current-password' : 'new-password'
                  "
                  @focus="enableLoginAutofill"
                />
              </label>
            </section>

            <section class="login-form__group login-form__group--action">
              <p v-if="loginError" class="login-form__error">
                {{ loginError }}
              </p>
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
            </section>
          </form>

          <form
            v-else-if="authModalType === 'register-verify'"
            class="login-form"
          >
            <section class="login-form__group">
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
            </section>

            <section class="login-form__group login-form__group--action">
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
            </section>
          </form>

          <form
            v-else-if="authModalType === 'register-profile'"
            class="login-form login-form--register-profile"
            autocomplete="off"
          >
            <section class="login-form__group login-form__group--register-top">
              <label
                class="login-form__field login-form__field--register-username"
              >
                <span class="login-form__label">用户名</span>
                <input
                  v-model="registerForm.username"
                  class="login-form__input"
                  type="text"
                  placeholder="请输入用户名"
                  autocomplete="off"
                />
              </label>

              <label class="login-form__field">
                <span class="login-form__label">密码</span>
                <input
                  v-model="registerForm.password"
                  class="login-form__input"
                  type="password"
                  placeholder="8-20位，需包含数字和字母"
                  autocomplete="new-password"
                />
              </label>

              <label class="login-form__field">
                <span class="login-form__label">确认密码</span>
                <input
                  v-model="registerForm.confirmPassword"
                  class="login-form__input"
                  type="password"
                  placeholder="请再次输入密码"
                  autocomplete="new-password"
                />
              </label>
            </section>

            <section
              class="login-form__group login-form__group--register-bottom"
            >
              <label class="login-form__field">
                <span class="login-form__label">联系人</span>
                <input
                  v-model="registerForm.name"
                  class="login-form__input"
                  type="text"
                  placeholder="请输入联系人"
                  autocomplete="off"
                />
              </label>

              <label class="login-form__field">
                <span class="login-form__label">联系电话</span>
                <input
                  v-model="registerForm.phone"
                  class="login-form__input"
                  type="tel"
                  placeholder="请输入联系电话"
                  autocomplete="off"
                />
              </label>

              <label class="login-form__field">
                <span class="login-form__label">收货地址</span>
                <textarea
                  v-model="registerForm.address"
                  class="login-form__input login-form__textarea"
                  rows="3"
                  placeholder="请输入收货地址"
                  autocomplete="off"
                />
              </label>
            </section>

            <section class="login-form__group login-form__group--action">
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
            </section>
          </form>

          <form
            v-else-if="authModalType === 'forgot-verify'"
            class="login-form"
          >
            <section class="login-form__group">
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
            </section>

            <section class="login-form__group login-form__group--action">
              <p v-if="forgotError" class="login-form__error">
                {{ forgotError }}
              </p>
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
            </section>
          </form>

          <form v-else-if="authModalType === 'forgot-reset'" class="login-form">
            <section class="login-form__group">
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
            </section>

            <section class="login-form__group login-form__group--action">
              <p v-if="forgotError" class="login-form__error">
                {{ forgotError }}
              </p>
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
            </section>
          </form>
        </div>
      </section>
    </div>
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
  watchEffect,
} from "vue";
import { useRoute, useRouter } from "vue-router";
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
  fetchHotKeywords,
  fetchHomeItems,
  fetchHomeNotices,
} from "../service/home/homeApiService";
import {
  fetchMessageConversations,
  fetchMessageNotificationUnreadCount,
} from "../service/chat/chatApiService";

const router = useRouter();
const route = useRoute();
const ADMIN_EMAIL = "3299166215@qq.com";
const HOME_NOTICE_CLOSED_ID_KEY = "home.notice.closed.id";
const MESSAGE_BADGE_SYNC_KEY = "cep-message-unread-sync";
const keyword = ref("");
const searchedKeyword = ref("");
const authModalType = ref("");
const isAuthModalVisible = computed(() => Boolean(authModalType.value));
const isOpsListOnlyMode = computed(() => route.query.opsView === "list");
const isSearchListOnlyMode = computed(() => route.name === "search");
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
  name: "",
  phone: "",
  address: "",
});
const registerAgree = ref(false);
const loginError = ref("");
const loginSuccess = ref("");
const registerError = ref("");
const registerSuccess = ref("");
const forgotError = ref("");
const forgotSuccess = ref("");
const loginAutofillEnabled = ref(false);
const isSendingCode = ref(false);
const codeCountdown = ref(0);
let codeCountdownTimer = null;
const isSendingForgotCode = ref(false);
const forgotCodeCountdown = ref(0);
let forgotCodeCountdownTimer = null;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const codePattern = /^\d{6}$/;
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/;
const phonePattern = /^(?:\+?86)?1[3-9]\d{9}$/;

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
const listItems = ref([]);
const homeError = ref("");
const HOT_CATEGORY_ID = "hot";
const HOT_BATCH_SIZE = 8;
const LIST_PAGE_SIZE = 12;
const OPS_BARGAIN_MAX_PRICE = 15;
const OPS_PREVIEW_FETCH_SIZE = 48;
const OPS_PREVIEW_REFRESH_INTERVAL_MS = 20000;
const VIEWER_SCOPE_ALL = "all";
const VIEWER_SCOPE_OTHERS = "others";
const VIEWER_SCOPE_SELF = "self";
const activeCategoryId = ref(HOT_CATEGORY_ID);
const sortBy = ref("price");
const sortOrder = ref("desc");
const homeAccessToken = computed(() =>
  isUserLoggedIn.value ? authState.accessToken : ""
);
const hotPage = ref(1);
const hotViewerScope = ref(VIEWER_SCOPE_ALL);
const hotHasMore = ref(true);
const listPage = ref(1);
const listViewerScope = ref(VIEWER_SCOPE_ALL);
const listHasMore = ref(true);
const isLoadingMore = ref(false);
const cardGridRef = ref(null);
const homeScrollRef = ref(null);
const hotKeywords = ref([]);
const unreadMessageCount = ref(0);
const homeNotices = ref([]);
const closedHomeNoticeId = ref("");
const opsPreviewItems = ref([]);
let opsPreviewRefreshTimer = null;
const OPS_COLUMNS = [
  {
    id: "ops-benefit",
    title: "校园抄底好物",
    keyword: "福利",
    columnCode: "campus-bargain",
  },
  {
    id: "ops-graduate",
    title: "毕业季「清仓市集」",
    keyword: "毕业",
    columnCode: "graduate-clearance",
  },
  {
    id: "ops-campus",
    title: "校园季「开学好物」",
    keyword: "开学",
    columnCode: "back-to-school",
  },
];

const GRADUATE_COLUMN_KEYWORDS = [
  "考研",
  "考公",
  "四六级",
  "复习",
  "教材",
  "资料",
  "宿舍家电",
  "小风扇",
  "风扇",
  "台灯",
  "电饭煲",
  "吹风机",
  "服饰",
  "鞋",
  "包",
  "运动",
  "器材",
  "床品",
  "衣柜",
  "书桌",
  "椅子",
  "行李箱",
];

const CAMPUS_COLUMN_KEYWORDS = [
  "宿舍",
  "床垫",
  "床帘",
  "收纳",
  "收纳箱",
  "洗漱",
  "暖壶",
  "学习",
  "笔记本",
  "文具",
  "四六级",
  "计算器",
  "军训",
  "通勤",
  "书包",
  "水杯",
  "雨伞",
  "伞",
  "数码",
  "耳机",
  "键盘",
  "鼠标",
  "电脑支架",
  "插排",
];

const activeOpsColumn = computed(() => {
  if (!isOpsListOnlyMode.value) {
    return null;
  }
  const rawIndex = Number(route.query.opsIndex);
  const safeIndex = Number.isInteger(rawIndex) ? rawIndex : 0;
  return OPS_COLUMNS[safeIndex] || OPS_COLUMNS[0];
});

const searchPlaceholder = computed(() => "搜二手闲置、书籍、电子产品...");

const ensureLoginForAction = () => {
  if (isUserLoggedIn.value) {
    return true;
  }
  triggerLoginModalWithCooldown();
  return false;
};

const consumeLoginRequiredQuery = async () => {
  if (isUserLoggedIn.value || route.query?.loginRequired !== "1") {
    return;
  }
  triggerLoginModalWithCooldown();

  const nextQuery = { ...route.query };
  delete nextQuery.loginRequired;
  delete nextQuery.reason;
  delete nextQuery.from;
  await router.replace({ path: "/", query: nextQuery });
};

const loadUnreadMessageCount = async () => {
  if (!isUserLoggedIn.value) {
    unreadMessageCount.value = 0;
    return;
  }

  try {
    const [conversationResponse, notificationResponse] = await Promise.all([
      fetchMessageConversations("all"),
      fetchMessageNotificationUnreadCount(),
    ]);
    const conversations = Array.isArray(conversationResponse?.data)
      ? conversationResponse.data
      : [];
    const chatUnreadCount = conversations.reduce((total, current) => {
      const unread = Number.isInteger(current?.unread)
        ? Math.max(current.unread, 0)
        : 0;
      return total + unread;
    }, 0);
    const notificationUnreadCount = Number.isInteger(
      notificationResponse?.data?.unread
    )
      ? Math.max(notificationResponse.data.unread, 0)
      : 0;
    unreadMessageCount.value = chatUnreadCount + notificationUnreadCount;
  } catch {
    unreadMessageCount.value = 0;
  }
};

const handleUnreadBadgeSync = (event) => {
  if (event?.key !== MESSAGE_BADGE_SYNC_KEY) {
    return;
  }
  loadUnreadMessageCount();
};

const sortOptions = [
  {
    id: "price-asc",
    sortBy: "price",
    sortOrder: "asc",
    label: "价格升序",
    icon: "↑",
  },
  {
    id: "price-desc",
    sortBy: "price",
    sortOrder: "desc",
    label: "价格降序",
    icon: "↓",
  },
];

const activeCategory = computed(() =>
  categories.value.find((cat) => cat.id === activeCategoryId.value)
);

const isBenefitOpsMode = computed(
  () =>
    isOpsListOnlyMode.value &&
    activeOpsColumn.value?.columnCode === "campus-bargain"
);

const isHotMode = computed(
  () =>
    !isSearchListOnlyMode.value &&
    !isOpsListOnlyMode.value &&
    activeCategoryId.value === HOT_CATEGORY_ID &&
    !searchedKeyword.value.trim()
);

const useHotStream = computed(
  () => isHotMode.value || isOpsListOnlyMode.value || isBenefitOpsMode.value
);

const blockTitle = computed(() => {
  if (isOpsListOnlyMode.value && activeOpsColumn.value) {
    return activeOpsColumn.value.title;
  }
  if (searchedKeyword.value.trim())
    return `搜索结果：${searchedKeyword.value.trim()}`;
  if (isHotMode.value) return "热门推荐";
  if (activeCategory.value) return activeCategory.value.name;
  return "热门推荐";
});

const blockDesc = computed(() => {
  if (searchedKeyword.value.trim()) return "已根据关键词筛选相关物品";
  return "";
});

const visibleHomeNotice = computed(() => {
  const currentNotice = homeNotices.value[0] || null;
  if (!currentNotice) {
    return null;
  }
  const currentNoticeId = String(currentNotice.id || "");
  if (currentNoticeId && currentNoticeId === closedHomeNoticeId.value) {
    return null;
  }
  return currentNotice;
});

const restoreClosedHomeNoticeId = () => {
  try {
    closedHomeNoticeId.value =
      localStorage.getItem(HOME_NOTICE_CLOSED_ID_KEY) || "";
  } catch {
    closedHomeNoticeId.value = "";
  }
};

const displayedItems = computed(() => {
  if (isSearchListOnlyMode.value) {
    return sortItemsByOwnerPriority(listItems.value);
  }

  if (isOpsListOnlyMode.value && activeOpsColumn.value) {
    const normalizeText = (value) =>
      typeof value === "string" ? value.trim().toLowerCase() : "";
    const containsAnyKeyword = (item, keywordList) => {
      const text = [item.title, item.desc, item.badge]
        .map(normalizeText)
        .filter(Boolean)
        .join(" ");
      return keywordList.some((keyword) =>
        text.includes(keyword.toLowerCase())
      );
    };

    const opsFilteredByColumn = hotItems.value.filter((item) => {
      if (activeOpsColumn.value?.columnCode === "campus-bargain") {
        const price = Number(item?.price);
        return Number.isFinite(price) && price < OPS_BARGAIN_MAX_PRICE;
      }

      if (activeOpsColumn.value?.columnCode === "graduate-clearance") {
        return containsAnyKeyword(item, GRADUATE_COLUMN_KEYWORDS);
      }

      if (activeOpsColumn.value?.columnCode === "back-to-school") {
        return containsAnyKeyword(item, CAMPUS_COLUMN_KEYWORDS);
      }

      return true;
    });

    const keywordText = searchedKeyword.value.trim().toLowerCase();
    if (!keywordText) {
      return sortItemsByOwnerPriority(opsFilteredByColumn);
    }

    return sortItemsByOwnerPriority(opsFilteredByColumn).filter((item) => {
      const priceText = Number.isFinite(Number(item?.price))
        ? String(item.price)
        : "";
      const searchable = [item.title, item.desc, item.badge, priceText]
        .map(normalizeText)
        .filter(Boolean)
        .join(" ");
      return searchable.includes(keywordText);
    });
  }

  if (isBenefitOpsMode.value) {
    return sortItemsByOwnerPriority(hotItems.value).filter((item) => {
      const price = Number(item?.price);
      return Number.isFinite(price) && price < OPS_BARGAIN_MAX_PRICE;
    });
  }
  const source = useHotStream.value ? hotItems.value : listItems.value;
  return sortItemsByOwnerPriority(source);
});

const doesItemMatchOpsColumn = (item, columnCode) => {
  const normalizeText = (value) =>
    typeof value === "string" ? value.trim().toLowerCase() : "";
  const containsAnyKeyword = (targetItem, keywordList) => {
    const text = [targetItem?.title, targetItem?.desc, targetItem?.badge]
      .map(normalizeText)
      .filter(Boolean)
      .join(" ");
    return keywordList.some((keyword) => text.includes(keyword.toLowerCase()));
  };

  if (columnCode === "campus-bargain") {
    const price = Number(item?.price);
    return Number.isFinite(price) && price < OPS_BARGAIN_MAX_PRICE;
  }

  if (columnCode === "graduate-clearance") {
    return containsAnyKeyword(item, GRADUATE_COLUMN_KEYWORDS);
  }

  if (columnCode === "back-to-school") {
    return containsAnyKeyword(item, CAMPUS_COLUMN_KEYWORDS);
  }

  return true;
};

const buildOpsCardPreviewMap = (items) => {
  const byColumn = {
    "campus-bargain": null,
    "graduate-clearance": null,
    "back-to-school": null,
  };
  const usedItemIds = new Set();

  OPS_COLUMNS.forEach((column) => {
    const matched = items.find((item) => {
      const itemId = item?.id;
      if (usedItemIds.has(itemId)) {
        return false;
      }
      return doesItemMatchOpsColumn(item, column.columnCode);
    });

    byColumn[column.columnCode] = matched || null;
    if (matched?.id !== null && matched?.id !== undefined) {
      usedItemIds.add(matched.id);
    }
  });

  return byColumn;
};

const opsCards = computed(() => ({
  benefit: {
    id: "ops-benefit",
    title: "校园抄底好物",
    desc: "「超低价捡漏・1 省到底」",
  },
  seasons: [
    {
      id: "ops-graduate",
      title: "毕业季「清仓市集」",
      desc: "集中展示毕业生商品，支持快速筛选与批量降价好货。",
    },
    {
      id: "ops-campus",
      title: "校园季「开学好物」",
      desc: "精选宿舍与学习必备，一个卡片聚焦一件高性价比单品。",
    },
  ],
}));

const opsCardPreviewItems = computed(() => {
  const previewSource = sortItemsByOwnerPriority(opsPreviewItems.value || []);
  const fallbackSource = sortItemsByOwnerPriority(hotItems.value || []);
  const primaryMap = buildOpsCardPreviewMap(previewSource);
  const fallbackMap = buildOpsCardPreviewMap(fallbackSource);

  return {
    benefit:
      primaryMap["campus-bargain"] || fallbackMap["campus-bargain"] || null,
    graduate:
      primaryMap["graduate-clearance"] ||
      fallbackMap["graduate-clearance"] ||
      null,
    campus:
      primaryMap["back-to-school"] || fallbackMap["back-to-school"] || null,
  };
});

const seasonPreviewItems = computed(() => {
  return [opsCardPreviewItems.value.graduate, opsCardPreviewItems.value.campus];
});

const heroActiveIndex = ref(0);

const heroCarouselItems = computed(() => {
  const source = hotItems.value.length ? hotItems.value : displayedItems.value;
  const validItems = source
    .filter((item) => item && (item.photoUrl || item.title || item.desc))
    .slice(0, 5)
    .map((item, index) => {
      const parsedDetailId = Number(item?.id);
      return {
        id: item.id ?? `hero-${index}`,
        detailId:
          Number.isInteger(parsedDetailId) && parsedDetailId > 0
            ? parsedDetailId
            : null,
        title: item.title || "校园精选福利",
        price: item.price ?? "0.00",
        desc: item.desc || "精选好物限时推荐，欢迎点击查看详情。",
        photoUrl: item.photoUrl || "",
      };
    });

  if (validItems.length > 0) {
    return validItems;
  }

  return [
    {
      id: "hero-fallback-1",
      detailId: null,
      title: "校园精选福利",
      price: "29.90",
      desc: "福利专区每日上新，覆盖学习与生活必备好物。",
      photoUrl: "",
    },
  ];
});

const heroCurrentItem = computed(() => {
  const list = heroCarouselItems.value;
  if (!list.length) {
    return {
      detailId: null,
      title: "校园精选福利",
      price: "0.00",
      desc: "暂无推荐商品",
      photoUrl: "",
    };
  }
  const safeIndex = Math.min(heroActiveIndex.value, list.length - 1);
  return list[safeIndex];
});

const hasMoreItems = computed(() =>
  useHotStream.value ? hotHasMore.value : listHasMore.value
);

const resolveBackendOrigin = () => {
  const customOrigin = (import.meta.env.VITE_BACKEND_ORIGIN || "").trim();
  if (customOrigin) {
    return customOrigin.replace(/\/+$/, "");
  }

  if (import.meta.env.PROD) {
    return window.location.origin;
  }

  const protocol = window.location.protocol;
  const host = window.location.hostname;
  const port = import.meta.env.VITE_BACKEND_PORT || "8080";
  return `${protocol}//${host}:${port}`;
};

const BACKEND_ORIGIN = resolveBackendOrigin();

const resolveMediaUrl = (value) => {
  if (typeof value !== "string") {
    return "";
  }

  const raw = value.trim();
  if (!raw) {
    return "";
  }

  const normalized = raw.replace(/\\/g, "/");
  if (
    /^(https?:)?\/\//i.test(normalized) ||
    normalized.startsWith("data:") ||
    normalized.startsWith("blob:")
  ) {
    return normalized;
  }

  if (normalized.startsWith("/")) {
    return `${BACKEND_ORIGIN}${normalized}`;
  }

  return `${BACKEND_ORIGIN}/${normalized.replace(/^\.\//, "")}`;
};

const resolveSellerAvatarUrl = (item) => {
  const candidates = [
    item.sellerAvatarUrl,
    item.publisherAvatarUrl,
    item.avatar,
  ];
  const target = candidates.find(
    (value) => typeof value === "string" && value.trim()
  );
  return resolveMediaUrl(typeof target === "string" ? target : "");
};

const resolveSellerName = (item) => {
  const candidates = [item.sellerName, item.publisherName, item.username];
  const target = candidates.find(
    (value) => typeof value === "string" && value.trim()
  );
  return typeof target === "string" ? target.trim() : "校园用户";
};

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
  publisherUserId: item.publisherUserId,
  isSelf: Boolean(item.isSelf),
  title: item.title,
  price: item.price,
  photoUrl: resolveMediaUrl(item.photoUrl),
  desc:
    typeof item.description === "string" && item.description.trim()
      ? item.description.trim()
      : "",
  time: formatRelativeTime(item.createdAt),
  badge:
    typeof item.badge === "string" && item.badge.trim() !== "未填写"
      ? item.badge.trim()
      : "",
  sellerAvatarUrl: resolveSellerAvatarUrl(item),
  sellerName: resolveSellerName(item),
  sellerCredit:
    typeof item.sellerCredit === "string" && item.sellerCredit.trim()
      ? item.sellerCredit.trim()
      : "良好",
});

const mergeUniqueItemsById = (existing, incoming) => {
  const seen = new Set(existing.map((item) => item.id));
  const deduped = incoming.filter((item) => !seen.has(item.id));
  return [...existing, ...deduped];
};

const sortItemsByOwnerPriority = (items) =>
  [...items].sort(
    (a, b) => Number(Boolean(a?.isSelf)) - Number(Boolean(b?.isSelf))
  );

const resolveInitialViewerScope = () =>
  isUserLoggedIn.value ? VIEWER_SCOPE_OTHERS : VIEWER_SCOPE_ALL;

const loadCategories = async () => {
  const responseBody = await fetchHomeCategories();
  const remoteCategories = responseBody.data.map((item) => ({
    id: item.id,
    name: item.name,
    desc: item.description,
    tags: item.tags,
  }));
  categories.value = [
    {
      id: HOT_CATEGORY_ID,
      name: "热门推荐",
      desc: "",
      tags: [],
    },
    ...remoteCategories,
  ];
};

const loadHotKeywords = async () => {
  try {
    const responseBody = await fetchHotKeywords(6);
    const words = (responseBody.data || [])
      .map((item) =>
        typeof item.keyword === "string" ? item.keyword.trim() : ""
      )
      .filter(Boolean)
      .slice(0, 6);
    hotKeywords.value = words.length
      ? words
      : ["耳机", "自行车", "计算器", "考研资料", "宿舍收纳"];
  } catch {
    hotKeywords.value = ["耳机", "自行车", "计算器", "考研资料", "宿舍收纳"];
  }
};

const loadHomeNotices = async () => {
  try {
    const responseBody = await fetchHomeNotices(3);
    homeNotices.value = Array.isArray(responseBody?.data)
      ? responseBody.data
      : [];
  } catch {
    homeNotices.value = [];
  }
};

const loadOpsPreviewItems = async () => {
  const responseBody = await fetchHomeItems({
    keyword: "",
    categoryId: undefined,
    viewerScope: resolveInitialViewerScope(),
    sortBy: "time",
    sortOrder: "desc",
    page: 1,
    size: OPS_PREVIEW_FETCH_SIZE,
    accessToken: homeAccessToken.value,
  });
  const incomingItems = (responseBody.data?.items || []).map(mapHomeItem);
  opsPreviewItems.value = sortItemsByOwnerPriority(incomingItems);
};

const stopOpsPreviewAutoRefresh = () => {
  if (!opsPreviewRefreshTimer) {
    return;
  }
  clearInterval(opsPreviewRefreshTimer);
  opsPreviewRefreshTimer = null;
};

const startOpsPreviewAutoRefresh = async () => {
  stopOpsPreviewAutoRefresh();

  if (isSearchListOnlyMode.value || isOpsListOnlyMode.value) {
    return;
  }

  try {
    await loadOpsPreviewItems();
  } catch {
    opsPreviewItems.value = [];
  }

  opsPreviewRefreshTimer = window.setInterval(async () => {
    if (isSearchListOnlyMode.value || isOpsListOnlyMode.value) {
      return;
    }
    try {
      await loadOpsPreviewItems();
    } catch {
      // ignore: 防止轮询打断主流程
    }
  }, OPS_PREVIEW_REFRESH_INTERVAL_MS);
};

const closeHomeNotice = () => {
  const currentNoticeId = String(visibleHomeNotice.value?.id || "");
  if (!currentNoticeId) {
    return;
  }
  closedHomeNoticeId.value = currentNoticeId;
  try {
    localStorage.setItem(HOME_NOTICE_CLOSED_ID_KEY, currentNoticeId);
  } catch {
    // ignore
  }
};

const loadHotItems = async ({ append = false } = {}) => {
  if (!append) {
    hotPage.value = 1;
    hotViewerScope.value = resolveInitialViewerScope();
    hotHasMore.value = true;
    hotItems.value = [];
  } else {
    if (!hotHasMore.value) {
      return;
    }
  }

  const nextPage = append ? hotPage.value + 1 : 1;
  const responseBody = await fetchHomeItems({
    keyword: "",
    categoryId: undefined,
    viewerScope: hotViewerScope.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value,
    page: nextPage,
    size: HOT_BATCH_SIZE,
    accessToken: homeAccessToken.value,
  });
  const incomingItems = (responseBody.data?.items || []).map(mapHomeItem);
  hotItems.value = sortItemsByOwnerPriority(
    append ? mergeUniqueItemsById(hotItems.value, incomingItems) : incomingItems
  );
  hotPage.value = nextPage;

  if (incomingItems.length < HOT_BATCH_SIZE) {
    if (hotViewerScope.value === VIEWER_SCOPE_OTHERS && isUserLoggedIn.value) {
      hotViewerScope.value = VIEWER_SCOPE_SELF;
      hotPage.value = 0;
      await loadHotItems({ append: true });
      return;
    }
    hotHasMore.value = false;
    return;
  }

  hotHasMore.value = true;
};

const loadListItems = async ({ append = false } = {}) => {
  if (!append) {
    listPage.value = 1;
    listViewerScope.value = resolveInitialViewerScope();
    listHasMore.value = true;
    listItems.value = [];
  } else if (!listHasMore.value) {
    return;
  }

  const nextPage = append ? listPage.value + 1 : 1;
  const normalizedCategoryId =
    typeof activeCategoryId.value === "number"
      ? activeCategoryId.value
      : undefined;
  const responseBody = await fetchHomeItems({
    keyword: searchedKeyword.value.trim(),
    categoryId: normalizedCategoryId,
    opsColumn: isOpsListOnlyMode.value
      ? activeOpsColumn.value?.columnCode
      : undefined,
    viewerScope: listViewerScope.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value,
    page: nextPage,
    size: LIST_PAGE_SIZE,
    accessToken: homeAccessToken.value,
  });
  const incomingItems = (responseBody.data?.items || []).map(mapHomeItem);
  listItems.value = sortItemsByOwnerPriority(
    append
      ? mergeUniqueItemsById(listItems.value, incomingItems)
      : incomingItems
  );
  listPage.value = nextPage;

  if (incomingItems.length < LIST_PAGE_SIZE) {
    if (listViewerScope.value === VIEWER_SCOPE_OTHERS && isUserLoggedIn.value) {
      listViewerScope.value = VIEWER_SCOPE_SELF;
      listPage.value = 0;
      await loadListItems({ append: true });
      return;
    }
    listHasMore.value = false;
    return;
  }

  listHasMore.value = true;
};

const scrollGridToTop = () => {
  nextTick(() => {
    homeScrollRef.value?.scrollTo({ top: 0, behavior: "auto" });
  });
};

const loadMoreItems = async () => {
  if (isLoadingMore.value || !hasMoreItems.value || homeError.value) {
    return;
  }
  isLoadingMore.value = true;
  try {
    if (useHotStream.value) {
      await loadHotItems({ append: true });
    } else {
      await loadListItems({ append: true });
    }
  } catch (error) {
    homeError.value = error.message || "加载更多失败";
  } finally {
    isLoadingMore.value = false;
  }
};

const handleHomeScroll = async () => {
  const element = homeScrollRef.value;
  if (!element) {
    return;
  }
  const reachBottom =
    element.scrollTop + element.clientHeight >= element.scrollHeight - 80;
  if (reachBottom) {
    await loadMoreItems();
  }
};

const ensureScrollableContent = async () => {
  await nextTick();
  let guard = 0;
  while (
    homeScrollRef.value &&
    homeScrollRef.value.scrollHeight <= homeScrollRef.value.clientHeight + 2 &&
    hasMoreItems.value &&
    guard < 8
  ) {
    await loadMoreItems();
    await nextTick();
    guard += 1;
  }
};

const withPreservedHomeScroll = async (loader) => {
  const element = homeScrollRef.value;
  const previousScrollTop = element ? element.scrollTop : 0;

  await loader();
  await nextTick();

  if (!element) {
    return;
  }

  const maxScrollTop = Math.max(0, element.scrollHeight - element.clientHeight);
  element.scrollTop = Math.min(previousScrollTop, maxScrollTop);
};

const handleSearch = async () => {
  if (!ensureLoginForAction()) {
    return;
  }
  const value = keyword.value.trim();

  if (isOpsListOnlyMode.value) {
    searchedKeyword.value = value;
    homeError.value = "";
    scrollGridToTop();
    await ensureScrollableContent();
    return;
  }

  if (isSearchListOnlyMode.value) {
    searchedKeyword.value = value;
    homeError.value = "";
    await router.replace({
      name: "search",
      query: {
        keyword: value,
      },
    });
    await applySearchListMode();
    return;
  }

  const resolved = router.resolve({
    name: "search",
    query: {
      keyword: value,
    },
  });
  window.open(resolved.href, "_blank");
};

const selectHotKeyword = async (word) => {
  keyword.value = word;
  await handleSearch();
};

const goToOpsItem = (index) => {
  if (!ensureLoginForAction()) {
    return;
  }
  const resolved = router.resolve({
    path: "/",
    query: {
      opsView: "list",
      opsIndex: String(index),
    },
  });
  window.open(resolved.href, "_blank");
};

const applySearchListMode = async () => {
  if (!isSearchListOnlyMode.value) {
    return;
  }

  activeCategoryId.value = HOT_CATEGORY_ID;
  keyword.value = String(route.query.keyword || "").trim();
  searchedKeyword.value = keyword.value;
  homeError.value = "";

  try {
    await Promise.all([loadCategories(), loadHotKeywords()]);
    await loadListItems({ append: false });
    scrollGridToTop();
    await ensureScrollableContent();
  } catch (error) {
    homeError.value = error.message || "获取搜索结果失败";
  }
};

const applyOpsListMode = async () => {
  if (!isOpsListOnlyMode.value) {
    return;
  }

  activeCategoryId.value = HOT_CATEGORY_ID;
  keyword.value = "";
  searchedKeyword.value = "";
  homeError.value = "";

  try {
    await loadHotItems({ append: false });
    scrollGridToTop();
    await ensureScrollableContent();
  } catch (error) {
    homeError.value = error.message || "获取活动物品列表失败";
  }
};

const handleHeroCarouselChange = (index) => {
  heroActiveIndex.value = Number.isFinite(index) ? index : 0;
};

const goToHeroItemDetail = (item) => {
  const detailId = Number(item?.detailId);
  if (!Number.isInteger(detailId) || detailId <= 0) {
    return;
  }
  goToItemDetail(detailId);
};

const selectCategory = async (id) => {
  activeCategoryId.value = id;
  searchedKeyword.value = "";
  keyword.value = "";
  homeError.value = "";
  if (activeCategoryId.value === HOT_CATEGORY_ID) {
    try {
      await withPreservedHomeScroll(async () => {
        await loadHotItems({ append: false });
        await ensureScrollableContent();
      });
    } catch (error) {
      homeError.value = error.message || "获取热门推荐失败";
    }
    return;
  }
  try {
    await withPreservedHomeScroll(async () => {
      await loadListItems({ append: false });
      await ensureScrollableContent();
    });
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
    await loadHotItems({ append: false });
    scrollGridToTop();
    await ensureScrollableContent();
    return;
  }
  homeError.value = "";
  try {
    await loadListItems({ append: false });
    scrollGridToTop();
    await ensureScrollableContent();
  } catch (error) {
    homeError.value = error.message || "获取物品列表失败";
  }
};

const openLoginModal = () => {
  authModalType.value = "login";
  loginError.value = "";
  loginSuccess.value = "";
  loginAutofillEnabled.value = false;
};

const enableLoginAutofill = () => {
  loginAutofillEnabled.value = true;
};

const LOGIN_MODAL_TRIGGER_COOLDOWN_MS = 1500;
let lastLoginModalTriggerAt = 0;

const triggerLoginModalWithCooldown = () => {
  if (authModalType.value === "login") {
    return;
  }

  const now = Date.now();
  if (now - lastLoginModalTriggerAt < LOGIN_MODAL_TRIGGER_COOLDOWN_MS) {
    return;
  }
  lastLoginModalTriggerAt = now;
  openLoginModal();
};

const openRegisterModal = () => {
  authModalType.value = "register-verify";
  registerError.value = "";
  registerSuccess.value = "";
  registerAgree.value = false;
  registerForm.value = {
    email: "",
    code: "",
    username: "",
    password: "",
    confirmPassword: "",
    name: "",
    phone: "",
    address: "",
  };
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
  if (!ensureLoginForAction()) {
    return;
  }
  const currentEmail = (authState.user?.email || "").trim().toLowerCase();
  if (currentEmail === ADMIN_EMAIL) {
    router.push("/admin");
    return;
  }
  router.push("/profile");
};

const goToPublish = () => {
  if (!ensureLoginForAction()) {
    return;
  }
  router.push("/publish");
};

const goToChat = () => {
  if (!ensureLoginForAction()) {
    return;
  }
  const resolved = router.resolve("/chat");
  window.open(resolved.href, "_blank");
};

const goToSupport = () => {
  if (!ensureLoginForAction()) {
    return;
  }
  const resolved = router.resolve("/support");
  window.open(resolved.href, "_blank");
};

const goToItemDetail = (id) => {
  if (!ensureLoginForAction()) {
    return;
  }
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
    await loadUnreadMessageCount();
    ElMessage.success("登录成功");
    closeAuthModal();
    if (email.toLowerCase() === ADMIN_EMAIL) {
      router.push("/admin");
    }
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
  const {
    email,
    code,
    username,
    password,
    confirmPassword,
    name,
    phone,
    address,
  } = registerForm.value;

  if (!emailPattern.test(email.trim())) {
    registerError.value = "邮箱格式不正确";
    return;
  }

  if (!codePattern.test(code.trim())) {
    registerError.value = "验证码格式不正确，请输入6位数字";
    return;
  }

  if (!username.trim()) {
    registerError.value = "请填写用户名";
    return;
  }

  if (!passwordPattern.test(password)) {
    registerError.value = "密码需为8-20位，且同时包含字母和数字";
    return;
  }

  if (!name.trim()) {
    registerError.value = "请填写联系人";
    return;
  }

  if (!phone.trim()) {
    registerError.value = "请填写联系电话";
    return;
  }

  const normalizedPhone = phone.trim().replace(/[\s-]/g, "");
  if (!phonePattern.test(normalizedPhone)) {
    registerError.value = "联系电话格式不正确，请输入11位手机号";
    return;
  }

  if (!address.trim()) {
    registerError.value = "请填写收货地址";
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
      name: name.trim(),
      phone: normalizedPhone.replace(/^\+?86/, ""),
      address: address.trim(),
    });

    const loginResponse = await loginUser(email.trim(), password);
    saveAuthSession(loginResponse);
    await loadUnreadMessageCount();

    registerSuccess.value = "注册并登录成功";
    closeAuthModal();
  } catch (error) {
    registerError.value = error.message || "注册失败";
  }
};

onMounted(async () => {
  window.addEventListener("storage", handleUnreadBadgeSync);
  restoreClosedHomeNoticeId();
  await initAuthSession();
  await consumeLoginRequiredQuery();
  await loadUnreadMessageCount();
  await loadHomeNotices();
  homeError.value = "";
  try {
    if (isSearchListOnlyMode.value) {
      await applySearchListMode();
    } else if (isOpsListOnlyMode.value) {
      await applyOpsListMode();
    } else {
      await Promise.all([loadCategories(), loadHotItems(), loadHotKeywords()]);
    }
    await startOpsPreviewAutoRefresh();
    await ensureScrollableContent();
  } catch (error) {
    homeError.value = error.message || "首页数据加载失败";
    ElMessage.error(homeError.value);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener("storage", handleUnreadBadgeSync);
  stopOpsPreviewAutoRefresh();
  if (codeCountdownTimer) {
    clearInterval(codeCountdownTimer);
    codeCountdownTimer = null;
  }
  if (forgotCodeCountdownTimer) {
    clearInterval(forgotCodeCountdownTimer);
    forgotCodeCountdownTimer = null;
  }
});

watchEffect(() => {
  if (isOpsListOnlyMode.value && activeOpsColumn.value) {
    document.title = `${activeOpsColumn.value.title} - 校园易物平台`;
    return;
  }
  document.title = "校园易物平台";
});

watch(
  () => [route.query.opsView, route.query.opsIndex],
  async () => {
    homeError.value = "";
    if (isSearchListOnlyMode.value) {
      return;
    }
    if (isOpsListOnlyMode.value) {
      stopOpsPreviewAutoRefresh();
      await applyOpsListMode();
      return;
    }
    await startOpsPreviewAutoRefresh();
  }
);

watch(
  () => [route.name, route.query.keyword],
  async () => {
    homeError.value = "";
    if (isSearchListOnlyMode.value) {
      stopOpsPreviewAutoRefresh();
      await applySearchListMode();
      return;
    }
    await startOpsPreviewAutoRefresh();
  }
);

watch(
  () => isUserLoggedIn.value,
  async () => {
    if (isSearchListOnlyMode.value || isOpsListOnlyMode.value) {
      return;
    }
    await startOpsPreviewAutoRefresh();
  }
);

watch(
  () => [route.query.loginRequired, route.query.reason, isUserLoggedIn.value],
  async () => {
    await consumeLoginRequiredQuery();
  }
);
</script>

<style scoped>
.home {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  background: #ffffff;
  color: #2f3150;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
    sans-serif;
}

.home-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  height: 85px;
  box-sizing: border-box;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid #efedf8;
  backdrop-filter: blur(10px);
}

.home-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 10px 28px 0;
  padding: 10px 14px;
  border: 1px solid #c4b5fd;
  border-radius: 12px;
  background: #fff;
}

.home-notice__content {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.home-notice__label {
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 700;
  color: #6d28d9;
  background: #efe7ff;
  border-radius: 999px;
  padding: 3px 10px;
}

.home-notice__text {
  min-width: 0;
  color: #2f3150;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.home-notice__close {
  border: none;
  background: transparent;
  color: #000;
  cursor: pointer;
  font-size: 24px;
  line-height: 1;
  font-weight: 400;
  padding: 0;
}

.home-header__left {
  display: flex;
  align-items: center;
  gap: 0;
}

.logo {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.5px;
  color: #8c7cf0;
}

.home-header__center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(52vw, 760px);
}

.home-header__search {
  width: 100%;
  box-shadow: 0 4px 14px rgba(140, 124, 240, 0.12);
}

.search-bar {
  display: flex;
  align-items: center;
  border-radius: 14px;
  background: #ffffff;
  padding: 4px;
  border: 1px solid rgba(201, 190, 246, 0.65);
  box-shadow: 0 12px 30px rgba(140, 124, 240, 0.16);
}

.search-bar__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 10px 14px 10px 16px;
  font-size: 15px;
  color: #433f67;
}

.search-bar__input::placeholder {
  color: #a8a1c8;
}

.search-bar__btn {
  min-width: 86px;
  height: 40px;
  border: none;
  border-radius: 12px;
  background: #8c7cf0;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.1s ease, background 0.2s ease;
}

.hot-keywords {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hot-keywords__label {
  font-size: 12px;
  color: #8b88a9;
}

.hot-keywords__item {
  border: 1px solid #e8e4fb;
  border-radius: 999px;
  background: #faf9ff;
  color: #6f67a2;
  font-size: 12px;
  padding: 2px 10px;
  cursor: pointer;
}

.search-bar__btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(140, 124, 240, 0.28);
}

.home-header__right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  padding-top: 2px;
  margin-left: auto;
}

.home-header__right .ghost-btn,
.home-header__right .primary-btn {
  height: 40px;
  white-space: nowrap;
  flex-shrink: 0;
}

.profile-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 50%;
  border-color: #e4e0f8;
  color: #716a98;
}

.login-btn {
  border-color: #e6e2f8;
  background: #f6f3ff;
  color: #6a6296;
}

.message-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  position: relative;
  border-color: #e6e2f8;
  color: #6a6296;
  background: #f6f3ff;
}

.message-btn__badge {
  position: absolute;
  top: 4px;
  right: 6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  line-height: 1;
  font-weight: 700;
  color: #fff;
  background: #ff4d4f;
  box-shadow: 0 0 0 2px #f6f3ff;
}

.message-btn :deep(svg) {
  font-size: 15px;
}

.primary-btn {
  border: none;
  border-radius: 999px;
  padding: 7px 16px;
  font-size: 14px;
  font-weight: 600;
  background: #8c7cf0;
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(140, 124, 240, 0.26);
}

.ghost-btn {
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 14px;
  border: 1px solid #e6e2f8;
  background: #ffffff;
  color: #655f8b;
  cursor: pointer;
}

.home-hero {
  --hero-showcase-height: 300px;
  margin: 0 0 14px;
  padding: 20px 28px;
  display: flex;
  align-items: stretch;
  gap: 16px;
  background: transparent;
}

.home-ops {
  flex: 0 0 450px;
  min-width: 450px;
  max-width: 450px;
  height: var(--hero-showcase-height);
  padding: 0;
  background: transparent;
  border: none;
}

.home-hero-showcase {
  flex: 1;
  min-width: 0;
  height: var(--hero-showcase-height);
  border-radius: 18px;
  border: 1px solid rgba(213, 204, 251, 0.75);
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: stretch;
  overflow: hidden;
}

.home-hero-showcase__carousel-wrap {
  width: 40%;
  height: 100%;
  min-width: 0;
  background: transparent;
}

.home-hero-showcase__carousel {
  width: 100%;
  height: 100%;
}

.home-hero-showcase__slide {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  box-sizing: border-box;
  cursor: pointer;
}

.home-hero-showcase__slide--disabled {
  cursor: default;
}

.home-hero-showcase__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.home-hero-showcase__image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8f88b8;
  font-size: 14px;
}

.home-hero-showcase__info {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 22px 20px;
  box-sizing: border-box;
}

.home-hero-showcase__title {
  margin: 0;
  font-size: 26px;
  line-height: 1.3;
  color: #3f3a68;
}

.home-hero-showcase__price {
  margin: 0;
  font-size: 34px;
  line-height: 1;
  font-weight: 700;
  color: #6d5ccf;
}

.home-hero-showcase__desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #6e6a90;
}

.home-ops__list {
  display: grid;
  gap: 10px;
}

.home-ops__layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  height: 100%;
}

.home-ops__season-list {
  display: grid;
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 10px;
  min-height: 0;
}

.ops-card {
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ece8ff;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.ops-card:hover {
  border-color: #cfc3f8;
  box-shadow: 0 10px 20px rgba(137, 122, 220, 0.12);
  transform: translateY(-1px);
}

.ops-card h4 {
  margin: 0;
  font-size: 18px;
  color: #5747ad;
}

.ops-card p {
  margin: 6px 0 0;
  font-size: 15px;
  color: #6e6a90;
  line-height: 1.5;
}

.ops-card--benefit {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  margin: 0;
  background: linear-gradient(160deg, #fcfbff 0%, #f2efff 100%);
  cursor: pointer;
}

.ops-benefit__content {
  margin-bottom: 10px;
}

.ops-benefit__poster {
  position: relative;
  width: 136px;
  height: 110px;
  margin: 2px auto 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ops-benefit__layer {
  position: absolute;
  width: 84px;
  height: 84px;
  border-radius: 4px;
  border: 2px solid #6f3fd7;
  box-shadow: 0 4px 10px rgba(86, 57, 153, 0.2);
}

.ops-benefit__layer--back {
  background: #d6bcff;
  transform: rotate(-8deg) translate(-8px, 2px);
}

.ops-benefit__layer--mid {
  background: #cbb1ff;
  transform: rotate(7deg) translate(8px, -4px);
}

.ops-benefit__layer--front {
  background: #f7efff;
  transform: rotate(-2deg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ops-benefit__poster-text {
  color: #6c36d2;
  font-size: 30px;
  line-height: 1.05;
  font-weight: 900;
  text-align: center;
  letter-spacing: 1px;
  text-shadow: -1px -1px 0 #fff, 1px -1px 0 #fff, -1px 1px 0 #fff,
    1px 1px 0 #fff, 0 3px 0 rgba(93, 45, 168, 0.32);
}

.ops-benefit__cta {
  margin: 12px auto 0;
  height: 30px;
  padding: 0 16px;
  border: 1px solid #bda1ff;
  border-radius: 999px;
  background: #fff;
  color: #6b42d8;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.ops-benefit__cta:hover {
  background: #f7f2ff;
  border-color: #a684ff;
}

.ops-benefit__content h4 {
  margin-top: 0;
}

.ops-benefit__content p {
  margin-top: 6px;
}

.ops-card--season {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 8px;
  min-height: 0;
  overflow: hidden;
}

.ops-card__main {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.ops-card__main h4 {
  margin: 0;
  font-size: 18px;
  line-height: 1.15;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}

.ops-card__main p {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  overflow: hidden;
}

.ops-item-preview {
  flex: 0 0 76px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
}

.ops-item-preview__thumb {
  width: 76px;
  height: 54px;
  border-radius: 8px;
  border: 1px solid #ebe6ff;
  background: #f7f4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.ops-item-preview__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ops-item-preview__thumb span {
  font-size: 12px;
  color: #7b72aa;
}

.ops-item-preview__title {
  margin: 0;
  max-width: 76px;
  font-size: 11px;
  line-height: 1.25;
  color: #7a729e;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.home-main {
  flex: 1;
  min-height: 0;
  overflow: visible;
  padding: 0 28px 20px;
}

.content {
  display: flex;
  flex-direction: column;
}

.category-tabs {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 9px;
  flex-wrap: wrap;
  padding: 0 0 14px;
}

.category-pill {
  border: 1px solid rgba(163, 149, 233, 0.32);
  border-radius: 999px;
  padding: 7px 15px;
  font-size: 14px;
  color: #675f92;
  background: rgba(255, 255, 255, 0.62);
  cursor: pointer;
  transition: all 0.15s ease;
}

.category-pill:hover {
  background: rgba(255, 255, 255, 0.84);
}

.category-pill--active {
  background: linear-gradient(135deg, #ffffff 0%, #f8f4ff 100%);
  border-color: #d4c8f8;
  color: #6b5ac9;
  font-weight: 700;
}

.block {
  background: transparent;
  border-radius: 0;
  padding: 0;
  box-shadow: none;
}

.block--featured {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: visible;
}

.section-title {
  margin: 0;
  font-size: 31px;
  font-weight: 600;
  color: #3f3a68;
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.sort-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.sort-btn {
  border: 1px solid #e0e4f2;
  border-radius: 10px;
  background: #ffffff;
  color: #495274;
  font-size: 14px;
  padding: 7px 12px;
  cursor: pointer;
}

.sort-btn--active {
  border-color: #b8a9f7;
  color: #6d5ccf;
  background: #f1edff;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  align-content: start;
  gap: 16px;
  flex: 0 0 auto;
  min-height: auto;
  overflow-y: visible;
  padding-right: 0;
}

.item-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid #eaecf5;
  background: #ffffff;
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}

.item-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(135, 123, 207, 0.16);
}

.item-card__thumb {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 12px;
  background: linear-gradient(135deg, #ebe8ff 0%, #f7ecff 100%);
  position: relative;
  overflow: hidden;
}

.item-card__thumb-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.item-card__badge {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 1;
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 999px;
  background: #ffffff;
  color: #4e5473;
}

.item-card__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  gap: 6px;
}

.item-card__title {
  font-size: 30px;
  font-weight: 600;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-card__desc {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-card__price-row {
  margin-top: auto;
}

.item-card__price {
  font-size: 39px;
  font-weight: 700;
  color: #6a5ad0;
}

.item-card__seller {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #8a90a8;
}

.item-card__seller-avatar {
  flex-shrink: 0;
  border: 1px solid #dbeafe;
}

.item-card__seller-name {
  color: #8a90a8;
  line-height: 1;
  white-space: nowrap;
}

.item-card__credit-level {
  margin-left: auto;
  color: #666666;
  font-weight: 400;
  line-height: 1;
  white-space: nowrap;
}

.list-status {
  min-height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 10px;
  font-size: 12px;
  color: #94a3b8;
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

.login-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 26px;
  background: rgba(42, 40, 64, 0.34);
  backdrop-filter: blur(8px);
}

.login-modal {
  width: min(100%, 560px);
  border-radius: 24px;
  background: #fbfaff;
  box-shadow: 0 32px 80px rgba(108, 90, 175, 0.22);
  overflow: hidden;
}

.login-modal__panel {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fcfbff;
}

.login-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.login-modal__title {
  margin: 0;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 700;
  color: #5745ad;
}

.login-modal__subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #8178a8;
}

.login-modal__close {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: #ece8fb;
  color: #6a58c7;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.login-form__group {
  border-radius: 20px;
  background: #f6f4ff;
  box-shadow: 0 10px 24px rgba(133, 117, 198, 0.1);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.login-form__group--action {
  background: #f9f8ff;
}

.login-form__field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-form__group--register-top,
.login-form__group--register-bottom {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 10px;
  row-gap: 10px;
  padding: 12px;
}

.login-form__field--register-username,
.login-form__group--register-bottom .login-form__field:last-child {
  grid-column: 1 / -1;
}

.login-form__label {
  font-size: 14px;
  font-weight: 700;
  color: #4f476f;
}

.login-form__input {
  height: 46px;
  border: none;
  border-radius: 14px;
  padding: 0 14px;
  background: #ffffff;
  font-size: 14px;
  color: #413b5f;
  outline: none;
  box-shadow: inset 0 0 0 1px rgba(193, 183, 236, 0.52);
}

.login-form__textarea {
  height: auto;
  min-height: 72px;
  padding: 10px 14px;
  line-height: 1.6;
  resize: vertical;
}

.login-form__input:focus {
  box-shadow: inset 0 0 0 2px rgba(140, 124, 240, 0.68),
    0 0 0 4px rgba(198, 185, 255, 0.34);
}

.login-form__submit {
  width: 100%;
  margin-top: 2px;
  padding: 12px 18px;
  background: #8c7cf0;
  border-radius: 14px;
  font-size: 18px;
  box-shadow: 0 10px 24px rgba(140, 124, 240, 0.25);
}

.login-form__hint {
  margin: 0;
  text-align: center;
  font-size: 14px;
  color: #7a729b;
}

.login-form__switch {
  border: none;
  background: transparent;
  color: #4f54de;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.login-form__error {
  margin: 0;
  font-size: 12px;
  color: #d95367;
}

.login-form__success {
  margin: 0;
  font-size: 12px;
  color: #2f9f67;
}

.login-form__code-row {
  display: flex;
  gap: 9px;
}

.login-form__code-row .login-form__input {
  flex: 1;
}

.login-form__code-btn {
  min-width: 110px;
  border-radius: 14px;
  padding: 0 14px;
  border: none;
  background: #ebe7fb;
  color: #61509f;
}

.agreement-check {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #645b87;
}

.agreement-check input {
  margin-top: 2px;
  accent-color: #8c7cf0;
}

.agreement-check a {
  color: #5258dd;
  text-decoration: none;
}

@media (max-width: 1024px) {
  .home-header {
    display: grid;
    grid-template-columns: 1fr;
    height: auto;
    padding: 12px 16px;
    gap: 10px;
  }

  .home-header__center,
  .home-header__right {
    justify-self: stretch;
    position: static;
    transform: none;
    width: 100%;
  }

  .home-header__left {
    gap: 14px;
  }

  .logo {
    font-size: 26px;
  }

  .home-header__right {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .home-notice {
    margin-left: 16px;
    margin-right: 16px;
  }

  .home-hero {
    flex-direction: column;
    padding: 16px;
  }

  .home-ops {
    width: 100%;
    max-width: none;
  }

  .home-ops__layout {
    grid-template-columns: 1fr;
  }

  .home-hero-showcase {
    width: 100%;
  }

  .home-main {
    padding-left: 16px;
    padding-right: 16px;
  }

  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-title,
  .item-card__title {
    font-size: 24px;
  }

  .item-card__price {
    font-size: 30px;
  }
}

@media (max-width: 640px) {
  .home-hero-showcase {
    height: auto;
    min-height: 300px;
    flex-direction: column;
  }

  .home-hero-showcase__carousel-wrap {
    width: 100%;
    height: 180px;
  }

  .home-hero-showcase__title {
    font-size: 22px;
  }

  .home-hero-showcase__price {
    font-size: 28px;
  }

  .card-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .home-header__center {
    order: 3;
  }

  .search-bar__input {
    font-size: 14px;
  }

  .search-bar__btn {
    min-width: 94px;
    font-size: 16px;
  }

  .item-card__price {
    font-size: 27px;
  }

  .login-modal-mask {
    padding: 14px;
  }

  .login-modal {
    border-radius: 20px;
  }

  .login-modal__panel {
    padding: 18px;
  }

  .login-modal__title {
    font-size: 28px;
  }

  .login-form__group {
    padding: 12px;
  }

  .login-form__group--register-top,
  .login-form__group--register-bottom {
    grid-template-columns: 1fr;
  }

  .login-form__field--register-username,
  .login-form__group--register-bottom .login-form__field:last-child {
    grid-column: auto;
  }

  .login-form__code-btn {
    min-width: 94px;
  }
}
</style>
