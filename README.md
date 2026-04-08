
# 医馆管理系统（前后端分离改造版）— README

> 更新时间：2026-04-08
> 当前状态：后端已接入 Spring Security + JWT，前端建议使用 Vue 3 独立工程

---

## 1. 项目简介

医馆管理系统后端，基于 **Spring Boot 4.0 + MyBatis + MySQL**，提供：

- 用户注册/登录
- 用户管理（增删改查、分页筛选）
- 客户管理（增删改查、分页筛选、批量导入）
- Swagger OpenAPI 文档

本仓库当前仍包含历史静态页面（`src/main/resources/static`），但推荐使用独立 Vue 前端工程对接。

---

## 2. 技术栈

- Java 21
- Spring Boot 4.0.0
- Spring MVC
- Spring Security（JWT 鉴权）
- MyBatis 3.0.3
- MySQL 8.x
- Jakarta Validation
- SpringDoc OpenAPI 2.3.0
- Maven Wrapper

---

## 3. 安全机制（已升级）

### 3.1 登录流程

- `POST /api/auth/login` 校验用户名/密码
- 登录成功后签发 JWT
- 返回：`token + user`

### 3.2 接口鉴权

- 放行：
  - `/api/auth/**`
  - `/swagger-ui/**`
  - `/swagger-ui.html`
  - `/v3/api-docs/**`
- 其余接口默认要求携带 `Authorization: Bearer <token>`

### 3.3 认证失败返回

统一返回 JSON：

```json
{
  "success": false,
  "message": "未登录或Token无效",
  "data": null
}
```

---

## 4. 后端目录（关键）

```text
src/main/java/com/example/demo/
├── config/
│   ├── AppConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   └── CustomerController.java
├── security/
│   ├── JwtProperties.java
│   ├── JwtService.java
│   ├── JwtAuthFilter.java
│   └── JsonAuthenticationEntryPoint.java
├── service/
│   ├── UserService.java
│   └── CustomerService.java
├── mapper/
├── dto/
├── domain/
└── DemoApplication.java
```

---

## 5. 配置说明（`application.properties`）

```properties
spring.application.name=demo

spring.datasource.url=jdbc:mysql://localhost:3306/medspa?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:123}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis.mapper-locations=classpath*:mapper/*.xml
mybatis.type-aliases-package=com.example.demo.domain
mybatis.configuration.map-underscore-to-camel-case=true

spring.jackson.time-zone=Asia/Shanghai
spring.jackson.date-format=yyyy-MM-dd'T'HH:mm:ss

# JWT（至少 32 字符）
security.jwt.secret=change-this-to-a-very-long-random-secret-key-123456
# 过期秒数（2小时）
security.jwt.expire-seconds=7200
```

---

## 6. 启动方式

### 6.1 前置

1. JDK 21+
2. MySQL 8.x
3. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS medspa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 6.2 启动命令

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

---

## 7. 联调验证（JWT 必测）

### 7.1 登录拿 token

`POST /api/auth/login`

### 7.2 不带 token 访问受保护接口

`GET /api/users` 应返回 `401`

### 7.3 带 token 访问

请求头加：
`Authorization: Bearer <token>`
应返回 `200`

---

## 8. Swagger

- `http://localhost:8080/swagger-ui/index.html`
- 或 `http://localhost:8080/swagger-ui.html`

---

## 9. 前后端分离建议

推荐前端独立仓库（或同级目录）：

- 后端：`.../demo`
- 前端：`.../medspa-admin`（Vue 3 + Vite）

开发代理（前端）转发 `/api` -> `http://localhost:8080`。

---

## 10. 已知注意事项

1. `security.jwt.secret` 请在生产环境使用高强度随机密钥（不要提交真实密钥）
2. 生产必须启用 HTTPS
3. 当前为最小 JWT 方案，后续建议增加：
   - refresh token
   - 更细粒度 RBAC
   - 审计日志
   - 单元/集成测试

---

## 11. API 响应约定

统一结构：

```json
{
  "success": true,
  "message": "可为空",
  "data": {}
}
```

分页结构（`data`）：

```json
{
  "total": 100,
  "items": []
}
```

---
