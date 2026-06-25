# 前端设计实现文档（Vue3 + Element Plus）

> 面向前端开发。基于现有后端接口（见 `API.md`）与 JWT 鉴权方案（见 `JWT_LOGIN.md`），实现：**登录 / 提供商管理 / 对话 / 生图** 四个模块。本文是设计与实现指引，含关键代码片段。

## 0. 与后端的接口对照（先看这张表）

| 模块 | 方法 & 路径 | 是否需要 JWT | 现状 |
|---|---|---|---|
| 登录 | `POST /api/auth/login` | 否（白名单） | 待落地（见 JWT_LOGIN.md） |
| 当前用户 | `GET /api/auth/me` | 是 | 待落地 |
| 模型列表 | `GET /api/models` | 是 | ✅ 已有 |
| 对话 | `POST /api/chat` | 是 | ✅ 已有 |
| 流式对话 | `POST /api/chat/stream`（SSE） | 是 | ✅ 已有 |
| 生图 | `POST /api/images/generate` | 是 | ✅ 已有 |
| 同步单个 provider | `POST /api/admin/providers/{aiCode}/sync-models` | 是 | ✅ 已有 |
| 同步全部 provider | `POST /api/admin/providers/sync-all-models`（SSE/流） | 是 | ✅ 已有 |

> ⚠️ **提供商管理的后端缺口**：当前后端**只有「同步模型」接口，没有 provider 的列表 / 新增 / 编辑 / 启停 / 删除**。
> 完整的「提供商管理页」需要后端补以下接口（建议）：
> - `GET /api/admin/providers` 列表
> - `POST /api/admin/providers` 新增
> - `PUT /api/admin/providers/{id}` 编辑
> - `DELETE /api/admin/providers/{id}` 删除
> - `PATCH /api/admin/providers/{id}/enabled` 启停
>
> 在后端补齐前，前端「提供商管理页」只能做到：**用 `GET /api/models` 展示已启用 provider 及其模型 + 提供「同步」按钮**。本文第 6 节按「完整版接口已就绪」设计，并标注降级方案。

## 1. 技术选型

| 关注点 | 选型 |
|---|---|
| 框架 | Vue 3（`<script setup>` + Composition API） |
| 构建 | Vite |
| UI | Element Plus |
| 路由 | Vue Router 4 |
| 状态 | Pinia |
| HTTP | Axios（封装统一实例 + 拦截器） |
| 流式 | 原生 `fetch` + `ReadableStream`（**不用 EventSource**，原因见第 7 节） |
| 语言 | TypeScript（推荐）或 JS |

## 2. 目录结构

```
src/
├── api/
│   ├── http.ts            # axios 实例 + 拦截器
│   ├── auth.ts            # 登录、me
│   ├── models.ts          # 模型列表
│   ├── chat.ts            # 对话（含流式）
│   ├── image.ts           # 生图
│   └── provider.ts        # 提供商管理（含同步）
├── stores/
│   ├── auth.ts            # token、当前用户
│   └── models.ts          # provider->models 快照缓存
├── router/
│   └── index.ts           # 路由 + 守卫
├── layouts/
│   └── MainLayout.vue     # 登录后主框架（侧边栏 + 顶栏）
├── views/
│   ├── Login.vue
│   ├── Providers.vue      # 提供商管理
│   ├── Chat.vue           # 对话
│   └── Image.vue          # 生图
├── utils/
│   └── sse.ts             # SSE 解析工具（fetch 流式）
├── App.vue
└── main.ts
```

## 3. 环境与跨域

后端在 `8888`，且是 WebFlux。开发期两种方式二选一：

**方式 A：Vite 代理（推荐，免 CORS）**
```ts
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8888', changeOrigin: true }
    }
  }
})
```
前端所有请求走相对路径 `/api/...`，由 Vite 转发。

**方式 B：后端开 CORS**
需后端加 `CorsWebFilter`（WebFlux 的 CORS），放行前端源、`Authorization` 头、`OPTIONS` 预检。**记得把 `OPTIONS` 也加进 JWT 白名单**（JWT_LOGIN.md 第 10 节已列）。

`.env.development`：
```
VITE_API_BASE=/api          # 走代理
```
`.env.production`：
```
VITE_API_BASE=https://你的域名/api
```

## 4. 鉴权层（核心）

### 4.1 Pinia auth store
```ts
// stores/auth.ts
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null as null | { id: number; username: string; nickname?: string },
  }),
  getters: { isLogin: (s) => !!s.token },
  actions: {
    setToken(t: string) { this.token = t; localStorage.setItem('token', t) },
    setUser(u: any) { this.user = u },
    logout() { this.token = ''; this.user = null; localStorage.removeItem('token') },
  },
})
```

### 4.2 Axios 实例 + 拦截器
统一注入 `Authorization`，统一处理后端 `{ "message": "..." }` 错误格式，401 自动登出跳登录。
```ts
// api/http.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 30000,
})

http.interceptors.request.use((cfg) => {
  const auth = useAuthStore()
  if (auth.token) cfg.headers.Authorization = `Bearer ${auth.token}`
  return cfg
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || '请求失败'
    if (status === 401) {
      useAuthStore().logout()
      router.replace('/login')
    }
    ElMessage.error(msg)
    return Promise.reject(err)
  },
)
```

### 4.3 auth api
```ts
// api/auth.ts
import { http } from './http'
export const login = (data: { username: string; password: string }) =>
  http.post('/auth/login', data)        // -> { token, tokenType, expiresAt, user }
export const fetchMe = () => http.get('/auth/me')
```

### 4.4 路由守卫
```ts
// router/index.ts （节选）
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!auth.isLogin) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})
```
路由表：`/login` 标 `meta.public = true`，其余挂在 `MainLayout` 下需登录。

## 5. 登录模块（Login.vue）

UI：Element Plus `el-form`（username、password）+ `el-button`。流程：
1. 调 `login()` → 拿 `token` 存入 store，再调 `fetchMe()` 存用户信息。
2. 成功后跳 `redirect` 或首页。

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, fetchMe } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const router = useRouter(); const route = useRoute()
const auth = useAuthStore()

async function onSubmit() {
  loading.value = true
  try {
    const res: any = await login(form)
    auth.setToken(res.token)
    auth.setUser(res.user ?? (await fetchMe()))
    router.replace((route.query.redirect as string) || '/chat')
  } finally { loading.value = false }
}
</script>
```
> 初始账号（来自 data.sql 种子）：`admin` / `dc673836112`。

## 6. 提供商管理（Providers.vue）

> 见第 0 节缺口说明。以下分「完整版」与「降级版」。

### 6.1 完整版（需后端补 CRUD 接口）
- `el-table` 列：`aiName`、`aiCode`、`adapterType`、`baseUrl`、`enabled`(开关)、模型数、操作。
- 操作列按钮：编辑、删除、**同步模型**、启停。
- 顶部：新增 provider（`el-dialog` 表单：`aiCode / aiName / adapterType / baseUrl / apiKey / enabled`）。
- `apiKey` 输入用 `show-password`，列表里**脱敏展示**（`****`）。
- 「同步全部」按钮调 `sync-all-models`。

```ts
// api/provider.ts
import { http } from './http'
export const listProviders  = () => http.get('/admin/providers')                       // 待后端
export const createProvider = (d: any) => http.post('/admin/providers', d)              // 待后端
export const updateProvider = (id: number, d: any) => http.put(`/admin/providers/${id}`, d) // 待后端
export const deleteProvider = (id: number) => http.delete(`/admin/providers/${id}`)     // 待后端
export const syncProvider   = (aiCode: string) => http.post(`/admin/providers/${aiCode}/sync-models`) // ✅ 已有
export const syncAll        = () => http.post('/admin/providers/sync-all-models')       // ✅ 已有(流)
```

同步返回 `SyncResultDto`：`{ providerId, aiCode, modelCount, status, message }`，同步后给 `ElMessage` 反馈并刷新模型数。

### 6.2 降级版（后端 CRUD 未就绪时）
- 用 `GET /api/models` 拿 `{ providers: { aiCode: [modelId...] } }`，渲染只读列表（aiCode + 模型数 + 模型清单）。
- 每行提供「同步模型」按钮（`syncProvider`），顶部「同步全部」。
- 新增/编辑/删除按钮置灰并提示「需后端支持」。

## 7. 对话模块（Chat.vue）

### 7.1 模型选择
进入页面先拉 `GET /api/models`（缓存进 `models` store）。两级选择：
- 选 `aiCode`（provider）→ 联动出该 provider 的 `model` 下拉。

```ts
// stores/models.ts 关键：providers = { [aiCode]: string[] }
```

### 7.2 非流式对话
`POST /api/chat`，body 见 API.md 的 `ChatRequestDto`：
```ts
// api/chat.ts
import { http } from './http'
export const chat = (data: {
  aiCode: string; model: string; conversationId?: number;
  messages: { role: 'system'|'user'|'assistant'; content: string }[];
  temperature?: number;
}) => http.post('/chat', data)   // -> { aiCode, model, content }
```

### 7.3 流式对话（重点）
后端 `POST /api/chat/stream` 返回 `text/event-stream`，事件 `chunk` / `done`，每条 `data` 是 `ChatChunkDto`：`{ aiCode, model, content, done }`。

**为什么不用 `EventSource`**：原生 `EventSource` 只支持 GET、无法设置请求体和 `Authorization` 头。本接口是 POST + 需要 JWT，**必须用 `fetch` 读取流并手动解析 SSE**。

```ts
// utils/sse.ts —— 通用 SSE（fetch 流式）解析
import { useAuthStore } from '@/stores/auth'

export async function postSse(
  url: string,
  body: any,
  onEvent: (event: string, data: any) => void,
  signal?: AbortSignal,
) {
  const auth = useAuthStore()
  const resp = await fetch(import.meta.env.VITE_API_BASE + url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: auth.token ? `Bearer ${auth.token}` : '',
    },
    body: JSON.stringify(body),
    signal,
  })
  if (resp.status === 401) { auth.logout(); location.href = '/login'; return }
  if (!resp.body) return

  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buf = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    // SSE 以空行分隔事件块
    const blocks = buf.split('\n\n')
    buf = blocks.pop() || ''
    for (const block of blocks) {
      let event = 'message', data = ''
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) data += line.slice(5).trim()
      }
      if (data) onEvent(event, JSON.parse(data))
    }
  }
}
```

Chat.vue 中使用：逐 `chunk` 追加到当前 assistant 气泡，`done` 事件结束。
```ts
const controller = new AbortController()
let answer = ''
await postSse('/chat/stream', payload, (event, data) => {
  if (event === 'chunk' && !data.done) { answer += data.content; /* 更新视图 */ }
  if (event === 'done') { /* 收尾、落入消息列表 */ }
}, controller.signal)
// 「停止生成」按钮调用 controller.abort()
```

### 7.4 会话 UI 建议
- 左侧会话列表（本地维护，`conversationId` 目前后端仅透传，可前端自增/本地存储）。
- 右侧消息流：`user` / `assistant` 气泡；支持 Markdown 渲染（可选 `markdown-it`）。
- 输入区：文本框 + 发送 + 停止 + 流式/非流式开关 + temperature 滑杆。

## 8. 生图模块（Image.vue）

`POST /api/images/generate`，请求 `ImageRequestDto`，返回 `ImageResponseDto`：
```ts
// api/image.ts
import { http } from './http'
export const generateImage = (data: {
  aiCode: string; model: string; prompt: string; size?: string; count?: number;
}) => http.post('/images/generate', data)
// 返回: { aiCode, model, status, urls: string[], errorMessage }
```

UI：
- 复用第 7.1 的 provider/model 选择。
- 表单：`prompt`(textarea)、`size`(下拉 `1024x1024` 等)、`count`(数量 1~n)。
- 提交后 loading；`status === 'success'` → `el-image` 宫格展示 `urls`，支持点击大图/下载；
  否则 `ElMessage.error(errorMessage)`。

## 9. 主框架与导航（MainLayout.vue）

- 顶栏：左 logo / 应用名，右当前用户 `nickname` + 下拉「退出登录」（调 `auth.logout()` → 跳 `/login`）。
- 侧边栏菜单：对话 `/chat`、生图 `/image`、提供商管理 `/providers`。
- `<router-view />` 承载内容。

## 10. 统一约定

- **错误处理**：全部走 axios 响应拦截器，后端错误体 `{ message }` 统一 `ElMessage.error` 弹出；流式接口在 `postSse` 里单独处理 401。
- **token 失效**：任何 401 → 清 token + 跳登录（拦截器 / postSse 双保险）。
- **Loading**：按钮级 `:loading`，列表级骨架/`v-loading`。
- **敏感信息**：`apiKey`、`password` 不回显明文。
- **响应字段命名**：后端是 record/驼峰 JSON，前端直接按驼峰取。

## 11. 开发顺序建议

1. 脚手架：`npm create vite@latest`（Vue+TS）→ 装 `element-plus pinia vue-router axios`。
2. 配 Vite 代理、`main.ts` 注册 Element Plus / Pinia / Router。
3. 鉴权层：`http.ts` + `auth store` + 路由守卫 + `Login.vue`（**先打通登录拿 token**）。
4. `MainLayout` + 路由骨架。
5. 模型 store + 对话页（先非流式，再接流式 `postSse`）。
6. 生图页。
7. 提供商管理页（降级版先上，待后端补 CRUD 再做完整版）。

## 12. 待后端配合的事项（汇总）

1. **JWT 登录接口落地**（`/api/auth/login`、`/api/auth/me`）—— 见 JWT_LOGIN.md。
2. **提供商 CRUD 接口**（list/create/update/delete/启停）—— 当前缺失，决定提供商管理页能否做完整版。
3. **CORS**（若不用 Vite 代理）—— 放行前端源与 `Authorization` 头、`OPTIONS` 预检。
4. （可选）**会话持久化** —— 若需要历史会话，`conversationId` 需后端真正落库与查询接口。
