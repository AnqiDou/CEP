<template>
  <div class="support-page">
    <main class="support-main card">
      <header class="support-header">
        <div>
          <h2>在线客服</h2>
          <p class="support-subtitle">有问题随时留言，我们会尽快回复</p>
        </div>
      </header>

      <section ref="messageListRef" class="message-list">
        <p v-if="loading" class="state-text">加载中...</p>
        <p v-else-if="messages.length === 0" class="state-text">
          暂无消息，发送后将创建客服会话
        </p>

        <article
          v-for="msg in messages"
          :key="msg.key"
          :class="[
            'message-item',
            msg.from === 'self' ? 'message-item--self' : '',
          ]"
        >
          <div class="message-bubble">
            <img
              v-if="getMessageImageUrl(msg.content)"
              :src="getMessageImageUrl(msg.content)"
              alt="消息图片"
              class="message-image"
            />
            <p v-if="getMessageText(msg.content)" class="message-text">
              {{ getMessageText(msg.content) }}
            </p>
            <time class="message-time">{{ msg.time }}</time>
          </div>
        </article>
      </section>

      <footer class="composer">
        <div class="composer-tools">
          <button
            type="button"
            class="upload-btn"
            :disabled="sending"
            @click="triggerImageSelect"
          >
            上传图片
          </button>
          <button
            type="button"
            class="order-picker-btn"
            :disabled="sending"
            @click="openOrderPicker"
          >
            +
          </button>
          <input
            ref="imageInputRef"
            class="image-input"
            type="file"
            accept="image/*"
            @change="handleImageChange"
          />
          <div v-if="pendingImagePreviewUrl" class="pending-image-box">
            <img
              :src="pendingImagePreviewUrl"
              alt="待发送图片"
              class="pending-image"
            />
            <button
              type="button"
              class="pending-image-remove"
              @click="clearPendingImage"
            >
              移除图片
            </button>
          </div>

          <div v-if="selectedOrderId" class="selected-order-chip">
            <span>
              订单号：{{ selectedOrderDisplayNo }}
              <template v-if="selectedOrder">
                · {{ selectedOrder.sourceLabel }} · {{ selectedOrder.title }}
                <template v-if="selectedOrder.time">
                  · {{ formatTime(selectedOrder.time) }}
                </template>
              </template>
            </span>
            <button
              type="button"
              class="selected-order-clear"
              :disabled="sending"
              @click="clearSelectedOrder"
            >
              清除
            </button>
          </div>
          <p v-else class="order-picker-placeholder">未关联订单（可选）</p>
        </div>

        <div class="composer-row">
          <input
            v-model.trim="draft"
            class="composer-input"
            placeholder="在这里输入您的问题试试~"
            :disabled="sending"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <button
            type="button"
            class="send-btn"
            :disabled="sending"
            @click="sendMessage"
          >
            {{ sending ? "发送中" : "发送" }}
          </button>
        </div>
        <p class="composer-tip">
          支持发送文字与图片；点击“+”可选择买到/卖出的订单并关联给客服。
        </p>
      </footer>

      <el-dialog
        v-model="orderPickerVisible"
        title="选择关联订单"
        width="680px"
        destroy-on-close
      >
        <p class="order-picker-tip">
          展示我买到和卖出的全部订单，按时间最新排序。
        </p>
        <div class="order-picker-list">
          <p v-if="orderListLoading" class="state-text">订单加载中...</p>
          <p v-else-if="orderSelectionItems.length === 0" class="state-text">
            暂无可关联订单
          </p>
          <button
            v-for="item in orderSelectionItems"
            v-else
            :key="item.key"
            type="button"
            class="order-picker-item"
            @click="selectOrder(item)"
          >
            <div class="order-picker-item__main">
              <p class="order-picker-item__title">
                订单号：{{ getOrderDisplayNo(item) }} · {{ item.sourceLabel }}
              </p>
              <p class="order-picker-item__desc">{{ item.title }}</p>
            </div>
            <time class="order-picker-item__time">{{
              formatTime(item.time)
            }}</time>
          </button>
        </div>
        <template #footer>
          <el-button @click="closeOrderPicker">关闭</el-button>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRoute } from "vue-router";
import {
  fetchMySupportMessages,
  sendMySupportMessage,
} from "../service/admin/adminApiService";
import { buildMessageWebSocketUrl } from "../service/chat/chatApiService";
import {
  fetchBoughtItems,
  fetchSoldItems,
} from "../service/profile/profileApiService";
import { uploadPublishImage } from "../service/publish/publishApiService";

const route = useRoute();

const loading = ref(false);
const draft = ref("");
const messages = ref([]);
const messageListRef = ref(null);
const socketRef = ref(null);
const reconnectTimerRef = ref(null);
const isManualClose = ref(false);
const imageInputRef = ref(null);
const pendingImageFile = ref(null);
const pendingImagePreviewUrl = ref("");
const sending = ref(false);
const orderPickerVisible = ref(false);
const orderListLoading = ref(false);
const orderSelectionItems = ref([]);
const selectedOrderId = ref(null);

const IMAGE_PREFIX = "【图片】";

const selectedOrder = computed(() => {
  const selected = Number(selectedOrderId.value || 0);
  if (!selected) return null;
  return (
    orderSelectionItems.value.find(
      (item) => Number(item.orderId || 0) === selected
    ) || null
  );
});

const getOrderDisplayNo = (item) => {
  const orderNo = String(item?.orderNo || "").trim();
  if (orderNo) return orderNo;
  const orderId = Number(item?.orderId || 0);
  return orderId > 0 ? String(orderId) : "";
};

const selectedOrderDisplayNo = computed(() => {
  if (selectedOrder.value) {
    return getOrderDisplayNo(selectedOrder.value);
  }
  const orderId = Number(selectedOrderId.value || 0);
  return orderId > 0 ? String(orderId) : "";
});

const normalizeListData = (responseBody) => {
  const payload = responseBody?.data;
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.records)) return payload.records;
  if (Array.isArray(payload?.list)) return payload.list;
  return [];
};

const toSortTimestamp = (value) => {
  if (!value) return 0;
  const text = String(value).trim();
  if (!text) return 0;
  const normalized = text.includes("T") ? text : text.replace(" ", "T");
  const ts = Date.parse(normalized);
  return Number.isNaN(ts) ? 0 : ts;
};

const mapSupportOrderItem = (item, source) => {
  const orderId = Number(item?.orderId ?? item?.id ?? 0);
  if (!Number.isInteger(orderId) || orderId <= 0) {
    return null;
  }
  const time = String(item?.time || "").trim();
  return {
    key: `${source}-${orderId}`,
    orderId,
    orderNo: String(item?.orderNo || "").trim(),
    source,
    sourceLabel: source === "bought" ? "买到" : "卖出",
    title:
      String(item?.title || item?.name || "未命名物品").trim() || "未命名物品",
    status: String(item?.status || "").trim(),
    time,
    sortTime: toSortTimestamp(time),
  };
};

const loadOrderSelectionItems = async () => {
  orderListLoading.value = true;
  try {
    const statusList = [
      "all",
      "pending-payment",
      "pending-confirmation",
      "completed",
      "cancelled",
    ];
    const tasks = statusList.flatMap((status) => [
      fetchBoughtItems(status),
      fetchSoldItems(status),
    ]);
    const responses = await Promise.allSettled(tasks);

    const mergedMap = new Map();
    responses.forEach((result, index) => {
      if (result.status !== "fulfilled") return;
      const source = index % 2 === 0 ? "bought" : "sold";
      const mapped = normalizeListData(result.value)
        .map((item) => mapSupportOrderItem(item, source))
        .filter(Boolean);
      mapped.forEach((item) => {
        if (!mergedMap.has(item.key)) {
          mergedMap.set(item.key, item);
        }
      });
    });

    orderSelectionItems.value = Array.from(mergedMap.values()).sort((a, b) => {
      if (b.sortTime !== a.sortTime) {
        return b.sortTime - a.sortTime;
      }
      return b.orderId - a.orderId;
    });
  } catch (error) {
    ElMessage.error(error.message || "加载订单列表失败");
  } finally {
    orderListLoading.value = false;
  }
};

const openOrderPicker = async () => {
  orderPickerVisible.value = true;
  await loadOrderSelectionItems();
};

const closeOrderPicker = () => {
  orderPickerVisible.value = false;
};

const selectOrder = (item) => {
  const orderId = Number(item?.orderId || 0);
  if (!Number.isInteger(orderId) || orderId <= 0) {
    ElMessage.warning("订单信息无效");
    return;
  }
  selectedOrderId.value = orderId;
  closeOrderPicker();
};

const clearSelectedOrder = () => {
  selectedOrderId.value = null;
};

const formatTime = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const y = date.getFullYear();
  const mon = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const h = String(date.getHours()).padStart(2, "0");
  const m = String(date.getMinutes()).padStart(2, "0");
  return `${y}-${mon}-${d} ${h}:${m}`;
};

const parseMessagePayload = (content) => {
  const raw = String(content || "").trim();
  if (!raw) {
    return { imageUrl: "", text: "" };
  }

  if (raw.startsWith(IMAGE_PREFIX)) {
    const lines = raw.split("\n");
    const imageUrl = lines[0].replace(IMAGE_PREFIX, "").trim();
    const text = lines.slice(1).join("\n").trim();
    return { imageUrl, text };
  }

  const directImageUrlPattern =
    /^https?:\/\/\S+\.(png|jpe?g|webp|gif|bmp|svg)(\?.*)?$/i;
  if (directImageUrlPattern.test(raw)) {
    return { imageUrl: raw, text: "" };
  }

  return { imageUrl: "", text: raw };
};

const getMessageImageUrl = (content) => parseMessagePayload(content).imageUrl;
const getMessageText = (content) => parseMessagePayload(content).text;

const buildMessageContent = (text, imageUrl) => {
  const safeText = String(text || "").trim();
  const safeImageUrl = String(imageUrl || "").trim();
  if (!safeImageUrl) {
    return safeText;
  }
  if (!safeText) {
    return `${IMAGE_PREFIX}${safeImageUrl}`;
  }
  return `${IMAGE_PREFIX}${safeImageUrl}\n${safeText}`;
};

const pushMessage = (from, content, createdAt, id = null) => {
  messages.value.push({
    key: `${id || "tmp"}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    from,
    content: String(content || ""),
    time: formatTime(createdAt || new Date().toISOString()),
  });
};

const scrollToBottom = async () => {
  await nextTick();
  const el = messageListRef.value;
  if (!el) return;
  el.scrollTop = el.scrollHeight;
};

const clearPendingImage = () => {
  if (pendingImagePreviewUrl.value) {
    URL.revokeObjectURL(pendingImagePreviewUrl.value);
  }
  pendingImageFile.value = null;
  pendingImagePreviewUrl.value = "";
  if (imageInputRef.value) {
    imageInputRef.value.value = "";
  }
};

const triggerImageSelect = () => {
  imageInputRef.value?.click();
};

const handleImageChange = (event) => {
  const file = event?.target?.files?.[0];
  if (!file) {
    return;
  }

  if (!file.type || !file.type.startsWith("image/")) {
    ElMessage.warning("仅支持上传图片");
    clearPendingImage();
    return;
  }

  const maxSize = 8 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.warning("图片大小不能超过 8MB");
    clearPendingImage();
    return;
  }

  clearPendingImage();
  pendingImageFile.value = file;
  pendingImagePreviewUrl.value = URL.createObjectURL(file);
};

const loadMessages = async () => {
  loading.value = true;
  try {
    const { data } = await fetchMySupportMessages();
    const list = Array.isArray(data) ? data : [];
    messages.value = list.map((item, index) => ({
      key: `${item.id || index}-${index}`,
      from: String(item.from || "").includes("管理") ? "other" : "self",
      content: String(item.content || ""),
      time: formatTime(item.createdAt),
    }));
    await scrollToBottom();
  } catch (error) {
    ElMessage.error(error.message || "加载客服消息失败");
  } finally {
    loading.value = false;
  }
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

    socket.onmessage = async (event) => {
      try {
        const payload = JSON.parse(event.data || "{}");
        const eventType = String(payload?.eventType || "").toUpperCase();
        if (eventType === "SUPPORT_MESSAGE_CREATED") {
          const msg = payload?.message || {};
          pushMessage(
            msg.from === "self" ? "self" : "other",
            msg.text,
            msg.time,
            msg.id
          );
          await scrollToBottom();
          return;
        }
        if (eventType === "ERROR") {
          ElMessage.warning(payload?.message || "发送失败");
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

const sendMessage = async () => {
  if (sending.value) {
    return;
  }

  const text = String(draft.value || "").trim();
  const imageFile = pendingImageFile.value;
  const orderId = Number.isInteger(Number(selectedOrderId.value || 0))
    ? Number(selectedOrderId.value || 0)
    : 0;

  if (!text && !imageFile) {
    ElMessage.warning("请输入问题内容或上传图片");
    return;
  }

  sending.value = true;
  try {
    let imageUrl = "";
    if (imageFile) {
      const uploadResult = await uploadPublishImage(imageFile);
      imageUrl = String(
        uploadResult?.data?.url || uploadResult?.data || ""
      ).trim();
      if (!imageUrl) {
        throw new Error("图片上传失败");
      }
    }

    const finalContent = buildMessageContent(text, imageUrl);
    draft.value = "";
    clearPendingImage();

    const socket = socketRef.value;
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(
        JSON.stringify({
          action: "SEND_SUPPORT_MESSAGE",
          text: finalContent,
          orderId: orderId > 0 ? orderId : null,
        })
      );
      return;
    }

    await sendMySupportMessage(finalContent, orderId > 0 ? orderId : null);
    pushMessage("self", finalContent, new Date().toISOString());
    await scrollToBottom();
  } catch (error) {
    ElMessage.error(error.message || "发送失败");
  } finally {
    sending.value = false;
  }
};

onMounted(async () => {
  const routeOrderId = String(route.query?.orderId || "").trim();
  if (/^\d+$/.test(routeOrderId) && Number(routeOrderId) > 0) {
    selectedOrderId.value = Number(routeOrderId);
  }
  await loadMessages();
  connectWebSocket();
});

onBeforeUnmount(() => {
  clearPendingImage();
  if (reconnectTimerRef.value) {
    clearTimeout(reconnectTimerRef.value);
    reconnectTimerRef.value = null;
  }
  isManualClose.value = true;
  if (socketRef.value) {
    socketRef.value.close();
    socketRef.value = null;
  }
});
</script>

<style scoped>
.support-page {
  min-height: 100vh;
  background: #f5f6fb;
  padding: 24px;
}

.card {
  max-width: 1280px;
  margin: 0 auto;
  border-radius: 22px;
  background: #fcfbff;
  box-shadow: 0 16px 36px rgba(132, 116, 185, 0.14);
  border: 1px solid #ece6ff;
}

.support-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: calc(100vh - 48px);
}

.support-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ece9f8;
  padding: 22px 28px 16px;
}

.support-header h2 {
  margin: 0;
  color: #352d5f;
  font-size: 40px;
  line-height: 1.1;
}

.support-subtitle {
  margin: 8px 0 0;
  color: #7c74a6;
  font-size: 14px;
}

.message-list {
  overflow: auto;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #faf9ff;
}

.state-text {
  color: #6d6595;
  text-align: center;
  margin: 24px 0;
}

.message-item {
  display: flex;
}

.message-item--self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(72%, 620px);
  border-radius: 16px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #ece8ff;
  box-shadow: 0 8px 18px rgba(143, 125, 197, 0.12);
}

.message-item--self .message-bubble {
  background: #f1ebff;
  border-color: #ddcfff;
}

.message-image {
  max-width: 280px;
  width: 100%;
  border-radius: 12px;
  display: block;
  object-fit: cover;
  margin-bottom: 8px;
}

.message-text {
  margin: 0;
  color: #312b52;
  line-height: 1.6;
  word-break: break-word;
}

.message-time {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #837bab;
}

.composer {
  border-top: 1px solid #ece9f8;
  padding: 12px 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fbfaff;
}

.composer-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.upload-btn {
  border: 1px solid #cfc3ff;
  background: #efe9ff;
  color: #5a4e92;
  border-radius: 12px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 14px;
}

.image-input {
  display: none;
}

.pending-image-box {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 12px;
  border: 1px solid #ddd2ff;
  background: #ffffff;
  padding: 6px 8px;
}

.pending-image {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  object-fit: cover;
}

.pending-image-remove {
  border: 1px solid #edd8d8;
  background: #fff2f2;
  color: #995a5a;
  border-radius: 10px;
  padding: 6px 10px;
  font-size: 13px;
  cursor: pointer;
}

.composer-row {
  display: flex;
  gap: 10px;
}

.composer-row--meta {
  align-items: center;
}

.order-picker-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #cfc3ff;
  background: #efe9ff;
  color: #5a4e92;
  border-radius: 999px;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.selected-order-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #d9cfff;
  background: #ffffff;
  border-radius: 999px;
  padding: 8px 12px;
  color: #3f3565;
  font-size: 13px;
  max-width: min(100%, 860px);
}

.selected-order-chip > span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.selected-order-clear {
  border: 1px solid #edd8d8;
  background: #fff2f2;
  color: #995a5a;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.order-picker-placeholder {
  margin: 0;
  color: #8d86af;
  font-size: 13px;
}

.order-picker-tip {
  margin: 0 0 12px;
  color: #6d6595;
  font-size: 13px;
}

.order-picker-list {
  max-height: 420px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-picker-item {
  border: 1px solid #e6ddff;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
  cursor: pointer;
}

.order-picker-item:hover {
  border-color: #cdbdff;
  background: #faf8ff;
}

.order-picker-item__main {
  min-width: 0;
}

.order-picker-item__title,
.order-picker-item__desc {
  margin: 0;
}

.order-picker-item__title {
  color: #3a3163;
  font-weight: 600;
}

.order-picker-item__desc {
  margin-top: 4px;
  color: #6d6595;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-picker-item__time {
  flex-shrink: 0;
  font-size: 12px;
  color: #837bab;
}

.composer-input {
  flex: 1;
  border: 1px solid #d9cfff;
  border-radius: 999px;
  padding: 11px 14px;
  font-size: 14px;
  color: #3f3565;
  background: #ffffff;
}

.composer-input--order {
  flex: 0 0 260px;
  max-width: 100%;
}

.composer-input:focus {
  outline: none;
  border-color: #b9a5ff;
  box-shadow: 0 0 0 3px rgba(185, 165, 255, 0.2);
}

.send-btn {
  border: none;
  background: #7c3aed;
  color: #ffffff;
  border-radius: 999px;
  padding: 10px 22px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.upload-btn:disabled,
.send-btn:disabled,
.order-picker-btn:disabled,
.selected-order-clear:disabled {
  opacity: 0.62;
  cursor: not-allowed;
}

.composer-tip {
  margin: 0;
  font-size: 12px;
  color: #8d86af;
}

@media (max-width: 760px) {
  .support-page {
    padding: 10px;
  }

  .support-main {
    min-height: calc(100vh - 20px);
  }

  .support-header {
    padding: 16px;
  }

  .support-header h2 {
    font-size: 30px;
  }

  .message-bubble {
    max-width: 86%;
  }

  .composer {
    padding: 10px;
  }
}
</style>
