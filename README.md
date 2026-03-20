# 医馆管理系统 — 代码全面解读

## 一、项目概述

本项目是一个基于 **Spring Boot 4.0** 的轻量级医馆（医疗美容/养生馆）管理后台，提供：

- **用户管理**：员工账号的注册、登录、增删改查
- **客户管理**：客户信息录入、搜索、批量导入
- **内嵌前端**：Bootstrap 5 简易管理页面，无需额外前端工程
- **Swagger 接口文档**：自动生成，方便调试

---

## 二、技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 4.0.0 |
| Web层 | Spring MVC | 随 Boot 版本 |
| ORM | MyBatis | 3.0.3 |
| 数据库 | MySQL | 8.x 推荐 |
| 密码加密 | BCrypt (Spring Security Crypto) | 随 Boot 版本 |
| 参数校验 | Jakarta Bean Validation | 随 Boot 版本 |
| 接口文档 | SpringDoc OpenAPI (Swagger UI) | 2.3.0 |
| 构建工具 | Maven | 包含 Wrapper |
| 前端 | Bootstrap 5 + 原生 JS（内嵌 HTML） | 5.3.3 |

---

## 三、目录结构

```
Manage-System/
├── pom.xml                              # Maven 构建配置，声明所有依赖
├── mvnw / mvnw.cmd                      # Maven Wrapper 脚本，无需本地安装 Maven
├── .mvn/                                # Maven Wrapper 配置目录
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java     # Spring Boot 启动入口
    │   │   ├── config/
    │   │   │   └── AppConfig.java       # 应用配置：MyBatis、BCrypt
    │   │   ├── controller/              # REST 控制器层（接收 HTTP 请求）
    │   │   │   ├── AuthController.java  # 认证接口：注册 / 登录
    │   │   │   ├── UserController.java  # 用户管理接口
    │   │   │   └── CustomerController.java  # 客户管理接口
    │   │   ├── service/                 # 业务逻辑层
    │   │   │   ├── UserService.java     # 用户业务逻辑
    │   │   │   └── CustomerService.java # 客户业务逻辑
    │   │   ├── domain/                  # 数据库实体类（与表结构对应）
    │   │   │   ├── UserAccount.java     # users 表实体
    │   │   │   └── Customer.java        # customers 表实体
    │   │   ├── dto/                     # 数据传输对象（请求/响应 DTO）
    │   │   │   ├── ApiResponse.java     # 统一响应包装器
    │   │   │   ├── PageResponse.java    # 分页响应包装器
    │   │   │   ├── AuthRequest.java     # 登录请求体
    │   │   │   ├── RegisterRequest.java # 注册请求体
    │   │   │   ├── LoginResponse.java   # 登录响应（token + 用户信息）
    │   │   │   ├── UserResponse.java    # 用户信息响应（不含密码）
    │   │   │   ├── UserUpsertRequest.java   # 创建/更新用户请求体
    │   │   │   ├── CustomerRequest.java     # 创建/更新客户请求体
    │   │   │   ├── CustomerResponse.java    # 客户信息响应
    │   │   │   └── CustomerImportRequest.java  # 批量导入客户请求体
    │   │   └── mapper/                  # MyBatis Mapper 接口（数据访问层）
    │   │       ├── UserMapper.java      # 用户数据库操作接口
    │   │       └── CustomerMapper.java  # 客户数据库操作接口
    │   └── resources/
    │       ├── application.properties   # 数据库连接、MyBatis、Jackson 配置
    │       ├── schema.sql               # 自动建表 SQL（应用启动时执行）
    │       ├── mapper/
    │       │   ├── UserMapper.xml       # 用户 MyBatis SQL 映射文件
    │       │   └── CustomerMapper.xml   # 客户 MyBatis SQL 映射文件
    │       └── static/
    │           ├── index.html           # 登录页面（管理员入口，Bootstrap 5）
    │           └── dashboard.html       # 管理主界面（用户管理 + 客户管理，需登录后访问）
    └── test/
        └── java/com/example/demo/
            └── DemoApplicationTests.java  # Spring 上下文加载冒烟测试
```

---

## 四、数据库设计

数据库名：`medspa`（在 `application.properties` 中配置）

### 4.1 用户表（`users`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK AUTO_INCREMENT | 主键 |
| `username` | VARCHAR(64) UNIQUE NOT NULL | 登录用户名，唯一 |
| `password_hash` | VARCHAR(255) NOT NULL | BCrypt 加密后的密码 |
| `role` | VARCHAR(32) NOT NULL | 角色，如 `STAFF`、`ADMIN` |
| `display_name` | VARCHAR(128) NOT NULL | 显示名称 |
| `phone` | VARCHAR(30) | 手机号 |
| `active` | TINYINT(1) DEFAULT 1 | 账号是否启用（1=启用，0=停用） |
| `created_at` | DATETIME | 创建时间（自动填充） |
| `updated_at` | DATETIME | 更新时间（自动更新） |

### 4.2 客户表（`customers`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK AUTO_INCREMENT | 主键 |
| `name` | VARCHAR(128) NOT NULL | 客户姓名 |
| `phone` | VARCHAR(30) | 手机号 |
| `email` | VARCHAR(128) | 邮箱 |
| `gender` | VARCHAR(16) | 性别 |
| `tags` | VARCHAR(255) | 标签（逗号分隔等） |
| `note` | TEXT | 备注信息 |
| `birthday` | DATE | 生日 |
| `created_at` | DATETIME | 创建时间（自动填充） |
| `updated_at` | DATETIME | 更新时间（自动更新） |

> **说明**：`schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，应用每次启动时如果表不存在则自动创建，已存在则跳过。

---

## 五、代码各层详解

### 5.1 启动入口：`DemoApplication.java`

```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

`@SpringBootApplication` 是组合注解，等价于：
- `@Configuration`：声明配置类
- `@EnableAutoConfiguration`：开启自动配置
- `@ComponentScan`：扫描当前包及子包的 Bean

---

### 5.2 配置层：`AppConfig.java`

```java
@Configuration
@MapperScan("com.example.demo.mapper")  // 扫描 mapper 接口，自动注册为 Bean
public class AppConfig {

    // 注册 BCrypt 密码编码器（用于注册时加密、登录时校验）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 手动配置 MyBatis SqlSessionFactory（指定数据源和 XML 映射文件路径）
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception { ... }

    // 注册 SqlSessionTemplate（线程安全的 MyBatis 会话模板）
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) { ... }
}
```

**关键点**：
- `@MapperScan` 让 MyBatis 自动扫描 `mapper` 包下所有接口，生成代理实现类注入容器，无需手动 `@Bean`。
- `BCryptPasswordEncoder` 使用自适应哈希算法，是存储密码的最佳实践。

---

### 5.3 控制器层（Controller）

所有控制器均返回 `ApiResponse<T>` 统一包装格式：

```json
{
  "success": true,
  "message": "操作描述",
  "data": { ... }
}
```

#### `AuthController`（`/api/auth`）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/auth/register` | 注册新用户，角色默认为 `STAFF` |
| POST | `/api/auth/login` | 登录，成功返回 token 和用户信息 |

#### `UserController`（`/api/users`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/users` | 分页列表，支持 `keyword`/`role`/`active` 过滤 |
| GET | `/api/users/{id}` | 根据 ID 获取单个用户 |
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{id}` | 更新用户信息 |
| DELETE | `/api/users/{id}` | 删除用户 |

#### `CustomerController`（`/api/customers`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/customers` | 分页列表，支持 `keyword` 过滤（姓名/电话/标签） |
| GET | `/api/customers/{id}` | 根据 ID 获取单个客户 |
| POST | `/api/customers` | 创建客户 |
| PUT | `/api/customers/{id}` | 更新客户信息 |
| DELETE | `/api/customers/{id}` | 删除客户 |
| POST | `/api/customers/import` | 批量导入客户（传入客户数组） |

---

### 5.4 业务逻辑层（Service）

#### `UserService` 核心逻辑

| 方法 | 说明 |
|------|------|
| `register()` | 检查用户名唯一性 → BCrypt 加密密码 → 插入数据库，角色固定为 `STAFF` |
| `login()` | 查找用户 → 检查 active 状态 → BCrypt 验证密码 → 生成 UUID 作为 token |
| `list()` | 计算分页偏移量 → 调用 Mapper 查询 → 封装 `PageResponse` |
| `create()` | 检查用户名唯一性 → 加密密码 → 插入 |
| `update()` | 构造更新实体（有密码则加密）→ 调用 Mapper 更新 → 重新查询返回最新数据 |
| `findById()` | 查询，不存在则抛 404 异常 |
| `delete()` | 直接删除，无软删除 |

> ⚠️ **安全警告（仅供开发调试）**：`login()` 生成的 token 是随机 UUID，**未持久化**，服务重启后失效，且 API 请求目前**不校验 token**（无拦截器/过滤器），任何人都可以直接访问所有接口。**此配置绝对不能用于生产环境**，生产环境必须引入 Spring Security + JWT 或 Session 机制进行认证鉴权。

#### `CustomerService` 核心逻辑

| 方法 | 说明 |
|------|------|
| `list()` | 分页查询，支持按姓名/电话/标签关键词搜索 |
| `create()` | 将 DTO 转为实体 → 插入数据库 |
| `update()` | 构造更新实体 → 更新 → 重新查询返回 |
| `findById()` | 查询，不存在则抛 404 异常 |
| `delete()` | 直接删除 |
| `importBatch()` | 空列表直接返回 0 → 批量转换 DTO 为实体 → 调用 `batchInsert` |

---

### 5.5 数据访问层（Mapper）

Mapper 接口 + XML 文件配合使用，MyBatis 在运行时自动生成代理实现。

#### `UserMapper.xml` 关键 SQL

- **动态查询**（`findAll` / `countAll`）：使用 `<where>` + `<if>` 标签，根据参数动态拼接 SQL，避免写多个重载方法：

```xml
<where>
    <if test="keyword != null and keyword != ''">
        AND (username LIKE CONCAT('%', #{keyword}, '%')
             OR display_name LIKE CONCAT('%', #{keyword}, '%')
             OR phone LIKE CONCAT('%', #{keyword}, '%'))
    </if>
    <if test="role != null and role != ''">
        AND role = #{role}
    </if>
    <if test="active != null">
        AND active = #{active}
    </if>
</where>
```

- **更新时保留原密码**（`update`）：使用 `COALESCE`，若传入密码为 `null` 则保留原密码哈希：

```sql
password_hash = COALESCE(#{passwordHash}, password_hash)
```

- **自增主键回填**（`insert`）：`useGeneratedKeys="true" keyProperty="id"` 让 MyBatis 将数据库生成的 ID 自动回填到实体对象的 `id` 字段。

#### `CustomerMapper.xml` 关键 SQL

- **批量插入**（`batchInsert`）：使用 `<foreach>` 标签拼接多行 VALUES：

```xml
<foreach collection="customers" item="item" separator=",">
    (#{item.name}, #{item.phone}, ...)
</foreach>
```

---

### 5.6 实体层（Domain）

| 类 | 对应表 | 说明 |
|----|--------|------|
| `UserAccount` | `users` | 包含 `passwordHash` 字段，内部使用，不对外暴露 |
| `Customer` | `customers` | 所有客户字段均可对外返回 |

MyBatis 通过 `<resultMap>` 将数据库下划线命名（`created_at`）映射到 Java 驼峰命名（`createdAt`），配置中也启用了 `map-underscore-to-camel-case=true`。

---

### 5.7 DTO 层

DTO（Data Transfer Object）与 Domain 实体分离，好处是：

1. **安全**：`UserResponse` 不含 `passwordHash`，密码不会泄露给前端
2. **解耦**：数据库字段变更不直接影响 API 格式
3. **校验**：DTO 上用 JSR-380 注解声明参数校验规则

| DTO | 方向 | 说明 |
|-----|------|------|
| `AuthRequest` | 请求 | 登录：`username` + `password`，均为 `@NotBlank` |
| `RegisterRequest` | 请求 | 继承 `AuthRequest`，增加 `displayName` |
| `UserUpsertRequest` | 请求 | 创建/更新用户，`password` 可选（更新时可不修改密码） |
| `UserResponse` | 响应 | 用户信息（不含密码） |
| `LoginResponse` | 响应 | 登录成功返回 `token` + `UserResponse` |
| `CustomerRequest` | 请求 | 创建/更新客户，`name` 为 `@NotBlank`，`email` 有 `@Email` 校验 |
| `CustomerResponse` | 响应 | 客户完整信息，含时间戳 |
| `CustomerImportRequest` | 请求 | 批量导入，包含 `@NotEmpty` 的 `customers` 列表 |
| `ApiResponse<T>` | 响应 | 统一响应包装器：`success`/`message`/`data` |
| `PageResponse<T>` | 响应 | 分页响应：`total`（总数）/`items`（当前页数据） |

---

## 六、数据流全链路

以 **查询客户列表** 为例：

```
浏览器/客户端
  │
  │  GET /api/customers?keyword=张&page=1&size=10
  ▼
CustomerController.list()
  │  解析 @RequestParam，调用 Service
  ▼
CustomerService.list()
  │  计算 offset = (1-1)*10 = 0
  │  调用 customerMapper.findAll("张", 10, 0)
  │  调用 customerMapper.countAll("张")
  ▼
CustomerMapper（MyBatis 代理）
  │  执行 CustomerMapper.xml 中的 findAll SQL
  │    SELECT * FROM customers
  │    WHERE name LIKE '%张%' OR phone LIKE '%张%' OR tags LIKE '%张%'
  │    ORDER BY id DESC LIMIT 10 OFFSET 0
  ▼
MySQL 数据库
  │  返回结果集
  ▼
MyBatis 将结果集映射为 List<Customer>
  ▼
CustomerService
  │  将 Customer 列表转换为 CustomerResponse 列表
  │  封装为 PageResponse<CustomerResponse>(total=N, items=[...])
  ▼
CustomerController
  │  封装为 ApiResponse.ok(pageResponse)
  ▼
Jackson（Spring MVC 自动序列化）
  │  将 ApiResponse 序列化为 JSON
  ▼
HTTP 响应（200 OK，JSON）：
{
  "success": true,
  "message": null,
  "data": {
    "total": 5,
    "items": [
      { "id": 3, "name": "张三", "phone": "...", ... }
    ]
  }
}
```

---

## 七、配置说明

### `application.properties`

```properties
# 应用名称
spring.application.name=demo

# 数据库连接（支持通过环境变量 DB_USERNAME / DB_PASSWORD 覆盖默认值）
spring.datasource.url=jdbc:mysql://localhost:3306/medspa?useSSL=false&serverTimezone=Asia/Shanghai&...
spring.datasource.username=${DB_USERNAME:root}   # 默认 root
spring.datasource.password=${DB_PASSWORD:123}    # 默认 123

# MyBatis：XML 映射文件路径
mybatis.mapper-locations=classpath*:mapper/*.xml
# MyBatis：实体类包（XML 中可用类名简称代替全限定名）
mybatis.type-aliases-package=com.example.demo.domain
# MyBatis：自动将下划线映射为驼峰（created_at → createdAt）
mybatis.configuration.map-underscore-to-camel-case=true

# Jackson：日期序列化时区和格式
spring.jackson.time-zone=Asia/Shanghai
spring.jackson.date-format=yyyy-MM-dd'T'HH:mm:ss
```

---

## 八、前端页面

前端由两个静态 HTML 文件组成，均位于 `src/main/resources/static/`，Spring Boot 自动托管。

### 登录页（`index.html`）

- **访问**：`http://localhost:8080/`
- **功能**：管理员登录入口；登录失败弹出 Bootstrap Modal 消息框；登录成功将 token 写入 `localStorage` 并跳转到 `dashboard.html`
- **技术**：Bootstrap 5.3.3 + Bootstrap Icons（CDN）+ 原生 `fetch` API

### 管理主界面（`dashboard.html`）

- **访问**：`http://localhost:8080/dashboard.html`（需先登录）
- **功能**：
  - 顶部渐变导航栏：品牌名 + 用户管理/客户管理 Tab + 当前用户 + 退出登录
  - **用户管理 Tab**：关键词搜索、角色筛选、分页列表（角色/状态 Badge）、模态框新增/编辑、删除确认
  - **客户管理 Tab**：关键词搜索、分页列表（标签 Chip 展示）、模态框新增/编辑、删除确认
  - Toast 全局操作通知（成功/失败）
- **认证方式**：从 `localStorage` 读取 token，后续请求自动添加 `Authorization: Bearer <token>` 请求头；未登录则自动重定向回 `/`

---

## 九、API 文档（Swagger UI）

应用启动后，访问：

```
http://localhost:8080/swagger-ui/index.html
```

SpringDoc OpenAPI 会自动扫描所有 `@RestController` 并生成交互式 API 文档，可直接在浏览器中测试接口。

---

## 十、启动运行

> ⚠️ **生产环境安全警告**：本项目默认未启用 HTTPS，密码在网络传输过程中为**明文**。同时所有 API 接口无认证校验，**严禁直接用于生产环境**。生产部署前必须：配置 HTTPS/TLS、实现 Token 认证校验、修改默认数据库密码。

### 前置条件

1. **JDK 21+** 已安装
2. **MySQL 8.x** 正在运行，并已创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS medspa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. （可选）设置环境变量以覆盖默认数据库密码：

```bash
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

### 启动命令

```bash
# 使用 Maven Wrapper（推荐，无需本地安装 Maven）
./mvnw spring-boot:run

# Windows 系统
mvnw.cmd spring-boot:run

# 或先打包再运行
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### 验证启动

- 管理页面：http://localhost:8080/
- Swagger 文档：http://localhost:8080/swagger-ui/index.html

---

## 十一、已知局限与可改进点

| 问题 | 说明 |
|------|------|
| Token 未持久化 | 登录生成的 UUID token 存在内存中，服务重启后失效，且接口请求未校验 token |
| 无权限控制 | 所有接口均可被任意调用，无基于角色的访问控制 |
| 密码可明文传输 | 未启用 HTTPS，密码在网络传输过程中未加密 |
| 测试覆盖率低 | 目前只有 Spring 上下文加载的冒烟测试，无业务逻辑单元/集成测试 |
| 无软删除 | 删除操作直接从数据库移除记录，无法恢复 |
| 无操作日志 | 增删改操作无审计日志 |
