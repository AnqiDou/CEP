# 他人主页模块接口文档

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

## 目标用户定位参数

- 所有接口都支持两种定位方式（二选一）：
  - `userId`（推荐，数值 ID）
  - `username`（用户昵称）

---

## 1. 获取他人主页概览

- **URL**: `GET /api/profile/other/overview`

### Query 参数

| 字段     | 类型   | 必填 | 说明                            |
| -------- | ------ | ---- | ------------------------------- |
| userId   | long   | 否   | 目标用户 ID（推荐）             |
| username | string | 否   | 目标用户名，`userId` 缺省时使用 |

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "userId": 5,
    "avatar": "https://xxx/avatar.jpg",
    "username": "王同学",
    "city": "东校区",
    "fans": 64,
    "following": 9,
    "bio": "支持当面验货交易",
    "sellerCredit": "优秀",
    "buyerCredit": "良好",
    "followed": true
  }
}
```

---

## 2. 获取他人宝贝列表

- **URL**: `GET /api/profile/other/items`

### Query 参数

| 字段     | 类型   | 必填 | 默认值       | 说明                             |
| -------- | ------ | ---- | ------------ | -------------------------------- |
| userId   | long   | 否   | -            | 目标用户 ID                      |
| username | string | 否   | -            | 目标用户名                       |
| status   | string | 否   | `all`        | 宝贝筛选：`all/onsale/sold`      |
| sort     | string | 否   | `price-desc` | 价格排序：`price-desc/price-asc` |

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 101,
      "itemId": 101,
      "title": "二手羽毛球拍",
      "price": 68.0,
      "status": "onsale",
      "image": "https://xxx/item.jpg",
      "time": "2026-03-28 20:10"
    }
  ]
}
```

---

## 3. 获取他人评价列表

- **URL**: `GET /api/profile/other/reviews`

### Query 参数

| 字段     | 类型   | 必填 | 默认值 | 说明                     |
| -------- | ------ | ---- | ------ | ------------------------ |
| userId   | long   | 否   | -      | 目标用户 ID              |
| username | string | 否   | -      | 目标用户名               |
| rating   | string | 否   | `all`  | 评价筛选：`all/good/bad` |

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
        "id": 2001,
        "user": "李同学",
        "avatar": "https://xxx/avatar.jpg",
        "rating": "good",
        "content": "回复很快，交易顺利",
        "time": "2026-03-20",
        "tag": "好评"
      }
    ]
  }
}
```

---

## 4. 关注他人

- **URL**: `POST /api/profile/other/follow`

### Query 参数

| 字段     | 类型   | 必填 | 说明        |
| -------- | ------ | ---- | ----------- |
| userId   | long   | 否   | 目标用户 ID |
| username | string | 否   | 目标用户名  |

### 成功响应

```json
{
  "success": true,
  "message": "关注成功",
  "data": null
}
```

---

## 5. 取消关注他人

- **URL**: `DELETE /api/profile/other/follow`

### Query 参数

| 字段     | 类型   | 必填 | 说明        |
| -------- | ------ | ---- | ----------- |
| userId   | long   | 否   | 目标用户 ID |
| username | string | 否   | 目标用户名  |

### 成功响应

```json
{
  "success": true,
  "message": "取消关注成功",
  "data": null
}
```
