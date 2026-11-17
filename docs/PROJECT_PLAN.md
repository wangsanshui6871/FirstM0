# FirstM0 项目规划（AI 智能客服）

> 这是项目的主计划。AI 助手先读 `AGENTS.md`，再读本文件。
> 当前状态：**M1 进行中（M1.1、M1.2 已完成），下一步 M1.3**。
> 最后更新：2026-11-17。

## 0. 这份文档怎么用

- 人类：知道“现在在哪、下一步做什么、做到什么程度算完成”。
- AI 助手：只做当前里程碑，不跳步、不顺手加功能。
- 每完成一个里程碑：更新本文档的“状态”和“下一步”，并在 README 里同步。

## 1. 背景与定位

项目主人是双非本科计算机专业大三学生，目标是在中小厂找到 **Java 后端 / AI 应用开发** 相关工作。

这个项目要证明三件事：

1. 能独立做一个完整的后端系统，不是只会写 Controller。
2. 能把大模型接进真实业务，不是只会调 API。
3. 能讲清楚取舍、能测试、能部署，不是跟着教程抄一遍。

**不是**：大厂高并发项目、算法研究项目、生产级 SaaS、微服务展示项目。

## 2. 最终目标

### 业务闭环

用户咨询 → 意图判断 → 知识库检索 / 业务工具 → 回答并引用来源 → 低置信度转人工 → 工单闭环 → 满意度反馈。

### 技术闭环

Java 21 + Spring Boot 3 + MySQL + 大模型 API + RAG + SSE + JWT + Docker。

### 求职闭环

- 能在 3 分钟内演示完整流程
- 能在 10 分钟内讲清架构、数据表、RAG 链路、难点和取舍
- 有 README、架构图、数据库设计、API 文档、评测结果
- 有可量化但不造假的指标

## 3. 成功标准

到 M3 结束时，项目应该具备：

- [ ] 浏览器里能完成一次完整客服对话
- [ ] 会话和消息持久化到 MySQL
- [ ] 能上传知识文档并基于文档回答
- [ ] 回答带引用来源
- [ ] 不知道时能拒答并转人工
- [ ] 有工单列表和处理状态
- [ ] 有 100 条以上评测集和评测报告
- [ ] 有 Docker 部署方式和演示视频
- [ ] 面试时能讲清楚“为什么这么做”

## 4. 里程碑总览

| 里程碑 | 目标 | 主要功能 | 预计 |
|---|---|---|---|
| M0 | 骨架 | Spring Boot + 静态页 + `/api/hello` | 已完成 |
| M0.1 | 第一个业务接口 | `POST /api/chat` 固定回复 | 1～2 天 |
| M0.2 | 接数据库 | MySQL 存会话和消息 | 已完成 |
| M0.3 | 接大模型 | 真实 LLM 回复 + 超时/降级 | 已完成 |
| M0.4 | 多轮对话 | 最近 N 条历史 + 上下文控制 | 已完成 |
| M0.5 | 聊天页 | 页面调用 `/api/chat` | 已完成 |
| M1 | 产品化 | JWT、会话列表、SSE、Docker | 1～2 周 |
| M2 | 客服能力 | 知识库、RAG、引用、拒答、工单 | 2～3 周 |
| M3 | 简历项目 | 管理后台、评测、部署、演示 | 2～3 周 |
| M4 | 可选差异化 | Android、工具调用、情绪识别 | 视时间 |

> 时间按每周 10～15 小时估算。如果每周只有 5 小时，整体时间翻倍。

## 5. 详细任务

### M0 骨架（已完成）

已完成：

- Java 21 + Spring Boot 3.5.16 + Maven
- `GET /api/hello`
- 静态聊天页
- 2 个测试
- README

不再重复做。M0.1 也已完成，下一步从 M0.2 开始。

### M0.1 第一个业务接口：`POST /api/chat`（已完成）

已完成内容：

- `controller/ChatController.java`、`service/ChatService.java`
- `dto/ChatRequest.java`、`dto/ChatResponse.java`
- `common/ErrorResponse.java`、`common/GlobalExceptionHandler.java`
- `spring-boot-starter-validation` 依赖
- `ChatControllerTest`：正常回复、空消息 400、超长消息 400（共 3 个用例）
- 静态页改为调用 `POST /api/chat`
- `mvn test`：5 个测试全部通过

验收结果：

- [x] `mvn test` 通过
- [x] 页面输入"你好"，显示"我收到了：你好"
- [x] 能解释 `@RequestBody`、`record`、`@Valid`、`@RestControllerAdvice`

**目标**：理解 HTTP POST、JSON 请求体、DTO、Controller → Service 调用链。先不接数据库、不接大模型。

**新增文件**：

```text
controller/ChatController.java
service/ChatService.java
dto/ChatRequest.java
dto/ChatResponse.java
common/GlobalExceptionHandler.java
```

**接口**：

```http
POST /api/chat
Content-Type: application/json

{
  "conversationId": null,
  "message": "你好"
}
```

返回：

```json
{
  "conversationId": null,
  "reply": "我收到了：你好"
}
```

**要求**：

- `message` 不能为空，最长 2000 字符
- 新增 `spring-boot-starter-validation` 依赖
- 用 `@Valid` + `@NotBlank` 做参数校验
- 参数错误返回 400，JSON 至少包含 `code` 和 `message`
- 加一个 `MockMvc` 测试
- 静态页改成调用 `/api/chat`

**验收**：

- `mvn test` 通过
- 页面输入“你好”，显示“我收到了：你好”
- 你能解释 `@RequestBody`、`record`、`@Valid`、`@RestControllerAdvice`

**不要做**：JPA、MySQL、LLM、SSE、登录。

### M0.2 接入 MySQL（已完成，2026-11-03）

已完成内容：

- 依赖：`spring-boot-starter-data-jpa`、`mysql-connector-j`、测试用 `h2`
- 实体：`domain/Conversation.java`、`domain/Message.java`、`domain/MessageRole.java`
- Repository：`ConversationRepository`、`MessageRepository`
- Service：`ChatService` 改成“建会话 → 存 user 消息 → 生成回复 → 存 assistant 消息”
- 接口：`POST /api/chat` 返回真实 `conversationId`；新增 `GET /api/conversations/{id}/messages`
- 异常：`ConversationNotFoundException` → 404 `CONVERSATION_NOT_FOUND`
- 配置：`application.yml` 用 `${DB_USERNAME:root}` / `${DB_PASSWORD:}`；测试用 H2
- 测试：11 个全部通过（含 4 个真实 JPA + H2 落库测试）

验收结果：

- [x] `mvn test` 通过（11 个测试）
- [x] 本机 MySQL `first_m0` 库中能看到 `conversation`、`message` 数据
- [x] 重启应用后 `GET /api/conversations/1/messages` 仍返回 2 条历史
- [x] `role` 列为 `varchar(20)`，`message` 表有 `(conversation_id, id)` 复合索引
- [x] 中文以 utf8mb4 正确落库
- [x] 密码未写进代码或 Git，通过 `DB_PASSWORD` 环境变量传入

**目标**：把会话和消息存进数据库，刷新/重启不丢。

**依赖**：

- `spring-boot-starter-data-jpa`
- `mysql-connector-j`
- 测试用 `com.h2database:h2`（test scope）

**表**：

```sql
CREATE TABLE conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_message_conversation (conversation_id, id)
);
```

**实体**：

- `Conversation`
- `Message`
- `MessageRole` 枚举：`USER`、`ASSISTANT`

**Repository**：

- `ConversationRepository`
- `MessageRepository`

**Service 流程**：

1. `conversationId` 为空则新建会话
2. 保存 user 消息
3. 暂时返回固定回复
4. 保存 assistant 消息
5. 返回 `conversationId` + `reply`

**新增接口**：

```http
GET /api/conversations/{id}/messages
```

**配置**：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/first_m0?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
```

密码不要写死，不要提交到 Git。

**验收**：

- MySQL 里能看到 `conversation`、`message` 数据
- 重开应用后还能查到历史
- `mvn test` 通过（测试用 H2，不依赖本机 MySQL）
- 你能解释实体、Repository、事务、索引

**注意**：不要在大事务里等 LLM 返回。先保存用户消息，再调模型，再保存回复。

### M0.3 接入大模型（已完成，2026-11-09）

已完成内容：

- `config/LlmProperties.java`：读取 `llm.*`，用 record + 紧凑构造器给默认值
- `llm/LlmClient.java`：模型调用接口，Service 不感知 HTTP 细节
- `llm/LlmException.java`：把 HTTP 错误、超时、空回复统一成一种异常
- `llm/OpenAiCompatibleClient.java`：`RestClient` 调 `/v1/chat/completions`，连接超时 10 秒、读取 60 秒
- `ChatService` 拆成三段：存用户消息（事务 1）→ 事务外调模型 → 存回复（事务 2），用 `TransactionTemplate` 显式控制事务边界
- 失败降级：未配 Key、401、超时、空回复都返回兜底话术，接口仍是 200，兜底话术也会落库
- `application.yml` 增加 `llm.*`，Key 只走 `LLM_API_KEY` 环境变量
- 测试：16 个全部通过（新增模型客户端 4 个、失败降级 1 个）

验收结果：

- [x] `mvn test` 通过（16 个测试）
- [x] 真实 DeepSeek Key 端到端成功：回复「你好，我是FirstM0的客服助手，很高兴为你服务。」
- [x] 模型耗时约 866ms，日志记录 model、耗时、回复长度，不记录 Key
- [x] 两条消息（USER / ASSISTANT）正确落库到 MySQL，`GET /api/conversations/4/messages` 能读回
- [x] `LLM_API_KEY` 只在环境变量里，代码和 Git 中都没有

**目标**：把固定回复换成真实 LLM 回复，并处理超时、错误和空回复。

**新增文件**：

```text
llm/LlmClient.java
llm/OpenAiCompatibleClient.java
config/LlmProperties.java
```

**配置**：

```yaml
llm:
  base-url: ${LLM_BASE_URL:https://api.deepseek.com}
  api-key: ${LLM_API_KEY:}
  model: ${LLM_MODEL:deepseek-chat}
  timeout-seconds: 60
```

**实现**：

- 用 Spring `RestClient` 调 OpenAI 兼容接口
- 请求体包含 `model`、`messages`、`temperature`
- 解析 `choices[0].message.content`
- 连接超时 10 秒，读取超时 60 秒
- 429/5xx 可以重试一次
- 失败时返回友好提示，不把原始异常/密钥暴露给前端
- 记录耗时、模型名、状态；不要记录完整密钥

**System Prompt 第一版**：

```text
你是「FirstM0」的客服助手。
回答要简洁、礼貌，只基于已知信息回答。
不知道答案时，明确说“这个问题我需要帮你转人工确认”，不要编造。
不要编造订单、物流、退款、价格信息。
```

**验收**：

- 页面能收到真实模型回复
- 错误 Key / 超时 / 空回复都有友好提示
- `LLM_API_KEY` 只在环境变量里
- 你能解释 HTTP 客户端、超时、重试、降级

### M0.4 多轮对话（已完成，2026-11-17）

已完成内容：

- `llm/LlmMessage.java`：消息统一用 role + content 表示
- `LlmClient.chat(List<LlmMessage>)`：接口从“一句话”改成“一组消息”
- `MessageRepository.findTop10ByConversationIdOrderByIdDesc`：只取最近 10 条
- `ChatService.buildPrompt`：system prompt + 历史；从新到旧累加字符，超过 4000 丢掉最早的；最后翻回时间正序
- 当前问题无条件保留，保证模型至少知道用户在问什么
- 页面用内存变量保存 `conversationId`，同一页面内连续对话会落进同一个会话
- 测试：18 个全部通过（新增“历史进入 prompt”“超长截断”两个用例）

验收结果：

- [x] `mvn test` 通过（18 个测试）
- [x] 真实两轮对话：先问「你们支持退货吗」，再问「那运费谁出」，模型回答「退货运费由谁承担…」，能接上上文
- [x] 历史上限 10 条 + 4000 字符，超出时丢掉最早的
- [x] 两轮消息都正确落库（会话 16，消息 id 31～34）
- [x] 你能解释上下文窗口和 token 成本

**目标**：让模型能接着上文回答。

**做法**：

- 查询最近 10 条消息
- 总字符数限制在 4000 左右
- system prompt + 历史 + 当前问题
- 超长时先截断最早的消息
- 后续再考虑“摘要压缩”，M0.4 不做

**验收**：

- 先问“你们支持退货吗”，再问“那运费谁出”，模型能理解“那”指退货
- 上下文字符数不会无限增长
- 你能解释上下文窗口和 token 成本

### M0.5 聊天页（已完成，2026-11-17）

已完成内容：

- `conversationId` 存 `localStorage`（key：`firstm0.conversationId`）
- 刷新页面时调 `GET /api/conversations/{id}/messages` 恢复历史；会话不存在（404）时清掉本地 id
- 发送时进入 loading 状态：禁用按钮和输入框、显示“正在输入…”气泡
- 错误提示区分“后端返回的错误”和“连不上后端”，并提示检查服务
- “新对话”按钮：清本地 id 和页面消息，数据库记录保留
- 右上角显示当前会话编号
- 纯原生 HTML/CSS/JS，没有引入前端框架
- `application.yml` 把静态资源缓存设为 `period: 0`，避免改了页面却加载到旧版本

验收结果：

- [x] 同一页面内连续多轮对话，落在同一个 `conversationId`
- [x] 刷新页面后历史消息能恢复
- [x] 点“新对话”后下一条消息生成新会话
- [x] 后端停掉时页面显示“请求失败：Failed to fetch（检查后端是否启动）”，不再静默
- [x] `mvn test` 通过（18 个测试，后端逻辑未改动）

**目标**：把静态页改成真正可用的聊天页。

**要求**：

- 调 `POST /api/chat`
- `conversationId` 存在 `localStorage`
- 有 loading 状态
- 有错误提示
- 有“新对话”按钮
- 页面刷新后能恢复当前会话
- 不引入前端框架，原生 HTML/CSS/JS 即可

**验收**：

- 能连续多轮聊天
- 刷新页面后还能继续
- 断网/后端报错时有提示

### M1 产品化（进行中）

拆成 4 小步，逐个完成：

| 小步 | 内容 | 状态 |
|---|---|---|
| M1.1 | 用户表 + 注册/登录 + JWT | 已完成 |
| M1.2 | JWT 拦截器 + 会话归属用户 + 会话列表 | 已完成 |
| M1.3 | SSE 流式输出 | 待做 |
| M1.4 | Docker Compose | 待做 |

#### M1.1 注册登录（已完成，2026-11-17）

已完成内容：

- 依赖：`spring-security-crypto`（BCrypt）、`jjwt` 0.12.6（签发/校验 JWT）
- 实体：`domain/User.java`、`domain/UserRole.java`，表名 `app_user`（避开 MySQL 系统库的 `user`）
- `repository/UserRepository.java`：`existsByUsername`、`findByUsername`
- `auth/JwtService.java`：HS256 签发与校验，subject 存 userId，claim 存 username
- `service/AuthService.java`：注册（BCrypt 加密）、登录（`matches` 校验）
- `controller/AuthController.java`：`POST /api/auth/register`（201）、`POST /api/auth/login`（200）
- 异常：用户名重复 409、密码错误 401、token 无效 401
- 页面：`static/login.html`（登录/注册切换），成功后 token 存 localStorage，跳回聊天页
- 聊天页右上角显示用户名
- 测试：23 个全部通过（新增 5 个认证用例）

验收结果：

- [x] `mvn test` 通过（23 个测试）
- [x] 真实注册返回 201、userId=1、token 长度 178
- [x] 数据库 `app_user` 表的 `password_hash` 是 `$2a$10$` 开头、长度 60，无明文密码
- [x] 正确密码登录 200，错误密码 401
- [x] 重复用户名 409、弱密码 400
- [x] 能解释 JWT 结构、BCrypt 加盐、为什么登录失败不区分原因

#### M1.2 会话隔离（已完成，2026-11-17）

已完成内容：

- `Conversation` 加 `user_id`（允许为空，兼容 M1.1 之前的无主记录）
- `auth/AuthInterceptor` + `config/WebConfig`：`/api/**` 要登录，`/api/auth/**` 和 `/api/hello` 免登录
- `ConversationRepository` 改成 `findByIdAndUserId`、`findByUserIdOrderByUpdatedAtDesc`
- `ChatService` 所有方法带 `userId`，权限校验直接写进 where 条件
- 新增 `service/ConversationService`：列表、重命名、删除（删除时先删消息）
- 新增接口：`GET /api/conversations`、`PATCH /api/conversations/{id}`、`DELETE /api/conversations/{id}`
- 前端改成左右布局：左侧会话栏（切换/改名/删除）、右侧聊天，底部退出登录
- 测试：32 个全部通过（新增 chat 鉴权 1 个、会话管理 6 个、跨用户隔离 3 个）

验收结果：

- [x] `mvn test` 通过（32 个测试）
- [x] 无 token 访问会话列表返回 401
- [x] 用户 1 发消息创建会话 23，列表显示 1 条
- [x] 用户 2 的列表是 0 条，读/删用户 1 的会话都返回 404
- [x] 改名成功、删除后列表变 0
- [x] 登录页跳转、退出登录、token 失效自动回登录页

#### M1.3 SSE（待做）

教学顺序：假流式 → 接真实模型流式 → 处理断线/取消。

#### M1.4 Docker（待做）

`docker compose up` 启动 app + MySQL。

**M1 整体验收**：

- 登录后只能看自己的会话
- 回答能逐字出现
- `docker compose up` 能启动
- 你能解释 SSE 和 WebSocket 的区别

### M2 客服能力

**目标**：从“通用聊天”变成“基于知识的客服”。

#### M2.1 知识库上传

先支持 `.md` / `.txt`，不要一上来做 PDF/Word。

- 上传文档
- 保存 `knowledge_doc`
- 解析文本
- 切分 chunk
- 保存 `knowledge_chunk`
- 管理页能看文档列表和处理状态

#### M2.2 关键词检索版 RAG

- 用 MySQL `LIKE` 或 `FULLTEXT` 做第一版检索
- 取 top 3 chunk
- 拼进 prompt
- 回答带来源 `[1]`
- 先不接 embedding，先理解 RAG 链路

#### M2.3 向量检索版 RAG

- 调 embedding 接口
- 把向量存到 MySQL（JSON/TEXT）
- Java 里做 cosine similarity
- 取 top 5
- 关键词 + 向量混合检索
- 低于阈值时拒答

> 不引入 Milvus/Qdrant/pgvector。数据量小，MySQL + Java cosine 足够；面试时讲清楚“数据量大了换什么”。

#### M2.4 拒答与转人工

- 没有检索结果 → 拒答
- 最高分低于阈值 → 拒答
- 用户连续两次不满意 → 转人工
- 创建 `ticket`
- 管理页能看工单、改状态、回复

**验收**：

- 问知识库里的问题，能回答并引用来源
- 问知识库没有的问题，明确说不知道并转人工
- 工单能创建、查看、关闭
- 你能解释 chunk、embedding、cosine、topK、阈值、引用

### M3 简历项目

**目标**：从“能演示”变成“能写进简历、能扛面试追问”。

**功能**：

- 简单管理后台（静态 HTML + API 即可）
- 工单状态流转
- 满意度反馈
- 100～150 条评测集
- 评测脚本（Python 可以做辅助脚本）
- 指标看板
- Docker 部署
- README：架构图、截图、API、数据库设计、评测结果
- 3 分钟演示视频

**评测指标**：

- 检索 Recall@3
- 回答正确率
- 引用准确率
- 拒答准确率
- 转人工准确率
- 首 token 延迟
- P95 端到端延迟
- 单次对话 token 成本

**验收**：

- 有评测报告，数字真实可复现
- 有部署地址或本地 Docker 演示
- 能从“用户问题”讲到“数据库表”和“RAG 链路”
- 能讲 3 个难点和取舍

### M4 可选差异化

只有 M3 完成后才做：

- Android 客户端（Kotlin + Compose，或 Java + XML）
- 订单查询工具调用
- 情绪识别/敏感词
- 多租户
- 语音输入

不要为了“技术栈多”而做，必须能讲清楚价值。

## 6. 数据模型（目标）

| 表 | 作用 | 关键字段 | 里程碑 |
|---|---|---|---|
| `user` | 用户 | id, username, password_hash, role | M1 |
| `conversation` | 会话 | id, user_id, title, created_at, updated_at | M0.2 |
| `message` | 消息 | id, conversation_id, role, content, created_at | M0.2 |
| `knowledge_doc` | 知识文档 | id, title, filename, status, created_at | M2 |
| `knowledge_chunk` | 文档分片 | id, doc_id, chunk_index, content, embedding | M2 |
| `ticket` | 转人工工单 | id, conversation_id, reason, status, summary | M2 |
| `feedback` | 满意度 | id, message_id, rating, comment | M3 |
| `llm_call_log` | 模型调用日志 | id, model, prompt_tokens, completion_tokens, latency_ms, status | M3 |

## 7. API 设计（目标）

| 方法 | 路径 | 作用 | 里程碑 |
|---|---|---|---|
| GET | `/api/hello` | 骨架测试 | M0 |
| POST | `/api/chat` | 发送消息 | M0.1 |
| GET | `/api/conversations/{id}/messages` | 查历史 | M0.2 |
| POST | `/api/auth/register` | 注册 | M1 |
| POST | `/api/auth/login` | 登录 | M1 |
| GET | `/api/conversations` | 会话列表 | M1 |
| GET | `/api/conversations/{id}/stream` | SSE 流式回答 | M1 |
| POST | `/api/knowledge/documents` | 上传知识文档 | M2 |
| GET | `/api/knowledge/documents` | 文档列表 | M2 |
| POST | `/api/tickets` | 创建工单 | M2 |
| GET | `/api/tickets` | 工单列表 | M2 |
| POST | `/api/feedback` | 满意度反馈 | M3 |

约定：

- 请求/响应 JSON
- 错误响应：`{ "code": "...", "message": "..." }`
- 时间统一 `Asia/Shanghai`
- 不在 URL 里传敏感信息

## 8. RAG 设计

### 8.1 入库

```text
上传文档 → 解析文本 → 清洗 → 切分 chunk → 存 MySQL → 生成 embedding → 存 vector
```

### 8.2 切分

- 第一版按字符数：500 字左右
- overlap：80 字
- 保留标题/来源
- 后续按 token 或语义切分再优化

### 8.3 检索

```text
用户问题 → query rewrite（多轮时）→ 关键词 top 10 + 向量 top 10
→ 合并去重 → 取 top 5 → 阈值过滤 → 拼 prompt
```

### 8.4 生成

- system prompt 要求“只基于资料回答”
- 资料带编号 `[1] [2]`
- 回答里引用编号
- 没有资料或分数低 → 拒答 + 转人工
- 不要编造订单/物流/价格

### 8.5 扩展

数据量小：MySQL + Java cosine 足够。  
数据量大：再考虑 Elasticsearch / Milvus / Qdrant / pgvector。  
面试时要能说清楚：为什么现在不用、什么时候该换。

## 9. 评测

### 9.1 评测集

100～150 条，来源：

- 知识库能回答的：60%
- 知识库没有的：20%
- 多轮指代：10%
- 工具调用/业务问题：10%

格式建议：

```csv
id,question,expected_answer,expected_source,type
1,退货政策是什么,7 天无理由……,退换货政策.md,answerable
2,你们老板是谁,应拒答,,unanswerable
```

### 9.2 指标

- Recall@3：正确 chunk 是否在前 3
- Answer Correctness：人工抽样或 LLM-as-judge
- Citation Accuracy：引用是否真的支持答案
- Refusal Accuracy：不知道时是否拒答
- Handoff Accuracy：该转人工时是否转
- First Token Latency
- P95 End-to-End Latency
- Cost per Conversation

### 9.3 原则

- 不编数字
- 记录测试日期、模型名、参数
- 评测脚本可重复运行
- 报告放在 `docs/EVALUATION.md`

## 10. 安全与工程化

- API Key 只在后端环境变量
- 密码用 BCrypt
- JWT 过期时间合理
- 输入长度限制
- 限流（至少登录和聊天接口）
- 防 Prompt Injection：知识库内容不可信，和系统指令隔离
- 日志脱敏：手机号、地址、订单号
- 异常不返回堆栈和密钥
- SQL 用 JPA/参数化，不拼字符串
- 依赖版本不随意升级

## 11. 学习目标

| 里程碑 | 学完能回答的问题 |
|---|---|
| M0.1 | POST 和 GET 区别？`@RequestBody` 怎么工作？record 为什么能变 JSON？ |
| M0.2 | 实体和 DTO 区别？JPA 怎么映射表？事务什么时候生效？索引为什么加在 `(conversation_id, id)`？ |
| M0.3 | 怎么设置超时？429/5xx 怎么处理？怎么避免 Key 泄漏？ |
| M0.4 | 上下文窗口是什么？历史太长怎么办？token 成本怎么控制？ |
| M0.5 | 前端怎么保存会话？loading/error 怎么处理？ |
| M1 | JWT 流程？SSE 和 WebSocket 区别？Docker 怎么把应用和 MySQL 连起来？ |
| M2 | chunk 怎么切？embedding 是什么？cosine 怎么算？怎么减少幻觉？ |
| M3 | 怎么评测 RAG？P95 怎么测？怎么讲清楚项目取舍？ |

## 12. 时间计划（按每周 10～15 小时）

- M0：已完成
- M0.1：1～2 天
- M0.2：3～5 天
- M0.3：3～5 天
- M0.4：2 天
- M0.5：2 天
- M1：7～10 天
- M2：10～15 天
- M3：10～15 天

到 M2 大约 6～8 周；到 M3 大约 10～12 周。  
不要为了赶进度跳过测试和文档。

## 13. 面试叙事

### 3 分钟讲法

1. 背景：中小电商售后咨询，人工重复回答多
2. 方案：Android/Web + Java 后端 + MySQL + LLM + RAG + 工单
3. 链路：用户提问 → 意图 → 检索 → 生成 → 引用 → 低置信度转人工
4. 难点：多轮指代、RAG 召回、幻觉控制、流式输出
5. 结果：评测集、延迟、成本、转人工率（用真实数据）
6. 取舍：为什么不用向量数据库、为什么先单体、为什么先 Java 不转 Python

### 简历 bullet 模板

```text
AI 智能客服系统（个人项目）｜Java 21 · Spring Boot 3 · MySQL · LLM
- 独立完成 Java 后端、知识库 RAG 与管理后台，支持多轮对话、流式输出、引用来源和转人工工单。
- 设计“关键词 + 向量”混合检索链路，基于 MySQL 存储知识分片与向量，在 Java 侧实现 cosine 召回；构建 120 条评测集，Recall@3 达 XX%。
- 通过 SSE、上下文裁剪和热点缓存，将首 token 延迟降至 XXms；Docker Compose 部署，累计完成 XX 轮测试对话。
```

数字必须真实测过，不能编。

### 高频追问

- 为什么用 MySQL 存向量？数据量大了怎么办？
- chunk 怎么切？overlap 为什么是 80？
- 多轮指代怎么处理？
- 怎么防止模型胡说？
- 怎么评测？LLM-as-judge 靠谱吗？
- SSE 和 WebSocket 怎么选？
- 模型超时/限流怎么降级？
- API Key 怎么保证安全？
- 你的系统能撑多少并发？瓶颈在哪？

## 14. 明确不做

- 微服务、K8s
- 自训练/微调模型
- 多 Agent 编排
- 语音客服、数字人
- 一上来做多租户
- 为“技术栈多”而加 Redis、MQ、向量库
- 抄 GitHub 项目改名字
- 编造用户量、准确率、薪资

## 15. 完成定义（DoD）

每个里程碑完成必须满足：

- [ ] 代码能运行
- [ ] `mvn test` 通过
- [ ] 至少一个测试或可复现验证步骤
- [ ] README 更新
- [ ] `docs/PROJECT_PLAN.md` 状态更新
- [ ] 没有密钥、密码进入 Git
- [ ] 能用 1～3 分钟讲清楚做了什么

## 16. TraeCode 使用方式

1. 用 TraeCode 打开 `D:\IT\FirstM0`。
2. 在 `设置 → 规则 → 导入设置` 中开启“将 AGENTS.md 包含在上下文中”。
3. 确认 `.trae/rules/project_rules.md` 已存在。
4. 新开一个对话，先发：

```text
请先读 AGENTS.md 和 docs/PROJECT_PLAN.md。
当前只做 M0.1：POST /api/chat 固定回复，不接 MySQL、不接大模型。
先给我 5 行计划：目标、要改的文件、验证方式。
然后实现，完成后运行 mvn test，并用中文解释 Controller → Service → DTO 的调用链。
不要修改与 M0.1 无关的文件。
```

5. 每个里程碑单独开对话，避免上下文太长。
6. 每完成一步，让它更新本文件的“当前状态”和 README。