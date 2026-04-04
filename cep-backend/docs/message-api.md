# 消息模块接口文档

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

## 1. 获取会话列表

- **URL**: `GET /api/messages/conversations`

### Query 参数

| 字段   | 类型   | 必填 | 默认值 | 说明                        |
| ------ | ------ | ---- | ------ | --------------------------- |
| filter | string | 否   | `all`  | 会话筛选：`all/read/unread` |

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "conversationId": 12,
      "peerUserId": 5,
      "peerName": "王同学",
      "peerAvatar": "https://xxx/avatar.jpg",
      "itemId": 301,
      "itemTitle": "机械键盘",
      "itemImage": "https://xxx/item.jpg",
      "unread": 2,
      "lastMessage": "今晚 8 点可以面交",
      "lastTime": "2026-03-28 20:10"
    }
  ]
}
```

---

## 2. 获取会话消息列表

- **URL**: `GET /api/messages/conversations/{conversationId}/messages`

### Path 参数

| 字段           | 类型 | 必填 | 说明    |
| -------------- | ---- | ---- | ------- |
| conversationId | long | 是   | 会话 ID |

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1001,
      "from": "other",
      "text": "你好，这个还在吗？",
      "imageUrl": "",
      "time": "2026-03-28 20:09",
      "messageType": "TEXT",
      "reviewOrderId": null,
      "reviewStatus": ""
    },
    {
      "id": 1002,
      "from": "self",
      "text": "在的，可以面交",
      "imageUrl": "",
      "time": "2026-03-28 20:10",
      "messageType": "TEXT",
      "reviewOrderId": null,
      "reviewStatus": ""
    },
    {
      "id": 1003,
      "from": "other",
      "text": "交易已完成，邀请你进行本次交易评价。",
      "imageUrl": "",
      "time": "2026-03-28 20:11",
      "messageType": "REVIEW_INVITE",
      "reviewOrderId": 88,
      "reviewStatus": "PENDING"
    }
  ]
}
```

### `data` 字段说明（消息项）

| 字段          | 类型   | 说明                                                          |
| ------------- | ------ | ------------------------------------------------------------- |
| id            | long   | 消息 ID                                                       |
| from          | string | `self/other`                                                  |
| text          | string | 文本内容                                                      |
| imageUrl      | string | 图片地址                                                      |
| time          | string | 时间（`yyyy-MM-dd HH:mm`）                                    |
| messageType   | string | 消息类型（`TEXT/IMAGE/REVIEW_INVITE`）                        |
| reviewOrderId | long   | 评价邀请关联订单 ID，非邀评消息为 `null`                      |
| reviewStatus  | string | 当前用户评价状态（`PENDING/SUBMITTED`），非邀评消息为空字符串 |

---

## 3. 获取通知消息列表（系统自动通知）

- **URL**: `GET /api/messages/notifications`

### Query 参数

| 字段  | 类型 | 必填 | 默认值 | 说明                  |
| ----- | ---- | ---- | ------ | --------------------- |
| limit | int  | 否   | `20`   | 返回条数，范围 `1~50` |

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 101,
      "type": "ITEM_FAVORITED",
      "title": "商品被收藏提醒",
      "content": "小王 收藏了你的商品《机械键盘》",
      "relatedItemId": 88,
      "relatedUserId": 12,
      "read": false,
      "createdAt": "2026-04-02 10:22"
    },
    {
      "id": 102,
      "type": "FAVORITE_PRICE_DROP",
      "title": "收藏商品降价提醒",
      "content": "你收藏的《二手耳机》已降价：¥120.00 → ¥99.00",
      "relatedItemId": 66,
      "relatedUserId": 9,
      "read": true,
      "createdAt": "2026-04-02 09:58"
    }
  ]
}
```

### `type` 枚举

- `ITEM_FAVORITED`：商品被收藏
- `FAVORITE_PRICE_DROP`：收藏商品降价
- `FOLLOWED`：被关注

---

## 4. 获取通知未读数

- **URL**: `GET /api/messages/notifications/unread-count`

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "unread": 3
  }
}
```

---

## 5. 标记单条通知已读

- **URL**: `POST /api/messages/notifications/{notificationId}/read`

### Path 参数

| 字段           | 类型 | 必填 | 说明    |
| -------------- | ---- | ---- | ------- |
| notificationId | long | 是   | 通知 ID |

---

## 6. 全部通知标记已读

- **URL**: `POST /api/messages/notifications/read-all`

---

## 7. 系统自动通知触发规则

以下通知均由系统自动检测并写入，不依赖管理员后台手动发送：

1. 商品被收藏：用户收藏他人商品后，向商品发布者发送 `ITEM_FAVORITED`
2. 收藏商品降价：卖家编辑商品价格且新价格低于原价格时，向收藏该商品的用户发送 `FAVORITE_PRICE_DROP`
3. 被关注：用户关注他人后，向被关注者发送 `FOLLOWED`
