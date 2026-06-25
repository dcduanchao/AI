# 接口调用文档

## 基础信息

- Base URL: `http://localhost:8888`
- 请求体格式: `application/json`
- 统一错误返回:
  ```json
  { "message": "..." }
  ```

## 鉴权说明

- 采用 JWT。除下列**白名单**外，所有接口都必须在请求头携带 token：
  ```
  Authorization: Bearer <token>
  ```
- 白名单（无需 token）：
  - `POST /api/auth/login`
  - 所有 `OPTIONS`（CORS 预检）
- token 通过 `POST /api/auth/login` 获取，默认有效期 120 分钟（由 `jwt.expire-minutes` 配置）。
- 鉴权失败返回：
  - 未携带 / 格式不对：`401 { "message": "未登录" }`
  - token 过期或无效：`401 { "message": "登录已过期或无效" }`
- 以下业务接口**均需登录**（携带 `Authorization` 头）：
  `/api/models`、`/api/chat`、`/api/chat/stream`、`/api/images/generate`、`/api/admin/**`、`/api/auth/me`。

## 启动加载规则

- 启动时读取 `ai_provider` 中 `enabled = 1` 的记录
- 以 `ai_code` 作为内存 key
- 每个 provider 自动请求 `{base_url}/v1/models`
- 只保存返回的 `data[].id`

## `/api/chat` 请求参数

### ChatRequestDto

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `aiCode` | string | 是 | provider 的 `ai_code` |
| `model` | string | 是 | `/v1/models` 返回的模型 `id` |
| `conversationId` | number | 否 | 会话 ID，当前接口仅透传 |
| `messages` | array | 是 | 消息列表 |
| `temperature` | number | 否 | 采样温度 |

### messages 结构

`messages` 中每一项都是：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `role` | string | 是 | `system` / `user` / `assistant` |
| `content` | string | 是 | 消息内容 |

### 请求示例

```json
{
  "aiCode": "openai-compatible-demo",
  "model": "grok-4.20-0309-non-reasoning",
  "conversationId": 1,
  "messages": [
    { "role": "system", "content": "你是一个助手" },
    { "role": "user", "content": "你好" }
  ],
  "temperature": 0.7
}
```

## 接口列表

### 0. 登录鉴权

#### 0.1 登录

- `POST /api/auth/login`（白名单，无需 token）

请求示例：
```json
{ "username": "admin", "password": "dc673836112" }
```

返回示例：
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-06-25T14:30:00Z",
  "user": { "id": 1, "username": "admin", "nickname": "管理员" }
}
```

失败返回（400）：
```json
{ "message": "用户名或密码错误" }
```

> 拿到 `token` 后，调用其它接口时在请求头带 `Authorization: Bearer <token>`。

#### 0.2 当前登录用户

- `GET /api/auth/me`（需登录）

返回示例：
```json
{ "id": 1, "username": "admin", "nickname": "管理员" }
```

### 1. 获取模型列表

- `GET /api/models`（需登录）

返回示例：
```json
{
  "providers": {
    "openai-compatible-demo": [
      "grok-4.20-0309-non-reasoning",
      "grok-4.20-0309-reasoning"
    ]
  }
}
```

### 2. 聊天

- `POST /api/chat`（需登录）

返回示例：
```json
{
  "aiCode": "openai-compatible-demo",
  "model": "grok-4.20-0309-non-reasoning",
  "content": "你好，有什么可以帮你"
}
```

### 3. 流式聊天

- `POST /api/chat/stream`（需登录）
- 响应类型: `text/event-stream`
- 注意：浏览器原生 `EventSource` 无法携带自定义请求头，前端需用 `fetch` 读取流并手动解析 SSE（见 `FRONTEND.md`）。

事件示例：
```text
event: chunk
data: {"aiCode":"openai-compatible-demo","model":"grok-4.20-0309-non-reasoning","content":"你","done":false}

event: done
data: {"aiCode":"openai-compatible-demo","model":"grok-4.20-0309-non-reasoning","content":"","done":true}
```

### 4. 图片生成

- `POST /api/images/generate`（需登录）

请求示例：
```json
{
  "aiCode": "openai-compatible-demo",
  "model": "grok-4.20-0309-non-reasoning",
  "prompt": "一只蓝色猫咪",
  "size": "1024x1024",
  "count": 1
}
```

返回示例：
```json
{
  "aiCode": "openai-compatible-demo",
  "model": "grok-4.20-0309-non-reasoning",
  "status": "success",
  "urls": ["https://..."],
  "errorMessage": null
}
```

### 5. 重新同步单个 provider

- `POST /api/admin/providers/{aiCode}/sync-models`（需登录）

返回示例：
```json
{
  "providerId": 1,
  "aiCode": "openai-compatible-demo",
  "modelCount": 2,
  "status": "success",
  "message": null
}
```

### 6. 同步全部 provider

- `POST /api/admin/providers/sync-all-models`（需登录）

返回 `SyncResultDto` 流。

## 说明

- 调用 `/api/chat` 时必须传 `aiCode` 和 `model`
- `model` 只能使用 `/v1/models` 返回的 `id`
- `base_url` 不需要手动带 `/v1`，系统会自动补齐
- 除登录接口外，所有接口都需带 `Authorization: Bearer <token>`（详见「鉴权说明」）
