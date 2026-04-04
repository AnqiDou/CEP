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
              'session-filter',
              'session-filter-btn',
              sessionFilter === 'all'
                ? ['session-filter-btn--active', 'session-filter--active']
                : '',
            ]"
            @click="sessionFilter = 'all'"
            type="button"
          >
            全部
          </button>
          <button
            :class="[
              'session-filter',
              'session-filter-btn',
              sessionFilter === 'unread'
                ? ['session-filter-btn--active', 'session-filter--active']
                : '',
            ]"
            @click="sessionFilter = 'unread'"
            type="button"
          >
            未读
          </button>
          <button
            :class="[
              'session-filter',
              'session-filter-btn',
              sessionFilter === 'read'
                ? ['session-filter-btn--active', 'session-filter--active']
                : '',
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

        <footer class="session-panel__foot">
          <div class="soft-mini-card">
            <p class="soft-mini-card__label">今日提醒</p>
            <p class="soft-mini-card__value">{{ totalUnread }} 条未读消息</p>
            <div class="soft-mini-card__bar">
              <span :style="{ width: unreadProgress + '%' }"></span>
            </div>
          </div>
        </footer>
      </aside>

      <section class="conversation-panel">
        <template v-if="hasActivePanel">
          <header class="conversation-topbar">
            <div class="conversation-peer">
              <div class="conversation-peer__head">
                <h3 class="conversation-peer__name">
                  {{ activePanelName }}
                </h3>
                <button
                  v-if="activeConversation"
                  class="conversation-home-btn"
                  type="button"
                  @click="goToSellerHome"
                >
                  TA的主页
                </button>
              </div>

              <div
                v-if="activeConversation"
                class="conversation-item-row"
                @click="goToItemDetail"
              >
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
              v-for="message in activePanelMessages"
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
                <div v-if="isReviewInvite(message)" class="review-invite-row">
                  <button
                    class="review-invite-btn"
                    type="button"
                    :disabled="message.reviewStatus === 'SUBMITTED'"
                    @click="goToReview(message.reviewOrderId)"
                  >
                    {{
                      message.reviewStatus === "SUBMITTED" ? "已评价" : "去评价"
                    }}
                  </button>
                </div>
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
          <div class="empty-illustration">
            <div class="empty-cloud"></div>
            <div class="empty-character">
              <span class="empty-character__head"></span>
              <span class="empty-character__body"></span>
            </div>
            <div class="empty-bubble"></div>
          </div>
          <h3 class="conversation-empty__title">尚未选择任何联系人</h3>
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
import {
  buildMessageWebSocketUrl,
  createOrGetDirectConversation,
  fetchConversationMessages,
  fetchMessageConversations,
  markConversationRead,
} from "../service/chat/chatApiService";
import data from "emoji-mart-vue-fast/data/all.json";
import { Picker, EmojiIndex } from "emoji-mart-vue-fast/src";
import "emoji-mart-vue-fast/css/emoji-mart.css";

const emojiIndex = new EmojiIndex(data);
const route = useRoute();
const router = useRouter();

const conversationList = ref([]);

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
const socketRef = ref(null);
const reconnectTimerRef = ref(null);
const isManualClose = ref(false);
const isLoadingConversations = ref(false);
const isLoadingMessages = ref(false);

const normalizeConversation = (raw) => ({
  id: String(raw?.conversationId ?? ""),
  sellerUserId:
    Number.isInteger(raw?.peerUserId) && raw.peerUserId > 0
      ? raw.peerUserId
      : null,
  sellerName:
    typeof raw?.peerName === "string" && raw.peerName.trim()
      ? raw.peerName.trim()
      : "校园用户",
  itemId: Number.isInteger(raw?.itemId) && raw.itemId > 0 ? raw.itemId : null,
  itemTitle:
    typeof raw?.itemTitle === "string" && raw.itemTitle.trim()
      ? raw.itemTitle.trim()
      : "校园闲置物品",
  itemImage:
    typeof raw?.itemImage === "string" && raw.itemImage.trim()
      ? raw.itemImage.trim()
      : "",
  unread: Number.isInteger(raw?.unread) ? Math.max(raw.unread, 0) : 0,
  lastMessage:
    typeof raw?.lastMessage === "string" && raw.lastMessage.trim()
      ? raw.lastMessage
      : "暂无消息",
  lastTime:
    typeof raw?.lastTime === "string" && raw.lastTime.trim()
      ? raw.lastTime.trim()
      : "",
  updatedAt: Date.now(),
  messages: [],
});

const normalizeMessage = (item) => ({
  id: item?.id ?? Date.now(),
  from: item?.from === "self" ? "self" : "other",
  text: typeof item?.text === "string" ? item.text : "",
  imageUrl: typeof item?.imageUrl === "string" ? item.imageUrl : "",
  time: typeof item?.time === "string" ? item.time : "",
  messageType:
    typeof item?.messageType === "string" ? item.messageType : "TEXT",
  reviewOrderId:
    Number.isInteger(item?.reviewOrderId) && item.reviewOrderId > 0
      ? item.reviewOrderId
      : null,
  reviewStatus: typeof item?.reviewStatus === "string" ? item.reviewStatus : "",
});

const upsertConversation = (normalizedConversation) => {
  if (!normalizedConversation?.id) return null;
  const list = conversationList.value;
  const index = list.findIndex((item) => item.id === normalizedConversation.id);
  if (index < 0) {
    list.unshift(normalizedConversation);
    return normalizedConversation;
  }

  const existing = list[index];
  const merged = {
    ...existing,
    ...normalizedConversation,
    messages: existing.messages || [],
  };
  list.splice(index, 1);
  list.unshift(merged);
  return merged;
};

const sortedConversations = computed(() => [...conversationList.value]);

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

const hasActivePanel = computed(() => Boolean(activeConversation.value));

const activePanelName = computed(() => {
  if (activeConversation.value) {
    return activeConversation.value.sellerName;
  }
  return "";
});

const activePanelMessages = computed(() => {
  if (activeConversation.value) {
    return activeConversation.value.messages;
  }
  return [];
});

const unreadTotal = computed(() =>
  conversationList.value.reduce(
    (total, conversation) => total + (conversation.unread || 0),
    0
  )
);

const totalUnread = computed(() => unreadTotal.value);

const unreadProgress = computed(() => Math.min(100, totalUnread.value * 10));

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
  loadMessages(conversationId);
  const current = conversationList.value.find(
    (conversation) => conversation.id === conversationId
  );
  if (current) {
    current.unread = 0;
  }
  markConversationRead(conversationId).catch(() => {});
};

const loadConversations = async () => {
  isLoadingConversations.value = true;
  try {
    const responseBody = await fetchMessageConversations("all");
    const list = Array.isArray(responseBody?.data) ? responseBody.data : [];
    conversationList.value = list.map(normalizeConversation);
    if (conversationList.value.length > 0) {
      const firstId = conversationList.value[0].id;
      selectedConversationId.value = firstId;
      await loadMessages(firstId);
    }
  } catch (error) {
    ElMessage.error(error.message || "加载消息列表失败");
  } finally {
    isLoadingConversations.value = false;
  }
};

const loadMessages = async (conversationId) => {
  const current = conversationList.value.find(
    (conversation) => conversation.id === conversationId
  );
  if (!current || current.messages.length > 0) {
    return;
  }

  isLoadingMessages.value = true;
  try {
    const responseBody = await fetchConversationMessages(conversationId);
    const messages = Array.isArray(responseBody?.data) ? responseBody.data : [];
    current.messages = messages.map(normalizeMessage);
  } catch (error) {
    ElMessage.error(error.message || "加载消息失败");
  } finally {
    isLoadingMessages.value = false;
  }
};

const isReviewInvite = (message) =>
  message?.messageType === "REVIEW_INVITE" &&
  Number.isInteger(message?.reviewOrderId) &&
  message.reviewOrderId > 0;

const goToReview = (orderId) => {
  if (!Number.isInteger(orderId) || orderId <= 0) {
    ElMessage.warning("评价订单参数无效");
    return;
  }

  router.push({
    name: "trade-review",
    query: {
      orderId: String(orderId),
    },
  });
};

const ensureConversationFromQuery = () => {
  const sellerUserIdRaw = Number(route.query.sellerUserId);
  const sellerUserId = Number.isInteger(sellerUserIdRaw) ? sellerUserIdRaw : 0;
  const itemIdRaw = Number(route.query.itemId);
  const itemId = Number.isInteger(itemIdRaw) ? itemIdRaw : 0;
  if (sellerUserId <= 0 || itemId <= 0) {
    return;
  }

  createOrGetDirectConversation({ peerUserId: sellerUserId, itemId })
    .then((responseBody) => {
      const raw = responseBody?.data;
      if (!raw) return;
      const conversation = upsertConversation(normalizeConversation(raw));
      if (conversation?.id) {
        selectConversation(conversation.id);
      }
    })
    .catch((error) => {
      ElMessage.error(error.message || "创建会话失败");
    });
};

watch(
  () => [
    route.query.sellerName,
    route.query.sellerUserId,
    route.query.itemTitle,
    route.query.itemId,
  ],
  () => {
    ensureConversationFromQuery();
  }
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
    query: current.sellerUserId
      ? {
          userId: String(current.sellerUserId),
        }
      : undefined,
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

  const socket = socketRef.value;
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    ElMessage.warning("消息通道连接中，请稍后重试");
    return;
  }

  socket.send(
    JSON.stringify({
      action: "SEND_MESSAGE",
      conversationId: Number(current.id),
      text,
      imageUrl: pendingImageUrl.value,
    })
  );

  draft.value = "";
  pendingImageUrl.value = "";
  showEmojiPanel.value = false;
};

const scheduleReconnect = () => {
  if (isManualClose.value || reconnectTimerRef.value) {
    return;
  }
  reconnectTimerRef.value = window.setTimeout(() => {
    reconnectTimerRef.value = null;
    connectWebSocket();
  }, 3000);
};

const appendMessageToConversation = (conversationId, message) => {
  const target = conversationList.value.find(
    (item) => item.id === conversationId
  );
  if (!target) return;
  const exists = target.messages.some(
    (item) => String(item.id) === String(message.id)
  );
  if (!exists) {
    target.messages.push(message);
  }
};

const handleWsMessageCreated = async (payload) => {
  const rawConversation = payload?.conversation;
  const rawMessage = payload?.message;
  if (!rawConversation?.conversationId || !rawMessage) {
    return;
  }

  const conversationId = String(rawConversation.conversationId);
  const conversation = upsertConversation(
    normalizeConversation(rawConversation)
  );
  if (!conversation) return;

  appendMessageToConversation(conversationId, normalizeMessage(rawMessage));

  if (selectedConversationId.value === conversationId) {
    conversation.unread = 0;
    await markConversationRead(conversationId).catch(() => {});
    scrollToBottom();
  }
};

const connectWebSocket = async () => {
  if (isManualClose.value) {
    return;
  }
  try {
    const wsUrl = await buildMessageWebSocketUrl();
    const socket = new WebSocket(wsUrl);
    socketRef.value = socket;

    socket.onopen = () => {
      if (reconnectTimerRef.value) {
        window.clearTimeout(reconnectTimerRef.value);
        reconnectTimerRef.value = null;
      }
    };

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data || "{}");
        const eventType = String(payload?.eventType || "").toUpperCase();
        if (eventType === "MESSAGE_CREATED") {
          handleWsMessageCreated(payload);
          return;
        }
        if (eventType === "ERROR") {
          ElMessage.warning(payload?.message || "消息发送失败");
        }
      } catch {
        // ignore invalid payload
      }
    };

    socket.onclose = () => {
      socketRef.value = null;
      scheduleReconnect();
    };

    socket.onerror = () => {
      scheduleReconnect();
    };
  } catch {
    scheduleReconnect();
  }
};

onMounted(() => {
  window.addEventListener("click", handleClickOutsideEmojiPanel);
  loadConversations().finally(() => {
    ensureConversationFromQuery();
  });
  connectWebSocket();
});

onBeforeUnmount(() => {
  window.removeEventListener("click", handleClickOutsideEmojiPanel);
  if (reconnectTimerRef.value) {
    window.clearTimeout(reconnectTimerRef.value);
    reconnectTimerRef.value = null;
  }
  isManualClose.value = true;
  if (socketRef.value) {
    socketRef.value.close();
    socketRef.value = null;
  }
  clearPendingImage();
});
</script>

<style scoped>
.chat-page {
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: #f5f7fb;
  color: #1f2937;
}

.chat-main {
  max-width: 1420px;
  margin: 0 auto;
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  background: transparent;
}

.card {
  box-shadow: 0 20px 44px rgba(140, 124, 240, 0.12);
}

.session-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  border-radius: 26px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(142, 126, 229, 0.14);
}

.session-panel__header {
  padding: 24px 18px 12px;
}

.session-panel__kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #8f82c8;
}

.session-panel__title {
  margin: 0;
  font-size: 40px;
  line-height: 1.1;
  font-weight: 800;
  color: #2f2952;
}

.session-panel__subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #8a83ab;
}

.session-filters {
  display: flex;
  gap: 8px;
  padding: 0 18px 14px;
}

.session-filter-btn {
  border: 1px solid #ddd7f7;
  border-radius: 999px;
  padding: 7px 13px;
  font-size: 13px;
  color: #7e71bf;
  background: #ffffff;
  cursor: pointer;
}

.session-filter-btn--active {
  background: #ffffff;
  border-color: #c7bbf9;
  color: #503f9b;
  font-weight: 700;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 12px 12px;
}

.session-item {
  padding: 10px;
  border-radius: 16px;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.session-item:hover {
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(140, 124, 240, 0.12);
}

.session-item--active {
  background: #ffffff;
}

.session-item__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #cbc0ff, #f0d9ff);
  color: #5e4eb6;
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
  color: #6c5eb8;
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
  color: #6f6a8f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item__badge {
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  padding: 0 6px;
  background: linear-gradient(150deg, #ff9fb8, #ff8fb2);
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
  color: #847ca8;
}

.session-panel__foot {
  padding: 0 16px 16px;
}

.soft-mini-card {
  background: linear-gradient(150deg, #f4f0ff, #fff6fb);
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 8px 18px rgba(140, 124, 240, 0.12);
}

.soft-mini-card__label {
  margin: 0;
  font-size: 12px;
  color: #8f87b1;
}

.soft-mini-card__value {
  margin: 6px 0 10px;
  font-size: 14px;
  color: #4a3f87;
  font-weight: 700;
}

.soft-mini-card__bar {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: #ece7ff;
  overflow: hidden;
}

.soft-mini-card__bar > span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(135deg, #9f8fff, #ffc4a8);
}

.conversation-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 26px;
  background: linear-gradient(155deg, #ffffff 0%, #fcfaff 100%);
  box-shadow: 0 12px 30px rgba(142, 126, 229, 0.14);
}

.conversation-topbar {
  padding: 16px 18px;
  border-bottom: 1px solid #ece8fb;
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
  color: #31295a;
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
  background: #f6f3ff;
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
  color: #6353b6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-item-sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: #8a82ab;
}

.conversation-home-btn {
  border: 1px solid #d9cffd;
  border-radius: 999px;
  background: #ffffff;
  color: #6252b5;
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
  background: #ffffff;
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
  border-radius: 14px;
  padding: 10px 12px;
  background: #eee8ff;
  color: #332f52;
}

.message-item--self .message-bubble {
  background: linear-gradient(135deg, #8f7fee, #ca93df);
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

.review-invite-row {
  margin-top: 8px;
}

.review-invite-btn {
  border: 1px solid #cfc1fd;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  color: #5f4eb0;
  background: #ffffff;
  cursor: pointer;
}

.review-invite-btn:disabled {
  border-color: #d1d5db;
  color: #6b7280;
  background: #f3f4f6;
  cursor: default;
}

.composer {
  border-top: 1px solid #ebe6fb;
  padding: 12px;
  background: #fffeff;
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
  border: 1px solid #d9d2f8;
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
  color: #53459a;
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
  border: 1px solid #d8cff9;
}

.remove-image-btn {
  border: none;
  background: #ffffff;
  color: #be5279;
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
  border: 1px solid #e1dafb;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fbf9ff;
  outline: none;
  font-size: 14px;
  line-height: 1.5;
}

.composer-input:focus {
  border-color: #b7a8f5;
  box-shadow: 0 0 0 4px rgba(165, 148, 241, 0.2);
}

.emoji-panel {
  position: absolute;
  left: 12px;
  bottom: calc(100% + 8px);
  z-index: 20;
  border: 1px solid #ddd5f8;
  border-radius: 10px;
  padding: 4px;
  background: #fbf9ff;
  box-shadow: 0 10px 24px rgba(142, 126, 229, 0.22);
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
  color: #7f78a1;
}

.empty-illustration {
  width: 180px;
  height: 130px;
  border-radius: 28px;
  position: relative;
  background: linear-gradient(145deg, #f2ebff, #fff4ef);
  box-shadow: 0 12px 30px rgba(140, 124, 240, 0.2);
}

.empty-cloud {
  position: absolute;
  width: 90px;
  height: 34px;
  left: 20px;
  top: 22px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
}

.empty-character {
  position: absolute;
  left: 40px;
  bottom: 18px;
  width: 72px;
  height: 72px;
}

.empty-character__head {
  position: absolute;
  left: 22px;
  top: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(145deg, #fff1d9, #ffdcb9);
}

.empty-character__body {
  position: absolute;
  left: 14px;
  bottom: 0;
  width: 46px;
  height: 40px;
  border-radius: 18px;
  background: linear-gradient(145deg, #9e8df6, #c39ae5);
}

.empty-bubble {
  position: absolute;
  right: 20px;
  top: 36px;
  width: 38px;
  height: 30px;
  border-radius: 16px;
  background: linear-gradient(145deg, #ffffff, #eee6ff);
  box-shadow: inset 0 0 0 1px #e0d7fb;
}

.conversation-empty__title {
  margin: 0;
  font-size: 32px;
  line-height: 1.2;
  color: #2c2452;
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
  background: #ffffff;
  color: #6252b5;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(140, 124, 240, 0.16);
  border: 1px solid #d9cffd;
}

@media (max-width: 1080px) {
  .chat-page {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .chat-main {
    grid-template-columns: 1fr;
    height: auto;
  }

  .session-panel,
  .conversation-panel {
    min-height: auto;
  }

  .session-panel {
    border-bottom: 1px solid #ece7fa;
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
