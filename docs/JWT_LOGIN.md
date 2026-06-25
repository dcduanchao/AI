# JWT 登录设计文档

> 本文只做设计梳理，不含落地代码。目标：在现有项目上增加「账号密码登录 + JWT 鉴权」，**不做角色/权限**，只区分「已登录 / 未登录」。

## 1. 现状与约束

| 项 | 现状 | 对方案的影响 |
|---|---|---|
| Web 栈 | **Spring WebFlux（响应式）** | 拦截器必须用 `WebFilter`，**不能用** `OncePerRequestFilter`/`HandlerInterceptor` |
| ORM | MyBatis-Plus 3.5.7（`BaseMapper`） | 用户表沿用 `@TableName/@TableField` 实体 + Mapper |
| DB | MySQL（`spring-boot-starter-jdbc`，阻塞驱动） | 登录查库是阻塞操作，需用 `boundedElastic` 调度，别阻塞事件循环 |
| 安全框架 | **未引入 Spring Security**，无 JWT 依赖 | 需新增依赖；方案见第 2 节 |
| 统一异常 | `@RestControllerAdvice` 返回 `{"message": "..."}` | 鉴权失败返回沿用同一格式 |
| 注入风格 | 构造器注入 | 新增组件保持一致 |
| 端口 | 8888 | — |

## 2. 技术选型

WebFlux 下有两条路：

- **方案 A（推荐，本文采用）：自定义 `WebFilter`，不引入 Spring Security。**
  需求只是「登录 + 校验 token」，无角色。自己写一个全局 `WebFilter` 校验 JWT，最轻量、依赖最少、心智负担最低，和现有「无 Security」的现状一致。
- 方案 B：引入 `spring-boot-starter-security` 的响应式安全链（`SecurityWebFilterChain` + `ReactiveAuthenticationManager`）。
  更标准、扩展角色/方法级鉴权更顺，但为「无角色登录」引入一整套响应式安全栈，偏重。**建议等需要角色或第三方登录时再切换。**

> 下面所有结构都以**方案 A** 展开。

## 3. 新增依赖（pom.xml）

```xml
<!-- JWT：jjwt 0.12.x，分 api/impl/jackson 三件套 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- 密码哈希：BCrypt。不想引 Spring Security 就用 spring-security-crypto 单包 -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

> `spring-security-crypto` 只是 `BCryptPasswordEncoder` 这一个工具类的来源，不会启用安全过滤链。若想零额外依赖，也可用 JDK 自带 `MessageDigest` + 盐，但 BCrypt 更安全，推荐。

## 4. 数据库设计

### 4.1 建表语句（追加到 `src/main/resources/schema.sql`）

风格对齐现有 `ai_provider`：`BIGINT AUTO_INCREMENT` 主键、`TIMESTAMP DEFAULT CURRENT_TIMESTAMP`、`CREATE TABLE IF NOT EXISTS` 幂等。

```sql
CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(72)  NOT NULL,                 -- 存 BCrypt 哈希（固定 60 字符），不存明文
    nickname     VARCHAR(128),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);
```

字段说明：
- `password` 存 BCrypt 哈希（`$2a$...`，固定 60 字符），**永不返回给前端**；列长给 72 留余量。
- `username` 唯一约束 `uk_sys_user_username`，登录/注册都靠它。
- `updated_at` 加 `ON UPDATE CURRENT_TIMESTAMP`，改密时自动刷新。
- 无角色字段（符合需求）。后续要加角色再扩 `role` 列或关联表。

### 4.2 种子数据（追加到 `src/main/resources/data.sql`）

沿用现有 `data.sql` 的 `INSERT ... SELECT ... WHERE NOT EXISTS` 幂等写法，预置管理员账号。**已落库**的实际语句（哈希为真实生成并验证通过）：

```sql
-- 初始管理员；password 为 BCrypt 哈希（明文 dc673836112，strength 10）
INSERT INTO sys_user (username, password, nickname, enabled)
SELECT 'admin', '$2a$10$08JG2HoA8r7N6U.fgMwIROaC5kqVCtdunspiIUy4PiIexKiKc9qhm', '管理员', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');
```

- 登录账号：`admin` / `dc673836112`
- 该哈希为 `$2a$` 前缀、strength 10，与 Spring `BCryptPasswordEncoder` 默认完全兼容，已用 `checkpw` 验证返回 `true`。
- **生产环境务必改密**：登录后通过改密接口（或重新 `BCryptPasswordEncoder().encode(新密码)` 后 UPDATE）覆盖。

> 后续要改其它密码：跑一次 `new BCryptPasswordEncoder().encode("新明文")` 取输出，或 `htpasswd -nbBC 10 user 新明文` 取冒号后部分。

### 4.3 执行方式

`application.properties` 里 `spring.sql.init.mode` 当前是注释掉的（手动管理 schema）。两种落库方式：

- **手动执行**：把 4.1 / 4.2 的 SQL 直接在 MySQL 客户端对 `ai` 库跑一遍。
- **交给 Spring 启动初始化**：临时打开 `spring.sql.init.mode=always`（首次建表后建议再注释回去，避免每次启动重复跑种子）。

## 5. 配置项（application.properties）

```properties
# JWT 配置
jwt.secret=${JWT_SECRET:please-change-this-to-a-long-random-secret-at-least-32-bytes}
jwt.expire-minutes=${JWT_EXPIRE_MINUTES:120}
jwt.header=Authorization
jwt.prefix=Bearer 
```

- `jwt.secret` 必须 ≥ 32 字节（HS256 要求），生产环境用环境变量注入，**不要提交真值**（与 AGENTS.md 的密钥规范一致）。
- `jwt.expire-minutes` token 有效期，默认 2 小时。
- 用 `@ConfigurationProperties(prefix = "jwt")` 绑定成 `JwtProperties`（项目已开 `@ConfigurationPropertiesScan`，直接生效）。

## 6. 新增文件清单与分层

```
com.dc.ai
├── config
│   ├── JwtProperties.java          # @ConfigurationProperties("jwt")
│   └── JwtAuthWebFilter.java       # 全局 WebFilter，校验 token
├── controller
│   └── AuthController.java         # /api/auth/login 等
├── service
│   └── AuthService.java            # 登录校验、签发 token
├── domain
│   └── UserEntity.java             # @TableName("sys_user")
├── mapper
│   └── UserMapper.java             # extends BaseMapper<UserEntity>
├── dto
│   ├── LoginRequestDto.java        # username + password
│   ├── LoginResponseDto.java       # token + 过期时间 + 用户信息
│   └── UserInfoDto.java            # 脱敏用户信息（不含 password）
└── util
    └── JwtUtil.java                # 生成/解析/校验 JWT
```

## 7. 核心组件职责（签名级，不含实现）

### JwtProperties
绑定第 5 节配置，提供 `secret / expireMinutes / header / prefix`。

### JwtUtil
```text
String  generateToken(Long userId, String username)   // 签发，subject=userId，claim 放 username
Long    parseUserId(String token)                      // 解析 + 验签 + 验过期；失败抛异常
String  parseUsername(String token)
boolean validate(String token)                         // 静默校验，返回 true/false
```
- 算法 HS256，密钥取自 `JwtProperties.secret`。
- payload 只放 `userId`、`username`、`exp`，**不放敏感信息、不放角色**。

### UserEntity / UserMapper
- 实体字段对应 `sys_user`，沿用 `@TableField` 下划线映射（项目已开 `map-underscore-to-camel-case`）。
- Mapper 继承 `BaseMapper<UserEntity>`，登录查询用：
  ```text
  UserEntity selectByUsername(String username)   // WHERE username = ? AND enabled = 1 LIMIT 1
  ```

### AuthService
```text
Mono<LoginResponseDto> login(LoginRequestDto req)
```
流程：
1. `Mono.fromCallable(() -> userMapper.selectByUsername(req.username()))`
   外加 `.subscribeOn(Schedulers.boundedElastic())` —— **阻塞 JDBC 不能跑在事件循环线程上**。
2. 用户不存在 / 已禁用 → 抛 `IllegalArgumentException("用户名或密码错误")`（与 `ApiExceptionHandler` 对接，返回 400 + `{"message"}`）。
3. `bCrypt.matches(明文, 库里哈希)` 不通过 → 同样抛「用户名或密码错误」（**不区分用户不存在/密码错，防枚举**）。
4. 通过 → `JwtUtil.generateToken(...)`，组装 `LoginResponseDto`。

### AuthController
```text
@RestController @RequestMapping("/api/auth")
POST /login   -> Mono<LoginResponseDto>
GET  /me      -> Mono<UserInfoDto>     // 从当前请求上下文取 userId
```

### JwtAuthWebFilter（核心拦截）
- 实现 `org.springframework.web.server.WebFilter`，加 `@Component` 全局生效。
- 逻辑：
  1. 命中**白名单**（见第 9 节）→ 直接放行。
  2. 取 `Authorization` 头，缺失或不以 `Bearer ` 开头 → 返回 401 + `{"message":"未登录"}`。
  3. `JwtUtil.validate(token)` 失败 → 401 + `{"message":"登录已过期或无效"}`。
  4. 校验通过 → 解析出 `userId`，写入 `ServerWebExchange` 的 attributes 或 Reactor Context，供下游 `/me` 等读取，然后 `chain.filter(exchange)`。
- 401 响应直接写 `exchange.getResponse()`，`Content-Type: application/json`，body 用 fastjson2 序列化（项目已依赖 fastjson2）。

> 如何把当前用户传给 Controller：推荐写入 `exchange.getAttributes().put("userId", userId)`，Controller 用 `@RequestAttribute("userId")` 或从 `ServerWebExchange` 取。需要响应式链路透传时用 `Mono.deferContextual` + `contextWrite`。

## 8. 流程图

### 登录
```
前端 ──POST /api/auth/login {username,password}──▶ AuthController
                                                        │
                                                   AuthService.login
                                                        │ (boundedElastic)
                                            UserMapper.selectByUsername
                                                        │
                                            BCrypt.matches(明文, 哈希)
                                                        │ 通过
                                              JwtUtil.generateToken
                                                        │
前端 ◀── {token, expiresAt, user} ──────────────────────┘
```

### 鉴权（除白名单外的所有请求）
```
请求 ──▶ JwtAuthWebFilter
            │ 白名单? ──是──▶ 放行
            │ 否
            │ 取 Authorization: Bearer <token>
            │ validate(token)
            │   ├─ 失败 ──▶ 401 {"message":"..."}
            │   └─ 成功 ──▶ 写入 userId ──▶ chain.filter ──▶ Controller
```

## 9. 接口定义

### POST /api/auth/login
请求：
```json
{ "username": "admin", "password": "123456" }
```
成功（200）：
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-06-25T14:30:00Z",
  "user": { "id": 1, "username": "admin", "nickname": "管理员" }
}
```
失败（400）：
```json
{ "message": "用户名或密码错误" }
```

### GET /api/auth/me
请求头：`Authorization: Bearer <token>`
成功（200）：
```json
{ "id": 1, "username": "admin", "nickname": "管理员" }
```
未携带/无效 token（401）：
```json
{ "message": "未登录" }
```

> 是否提供 `/api/auth/register`、`/api/auth/logout` 视需求而定：
> - **register**：若用户由后台预置，可不做；要做则插入 `sys_user`，密码先 BCrypt 加密。
> - **logout**：无状态 JWT 服务端不存 token，登出本质是前端删 token。要做服务端强制失效需引入黑名单（Redis），当前无角色需求下可暂不做。

## 10. 放行白名单

`JwtAuthWebFilter` 中对以下路径**不校验 token**：

| 路径 | 说明 |
|---|---|
| `POST /api/auth/login` | 登录入口 |
| `POST /api/auth/register` | 如启用注册 |
| `GET /api/models` 等公开接口 | 按业务决定是否公开 |
| `OPTIONS *` | CORS 预检 |

建议把白名单做成可配置列表（`jwt.permit-paths`）或在 Filter 里用 `PathPattern` 匹配。**现有 `/api/chat`、`/api/images`、`/api/admin/**` 默认需要登录。**

## 11. 密码与安全要点

- 密码一律 **BCrypt** 存储，登录用 `matches` 比对；注册/改密时 `encode`。
- 登录失败信息统一为「用户名或密码错误」，不暴露「用户不存在」。
- `jwt.secret` 走环境变量，长度 ≥ 32 字节；泄露需立即轮换（旧 token 全部失效）。
- token 默认 2 小时，按需调整；要「记住我」再考虑 refresh token（第 13 节）。
- 响应中的用户信息走 `UserInfoDto`，**绝不带 password 字段**。

## 12. 测试要点

- `AuthServiceTest`：正确密码签发 token；错误密码/禁用用户抛异常。
- `JwtUtilTest`：签发的 token 能解析出正确 `userId`；过期 token 校验失败；篡改 token 验签失败。
- `JwtAuthWebFilterTest`（`WebTestClient`）：
  - 无 token 访问 `/api/chat` → 401；
  - 带合法 token → 放行；
  - 访问 `/api/auth/login` 无 token → 放行。
- 沿用 AGENTS.md：测试用默认 H2 配置，`./mvnw test`（Windows `.\mvnw.cmd test`）。

## 13. 后续可扩展（本期不做）

- **角色/权限**：`sys_user` 加 `role`，token 里加 `role` claim，Filter 里按路径校验——或此时切到方案 B（Spring Security 响应式 + 方法级注解）。
- **Refresh Token**：双 token，access 短、refresh 长，refresh 落库/Redis。
- **登出黑名单**：Redis 存已注销 token 的 jti，Filter 增加黑名单校验。
- **登录限流/锁定**：连续失败锁定账号，防爆破。

## 14. 落地步骤清单（按序）

1. `pom.xml` 加 jjwt 三件套 + `spring-security-crypto`。
2. `schema.sql` 加 `sys_user`，预置一条管理员（密码用 BCrypt 哈希）。
3. `application.properties` 加 `jwt.*` 配置。
4. 写 `JwtProperties`、`JwtUtil`。
5. 写 `UserEntity`、`UserMapper`。
6. 写 `LoginRequestDto / LoginResponseDto / UserInfoDto`、`AuthService`、`AuthController`。
7. 写 `JwtAuthWebFilter`（含白名单与 401 输出）。
8. 补 `ApiExceptionHandler`（可加 401 异常类型，或维持现状由 Filter 自行输出）。
9. 写测试，`./mvnw test` 通过。
10. 更新 `API.md`，补登录接口与「需要鉴权的接口要带 `Authorization` 头」说明。
```
