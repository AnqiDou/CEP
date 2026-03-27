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
