<template>
  <div class="admin-page">
    <aside class="admin-sidebar card">
      <div class="brand">
        <h1>校园易物</h1>
        <p>管理员后台</p>
      </div>

      <nav class="menu-list">
        <button
          v-for="item in navItems"
          :key="item.key"
          :class="[
            'menu-item',
            activeMenu === item.key ? 'menu-item--active' : '',
          ]"
          @click="handleMenuClick(item.key)"
        >
          <span class="menu-item__label">{{ item.label }}</span>
          <span class="menu-item__count">{{ item.count }}</span>
        </button>
      </nav>

      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </aside>

    <main class="admin-main">
      <section v-if="activeMenu === 'dashboard'" class="dashboard-grid">
        <article
          class="metric-card card"
          v-for="metric in metrics"
          :key="metric.label"
        >
          <p class="metric-card__label">{{ metric.label }}</p>
          <p class="metric-card__value">{{ metric.value }}</p>
        </article>

        <article class="card panel chart-panel">
          <h3>交易状态分布</h3>
          <div class="status-bars">
            <div
              v-for="state in orderStateStats"
              :key="state.label"
              class="status-bar-row"
            >
              <span>{{ state.label }}</span>
              <div class="status-bar-track">
                <div
                  class="status-bar-fill"
                  :style="{ width: `${state.percent}%` }"
                ></div>
              </div>
              <strong>{{ state.count }}</strong>
            </div>
          </div>
        </article>

        <article class="card panel todo-panel">
          <h3>平台风险提醒</h3>
          <ul>
            <li>待处理异常订单：{{ abnormalOrderCount }} 单</li>
            <li>待回复客服会话：{{ pendingConversationCount }} 条</li>
          </ul>
        </article>
      </section>

      <section v-else-if="activeMenu === 'users'" class="card panel">
        <div class="toolbar toolbar--multi">
          <input
            v-model.trim="userKeyword"
            class="toolbar-input"
            placeholder="搜索用户名 / 电话 / 邮箱"
          />
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>昵称</th>
                <th>电话</th>
                <th>邮箱</th>
                <th>注册时间</th>
                <th>卖家信用分</th>
                <th>买家信用分</th>
                <th>状态</th>
                <th>发布商品</th>
                <th>订单数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in pagedUsers" :key="user.id">
                <td>{{ user.name }}</td>
                <td>{{ user.phone || "-" }}</td>
                <td>{{ user.email || "-" }}</td>
                <td>{{ user.registeredAt }}</td>
                <td>{{ user.sellerCreditScore ?? "100.0" }}</td>
                <td>{{ user.buyerCreditScore ?? "100.0" }}</td>
                <td>
                  <span
                    :class="[
                      'pill',
                      user.disabled ? 'pill--danger' : 'pill--ok',
                    ]"
                  >
                    {{ user.disabled ? "已禁用" : "正常" }}
                  </span>
                </td>
                <td>{{ user.itemCount }}</td>
                <td>{{ user.orderCount }}</td>
                <td>
                  <div class="actions">
                    <button class="text-btn" @click="editUserCreditScore(user)">
                      修改信用分
                    </button>
                    <button class="text-btn" @click="toggleUserState(user)">
                      {{ user.disabled ? "解封" : "禁用" }}
                    </button>
                    <button
                      class="text-btn text-btn--danger"
                      @click="removeUser(user.id)"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination-bar">
          <span class="pagination-info"
            >第 {{ userPage }} / {{ userTotalPages }} 页 · 共
            {{ users.length }} 条</span
          >
          <div class="pagination-actions">
            <button
              class="text-btn"
              :disabled="userPage <= 1"
              @click="goUserPage(userPage - 1)"
            >
              上一页
            </button>
            <button
              class="text-btn"
              :disabled="userPage >= userTotalPages"
              @click="goUserPage(userPage + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </section>

      <section v-else-if="activeMenu === 'items'" class="card panel">
        <div class="toolbar toolbar--multi toolbar--grid4">
          <input
            v-model.trim="itemTitleKeyword"
            class="toolbar-input"
            placeholder="商品名称"
          />
          <div ref="itemCategoryDropdownRef" class="status-select">
            <button
              type="button"
              class="status-select__trigger"
              @click="itemCategoryOpen = !itemCategoryOpen"
            >
              <span>{{ itemCategoryLabel }}</span>
              <span
                class="status-select__arrow"
                :class="{ 'is-open': itemCategoryOpen }"
                >▾</span
              >
            </button>
            <ul v-if="itemCategoryOpen" class="status-select__menu">
              <li
                class="status-select__option"
                :class="{ 'is-selected': itemCategoryKeyword === '' }"
                @click="selectItemCategory('')"
              >
                全部分类
              </li>
              <li
                v-for="option in itemCategoryOptions"
                :key="option.value"
                class="status-select__option"
                :class="{ 'is-selected': itemCategoryKeyword === option.value }"
                @click="selectItemCategory(option.value)"
              >
                {{ option.label }}
              </li>
            </ul>
          </div>
          <input
            v-model.trim="itemPriceKeyword"
            class="toolbar-input"
            placeholder="价格"
          />
          <input
            v-model.trim="itemPublisherKeyword"
            class="toolbar-input"
            placeholder="发布者"
          />
          <div ref="itemStatusDropdownRef" class="status-select">
            <button
              type="button"
              class="status-select__trigger"
              @click="itemStatusOpen = !itemStatusOpen"
            >
              <span>{{ itemStatusLabel }}</span>
              <span
                class="status-select__arrow"
                :class="{ 'is-open': itemStatusOpen }"
                >▾</span
              >
            </button>
            <ul v-if="itemStatusOpen" class="status-select__menu">
              <li
                v-for="option in itemStatusOptions"
                :key="option.value"
                class="status-select__option"
                :class="{ 'is-selected': itemStatus === option.value }"
                @click="selectItemStatus(option.value)"
              >
                {{ option.label }}
              </li>
            </ul>
          </div>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>商品名称</th>
                <th>分类</th>
                <th>价格</th>
                <th>发布者</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in pagedItems" :key="item.id">
                <td>{{ item.title }}</td>
                <td>{{ item.category }}</td>
                <td>￥{{ item.price }}</td>
                <td>{{ item.owner }}</td>
                <td>
                  <span class="pill" :class="statusClass(item.status)">{{
                    statusText(item.status)
                  }}</span>
                </td>
                <td>
                  <div class="actions">
                    <button
                      class="text-btn"
                      @click="approveItem(item)"
                      :disabled="item.status !== 'pending'"
                    >
                      审核通过
                    </button>
                    <button
                      class="text-btn"
                      @click="forceOffline(item)"
                      :disabled="item.status === 'offline'"
                    >
                      强制下架
                    </button>
                    <button
                      class="text-btn text-btn--danger"
                      @click="deleteItem(item.id)"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination-bar">
          <span class="pagination-info"
            >第 {{ itemPage }} / {{ itemTotalPages }} 页 · 共
            {{ items.length }} 条</span
          >
          <div class="pagination-actions">
            <button
              class="text-btn"
              :disabled="itemPage <= 1"
              @click="goItemPage(itemPage - 1)"
            >
              上一页
            </button>
            <button
              class="text-btn"
              :disabled="itemPage >= itemTotalPages"
              @click="goItemPage(itemPage + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </section>

      <section v-else-if="activeMenu === 'orders'" class="card panel">
        <div class="toolbar toolbar--multi toolbar--grid4">
          <input
            v-model.trim="orderNoKeyword"
            class="toolbar-input"
            placeholder="订单号"
          />
          <input
            v-model.trim="orderBuyerKeyword"
            class="toolbar-input"
            placeholder="买家"
          />
          <input
            v-model.trim="orderSellerKeyword"
            class="toolbar-input"
            placeholder="卖家"
          />
          <input
            v-model.trim="orderItemKeyword"
            class="toolbar-input"
            placeholder="商品"
          />
          <div ref="orderStateDropdownRef" class="status-select">
            <button
              type="button"
              class="status-select__trigger"
              @click="orderStateOpen = !orderStateOpen"
            >
              <span>{{ orderStateLabel }}</span>
              <span
                class="status-select__arrow"
                :class="{ 'is-open': orderStateOpen }"
                >▾</span
              >
            </button>
            <ul v-if="orderStateOpen" class="status-select__menu">
              <li
                v-for="option in orderStateOptions"
                :key="option.value"
                class="status-select__option"
                :class="{ 'is-selected': orderState === option.value }"
                @click="selectOrderState(option.value)"
              >
                {{ option.label }}
              </li>
            </ul>
          </div>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>订单号</th>
                <th>商品</th>
                <th>买家</th>
                <th>卖家</th>
                <th>金额</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in pagedOrders" :key="order.orderNo">
                <td>{{ order.orderNo }}</td>
                <td>{{ order.itemTitle }}</td>
                <td>{{ order.buyer }}</td>
                <td>{{ order.seller }}</td>
                <td>￥{{ order.amount }}</td>
                <td>
                  <span class="pill" :class="orderStatusClass(order)">{{
                    orderStatusText(order)
                  }}</span>
                </td>
                <td>
                  <div class="actions actions--wrap">
                    <button
                      class="text-btn"
                      @click="updateOrderStatusByAdmin(order, 'COMPLETED')"
                    >
                      设为已完成
                    </button>
                    <button
                      class="text-btn text-btn--danger"
                      @click="updateOrderStatusByAdmin(order, 'CANCELLED')"
                    >
                      设为已取消
                    </button>
                    <button
                      class="text-btn"
                      @click="updateOrderRefundStatusByAdmin(order, 'APPROVED')"
                    >
                      设为退款
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination-bar">
          <span class="pagination-info"
            >第 {{ orderPage }} / {{ orderTotalPages }} 页 · 共
            {{ orders.length }} 条</span
          >
          <div class="pagination-actions">
            <button
              class="text-btn"
              :disabled="orderPage <= 1"
              @click="goOrderPage(orderPage - 1)"
            >
              上一页
            </button>
            <button
              class="text-btn"
              :disabled="orderPage >= orderTotalPages"
              @click="goOrderPage(orderPage + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </section>

      <section
        v-else-if="activeMenu === 'support'"
        class="card panel support-panel"
      >
        <div class="support-layout">
          <aside class="conversation-list">
            <button
              v-for="session in conversations"
              :key="session.id"
              :class="[
                'conversation-item',
                activeConversationId === session.id
                  ? 'conversation-item--active'
                  : '',
              ]"
              @click="activeConversationId = session.id"
            >
              <strong>{{ supportConversationTitle(session) }}</strong>
              <span class="conversation-item__meta"
                >{{ session.reporterName || "用户" }} ·
                {{ mapReportTypeText(session.reportType) }}</span
              >
              <span class="conversation-item__preview">{{
                supportConversationPreview(session.preview)
              }}</span>
            </button>
          </aside>

          <section class="conversation-main">
            <div class="support-topbar">
              <h3>{{ supportConversationTitle(currentConversation) }}</h3>
              <button
                v-if="
                  currentConversation &&
                  currentConversation.status !== 'RESOLVED' &&
                  currentConversation.status !== 'CLOSED'
                "
                class="text-btn"
                @click="resolveConversation"
              >
                标记已解决
              </button>
              <button
                v-else-if="currentConversation"
                class="text-btn"
                @click="reopenConversation"
              >
                重新打开
              </button>
            </div>
            <div v-if="currentConversation" class="support-meta">
              <span
                :class="[
                  'pill',
                  supportStatusClass(currentConversation.status),
                ]"
              >
                {{ supportStatusText(currentConversation.status) }}
              </span>
              <span>举报人：{{ currentConversation.reporterName || "-" }}</span>
              <span>商品：{{ currentConversation.itemTitle || "-" }}</span>
            </div>
            <p v-if="currentConversation?.reportContent" class="report-content">
              <strong>举报内容：</strong>{{ currentConversation.reportContent }}
            </p>
            <div class="chat-box">
              <article
                v-for="msg in currentConversation?.messages || []"
                :key="msg.id"
                :class="[
                  'support-message',
                  msg.from === '管理员'
                    ? 'support-message--admin'
                    : 'support-message--user',
                ]"
              >
                <img
                  v-if="getSupportMessageImageUrl(msg)"
                  :src="getSupportMessageImageUrl(msg)"
                  alt="客服图片"
                  class="support-message__image"
                />
                <p
                  v-if="getSupportMessageText(msg)"
                  class="support-message__content"
                >
                  {{ getSupportMessageText(msg) }}
                </p>
                <time class="support-message__time">{{
                  formatSupportMessageTime(msg.createdAt)
                }}</time>
              </article>
            </div>
            <div class="toolbar">
              <input
                ref="supportImageInputRef"
                type="file"
                class="support-image-input"
                accept="image/*"
                @change="handleSupportImageUpload"
              />
              <button
                class="text-btn"
                type="button"
                :disabled="supportUploadingImage"
                @click="openSupportImagePicker"
              >
                {{ supportUploadingImage ? "上传中..." : "发送图片" }}
              </button>
              <span v-if="pendingSupportImageUrl" class="support-image-ready"
                >已选择图片</span
              >
              <input
                v-model.trim="supportReply"
                class="toolbar-input"
                placeholder="输入处理意见"
              />
              <button class="primary-btn" @click="appendReply">发送</button>
            </div>
          </section>
        </div>
      </section>

      <section v-else class="card panel">
        <div class="setting-block">
          <div class="setting-header">
            <h3>发布平台公告</h3>
            <button class="primary-btn" @click="publishNotice">发布公告</button>
          </div>
          <div class="notice-editor">
            <textarea
              v-model="newNotice"
              class="notice-input"
              placeholder="请输入公告内容"
            ></textarea>
          </div>
        </div>

        <div class="setting-block">
          <h3>当前公告</h3>
          <div v-if="currentNotice" class="current-notice-card">
            <div class="notice-item__content">
              <p class="notice-item__text">{{ currentNotice.content }}</p>
              <span class="notice-item__date"
                >发布时间：{{ formatNoticeDate(currentNotice.createdAt) }}</span
              >
            </div>
            <button
              class="text-btn text-btn--danger"
              @click="deleteNotice(currentNotice.id)"
            >
              删除
            </button>
          </div>
          <p v-else class="notice-empty">暂无当前公告</p>
        </div>

        <div class="setting-block">
          <h3>历史公告</h3>
          <ul class="notice-list">
            <li v-for="notice in historyNotices" :key="notice.id">
              <div class="notice-item__content">
                <p class="notice-item__text">{{ notice.content }}</p>
                <span class="notice-item__date"
                  >发布时间：{{ formatNoticeDate(notice.createdAt) }}</span
                >
              </div>
              <button
                class="text-btn text-btn--danger"
                @click="deleteNotice(notice.id)"
              >
                删除
              </button>
            </li>
          </ul>
        </div>
      </section>
    </main>

    <div
      v-if="creditModalVisible"
      class="credit-modal-mask"
      @click.self="closeCreditModal"
    >
      <section class="credit-modal card">
        <header class="credit-modal__header">
          <h3>修改信用分</h3>
        </header>
        <p class="credit-modal__subtitle">
          用户：{{ creditModalTarget?.name || creditModalTarget?.email || "-" }}
        </p>
        <label class="credit-modal__label" for="seller-credit-score-input"
          >卖家信用分</label
        >
        <input
          id="seller-credit-score-input"
          v-model.trim="creditModalSellerValue"
          class="toolbar-input credit-modal__input"
          placeholder="请输入卖家信用分"
          :disabled="creditModalSubmitting"
          @keydown.enter.prevent="submitCreditModal"
        />
        <label class="credit-modal__label" for="buyer-credit-score-input"
          >买家信用分</label
        >
        <input
          id="buyer-credit-score-input"
          v-model.trim="creditModalBuyerValue"
          class="toolbar-input credit-modal__input"
          placeholder="请输入买家信用分"
          :disabled="creditModalSubmitting"
          @keydown.enter.prevent="submitCreditModal"
        />
        <footer class="credit-modal__actions">
          <button
            class="text-btn"
            type="button"
            :disabled="creditModalSubmitting"
            @click="closeCreditModal"
          >
            取消
          </button>
          <button
            class="primary-btn"
            type="button"
            :disabled="creditModalSubmitting"
            @click="submitCreditModal"
          >
            {{ creditModalSubmitting ? "保存中..." : "确认保存" }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { logout } from "../service/common/authSessionService";
import { buildMessageWebSocketUrl } from "../service/chat/chatApiService";
import { fetchHomeCategories } from "../service/home/homeApiService";
import { uploadPublishImage } from "../service/publish/publishApiService";
import {
  approveAdminItem,
  createAdminNotice,
  deleteAdminItem,
  deleteAdminNotice,
  deleteAdminUser,
  fetchAdminConversations,
  fetchAdminDashboard,
  fetchAdminItems,
  fetchAdminNotices,
  fetchAdminOrders,
  fetchAdminUsers,
  offlineAdminItem,
  replyAdminConversation,
  updateAdminOrder,
  updateAdminConversationStatus,
  updateAdminUserCreditScore,
  updateAdminUserStatus,
} from "../service/admin/adminApiService";

const router = useRouter();
const loading = ref(false);
const socketRef = ref(null);
const reconnectTimerRef = ref(null);
const isManualClose = ref(false);
const ADMIN_ACTIVE_MENU_KEY = "admin.dashboard.activeMenu";

const navItems = ref([
  { key: "dashboard", label: "仪表盘", count: "00" },
  { key: "users", label: "用户管理", count: "00" },
  { key: "items", label: "商品管理", count: "00" },
  { key: "orders", label: "订单管理", count: "00" },
  { key: "support", label: "客服模块", count: "00" },
  { key: "settings", label: "系统设置", count: "00" },
]);

const activeMenu = ref("dashboard");
const loadedMenus = ref(new Set());

const metrics = ref([]);
const orderStateStats = ref([]);
const dashboardRaw = ref({
  pendingItemCount: 0,
  abnormalOrderCount: 0,
  pendingConversationCount: 0,
});

const users = ref([]);
const userKeyword = ref("");
const userSearchTimer = ref(null);
const userPage = ref(1);
const creditModalVisible = ref(false);
const creditModalSellerValue = ref("100.0");
const creditModalBuyerValue = ref("100.0");
const creditModalTarget = ref(null);
const creditModalSubmitting = ref(false);

const items = ref([]);
const itemTitleKeyword = ref("");
const itemCategoryKeyword = ref("");
const itemPriceKeyword = ref("");
const itemPublisherKeyword = ref("");
const itemPage = ref(1);
const itemStatus = ref("all");
const itemCategoryOpen = ref(false);
const itemStatusOpen = ref(false);
const itemCategoryDropdownRef = ref(null);
const itemStatusDropdownRef = ref(null);
const itemSearchTimer = ref(null);
const itemCategoryOptions = ref([]);
const itemStatusOptions = [
  { value: "all", label: "全部状态" },
  { value: "pending", label: "待审核" },
  { value: "online", label: "上架中" },
  { value: "offline", label: "已下架" },
];
const itemStatusLabel = computed(
  () =>
    itemStatusOptions.find((option) => option.value === itemStatus.value)
      ?.label || "全部状态"
);
const itemCategoryLabel = computed(() => {
  if (!itemCategoryKeyword.value) return "全部分类";
  return (
    itemCategoryOptions.value.find(
      (option) => option.value === itemCategoryKeyword.value
    )?.label || itemCategoryKeyword.value
  );
});

const orders = ref([]);
const orderNoKeyword = ref("");
const orderBuyerKeyword = ref("");
const orderSellerKeyword = ref("");
const orderItemKeyword = ref("");
const orderPage = ref(1);
const orderState = ref("all");
const orderStateOpen = ref(false);
const orderStateDropdownRef = ref(null);
const orderSearchTimer = ref(null);
const orderStateOptions = [
  { value: "all", label: "全部状态" },
  { value: "pending-pay", label: "待付款" },
  { value: "pending-confirm", label: "待确认" },
  { value: "completed", label: "已完成" },
  { value: "cancelled", label: "已取消" },
];
const orderStateLabel = computed(
  () =>
    orderStateOptions.find((option) => option.value === orderState.value)
      ?.label || "全部状态"
);

const conversations = ref([]);
const activeConversationId = ref(null);
const supportReply = ref("");
const supportImageInputRef = ref(null);
const pendingSupportImageUrl = ref("");
const supportUploadingImage = ref(false);
const currentConversation = computed(() =>
  conversations.value.find(
    (session) => session.id === activeConversationId.value
  )
);

const notices = ref([]);
const newNotice = ref("");
const currentNotice = computed(() => notices.value[0] || null);
const historyNotices = computed(() => notices.value.slice(1));

const abnormalOrderCount = computed(
  () => dashboardRaw.value.abnormalOrderCount || 0
);
const pendingConversationCount = computed(
  () => dashboardRaw.value.pendingConversationCount || 0
);

const PAGE_SIZE = 10;

const userTotalPages = computed(() =>
  Math.max(1, Math.ceil(users.value.length / PAGE_SIZE))
);
const itemTotalPages = computed(() =>
  Math.max(1, Math.ceil(items.value.length / PAGE_SIZE))
);
const orderTotalPages = computed(() =>
  Math.max(1, Math.ceil(orders.value.length / PAGE_SIZE))
);

const pagedUsers = computed(() => {
  const start = (userPage.value - 1) * PAGE_SIZE;
  return users.value.slice(start, start + PAGE_SIZE);
});

const pagedItems = computed(() => {
  const start = (itemPage.value - 1) * PAGE_SIZE;
  return items.value.slice(start, start + PAGE_SIZE);
});

const pagedOrders = computed(() => {
  const start = (orderPage.value - 1) * PAGE_SIZE;
  return orders.value.slice(start, start + PAGE_SIZE);
});

const goUserPage = (nextPage) => {
  const page = Number(nextPage || 1);
  if (!Number.isFinite(page)) return;
  userPage.value = Math.min(
    userTotalPages.value,
    Math.max(1, Math.trunc(page))
  );
};

const goItemPage = (nextPage) => {
  const page = Number(nextPage || 1);
  if (!Number.isFinite(page)) return;
  itemPage.value = Math.min(
    itemTotalPages.value,
    Math.max(1, Math.trunc(page))
  );
};

const goOrderPage = (nextPage) => {
  const page = Number(nextPage || 1);
  if (!Number.isFinite(page)) return;
  orderPage.value = Math.min(
    orderTotalPages.value,
    Math.max(1, Math.trunc(page))
  );
};

const normalizeDate = (value) => {
  if (!value) return "-";
  return String(value).slice(0, 10);
};

const moneyText = (value) => {
  const number = Number(value || 0);
  if (Number.isNaN(number)) return "0.00";
  return number.toFixed(2);
};

const formatNoticeDate = (value) => {
  if (!value) return "-";
  const raw = String(value).replace("T", " ");
  if (raw.length >= 16) {
    return raw.slice(0, 16);
  }
  return raw;
};

const formatSupportMessageTime = (value) => {
  if (!value) return "";
  const raw = String(value).replace("T", " ");
  return raw.length >= 16 ? raw.slice(0, 16) : raw;
};

const SUPPORT_IMAGE_PREFIX = "【图片】";

const parseSupportMessagePayload = (msg) => {
  const imageUrl = String(msg?.imageUrl || "").trim();
  const contentRaw = String(msg?.content || "").trim();

  if (imageUrl) {
    return { imageUrl, text: contentRaw };
  }

  if (!contentRaw) {
    return { imageUrl: "", text: "" };
  }

  if (contentRaw.startsWith(SUPPORT_IMAGE_PREFIX)) {
    const lines = contentRaw.split("\n");
    return {
      imageUrl: lines[0].replace(SUPPORT_IMAGE_PREFIX, "").trim(),
      text: lines.slice(1).join("\n").trim(),
    };
  }

  const directImageUrlPattern =
    /^https?:\/\/\S+\.(png|jpe?g|webp|gif|bmp|svg)(\?.*)?$/i;
  if (directImageUrlPattern.test(contentRaw)) {
    return { imageUrl: contentRaw, text: "" };
  }

  return { imageUrl: "", text: contentRaw };
};

const getSupportMessageImageUrl = (msg) =>
  parseSupportMessagePayload(msg).imageUrl;
const getSupportMessageText = (msg) => parseSupportMessagePayload(msg).text;

const applyDashboard = (data = {}) => {
  dashboardRaw.value = {
    pendingItemCount: Number(data.pendingItemCount || 0),
    abnormalOrderCount: Number(data.abnormalOrderCount || 0),
    pendingConversationCount: Number(data.pendingConversationCount || 0),
  };

  metrics.value = [
    {
      label: "今日新增用户",
      value: String(data.todayNewUsers ?? 0),
    },
    {
      label: "总用户数",
      value: String(data.totalUsers ?? 0),
    },
    {
      label: "今日新增商品",
      value: String(data.todayNewItems ?? 0),
    },
    {
      label: "总商品数",
      value: String(data.totalItems ?? 0),
    },
    {
      label: "今日订单数",
      value: String(data.todayOrders ?? 0),
    },
    {
      label: "今日销售额",
      value: `￥${moneyText(data.todaySales)}`,
    },
  ];

  orderStateStats.value = Array.isArray(data.orderStateStats)
    ? data.orderStateStats
    : [];
};

const updateNavCount = () => {
  const map = {
    users: users.value.length,
    items: items.value.length,
    orders: orders.value.length,
    support: conversations.value.length,
    settings: notices.value.length,
  };
  navItems.value = navItems.value.map((item) =>
    map[item.key] === undefined
      ? item
      : { ...item, count: String(map[item.key]).padStart(2, "0") }
  );
};

const loadDashboard = async () => {
  const { data } = await fetchAdminDashboard();
  applyDashboard(data);
};

const loadUsers = async () => {
  const { data } = await fetchAdminUsers({
    keyword: userKeyword.value,
  });
  users.value = (data || []).map((item) => ({
    ...item,
    registeredAt: normalizeDate(item.registeredAt),
  }));
  goUserPage(1);
  updateNavCount();
};

const loadItems = async () => {
  const { data } = await fetchAdminItems({
    title: itemTitleKeyword.value,
    category: itemCategoryKeyword.value,
    price: itemPriceKeyword.value,
    publisher: itemPublisherKeyword.value,
    status: itemStatus.value,
  });
  items.value = data || [];
  goItemPage(1);
  updateNavCount();
};

const loadItemCategories = async () => {
  const response = await fetchHomeCategories();
  const remoteOptions = Array.isArray(response?.data)
    ? response.data
        .map((item) => ({
          value: String(item?.name || "").trim(),
          label: String(item?.name || "").trim(),
        }))
        .filter((item) => item.value)
    : [];
  itemCategoryOptions.value = remoteOptions;
};

const loadOrders = async () => {
  const { data } = await fetchAdminOrders({
    orderNo: orderNoKeyword.value,
    buyer: orderBuyerKeyword.value,
    seller: orderSellerKeyword.value,
    itemTitle: orderItemKeyword.value,
    status: orderState.value,
  });
  orders.value = data || [];
  goOrderPage(1);
  updateNavCount();
};

const scheduleUsersReload = () => {
  if (activeMenu.value !== "users" || !loadedMenus.value.has("users")) return;
  if (userSearchTimer.value) clearTimeout(userSearchTimer.value);
  userSearchTimer.value = window.setTimeout(() => {
    userSearchTimer.value = null;
    loadUsers().catch((error) =>
      ElMessage.error(error.message || "用户搜索失败")
    );
  }, 260);
};

const scheduleItemsReload = () => {
  if (activeMenu.value !== "items" || !loadedMenus.value.has("items")) return;
  if (itemSearchTimer.value) clearTimeout(itemSearchTimer.value);
  itemSearchTimer.value = window.setTimeout(() => {
    itemSearchTimer.value = null;
    loadItems().catch((error) =>
      ElMessage.error(error.message || "商品搜索失败")
    );
  }, 260);
};

const scheduleOrdersReload = () => {
  if (activeMenu.value !== "orders" || !loadedMenus.value.has("orders")) return;
  if (orderSearchTimer.value) clearTimeout(orderSearchTimer.value);
  orderSearchTimer.value = window.setTimeout(() => {
    orderSearchTimer.value = null;
    loadOrders().catch((error) =>
      ElMessage.error(error.message || "订单搜索失败")
    );
  }, 260);
};

const loadConversations = async () => {
  const { data } = await fetchAdminConversations();
  conversations.value = data || [];
  if (!activeConversationId.value && conversations.value.length > 0) {
    activeConversationId.value = conversations.value[0].id;
  }
  updateNavCount();
};

const scheduleReconnect = () => {
  if (isManualClose.value || reconnectTimerRef.value) return;
  reconnectTimerRef.value = window.setTimeout(() => {
    reconnectTimerRef.value = null;
    connectWebSocket();
  }, 3000);
};

const connectWebSocket = async () => {
  if (isManualClose.value) return;
  try {
    const wsUrl = await buildMessageWebSocketUrl();
    const socket = new WebSocket(wsUrl);
    socketRef.value = socket;

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data || "{}");
        const eventType = String(payload?.eventType || "").toUpperCase();
        if (eventType === "SUPPORT_MESSAGE_CREATED") {
          loadConversations();
          loadDashboard();
          return;
        }
      } catch {
        // ignore
      }
    };

    socket.onclose = () => {
      socketRef.value = null;
      scheduleReconnect();
    };
  } catch {
    scheduleReconnect();
  }
};

const loadNotices = async () => {
  const { data } = await fetchAdminNotices();
  notices.value = data || [];
  updateNavCount();
};

const persistActiveMenu = (menuKey) => {
  try {
    localStorage.setItem(ADMIN_ACTIVE_MENU_KEY, menuKey);
  } catch {
    // ignore
  }
};

const restoreActiveMenu = () => {
  try {
    const saved = localStorage.getItem(ADMIN_ACTIVE_MENU_KEY);
    const existed = navItems.value.some((item) => item.key === saved);
    return existed ? saved : "dashboard";
  } catch {
    return "dashboard";
  }
};

const ensureMenuDataLoaded = async (menuKey, options = {}) => {
  const { force = false } = options;
  if (!force && loadedMenus.value.has(menuKey)) return;

  loading.value = true;
  try {
    if (menuKey === "dashboard") {
      await loadDashboard();
    } else if (menuKey === "users") {
      await loadUsers();
    } else if (menuKey === "items") {
      await Promise.all([loadItemCategories(), loadItems()]);
    } else if (menuKey === "orders") {
      await loadOrders();
    } else if (menuKey === "support") {
      await loadConversations();
    } else if (menuKey === "settings") {
      await loadNotices();
    }
    loadedMenus.value.add(menuKey);
  } finally {
    loading.value = false;
  }
};

const handleMenuClick = async (menuKey) => {
  if (activeMenu.value === menuKey) {
    await ensureMenuDataLoaded(menuKey);
    return;
  }
  activeMenu.value = menuKey;
  persistActiveMenu(menuKey);
  try {
    await ensureMenuDataLoaded(menuKey);
  } catch (error) {
    ElMessage.error(error.message || "数据加载失败");
  }
};

const statusText = (status) => {
  const map = {
    pending: "待审核",
    online: "上架中",
    offline: "已下架",
    "pending-pay": "待付款",
    "pending-confirm": "待确认",
    completed: "已完成",
    cancelled: "已取消",
  };
  return map[status] || "未知";
};

const statusClass = (status) => {
  if (status === "offline" || status === "cancelled") return "pill--danger";
  if (
    status === "pending" ||
    status === "pending-pay" ||
    status === "pending-confirm"
  )
    return "pill--warn";
  if (status === "completed" || status === "online") return "pill--ok";
  return "";
};

const normalizeRefundStatus = (refundStatus) =>
  String(refundStatus || "")
    .trim()
    .toLowerCase();

const orderStatusText = (order) => {
  const refundStatus = normalizeRefundStatus(order?.refundStatus);
  if (refundStatus === "applied") {
    return "退款";
  }
  if (refundStatus === "approved") {
    return "已退款";
  }
  if (refundStatus === "rejected") {
    return "退款已拒绝";
  }
  return statusText(order?.status);
};

const orderStatusClass = (order) => {
  const refundStatus = normalizeRefundStatus(order?.refundStatus);
  if (refundStatus === "applied") return "pill--warn";
  if (refundStatus === "approved") return "pill--ok";
  if (refundStatus === "rejected") return "pill--danger";
  return statusClass(order?.status);
};

const supportStatusText = (status) => {
  const map = {
    OPEN: "待处理",
    PROCESSING: "处理中",
    RESOLVED: "已解决",
    CLOSED: "已关闭",
  };
  return map[String(status || "").toUpperCase()] || "待处理";
};

const supportStatusClass = (status) => {
  const value = String(status || "").toUpperCase();
  if (value === "RESOLVED" || value === "CLOSED") return "pill--ok";
  if (value === "PROCESSING") return "pill--warn";
  return "pill--danger";
};

const mapReportTypeText = (type) => {
  const map = {
    PROHIBITED_CONTACT: "违规联系方式",
    COUNTERFEIT: "疑似假货",
    WRONG_CATEGORY: "类目错误",
    FRAUD_RISK: "欺诈风险",
    OTHER: "其他",
  };
  return map[String(type || "").toUpperCase()] || "违规举报";
};

const supportConversationTitle = (session) => {
  if (!session) return "请选择会话";
  const reportTypeText = mapReportTypeText(session.reportType);
  const base = reportTypeText === "其他" ? "客服咨询" : reportTypeText;
  if (session.itemTitle) {
    return `${base} · ${session.itemTitle}`;
  }
  return base;
};

const supportConversationPreview = (value) => {
  const raw = String(value || "").trim();
  if (!raw) return "";
  const imagePrefix = "【图片】";
  if (raw.startsWith(imagePrefix)) return "[图片]";
  const directImageUrlPattern =
    /^https?:\/\/\S+\.(png|jpe?g|webp|gif|bmp|svg)(\?.*)?$/i;
  if (directImageUrlPattern.test(raw)) return "[图片]";
  return raw;
};

const selectItemStatus = (value) => {
  itemStatus.value = value;
  itemStatusOpen.value = false;
};

const selectItemCategory = (value) => {
  itemCategoryKeyword.value = value;
  itemCategoryOpen.value = false;
};

const selectOrderState = (value) => {
  orderState.value = value;
  orderStateOpen.value = false;
};

const handleDocumentClick = (event) => {
  const target = event.target;
  if (
    itemCategoryDropdownRef.value &&
    !itemCategoryDropdownRef.value.contains(target)
  ) {
    itemCategoryOpen.value = false;
  }
  if (
    itemStatusDropdownRef.value &&
    !itemStatusDropdownRef.value.contains(target)
  ) {
    itemStatusOpen.value = false;
  }
  if (
    orderStateDropdownRef.value &&
    !orderStateDropdownRef.value.contains(target)
  ) {
    orderStateOpen.value = false;
  }
};

const toggleUserState = (user) => {
  updateAdminUserStatus(user.id, !user.disabled)
    .then(async () => {
      await Promise.all([loadUsers(), loadDashboard()]);
      ElMessage.success(!user.disabled ? "用户已禁用" : "用户已解封");
    })
    .catch((error) => ElMessage.error(error.message || "操作失败"));
};

const removeUser = (id) => {
  deleteAdminUser(id)
    .then(async () => {
      await Promise.all([loadUsers(), loadDashboard()]);
      ElMessage.success("用户已删除");
    })
    .catch((error) => ElMessage.error(error.message || "删除失败"));
};

const editUserCreditScore = (user) => {
  if (!user?.id) return;
  const sellerCurrent = Number(user?.sellerCreditScore ?? 100);
  const buyerCurrent = Number(user?.buyerCreditScore ?? 100);
  creditModalTarget.value = user;
  creditModalSellerValue.value = Number.isFinite(sellerCurrent)
    ? sellerCurrent.toFixed(1)
    : "100.0";
  creditModalBuyerValue.value = Number.isFinite(buyerCurrent)
    ? buyerCurrent.toFixed(1)
    : "100.0";
  creditModalVisible.value = true;
};

const closeCreditModal = () => {
  if (creditModalSubmitting.value) return;
  creditModalVisible.value = false;
  creditModalTarget.value = null;
};

const submitCreditModal = async () => {
  if (!creditModalTarget.value?.id || creditModalSubmitting.value) return;

  const sellerNext = Number(String(creditModalSellerValue.value || "").trim());
  const buyerNext = Number(String(creditModalBuyerValue.value || "").trim());
  if (!Number.isFinite(sellerNext) || !Number.isFinite(buyerNext)) {
    ElMessage.warning("请输入有效数字信用分");
    return;
  }

  creditModalSubmitting.value = true;
  try {
    const normalizedSeller = Math.round(sellerNext * 10) / 10;
    const normalizedBuyer = Math.round(buyerNext * 10) / 10;
    await Promise.all([
      updateAdminUserCreditScore(
        creditModalTarget.value.id,
        "SELLER",
        normalizedSeller
      ),
      updateAdminUserCreditScore(
        creditModalTarget.value.id,
        "BUYER",
        normalizedBuyer
      ),
    ]);
    await Promise.all([loadUsers(), loadDashboard()]);
    ElMessage.success("用户买卖信用分已更新");
    closeCreditModal();
  } catch (error) {
    ElMessage.error(error.message || "更新信用分失败");
  } finally {
    creditModalSubmitting.value = false;
  }
};

const approveItem = (item) => {
  if (item.status !== "pending") return;
  approveAdminItem(item.id)
    .then(async () => {
      await Promise.all([loadItems(), loadDashboard()]);
      ElMessage.success("商品审核通过");
    })
    .catch((error) => ElMessage.error(error.message || "审核失败"));
};

const forceOffline = (item) => {
  offlineAdminItem(item.id)
    .then(async () => {
      await Promise.all([loadItems(), loadDashboard()]);
      ElMessage.success("商品已强制下架");
    })
    .catch((error) => ElMessage.error(error.message || "下架失败"));
};

const deleteItem = (id) => {
  deleteAdminItem(id)
    .then(async () => {
      await Promise.all([loadItems(), loadDashboard()]);
      ElMessage.success("商品已删除");
    })
    .catch((error) => ElMessage.error(error.message || "删除失败"));
};

const adminOrderStatusOptions = [
  { value: "PENDING_PAYMENT", label: "待付款" },
  { value: "PENDING_CONFIRMATION", label: "待确认" },
  { value: "COMPLETED", label: "已完成" },
  { value: "CANCELLED", label: "已取消" },
];

const getAdminStatusLabel = (value) =>
  adminOrderStatusOptions.find((option) => option.value === String(value || ""))
    ?.label || String(value || "-");

const toAdminOrderStatus = (status) => {
  const normalized = String(status || "")
    .trim()
    .toLowerCase();
  if (normalized === "pending-pay") return "PENDING_PAYMENT";
  if (normalized === "pending-confirm") return "PENDING_CONFIRMATION";
  if (normalized === "completed") return "COMPLETED";
  if (normalized === "cancelled") return "CANCELLED";
  return null;
};

const toAdminRefundStatus = (status) => {
  const normalized = String(status || "")
    .trim()
    .toLowerCase();
  if (!normalized || normalized === "none") return "NONE";
  if (normalized === "applied") return "APPLIED";
  if (normalized === "approved") return "APPROVED";
  if (normalized === "rejected") return "REJECTED";
  return "NONE";
};

const updateOrderStatusByAdmin = async (order, nextStatus) => {
  const orderNo = String(order?.orderNo || "").trim();
  if (!orderNo || !nextStatus) return;
  try {
    await updateAdminOrder(orderNo, {
      status: nextStatus,
      refundStatus: null,
    });
    await Promise.all([loadOrders(), loadDashboard()]);
    ElMessage.success(`订单状态已更新为${getAdminStatusLabel(nextStatus)}`);
  } catch (error) {
    ElMessage.error(error.message || "更新订单状态失败");
  }
};

const updateOrderRefundStatusByAdmin = async (order, nextRefundStatus) => {
  const orderNo = String(order?.orderNo || "").trim();
  if (!orderNo || !nextRefundStatus) return;
  try {
    await updateAdminOrder(orderNo, {
      status: null,
      refundStatus: nextRefundStatus,
    });
    await Promise.all([loadOrders(), loadDashboard()]);
    ElMessage.success("订单已设为退款");
  } catch (error) {
    ElMessage.error(error.message || "更新退款状态失败");
  }
};

const appendReply = () => {
  if (!supportReply.value && !pendingSupportImageUrl.value) {
    ElMessage.warning("请输入回复内容或上传图片");
    return;
  }
  const target = currentConversation.value;
  if (!target) return;
  replyAdminConversation(target.id, {
    content: supportReply.value,
    imageUrl: pendingSupportImageUrl.value,
  })
    .then(async () => {
      supportReply.value = "";
      pendingSupportImageUrl.value = "";
      await Promise.all([loadConversations(), loadDashboard()]);
      ElMessage.success("回复已发送");
    })
    .catch((error) => ElMessage.error(error.message || "发送失败"));
};

const openSupportImagePicker = () => {
  supportImageInputRef.value?.click();
};

const handleSupportImageUpload = async (event) => {
  const input = event.target;
  const file = input?.files?.[0];
  if (!file) return;
  supportUploadingImage.value = true;
  try {
    const response = await uploadPublishImage(file);
    const url = String(response?.data?.url || "").trim();
    if (!url) {
      throw new Error("图片上传失败");
    }
    pendingSupportImageUrl.value = url;
    ElMessage.success("图片已上传");
  } catch (error) {
    ElMessage.error(error.message || "图片上传失败");
  } finally {
    supportUploadingImage.value = false;
    if (input) input.value = "";
  }
};

const resolveConversation = () => {
  const target = currentConversation.value;
  if (!target) return;
  updateAdminConversationStatus(target.id, "RESOLVED")
    .then(async () => {
      await Promise.all([loadConversations(), loadDashboard()]);
      ElMessage.success("会话已标记为已解决");
    })
    .catch((error) => ElMessage.error(error.message || "更新失败"));
};

const reopenConversation = () => {
  const target = currentConversation.value;
  if (!target) return;
  updateAdminConversationStatus(target.id, "OPEN")
    .then(async () => {
      await Promise.all([loadConversations(), loadDashboard()]);
      ElMessage.success("会话已重新打开");
    })
    .catch((error) => ElMessage.error(error.message || "更新失败"));
};

const publishNotice = () => {
  if (!newNotice.value.trim()) {
    ElMessage.warning("公告内容不能为空");
    return;
  }
  createAdminNotice(newNotice.value.trim())
    .then(async () => {
      newNotice.value = "";
      await loadNotices();
      ElMessage.success("公告已发布");
    })
    .catch((error) => ElMessage.error(error.message || "发布失败"));
};

const deleteNotice = (id) => {
  deleteAdminNotice(id)
    .then(async () => {
      await loadNotices();
      ElMessage.success("公告已删除");
    })
    .catch((error) => ElMessage.error(error.message || "删除失败"));
};

const handleLogout = async () => {
  try {
    await logout();
    ElMessage.success("已退出登录");
  } catch (error) {
    ElMessage.warning(error.message || "退出登录异常");
  } finally {
    router.replace("/");
  }
};

onMounted(async () => {
  document.addEventListener("click", handleDocumentClick);
  activeMenu.value = restoreActiveMenu();
  persistActiveMenu(activeMenu.value);
  try {
    await ensureMenuDataLoaded(activeMenu.value);
    connectWebSocket();
  } catch (error) {
    ElMessage.error(error.message || "管理后台数据加载失败");
  }
});

watch([userKeyword], () => {
  goUserPage(1);
  scheduleUsersReload();
});

watch(
  [
    itemTitleKeyword,
    itemCategoryKeyword,
    itemPriceKeyword,
    itemPublisherKeyword,
    itemStatus,
  ],
  () => {
    goItemPage(1);
    scheduleItemsReload();
  }
);

watch(
  [
    orderNoKeyword,
    orderBuyerKeyword,
    orderSellerKeyword,
    orderItemKeyword,
    orderState,
  ],
  () => {
    goOrderPage(1);
    scheduleOrdersReload();
  }
);

onBeforeUnmount(() => {
  document.removeEventListener("click", handleDocumentClick);
  if (userSearchTimer.value) clearTimeout(userSearchTimer.value);
  if (itemSearchTimer.value) clearTimeout(itemSearchTimer.value);
  if (orderSearchTimer.value) clearTimeout(orderSearchTimer.value);
});
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  padding: 22px;
  background: #f5f4fb;
  color: #1f2933;
}

.card {
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
}

.admin-sidebar {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.brand h1 {
  margin: 0;
  font-size: 30px;
  color: #7c3aed;
}

.brand p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 15px;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #1f2937;
  padding: 14px;
  text-align: left;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
}

.menu-item:hover {
  background: #f3f4f6;
}

.menu-item--active {
  background: #ede9fe;
  color: #6d28d9;
}

.menu-item__count {
  font-size: 14px;
  color: #6b7280;
}

.logout-btn {
  margin-top: auto;
  border: none;
  border-radius: 12px;
  background: #fee2e2;
  color: #b91c1c;
  padding: 12px 14px;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
}

.admin-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  padding: 20px;
}

.metric-card__label {
  margin: 0;
  color: #64748b;
  font-size: 15px;
}

.metric-card__value {
  margin: 8px 0;
  font-size: 36px;
  font-weight: 700;
  color: #0f172a;
}

.panel {
  padding: 20px;
}

.chart-panel,
.todo-panel {
  grid-column: span 3;
}

.status-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-bar-row {
  display: grid;
  grid-template-columns: 96px 1fr 44px;
  align-items: center;
  gap: 10px;
  font-size: 15px;
}

.status-bar-track {
  height: 10px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.status-bar-fill {
  height: 100%;
  background: linear-gradient(135deg, #7c3aed, #a78bfa);
}

.todo-panel ul {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.9;
  font-size: 15px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar--multi {
  align-items: center;
  flex-wrap: wrap;
}

.toolbar--grid4 .toolbar-input {
  flex: 1 1 calc(25% - 8px);
  min-width: 160px;
}

.toolbar-input,
.toolbar-select,
.notice-input {
  border: 1px solid #d8ccff;
  border-radius: 12px;
  outline: none;
  padding: 12px 14px;
  font-size: 15px;
  box-sizing: border-box;
}

.toolbar-input {
  flex: 1;
}

.toolbar-select {
  min-width: 140px;
}

.status-select {
  position: relative;
  min-width: 170px;
  width: 190px;
  flex: 0 0 auto;
}

.status-select__trigger {
  width: 100%;
  border: 1px solid #c8b7ff;
  border-radius: 16px;
  background: #ffffff;
  color: #1f2937;
  padding: 12px 14px;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.status-select__trigger:hover {
  border-color: #b59cff;
}

.status-select__trigger:focus,
.status-select__trigger:active {
  border-color: #a78bfa;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2);
}

.status-select__trigger:focus-visible {
  outline: 2px solid #c4b5fd;
  outline-offset: 1px;
}

.status-select__arrow {
  color: #7c3aed;
  transition: transform 0.2s ease;
}

.status-select__arrow.is-open {
  transform: rotate(180deg);
}

.status-select__menu {
  position: absolute;
  z-index: 20;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  margin: 0;
  padding: 6px 0;
  list-style: none;
  border: 1px solid #d8cbff;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 12px 26px rgba(76, 29, 149, 0.15);
  max-height: 240px;
  overflow-y: auto;
}

.status-select__option {
  padding: 12px 14px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 15px;
}

.status-select__option:hover {
  background: #f4efff;
  color: #5b21b6;
}

.status-select__option.is-selected {
  background: #e9defe;
  color: #5b21b6;
  font-weight: 600;
}

.table-wrap {
  overflow: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1160px;
}

.table th,
.table td {
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  padding: 14px 10px;
  font-size: 15px;
}

.table th {
  color: #64748b;
  font-weight: 600;
}

.pagination-bar {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-info {
  font-size: 14px;
  color: #64748b;
}

.pagination-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  background: #e2e8f0;
  color: #334155;
}

.pill--ok {
  background: #dcfce7;
  color: #166534;
}

.pill--warn {
  background: #fef3c7;
  color: #92400e;
}

.pill--danger {
  background: #fee2e2;
  color: #b91c1c;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.credit-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(17, 24, 39, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.credit-modal {
  width: min(560px, 100%);
  padding: 22px;
  border-radius: 18px;
}

.credit-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.credit-modal__header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 24px;
}

.credit-modal__subtitle {
  margin: 8px 0 12px;
  color: #6b7280;
  font-size: 14px;
}

.credit-modal__label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #374151;
}

.credit-modal__input {
  width: 100%;
}

.credit-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.text-btn,
.primary-btn {
  border: none;
  background: transparent;
  color: #6d28d9;
  cursor: pointer;
  font-size: 14px;
}

.text-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.text-btn--danger {
  color: #b91c1c;
}

.primary-btn {
  border-radius: 999px;
  padding: 10px 18px;
  color: #fff;
  font-weight: 600;
  background: linear-gradient(135deg, #7c3aed, #a78bfa);
  box-shadow: 0 6px 14px rgba(124, 58, 237, 0.33);
}

.support-layout {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 16px;
  min-height: calc(100vh - 64px);
}

.conversation-list {
  background: #f8fafc;
  border-radius: 14px;
  border: 1px solid #e6edf5;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.conversation-item {
  border: 1px solid transparent;
  border-radius: 14px;
  background: #ffffff;
  cursor: pointer;
  text-align: left;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.15s ease;
  min-height: 124px;
}

.conversation-item:hover {
  border-color: #d8ccff;
  box-shadow: 0 6px 14px rgba(76, 29, 149, 0.08);
}

.conversation-item strong {
  color: #0f172a;
  font-size: 18px;
}

.conversation-item span {
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.conversation-item__meta,
.conversation-item__preview {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}

.conversation-item__meta {
  -webkit-line-clamp: 1;
}

.conversation-item__preview {
  -webkit-line-clamp: 2;
}

.conversation-item--active {
  border-color: #cdb7ff;
  background: #f5f0ff;
  box-shadow: 0 8px 18px rgba(124, 58, 237, 0.15);
}

.conversation-main {
  border-radius: 14px;
  padding: 18px;
  background: #fff;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.support-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.conversation-main h3 {
  margin: 0;
}

.support-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
  color: #475569;
  font-size: 14px;
}

.report-content {
  margin: 0 0 10px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  color: #334155;
  font-size: 15px;
}

.chat-box {
  flex: 1;
  min-height: 360px;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 12px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.support-message {
  border-radius: 12px;
  background: #fff;
  padding: 12px 14px;
  max-width: 72%;
}

.support-message__image {
  max-width: min(320px, 100%);
  max-height: 260px;
  border-radius: 8px;
  display: block;
  margin-bottom: 6px;
}

.support-message__content {
  margin: 0;
  color: #1f2937;
  line-height: 1.65;
  word-break: break-word;
  font-size: 15px;
}

.support-message__time {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.support-message--admin {
  align-self: flex-end;
  background: #ede9fe;
}

.support-message--user {
  align-self: flex-start;
}

.support-image-input {
  display: none;
}

.support-image-ready {
  font-size: 13px;
  color: #16a34a;
  align-self: center;
}

.setting-block + .setting-block {
  margin-top: 22px;
}

.setting-block {
  border: none !important;
  box-shadow: none !important;
  background: transparent;
  padding: 0;
}

.setting-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.setting-header h3 {
  margin: 0;
  font-size: 22px;
}

.notice-editor {
  display: block;
}

.notice-input {
  display: block;
  width: 100%;
  max-width: 100%;
  min-height: 120px;
  resize: vertical;
  margin-bottom: 0;
}

.notice-list {
  list-style: none;
  padding: 12px;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
  background: #fff;
  border-radius: 10px;
}

.current-notice-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
}

.notice-item__content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.notice-item__text {
  margin: 0;
  color: #1f2937;
  word-break: break-word;
  font-size: 15px;
}

.notice-item__date {
  font-size: 13px;
  color: #6b7280;
}

.notice-empty {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.notice-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
}

@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-panel,
  .todo-panel {
    grid-column: span 2;
  }
}

@media (max-width: 960px) {
  .admin-page {
    grid-template-columns: 1fr;
  }

  .support-layout {
    grid-template-columns: 1fr;
  }

  .conversation-list {
    border-bottom: 1px solid #e5e7eb;
    padding-bottom: 10px;
  }

  .support-message {
    max-width: 88%;
  }

  .setting-header {
    flex-wrap: wrap;
  }

  .setting-header .primary-btn {
    align-self: flex-start;
  }
}
</style>
