# 管理后台接口文档

统一返回结构：

```json
{
  "success": true,
  "message": "获取成功",
  "data": {}
}
```

鉴权说明：所有接口都需要请求头 `Authorization: Bearer <accessToken>`，且登录账号邮箱必须为管理员邮箱（默认 `3299166215@qq.com`，可由 `APP_ADMIN_EMAIL` 覆盖）。

## 1. 仪表盘

- `GET /api/admin/dashboard`
- 响应 `data`：
  - `todayNewUsers`
  - `totalUsers`
  - `todayNewItems`
  - `totalItems`
  - `totalOrders`
  - `totalSales`
  - `pendingItemCount`
  - `abnormalOrderCount`
  - `pendingConversationCount`
  - `orderStateStats`: `[{ label, count, percent }]`

## 2. 用户管理

- `GET /api/admin/users`
  - 支持参数（均可选）：
    - `keyword`：全局关键字（用户名/邮箱/电话）
    - `username`：按用户名搜索
    - `phone`：按电话搜索
    - `email`：按邮箱搜索
- `PATCH /api/admin/users/{userId}/status`
  - 请求体：`{ "disabled": true }`
- `PATCH /api/admin/users/{userId}/credit-score`
  - 请求体：`{ "role": "seller", "creditScore": 105.5 }`
  - `role` 可选值：`seller|buyer`
- `DELETE /api/admin/users/{userId}`

## 3. 商品管理

- `GET /api/admin/items`
  - 支持参数（均可选）：
    - `keyword`：全局关键字
    - `title`：商品名称
    - `category`：分类
    - `minPrice`：最低价格
    - `maxPrice`：最高价格
    - `publisher`：发布者（用户名/邮箱）
    - `status`：`all|pending|online|offline`
- `POST /api/admin/items/{itemId}/approve`
- `POST /api/admin/items/{itemId}/offline`
- `DELETE /api/admin/items/{itemId}`

状态映射：

- `pending -> PENDING_REVIEW`
- `online -> PUBLISHED`
- `offline -> OFF_SHELF`

## 4. 订单管理

- `GET /api/admin/orders`
  - 支持参数（均可选）：
    - `keyword`：全局关键字
    - `orderNo`：订单号
    - `buyer`：买家
    - `seller`：卖家
    - `itemTitle`：商品
    - `status`：`all|pending-pay|completed|cancelled`
- `POST /api/admin/orders/{orderNo}/handle-abnormal`
- `PATCH /api/admin/orders/{orderNo}`
  - 请求体：`{ "status": "PAID", "refundStatus": "NONE" }`

状态映射：

- `pending-pay -> PENDING_PAYMENT`
- `completed -> PAID`
- `cancelled -> CANCELLED`

## 5. 客服会话

- `GET /api/admin/support/conversations`
  - 响应新增字段：
    - `reportType`：举报类型（`PROHIBITED_CONTACT/COUNTERFEIT/WRONG_CATEGORY/FRAUD_RISK/OTHER`）
    - `reporterName`：举报人展示名
    - `itemId`：关联商品 ID
    - `itemTitle`：关联商品标题
    - `reportContent`：举报内容
    - `status`：会话状态（`OPEN/PROCESSING/RESOLVED/CLOSED`）
    - `preview`：会话预览
    - `messages`：消息列表
- `POST /api/admin/support/conversations/{conversationId}/messages`
  - 请求体：`{ "content": "处理意见" }`
  - 行为说明：发送成功后，会将会话状态从 `OPEN` 自动推进为 `PROCESSING`（若当前仍为 `OPEN`）
- `PATCH /api/admin/support/conversations/{conversationId}/status`
  - 请求体：`{ "status": "RESOLVED" }`
  - 可选状态：`OPEN/PROCESSING/RESOLVED/CLOSED`

用户侧工单接口（同一控制器，前台“联系客服”页面使用）：

- `GET /api/admin/support/me/messages`
  - 返回当前登录用户与平台客服会话的消息列表
- `POST /api/admin/support/me/messages`
  - 请求体：`{ "content": "订单有问题", "orderId": 100 }`
  - `orderId` 可为空

## 6. 公告管理

- `GET /api/admin/notices`
- `POST /api/admin/notices`
  - 请求体：`{ "content": "公告内容" }`
- `DELETE /api/admin/notices/{noticeId}`
