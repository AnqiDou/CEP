# 评价模块接口文档

## 响应格式

所有接口统一返回：

```json
{
  "success": true,
  "message": "获取成功",
  "data": {}
}
```

---

## 鉴权说明

- 全部接口需要请求头：`Authorization: Bearer {accessToken}`

---

## 1. 获取订单评价详情（评价页初始化）

- **URL**: `GET /api/reviews/orders/{orderId}`

### Path 参数

| 字段    | 类型 | 必填 | 说明    |
| ------- | ---- | ---- | ------- |
| orderId | long | 是   | 订单 ID |

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "orderId": 88,
    "itemId": 301,
    "itemTitle": "机械键盘",
    "itemCover": "https://xxx/item.jpg",
    "targetUserId": 5,
    "targetUserName": "王同学",
    "targetRole": "seller",
    "status": "PENDING",
    "canSubmit": true
  }
}
```

---

## 2. 提交评价

- **URL**: `POST /api/reviews/orders/{orderId}`

### Path 参数

| 字段    | 类型 | 必填 | 说明    |
| ------- | ---- | ---- | ------- |
| orderId | long | 是   | 订单 ID |

### 请求体

```json
{
  "rating": "good",
  "content": "沟通顺畅，交易准时，推荐！"
}
```

### 字段约束

| 字段    | 类型   | 必填 | 说明                      |
| ------- | ------ | ---- | ------------------------- |
| rating  | string | 是   | 评价结果：`good` 或 `bad` |
| content | string | 否   | 评价内容，最多 300 字     |

### 成功响应示例

```json
{
  "success": true,
  "message": "评价成功",
  "data": {
    "orderId": 88,
    "status": "SUBMITTED"
  }
}
```
