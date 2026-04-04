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
          @click="activeMenu = item.key"
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
          <p class="metric-card__desc">{{ metric.desc }}</p>
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
            <li>今日待审核商品：{{ pendingItemCount }} 件</li>
            <li>待处理异常订单：{{ abnormalOrderCount }} 单</li>
            <li>待回复客服会话：{{ pendingConversationCount }} 条</li>
          </ul>
        </article>
      </section>

      <section v-else-if="activeMenu === 'users'" class="card panel">
        <div class="toolbar">
          <input
            v-model.trim="userKeyword"
            class="toolbar-input"
            placeholder="搜索昵称/邮箱/电话"
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
                <th>状态</th>
                <th>发布商品</th>
                <th>订单数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in filteredUsers" :key="user.id">
                <td>{{ user.name }}</td>
                <td>{{ user.phone || "-" }}</td>
                <td>{{ user.email || "-" }}</td>
                <td>{{ user.registeredAt }}</td>
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
      </section>

      <section v-else-if="activeMenu === 'items'" class="card panel">
        <div class="toolbar toolbar--multi">
          <input
            v-model.trim="itemKeyword"
            class="toolbar-input"
            placeholder="搜索商品名/分类"
          />
          <select v-model="itemStatus" class="toolbar-select">
            <option value="all">全部状态</option>
            <option value="pending">待审核</option>
            <option value="online">上架中</option>
            <option value="offline">已下架</option>
          </select>
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
              <tr v-for="item in filteredItems" :key="item.id">
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
      </section>

      <section v-else-if="activeMenu === 'orders'" class="card panel">
        <div class="toolbar toolbar--multi">
          <input
            v-model.trim="orderKeyword"
            class="toolbar-input"
            placeholder="搜索订单号/买家/卖家/商品"
          />
          <select v-model="orderState" class="toolbar-select">
            <option value="all">全部状态</option>
            <option value="pending-pay">待付款</option>
            <option value="completed">已完成</option>
            <option value="cancelled">已取消</option>
          </select>
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
              <tr v-for="order in filteredOrders" :key="order.orderNo">
                <td>{{ order.orderNo }}</td>
                <td>{{ order.itemTitle }}</td>
                <td>{{ order.buyer }}</td>
                <td>{{ order.seller }}</td>
                <td>￥{{ order.amount }}</td>
                <td>
                  <span class="pill" :class="statusClass(order.status)">{{
                    statusText(order.status)
                  }}</span>
                </td>
                <td>
                  <button class="text-btn" @click="markAbnormalHandled(order)">
                    处理异常
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
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
              <strong>{{ session.title }}</strong>
              <span
                >{{ session.reporterName || "用户" }} ·
                {{ mapReportTypeText(session.reportType) }}</span
              >
              <span>{{ session.preview }}</span>
            </button>
          </aside>

          <section class="conversation-main">
            <h3>{{ currentConversation?.title || "请选择会话" }}</h3>
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
              <p
                v-for="msg in currentConversation?.messages || []"
                :key="msg.id"
              >
                <strong>{{ msg.from }}：</strong>{{ msg.content }}
              </p>
            </div>
            <div class="toolbar">
              <input
                v-model.trim="supportReply"
                class="toolbar-input"
                placeholder="输入处理意见"
              />
              <button class="primary-btn" @click="appendReply">发送</button>
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
          </section>
        </div>
      </section>

      <section v-else class="card panel">
        <div class="setting-block">
          <h3>发布平台公告</h3>
          <textarea
            v-model="newNotice"
            class="notice-input"
            placeholder="请输入公告内容"
          ></textarea>
          <button class="primary-btn" @click="publishNotice">发布公告</button>
        </div>

        <div class="setting-block">
          <h3>历史公告</h3>
          <ul class="notice-list">
            <li v-for="notice in notices" :key="notice.id">
              <span>{{ notice.content }}</span>
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { logout } from "../service/common/authSessionService";
import { buildMessageWebSocketUrl } from "../service/chat/chatApiService";
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
  handleAdminOrderAbnormal,
  offlineAdminItem,
  replyAdminConversation,
  updateAdminConversationStatus,
  updateAdminUserStatus,
} from "../service/admin/adminApiService";

const router = useRouter();
const loading = ref(false);
const socketRef = ref(null);
const reconnectTimerRef = ref(null);
const isManualClose = ref(false);

const navItems = ref([
  { key: "dashboard", label: "仪表盘", count: "00" },
  { key: "users", label: "用户管理", count: "00" },
  { key: "items", label: "商品管理", count: "00" },
  { key: "orders", label: "订单管理", count: "00" },
  { key: "support", label: "客服模块", count: "00" },
  { key: "settings", label: "系统设置", count: "00" },
]);

const activeMenu = ref("dashboard");

const metrics = ref([]);
const orderStateStats = ref([]);
const dashboardRaw = ref({
  pendingItemCount: 0,
  abnormalOrderCount: 0,
  pendingConversationCount: 0,
});

const users = ref([]);
const userKeyword = ref("");
const filteredUsers = computed(() => {
  if (!userKeyword.value) return users.value;
  const keyword = userKeyword.value.toLowerCase();
  return users.value.filter(
    (item) =>
      String(item.name || "")
        .toLowerCase()
        .includes(keyword) ||
      String(item.phone || "").includes(keyword) ||
      String(item.email || "")
        .toLowerCase()
        .includes(keyword)
  );
});

const items = ref([]);
const itemKeyword = ref("");
const itemStatus = ref("all");
const filteredItems = computed(() =>
  items.value.filter((item) => {
    const hitKeyword =
      !itemKeyword.value ||
      String(item.title || "").includes(itemKeyword.value) ||
      String(item.category || "").includes(itemKeyword.value);
    const hitStatus =
      itemStatus.value === "all" || item.status === itemStatus.value;
    return hitKeyword && hitStatus;
  })
);

const orders = ref([]);
const orderKeyword = ref("");
const orderState = ref("all");
const filteredOrders = computed(() =>
  orders.value.filter((order) => {
    const hitKeyword =
      !orderKeyword.value ||
      String(order.orderNo || "").includes(orderKeyword.value) ||
      String(order.itemTitle || "").includes(orderKeyword.value) ||
      String(order.buyer || "").includes(orderKeyword.value) ||
      String(order.seller || "").includes(orderKeyword.value);
    const hitStatus =
      orderState.value === "all" || order.status === orderState.value;
    return hitKeyword && hitStatus;
  })
);

const conversations = ref([]);
const activeConversationId = ref(null);
const supportReply = ref("");
const currentConversation = computed(() =>
  conversations.value.find(
    (session) => session.id === activeConversationId.value
  )
);

const notices = ref([]);
const newNotice = ref("");

const pendingItemCount = computed(
  () => dashboardRaw.value.pendingItemCount || 0
);
const abnormalOrderCount = computed(
  () => dashboardRaw.value.abnormalOrderCount || 0
);
const pendingConversationCount = computed(
  () => dashboardRaw.value.pendingConversationCount || 0
);

const normalizeDate = (value) => {
  if (!value) return "-";
  return String(value).slice(0, 10);
};

const moneyText = (value) => {
  const number = Number(value || 0);
  if (Number.isNaN(number)) return "0.00";
  return number.toFixed(2);
};

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
      desc: `总用户 ${data.totalUsers ?? 0}`,
    },
    {
      label: "总用户数",
      value: String(data.totalUsers ?? 0),
      desc: "累计注册用户",
    },
    {
      label: "今日新增商品",
      value: String(data.todayNewItems ?? 0),
      desc: `待审核 ${data.pendingItemCount ?? 0} 件`,
    },
    {
      label: "总商品数",
      value: String(data.totalItems ?? 0),
      desc: "非删除商品总量",
    },
    {
      label: "今日订单数",
      value: String(data.todayOrders ?? 0),
      desc: `异常订单 ${data.abnormalOrderCount ?? 0} 单`,
    },
    {
      label: "今日销售额",
      value: `￥${moneyText(data.todaySales)}`,
      desc: "按已支付订单统计",
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
  const { data } = await fetchAdminUsers("");
  users.value = (data || []).map((item) => ({
    ...item,
    registeredAt: normalizeDate(item.registeredAt),
  }));
  updateNavCount();
};

const loadItems = async () => {
  const { data } = await fetchAdminItems({ keyword: "", status: "all" });
  items.value = data || [];
  updateNavCount();
};

const loadOrders = async () => {
  const { data } = await fetchAdminOrders({ keyword: "", status: "all" });
  orders.value = data || [];
  updateNavCount();
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

const loadAll = async () => {
  if (loading.value) return;
  loading.value = true;
  try {
    await Promise.all([
      loadDashboard(),
      loadUsers(),
      loadItems(),
      loadOrders(),
      loadConversations(),
      loadNotices(),
    ]);
  } finally {
    loading.value = false;
  }
};

const statusText = (status) => {
  const map = {
    pending: "待审核",
    online: "上架中",
    offline: "已下架",
    "pending-pay": "待付款",
    completed: "已完成",
    cancelled: "已取消",
  };
  return map[status] || "未知";
};

const statusClass = (status) => {
  if (status === "offline" || status === "cancelled") return "pill--danger";
  if (status === "pending" || status === "pending-pay") return "pill--warn";
  if (status === "completed" || status === "online") return "pill--ok";
  return "";
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

const markAbnormalHandled = (order) => {
  handleAdminOrderAbnormal(order.orderNo)
    .then(async () => {
      await Promise.all([loadOrders(), loadDashboard()]);
      ElMessage.success("异常订单已处理");
    })
    .catch((error) => ElMessage.error(error.message || "处理失败"));
};

const appendReply = () => {
  if (!supportReply.value) {
    ElMessage.warning("请输入回复内容");
    return;
  }
  const target = currentConversation.value;
  if (!target) return;
  replyAdminConversation(target.id, supportReply.value)
    .then(async () => {
      supportReply.value = "";
      await Promise.all([loadConversations(), loadDashboard()]);
      ElMessage.success("回复已发送");
    })
    .catch((error) => ElMessage.error(error.message || "发送失败"));
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
  try {
    await loadAll();
    connectWebSocket();
  } catch (error) {
    ElMessage.error(error.message || "管理后台数据加载失败");
  }
});
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  background: #f5f7fb;
  color: #1f2933;
}

.card {
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
}

.admin-sidebar {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.brand h1 {
  margin: 0;
  font-size: 24px;
  color: #2563eb;
}

.brand p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #1f2937;
  padding: 12px;
  text-align: left;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  cursor: pointer;
}

.menu-item:hover {
  background: #f3f4f6;
}

.menu-item--active {
  background: #eef4ff;
  color: #1d4ed8;
}

.menu-item__count {
  font-size: 12px;
  color: #6b7280;
}

.logout-btn {
  margin-top: auto;
  border: none;
  border-radius: 10px;
  background: #fee2e2;
  color: #b91c1c;
  padding: 10px 12px;
  font-weight: 600;
  cursor: pointer;
}

.admin-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 16px;
}

.metric-card__label {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.metric-card__value {
  margin: 8px 0;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.metric-card__desc {
  margin: 0;
  color: #1d4ed8;
  font-size: 13px;
}

.panel {
  padding: 16px;
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
  grid-template-columns: 80px 1fr 36px;
  align-items: center;
  gap: 10px;
}

.status-bar-track {
  height: 8px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.status-bar-fill {
  height: 100%;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}

.todo-panel ul {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.9;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.toolbar--multi {
  justify-content: space-between;
}

.toolbar-input,
.toolbar-select,
.notice-input {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  outline: none;
  padding: 10px 12px;
  font-size: 14px;
}

.toolbar-input {
  flex: 1;
}

.toolbar-select {
  min-width: 140px;
}

.table-wrap {
  overflow: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 980px;
}

.table th,
.table td {
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  padding: 12px 8px;
  font-size: 14px;
}

.table th {
  color: #64748b;
  font-weight: 600;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
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
  gap: 8px;
}

.text-btn,
.primary-btn {
  border: none;
  background: transparent;
  color: #1d4ed8;
  cursor: pointer;
  font-size: 13px;
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
  padding: 8px 16px;
  color: #fff;
  font-weight: 600;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.33);
}

.support-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 14px;
}

.conversation-list {
  border-right: 1px solid #e5e7eb;
  padding-right: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conversation-item {
  border: none;
  border-radius: 10px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conversation-item strong {
  color: #0f172a;
}

.conversation-item span {
  color: #64748b;
  font-size: 12px;
}

.conversation-item--active {
  background: #eef4ff;
}

.conversation-main h3 {
  margin-top: 0;
}

.support-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
  color: #475569;
  font-size: 13px;
}

.report-content {
  margin: 0 0 10px;
  padding: 10px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  color: #334155;
}

.chat-box {
  min-height: 220px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 12px;
  background: #fcfdff;
}

.setting-block + .setting-block {
  margin-top: 22px;
}

.notice-input {
  display: block;
  width: 100%;
  min-height: 90px;
  resize: vertical;
  margin-bottom: 10px;
}

.notice-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
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
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
    padding-right: 0;
    padding-bottom: 10px;
  }
}
</style>
