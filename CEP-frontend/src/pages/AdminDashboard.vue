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
            placeholder="搜索昵称/手机号"
          />
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>昵称</th>
                <th>手机号</th>
                <th>注册时间</th>
                <th>账号状态</th>
                <th>发布商品</th>
                <th>订单数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in filteredUsers" :key="user.id">
                <td>{{ user.name }}</td>
                <td>{{ user.phone }}</td>
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
            placeholder="搜索商品名称/分类"
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
            placeholder="搜索订单号/用户/商品"
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
              <span>{{ session.preview }}</span>
            </button>
          </aside>
          <section class="conversation-main">
            <h3>{{ currentConversation?.title || "请选择会话" }}</h3>
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
                placeholder="输入处理意见（仅前端演示）"
              />
              <button class="primary-btn" @click="appendReply">发送</button>
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
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { logout } from "../service/common/authSessionService";

const router = useRouter();
const navItems = ref([
  { key: "dashboard", label: "仪表盘", count: "06" },
  { key: "users", label: "用户管理", count: "128" },
  { key: "items", label: "商品管理", count: "96" },
  { key: "orders", label: "订单管理", count: "53" },
  { key: "support", label: "客服模块", count: "12" },
  { key: "settings", label: "系统设置", count: "03" },
]);

const activeMenu = ref("dashboard");

const metrics = ref([
  { label: "今日新增用户", value: "24", desc: "较昨日 +8%" },
  { label: "总用户数", value: "1,286", desc: "累计注册用户" },
  { label: "今日新增商品", value: "31", desc: "待审核 8 件" },
  { label: "总商品数", value: "5,402", desc: "在架商品 2,109 件" },
  { label: "今日订单数", value: "17", desc: "异常订单 2 单" },
  { label: "销售额", value: "￥8,760", desc: "今日成交额" },
]);

const orderStateStats = ref([
  { label: "待付款", count: 13, percent: 22 },
  { label: "已完成", count: 32, percent: 54 },
  { label: "已取消", count: 14, percent: 24 },
]);

const users = ref([
  {
    id: "u-1",
    name: "沐风同学",
    phone: "13800138000",
    registeredAt: "2026-03-01",
    disabled: false,
    itemCount: 5,
    orderCount: 7,
  },
  {
    id: "u-2",
    name: "星河",
    phone: "18612345678",
    registeredAt: "2026-02-21",
    disabled: true,
    itemCount: 11,
    orderCount: 2,
  },
  {
    id: "u-3",
    name: "云间",
    phone: "13911112222",
    registeredAt: "2026-02-10",
    disabled: false,
    itemCount: 3,
    orderCount: 4,
  },
]);
const userKeyword = ref("");

const filteredUsers = computed(() => {
  if (!userKeyword.value) return users.value;
  const keyword = userKeyword.value.toLowerCase();
  return users.value.filter(
    (item) =>
      item.name.toLowerCase().includes(keyword) || item.phone.includes(keyword)
  );
});

const items = ref([
  {
    id: "g-1",
    title: "九成新机械键盘",
    category: "数码",
    price: 199,
    owner: "沐风同学",
    status: "pending",
  },
  {
    id: "g-2",
    title: "高数教材",
    category: "图书",
    price: 35,
    owner: "星河",
    status: "online",
  },
  {
    id: "g-3",
    title: "违规广告测试商品",
    category: "其他",
    price: 1,
    owner: "云间",
    status: "offline",
  },
]);
const itemKeyword = ref("");
const itemStatus = ref("all");

const filteredItems = computed(() => {
  return items.value.filter((item) => {
    const matchesKeyword =
      !itemKeyword.value ||
      item.title.includes(itemKeyword.value) ||
      item.category.includes(itemKeyword.value);
    const matchesState =
      itemStatus.value === "all" || item.status === itemStatus.value;
    return matchesKeyword && matchesState;
  });
});

const orders = ref([
  {
    orderNo: "CEP202603280001",
    itemTitle: "九成新机械键盘",
    buyer: "阿晨",
    seller: "沐风同学",
    amount: 199,
    status: "pending-pay",
  },
  {
    orderNo: "CEP202603280002",
    itemTitle: "高数教材",
    buyer: "星河",
    seller: "云间",
    amount: 35,
    status: "completed",
  },
  {
    orderNo: "CEP202603280003",
    itemTitle: "校园路由器",
    buyer: "小北",
    seller: "星河",
    amount: 88,
    status: "cancelled",
  },
]);

const orderKeyword = ref("");
const orderState = ref("all");

const filteredOrders = computed(() => {
  return orders.value.filter((order) => {
    const matchesKeyword =
      !orderKeyword.value ||
      order.orderNo.includes(orderKeyword.value) ||
      order.itemTitle.includes(orderKeyword.value) ||
      order.buyer.includes(orderKeyword.value) ||
      order.seller.includes(orderKeyword.value);
    const matchesState =
      orderState.value === "all" || order.status === orderState.value;
    return matchesKeyword && matchesState;
  });
});

const conversations = ref([
  {
    id: "c-1",
    title: "订单 CEP202603280001 纠纷",
    preview: "买家反馈商品描述不一致",
    messages: [
      { id: 1, from: "买家", content: "收到后发现轴体不是描述里的型号。" },
      { id: 2, from: "卖家", content: "可以协商部分退款。" },
    ],
  },
  {
    id: "c-2",
    title: "违规商品举报",
    preview: "用户举报疑似引流信息",
    messages: [
      { id: 1, from: "举报人", content: "商品详情中有外链联系方式。" },
    ],
  },
]);

const activeConversationId = ref("c-1");
const supportReply = ref("");

const currentConversation = computed(() =>
  conversations.value.find(
    (session) => session.id === activeConversationId.value
  )
);

const notices = ref([
  { id: "n-1", content: "请勿线下绕过平台交易，谨防诈骗。" },
  { id: "n-2", content: "本周将进行系统升级，部分功能短时维护。" },
]);
const newNotice = ref("");

const pendingItemCount = computed(
  () => items.value.filter((item) => item.status === "pending").length
);
const abnormalOrderCount = computed(
  () => orders.value.filter((order) => order.status !== "completed").length
);
const pendingConversationCount = computed(() => conversations.value.length);

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

const toggleUserState = (user) => {
  user.disabled = !user.disabled;
  ElMessage.success(user.disabled ? "用户已禁用" : "用户已解封");
};

const removeUser = (id) => {
  users.value = users.value.filter((user) => user.id !== id);
  ElMessage.success("用户已删除");
};

const approveItem = (item) => {
  if (item.status !== "pending") return;
  item.status = "online";
  ElMessage.success("商品审核通过");
};

const forceOffline = (item) => {
  item.status = "offline";
  ElMessage.success("商品已强制下架");
};

const deleteItem = (id) => {
  items.value = items.value.filter((item) => item.id !== id);
  ElMessage.success("商品已删除");
};

const markAbnormalHandled = (order) => {
  if (order.status !== "completed") {
    order.status = "completed";
  }
  ElMessage.success("异常订单已标记处理完成");
};

const appendReply = () => {
  if (!supportReply.value) {
    ElMessage.warning("请输入回复内容");
    return;
  }
  const target = currentConversation.value;
  if (!target) return;
  target.messages.push({
    id: Date.now(),
    from: "管理员",
    content: supportReply.value,
  });
  supportReply.value = "";
  ElMessage.success("已发送处理意见");
};

const publishNotice = () => {
  if (!newNotice.value.trim()) {
    ElMessage.warning("公告内容不能为空");
    return;
  }
  notices.value.unshift({
    id: `n-${Date.now()}`,
    content: newNotice.value.trim(),
  });
  newNotice.value = "";
  ElMessage.success("公告已发布");
};

const deleteNotice = (id) => {
  notices.value = notices.value.filter((notice) => notice.id !== id);
  ElMessage.success("公告已删除");
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
  min-width: 880px;
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
