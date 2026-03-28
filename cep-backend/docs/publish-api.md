# 闲置发布模块接口文档

## 响应格式

所有接口统一返回：

```json
{
  "success": true,
  "message": "发布成功",
  "data": {}
}
```

---

## 1. 上传图片

- **URL**: `POST /api/publish/images`
- **Content-Type**: `multipart/form-data`
- **鉴权**: `Authorization: Bearer {accessToken}`

### FormData 参数

| 字段 | 类型 | 必填 | 说明                                               |
| ---- | ---- | ---- | -------------------------------------------------- |
| file | file | 是   | 图片文件，支持 `jpg/png/webp/gif`，单张最大 `10MB` |

### 成功响应示例

```json
{
  "success": true,
  "message": "上传成功",
  "data": {
    "url": "https://your-bucket.cos.ap-guangzhou.myqcloud.com/publish-images/2026-03-26/3f0f7df3-7a18-4ed6-93ba-33d7e2b25501.jpg"
  }
}
```

---

## 2. 提交闲置发布

- **URL**: `POST /api/publish/items`
- **Content-Type**: `application/json`
- **鉴权**: `Authorization: Bearer {accessToken}`

### 请求参数

| 字段          | 类型          | 必填 | 说明                                      |
| ------------- | ------------- | ---- | ----------------------------------------- |
| name          | string        | 是   | 物品名称，最长 120 字符                   |
| categoryCode  | string        | 否   | 分类编码，不传默认 `other`                |
| purchaseDate  | string (date) | 否   | 购买日期，例如 `2025-09-01`，不传默认当天 |
| usageDuration | string        | 否   | 使用时长，最长 50 字符                    |
| description   | string        | 否   | 描述，最长 500 字符                       |
| photoUrls     | string[]      | 否   | 图片 URL 列表，最多 6 张，不重复          |

### 请求示例

```json
{
  "name": "九成新机械键盘",
  "categoryCode": "digital",
  "purchaseDate": "2025-06-18",
  "usageDuration": "8个月",
  "description": "手感良好，灯效正常，原包装在。",
  "photoUrls": [
    "https://your-bucket.cos.ap-guangzhou.myqcloud.com/publish-images/2026-03-26/a.jpg",
    "https://your-bucket.cos.ap-guangzhou.myqcloud.com/publish-images/2026-03-26/b.jpg"
  ]
}
```

### 成功响应示例

```json
{
  "success": true,
  "message": "发布成功",
  "data": {
    "id": 1001,
    "name": "九成新机械键盘",
    "categoryCode": "digital",
    "purchaseDate": "2025-06-18",
    "usageDuration": "8个月",
    "description": "手感良好，灯效正常，原包装在。",
    "photoUrls": [
      "https://your-bucket.cos.ap-guangzhou.myqcloud.com/publish-images/2026-03-26/a.jpg"
    ],
    "createdAt": "2026-03-26T17:28:30"
  }
}
```

---

## 3. 获取我发布的物品列表

- **URL**: `GET /api/publish/items/mine`
- **鉴权**: `Authorization: Bearer {accessToken}`

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1001,
      "name": "九成新机械键盘",
      "categoryCode": "digital",
      "price": 99.0,
      "purchaseDate": "2025-06-18",
      "usageDuration": "8个月",
      "description": "手感良好，灯效正常，原包装在。",
      "photoUrls": ["https://xxx/a.jpg"],
      "status": "PUBLISHED",
      "createdAt": "2026-03-28T16:30:00"
    }
  ]
}
```

---

## 4. 编辑我发布的物品

- **URL**: `PUT /api/publish/items/{itemId}`
- **鉴权**: `Authorization: Bearer {accessToken}`
- **Content-Type**: `application/json`

### 请求参数

与发布接口参数一致：

| 字段          | 类型          | 必填 | 说明                             |
| ------------- | ------------- | ---- | -------------------------------- |
| name          | string        | 是   | 物品名称，最长 120 字符          |
| categoryCode  | string        | 否   | 分类编码，不传默认 `other`       |
| price         | number        | 是   | 价格，`>=0`                      |
| purchaseDate  | string (date) | 否   | 购买日期                         |
| usageDuration | string        | 否   | 使用时长，最长 50 字符           |
| description   | string        | 否   | 描述，最长 500 字符              |
| photoUrls     | string[]      | 否   | 图片 URL 列表，最多 6 张，不重复 |

---

## 5. 删除我发布的物品

- **URL**: `DELETE /api/publish/items/{itemId}`
- **鉴权**: `Authorization: Bearer {accessToken}`

### 说明

- 逻辑删除：状态改为 `DELETED`，不再在“我发布的”列表展示。

---

## 6. 上下架我发布的物品

- **URL**: `PATCH /api/publish/items/{itemId}/status`
- **鉴权**: `Authorization: Bearer {accessToken}`
- **Content-Type**: `application/json`

### 请求参数

| 字段   | 类型   | 必填 | 说明                                     |
| ------ | ------ | ---- | ---------------------------------------- |
| status | string | 是   | `PUBLISHED`（上架）/ `OFF_SHELF`（下架） |

### 请求示例

```json
{
  "status": "OFF_SHELF"
}
```
