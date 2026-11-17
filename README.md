# FirstM0

M1.1：支持注册和登录（JWT）。登录状态存在浏览器里，右上角显示用户名。

当前版本刻意保持很小：

- 一个 Spring Boot 启动类
- 一个 `GET /api/hello`（骨架验证用）
- `POST /api/auth/register`、`POST /api/auth/login`（BCrypt 加密密码 + 签发 JWT）
- 一个 `POST /api/chat`（参数校验 + 落库 + 多轮上下文 + 真实模型回复 + 失败降级）
- 一个 `GET /api/conversations/{id}/messages`（查历史消息）
- 聊天页（localStorage 记会话、刷新恢复、loading、错误提示、新对话）
- 登录页（登录/注册切换）
- 23 个测试（认证 5 个、上下文启动、hello、chat 3 个、历史 2 个、数据库与上下文 7 个、模型客户端 4 个）

**现在还没有**：会话按用户隔离、会话列表、SSE 流式输出、RAG 知识库。这些在 M1.2 / M1.3 / M2 里加。

## AI 接手入口

- [AGENTS.md](AGENTS.md)：给 TraeCode / 其他 AI 的项目交接说明
- [docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md)：目标、里程碑、验收标准、学习路线
- [.trae/rules/project_rules.md](.trae/rules/project_rules.md)：TraeCode 项目规则

TraeCode 打开项目后，建议在 `设置 → 规则 → 导入设置` 中开启“将 AGENTS.md 包含在上下文中”，然后新开对话，让它先读 `AGENTS.md` 和 `docs/PROJECT_PLAN.md`。

## 环境

- JDK 21
- Maven 3.9+
- MySQL 8（本地开发用）
- 一个大模型 API Key（默认接入 DeepSeek）

## 启动

先建库（只需执行一次）：

```sql
CREATE DATABASE IF NOT EXISTS first_m0 DEFAULT CHARACTER SET utf8mb4;
```

再配置两个环境变量（只需设置一次，之后新开的终端都能读到）：

```powershell
# 数据库密码，不要写进代码或配置文件
setx DB_PASSWORD "你的 MySQL 密码"

# 大模型 Key，只放环境变量，绝不提交 Git
setx LLM_API_KEY "sk-你的DeepSeekKey"
```

然后在 `D:\IT\FirstM0` 下执行：

```powershell
mvn spring-boot:run
```

> 用户名默认是 `root`，想换就再设置 `DB_USERNAME`。
> 表结构由 JPA 自动创建（`ddl-auto: update`），第一次启动会自动建 `conversation` 和 `message` 表。
> 换模型厂商时改 `llm.base-url` / `llm.model` 即可，代码不用动。

然后打开：

```text
http://localhost:8080
```

页面里输入一句话，会调用 `POST /api/chat`，由大模型生成回复，同时把两条消息写进 MySQL。

> 注意：`setx` 设置的变量对已经打开的终端和 IDEA 不生效，需要重开终端或重启 IDEA。

## Java 版本

项目按 Java 21 编译。启动前确认 `java -version` 和 `mvn -version` 都显示 21。

> 本机默认 JDK 是 25（`PATH` 里指向 `D:\JavaJDK`），且 `JAVA_HOME` 未设置。
> 所以每次新开终端跑 Maven 前，都要先按下面的命令切到 21，否则会用 25 运行。

如果机器上有多个 JDK，在 PowerShell 里临时切换：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -version
```

IDEA 里还要确认：

- `Project Structure → Project → SDK` 选 JDK 21
- `Settings → Build Tools → Maven → Runner → JRE` 选 JDK 21

## 接口

### 0. 注册与登录（M1.1）

```http
POST /api/auth/register
POST /api/auth/login
Content-Type: application/json
```

请求体：

```json
{ "username": "testuser01", "password": "password123" }
```

返回（注册成功是 `201`，登录成功是 `200`）：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "username": "testuser01",
  "role": "USER"
}
```

说明：

- 密码用 BCrypt 加密后入库，数据库里只有 `$2a$10$...` 这样的哈希，没有明文
- 用户名重复返回 `409`，密码错误返回 `401`
- `token` 有效期 7 天，前端存在 `localStorage` 的 `firstm0.token`
- 聊天接口目前还不校验 token（M1.2 会加拦截器做会话隔离）

### 1. 骨架验证

```http
GET /api/hello?name=M0
```

返回：

```json
{
  "message": "Hello, M0!"
}
```

### 2. 聊天（M0.4）

```http
POST /api/chat
Content-Type: application/json
```

请求体：

```json
{
  "conversationId": null,
  "message": "你们支持退货吗"
}
```

返回：

```json
{
  "conversationId": 16,
  "reply": "您好，退货政策我这边没有具体信息，这个问题我需要帮你转人工确认。"
}
```

接着聊，把上次返回的 id 带上：

```json
{
  "conversationId": 16,
  "message": "那运费谁出"
}
```

模型会结合上文回答「退货运费由谁承担…」，而不是把“那”当成一个孤立的问题。

说明：

- 每次只带**最近 10 条**消息，历史总字符数不超过 **4000**，超出时丢掉最早的
- 当前问题一定会保留，保证模型至少知道你在问什么
- `conversationId` 为 `null` 时新建会话，响应里返回真实 id
- `conversationId` 不存在时返回 `404`
- 模型超时、Key 错误、返回为空时，回复变成兜底话术，接口仍然是 `200`

参数校验：

- `message` 不能为空，最长 2000 字符
- 校验失败返回 `400`，格式统一为：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "消息不能为空"
}
```

### 3. 查历史消息（M0.2）

```http
GET /api/conversations/4/messages
```

返回按时间从早到晚排列的消息数组：

```json
[
  { "id": 7, "role": "USER", "content": "你好，请用一句话介绍你自己", "createdAt": "2026-11-09T17:59:32.465836" },
  { "id": 8, "role": "ASSISTANT", "content": "你好，我是FirstM0的客服助手，很高兴为你服务。", "createdAt": "2026-11-09T17:59:33.481215" }
]
```

会话不存在时返回 `404`：

```json
{
  "code": "CONVERSATION_NOT_FOUND",
  "message": "会话不存在：99"
}
```

## 项目结构

```text
src/main/java/com/example/firstm0/
├── FirstM0Application.java      # 启动类
├── controller/
│   ├── HelloController.java     # /api/hello（骨架验证）
│   ├── ChatController.java      # /api/chat
│   └── ConversationController.java  # /api/conversations/{id}/messages
├── service/
│   ├── ChatService.java         # 三段式：存消息 → 事务外调模型 → 存回复
│   └── AuthService.java         # 注册（BCrypt 加密）、登录（校验密码 + 签发 JWT）
├── auth/
│   ├── JwtService.java          # 签发和校验 JWT
│   └── TokenInvalidException.java  # token 无效 -> 401
├── llm/
│   ├── LlmClient.java           # 模型调用接口（Service 只依赖它）
│   ├── LlmMessage.java          # 一条模型消息：role + content
│   ├── LlmException.java        # 把 HTTP 错误/超时/空回复统一成一种异常
│   └── OpenAiCompatibleClient.java  # 用 RestClient 调 OpenAI 兼容接口
├── config/
│   ├── LlmProperties.java       # 读取 llm.* 配置
│   └── JwtProperties.java       # 读取 jwt.* 配置
├── domain/
│   ├── Conversation.java        # 会话实体 -> conversation 表
│   ├── Message.java             # 消息实体 -> message 表
│   ├── MessageRole.java         # USER / ASSISTANT
│   ├── User.java                # 用户实体 -> app_user 表
│   └── UserRole.java            # USER / ADMIN
├── repository/
│   ├── ConversationRepository.java  # 会话表操作
│   ├── MessageRepository.java       # 消息表操作
│   └── UserRepository.java          # 用户表操作
├── dto/
│   ├── HelloResponse.java       # hello 响应对象（record）
│   ├── ChatRequest.java         # chat 请求体（含参数校验）
│   ├── ChatResponse.java        # chat 响应体
│   └── MessageResponse.java     # 历史消息响应（record）
└── common/
    ├── ErrorResponse.java       # 统一错误响应 { code, message }
    ├── ConversationNotFoundException.java  # 会话不存在 -> 404
    └── GlobalExceptionHandler.java  # 全局异常处理（400 / 404）

src/main/resources/
├── application.yml              # 端口、数据源、JPA 配置
└── static/index.html            # 聊天页面（调用 POST /api/chat）

src/test/resources/
└── application.yml              # 测试用 H2 内存库配置

src/test/java/com/example/firstm0/
├── FirstM0ApplicationTests.java # 上下文启动测试
├── controller/
│   ├── HelloControllerTest.java # hello 接口测试
│   ├── AuthControllerTest.java  # 注册/登录/重复用户名/弱密码/密码错误
│   ├── ChatControllerTest.java  # chat 接口测试（正常 / 空消息 / 超长）
│   └── ConversationControllerTest.java  # 历史消息 200 / 404
├── llm/
│   └── OpenAiCompatibleClientTest.java  # 用本地假服务器测 HTTP：正常/空回复/500/缺 Key
└── service/
    └── ChatServiceTest.java     # 真实 JPA + H2 的落库测试 + 模型失败降级
```

## 建议先弄懂这 6 个点

1. JWT 的三段结构分别是什么？为什么 payload 不是加密的，却还能防篡改？
2. BCrypt 为什么每次加密同一个密码结果都不一样？怎么验证密码对不对？
3. 登录失败为什么统一提示“用户名或密码错误”，而不是区分两种情况？
4. `@Valid` 为什么只校验注册请求、不校验登录请求的密码长度？
5. `localStorage` 存 token 有什么风险？换成 Cookie 会怎样？
6. 为什么这轮不引入 Spring Security 全套？它默认会做什么？

## 下一步（按顺序）

- **M0.1 ~ M0.5** —— ✅ 已完成
- **M1.1**：注册 / 登录 / JWT —— ✅ 已完成
- **M1.2**：加 JWT 拦截器，会话归属当前用户（列表、重命名、删除）
- **M1.3**：SSE 流式输出
- **M1.4**：Docker Compose
- **M2**：知识库 RAG、引用、拒答、转人工
- **M3**：管理后台、工单、评测、部署、演示视频

## 常用命令

```powershell
mvn test          # 跑测试（用 H2 内存库，不需要本机 MySQL）
mvn spring-boot:run
mvn clean package # 打包
java -jar target/first-m0-0.0.1-SNAPSHOT.jar
```