<template>
  <div class="chat-page">
    <main class="chat-main card">
      <aside class="session-panel">
        <header class="session-panel__header">
          <h2 class="session-panel__title">消息</h2>
        </header>

        <div class="session-filters">
          <button
            :class="[
              'session-filter-btn',
              sessionFilter === 'all' ? 'session-filter-btn--active' : '',
            ]"
            @click="sessionFilter = 'all'"
            type="button"
          >
            全部
          </button>
          <button
            :class="[
              'session-filter-btn',
              sessionFilter === 'unread' ? 'session-filter-btn--active' : '',
            ]"
            @click="sessionFilter = 'unread'"
            type="button"
          >
            未读
          </button>
          <button
            :class="[
              'session-filter-btn',
              sessionFilter === 'read' ? 'session-filter-btn--active' : '',
            ]"
            @click="sessionFilter = 'read'"
            type="button"
          >
            已读
          </button>
        </div>

        <div class="session-list">
          <article
            v-for="conversation in filteredConversations"
            :key="conversation.id"
            :class="[
              'session-item',
              selectedConversationId === conversation.id
                ? 'session-item--active'
                : '',
            ]"
            @click="selectConversation(conversation.id)"
          >
            <div class="session-item__avatar">
              {{ conversation.sellerName.slice(0, 1) }}
            </div>
            <div class="session-item__content">
              <div class="session-item__top">
                <h3 class="session-item__name">
                  {{ conversation.sellerName }}
                </h3>
                <time class="session-item__time">{{
                  conversation.lastTime
                }}</time>
              </div>
              <p class="session-item__item" :title="conversation.itemTitle">
                {{ conversation.itemTitle }}
              </p>
              <div class="session-item__bottom">
                <p
                  class="session-item__preview"
                  :title="conversation.lastMessage"
                >
                  {{ conversation.lastMessage }}
                </p>
                <span
                  v-if="conversation.unread > 0"
                  class="session-item__badge"
                >
                  {{ conversation.unread > 99 ? "99+" : conversation.unread }}
                </span>
              </div>
            </div>
          </article>

          <div v-if="filteredConversations.length === 0" class="session-empty">
            当前筛选下暂无会话
          </div>
        </div>
      </aside>

      <section class="conversation-panel">
        <template v-if="activeConversation">
          <header class="conversation-topbar">
            <div class="conversation-peer">
              <div class="conversation-peer__head">
                <h3 class="conversation-peer__name">
                  {{ activeConversation.sellerName }}
                </h3>
                <button
                  class="conversation-home-btn"
                  type="button"
                  @click="goToSellerHome"
                >
                  TA的主页
                </button>
              </div>

              <div class="conversation-item-row" @click="goToItemDetail">
                <img
                  v-if="activeConversation.itemImage"
                  :src="activeConversation.itemImage"
                  :alt="activeConversation.itemTitle"
                  class="conversation-item-thumb"
                />
                <div class="conversation-item-text">
                  <p class="conversation-item-title">
                    {{ activeConversation.itemTitle }}
                  </p>
                  <p class="conversation-item-sub">点击查看物品详情</p>
                </div>
              </div>
            </div>
          </header>

          <div ref="messageContainerRef" class="message-list">
            <article
              v-for="message in activeConversation.messages"
              :key="message.id"
              :class="[
                'message-item',
                message.from === 'self' ? 'message-item--self' : '',
              ]"
            >
              <div class="message-bubble">
                <img
                  v-if="message.imageUrl"
                  :src="message.imageUrl"
                  alt="聊天图片"
                  class="message-image"
                />
                <p v-if="message.text" class="message-text">
                  {{ message.text }}
                </p>
                <time class="message-time">{{ message.time }}</time>
              </div>
            </article>
          </div>

          <footer class="composer">
            <div class="composer-tools">
              <button
                ref="emojiToggleBtnRef"
                class="tool-btn tool-btn--emoji"
                type="button"
                @click="toggleEmojiPanel"
              >
                ☺
              </button>

              <label class="tool-btn tool-btn--upload">
                <el-icon><Picture /></el-icon>
                <input
                  class="hidden-file"
                  type="file"
                  accept="image/*"
                  @change="handleImageUpload"
                />
              </label>
            </div>

            <div v-if="pendingImageUrl" class="pending-image-row">
              <img
                :src="pendingImageUrl"
                alt="待发送图片"
                class="pending-image"
              />
              <button
                class="remove-image-btn"
                type="button"
                @click="clearPendingImage"
              >
                移除
              </button>
            </div>

            <div class="composer-input-row">
              <textarea
                v-model="draft"
                class="composer-input"
                placeholder="输入消息，Enter 发送，Shift + Enter 换行"
                @keydown.enter.exact.prevent="sendMessage"
              />
              <button class="primary-btn" type="button" @click="sendMessage">
                发送
              </button>
            </div>

            <div
              ref="emojiPanelRef"
              v-show="showEmojiPanel"
              class="emoji-panel"
            >
              <Picker
                :data="emojiIndex"
                :include="emojiCategories"
                set="apple"
                :native="true"
                :emojiSize="20"
                :perLine="8"
                :showSearch="false"
                :showPreview="false"
                :showCategories="false"
                :showSkinTones="false"
                @select="onSelectEmoji"
              />
            </div>
          </footer>
        </template>

        <section v-else class="conversation-empty">
          <div class="empty-illustration">💬</div>
          <h3 class="conversation-empty__title">尚未选择任何联系人</h3>
          <p class="conversation-empty__desc">快点左侧列表聊起来吧～</p>
        </section>
      </section>
    </main>
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
} from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { Picture } from "@element-plus/icons-vue";
import data from "emoji-mart-vue-fast/data/all.json";
import { Picker, EmojiIndex } from "emoji-mart-vue-fast/src";
import "emoji-mart-vue-fast/css/emoji-mart.css";

const emojiIndex = new EmojiIndex(data);
const route = useRoute();
const router = useRouter();

const createMockThumb = (label, color) =>
  `data:image/svg+xml;utf8,${encodeURIComponent(
    `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'>
      <defs>
        <linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>
          <stop offset='0%' stop-color='${color}'/>
          <stop offset='100%' stop-color='#e2e8f0'/>
        </linearGradient>
      </defs>
      <rect width='200' height='200' fill='url(#g)'/>
      <text x='100' y='108' text-anchor='middle' font-size='28' fill='#1e3a8a' font-family='Arial'>${label}</text>
    </svg>`
  )}`;

const conversationList = ref([
  {
    id: "conv-101",
    sellerName: "不吃奶酪的熙熙",
    itemId: 1,
    itemTitle: "投缘优惠团购券",
    itemImage: createMockThumb("团购", "#bfdbfe"),
    unread: 2,
    lastMessage: "在的，今晚 8 点前都可以当面交易。",
    lastTime: "18分钟前",
    updatedAt: Date.now() - 1000 * 60 * 18,
    messages: [
      {
        id: 1,
        from: "other",
        text: "你好，这个优惠券还在吗？",
        imageUrl: "",
        time: "10:18",
      },
      {
        id: 2,
        from: "self",
        text: "还在的，我在本部。",
        imageUrl: "",
        time: "10:19",
      },
      {
        id: 3,
        from: "other",
        text: "在的，今晚 8 点前都可以当面交易。",
        imageUrl: "",
        time: "10:20",
      },
    ],
  },
  {
    id: "conv-102",
    sellerName: "AI领航店",
    itemId: 2,
    itemTitle: "Claude code 教程合集",
    itemImage: createMockThumb("教程", "#93c5fd"),
    unread: 0,
    lastMessage: "支持小刀，资料发你预览图。",
    lastTime: "1天前",
    updatedAt: Date.now() - 1000 * 60 * 60 * 24,
    messages: [
      {
        id: 11,
        from: "other",
        text: "这套资料是最新版吗？",
        imageUrl: "",
        time: "昨天 12:12",
      },
      {
        id: 12,
        from: "other",
        text: "支持小刀，资料发你预览图。",
        imageUrl: "",
        time: "昨天 12:13",
      },
    ],
  },
  {
    id: "conv-103",
    sellerName: "闲小蜜",
    itemId: 205,
    itemTitle: "售后问卷礼包",
    itemImage: createMockThumb("礼包", "#a5b4fc"),
    unread: 1,
    lastMessage: "收到，辛苦给个评价～",
    lastTime: "03-11",
    updatedAt: Date.now() - 1000 * 60 * 60 * 24 * 5,
    messages: [
      {
        id: 21,
        from: "self",
        text: "礼包已收到，谢谢。",
        imageUrl: "",
        time: "03-11 15:06",
      },
      {
        id: 22,
        from: "other",
        text: "收到，辛苦给个评价～",
        imageUrl: "",
        time: "03-11 15:07",
      },
    ],
  },
]);

const sessionFilter = ref("all");
const selectedConversationId = ref(null);
const draft = ref("");
const showEmojiPanel = ref(false);
const emojiPanelRef = ref(null);
const emojiToggleBtnRef = ref(null);
const pendingImageUrl = ref("");
const messageContainerRef = ref(null);
const emojiCategories = [
  "people",
  "nature",
  "foods",
  "activity",
  "places",
  "objects",
  "symbols",
  "flags",
];
const replyTimers = new Set();

const sortedConversations = computed(() =>
  [...conversationList.value].sort(
    (first, second) => second.updatedAt - first.updatedAt
  )
);

const filteredConversations = computed(() => {
  if (sessionFilter.value === "unread") {
    return sortedConversations.value.filter(
      (conversation) => conversation.unread > 0
    );
  }

  if (sessionFilter.value === "read") {
    return sortedConversations.value.filter(
      (conversation) => conversation.unread === 0
    );
  }

  return sortedConversations.value;
});

const activeConversation = computed(() =>
  conversationList.value.find(
    (conversation) => conversation.id === selectedConversationId.value
  )
);

const getNowTime = () => {
  const date = new Date();
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${hour}:${minute}`;
};

const scrollToBottom = async () => {
  await nextTick();
  const container = messageContainerRef.value;
  if (!container) return;
  container.scrollTop = container.scrollHeight;
};

watch(
  () => activeConversation.value?.messages.length,
  () => {
    scrollToBottom();
  }
);

const selectConversation = (conversationId) => {
  selectedConversationId.value = conversationId;
  const current = conversationList.value.find(
    (conversation) => conversation.id === conversationId
  );
  if (current) {
    current.unread = 0;
  }
};

const ensureConversationFromQuery = () => {
  const sellerName = String(route.query.sellerName || "").trim();
  const itemTitle = String(route.query.itemTitle || "").trim();
  const itemIdRaw = Number(route.query.itemId);
  const itemId = Number.isFinite(itemIdRaw) && itemIdRaw > 0 ? itemIdRaw : null;

  if (!sellerName && !itemTitle) {
    return;
  }

  const existing = conversationList.value.find(
    (conversation) =>
      conversation.sellerName === sellerName &&
      conversation.itemTitle === itemTitle
  );

  if (existing) {
    selectConversation(existing.id);
    return;
  }

  const now = getNowTime();
  const id = `conv-${Date.now()}`;
  conversationList.value.unshift({
    id,
    sellerName: sellerName || "卖家同学",
    itemId,
    itemTitle: itemTitle || "校园闲置物品",
    itemImage: createMockThumb("闲置", "#dbeafe"),
    unread: 0,
    lastMessage: "你好，物品还在，欢迎咨询交易细节。",
    lastTime: "刚刚",
    updatedAt: Date.now(),
    messages: [
      {
        id: Date.now(),
        from: "other",
        text: "你好，物品还在，欢迎咨询交易细节。",
        imageUrl: "",
        time: now,
      },
    ],
  });
  selectConversation(id);
};

watch(
  () => [route.query.sellerName, route.query.itemTitle, route.query.itemId],
  () => {
    ensureConversationFromQuery();
  },
  { immediate: true }
);

const goToItemDetail = () => {
  const current = activeConversation.value;
  if (!current?.itemId) {
    ElMessage.warning("当前会话暂无关联物品");
    return;
  }

  const resolved = router.resolve({
    name: "item-detail",
    params: {
      id: current.itemId,
    },
  });
  window.open(resolved.href, "_blank");
};

const goToSellerHome = () => {
  const current = activeConversation.value;
  if (!current?.sellerName) {
    ElMessage.warning("当前会话暂无关联卖家");
    return;
  }

  const resolved = router.resolve({
    name: "other-profile",
    params: {
      name: current.sellerName,
    },
  });
  window.open(resolved.href, "_blank");
};

const clearPendingImage = () => {
  if (pendingImageUrl.value) {
    URL.revokeObjectURL(pendingImageUrl.value);
  }
  pendingImageUrl.value = "";
};

const handleImageUpload = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;
  clearPendingImage();
  pendingImageUrl.value = URL.createObjectURL(file);
  event.target.value = "";
};

const toggleEmojiPanel = () => {
  showEmojiPanel.value = !showEmojiPanel.value;
};

const onSelectEmoji = (emoji) => {
  const value = emoji?.native || "";
  if (!value) return;
  draft.value += value;
};

const handleClickOutsideEmojiPanel = (event) => {
  if (!showEmojiPanel.value) return;
  const panelElement = emojiPanelRef.value;
  const toggleElement = emojiToggleBtnRef.value;
  const clickTarget = event.target;

  if (
    panelElement?.contains(clickTarget) ||
    toggleElement?.contains(clickTarget)
  ) {
    return;
  }

  showEmojiPanel.value = false;
};

const sendMessage = () => {
  const current = activeConversation.value;
  if (!current) {
    ElMessage.warning("请先选择一个会话");
    return;
  }

  const text = draft.value.trim();
  const hasImage = Boolean(pendingImageUrl.value);

  if (!text && !hasImage) {
    ElMessage.warning("请输入消息或选择图片");
    return;
  }

  const now = getNowTime();
  current.messages.push({
    id: Date.now(),
    from: "self",
    text,
    imageUrl: pendingImageUrl.value,
    time: now,
  });
  current.lastMessage = text || "[图片]";
  current.lastTime = now;
  current.updatedAt = Date.now();
  current.unread = 0;
  draft.value = "";
  pendingImageUrl.value = "";
  showEmojiPanel.value = false;

  const timer = window.setTimeout(() => {
    const replyText = "收到，我这边看到了，今晚可以在二食堂门口面交。";
    const replyNow = getNowTime();
    current.messages.push({
      id: Date.now() + 1,
      from: "other",
      text: replyText,
      imageUrl: "",
      time: replyNow,
    });
    current.lastMessage = replyText;
    current.lastTime = replyNow;
    current.updatedAt = Date.now();

    replyTimers.delete(timer);
  }, 700);

  replyTimers.add(timer);
};

onMounted(() => {
  window.addEventListener("click", handleClickOutsideEmojiPanel);
});

onBeforeUnmount(() => {
  window.removeEventListener("click", handleClickOutsideEmojiPanel);
  replyTimers.forEach((timer) => {
    window.clearTimeout(timer);
  });
  replyTimers.clear();
  clearPendingImage();
});
</script>

<style scoped>
.chat-page {
  min-height: 100vh;
  background: #f5f7fb;
  color: #1f2937;
}

.chat-main {
  max-width: 1280px;
  margin: 0 auto;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  background: #ffffff;
}

.card {
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.session-panel {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  border-right: 1px solid #e5e7eb;
  background: #f8fbff;
}

.session-panel__header {
  padding: 20px 16px 10px;
}

.session-panel__title {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
  font-weight: 800;
  color: #111827;
}

.session-filters {
  display: flex;
  gap: 8px;
  padding: 0 16px 12px;
}

.session-filter-btn {
  border: 1px solid #dbeafe;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  color: #1d4ed8;
  background: #ffffff;
  cursor: pointer;
}

.session-filter-btn--active {
  background: #dbeafe;
  border-color: #93c5fd;
  font-weight: 700;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 10px 12px;
}

.session-item {
  padding: 8px;
  border-radius: 12px;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 8px;
  cursor: pointer;
  transition: background 0.12s ease;
}

.session-item:hover {
  background: #edf4ff;
}

.session-item--active {
  background: #e5edff;
}

.session-item__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #bfdbfe, #93c5fd);
  color: #1d4ed8;
  font-size: 24px;
  font-weight: 700;
}

.session-item__top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.session-item__name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item__time {
  flex-shrink: 0;
  font-size: 12px;
  color: #6b7280;
}

.session-item__item {
  margin: 2px 0 0;
  font-size: 12px;
  color: #2563eb;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item__bottom {
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-item__preview {
  margin: 0;
  flex: 1;
  font-size: 12px;
  color: #4b5563;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item__badge {
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  padding: 0 6px;
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.session-empty {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}

.conversation-panel {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.conversation-topbar {
  padding: 14px 18px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.conversation-peer {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.conversation-peer__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.conversation-peer__name {
  margin: 0;
  font-size: 22px;
  line-height: 1.2;
}

.conversation-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 10px;
  padding: 4px 6px;
  cursor: pointer;
}

.conversation-item-row:hover {
  background: #f8fbff;
}

.conversation-item-thumb {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #dbeafe;
}

.conversation-item-text {
  min-width: 0;
}

.conversation-item-title {
  margin: 0;
  font-size: 15px;
  color: #2563eb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-item-sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.conversation-home-btn {
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 7px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px;
  background: #fdfefe;
}

.message-item {
  display: flex;
  margin-bottom: 10px;
}

.message-item--self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(70%, 620px);
  border-radius: 12px;
  padding: 10px 12px;
  background: #e5edff;
  color: #1f2937;
}

.message-item--self .message-bubble {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
}

.message-image {
  display: block;
  width: min(260px, 100%);
  border-radius: 10px;
  margin-bottom: 6px;
}

.message-text {
  margin: 0;
  line-height: 1.55;
  font-size: 14px;
  white-space: pre-wrap;
}

.message-time {
  margin-top: 6px;
  display: block;
  font-size: 11px;
  opacity: 0.76;
}

.composer {
  border-top: 1px solid #dbeafe;
  padding: 12px;
  background: #ffffff;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.composer-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-btn {
  width: 34px;
  height: 34px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #ffffff;
  cursor: pointer;
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.tool-btn--emoji {
  font-size: 22px;
  color: #111827;
}

.tool-btn--upload {
  position: relative;
  overflow: hidden;
}

.hidden-file {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.pending-image-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pending-image {
  width: 74px;
  height: 74px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #dbeafe;
}

.remove-image-btn {
  border: none;
  background: #fee2e2;
  color: #b91c1c;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}

.composer-input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.composer-input {
  flex: 1;
  min-height: 88px;
  max-height: 180px;
  resize: vertical;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  padding: 10px 12px;
  background: #f8fbff;
  outline: none;
  font-size: 14px;
  line-height: 1.5;
}

.composer-input:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.16);
}

.emoji-panel {
  position: absolute;
  left: 12px;
  bottom: calc(100% + 8px);
  z-index: 20;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  padding: 4px;
  background: #f8fbff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.16);
  max-width: 260px;
}

.emoji-panel :deep(.emoji-mart),
.emoji-panel :deep(.emoji-mart-scroll) {
  width: 252px !important;
}

.emoji-panel :deep(.emoji-mart-preview),
.emoji-panel :deep(.emoji-mart-bar),
.emoji-panel :deep(.emoji-mart-category-label),
.emoji-panel :deep(.emoji-mart-scroll .emoji-mart-category:first-of-type) {
  display: none;
}

.conversation-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  color: #6b7280;
}

.empty-illustration {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff7d1;
  font-size: 42px;
}

.conversation-empty__title {
  margin: 0;
  font-size: 32px;
  line-height: 1.2;
  color: #111827;
}

.conversation-empty__desc {
  margin: 0;
  font-size: 20px;
}

.primary-btn {
  border: none;
  border-radius: 999px;
  padding: 11px 22px;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.33);
}

@media (max-width: 1080px) {
  .chat-main {
    grid-template-columns: 1fr;
  }

  .session-panel,
  .conversation-panel {
    min-height: auto;
  }

  .session-panel {
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
  }

  .conversation-empty__title {
    font-size: 26px;
  }

  .conversation-empty__desc {
    font-size: 16px;
  }
}

@media (max-width: 760px) {
  .conversation-topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .message-bubble {
    max-width: 90%;
  }
}
</style>
