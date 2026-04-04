# 物品详情模块接口文档

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

## 1. 获取物品详情

- **URL**: `GET /api/items/{itemId}/detail`

### Path 参数

| 字段   | 类型   | 必填 | 说明         |
| ------ | ------ | ---- | ------------ |
| itemId | number | 是   | 物品 ID，> 0 |

### 说明

- 仅返回状态为 `PUBLISHED` 的物品。
- 接口调用成功后，物品浏览量 `view_count` 自动 +1。
- 若物品不存在或已下架，返回业务错误：`物品不存在或已下架`。

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "id": 12,
    "categoryId": 1,
    "categoryCode": "digital",
    "categoryName": "数码产品",
    "title": "iPad 9 64G + 原装笔",
    "price": 1200.0,
    "originalPrice": 1999.0,
    "purchaseDate": "2025-03-12",
    "usageDuration": "12个月",
    "location": "本部",
    "publishTime": "2026-03-28T12:23:10",
    "condition": "95新",
    "accessories": "原盒、充电器、手写笔",
    "description": "自用平板，学习记笔记为主，屏幕无划痕。",
    "detailNote": "可现场检查序列号与功能。",
    "photos": [
      "https://your-bucket.cos.ap-beijing.myqcloud.com/publish-images/2026-03-28/a.jpg",
      "https://your-bucket.cos.ap-beijing.myqcloud.com/publish-images/2026-03-28/b.jpg"
    ],
    "publisher": {
      "id": 3,
      "name": "李同学",
      "college": "信息工程学院",
      "campus": "本部",
      "credit": 4.9,
      "note": "回复及时，支持面交。"
    }
  }
}
```

---

## 2. 获取收藏状态

- **URL**: `GET /api/items/{itemId}/favorite`
- **鉴权**: 需要 `Authorization: Bearer {accessToken}`

### Path 参数

| 字段   | 类型   | 必填 | 说明         |
| ------ | ------ | ---- | ------------ |
| itemId | number | 是   | 物品 ID，> 0 |

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "favorite": true
  }
}
```

---

## 3. 收藏商品

- **URL**: `POST /api/items/{itemId}/favorite`
- **鉴权**: 需要 `Authorization: Bearer {accessToken}`

### Path 参数

| 字段   | 类型   | 必填 | 说明         |
| ------ | ------ | ---- | ------------ |
| itemId | number | 是   | 物品 ID，> 0 |

### 说明

- 不能收藏自己发布的商品。
- 收藏成功后会自动给商品发布者生成一条系统通知（`ITEM_FAVORITED`）。

### 成功响应示例

```json
{
  "success": true,
  "message": "收藏成功",
  "data": {
    "favorite": true
  }
}
```

---

## 4. 取消收藏

- **URL**: `DELETE /api/items/{itemId}/favorite`
- **鉴权**: 需要 `Authorization: Bearer {accessToken}`

### Path 参数

| 字段   | 类型   | 必填 | 说明         |
| ------ | ------ | ---- | ------------ |
| itemId | number | 是   | 物品 ID，> 0 |

### 成功响应示例

```json
{
  "success": true,
  "message": "取消收藏成功",
  "data": {
    "favorite": false
  }
}
```
