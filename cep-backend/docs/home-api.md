# 首页模块接口文档

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

## 1. 获取分类列表

- **URL**: `GET /api/home/categories`

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "code": "digital",
      "name": "数码产品",
      "description": "手机、电脑、平板、耳机、充电器等",
      "tags": ["二手手机", "笔记本", "平板", "耳机", "充电器"]
    }
  ]
}
```

---

## 2. 搜索/分类/排序获取物品列表

- **URL**: `GET /api/home/items`

### Query 参数

| 字段       | 类型   | 必填 | 默认值 | 说明                               |
| ---------- | ------ | ---- | ------ | ---------------------------------- |
| keyword    | string | 否   | 空     | 搜索关键字（匹配标题/描述/分类名） |
| categoryId | number | 否   | 空     | 分类 ID                            |
| sortBy     | string | 否   | `time` | 排序字段：`time` / `price`         |
| sortOrder  | string | 否   | `desc` | 排序方向：`asc` / `desc`           |
| page       | number | 否   | `1`    | 页码，从 1 开始                    |
| size       | number | 否   | `12`   | 每页数量，范围 `1-50`              |

### 请求示例

`GET /api/home/items?keyword=耳机&categoryId=1&sortBy=price&sortOrder=asc&page=1&size=12`

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "items": [
      {
        "id": 4,
        "categoryId": 1,
        "categoryCode": "digital",
        "categoryName": "数码产品",
        "title": "降噪无线蓝牙耳机",
        "description": "音质不错，适合通勤与自习使用。",
        "price": 80.0,
        "campus": "本部",
        "badge": "精选",
        "photoUrl": "https://example.com/item-4-1.jpg",
        "createdAt": "2026-03-25T16:15:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 12
  }
}
```

---

## 3. 获取热门推荐物品

- **URL**: `GET /api/home/hot-items`

### Query 参数

| 字段  | 类型   | 必填 | 默认值 | 说明                  |
| ----- | ------ | ---- | ------ | --------------------- |
| limit | number | 否   | `8`    | 返回条数，范围 `1-20` |

### 排序规则

- 按热度分值降序：`favorite_count * 6 + view_count`
- 热度相同按发布时间倒序

### 字段说明

- 返回的 `photoUrl` 为物品首图（按 `item_photos.sort_order` 升序取第一张）
- 若物品未上传图片，则 `photoUrl` 为空

### 请求示例

`GET /api/home/hot-items?limit=8`

---

## 4. 获取热门搜索词

- **URL**: `GET /api/home/hot-keywords`

### Query 参数

| 字段  | 类型   | 必填 | 默认值 | 说明                  |
| ----- | ------ | ---- | ------ | --------------------- |
| limit | number | 否   | `8`    | 返回条数，范围 `1-20` |

### 请求示例

`GET /api/home/hot-keywords?limit=8`

### 成功响应示例

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "keyword": "考研资料",
      "searchCount": 126
    },
    {
      "keyword": "平板",
      "searchCount": 95
    }
  ]
}
```
