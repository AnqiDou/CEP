# 个人主页模块接口文档

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

- 除特别说明外，全部接口都需要请求头：`Authorization: Bearer {accessToken}`

---

## 1. 获取个人主页概览

- **URL**: `GET /api/profile/overview`

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "avatar": "https://xxx.cos.ap-beijing.myqcloud.com/profile-avatars/2026-03-28/a.jpg",
    "username": "安琪",
    "fans": 0,
    "following": 0,
    "sellerCredit": "极好",
    "buyerCredit": "优秀"
  }
}
```

---

## 2. 获取信用评价列表

- **URL**: `GET /api/profile/reviews`

### Query 参数

| 字段   | 类型   | 必填 | 默认值 | 说明                             |
| ------ | ------ | ---- | ------ | -------------------------------- |
| rating | string | 否   | `all`  | 评价筛选：`all` / `good` / `bad` |

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "total": 34,
    "goodCount": 32,
    "badCount": 2,
    "reviews": [
      {
        "id": 1,
        "user": "计算机学院-王同学",
        "avatar": "https://xxx/avatar.jpg",
        "rating": "good",
        "content": "交易顺利，描述一致。",
        "time": "2026-03-20"
      }
    ]
  }
}
```

---

## 3. 获取我发布的

- **URL**: `GET /api/profile/trades/published`

---

## 4. 获取我卖出的

- **URL**: `GET /api/profile/trades/sold`

---

## 5. 获取我买到的

- **URL**: `GET /api/profile/trades/bought`

---

## 6. 获取我的收藏

- **URL**: `GET /api/profile/favorites`

### 3~6 统一响应 `data` 结构

```json
[
  {
    "id": 1001,
    "itemId": 201,
    "title": "宿舍护眼台灯",
    "price": 28.0,
    "campus": "东校区",
    "time": "2026-03-28 16:20",
    "photoUrl": "https://xxx/item.jpg"
  }
]
```

---

## 7. 获取待处理交易（付款未完成）

- **URL**: `GET /api/profile/trades/pending-payment`

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 100,
      "orderId": 100,
      "itemId": 301,
      "title": "机械键盘",
      "partner": "信息学院-陈同学",
      "location": "南校区快递点",
      "time": "2026-03-28 18:30",
      "status": "PENDING_PAYMENT",
      "statusText": "待付款"
    }
  ]
}
```

---

## 8. 编辑资料

- **URL**: `PUT /api/profile/basic`
- **Content-Type**: `application/json`

### 请求参数

| 字段     | 类型   | 必填 | 说明                               |
| -------- | ------ | ---- | ---------------------------------- |
| username | string | 是   | 用户名，1~20 字符                  |
| password | string | 否   | 新密码，不传/空字符串表示不修改    |
| avatar   | string | 否   | 头像 URL（通常由头像上传接口返回） |

### 请求示例

```json
{
  "username": "安琪",
  "password": "abc12345",
  "avatar": "https://xxx.cos.ap-beijing.myqcloud.com/profile-avatars/2026-03-28/a.jpg"
}
```

---

## 9. 上传头像（腾讯云 COS）

- **URL**: `POST /api/profile/avatar`
- **Content-Type**: `multipart/form-data`

### FormData 参数

| 字段 | 类型 | 必填 | 说明                                               |
| ---- | ---- | ---- | -------------------------------------------------- |
| file | file | 是   | 头像文件，支持 `jpg/png/webp/gif`，单张最大 `10MB` |

### 成功响应示例

```json
{
  "success": true,
  "message": "上传成功",
  "data": {
    "url": "https://xxx.cos.ap-beijing.myqcloud.com/profile-avatars/2026-03-28/uuid.jpg"
  }
}
```

---

## 10. 已调整接口（支付下单）

- **URL**: `POST /api/payment/orders`
- **变更点**: 现在需要登录态（`Authorization: Bearer {accessToken}`），后端会自动记录买家/卖家用户 ID，用于“我买到的 / 我卖出的 / 待付款”列表。

---

## 信用规则说明（按页面问号规则）

- 初始分：`100`
- 每条好评：`+1`
- 每条差评：`-1`
- 分数 `< 90`：`较差`
- 分数 `90 ~ 109`：`良好`
- 分数 `110 ~ 139`：`优秀`
- 分数 `>= 140`：`极好`
- 买家/卖家信用独立计算（`buyerCredit`、`sellerCredit`）
