<template>
  <div class="profile-page">
    <header class="profile-header">
      <div class="profile-header__left">
        <el-avatar :size="72" class="profile-avatar">
          <el-icon :size="36"><UserFilled /></el-icon>
        </el-avatar>
        <div>
          <h2 class="profile-name">{{ userInfo.username }}</h2>
          <p class="profile-credit">
            信用等级：
            <el-tag type="success" effect="light">{{
              userInfo.creditLevel
            }}</el-tag>
          </p>
          <el-rate
            v-model="userInfo.creditScore"
            disabled
            show-score
            text-color="#f59e0b"
            score-template="{value} 分"
          />
        </div>
      </div>
      <el-button type="primary" plain @click="goHome">
        <el-icon><House /></el-icon>
        返回首页
      </el-button>
    </header>

    <section class="panel">
      <div class="panel__title">
        <el-icon><Goods /></el-icon>
        <span>发布的物品</span>
      </div>

      <el-table :data="publishedItems" stripe>
        <el-table-column prop="title" label="物品名称" min-width="180" />
        <el-table-column prop="price" label="价格" width="120">
          <template #default="scope">￥{{ scope.row.price }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="140" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              link
              @click="openEditDialog(scope.row)"
            >
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="panel">
      <div class="panel__title">
        <el-icon><Tickets /></el-icon>
        <span>个人信息与记录</span>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane name="pending" label="待确认交易">
          <el-table :data="pendingTrades" stripe>
            <el-table-column prop="item" label="物品" min-width="180" />
            <el-table-column prop="buyer" label="买家" width="120" />
            <el-table-column prop="price" label="金额" width="120">
              <template #default="scope">￥{{ scope.row.price }}</template>
            </el-table-column>
            <el-table-column prop="time" label="申请时间" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane name="bought" label="买过的物品（交易记录）">
          <el-table :data="boughtItems" stripe>
            <el-table-column prop="item" label="物品" min-width="200" />
            <el-table-column prop="seller" label="卖家" width="120" />
            <el-table-column prop="price" label="成交价" width="120">
              <template #default="scope">￥{{ scope.row.price }}</template>
            </el-table-column>
            <el-table-column prop="time" label="成交时间" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane name="favorites" label="收藏">
          <div class="card-list">
            <el-card
              v-for="item in favorites"
              :key="item.id"
              shadow="hover"
              class="record-card"
            >
              <h4>{{ item.title }}</h4>
              <p>价格：￥{{ item.price }}</p>
              <p>发布者：{{ item.owner }}</p>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane name="history" label="浏览">
          <el-timeline>
            <el-timeline-item
              v-for="entry in browsingHistory"
              :key="entry.id"
              :timestamp="entry.time"
              placement="top"
            >
              {{ entry.content }}
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>

        <el-tab-pane name="following" label="关注">
          <div class="card-list">
            <el-card
              v-for="follow in followingUsers"
              :key="follow.id"
              shadow="never"
              class="record-card"
            >
              <h4>{{ follow.name }}</h4>
              <p>近期发布：{{ follow.recentPost }}</p>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="isEditDialogVisible"
      title="编辑已发布物品"
      width="460px"
    >
      <el-form :model="editingItem" label-width="90px">
        <el-form-item label="物品名称">
          <el-input v-model="editingItem.title" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="editingItem.price" :min="1" :step="5" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editingItem.status" placeholder="请选择状态">
            <el-option label="在售" value="在售" />
            <el-option label="待确认" value="待确认" />
            <el-option label="已下架" value="已下架" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="editingItem.description"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="isEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItemEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Goods, House, Tickets, UserFilled } from "@element-plus/icons-vue";

const router = useRouter();

const userInfo = reactive({
  username: "校园用户_小易",
  creditLevel: "优秀",
  creditScore: 4.8,
});

const activeTab = ref("pending");
const isEditDialogVisible = ref(false);
const editingItem = reactive({
  id: null,
  title: "",
  price: 1,
  status: "在售",
  description: "",
});

const publishedItems = ref([
  {
    id: 1,
    title: "考研数学复习全书",
    price: 45,
    status: "在售",
    updatedAt: "2026-03-21 18:30",
    description: "九成新，附笔记。",
  },
  {
    id: 2,
    title: "机械键盘 87键",
    price: 120,
    status: "待确认",
    updatedAt: "2026-03-20 09:12",
    description: "轴体手感良好，附拔键器。",
  },
]);

const pendingTrades = ref([
  {
    id: 1,
    item: "机械键盘 87键",
    buyer: "张同学",
    price: 120,
    time: "2026-03-23 14:10",
  },
  {
    id: 2,
    item: "考研数学复习全书",
    buyer: "李同学",
    price: 45,
    time: "2026-03-23 11:05",
  },
]);

const boughtItems = ref([
  {
    id: 1,
    item: "宿舍护眼台灯",
    seller: "王同学",
    price: 35,
    time: "2026-03-10 16:40",
  },
  {
    id: 2,
    item: "耳机收纳盒",
    seller: "陈同学",
    price: 20,
    time: "2026-03-08 19:30",
  },
]);

const favorites = ref([
  { id: 1, title: "iPad 9 64G", price: 1250, owner: "数码小铺" },
  { id: 2, title: "羽毛球拍双拍套装", price: 80, owner: "运动专区" },
]);

const browsingHistory = ref([
  { id: 1, content: "浏览了：二手自行车（九成新）", time: "今天 13:22" },
  { id: 2, content: "浏览了：英语六级真题资料", time: "今天 09:16" },
  { id: 3, content: "浏览了：宿舍小风扇", time: "昨天 21:05" },
]);

const followingUsers = ref([
  { id: 1, name: "教材转让铺", recentPost: "高数教材（下册）" },
  { id: 2, name: "数码闲置站", recentPost: "蓝牙耳机（降噪）" },
]);

const goHome = () => {
  router.push("/");
};

const openEditDialog = (item) => {
  editingItem.id = item.id;
  editingItem.title = item.title;
  editingItem.price = item.price;
  editingItem.status = item.status;
  editingItem.description = item.description;
  isEditDialogVisible.value = true;
};

const saveItemEdit = () => {
  const target = publishedItems.value.find(
    (item) => item.id === editingItem.id
  );
  if (!target) return;
  target.title = editingItem.title;
  target.price = editingItem.price;
  target.status = editingItem.status;
  target.description = editingItem.description;
  target.updatedAt = new Date().toLocaleString("zh-CN", { hour12: false });
  isEditDialogVisible.value = false;
  ElMessage.success("物品信息已更新");
};
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 24px;
  background: #f5f7fb;
}

.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.profile-header__left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  background: #e0e7ff;
  color: #2563eb;
}

.profile-name {
  margin: 0;
  font-size: 24px;
  color: #1e293b;
}

.profile-credit {
  margin: 8px 0;
  color: #475569;
}

.panel {
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 16px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 18px;
  font-weight: 600;
  color: #1e3a8a;
}

.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.record-card h4 {
  margin: 0 0 8px;
}

.record-card p {
  margin: 0 0 6px;
  color: #475569;
  font-size: 14px;
}

@media (max-width: 768px) {
  .profile-page {
    padding: 12px;
  }

  .profile-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
