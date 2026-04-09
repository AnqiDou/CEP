<template>
  <div class="order-detail-page">
    <div class="order-detail-card">
      <header class="order-detail-header">
        <h2>订单详情</h2>
        <el-tag type="info">{{ detail.statusText }}</el-tag>
      </header>

      <section class="detail-section">
        <h3>订单信息</h3>
        <div class="detail-grid">
          <p><span>订单状态：</span>{{ detail.statusText }}</p>
          <p><span>订单编号：</span>{{ detail.orderNo || "-" }}</p>
          <p><span>创建时间：</span>{{ formatTime(detail.createdAt) }}</p>
          <p><span>支付时间：</span>{{ formatTime(detail.paidAt) }}</p>
          <p><span>完成时间：</span>{{ formatTime(detail.completedAt) }}</p>
        </div>
      </section>

      <section class="detail-section">
        <h3>商品信息</h3>
        <div class="item-row">
          <img
            class="item-cover"
            :src="detail.itemImage || fallbackImage"
            alt="商品图片"
          />
          <div class="item-info">
            <p class="item-title">{{ detail.itemTitle || "未命名物品" }}</p>
            <p class="item-desc">{{ detail.itemDescription || "暂无描述" }}</p>
            <p class="item-amount">金额：￥{{ formatAmount(detail.amount) }}</p>
          </div>
        </div>
      </section>

      <section class="detail-section">
        <h3>交易双方信息</h3>
        <div class="party-grid">
          <div class="party-card">
            <p class="party-title">买家</p>
            <div
              class="party-user"
              @click="goUser(detail.buyerUserId, detail.buyerName)"
            >
              <el-avatar :src="detail.buyerAvatar || ''" :size="42" />
              <span>{{ detail.buyerName || "校园用户" }}</span>
            </div>
            <p>联系方式：{{ detail.buyerPhone || "-" }}</p>
          </div>
          <div class="party-card">
            <p class="party-title">卖家</p>
            <div
              class="party-user"
              @click="goUser(detail.sellerUserId, detail.sellerName)"
            >
              <el-avatar :src="detail.sellerAvatar || ''" :size="42" />
              <span>{{ detail.sellerName || "校园用户" }}</span>
            </div>
            <p>联系方式：{{ detail.sellerPhone || "-" }}</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { fetchTradeOrderDetail } from "../service/payment/paymentApiService";

const route = useRoute();
const router = useRouter();
const fallbackImage = "https://via.placeholder.com/120x120?text=No+Image";

const detail = reactive({
  orderId: null,
  orderNo: "",
  status: "",
  statusText: "-",
  createdAt: "",
  paidAt: "",
  completedAt: "",
  itemId: null,
  itemTitle: "",
  itemDescription: "",
  itemImage: "",
  amount: 0,
  buyerUserId: null,
  buyerName: "",
  buyerAvatar: "",
  buyerPhone: "",
  sellerUserId: null,
  sellerName: "",
  sellerAvatar: "",
  sellerPhone: "",
});

const formatTime = (value) => {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "-";
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(
    2,
    "0"
  )}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(
    2,
    "0"
  )}:${String(d.getMinutes()).padStart(2, "0")}`;
};

const formatAmount = (v) => {
  const n = Number(v || 0);
  return Number.isFinite(n) ? n.toFixed(2) : "0.00";
};

const goUser = (userId, username) => {
  if (!userId) return;
  router.push({
    name: "other-profile",
    params: { name: String(username || "校园用户") },
    query: { userId: String(userId) },
  });
};

onMounted(async () => {
  const orderId = Number(route.query?.orderId || 0);
  if (!orderId) {
    ElMessage.error("订单参数无效");
    return;
  }
  try {
    const res = await fetchTradeOrderDetail(orderId);
    Object.assign(detail, res?.data || {});
  } catch (error) {
    ElMessage.error(error.message || "加载订单详情失败");
  }
});
</script>

<style scoped>
.order-detail-page {
  padding: 12px 16px 10px;
  background: #f7f8fc;
  min-height: auto;
}
.order-detail-card {
  max-width: 920px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  padding: 14px 16px;
}
.order-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.detail-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
  margin-top: 10px;
}
.detail-section h3 {
  margin: 0 0 8px;
  font-size: 16px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 12px;
  font-size: 14px;
}
.detail-grid span {
  color: #8a8fa3;
  margin-right: 6px;
}
.item-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.item-cover {
  width: 96px;
  height: 96px;
  border-radius: 10px;
  object-fit: cover;
  background: #f2f3f8;
}
.item-title {
  font-weight: 600;
  margin: 0 0 4px;
}
.item-desc {
  margin: 0 0 6px;
  color: #6a7287;
  font-size: 13px;
}
.item-amount {
  margin: 0;
  color: #5b6cff;
  font-weight: 600;
}
.party-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.party-card {
  background: #fafbff;
  border: 1px solid #eef0f7;
  border-radius: 12px;
  padding: 10px;
  font-size: 13px;
}
.party-title {
  margin: 0 0 6px;
  color: #7a8297;
}
.party-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-bottom: 4px;
}
</style>
