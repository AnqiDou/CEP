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
  - `todayOrders`
  - `todaySales`
  - `pendingItemCount`
  - `abnormalOrderCount`
  - `pendingConversationCount`
  - `orderStateStats`: `[{ label, count, percent }]`

## 2. 用户管理

- `GET /api/admin/users?keyword=xxx`
  - `keyword` 可选，支持邮箱/用户名模糊搜索
- `PATCH /api/admin/users/{userId}/status`
  - 请求体：`{ "disabled": true }`
- `DELETE /api/admin/users/{userId}`

## 3. 商品管理

- `GET /api/admin/items?keyword=xxx&status=all|pending|online|offline`
- `POST /api/admin/items/{itemId}/approve`
- `POST /api/admin/items/{itemId}/offline`
- `DELETE /api/admin/items/{itemId}`

状态映射：

- `pending -> PENDING_REVIEW`
- `online -> PUBLISHED`
- `offline -> OFF_SHELF`

## 4. 订单管理

- `GET /api/admin/orders?keyword=xxx&status=all|pending-pay|completed|cancelled`
- `POST /api/admin/orders/{orderNo}/handle-abnormal`

状态映射：

- `pending-pay -> PENDING_PAYMENT`
- `completed -> PAID`
- `cancelled -> CANCELLED`

## 5. 客服会话

- `GET /api/admin/support/conversations`
- `POST /api/admin/support/conversations/{conversationId}/messages`
  - 请求体：`{ "content": "处理意见" }`

## 6. 公告管理

- `GET /api/admin/notices`
- `POST /api/admin/notices`
  - 请求体：`{ "content": "公告内容" }`
- `DELETE /api/admin/notices/{noticeId}`
