# 医馆管理系统（MedSpa）— 后端 API 文档

> 技术栈：Spring Boot 4.0.6 · Java 21 · MyBatis 3.0.3 · MySQL 8 · Spring Security + JWT
> 更新时间：2026-05-13

---

## 1. 项目简介

医馆管理系统后端，提供完整的 RESTful API，涵盖：

- 认证与授权（JWT + HttpOnly Cookie + 图形验证码 + 频率限制）
- 用户管理（CRUD、角色控制、密码修改、账号注销、Excel 导出）
- 客户管理（CRUD、批量导入、Excel 导出）
- 健康档案（嵌套在客户资源下的 CRUD，自动记录操作人）
- 服务项目（CRUD、启用/停用切换，GET 公开访问）
- 预约管理（CRUD、状态流转、多维度筛选）

---

## 2. 技术栈

| 类别 | 技术 |
|------|------|
| 运行时 | Java 21 |
| 框架 | Spring Boot 4.0.6 |
| 持久层 | MyBatis 3.0.3 + MySQL 8.x |
| 安全 | Spring Security + JJWT 0.12.5（JWT） |
| 验证 | Jakarta Validation + 分组校验 |
| 文档 | SpringDoc OpenAPI 2.8.6 |
| 导出 | EasyExcel 3.3.4 |
| 工具 | Hutool 5.8.28 |
| 密码 | BCryptPasswordEncoder |

---

## 3. 通用约定

### 3.1 统一响应结构

所有接口均返回以下 JSON 结构：

```json
{
  "success": true,
  "message": "操作描述（可为空）",
  "data": {}
}
```

失败时：

```json
{
  "success": false,
  "message": "错误描述",
  "data": null
}
```

### 3.2 分页响应结构

分页接口的 `data` 字段统一为：

```json
{
  "total": 100,
  "items": []
}
```

### 3.3 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | Integer | 1 | 当前页码 |
| `size` | Integer | 20 | 每页条数 |
| `sort` | String | 无 | 排序（如 `createdAt,desc`） |

### 3.4 日期时间格式

- 日期：`yyyy-MM-dd`（LocalDate）
- 日期时间：`yyyy-MM-dd'T'HH:mm:ss`（LocalDateTime，Jackson 全局配置 Asia/Shanghai 时区）

### 3.5 认证方式

- JWT 通过 **HttpOnly Cookie** 传递（`token` Cookie，SameSite=Lax）
- 同时支持 `Authorization: Bearer <token>` 请求头方式
- Cookie 过期时间与 JWT 一致（默认 2 小时）

### 3.6 错误码

| HTTP 状态码 | 含义 |
|-------------|------|
| 200 | 成功 |
| 400 | 请求参数不合法 / 数据约束冲突 |
| 401 | 未登录或 Token 无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 业务冲突（如用户名已存在） |
| 429 | 请求频率超限 |
| 500 | 服务器内部错误 |

### 3.7 全局异常处理

| 异常类型 | HTTP 状态码 | 说明 |
|----------|-------------|------|
| `ResponseStatusException` | 自定义 | Service 层主动抛出的业务异常 |
| `MethodArgumentNotValidException` | 400 | @RequestBody 校验失败 |
| `BindException` | 400 | 表单绑定校验失败 |
| `ConstraintViolationException` | 400 | @RequestParam / @PathVariable 校验失败 |
| `DataIntegrityViolationException` | 400 | 数据库约束冲突 |
| `AccessDeniedException` | 403 | Spring Security 权限不足 |
| 其他 `Exception` | 500 | 兜底处理 |

---

## 4. 安全机制

### 4.1 登录流程

1. `GET /api/auth/captcha` → 获取图形验证码（captchaId + Base64 图片）
2. `POST /api/auth/login` → 校验用户名/密码 → 签发 JWT → 写入 HttpOnly Cookie → 返回用户信息
3. 后续请求浏览器自动携带 Cookie；或前端手动加 `Authorization` 头

### 4.2 注册频率限制

- 路径：`POST /api/auth/register`
- 限制：同一 IP 每分钟最多 **5 次** 请求
- 超限返回 HTTP 429 + `{"success":false,"message":"注册请求过于频繁，请稍后再试"}`

### 4.3 接口权限规则

```
公开（permitAll）：
  /api/auth/**
  GET  /api/services/**        ← 官网/大屏展示
  /swagger-ui/**  /swagger-ui.html  /v3/api-docs/**
  /error
  OPTIONS /**

已登录（authenticated）：
  GET    /api/users  /api/users/{id}  /api/users/me/password
  DELETE /api/users/me
  GET    /api/customers  /api/customers/{id}
  POST   /api/customers  PUT /api/customers/{id}  DELETE /api/customers/{id}
  /api/customers/{customerId}/records/**
  GET    /api/appointments  /api/appointments/{id}
  POST   /api/appointments  PUT /api/appointments/{id}
  PATCH  /api/appointments/{id}/status

仅 ADMIN：
  POST   /api/users
  PUT    /api/users/{id}
  DELETE /api/users/{id}
  GET    /api/users/export
  POST   /api/customers/import
  GET    /api/customers/export
  POST   /api/services  PUT /api/services/{id}
  DELETE /api/services/{id}  PATCH /api/services/{id}/toggle
  DELETE /api/appointments/{id}
```

### 4.4 CORS 配置

当前允许的来源：`http://localhost:5173`（开发环境 Vue 前端），支持 Credentials。

---

## 5. 完整 API 接口

### 5.1 认证模块 — `/api/auth`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/auth/captcha` | 获取图形验证码 | 公开 |
| `POST` | `/api/auth/register` | 用户注册 | 公开 |
| `POST` | `/api/auth/login` | 用户登录 | 公开 |
| `POST` | `/api/auth/logout` | 退出登录 | 公开 |

#### `GET /api/auth/captcha`

获取图形验证码，用于注册。

**响应 data：**

| 字段 | 类型 | 说明 |
|------|------|------|
| captchaId | String | 验证码唯一标识 |
| imageBase64 | String | Base64 编码的验证码图片 |

#### `POST /api/auth/register`

**请求体 (RegisterRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| username | String | 是 | @NotBlank, max=50 | 用户名 |
| password | String | 是 | @NotBlank, 6~128 | 密码 |
| displayName | String | 是 | @NotBlank, max=128 | 显示名称 |
| phone | String | 否 | max=30 | 手机号 |
| occupation | String | 否 | max=255 | 职业 |
| captchaId | String | 是 | @NotBlank | 验证码标识 |
| captchaCode | String | 是 | @NotBlank | 验证码内容 |

**响应 data：** UserResponse

#### `POST /api/auth/login`

**请求体 (AuthRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| username | String | 是 | @NotBlank, max=50 | 用户名 |
| password | String | 是 | @NotBlank, 6~128 | 密码 |

**响应：**
- Set-Cookie: `token=<jwt>; Path=/; HttpOnly; SameSite=Lax`
- data: UserResponse

#### `POST /api/auth/logout`

清除 Cookie。

---

### 5.2 用户管理模块 — `/api/users`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/users` | 分页查询用户列表 | 已登录 |
| `GET` | `/api/users/{id}` | 查询单个用户 | 已登录 |
| `POST` | `/api/users` | 新增用户 | ADMIN |
| `PUT` | `/api/users/{id}` | 修改用户 | ADMIN |
| `DELETE` | `/api/users/{id}` | 删除用户 | ADMIN |
| `DELETE` | `/api/users/me` | 注销当前账号 | 已登录 |
| `PUT` | `/api/users/me/password` | 修改当前用户密码 | 已登录 |
| `GET` | `/api/users/export` | 导出用户 Excel | ADMIN |

#### `GET /api/users`

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 用户名/显示名称模糊搜索 |
| role | String | 否 | 角色筛选（ADMIN / STAFF） |
| active | Boolean | 否 | 是否启用 |
| page | Integer | 否 | 页码（默认 1） |
| size | Integer | 否 | 每页条数（默认 20） |
| sort | String | 否 | 排序（如 `createdAt,desc`） |

**响应 data：** PageResponse\<UserResponse\>

#### `POST /api/users` / `PUT /api/users/{id}`

**请求体 (UserUpsertRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| username | String | 是 | @NotBlank, max=50 | 用户名 |
| displayName | String | 是 | @NotBlank, max=128 | 显示名称 |
| password | String | 新增时必填 | 6~128 | 密码（修改时为空则不改） |
| role | String | 是 | @NotBlank, /^(ADMIN\|STAFF)$/ | 角色 |
| phone | String | 否 | max=30 | 手机号 |
| occupation | String | 否 | max=255 | 职业 |
| active | Boolean | 否 | - | 是否启用 |

**响应 data：** UserResponse

#### `DELETE /api/users/me`

**请求体 (DeleteAccountRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| currentPassword | String | 是 | @NotBlank | 当前密码（确认身份） |

#### `PUT /api/users/me/password`

**请求体 (ChangePasswordRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| currentPassword | String | 是 | @NotBlank | 当前密码 |
| newPassword | String | 是 | @NotBlank, min=6 | 新密码 |

#### `GET /api/users/export`

**查询参数：** keyword, role, active（同列表接口）

**响应：** 直接下载 `.xlsx` 文件（Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）

---

### 5.3 客户管理模块 — `/api/customers`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/customers` | 分页查询客户列表 | 已登录 |
| `GET` | `/api/customers/{id}` | 查询单个客户 | 已登录 |
| `POST` | `/api/customers` | 新增客户 | 已登录 |
| `PUT` | `/api/customers/{id}` | 修改客户 | 已登录 |
| `DELETE` | `/api/customers/{id}` | 删除客户 | 已登录 |
| `POST` | `/api/customers/import` | 批量导入客户 | ADMIN |
| `GET` | `/api/customers/export` | 导出客户 Excel | ADMIN |

#### `GET /api/customers`

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 姓名/手机号模糊搜索 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |
| sort | String | 否 | 排序 |

**响应 data：** PageResponse\<CustomerResponse\>

#### `POST /api/customers` / `PUT /api/customers/{id}`

**请求体 (CustomerRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| name | String | 是 | @NotBlank | 客户姓名 |
| phone | String | 否 | max=30 | 手机号 |
| email | String | 否 | @Email | 邮箱 |
| gender | String | 否 | - | 性别 |
| tags | String | 否 | - | 标签 |
| note | String | 否 | - | 备注 |
| birthday | LocalDate | 否 | - | 生日 |

**响应 data：** CustomerResponse

#### `POST /api/customers/import`

**请求体 (CustomerImportRequest)：**

```json
{
  "customers": [
    { "name": "张三", "phone": "13800000001", "email": "z@test.com", ... },
    { "name": "李四", ... }
  ]
}
```

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| customers | List\<CustomerRequest\> | 是 | @NotEmpty, @Valid | 客户列表 |

**响应 data：** Integer（成功导入条数）

#### `GET /api/customers/export`

**查询参数：** keyword（同列表接口）

**响应：** 直接下载 `.xlsx` 文件

---

### 5.4 健康档案模块 — `/api/customers/{customerId}/records`

> 嵌套在客户资源下，属于子资源。创建/修改/删除自动记录当前操作用户。

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/customers/{customerId}/records` | 查询客户健康档案 | 已登录 |
| `POST` | `/api/customers/{customerId}/records` | 新增健康档案 | 已登录 |
| `PUT` | `/api/customers/{customerId}/records/{recordId}` | 修改健康档案 | 已登录 |
| `DELETE` | `/api/customers/{customerId}/records/{recordId}` | 删除健康档案 | 已登录 |

#### `GET /api/customers/{customerId}/records`

**路径参数：** customerId

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |

**响应 data：** PageResponse\<RecordResponse\>

#### `POST` / `PUT` 请求体 (RecordRequest)

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| assessment | String | 否 | - | 健康评估 |
| recommendation | String | 否 | - | 建议 |
| recordDate | LocalDate | 否 | - | 记录日期 |

**响应 data：** RecordResponse（含 createdBy / createdByName）

---

### 5.5 服务项目模块 — `/api/services`

> GET 端点**公开访问**（适用于官网/大屏展示），写操作仅 ADMIN。

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/services` | 分页查询服务项目 | 公开 |
| `GET` | `/api/services/{id}` | 查询单个服务项目 | 公开 |
| `POST` | `/api/services` | 新增服务项目 | ADMIN |
| `PUT` | `/api/services/{id}` | 修改服务项目 | ADMIN |
| `DELETE` | `/api/services/{id}` | 删除服务项目 | ADMIN |
| `PATCH` | `/api/services/{id}/toggle` | 切换启用/停用状态 | ADMIN |

#### `GET /api/services`

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 名称模糊搜索 |
| active | Boolean | 否 | 是否启用筛选 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |
| sort | String | 否 | 排序 |

**响应 data：** PageResponse\<ServiceResponse\>

#### `POST /api/services` / `PUT /api/services/{id}`

**请求体 (ServiceRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| name | String | 新增时必填 | @NotBlank(OnCreate), max=128 | 服务名称 |
| description | String | 否 | - | 描述 |
| price | BigDecimal | 否 | @DecimalMin("0.00") | 价格 |
| durationMinutes | Integer | 否 | @PositiveOrZero | 时长（分钟） |
| active | Boolean | 否 | - | 是否启用 |

> 使用分组校验：`OnCreate`（新增）/ `OnUpdate`（修改）

**响应 data：** ServiceResponse

#### `PATCH /api/services/{id}/toggle`

切换服务的启用/停用状态（无需请求体）。

**响应 data：** ServiceResponse

---

### 5.6 预约管理模块 — `/api/appointments`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/appointments` | 分页查询预约列表 | 已登录 |
| `GET` | `/api/appointments/{id}` | 查询单个预约 | 已登录 |
| `POST` | `/api/appointments` | 新增预约 | 已登录 |
| `PUT` | `/api/appointments/{id}` | 修改预约 | 已登录 |
| `PATCH` | `/api/appointments/{id}/status` | 变更预约状态 | 已登录 |
| `DELETE` | `/api/appointments/{id}` | 删除预约 | ADMIN (@PreAuthorize) |

#### `GET /api/appointments`

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| therapistId | Long | 否 | 理疗师 ID |
| customerId | Long | 否 | 客户 ID |
| date | LocalDate | 否 | 精确日期筛选 |
| startDate | LocalDate | 否 | 日期范围起始 |
| endDate | LocalDate | 否 | 日期范围结束 |
| status | String | 否 | 预约状态 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |
| sort | String | 否 | 排序 |

**响应 data：** PageResponse\<AppointmentResponse\>

#### `POST /api/appointments` / `PUT /api/appointments/{id}`

**请求体 (AppointmentRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| customerId | Long | 是 | @NotNull | 客户 ID |
| serviceId | Long | 是 | @NotNull | 服务项目 ID |
| therapistId | Long | 是 | @NotNull | 理疗师 ID |
| appointmentTime | LocalDateTime | 是 | @NotNull | 预约时间 |
| note | String | 否 | - | 备注 |

**响应 data：** AppointmentResponse（含 customerName / serviceName / therapistName / endTime）

#### `PATCH /api/appointments/{id}/status`

**请求体 (AppointmentStatusRequest)：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| status | String | 是 | @NotNull, /^(COMPLETED\|CANCELLED)$/ | 目标状态 |

> 预约状态流转：`SCHEDULED` → `COMPLETED` / `CANCELLED`（仅允许变更为这两种终态）

---

### 5.7 其他公开端点

| 路径 | 说明 |
|------|------|
| `/swagger-ui/index.html` | Swagger UI |
| `/swagger-ui.html` | Swagger UI 入口 |
| `/v3/api-docs/**` | OpenAPI JSON/YAML |
| `/error` | Spring Boot 默认错误页 |

---

## 6. DTO 速查

### UserResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户 ID |
| username | String | 用户名 |
| role | String | 角色（ADMIN / STAFF） |
| displayName | String | 显示名称 |
| phone | String | 手机号 |
| occupation | String | 职业 |
| active | Boolean | 是否启用 |

### CustomerResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 客户 ID |
| name | String | 姓名 |
| phone | String | 手机号 |
| email | String | 邮箱 |
| gender | String | 性别 |
| tags | String | 标签 |
| note | String | 备注 |
| birthday | LocalDate | 生日 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 修改时间 |

### RecordResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 档案 ID |
| customerId | Long | 客户 ID |
| assessment | String | 健康评估 |
| recommendation | String | 建议 |
| recordDate | LocalDate | 记录日期 |
| createdBy | Long | 创建人 ID |
| createdByName | String | 创建人姓名 |
| createdAt | LocalDateTime | 创建时间 |

### ServiceResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 服务 ID |
| name | String | 服务名称 |
| description | String | 描述 |
| price | BigDecimal | 价格 |
| durationMinutes | Integer | 时长（分钟） |
| active | Boolean | 是否启用 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 修改时间 |

### AppointmentResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 预约 ID |
| customerId | Long | 客户 ID |
| serviceId | Long | 服务项目 ID |
| therapistId | Long | 理疗师 ID |
| appointmentTime | LocalDateTime | 预约时间 |
| endTime | LocalDateTime | 结束时间（自动计算） |
| status | String | 状态（SCHEDULED / COMPLETED / CANCELLED） |
| note | String | 备注 |
| customerName | String | 客户姓名（连表） |
| serviceName | String | 服务名称（连表） |
| therapistName | String | 理疗师姓名（连表） |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 修改时间 |

---

## 7. 后端目录结构

```text
src/main/java/com/example/demo/
├── config/
│   ├── AppConfig.java            ← BCrypt、SqlSessionFactory、MapperScan
│   ├── JacksonConfig.java        ← ObjectMapper 全局配置
│   ├── OpenApiConfig.java        ← Swagger 安全方案配置
│   ├── SecurityConfig.java       ← Spring Security + CORS + 路径权限
│   └── WebMvcConfig.java         ← 注册频率限制拦截器
├── controller/
│   ├── AuthController.java       ← /api/auth（验证码、注册、登录、登出）
│   ├── UserController.java       ← /api/users（CRUD + 密码 + 导出）
│   ├── CustomerController.java   ← /api/customers（CRUD + 导入导出）
│   ├── HealthRecordController.java ← /api/customers/{id}/records
│   ├── ServiceItemController.java  ← /api/services（CRUD + toggle）
│   └── AppointmentController.java  ← /api/appointments（CRUD + 状态）
├── security/
│   ├── JwtProperties.java        ← JWT 配置项（secret / expireSeconds / secure）
│   ├── JwtService.java           ← JWT 签发与解析
│   ├── JwtAuthFilter.java        ← JWT 认证过滤器
│   ├── JsonAuthenticationEntryPoint.java ← 401 JSON 响应
│   ├── JsonAccessDeniedHandler.java     ← 403 JSON 响应
│   └── RateLimitInterceptor.java        ← IP 频率限制（注册接口）
├── service/
│   ├── UserService.java
│   ├── CustomerService.java
│   ├── HealthRecordService.java
│   ├── ServiceItemService.java
│   ├── AppointmentService.java
│   └── CaptchaService.java
├── mapper/
├── domain/
│   ├── UserAccount.java
│   ├── Customer.java
│   ├── HealthRecord.java
│   ├── ServiceItem.java
│   └── Appointment.java
├── dto/                          ← 22 个 DTO 类
├── exception/
│   └── GlobalExceptionHandler.java ← 全局异常处理
└── DemoApplication.java
```

---

## 8. 配置说明

```properties
spring.application.name=demo

# 数据源
spring.datasource.url=jdbc:mysql://localhost:3306/medspa?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:123}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis
mybatis.mapper-locations=classpath*:mapper/*.xml
mybatis.type-aliases-package=com.example.demo.domain
mybatis.configuration.map-underscore-to-camel-case=true

# Jackson 时区与日期格式
spring.jackson.time-zone=Asia/Shanghai
spring.jackson.date-format=yyyy-MM-dd'T'HH:mm:ss

# JWT（至少 32 字符）
security.jwt.secret=change-this-to-a-very-long-random-secret-key-123456
security.jwt.expire-seconds=7200
# Cookie Secure 标志（生产环境应设为 true）
security.jwt.secure=false
```

---

## 9. 启动方式

### 9.1 前置

1. JDK 21+
2. MySQL 8.x
3. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS medspa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 9.2 启动命令

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

### 9.3 Swagger

- http://localhost:8080/swagger-ui/index.html
- http://localhost:8080/swagger-ui.html

---

## 10. 联调验证

```bash
# 1. 获取验证码
curl http://localhost:8080/api/auth/captcha

# 2. 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","displayName":"管理员","captchaId":"xxx","captchaCode":"xxxx"}'

# 3. 登录（Cookie 模式）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  -c cookie.txt

# 4. 查询用户列表（使用 Cookie）
curl http://localhost:8080/api/users -b cookie.txt

# 5. 查询用户列表（使用 Bearer Token）
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer <token>"

# 6. 不带认证访问受保护接口 → 401
curl http://localhost:8080/api/users
```

---

## 11. 前后端分离建议

推荐前端独立仓库（或同级目录）：

- 后端：`.../demo`
- 前端：`.../medspa-admin`（Vue 3 + Vite）

前端开发代理配置（`vite.config.ts`）：

```ts
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

---

## 12. 已知注意事项

1. `security.jwt.secret` 请在生产环境使用高强度随机密钥（不要提交真实密钥）
2. 生产环境必须启用 HTTPS，并将 `security.jwt.secure` 设为 `true`
3. CORS 当前仅允许 `http://localhost:5173`，部署时需修改
4. 频率限制基于内存，重启后重置；集群部署需替换为 Redis 方案
5. 当前为最小 JWT 方案，后续建议增加：
   - Refresh Token（无感刷新）
   - 更细粒度 RBAC（方法级 @PreAuthorize）
   - 审计日志
   - 单元/集成测试
