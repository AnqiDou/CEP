<template>
  <div class="support-page">
    <main class="support-main card">
      <header class="support-header">
        <button class="back-btn" type="button" @click="goBack">←</button>
        <h2>在线客服</h2>
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
            <p class="message-text">{{ msg.content }}</p>
            <time class="message-time">{{ msg.time }}</time>
          </div>
        </article>
      </section>

      <footer class="composer">
        <input
          v-model.trim="draft"
          class="composer-input"
          placeholder="在这里输入您的问题试试~"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <button type="button" class="send-btn" @click="sendMessage">
          发送
        </button>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import {
  fetchMySupportMessages,
  sendMySupportMessage,
} from "../service/admin/adminApiService";
import { buildMessageWebSocketUrl } from "../service/chat/chatApiService";

const router = useRouter();
const loading = ref(false);
const draft = ref("");
const messages = ref([]);
const messageListRef = ref(null);
const socketRef = ref(null);
const reconnectTimerRef = ref(null);
const isManualClose = ref(false);

const formatTime = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const h = String(date.getHours()).padStart(2, "0");
  const m = String(date.getMinutes()).padStart(2, "0");
  return `${h}:${m}`;
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
  if (!draft.value) {
    ElMessage.warning("请输入问题内容");
    return;
  }
  const text = draft.value;
  draft.value = "";

  const socket = socketRef.value;
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(
      JSON.stringify({
        action: "SEND_SUPPORT_MESSAGE",
        text,
      })
    );
    return;
  }

  try {
    await sendMySupportMessage(text);
    pushMessage("self", text, new Date().toISOString());
    await scrollToBottom();
  } catch (error) {
    ElMessage.error(error.message || "发送失败");
  }
};

const goBack = () => router.back();

onMounted(async () => {
  await loadMessages();
  connectWebSocket();
});

onBeforeUnmount(() => {
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
  background: #f5f7fb;
  padding: 20px;
}

.card {
  max-width: 1200px;
  margin: 0 auto;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
}

.support-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 82vh;
}

.support-header {
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #eceff5;
  padding: 14px 18px;
}

.back-btn {
  border: none;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
}

.message-list {
  overflow: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.state-text {
  color: #64748b;
  text-align: center;
}

.message-item {
  display: flex;
}

.message-item--self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 65%;
  border-radius: 12px;
  padding: 10px 12px;
  background: #eef2ff;
}

.message-item--self .message-bubble {
  background: #e8f8ee;
}

.message-text {
  margin: 0;
  color: #0f172a;
  word-break: break-word;
}

.message-time {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.composer {
  border-top: 1px solid #eceff5;
  padding: 12px 18px;
  display: flex;
  gap: 10px;
}

.composer-input {
  flex: 1;
  border: 1px solid #d6dbe7;
  border-radius: 999px;
  padding: 10px 14px;
}

.send-btn {
  border: none;
  background: #ffb56c;
  color: #fff;
  border-radius: 999px;
  padding: 10px 20px;
  cursor: pointer;
}
</style>
