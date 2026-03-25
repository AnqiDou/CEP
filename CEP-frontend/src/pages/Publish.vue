<template>
  <div class="publish-page">
    <header class="publish-header">
      <div>
        <h1 class="publish-title">发布闲置</h1>
        <p class="publish-subtitle">
          完善信息后可快速发布，当前仅前端展示不提交后端
        </p>
      </div>
      <div class="publish-header__actions">
        <button
          class="primary-btn publish-header__submit"
          @click="handleSubmit"
        >
          提交发布
        </button>
        <button class="ghost-btn" @click="goBackHome">返回首页</button>
      </div>
    </header>

    <main class="publish-main">
      <section class="form-card">
        <form class="publish-form" @submit.prevent="handleSubmit">
          <label class="form-field">
            <span class="form-label">物品名称 *</span>
            <input
              v-model="form.name"
              class="form-input"
              type="text"
              required
              placeholder="例如：九成新机械键盘"
            />
          </label>

          <label class="form-field">
            <span class="form-label">分类</span>
            <select v-model="form.category" class="form-input form-select">
              <option disabled value="">请选择分类</option>
              <option
                v-for="category in categories"
                :key="category.value"
                :value="category.value"
              >
                {{ category.label }}
              </option>
            </select>
          </label>

          <div class="form-grid">
            <label class="form-field">
              <span class="form-label">购买时间</span>
              <input
                v-model="form.purchaseDate"
                class="form-input"
                type="date"
              />
            </label>

            <label class="form-field">
              <span class="form-label">使用时长</span>
              <input
                v-model="form.usageDuration"
                class="form-input"
                type="text"
                placeholder="例如：8个月"
              />
            </label>
          </div>

          <label class="form-field">
            <span class="form-label">描述</span>
            <textarea
              v-model="form.description"
              class="form-input form-textarea"
              placeholder="补充成色、功能、交易方式等信息"
            />
          </label>

          <div class="form-field">
            <span class="form-label">照片上传</span>
            <label class="upload-area" for="item-photos">
              <input
                id="item-photos"
                class="upload-input"
                type="file"
                accept="image/*"
                multiple
                @change="handleFileChange"
              />
              <span class="upload-text">点击或拖拽上传（支持多图预览）</span>
              <span class="upload-hint">建议上传清晰实拍图，最多 6 张</span>
            </label>

            <div v-if="photoPreviews.length" class="preview-grid">
              <article
                v-for="(photo, index) in photoPreviews"
                :key="photo.id"
                class="preview-card"
              >
                <img
                  :src="photo.url"
                  :alt="`预览图${index + 1}`"
                  class="preview-image"
                />
                <button
                  type="button"
                  class="preview-remove"
                  @click="removePhoto(photo.id)"
                >
                  移除
                </button>
              </article>
            </div>
          </div>

          <p v-if="submitMessage" class="submit-message">{{ submitMessage }}</p>
        </form>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

const categories = [
  { label: "学习教材 / 书籍", value: "book" },
  { label: "数码电子", value: "digital" },
  { label: "生活用品", value: "life" },
  { label: "运动出行", value: "sport" },
  { label: "其他闲置", value: "other" },
];

const form = ref({
  name: "",
  category: "",
  purchaseDate: "",
  usageDuration: "",
  description: "",
});

const submitMessage = ref("");
const photoPreviews = ref([]);
let photoId = 0;

const goBackHome = () => {
  router.push("/");
};

const revokeUrls = (photos) => {
  photos.forEach((photo) => {
    URL.revokeObjectURL(photo.url);
  });
};

const handleFileChange = (event) => {
  const input = event.target;
  const files = Array.from(input.files || []);
  if (!files.length) return;

  const availableCount = 6 - photoPreviews.value.length;
  if (availableCount <= 0) {
    submitMessage.value = "最多上传 6 张图片";
    input.value = "";
    return;
  }

  const nextFiles = files.slice(0, availableCount);
  if (files.length > availableCount) {
    submitMessage.value = "最多上传 6 张图片，已自动截取前几张";
  } else {
    submitMessage.value = "";
  }

  const nextPreviews = nextFiles.map((file) => ({
    id: `${Date.now()}-${photoId++}`,
    url: URL.createObjectURL(file),
  }));
  photoPreviews.value = [...photoPreviews.value, ...nextPreviews];
  input.value = "";
};

const removePhoto = (id) => {
  const target = photoPreviews.value.find((photo) => photo.id === id);
  if (!target) return;
  URL.revokeObjectURL(target.url);
  photoPreviews.value = photoPreviews.value.filter((photo) => photo.id !== id);
};

const handleSubmit = () => {
  if (!form.value.name.trim()) {
    submitMessage.value = "请填写物品名称";
    return;
  }
  if (!form.value.category) {
    submitMessage.value = "请选择物品分类";
    return;
  }
  if (!form.value.purchaseDate) {
    submitMessage.value = "请选择购买时间";
    return;
  }
  if (!form.value.usageDuration.trim()) {
    submitMessage.value = "请填写使用时长";
    return;
  }
  if (!form.value.description.trim()) {
    submitMessage.value = "请填写物品描述";
    return;
  }
  if (!photoPreviews.value.length) {
    submitMessage.value = "请至少上传一张照片";
    return;
  }

  submitMessage.value = "发布信息已暂存（仅前端演示，后端接口暂未接入）";
};

onBeforeUnmount(() => {
  revokeUrls(photoPreviews.value);
});
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4ff 0%, #f8fbff 36%, #f3f6fb 100%);
  color: #1f2937;
}

.publish-header {
  max-width: 1160px;
  margin: 0 auto;
  padding: 28px 24px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}

.publish-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.publish-title {
  margin: 0;
  font-size: 30px;
  color: #1d4ed8;
}

.publish-subtitle {
  margin: 10px 0 0;
  font-size: 14px;
  color: #64748b;
}

.publish-main {
  max-width: 1160px;
  margin: 0 auto;
  padding: 0 24px 28px;
  display: block;
}

.form-card {
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.form-card {
  padding: 22px;
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
}

.form-input {
  min-height: 44px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  padding: 10px 14px;
  background: #f8fbff;
  font-size: 14px;
  color: #1f2937;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.16);
}

.form-select {
  appearance: none;
}

.form-textarea {
  min-height: 120px;
  resize: vertical;
}

.upload-area {
  position: relative;
  border: 1px dashed #93c5fd;
  border-radius: 12px;
  padding: 20px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #f8fbff;
  cursor: pointer;
}

.upload-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.upload-text {
  color: #1d4ed8;
  font-weight: 600;
}

.upload-hint {
  font-size: 12px;
  color: #64748b;
}

.preview-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.preview-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #dbeafe;
  background: #f8fbff;
}

.preview-image {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

.preview-remove {
  width: 100%;
  border: none;
  padding: 8px;
  background: #e5edff;
  color: #1d4ed8;
  font-size: 12px;
  cursor: pointer;
}

.submit-message {
  margin: 0;
  font-size: 13px;
  color: #2563eb;
}

.primary-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 20px;
  font-size: 15px;
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

@media (max-width: 960px) {
  .publish-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .publish-header__actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 680px) {
  .publish-header,
  .publish-main {
    padding-left: 14px;
    padding-right: 14px;
  }

  .publish-title {
    font-size: 26px;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .preview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
