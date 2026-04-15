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

### 内容安全校验

- 系统使用数据库表 `review_sensitive_words` 维护评价屏蔽词库（每条记录是一个“词”，不是整句），并按以下类别管理：
  - 辱骂人身攻击类
  - 广告导流类（微信、QQ、电话、加好友等）
  - 违规承诺类（包过、代写、作弊等）
  - 涉政、涉黄、暴力类
  - 隐私信息类（手机号、地址、身份证号）
- 命中规则：**完全匹配触发**。即评价文本中出现完整屏蔽词（子串匹配）时，直接拦截提交。
- 拦截提示：`内容包含不当用语`

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

### 失败响应示例（命中屏蔽词）

```json
{
  "success": false,
  "message": "内容包含不当用语",
  "data": null
}
```

### 表结构生效说明

- 开发环境默认依赖后端启动时自动建表逻辑：[`ProfileSchemaInitializer.ensureProfileSchema()`](cep-backend/src/main/java/cep_backend/service/ProfileSchemaInitializer.java:32)。
- 如果数据库已在运行且未重启后端，可手动执行脚本中的敏感词表 DDL/DML：[`init-profile-tables.sql`](cep-backend/docs/init-profile-tables.sql)。
