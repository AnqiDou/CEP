# 认证模块接口文档

## 1. 发送注册验证码

- **URL**: `POST /api/auth/send-register-code`
- **Content-Type**: `application/json`

### 请求参数

| 字段  | 类型   | 必填 | 说明     |
| ----- | ------ | ---- | -------- |
| email | string | 是   | 注册邮箱 |

### 请求示例

```json
{
  "email": "student@example.com"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "验证码发送成功",
  "data": null
}
```

### 失败响应示例

```json
{
  "success": false,
  "message": "验证码发送过于频繁，请稍后再试",
  "data": null
}
```

---

## 2. 校验注册验证码

- **URL**: `POST /api/auth/verify-register-code`
- **Content-Type**: `application/json`

### 请求参数

| 字段  | 类型   | 必填 | 说明           |
| ----- | ------ | ---- | -------------- |
| email | string | 是   | 注册邮箱       |
| code  | string | 是   | 6 位数字验证码 |

### 请求示例

```json
{
  "email": "student@example.com",
  "code": "123456"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "验证码校验通过",
  "data": null
}
```

---

## 3. 注册

- **URL**: `POST /api/auth/register`
- **Content-Type**: `application/json`

### 请求参数

| 字段     | 类型   | 必填 | 说明                    |
| -------- | ------ | ---- | ----------------------- |
| email    | string | 是   | 注册邮箱                |
| code     | string | 是   | 6 位数字验证码          |
| username | string | 否   | 用户名，可为空字符串    |
| password | string | 是   | 8-20 位，包含字母和数字 |

### 请求示例

```json
{
  "email": "student@example.com",
  "code": "123456",
  "username": "小明",
  "password": "abc12345"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "email": "student@example.com",
    "username": "小明"
  }
}
```

---

## 4. 登录

- **URL**: `POST /api/auth/login`
- **Content-Type**: `application/json`

### 请求参数

| 字段     | 类型   | 必填 | 说明     |
| -------- | ------ | ---- | -------- |
| email    | string | 是   | 用户邮箱 |
| password | string | 是   | 登录密码 |

### 请求示例

```json
{
  "email": "student@example.com",
  "password": "abc12345"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "userId": 1,
    "email": "student@example.com",
    "username": "小明",
    "accessToken": "<access-token>",
    "refreshToken": "<refresh-token>",
    "accessTokenExpiresInSeconds": 900,
    "tokenType": "Bearer"
  }
}
```

---

## 5. 发送重置密码验证码

- **URL**: `POST /api/auth/send-reset-password-code`
- **Content-Type**: `application/json`

### 请求参数

| 字段  | 类型   | 必填 | 说明       |
| ----- | ------ | ---- | ---------- |
| email | string | 是   | 已注册邮箱 |

### 请求示例

```json
{
  "email": "student@example.com"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "验证码发送成功",
  "data": null
}
```

---

## 6. 校验重置密码验证码

- **URL**: `POST /api/auth/verify-reset-password-code`
- **Content-Type**: `application/json`

### 请求参数

| 字段  | 类型   | 必填 | 说明           |
| ----- | ------ | ---- | -------------- |
| email | string | 是   | 已注册邮箱     |
| code  | string | 是   | 6 位数字验证码 |

### 请求示例

```json
{
  "email": "student@example.com",
  "code": "123456"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "验证码校验通过",
  "data": null
}
```

---

## 7. 重置密码

- **URL**: `POST /api/auth/reset-password`
- **Content-Type**: `application/json`

### 请求参数

| 字段     | 类型   | 必填 | 说明                    |
| -------- | ------ | ---- | ----------------------- |
| email    | string | 是   | 已注册邮箱              |
| code     | string | 是   | 6 位数字验证码          |
| password | string | 是   | 8-20 位，包含字母和数字 |

### 请求示例

```json
{
  "email": "student@example.com",
  "code": "123456",
  "password": "abc12345"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "密码重置成功",
  "data": null
}
```

---

## 8. 刷新令牌

- **URL**: `POST /api/auth/refresh`
- **Content-Type**: `application/json`

### 请求参数

| 字段         | 类型   | 必填 | 说明               |
| ------------ | ------ | ---- | ------------------ |
| refreshToken | string | 是   | 登录返回的刷新令牌 |

### 请求示例

```json
{
  "refreshToken": "<refresh-token>"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "刷新成功",
  "data": {
    "userId": 1,
    "email": "student@example.com",
    "username": "小明",
    "accessToken": "<new-access-token>",
    "refreshToken": "<new-refresh-token>",
    "accessTokenExpiresInSeconds": 900,
    "tokenType": "Bearer"
  }
}
```

---

## 9. 获取当前登录用户

- **URL**: `GET /api/auth/me`
- **Header**: `Authorization: Bearer <access-token>`

### 成功响应

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "userId": 1,
    "email": "student@example.com",
    "username": "小明"
  }
}
```

---

## 10. 退出登录

- **URL**: `POST /api/auth/logout`
- **Content-Type**: `application/json`

### 请求参数

| 字段         | 类型   | 必填 | 说明               |
| ------------ | ------ | ---- | ------------------ |
| refreshToken | string | 是   | 当前会话的刷新令牌 |

### 请求示例

```json
{
  "refreshToken": "<refresh-token>"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "退出成功",
  "data": null
}
```

## 通用错误码说明

- 当前接口统一返回 HTTP `400`（业务错误）和 HTTP `500`（系统错误）。
- 业务错误通过 `message` 字段提示：如邮箱格式错误、验证码过期、密码错误、登录态失效等。
