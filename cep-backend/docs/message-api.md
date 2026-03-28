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
