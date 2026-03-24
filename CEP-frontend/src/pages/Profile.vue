<template>
  <div class="profile-page">
    <div class="profile-layout">
      <aside class="profile-sidebar card">
        <button
          :class="[
            'menu-item',
            selectedMenu === 'idle' ? 'menu-item--active' : '',
          ]"
          type="button"
          @click="selectMenu('idle')"
        >
          <el-icon><User /></el-icon>
          <span>我的闲置</span>
        </button>

        <div class="menu-group">
          <button
            class="menu-title"
            type="button"
            @click="tradeOpen = !tradeOpen"
          >
            <span class="menu-title__left"
              ><el-icon><Goods /></el-icon>我的交易</span
            >
            <el-icon
              ><ArrowDown v-if="tradeOpen" /><ArrowRight v-else
            /></el-icon>
          </button>
          <div v-if="tradeOpen" class="menu-sublist">
            <button
              v-for="item in tradeMenus"
              :key="item.key"
              :class="[
                'sub-item',
                selectedMenu === item.key ? 'sub-item--active' : '',
              ]"
              type="button"
              @click="selectMenu(item.key)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <button
          :class="[
            'menu-item',
            selectedMenu === 'favorite' ? 'menu-item--active' : '',
          ]"
          type="button"
          @click="selectMenu('favorite')"
        >
          <el-icon><Star /></el-icon>
          <span>我的收藏</span>
        </button>
      </aside>

      <main class="profile-main card">
        <section class="profile-banner">
          <div class="profile-user">
            <el-avatar :size="74" :src="userInfo.avatar" class="profile-avatar">
              <el-icon :size="34"><UserFilled /></el-icon>
            </el-avatar>
            <div>
              <h2 class="user-name">{{ userInfo.username }}</h2>
              <div class="user-stats">
                <span>{{ userInfo.fans }}粉丝</span><span>|</span>
                <span>{{ userInfo.following }}关注</span>
              </div>
            </div>
          </div>
          <div class="banner-actions">
            <button class="action-btn" type="button" @click="openEditDialog">
              编辑资料
            </button>
            <button
              class="action-btn action-btn--danger"
              type="button"
              @click="handleLogout"
            >
              退出登录
            </button>
            <el-button class="home-btn" plain @click="goHome"
              ><el-icon><House /></el-icon>返回首页</el-button
            >
          </div>
        </section>

        <section class="profile-content">
          <div v-if="selectedMenu === 'idle'" class="credit-head">
            <h3 class="credit-title">信用及评价</h3>
            <span class="credit-count">{{ reviewTotal }}</span>
          </div>

          <div v-if="selectedMenu === 'idle'" class="review-tabs">
            <button
              v-for="tab in reviewTabs"
              :key="tab.key"
              :class="[
                'review-tab',
                activeReviewTab === tab.key ? 'review-tab--active' : '',
              ]"
              type="button"
              @click="activeReviewTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <div v-if="selectedMenu === 'idle'" class="review-list">
            <div
              v-for="item in filteredReviewList"
              :key="item.id"
              class="review-item"
            >
              <div class="review-item__top">
                <span class="review-user">{{ item.user }}</span>
                <span class="review-time">{{ item.time }}</span>
              </div>
              <p class="review-content">{{ item.content }}</p>
            </div>
          </div>

          <div v-else class="section-head">
            <h3 class="section-title">{{ currentSection.title }}</h3>
          </div>
        </section>
      </main>
    </div>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑资料"
      width="420px"
      destroy-on-close
    >
      <div class="edit-avatar-row">
        <el-avatar :size="64" :src="editForm.avatar || userInfo.avatar"
          ><el-icon><UserFilled /></el-icon
        ></el-avatar>
        <input
          ref="avatarInputRef"
          class="avatar-input"
          type="file"
          accept="image/*"
          @change="handleAvatarChange"
        />
        <el-button size="small" @click="triggerAvatarSelect"
          >更换头像</el-button
        >
      </div>
      <el-form label-width="86px">
        <el-form-item label="用户名"
          ><el-input v-model="editForm.username" maxlength="20" show-word-limit
        /></el-form-item>
        <el-form-item label="新密码"
          ><el-input
            v-model="editForm.password"
            type="password"
            show-password
            placeholder="不修改可留空"
        /></el-form-item>
      </el-form>
      <p v-if="editError" class="edit-error">{{ editError }}</p>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import {
  ArrowDown,
  ArrowRight,
  Goods,
  House,
  Star,
  User,
  UserFilled,
} from "@element-plus/icons-vue";
import { authState, initAuthSession, logout } from "../auth/session";

const router = useRouter();

const userInfo = reactive({
  avatar: "",
  username: "金星焦糖味的柑桔",
  fans: 0,
  following: 0,
  passwordUpdatedAt: "2026-03-01",
});
const tradeMenus = [
  { key: "trade-published", label: "我发布的" },
  { key: "trade-sold", label: "我卖出的" },
  { key: "trade-bought", label: "我买到的" },
];

const tradeOpen = ref(true);
const selectedMenu = ref("idle");
const editDialogVisible = ref(false);
const avatarInputRef = ref(null);
const editError = ref("");
const activeReviewTab = ref("all");
const editForm = reactive({ avatar: "", username: "", password: "" });

const sectionMap = {
  idle: { title: "我的闲置" },
  "trade-published": { title: "我发布的" },
  "trade-sold": { title: "我卖出的" },
  "trade-bought": { title: "我买到的" },
  favorite: { title: "我的收藏" },
};

const reviewList = [
  {
    id: 1,
    user: "计算机学院-王同学",
    time: "2026-03-20",
    source: "buyer",
    content: "卖家回复很快，商品描述一致，交易顺利。",
  },
  {
    id: 2,
    user: "外语学院-李同学",
    time: "2026-03-15",
    source: "seller",
    content: "沟通友好，见面交易很准时，体验不错。",
  },
  {
    id: 3,
    user: "经管学院-周同学",
    time: "2026-03-10",
    source: "seller",
    content: "物品成色很好，和图片一致，推荐。",
  },
];
const reviewTotal = 34;
const reviewTabs = [
  { key: "all", label: "全部" },
  { key: "good", label: "好评 32" },
  { key: "buyer", label: "来自买家 5" },
  { key: "seller", label: "来自卖家 29" },
];

const filteredReviewList = computed(() => {
  if (activeReviewTab.value === "buyer") {
    return reviewList.filter((item) => item.source === "buyer");
  }
  if (activeReviewTab.value === "seller") {
    return reviewList.filter((item) => item.source === "seller");
  }
  return reviewList;
});

const currentSection = computed(
  () => sectionMap[selectedMenu.value] || sectionMap.idle
);

const selectMenu = (key) => {
  selectedMenu.value = key;
};
const goHome = () => {
  router.push("/");
};
const openEditDialog = () => {
  editForm.avatar = userInfo.avatar;
  editForm.username = userInfo.username;
  editForm.password = "";
  editError.value = "";
  editDialogVisible.value = true;
};
const triggerAvatarSelect = () => {
  avatarInputRef.value?.click();
};
const handleAvatarChange = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    editForm.avatar = typeof reader.result === "string" ? reader.result : "";
  };
  reader.readAsDataURL(file);
  event.target.value = "";
};
const saveProfile = () => {
  const username = editForm.username.trim();
  if (!username) {
    editError.value = "用户名不能为空";
    return;
  }
  if (editForm.password && editForm.password.length < 6) {
    editError.value = "新密码至少 6 位";
    return;
  }
  userInfo.username = username;
  if (editForm.avatar) userInfo.avatar = editForm.avatar;
  if (editForm.password)
    userInfo.passwordUpdatedAt = new Date().toISOString().slice(0, 10);
  editDialogVisible.value = false;
  ElMessage.success("资料已更新");
};

const syncUserInfo = () => {
  if (!authState.user) return;
  userInfo.username = authState.user.username || "校园用户";
};

const handleLogout = async () => {
  try {
    await logout();
    ElMessage.success("已退出登录");
  } catch (error) {
    ElMessage.warning(error.message || "退出登录异常");
  } finally {
    router.push("/");
  }
};

onMounted(async () => {
  await initAuthSession();
  if (!authState.user) {
    router.push("/");
    return;
  }
  syncUserInfo();
});
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f7fb;
}
.profile-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 16px;
  padding: 16px 16px 16px 0;
}
.card {
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
}
.profile-sidebar {
  border-radius: 0 14px 14px 0;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.menu-item,
.menu-title,
.sub-item {
  width: 100%;
  border: none;
  text-align: left;
  background: transparent;
  color: #1f2937;
  cursor: pointer;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
}
.menu-item--active {
  background: #eef4ff;
  color: #1d4ed8;
}
.menu-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  font-size: 15px;
  font-weight: 600;
}
.menu-title__left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.menu-sublist {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
}
.sub-item {
  padding: 9px 36px;
  color: #4b5563;
  font-size: 14px;
  border-radius: 8px;
}
.sub-item:hover,
.sub-item--active {
  color: #1d4ed8;
  background: #eff6ff;
}
.profile-main {
  overflow: hidden;
}
.profile-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #e0f2fe 0%, #eff6ff 100%);
}
.profile-user {
  display: flex;
  align-items: center;
  gap: 14px;
}
.profile-avatar {
  background: linear-gradient(135deg, #bfdbfe, #93c5fd);
  color: #1d4ed8;
}
.user-name {
  margin: 0;
  font-size: 30px;
  color: #111827;
}
.user-stats {
  margin-top: 6px;
  display: flex;
  gap: 8px;
  font-size: 16px;
  color: #4b5563;
}
.banner-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.action-btn {
  border: none;
  border-radius: 999px;
  padding: 9px 16px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.action-btn--danger {
  background: #fee2e2;
  color: #b91c1c;
}
.home-btn {
  border-radius: 999px;
}
.profile-content {
  padding: 16px 22px 22px;
  min-height: 380px;
}
.credit-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}
.credit-title {
  margin: 0;
  font-size: 24px;
  color: #111827;
}
.credit-count {
  font-size: 20px;
  color: #6b7280;
}
.review-tabs {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.review-tab {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 14px;
  background: #f1f5f9;
  color: #111827;
  font-size: 15px;
  cursor: pointer;
}
.review-tab--active {
  border-color: #bfdbfe;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 700;
}
.review-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.review-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 12px;
}
.review-item__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.review-user {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}
.review-time {
  font-size: 12px;
  color: #6b7280;
}
.review-content {
  margin: 0;
  font-size: 14px;
  color: #374151;
  line-height: 1.5;
}
.section-head {
  display: flex;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}
.section-title {
  margin: 0;
  font-size: 24px;
  color: #111827;
}
.edit-avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.avatar-input {
  display: none;
}
.edit-error {
  margin: 0;
  color: #dc2626;
  font-size: 13px;
}
@media (max-width: 980px) {
  .profile-layout {
    grid-template-columns: minmax(0, 1fr);
    padding: 12px;
  }
  .profile-sidebar {
    border-radius: 14px;
  }
  .profile-banner {
    flex-direction: column;
    align-items: flex-start;
  }
  .banner-actions {
    width: 100%;
    justify-content: flex-end;
  }
  .user-name {
    font-size: 24px;
  }
}
</style>
