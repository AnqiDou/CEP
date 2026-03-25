<template>
  <div class="chat-page">
    <main class="chat-layout">
      <section class="chat-window card">
        <header class="chat-topbar">
          <div class="chat-seller">
            <div class="chat-seller__avatar">{{ sellerName.slice(0, 1) }}</div>
            <div>
              <p class="chat-seller__name">{{ sellerName }}</p>
              <p class="chat-seller__meta">在线沟通中</p>
            </div>
          </div>
          <button class="ghost-btn" type="button" @click="goBack">返回</button>
        </header>

        <section class="chat-product-bar">
          <div class="chat-product__thumb">闲置</div>
          <div class="chat-product__info">
            <p class="chat-product__title">{{ itemTitle }}</p>
            <p class="chat-product__sub">当前会话物品</p>
          </div>
          <button class="chat-product__buy" type="button">立即购买</button>
        </section>

        <div ref="messageContainerRef" class="message-list">
          <article
            v-for="message in messages"
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
              <p v-if="message.text" class="message-text">{{ message.text }}</p>
              <span class="message-time">{{ message.time }}</span>
            </div>
          </article>
        </div>

        <footer class="composer">
          <div class="composer-tools">
            <button
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

          <div v-show="showEmojiPanel" class="emoji-panel">
            <Picker
              :data="emojiIndex"
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
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { Picture } from "@element-plus/icons-vue";
import data from "emoji-mart-vue-fast/data/all.json";
import { Picker, EmojiIndex } from "emoji-mart-vue-fast/src";
import "emoji-mart-vue-fast/css/emoji-mart.css";

const emojiIndex = new EmojiIndex(data);

const route = useRoute();
const router = useRouter();

const itemTitle = computed(() => route.query.itemTitle || "校园闲置物品");
const sellerName = computed(() => route.query.sellerName || "卖家同学");

const draft = ref("");
const showEmojiPanel = ref(false);
const pendingImageUrl = ref("");
const messageContainerRef = ref(null);
const messages = ref([
  {
    id: 1,
    from: "other",
    text: "你好，物品还在，成色和详情页一致。",
    imageUrl: "",
    time: "10:20",
  },
]);

let replyTimer = null;

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
  () => messages.value.length,
  () => {
    scrollToBottom();
  },
  { immediate: true }
);

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

const sendMessage = () => {
  const text = draft.value.trim();
  const hasImage = Boolean(pendingImageUrl.value);

  if (!text && !hasImage) {
    ElMessage.warning("请输入消息或选择图片");
    return;
  }

  messages.value.push({
    id: Date.now(),
    from: "self",
    text,
    imageUrl: pendingImageUrl.value,
    time: getNowTime(),
  });

  draft.value = "";
  pendingImageUrl.value = "";
  showEmojiPanel.value = false;

  if (replyTimer) {
    window.clearTimeout(replyTimer);
  }

  replyTimer = window.setTimeout(() => {
    messages.value.push({
      id: Date.now() + 1,
      from: "other",
      text: "收到，我看到了，我们可以约今晚当面交易。",
      imageUrl: "",
      time: getNowTime(),
    });
  }, 700);
};

const goBack = () => {
  router.back();
};

onBeforeUnmount(() => {
  if (replyTimer) {
    window.clearTimeout(replyTimer);
  }
  clearPendingImage();
});
</script>

<style scoped>
.chat-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4ff 0%, #f8fbff 36%, #f3f6fb 100%);
  color: #1f2937;
}

.chat-layout {
  max-width: 1240px;
  margin: 0 auto;
  padding: 18px;
}

.card {
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.chat-window {
  min-height: calc(100vh - 38px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-topbar {
  padding: 14px 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chat-seller {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-seller__avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #bfdbfe, #93c5fd);
  color: #1d4ed8;
  font-weight: 700;
}

.chat-seller__name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.chat-seller__meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.chat-product-bar {
  padding: 10px 16px;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 98px;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fbff;
}

.chat-product__thumb {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  background: linear-gradient(135deg, #bfdbfe, #dbeafe);
  color: #1d4ed8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.chat-product__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.chat-product__sub {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.chat-product__buy {
  border: 1px solid #d1d5db;
  border-radius: 999px;
  padding: 8px 0;
  background: #ffffff;
  color: #111827;
  font-weight: 700;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
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
  max-width: min(72%, 620px);
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
  opacity: 0.75;
}

.composer {
  border-top: 1px solid #dbeafe;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #ffffff;
  position: relative;
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
  min-height: 84px;
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
.emoji-panel :deep(.emoji-mart-bar) {
  display: none;
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

.ghost-btn {
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
}

@media (max-width: 720px) {
  .chat-layout {
    padding: 10px;
  }

  .chat-window {
    min-height: calc(100vh - 20px);
  }

  .chat-product-bar {
    grid-template-columns: 56px minmax(0, 1fr);
  }

  .chat-product__thumb {
    width: 56px;
    height: 56px;
  }

  .chat-product__buy {
    grid-column: 1 / -1;
    width: 120px;
    justify-self: end;
  }

  .message-bubble {
    max-width: 90%;
  }
}
</style>
