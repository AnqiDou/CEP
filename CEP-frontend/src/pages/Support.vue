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
        <p class="composer-tip">支持发送文字与图片，图片将自动上传后发送</p>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  fetchMySupportMessages,
  sendMySupportMessage,
} from "../service/admin/adminApiService";
import { buildMessageWebSocketUrl } from "../service/chat/chatApiService";
import { uploadPublishImage } from "../service/publish/publishApiService";

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

const IMAGE_PREFIX = "【图片】";

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
        })
      );
      return;
    }

    await sendMySupportMessage(finalContent);
    pushMessage("self", finalContent, new Date().toISOString());
    await scrollToBottom();
  } catch (error) {
    ElMessage.error(error.message || "发送失败");
  } finally {
    sending.value = false;
  }
};

onMounted(async () => {
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

.composer-input {
  flex: 1;
  border: 1px solid #d9cfff;
  border-radius: 999px;
  padding: 11px 14px;
  font-size: 14px;
  color: #3f3565;
  background: #ffffff;
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
.send-btn:disabled {
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
