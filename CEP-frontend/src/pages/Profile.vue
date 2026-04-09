<template>
  <div class="profile-page">
    <div class="profile-layout">
      <aside class="profile-sidebar soft-card">
        <p class="sidebar-title">个人主页</p>
        <button
          :class="[
            'menu-item',
            selectedMenu === 'idle' ? 'menu-item--active' : '',
          ]"
          type="button"
          @click="selectMenu('idle')"
        >
          <el-icon><User /></el-icon>
          <span>我的评价</span>
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

        <button
          v-for="item in followMenus"
          :key="item.key"
          :class="[
            'menu-item',
            selectedMenu === item.key ? 'menu-item--active' : '',
          ]"
          type="button"
          @click="selectMenu(item.key)"
        >
          <el-icon><User /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </aside>

      <main class="profile-main">
        <section class="hero-card soft-card">
          <div class="profile-user">
            <el-avatar :size="78" :src="userInfo.avatar" class="profile-avatar">
              <el-icon :size="34"><UserFilled /></el-icon>
            </el-avatar>
            <div>
              <div class="user-name-row">
                <h2 class="user-name">{{ userInfo.username }}</h2>
                <div class="user-credit-badges">
                  <span class="credit-badge">
                    <span class="credit-badge__icon">⭐</span>
                    卖家信用{{ userInfo.sellerCredit }}
                  </span>
                  <span class="credit-badge">
                    <span class="credit-badge__icon">⭐</span>
                    买家信用{{ userInfo.buyerCredit }}
                  </span>
                  <div class="credit-help">
                    <button
                      class="credit-help__btn"
                      type="button"
                      aria-label="查看信用等级说明"
                    >
                      ?
                    </button>
                    <div class="credit-help__popover">
                      <p class="credit-help__line">初始信用分：100 分</p>
                      <p class="credit-help__line">好评 +1 分，差评 -1 分</p>
                      <table class="credit-help__table">
                        <thead>
                          <tr>
                            <th>信用分数</th>
                            <th>信用等级</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>&lt; 90 分</td>
                            <td>信用较差</td>
                          </tr>
                          <tr>
                            <td>90 ~ 109 分</td>
                            <td>信用良好</td>
                          </tr>
                          <tr>
                            <td>110 ~ 139 分</td>
                            <td>信用优秀</td>
                          </tr>
                          <tr>
                            <td>≥ 140 分</td>
                            <td>信用极好</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>
              <div class="user-stats">
                <span>{{ userInfo.fans }} 粉丝</span>
                <span class="dot">·</span>
                <span>{{ userInfo.following }} 关注</span>
              </div>
              <div class="banner-actions">
                <button
                  class="action-btn"
                  type="button"
                  @click="openEditDialog"
                >
                  编辑资料
                </button>
                <button
                  class="action-btn action-btn--danger"
                  type="button"
                  @click="handleLogout"
                >
                  退出登录
                </button>
                <button
                  class="action-btn action-btn--home"
                  type="button"
                  @click="goHome"
                >
                  返回首页
                </button>
              </div>
            </div>
          </div>
        </section>

        <section
          :class="[
            'profile-content',
            'soft-card',
            selectedMenu === 'idle' ? 'profile-content--review-fixed' : '',
          ]"
        >
          <div class="section-head">
            <div class="section-head__left">
              <h3 class="section-title">{{ currentSection.title }}</h3>
              <span v-if="selectedMenu === 'idle'" class="section-count"
                >共 {{ reviewTotal }} 条</span
              >
              <span
                v-else-if="detailMenuKeys.includes(selectedMenu)"
                class="section-count"
                >共 {{ displaySectionItems.length }} 条</span
              >
            </div>
            <div
              v-if="selectedMenu === 'trade-published'"
              class="section-head__actions"
            >
              <button
                class="batch-btn batch-btn--danger"
                type="button"
                @click="startBatchDeleteMode"
              >
                {{
                  batchActionMode === "delete"
                    ? `确认删除（${selectedPublishedItemIds.length}）`
                    : "批量删除"
                }}
              </button>
              <button
                class="batch-btn batch-btn--shelf"
                type="button"
                @click="startBatchOffShelfMode"
              >
                {{
                  batchActionMode === "off_shelf"
                    ? `确认下架（${selectedPublishedItemIds.length}）`
                    : "批量下架"
                }}
              </button>
            </div>
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

          <div
            v-else-if="tradeOrderStatusMenuKeys.includes(selectedMenu)"
            class="review-tabs"
          >
            <button
              v-for="tab in tradeOrderStatusTabs"
              :key="tab.key"
              :class="[
                'review-tab',
                currentTradeOrderStatus === tab.key ? 'review-tab--active' : '',
              ]"
              type="button"
              @click="onTradeOrderStatusChange(tab.key)"
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
                <img
                  class="review-avatar"
                  :src="item.avatar"
                  :alt="item.user"
                />
                <div class="review-main">
                  <div class="review-user-row">
                    <span class="review-user">{{ item.user }}</span>
                    <span class="review-identity">{{ item.identity }}</span>
                  </div>
                  <div class="review-content-row">
                    <span class="review-tag">
                      {{ item.rating === "good" ? "好评" : "差评" }}
                    </span>
                    <p class="review-content">{{ item.content }}</p>
                  </div>
                  <span class="review-time">{{ item.time }}</span>
                </div>
              </div>
            </div>
          </div>

          <div
            v-else-if="selectedMenu === 'trade-published'"
            class="published-grid"
            @click.self="cancelBatchMode"
          >
            <article
              v-for="item in currentSectionItems"
              :key="item.id"
              class="published-card"
              @click="handlePublishedCardClick(item)"
            >
              <div class="published-card__thumb">
                <label
                  v-if="batchActionMode"
                  class="published-card__check"
                  @click.stop
                >
                  <input
                    v-model="selectedPublishedItemIds"
                    type="checkbox"
                    :value="item.itemId"
                    @click.stop
                  />
                </label>
                <img
                  v-if="getTradeItemPhoto(item)"
                  :src="getTradeItemPhoto(item)"
                  :alt="item.title"
                  class="published-card__thumb-image"
                  loading="lazy"
                />
                <div v-else class="published-card__thumb-placeholder">
                  暂无图片
                </div>
                <span class="published-card__status">{{
                  mapStatusText(item)
                }}</span>
              </div>
              <div class="published-card__body">
                <div class="published-card__title-row">
                  <h4 class="published-card__title" :title="item.title">
                    {{ item.title }}
                  </h4>
                  <div class="published-card__menu-wrap">
                    <button
                      class="published-card__more-btn"
                      type="button"
                      @click.stop="togglePublishedActionMenu(item.itemId)"
                    >
                      ⋯
                    </button>
                    <div
                      v-if="activePublishedActionMenuId === item.itemId"
                      class="published-card__menu"
                    >
                      <button
                        class="published-card__menu-item"
                        type="button"
                        @click.stop="onPublishedActionEdit(item)"
                      >
                        编辑
                      </button>
                      <button
                        class="published-card__menu-item published-card__menu-item--danger"
                        type="button"
                        @click.stop="onPublishedActionDelete(item)"
                      >
                        删除
                      </button>
                      <button
                        class="published-card__menu-item published-card__menu-item--shelf"
                        type="button"
                        @click.stop="onPublishedActionToggleShelf(item)"
                      >
                        {{ item.status === "OFF_SHELF" ? "上架" : "下架" }}
                      </button>
                    </div>
                  </div>
                </div>
                <p class="published-card__meta">
                  {{ item.price }} · {{ item.time }}
                </p>
                <p class="published-card__meta published-card__meta--quantity">
                  {{ buildPublishedQuantityText(item) }}
                </p>
                <p
                  v-if="item.description"
                  class="published-card__desc"
                  :title="item.description"
                >
                  {{ item.description }}
                </p>
              </div>
            </article>

            <div
              v-if="
                !currentSectionItems.length &&
                (selectedMenu === 'following' || selectedMenu === 'fans')
              "
              class="pending-empty"
            >
              暂无用户
            </div>
            <div v-else-if="!currentSectionItems.length" class="pending-empty">
              当前暂无物品
            </div>
          </div>

          <div
            v-else-if="detailMenuKeys.includes(selectedMenu)"
            class="section-list"
          >
            <template
              v-if="selectedMenu === 'following' || selectedMenu === 'fans'"
            >
              <article
                v-for="item in currentSectionItems"
                :key="`${selectedMenu}-${item.userId}`"
                class="follow-user-item"
              >
                <div class="follow-user-item__main">
                  <el-avatar
                    :size="44"
                    :src="item.avatar"
                    class="follow-user-item__avatar"
                  >
                    <el-icon><UserFilled /></el-icon>
                  </el-avatar>
                  <div>
                    <p class="follow-user-item__name">{{ item.title }}</p>
                    <p class="follow-user-item__meta">
                      关注时间：{{ item.time || "-" }}
                    </p>
                  </div>
                </div>
                <button
                  class="section-item__btn section-item__btn--contact"
                  type="button"
                  @click="goToOtherProfile(item)"
                >
                  查看主页
                </button>
              </article>
            </template>

            <template v-else>
              <article
                v-for="item in displaySectionItems"
                :key="item.id"
                class="section-item"
              >
                <div>
                  <h4 class="section-item__title">{{ item.title }}</h4>
                  <p class="section-item__meta">
                    {{ item.price }} · {{ item.time }}
                    <template
                      v-if="tradeOrderStatusMenuKeys.includes(selectedMenu)"
                    >
                      · {{ mapTradeOrderStatusText(item.status, item) }}
                    </template>
                  </p>
                </div>
                <div class="section-item__actions">
                  <button
                    v-if="
                      selectedMenu === 'trade-bought' &&
                      item.status === 'PENDING_PAYMENT'
                    "
                    class="section-item__btn section-item__btn--edit"
                    type="button"
                    @click="goPay(item.orderId || item.id)"
                  >
                    去付款
                  </button>
                  <button
                    v-if="
                      tradeOrderStatusMenuKeys.includes(selectedMenu) &&
                      item.status === 'PENDING_PAYMENT'
                    "
                    class="section-item__btn section-item__btn--danger"
                    type="button"
                    @click="handleTradeOrderCancel(item)"
                  >
                    取消订单
                  </button>
                  <button
                    v-if="
                      selectedMenu === 'trade-sold' &&
                      item.status === 'PENDING_CONFIRMATION' &&
                      !item.sellerConfirmed
                    "
                    class="section-item__btn section-item__btn--edit"
                    type="button"
                    @click="handleSellerConfirmDelivered(item)"
                  >
                    确认已交付物品
                  </button>
                  <button
                    v-if="
                      selectedMenu === 'trade-bought' &&
                      item.status === 'PENDING_CONFIRMATION' &&
                      !item.buyerConfirmed
                    "
                    class="section-item__btn section-item__btn--edit"
                    type="button"
                    @click="handleBuyerConfirmReceived(item)"
                  >
                    确认已收到物品
                  </button>
                  <button
                    v-if="
                      selectedMenu === 'trade-bought' &&
                      item.status === 'PENDING_CONFIRMATION' &&
                      item.refundStatus !== 'APPLIED'
                    "
                    class="section-item__btn section-item__btn--danger"
                    type="button"
                    @click="handleApplyRefund(item, 'NO_RECEIPT')"
                  >
                    申请退款（未收到货）
                  </button>
                  <button
                    v-if="
                      selectedMenu === 'trade-bought' &&
                      item.status === 'PENDING_CONFIRMATION' &&
                      item.refundStatus !== 'APPLIED'
                    "
                    class="section-item__btn section-item__btn--danger"
                    type="button"
                    @click="handleApplyRefund(item, 'RETURN_AFTER_RECEIPT')"
                  >
                    申请退款（已收到货）
                  </button>
                  <button
                    v-if="
                      selectedMenu === 'trade-sold' &&
                      item.status === 'PENDING_CONFIRMATION' &&
                      item.refundStatus === 'APPLIED'
                    "
                    class="section-item__btn section-item__btn--danger"
                    type="button"
                    @click="handleApproveRefund(item)"
                  >
                    {{
                      item.refundType === "RETURN_AFTER_RECEIPT"
                        ? "确认收到退货并退款"
                        : "同意退款"
                    }}
                  </button>
                  <button
                    v-if="selectedMenu === 'trade-sold'"
                    class="section-item__btn section-item__btn--contact"
                    type="button"
                    @click="handleContactBuyer(item)"
                  >
                    联系买家
                  </button>
                  <button
                    v-if="selectedMenu === 'trade-bought'"
                    class="section-item__btn section-item__btn--contact"
                    type="button"
                    @click="handleContactSeller(item)"
                  >
                    联系卖家
                  </button>
                  <button
                    v-if="
                      selectedMenu === 'trade-bought' &&
                      item.status !== 'PENDING_CONFIRMATION' &&
                      !isRefundAfterSaleOrder(item)
                    "
                    class="section-item__btn section-item__btn--rebuy"
                    type="button"
                    @click="handleRebuy(item)"
                  >
                    再次购买
                  </button>
                  <button
                    class="section-item__btn"
                    type="button"
                    @click="goToItemDetail(item.itemId)"
                  >
                    查看详情
                  </button>
                </div>
              </article>
            </template>

            <div
              v-if="
                !displaySectionItems.length &&
                (selectedMenu === 'following' || selectedMenu === 'fans')
              "
              class="pending-empty"
            >
              暂无用户
            </div>
            <div v-else-if="!displaySectionItems.length" class="pending-empty">
              当前暂无物品
            </div>
          </div>
        </section>
      </main>
    </div>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑资料"
      width="420px"
      destroy-on-close
      class="profile-edit-dialog"
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
      <el-form label-width="86px" class="edit-form-grid">
        <el-form-item label="用户名"
          ><el-input v-model="editForm.username" maxlength="20" show-word-limit
        /></el-form-item>
        <el-form-item label="姓名"
          ><el-input
            v-model="editForm.name"
            maxlength="30"
            placeholder="请输入姓名"
        /></el-form-item>
        <el-form-item label="联系电话"
          ><el-input
            v-model="editForm.phone"
            maxlength="30"
            placeholder="请输入联系电话"
        /></el-form-item>
        <el-form-item label="收货地址"
          ><el-input
            v-model="editForm.address"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入收货地址"
        /></el-form-item>
      </el-form>
      <p v-if="editError" class="edit-error">{{ editError }}</p>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="itemEditDialogVisible"
      title="编辑我发布的物品"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="86px">
        <el-form-item label="名称"
          ><el-input
            v-model="itemEditForm.name"
            maxlength="120"
            show-word-limit
        /></el-form-item>
        <el-form-item label="分类"
          ><el-input
            v-model="itemEditForm.categoryCode"
            placeholder="例如：数码、图书、其他"
        /></el-form-item>
        <el-form-item label="价格"
          ><el-input-number
            v-model="itemEditForm.price"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 100%"
        /></el-form-item>
        <el-form-item label="购买日期"
          ><el-date-picker
            v-model="itemEditForm.purchaseDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
        /></el-form-item>
        <el-form-item label="使用时长"
          ><el-input v-model="itemEditForm.usageDuration"
        /></el-form-item>
        <el-form-item label="描述"
          ><el-input
            v-model="itemEditForm.description"
            type="textarea"
            :rows="3"
        /></el-form-item>
      </el-form>
      <p v-if="itemEditError" class="edit-error">{{ itemEditError }}</p>
      <template #footer>
        <el-button @click="itemEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePublishedItemEdit"
          >保存</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import {
  ArrowDown,
  ArrowRight,
  Goods,
  Star,
  User,
  UserFilled,
} from "@element-plus/icons-vue";
import {
  authState,
  initAuthSession,
  logout,
} from "../service/common/authSessionService";
import {
  fetchBoughtOrderContact,
  fetchBoughtItems,
  fetchFavoriteItems,
  fetchFansUsers,
  fetchFollowingUsers,
  fetchProfileOverview,
  fetchProfileReviews,
  fetchSoldOrderContact,
  rebuyBoughtOrder,
  fetchSoldItems,
  updateProfileBasic,
  uploadProfileAvatar,
} from "../service/profile/profileApiService";
import {
  applyTradeOrderRefund,
  approveTradeOrderRefund,
  cancelTradeOrder,
  confirmBuyerReceived,
  confirmSellerDelivered,
} from "../service/payment/paymentApiService";
import {
  deleteMyPublishItem,
  fetchMyPublishItems,
  updateMyPublishItem,
  updateMyPublishItemStatus,
} from "../service/publish/publishApiService";

const router = useRouter();
const PROFILE_SELECTED_MENU_KEY = "profile:selectedMenu";
const TRADE_REMINDER_PREFIX = "[TRADE_REMINDER]";

const userInfo = reactive({
  avatar: "",
  username: "校园用户",
  name: "",
  phone: "",
  address: "",
  fans: 0,
  following: 0,
  sellerCredit: "良好",
  buyerCredit: "良好",
  passwordUpdatedAt: "2026-03-01",
});
const tradeMenus = [
  { key: "trade-published", label: "我发布的" },
  { key: "trade-sold", label: "我卖出的" },
  { key: "trade-bought", label: "我买到的" },
];

const followMenus = [
  { key: "following", label: "我的关注" },
  { key: "fans", label: "我的粉丝" },
];

const tradeOpen = ref(true);
const selectedMenu = ref("idle");
const editDialogVisible = ref(false);
const itemEditDialogVisible = ref(false);
const avatarInputRef = ref(null);
const editError = ref("");
const selectedAvatarFile = ref(null);
const itemEditError = ref("");
const selectedPublishedItemIds = ref([]);
const activePublishedActionMenuId = ref(null);
const batchActionMode = ref("");
const activeReviewTab = ref("all");
const editForm = reactive({
  avatar: "",
  username: "",
  name: "",
  phone: "",
  address: "",
});
const itemEditForm = reactive({
  id: null,
  itemId: null,
  name: "",
  categoryCode: "other",
  price: 0,
  purchaseDate: "",
  usageDuration: "",
  description: "",
  photoUrls: [],
});
const reviewStats = reactive({ total: 0, good: 0, bad: 0 });
const reviewList = ref([]);

const sectionMap = {
  idle: { title: "我的评价" },
  "trade-published": { title: "我发布的" },
  "trade-sold": { title: "我卖出的" },
  "trade-bought": { title: "我买到的" },
  favorite: { title: "我的收藏" },
  following: { title: "我的关注" },
  fans: { title: "我的粉丝" },
};
const detailMenuKeys = [
  "trade-published",
  "trade-sold",
  "trade-bought",
  "favorite",
  "following",
  "fans",
];
const tradeOrderStatusMenuKeys = ["trade-sold", "trade-bought"];
const tradeOrderStatusTabs = [
  { key: "all", label: "全部" },
  { key: "pending-payment", label: "待付款" },
  { key: "pending-confirmation", label: "待确认" },
  { key: "completed", label: "已完成" },
  { key: "cancelled", label: "已取消" },
];
const activeTradeOrderStatusMap = reactive({
  "trade-sold": "all",
  "trade-bought": "all",
});
const sectionItemMap = reactive({
  "trade-published": [],
  "trade-sold": [],
  "trade-bought": [],
  favorite: [],
  following: [],
  fans: [],
});
const loadedMenus = reactive({
  idle: false,
  "trade-published": false,
  "trade-sold": false,
  "trade-bought": false,
  favorite: false,
  following: false,
  fans: false,
});

const reviewTotal = computed(() => reviewStats.total);
const reviewTabs = computed(() => [
  { key: "all", label: "全部" },
  { key: "good", label: `好评 ${reviewStats.good}` },
  { key: "bad", label: `差评 ${reviewStats.bad}` },
]);

const filteredReviewList = computed(() => {
  if (activeReviewTab.value === "good") {
    return reviewList.value.filter((item) => item.rating === "good");
  }
  if (activeReviewTab.value === "bad") {
    return reviewList.value.filter((item) => item.rating === "bad");
  }
  return reviewList.value;
});

const currentSection = computed(
  () => sectionMap[selectedMenu.value] || sectionMap.idle
);
const currentSectionItems = computed(
  () => sectionItemMap[selectedMenu.value] || []
);
const currentTradeOrderStatus = computed(() => {
  if (!tradeOrderStatusMenuKeys.includes(selectedMenu.value)) {
    return "all";
  }
  return activeTradeOrderStatusMap[selectedMenu.value] || "all";
});

const isRefundAfterSaleOrder = (item) => {
  const refundStatus = String(item?.refundStatus || "")
    .trim()
    .toUpperCase();
  return refundStatus === "APPLIED" || refundStatus === "APPROVED";
};

const getTradeOrderApiStatus = (status) => {
  if (status === "refund-after-sale") {
    return "all";
  }
  return status;
};

const getDisplaySectionItems = (menuKey, items) => {
  if (!tradeOrderStatusMenuKeys.includes(menuKey)) {
    return items;
  }
  if (currentTradeOrderStatus.value === "refund-after-sale") {
    return items.filter(isRefundAfterSaleOrder);
  }
  if (currentTradeOrderStatus.value === "pending-confirmation") {
    return items.filter((item) => !isRefundAfterSaleOrder(item));
  }
  return items;
};

const displaySectionItems = computed(() =>
  getDisplaySectionItems(selectedMenu.value, currentSectionItems.value)
);

const normalizeListData = (responseBody) => {
  const payload = responseBody?.data;
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.records)) return payload.records;
  if (Array.isArray(payload?.list)) return payload.list;
  return [];
};

const buildTradeReminderText = ({
  type,
  orderId,
  itemTitle,
  content,
  actionText,
  targetMenu,
}) => {
  const payload = {
    type: String(type || "TRADE_REMINDER").trim() || "TRADE_REMINDER",
    orderId: Number(orderId) > 0 ? Number(orderId) : null,
    itemTitle: String(itemTitle || "").trim(),
    content: String(content || "").trim(),
    actionText: String(actionText || "去查看").trim() || "去查看",
    targetMenu: String(targetMenu || "").trim(),
  };
  return `${TRADE_REMINDER_PREFIX}${JSON.stringify(payload)}`;
};

const sendConversationTextBySocket = async (conversationId, text) => {
  const wsUrl = await buildMessageWebSocketUrl();
  await new Promise((resolve, reject) => {
    const socket = new WebSocket(wsUrl);
    let settled = false;

    const settle = (error) => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timeout);
      if (socket.readyState === WebSocket.OPEN) {
        socket.close();
      }
      if (error) {
        reject(error);
        return;
      }
      resolve();
    };

    const timeout = window.setTimeout(() => {
      settle(new Error("消息提醒发送超时"));
    }, 6000);

    socket.onopen = () => {
      try {
        socket.send(
          JSON.stringify({
            action: "SEND_MESSAGE",
            conversationId: Number(conversationId),
            text,
            imageUrl: "",
          })
        );
        window.setTimeout(() => settle(), 120);
      } catch (error) {
        settle(error instanceof Error ? error : new Error("消息提醒发送失败"));
      }
    };

    socket.onerror = () => {
      settle(new Error("消息提醒发送失败"));
    };

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event?.data || "{}");
        if (String(payload?.eventType || "").toUpperCase() === "ERROR") {
          settle(new Error(payload?.message || "消息提醒发送失败"));
        }
      } catch {
        // ignore invalid payload
      }
    };

    socket.onclose = () => {
      if (!settled) {
        settle();
      }
    };
  });
};

const sendTradeReminderMessage = async ({
  item,
  fetchContactFn,
  type,
  content,
  actionText,
  targetMenu,
}) => {
  const orderId = getTradeOrderId(item);
  if (!orderId) return;

  const responseBody = await fetchContactFn(orderId);
  const contact = responseBody?.data || {};
  const peerUserId = Number(contact.peerUserId || 0);
  const itemId = Number(contact.itemId || item?.itemId || 0);
  if (!peerUserId || !itemId) {
    throw new Error("交易联系人信息无效，无法发送提醒");
  }

  const conversationBody = await createOrGetDirectConversation({
    peerUserId,
    itemId,
  });
  const rawConversation = conversationBody?.data || {};
  const conversationId = Number(
    rawConversation.conversationId || rawConversation.id || 0
  );
  if (!conversationId) {
    throw new Error("会话信息无效，无法发送提醒");
  }

  const text = buildTradeReminderText({
    type,
    orderId,
    itemTitle: contact.itemTitle || item?.title || "",
    content,
    actionText,
    targetMenu,
  });
  await sendConversationTextBySocket(conversationId, text);
};

const selectMenu = async (key) => {
  selectedMenu.value = key;
  if (key !== "trade-published") {
    selectedPublishedItemIds.value = [];
    activePublishedActionMenuId.value = null;
    batchActionMode.value = "";
  }
  localStorage.setItem(PROFILE_SELECTED_MENU_KEY, key);
  try {
    await loadMenuData(key, key === "idle");
  } catch (error) {
    ElMessage.error(error.message || "数据加载失败");
  }
};
const goPay = (orderId) => {
  if (!orderId) {
    ElMessage.warning("订单信息无效");
    return;
  }
  router.push({
    name: "payment-method",
    query: { orderId: String(orderId) },
  });
};
const goToItemDetail = (id) => {
  const resolved = router.resolve(`/item/${id}`);
  window.open(resolved.href, "_blank");
};

const goToOtherProfile = (item) => {
  const userId = Number(item?.userId || 0);
  if (!Number.isInteger(userId) || userId <= 0) {
    ElMessage.warning("用户信息无效");
    return;
  }
  const username = String(item?.title || "校园用户").trim() || "校园用户";
  router.push({
    name: "other-profile",
    params: { name: username },
    query: { userId: String(userId) },
  });
};
const goHome = () => {
  router.push("/");
};
const openEditDialog = () => {
  editForm.avatar = userInfo.avatar;
  editForm.username = userInfo.username;
  editForm.name = userInfo.name;
  editForm.phone = userInfo.phone;
  editForm.address = userInfo.address;
  selectedAvatarFile.value = null;
  editError.value = "";
  editDialogVisible.value = true;
};
const triggerAvatarSelect = () => {
  avatarInputRef.value?.click();
};
const handleAvatarChange = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;
  selectedAvatarFile.value = file;
  const reader = new FileReader();
  reader.onload = () => {
    editForm.avatar = typeof reader.result === "string" ? reader.result : "";
  };
  reader.readAsDataURL(file);
  event.target.value = "";
};
const saveProfile = async () => {
  const username = editForm.username.trim();
  const name = editForm.name.trim();
  const phone = editForm.phone.trim();
  const address = editForm.address.trim();
  if (!username) {
    editError.value = "用户名不能为空";
    return;
  }
  if (!name) {
    editError.value = "姓名不能为空";
    return;
  }
  if (!phone) {
    editError.value = "联系电话不能为空";
    return;
  }
  if (!address) {
    editError.value = "收货地址不能为空";
    return;
  }
  editError.value = "";
  try {
    let avatarUrl = "";
    if (selectedAvatarFile.value) {
      const uploadRes = await uploadProfileAvatar(selectedAvatarFile.value);
      avatarUrl = uploadRes?.data?.url || "";
    }
    const responseBody = await updateProfileBasic({
      username,
      name,
      phone,
      address,
      avatar: avatarUrl,
    });
    const overview = responseBody?.data || {};
    userInfo.username = overview.username || username;
    userInfo.name = overview.name || name;
    userInfo.phone = overview.phone || phone;
    userInfo.address = overview.address || address;
    userInfo.avatar = overview.avatar || userInfo.avatar;
    userInfo.sellerCredit = overview.sellerCredit || userInfo.sellerCredit;
    userInfo.buyerCredit = overview.buyerCredit || userInfo.buyerCredit;
    userInfo.fans = Number(overview.fans || 0);
    userInfo.following = Number(overview.following || 0);
    if (authState.user) {
      authState.user.username = userInfo.username;
    }
    editDialogVisible.value = false;
    ElMessage.success("资料已更新");
  } catch (error) {
    editError.value = error.message || "资料更新失败";
  }
};

const toPrice = (value) => {
  const numberValue = Number(value || 0);
  const fixed = Number.isFinite(numberValue) ? numberValue.toFixed(2) : "0.00";
  return fixed.endsWith(".00") ? `￥${fixed.slice(0, -3)}` : `￥${fixed}`;
};

const mapTradeItem = (item) => {
  const quantityMode = item?.quantityMode || "SINGLE";
  const soldQuantity = Number(item?.soldQuantity ?? 0);
  const totalQuantityRaw = item?.totalQuantity;
  const totalQuantity =
    totalQuantityRaw === null || totalQuantityRaw === undefined
      ? quantityMode === "UNLIMITED"
        ? null
        : 1
      : Number(totalQuantityRaw);
  const remainingQuantityRaw = item?.remainingQuantity;
  const remainingQuantity =
    remainingQuantityRaw === null || remainingQuantityRaw === undefined
      ? quantityMode === "UNLIMITED"
        ? null
        : Math.max(Number(totalQuantity || 1) - soldQuantity, 0)
      : Number(remainingQuantityRaw);

  return {
    id: item.id,
    orderId: item.orderId || item.id,
    itemId: item.itemId || item.id,
    title: item.title || item.name || "未命名物品",
    price: toPrice(item.price),
    time: item.time || "",
    photoUrl: item.photoUrl || "",
    status: item.status || "PUBLISHED",
    buyerConfirmed: Boolean(item.buyerConfirmed),
    sellerConfirmed: Boolean(item.sellerConfirmed),
    refundStatus: item.refundStatus || "NONE",
    refundType: item.refundType || "",
    categoryCode: item.categoryCode || "other",
    purchaseDate: item.purchaseDate || "",
    usageDuration: item.usageDuration || "",
    description: item.description || "",
    photoUrls: Array.isArray(item.photoUrls) ? item.photoUrls : [],
    quantityMode,
    totalQuantity,
    soldQuantity,
    remainingQuantity,
  };
};

const mapFollowUser = (item) => ({
  id: item.userId,
  userId: item.userId,
  title: item.username || "校园用户",
  time: item.followedAt || "",
  avatar: item.avatar || "",
});

const mapTradeOrderStatusText = (status, item) => {
  if (isRefundAfterSaleOrder(item)) {
    return "退款/售后";
  }
  if (status === "PENDING_PAYMENT") {
    return "待付款";
  }
  if (status === "PENDING_CONFIRMATION") {
    return "待确认";
  }
  if (status === "COMPLETED") {
    return "已完成";
  }
  if (status === "CANCELLED") {
    return "已取消";
  }
  return "进行中";
};

const getTradeItemPhoto = (item) => {
  if (item?.photoUrl) {
    return item.photoUrl;
  }
  if (Array.isArray(item?.photoUrls) && item.photoUrls.length > 0) {
    return item.photoUrls[0];
  }
  return "";
};

const mapStatusText = (item) => {
  if (item?.status === "DELETED") {
    return "已删除";
  }
  const isSoldOut =
    item?.quantityMode !== "UNLIMITED" &&
    Number(item?.remainingQuantity ?? 0) <= 0;
  if (isSoldOut) {
    return "已售出";
  }
  if (item?.status === "OFF_SHELF") {
    return "已下架";
  }
  return "已上架";
};

const buildPublishedQuantityText = (item) => {
  if (item?.quantityMode === "UNLIMITED") {
    return "数量：无限量";
  }
  const total = Number(item?.totalQuantity ?? 1);
  const sold = Number(item?.soldQuantity ?? 0);
  const remain = Number(item?.remainingQuantity ?? Math.max(total - sold, 0));
  return `数量：${total}（已售 ${sold}，剩余 ${Math.max(remain, 0)}）`;
};

const reloadMyPublishedItems = async () => {
  const publishedRes = await fetchMyPublishItems();
  sectionItemMap["trade-published"] =
    normalizeListData(publishedRes).map(mapTradeItem);
  selectedPublishedItemIds.value = [];
  activePublishedActionMenuId.value = null;
  batchActionMode.value = "";
  loadedMenus["trade-published"] = true;
};

const startBatchDeleteMode = async () => {
  if (batchActionMode.value !== "delete") {
    batchActionMode.value = "delete";
    selectedPublishedItemIds.value = [];
    activePublishedActionMenuId.value = null;
    ElMessage.info("请勾选要删除的物品后，再点一次“确认删除”");
    return;
  }
  await handleBatchDeletePublishedItems();
};

const startBatchOffShelfMode = async () => {
  if (batchActionMode.value !== "off_shelf") {
    batchActionMode.value = "off_shelf";
    selectedPublishedItemIds.value = [];
    activePublishedActionMenuId.value = null;
    ElMessage.info("请勾选要下架的物品后，再点一次“确认下架”");
    return;
  }
  await handleBatchOffShelfPublishedItems();
};

const cancelBatchMode = () => {
  batchActionMode.value = "";
  selectedPublishedItemIds.value = [];
  activePublishedActionMenuId.value = null;
};

const handlePublishedCardClick = (item) => {
  if (!batchActionMode.value) {
    goToItemDetail(item.itemId);
    return;
  }
  const currentId = String(item.itemId);
  const next = selectedPublishedItemIds.value.map((id) => String(id));
  const index = next.findIndex((id) => id === currentId);
  if (index >= 0) {
    next.splice(index, 1);
  } else {
    next.push(currentId);
  }
  selectedPublishedItemIds.value = next;
};

const togglePublishedActionMenu = (itemId) => {
  activePublishedActionMenuId.value =
    activePublishedActionMenuId.value === itemId ? null : itemId;
};

const onPublishedActionEdit = (item) => {
  activePublishedActionMenuId.value = null;
  router.push({
    name: "publish",
    query: {
      editItemId: String(item.itemId),
    },
  });
};

const onPublishedActionDelete = async (item) => {
  activePublishedActionMenuId.value = null;
  await handleDeletePublishedItem(item);
};

const onPublishedActionToggleShelf = async (item) => {
  activePublishedActionMenuId.value = null;
  await togglePublishedItemShelf(item);
};

const selectedPublishedItems = computed(() => {
  const selectedSet = new Set(
    selectedPublishedItemIds.value.map((itemId) => String(itemId))
  );
  return sectionItemMap["trade-published"].filter((item) =>
    selectedSet.has(String(item.itemId))
  );
});

const handleBatchDeletePublishedItems = async () => {
  const targets = selectedPublishedItems.value;
  if (!targets.length) {
    ElMessage.warning("请先勾选要删除的物品");
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认批量删除已选的 ${targets.length} 件物品吗？删除后不可恢复。`,
      "批量删除确认",
      {
        confirmButtonText: "确认删除",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    await Promise.all(targets.map((item) => deleteMyPublishItem(item.itemId)));
    await reloadMyPublishedItems();
    ElMessage.success("批量删除成功");
  } catch (error) {
    if (error === "cancel" || error === "close") {
      return;
    }
    ElMessage.error(error.message || "批量删除失败");
  }
};

const handleBatchOffShelfPublishedItems = async () => {
  const targets = selectedPublishedItems.value.filter(
    (item) => item.status !== "OFF_SHELF"
  );
  if (!targets.length) {
    ElMessage.warning("已选物品均为下架状态");
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认批量下架已选的 ${targets.length} 件物品吗？`,
      "批量下架确认",
      {
        confirmButtonText: "确认下架",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    await Promise.all(
      targets.map((item) => updateMyPublishItemStatus(item.itemId, "OFF_SHELF"))
    );
    await reloadMyPublishedItems();
    ElMessage.success("批量下架成功");
  } catch (error) {
    if (error === "cancel" || error === "close") {
      return;
    }
    ElMessage.error(error.message || "批量下架失败");
  }
};

const openItemEditDialog = (item) => {
  itemEditForm.id = item.id;
  itemEditForm.itemId = item.itemId;
  itemEditForm.name = item.title || "";
  itemEditForm.categoryCode = item.categoryCode || "other";
  itemEditForm.price = Number(
    item.price?.replace?.("￥", "") || item.price || 0
  );
  itemEditForm.purchaseDate = item.purchaseDate || "";
  itemEditForm.usageDuration = item.usageDuration || "";
  itemEditForm.description = item.description || "";
  itemEditForm.photoUrls = Array.isArray(item.photoUrls)
    ? [...item.photoUrls]
    : [];
  itemEditError.value = "";
  itemEditDialogVisible.value = true;
};

const savePublishedItemEdit = async () => {
  if (!itemEditForm.itemId) {
    itemEditError.value = "物品信息无效";
    return;
  }
  if (!itemEditForm.name.trim()) {
    itemEditError.value = "物品名称不能为空";
    return;
  }
  if (Number(itemEditForm.price) < 0) {
    itemEditError.value = "价格不能小于 0";
    return;
  }

  itemEditError.value = "";
  try {
    await updateMyPublishItem(itemEditForm.itemId, {
      name: itemEditForm.name.trim(),
      categoryCode: itemEditForm.categoryCode || "other",
      price: Number(Number(itemEditForm.price || 0).toFixed(2)),
      purchaseDate: itemEditForm.purchaseDate || null,
      usageDuration: itemEditForm.usageDuration || "",
      description: itemEditForm.description || "",
      photoUrls: itemEditForm.photoUrls,
    });
    await reloadMyPublishedItems();
    itemEditDialogVisible.value = false;
    ElMessage.success("物品已更新");
  } catch (error) {
    itemEditError.value = error.message || "更新失败";
  }
};

const handleDeletePublishedItem = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确认删除「${item.title}」吗？删除后不可恢复。`,
      "删除确认",
      {
        confirmButtonText: "确认删除",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    await deleteMyPublishItem(item.itemId);
    await reloadMyPublishedItems();
    ElMessage.success("已删除");
  } catch (error) {
    if (error === "cancel" || error === "close") {
      return;
    }
    ElMessage.error(error.message || "删除失败");
  }
};

const togglePublishedItemShelf = async (item) => {
  const targetStatus = item.status === "OFF_SHELF" ? "PUBLISHED" : "OFF_SHELF";
  try {
    await updateMyPublishItemStatus(item.itemId, targetStatus);
    await reloadMyPublishedItems();
    ElMessage.success(targetStatus === "PUBLISHED" ? "已上架" : "已下架");
  } catch (error) {
    ElMessage.error(error.message || "操作失败");
  }
};

const applyOverview = (overviewRes) => {
  const overview = overviewRes?.data || {};
  userInfo.avatar = overview.avatar || "";
  userInfo.username =
    overview.username || authState.user?.username || "校园用户";
  userInfo.name = overview.name || "";
  userInfo.phone = overview.phone || "";
  userInfo.address = overview.address || "";
  userInfo.fans = Number(overview.fans || 0);
  userInfo.following = Number(overview.following || 0);
  userInfo.sellerCredit = overview.sellerCredit || "良好";
  userInfo.buyerCredit = overview.buyerCredit || "良好";
};

const applyReviews = (reviewsRes) => {
  const reviewSummary = reviewsRes?.data || {};
  reviewStats.total = Number(reviewSummary.total || 0);
  reviewStats.good = Number(reviewSummary.goodCount || 0);
  reviewStats.bad = Number(reviewSummary.badCount || 0);
  reviewList.value = (reviewSummary.reviews || []).map((item) => ({
    id: item.id,
    user: item.user || "校园用户",
    identity: item.identity || "交易方",
    time: item.time || "",
    rating: item.rating === "bad" ? "bad" : "good",
    avatar: item.avatar || "",
    content: item.content || "",
  }));
};

const loadOverview = async () => {
  const overviewRes = await fetchProfileOverview();
  applyOverview(overviewRes);
};

const loadIdleData = async () => {
  const reviewsRes = await fetchProfileReviews("all");
  applyReviews(reviewsRes);
  loadedMenus.idle = true;
};

const loadMenuData = async (menuKey, force = false) => {
  if (
    !force &&
    loadedMenus[menuKey] &&
    !tradeOrderStatusMenuKeys.includes(menuKey)
  ) {
    return;
  }

  if (menuKey === "idle") {
    await loadIdleData();
    return;
  }

  if (menuKey === "trade-published") {
    await reloadMyPublishedItems();
    return;
  }

  if (menuKey === "trade-sold") {
    const soldRes = await fetchSoldItems(
      getTradeOrderApiStatus(activeTradeOrderStatusMap["trade-sold"])
    );
    sectionItemMap["trade-sold"] = normalizeListData(soldRes).map(mapTradeItem);
    loadedMenus["trade-sold"] = true;
    return;
  }

  if (menuKey === "trade-bought") {
    const boughtRes = await fetchBoughtItems(
      getTradeOrderApiStatus(activeTradeOrderStatusMap["trade-bought"])
    );
    sectionItemMap["trade-bought"] =
      normalizeListData(boughtRes).map(mapTradeItem);
    loadedMenus["trade-bought"] = true;
    return;
  }

  if (menuKey === "favorite") {
    const favoritesRes = await fetchFavoriteItems();
    sectionItemMap.favorite = normalizeListData(favoritesRes).map(mapTradeItem);
    loadedMenus.favorite = true;
    return;
  }

  if (menuKey === "following") {
    const followingRes = await fetchFollowingUsers();
    sectionItemMap.following =
      normalizeListData(followingRes).map(mapFollowUser);
    loadedMenus.following = true;
    return;
  }

  if (menuKey === "fans") {
    const fansRes = await fetchFansUsers();
    sectionItemMap.fans = normalizeListData(fansRes).map(mapFollowUser);
    loadedMenus.fans = true;
  }
};

const onTradeOrderStatusChange = async (status) => {
  if (!tradeOrderStatusMenuKeys.includes(selectedMenu.value)) {
    return;
  }
  activeTradeOrderStatusMap[selectedMenu.value] = status;
  loadedMenus[selectedMenu.value] = false;
  try {
    await loadMenuData(selectedMenu.value, true);
  } catch (error) {
    ElMessage.error(error.message || "状态筛选失败");
  }
};

const handleTradeOrderCancel = async (item) => {
  const orderId = item?.orderId || item?.id;
  if (!orderId) {
    ElMessage.warning("订单信息无效");
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认取消订单「${item.title || "当前订单"}」吗？`,
      "取消订单确认",
      {
        confirmButtonText: "确认取消",
        cancelButtonText: "再想想",
        type: "warning",
      }
    );
    await cancelTradeOrder(orderId);
    if (tradeOrderStatusMenuKeys.includes(selectedMenu.value)) {
      await loadMenuData(selectedMenu.value, true);
    }
    ElMessage.success("订单已取消");
  } catch (error) {
    if (error === "cancel" || error === "close") {
      return;
    }
    ElMessage.error(error.message || "取消订单失败");
  }
};

const handleSellerConfirmDelivered = async (item) => {
  const orderId = getTradeOrderId(item);
  if (!orderId) {
    ElMessage.warning("订单信息无效");
    return;
  }
  try {
    await confirmSellerDelivered(orderId);
    await loadMenuData("trade-sold", true);
    ElMessage.success("已确认交付");
  } catch (error) {
    ElMessage.error(error.message || "确认交付失败");
  }
};

const handleBuyerConfirmReceived = async (item) => {
  const orderId = getTradeOrderId(item);
  if (!orderId) {
    ElMessage.warning("订单信息无效");
    return;
  }
  try {
    await confirmBuyerReceived(orderId);
    await loadMenuData("trade-bought", true);
    ElMessage.success("已确认收货");
  } catch (error) {
    ElMessage.error(error.message || "确认收货失败");
  }
};

const handleApplyRefund = async (item, refundType) => {
  const orderId = getTradeOrderId(item);
  if (!orderId) {
    ElMessage.warning("订单信息无效");
    return;
  }
  try {
    await applyTradeOrderRefund(orderId, refundType);
    await loadMenuData("trade-bought", true);
    ElMessage.success("退款申请已提交");
  } catch (error) {
    ElMessage.error(error.message || "退款申请失败");
  }
};

const handleApproveRefund = async (item) => {
  const orderId = getTradeOrderId(item);
  if (!orderId) {
    ElMessage.warning("订单信息无效");
    return;
  }
  try {
    await approveTradeOrderRefund(orderId);
    await loadMenuData("trade-sold", true);
    ElMessage.success("已完成退款");
  } catch (error) {
    ElMessage.error(error.message || "退款处理失败");
  }
};

const getTradeOrderId = (item) => item?.orderId || item?.id;

const openContactChat = async (item, fetchContactFn) => {
  const orderId = getTradeOrderId(item);
  if (!orderId) {
    ElMessage.warning("订单信息无效");
    return;
  }

  const responseBody = await fetchContactFn(orderId);
  const contact = responseBody?.data || {};
  const peerUserId = Number(contact.peerUserId || 0);
  const itemId = Number(contact.itemId || item?.itemId || 0);
  if (!peerUserId || !itemId) {
    throw new Error("交易联系人信息无效");
  }

  await router.push({
    name: "chat",
    query: {
      sellerUserId: String(peerUserId),
      itemId: String(itemId),
      sellerName: contact.peerName || "校园用户",
      itemTitle: contact.itemTitle || item?.title || "未命名物品",
    },
  });
};

const handleContactBuyer = async (item) => {
  try {
    await openContactChat(item, fetchSoldOrderContact);
  } catch (error) {
    ElMessage.error(error.message || "打开会话失败");
  }
};

const handleContactSeller = async (item) => {
  try {
    await openContactChat(item, fetchBoughtOrderContact);
  } catch (error) {
    ElMessage.error(error.message || "打开会话失败");
  }
};

const handleRebuy = async (item) => {
  const orderId = getTradeOrderId(item);
  if (!orderId) {
    ElMessage.warning("订单信息无效");
    return;
  }

  try {
    const responseBody = await rebuyBoughtOrder(orderId);
    const itemId = Number(responseBody?.data?.itemId || item?.itemId || 0);
    if (!itemId) {
      throw new Error("商品信息无效，暂无法再次购买");
    }
    await router.push({
      name: "confirm-order",
      query: { itemId: String(itemId) },
    });
  } catch (error) {
    ElMessage.error(error.message || "再次购买失败");
  }
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
  const savedMenu = localStorage.getItem(PROFILE_SELECTED_MENU_KEY) || "idle";
  selectedMenu.value = sectionMap[savedMenu] ? savedMenu : "idle";
  if (!sectionMap[savedMenu]) {
    localStorage.setItem(PROFILE_SELECTED_MENU_KEY, "idle");
  }
  tradeOpen.value = selectedMenu.value.startsWith("trade-");
  activeReviewTab.value = "all";
  await initAuthSession();
  if (!authState.user) {
    router.push("/");
    return;
  }
  try {
    await loadOverview();
    await loadMenuData(selectedMenu.value, true);
  } catch (error) {
    ElMessage.error(error.message || "个人中心加载失败");
  }
});
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 24px;
  background: #f8f7fd;
}

.profile-layout {
  max-width: 1460px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.soft-card {
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 14px 36px rgba(140, 124, 240, 0.12);
}

.profile-sidebar {
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-self: start;
  position: static;
  margin-top: 0;
}

.sidebar-title {
  margin: 4px 8px 10px;
  font-size: 15px;
  font-weight: 700;
  color: #6f5ab8;
}

.menu-item,
.menu-title,
.sub-item {
  width: 100%;
  border: none;
  text-align: left;
  background: transparent;
  color: #2f2f3f;
  cursor: pointer;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 12px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
}

.menu-item--active {
  background: #f1ecff;
  color: #6f5ab8;
}

.menu-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 12px;
  border-radius: 12px;
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
  border-radius: 10px;
  margin: 2px 0;
  padding: 9px 35px;
  color: #6a6482;
  font-size: 14px;
}

.sub-item:hover,
.sub-item--active {
  color: #6f5ab8;
  background: #f4f0ff;
}

.profile-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 0;
}

.hero-card {
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  background: linear-gradient(135deg, #efe8ff 0%, #ffeefa 100%);
}

.hero-illustration,
.character,
.bubble {
  display: none !important;
}

.profile-user {
  display: flex;
  align-items: center;
  gap: 15px;
}

.profile-avatar {
  background: #c9bdfc;
  color: #4f3f93;
}

.user-name {
  margin: 0;
  font-size: 28px;
  color: #2f2f3f;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.user-credit-badges {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}

.credit-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid #d7cefa;
  background: #f6f2ff;
  color: #66539d;
  font-size: 12px;
  font-weight: 700;
}

.credit-help {
  position: relative;
}

.credit-help__btn {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid #d6cafb;
  background: #f6f2ff;
  color: #6754a6;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.credit-help__popover {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  width: 292px;
  padding: 11px;
  border-radius: 12px;
  border: 1px solid #e5dcff;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(111, 90, 184, 0.2);
  color: #3f3d50;
  z-index: 20;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-4px);
  transition: all 0.16s ease;
}

.credit-help:hover .credit-help__popover,
.credit-help:focus-within .credit-help__popover {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.credit-help__line {
  margin: 0 0 6px;
  font-size: 12px;
}

.credit-help__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.credit-help__table th,
.credit-help__table td {
  border: 1px solid #ece4ff;
  padding: 4px 6px;
  text-align: left;
}

.credit-help__table th {
  background: #f5f0ff;
}

.user-stats {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 15px;
  color: #6f6a83;
}

.dot {
  color: #b8b0d7;
}

.banner-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  border: none;
  border-radius: 999px;
  padding: 9px 16px;
  background: #8c7cf0;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.action-btn--danger {
  background: #f0cfd0;
  color: #8c3f46;
}

.action-btn--home {
  border: 1px solid #d9cef8;
  background: #f7f3ff;
  color: #6f5ab8;
}

.profile-content {
  padding: 20px 22px 22px;
  min-height: 380px;
}

.profile-content--review-fixed {
  height: calc(100vh - 210px);
  min-height: 420px;
  display: flex;
  flex-direction: column;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0ecff;
}

.section-head__left {
  display: inline-flex;
  align-items: baseline;
  gap: 10px;
}

.section-head__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.batch-btn {
  border: none;
  border-radius: 999px;
  padding: 7px 12px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.batch-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.batch-btn--danger {
  background: #f19aa3;
}

.batch-btn--shelf {
  background: #72c8a0;
}

.section-title {
  margin: 0;
  font-size: 23px;
  color: #2f2f3f;
}

.section-count {
  color: #938cb0;
  font-size: 14px;
}

.review-tabs {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.review-tab {
  border: 1px solid #ece6ff;
  border-radius: 10px;
  padding: 8px 14px;
  background: #faf8ff;
  color: #3b3652;
  font-size: 15px;
  cursor: pointer;
}

.review-tab--active {
  border-color: #d8cbff;
  background: #f1ebff;
  color: #6f5ab8;
  font-weight: 700;
}

.review-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
}

.profile-content--review-fixed .review-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.review-item {
  border-bottom: 1px solid #f1edff;
  padding: 12px 0;
}

.review-item:last-child {
  border-bottom: none;
}

.review-item__top {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.review-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.review-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-user-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-identity {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #f1ecff;
  color: #6d5bb1;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
}

.review-user {
  font-size: 14px;
  font-weight: 600;
  color: #2f2f3f;
}

.review-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #eee7ff;
  color: #6751a8;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
}

.review-content-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-time {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: #8f89ab;
}

.review-content {
  margin: 0;
  font-size: 14px;
  color: #4b4662;
  line-height: 1.55;
}

.pending-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pending-item {
  border-radius: 14px;
  padding: 14px;
  background: #f7f3ff;
}

.pending-item__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.pending-item__title {
  margin: 0;
  font-size: 16px;
  color: #2f2f3f;
}

.pending-item__title--link {
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.pending-item__title--link:hover {
  color: #6f5ab8;
}

.pending-item__status {
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  color: #6751a8;
  background: #ece5ff;
}

.pending-item__meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #6f6a83;
  font-size: 13px;
}

.pending-item__actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pending-btn {
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.pending-btn--confirm {
  color: #ffffff;
  background: #8c7cf0;
}

.pending-btn--cancel {
  color: #8b4d4f;
  background: #f6dcde;
}

.pending-empty {
  border-radius: 14px;
  padding: 20px;
  text-align: center;
  color: #8e88aa;
  background: #faf8ff;
  border: 1px dashed #e3dbff;
}

.section-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.published-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 12px;
}

.published-grid > .pending-empty {
  grid-column: 1 / -1;
  width: 100%;
  box-sizing: border-box;
}

.published-card {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  border: 1px solid #ece6ff;
  background: #ffffff;
  overflow: visible;
  cursor: pointer;
}

.published-card:hover {
  box-shadow: 0 10px 22px rgba(111, 90, 184, 0.14);
}

.published-card__thumb {
  width: 100%;
  aspect-ratio: 4 / 3;
  background: linear-gradient(135deg, #ebe8ff 0%, #f7ecff 100%);
  position: relative;
  overflow: hidden;
}

.published-card__check {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
}

.published-card__check input {
  width: 14px;
  height: 14px;
  cursor: pointer;
}

.published-card__thumb-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.published-card__thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8e88aa;
  font-size: 13px;
}

.published-card__status {
  position: absolute;
  top: 8px;
  left: 8px;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 12px;
  color: #6751a8;
  background: rgba(255, 255, 255, 0.9);
}

.published-card__body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.published-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.published-card__menu-wrap {
  position: relative;
}

.published-card__more-btn {
  border: none;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f5f1ff;
  color: #6351a9;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.published-card__menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  z-index: 5;
  min-width: 90px;
  border-radius: 10px;
  border: 1px solid #e7ddff;
  background: #ffffff;
  box-shadow: 0 12px 24px rgba(111, 90, 184, 0.18);
  overflow: hidden;
}

.published-card__menu-item {
  width: 100%;
  border: none;
  background: #ffffff;
  color: #3d3757;
  font-size: 13px;
  text-align: left;
  padding: 8px 10px;
  cursor: pointer;
}

.published-card__menu-item:hover {
  background: #f8f4ff;
}

.published-card__menu-item--danger {
  color: #cb4f6c;
}

.published-card__menu-item--shelf {
  color: #3f8c6d;
}

.published-card__title {
  margin: 0;
  font-size: 16px;
  color: #2f2f3f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.published-card__meta {
  margin: 0;
  color: #8e88aa;
  font-size: 12px;
}

.published-card__desc {
  margin: 0;
  color: #6f6a83;
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.section-item {
  border-radius: 14px;
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #ffffff;
  border: 1px solid #ece8fb;
}

.follow-user-item {
  border-radius: 14px;
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #ffffff;
  border: 1px solid #ece8fb;
}

.follow-user-item__main {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.follow-user-item__avatar {
  background: #c9bdfc;
  color: #4f3f93;
}

.follow-user-item__name {
  margin: 0;
  font-size: 15px;
  color: #2f2f3f;
}

.follow-user-item__meta {
  margin: 6px 0 0;
  color: #8e88aa;
  font-size: 12px;
}

.section-item__title {
  margin: 0;
  font-size: 15px;
  color: #2f2f3f;
}

.section-item__meta {
  margin: 6px 0 0;
  color: #8e88aa;
  font-size: 12px;
}

.section-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.section-item__btn {
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  color: #ffffff;
  background: #8c7cf0;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.section-item__btn--edit {
  background: #6bb9ff;
}

.section-item__btn--danger {
  background: #f19aa3;
}

.section-item__btn--shelf {
  background: #72c8a0;
}

.section-item__btn--contact {
  background: #7f72e8;
}

.section-item__btn--rebuy {
  background: #5ca7ee;
}

.edit-avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 16px;
  background: #f7f4ff;
}

.avatar-input {
  display: none;
}

.edit-error {
  margin: 0;
  color: #d14557;
  font-size: 13px;
}

.edit-form-grid {
  margin-top: 14px;
  padding: 8px 2px;
}

@media (max-width: 980px) {
  .profile-page {
    padding: 12px;
  }

  .profile-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .profile-sidebar {
    position: static;
  }

  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
