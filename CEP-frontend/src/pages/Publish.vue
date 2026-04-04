<template>
  <div class="publish-page">
    <main class="publish-main">
      <aside class="left-rail soft-card">
        <h2 class="rail-title">{{ isEditMode ? "编辑导航" : "发布导航" }}</h2>
        <ul class="left-nav-list">
          <li class="left-nav-item is-active">
            <span class="left-nav-icon">📝</span>
            <div>
              <p class="left-nav-text">填写信息</p>
              <p class="left-nav-sub">名称 / 分类 / 价格</p>
            </div>
          </li>
          <li class="left-nav-item">
            <span class="left-nav-icon">📸</span>
            <div>
              <p class="left-nav-text">上传图片</p>
              <p class="left-nav-sub">最多 6 张高清实拍图</p>
            </div>
          </li>
          <li class="left-nav-item">
            <span class="left-nav-icon">🚀</span>
            <div>
              <p class="left-nav-text">提交发布</p>
              <p class="left-nav-sub">
                {{ isEditMode ? "保存后立即生效" : "审核后即可展示" }}
              </p>
            </div>
          </li>
        </ul>
      </aside>

      <section class="center-rail">
        <header class="publish-header soft-card">
          <div>
            <h1 class="publish-title">
              {{ isEditMode ? "编辑闲置" : "发布闲置" }}
            </h1>
            <p class="publish-subtitle">
              {{ isEditMode ? "修改信息后保存" : "完善信息后可快速发布" }}
            </p>
          </div>
          <div class="publish-header__actions">
            <button
              type="button"
              class="primary-btn publish-header__submit"
              :disabled="submitting"
              @click="handleSubmit"
            >
              {{
                submitting
                  ? isEditMode
                    ? "保存中..."
                    : "发布中..."
                  : isEditMode
                  ? "保存修改"
                  : "提交发布"
              }}
            </button>
            <button class="ghost-btn" @click="goBackHome">返回首页</button>
          </div>
        </header>

        <section class="form-card soft-card">
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

            <label class="form-field">
              <span class="form-label">价格（元） *</span>
              <input
                v-model="form.price"
                class="form-input"
                type="number"
                min="0"
                step="0.01"
                required
                placeholder="例如：99.00"
              />
            </label>

            <div class="form-grid">
              <label class="form-field">
                <span class="form-label">购买时间</span>
                <el-date-picker
                  v-model="form.purchaseDate"
                  class="form-input publish-date-picker"
                  type="date"
                  format="YYYY年MM月DD日"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择购买日期"
                  popper-class="publish-date-popper"
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

            <p v-if="submitMessage" class="submit-message">
              {{ submitMessage }}
            </p>
          </form>
        </section>
      </section>

      <aside class="right-rail">
        <section class="status-card soft-card">
          <h3 class="rail-title">当前进度</h3>
          <p class="status-line">图片数量：{{ photoPreviews.length }} / 6</p>
          <div class="status-progress">
            <span
              class="status-progress__bar"
              :style="{
                width: `${Math.min((photoPreviews.length / 6) * 100, 100)}%`,
              }"
            ></span>
          </div>
          <p class="status-tip">建议至少上传 3 张真实细节图，提升成交率</p>
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  createPublishItem,
  fetchMyPublishItems,
  updateMyPublishItem,
  uploadPublishImage,
} from "../service/publish/publishApiService";

const router = useRouter();
const route = useRoute();

const editItemId = computed(() => {
  const parsed = Number(route.query.editItemId);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
});
const isEditMode = computed(() => Boolean(editItemId.value));

const categories = [
  {
    label: "数码产品（手机、电脑、平板、耳机、充电器等）",
    value: "digital",
  },
  {
    label: "图书教材（课本、考研考公资料、小说、专业书）",
    value: "book",
  },
  { label: "服饰鞋包（衣服、鞋子、包包、配饰）", value: "clothes" },
  { label: "美妆护肤（化妆品、护肤品、香水）", value: "beauty" },
  {
    label: "运动器材（篮球、羽毛球拍、瑜伽垫、自行车）",
    value: "sports",
  },
  {
    label: "生活用品（收纳、小家电、锅碗瓢盆、寝室用品）",
    value: "daily",
  },
  { label: "文具办公（笔、本、计算器、文件夹等）", value: "stationery" },
  { label: "其他", value: "other" },
];

const form = ref({
  name: "",
  category: "other",
  price: "",
  purchaseDate: "",
  usageDuration: "",
  description: "",
});

const submitMessage = ref("");
const photoPreviews = ref([]);
const submitting = ref(false);
let photoId = 0;

const goBackHome = () => {
  router.push("/");
};

const revokeUrls = (photos) => {
  photos.forEach((photo) => {
    if (!photo?.isRemote && typeof photo?.url === "string") {
      URL.revokeObjectURL(photo.url);
    }
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
    file,
    url: URL.createObjectURL(file),
    isRemote: false,
  }));
  photoPreviews.value = [...photoPreviews.value, ...nextPreviews];
  input.value = "";
};

const removePhoto = (id) => {
  const target = photoPreviews.value.find((photo) => photo.id === id);
  if (!target) return;
  if (!target.isRemote) {
    URL.revokeObjectURL(target.url);
  }
  photoPreviews.value = photoPreviews.value.filter((photo) => photo.id !== id);
};

const normalizeListData = (responseBody) => {
  const payload = responseBody?.data;
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.records)) return payload.records;
  if (Array.isArray(payload?.list)) return payload.list;
  return [];
};

const fillFormForEdit = (item) => {
  form.value = {
    name: item?.title || item?.name || "",
    category: item?.categoryCode || "other",
    price:
      item?.price === null || item?.price === undefined
        ? ""
        : String(item.price),
    purchaseDate: item?.purchaseDate || "",
    usageDuration: item?.usageDuration || "",
    description: item?.description || "",
  };

  const remotePhotoUrls = Array.isArray(item?.photoUrls)
    ? item.photoUrls
    : typeof item?.photoUrl === "string" && item.photoUrl.trim()
    ? [item.photoUrl.trim()]
    : [];
  revokeUrls(photoPreviews.value);
  photoPreviews.value = remotePhotoUrls
    .filter((url) => typeof url === "string" && url.trim())
    .slice(0, 6)
    .map((url) => ({
      id: `${Date.now()}-${photoId++}`,
      file: null,
      url,
      isRemote: true,
    }));
};

const loadEditItem = async () => {
  if (!isEditMode.value || !editItemId.value) {
    return;
  }
  submitMessage.value = "正在加载待编辑物品...";
  try {
    const responseBody = await fetchMyPublishItems();
    const list = normalizeListData(responseBody);
    const target = list.find((item) => {
      const id = Number(item?.itemId ?? item?.id ?? 0);
      return id === editItemId.value;
    });
    if (!target) {
      throw new Error("未找到可编辑物品");
    }
    fillFormForEdit(target);
    submitMessage.value = "";
  } catch (error) {
    submitMessage.value = error?.message || "加载编辑数据失败";
  }
};

const loadCategories = async () => {
  try {
    const response = await fetch("/api/home/categories");
    const body = await response.json().catch(() => null);
    if (!response.ok || !body?.success || !Array.isArray(body.data)) {
      return;
    }

    const remoteCategories = body.data
      .filter((item) => item?.code && item?.name)
      .map((item) => ({
        label:
          item.code === "other"
            ? "其他"
            : `${item.name}（${item.description || ""}）`,
        value: item.code,
      }));
    if (remoteCategories.length) {
      categories.splice(0, categories.length, ...remoteCategories);
    }
  } catch {}
};

const showSubmitMessage = (message, type = "warning") => {
  submitMessage.value = message;
  if (type === "success") {
    ElMessage.success(message);
    return;
  }
  if (type === "error") {
    ElMessage.error(message);
    return;
  }
  ElMessage.warning(message);
};

const handleSubmit = async () => {
  if (submitting.value) return;

  if (!form.value.name.trim()) {
    showSubmitMessage("请填写物品名称");
    return;
  }

  const rawPrice = `${form.value.price ?? ""}`.trim();
  if (!rawPrice) {
    showSubmitMessage("请填写价格");
    return;
  }

  const normalizedPrice = Number(rawPrice);
  if (!Number.isFinite(normalizedPrice)) {
    showSubmitMessage("请填写价格");
    return;
  }

  submitting.value = true;
  submitMessage.value = isEditMode.value
    ? "正在上传图片并保存修改..."
    : "正在上传图片并提交发布...";

  try {
    const uploadUrls = [];
    for (const photo of photoPreviews.value) {
      if (
        photo?.isRemote &&
        typeof photo?.url === "string" &&
        photo.url.trim()
      ) {
        uploadUrls.push(photo.url.trim());
        continue;
      }
      if (!photo?.file) {
        continue;
      }
      const uploadResult = await uploadPublishImage(photo.file);
      if (uploadResult?.data?.url) {
        uploadUrls.push(uploadResult.data.url);
      }
    }

    const payload = {
      name: form.value.name.trim(),
      categoryCode: form.value.category || "other",
      price: Number(normalizedPrice.toFixed(2)),
      purchaseDate: form.value.purchaseDate || null,
      usageDuration: form.value.usageDuration.trim(),
      description: form.value.description.trim(),
      photoUrls: uploadUrls,
    };

    if (isEditMode.value && editItemId.value) {
      await updateMyPublishItem(editItemId.value, payload);
      showSubmitMessage("修改成功", "success");
      window.setTimeout(() => {
        router.push("/profile");
      }, 500);
      return;
    }

    await createPublishItem(payload);

    showSubmitMessage("发布成功", "success");
    revokeUrls(photoPreviews.value);
    photoPreviews.value = [];
    form.value = {
      name: "",
      category: "other",
      price: "",
      purchaseDate: "",
      usageDuration: "",
      description: "",
    };
    window.setTimeout(() => {
      router.push("/");
    }, 600);
  } catch (error) {
    showSubmitMessage(error?.message || "发布失败，请稍后重试", "error");
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadCategories();
  loadEditItem();
});

onBeforeUnmount(() => {
  revokeUrls(photoPreviews.value);
});
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  padding: 24px;
  background: radial-gradient(
      circle at 12% 16%,
      rgba(198, 185, 255, 0.52),
      transparent 32%
    ),
    radial-gradient(
      circle at 84% 8%,
      rgba(255, 204, 227, 0.44),
      transparent 28%
    ),
    linear-gradient(180deg, #fbfbff 0%, #f6f4ff 48%, #f8f9ff 100%);
  background-attachment: fixed;
  color: #443b63;
  box-sizing: border-box;
}

.publish-main {
  max-width: 1320px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 300px;
  gap: 20px;
  align-items: start;
}

.soft-card {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 18px 36px rgba(140, 124, 240, 0.13),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
}

.left-rail {
  padding: 18px 14px;
  position: sticky;
  top: 18px;
}

.rail-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 700;
  color: #6656b6;
}

.left-nav-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.left-nav-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
  background: #f8f5ff;
}

.left-nav-item.is-active {
  background: linear-gradient(140deg, #ece7ff 0%, #ffeef7 100%);
}

.left-nav-icon {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #ffffff;
}

.left-nav-text {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #5f4fa8;
}

.left-nav-sub {
  margin: 3px 0 0;
  font-size: 12px;
  color: #948ab7;
}

.center-rail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.publish-header {
  padding: 20px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.publish-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.publish-title {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #6452b5;
}

.publish-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #9388b5;
}

.form-card {
  padding: 24px;
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #635790;
}

.form-input {
  min-height: 44px;
  border: 1px solid #ebe5ff;
  border-radius: 14px;
  padding: 10px 14px;
  background: #fdfcff;
  font-size: 14px;
  color: #4a3f6c;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.form-input[type="number"]::-webkit-outer-spin-button,
.form-input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.form-input[type="number"] {
  -moz-appearance: textfield;
  appearance: textfield;
}

.form-input:focus {
  border-color: #c6b9ff;
  box-shadow: 0 0 0 4px rgba(198, 185, 255, 0.34);
}

.form-select {
  appearance: none;
}

:deep(.publish-date-picker) {
  width: 100%;
}

:deep(.publish-date-picker .el-input__wrapper) {
  min-height: 44px;
  border-radius: 14px;
  border: 1px solid #ebe5ff;
  background: #fdfcff;
  box-shadow: none;
  cursor: pointer;
}

:deep(.publish-date-picker .el-input__inner) {
  color: #4a3f6c;
  cursor: pointer;
}

:deep(.publish-date-picker .el-input__prefix),
:deep(.publish-date-picker .el-input__suffix),
:deep(.publish-date-picker .el-input__icon) {
  cursor: pointer;
}

:deep(.publish-date-picker .el-input__wrapper:hover) {
  border-color: #d7cbff;
}

:deep(.publish-date-picker.is-focus .el-input__wrapper),
:deep(.publish-date-picker .el-input__wrapper.is-focus) {
  border-color: #c6b9ff;
  box-shadow: 0 0 0 4px rgba(198, 185, 255, 0.34);
}

.form-textarea {
  min-height: 120px;
  resize: vertical;
}

.upload-area {
  position: relative;
  border: 1px dashed #c8bbff;
  border-radius: 16px;
  padding: 20px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: linear-gradient(145deg, #f9f6ff 0%, #fff8fc 100%);
  cursor: pointer;
}

.upload-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.upload-text {
  color: #6754b9;
  font-weight: 600;
}

.upload-hint {
  font-size: 12px;
  color: #9c92be;
}

.preview-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.preview-card {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #ede7ff;
  background: #fefcff;
  box-shadow: 0 8px 20px rgba(140, 124, 240, 0.12);
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
  background: #efe9ff;
  color: #6754b9;
  font-size: 12px;
  cursor: pointer;
}

.submit-message {
  margin: 0;
  font-size: 13px;
  color: #6b5ab5;
}

.primary-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 20px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #8c7cf0 0%, #c6b9ff 58%, #ffbfd9 100%);
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 10px 20px rgba(140, 124, 240, 0.34);
}

.publish-header__submit {
  background: #8c7cf0;
  box-shadow: 0 10px 20px rgba(140, 124, 240, 0.36);
}

.primary-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.ghost-btn {
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 14px;
  border: 1px solid #e3dcff;
  background: #ffffff;
  color: #61548f;
  cursor: pointer;
}

.right-rail {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 18px;
}

.status-card {
  padding: 16px;
}

.status-line {
  margin: 0;
  color: #7465af;
  font-size: 13px;
}

.status-progress {
  margin-top: 10px;
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: #eee9ff;
  overflow: hidden;
}

.status-progress__bar {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #8c7cf0, #fcb0cd);
  transition: width 0.25s ease;
}

.status-tip {
  margin: 10px 0 0;
  color: #9a8fbd;
  font-size: 12px;
  line-height: 1.5;
}

:deep(.publish-date-popper) {
  border: 1px solid #ece5ff;
  border-radius: 18px;
  padding: 10px;
  box-shadow: 0 16px 34px rgba(140, 124, 240, 0.2);
}

:deep(.publish-date-popper .el-picker-panel__body-wrapper),
:deep(.publish-date-popper .el-picker-panel__body) {
  background: linear-gradient(180deg, #fdfbff 0%, #f9f6ff 100%);
}

:deep(.publish-date-popper .el-date-picker__header-label),
:deep(.publish-date-popper .el-picker-panel__icon-btn) {
  color: #6656b6;
  cursor: pointer;
}

:deep(.publish-date-popper .el-date-table th) {
  color: #8d82b3;
}

:deep(.publish-date-popper .el-date-table td.available) {
  color: #4a3f6c;
}

:deep(
    .publish-date-popper
      .el-date-table
      td.current:not(.disabled)
      .el-date-table-cell__text
  ) {
  color: #ffffff;
  background: linear-gradient(135deg, #8c7cf0 0%, #c6b9ff 100%);
  border-radius: 9px;
}

:deep(.publish-date-popper .el-date-table td.today .el-date-table-cell__text) {
  color: #7a67cd;
}

:deep(
    .publish-date-popper
      .el-date-table
      td.available:hover
      .el-date-table-cell__text
  ),
:deep(
    .publish-date-popper
      .el-date-table
      td.available.in-range
      .el-date-table-cell__text
  ) {
  background: #efe9ff;
  color: #6150ab;
  border-radius: 9px;
}

:deep(.publish-date-popper .el-picker-panel__footer .el-button) {
  border-radius: 999px;
}

:deep(.publish-date-popper .el-picker-panel__footer .el-button--text) {
  color: #7464bb;
}

:deep(.publish-date-popper .el-picker-panel__footer .el-button--default) {
  border-color: #d9ceff;
  color: #6150ab;
}

@media (max-width: 960px) {
  .publish-page {
    padding: 16px;
  }

  .publish-main {
    grid-template-columns: minmax(0, 1fr);
  }

  .left-rail,
  .right-rail {
    position: static;
  }

  .publish-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .publish-header__actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 680px) {
  .publish-page {
    padding: 12px;
  }

  .publish-title {
    font-size: 26px;
  }

  .publish-header,
  .form-card,
  .left-rail,
  .status-card {
    border-radius: 18px;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .preview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
