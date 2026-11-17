# AGENTS.md — FirstM0 项目交接说明

> 这是给 AI 编程助手（TraeCode / Claude Code / Codex 等）的项目入口文档。
> **开始任何任务前，先读本文件，再读 `docs/PROJECT_PLAN.md`。**
> 如果文档与代码冲突，以代码、测试和 `pom.xml` 为准，并在回复中指出冲突。

## 1. 项目一句话

FirstM0 是一个用于求职简历的 **AI 智能客服** 项目：Java 后端负责业务与会话，MySQL 存数据，大模型负责回答，后续加知识库 RAG、转人工工单和评测。

目标岗位：**中小厂 Java 后端 / AI 应用开发**。  
项目定位：**简历项目 + 学习项目**，不是生产级 SaaS，不追求微服务和大厂架构。

## 2. 当前真实状态

已完成（M0 骨架 + M0.1 ~ M0.5 + M1.1 ~ M1.3）：

- Java 21
- Spring Boot 3.5.16
- Maven
- MySQL 8（库名 `first_m0`）
- `GET /api/hello`
- `POST /api/chat`（参数校验 + 会话/消息落库 + 多轮上下文 + 真实大模型回复 + 失败降级）
- `GET /api/conversations/{id}/messages`（查历史，会话不存在返回 404）
- 实体 `Conversation`、`Message`、`MessageRole` + 两个 JPA Repository
- 大模型：`llm/LlmClient`、`llm/LlmMessage`、`llm/LlmException`、`llm/OpenAiCompatibleClient`
- 上下文控制：最近 10 条消息、总字符上限 4000，超出丢最早的
- 配置：`config/LlmProperties`，Key 只走 `LLM_API_KEY` 环境变量
- 静态聊天页 `src/main/resources/static/index.html`（localStorage 记会话、刷新恢复历史、loading、错误提示、新对话）
- 登录页 `src/main/resources/static/login.html`（登录/注册切换）
- 认证：`domain/User`、`UserRole`、`repository/UserRepository`、`auth/JwtService`、`service/AuthService`、`controller/AuthController`
- 鉴权：`auth/AuthInterceptor` + `config/WebConfig`，`/api/**` 需登录，`/api/auth/**` 与 `/api/hello` 除外
- 会话归属：`conversation.user_id`，所有查询都带 userId，别人的会话一律 404
- 会话管理：`service/ConversationService`，列表 / 重命名 / 删除
- 流式：`LlmClient.chatStream` + `POST /api/chat/stream`（SSE，事件 meta/delta/done/error）
- 异步：`spring.threads.virtual.enabled=true`，推流跑在虚拟线程上
- 密码用 BCrypt 加密存库，JWT 走 `jwt.*` 配置，密钥可用 `JWT_SECRET` 覆盖
- 35 个测试：认证 5 个、chat 5 个、会话管理 8 个、上下文启动 1 个、hello 1 个、数据库与上下文 9 个、模型客户端 6 个
- README、`docs/PROJECT_PLAN.md`

尚未完成：

- Docker（M1.4）
- RAG 知识库
- 转人工工单
- 管理后台
- Android

Git 已初始化并推送到 https://github.com/wangsanshui6871/FirstM0（main 分支）。

## 3. 技术栈约束

必须保持：

- Java 21
- Spring Boot 3.5.x
- Maven
- 包名 `com.example.firstm0`
- API 前缀 `/api`
- 数据库 MySQL 8
- 持久层先用 Spring Data JPA
- HTTP 调模型先用 Spring `RestClient`
- 配置通过 `application.yml` + 环境变量

在对应里程碑之前，不要引入：

- Redis
- Docker / K8s
- Spring AI / LangChain4j
- 向量数据库（Milvus / Qdrant / pgvector）
- Android
- 微服务、消息队列
- Lombok

## 4. 目录约定

```text
src/main/java/com/example/firstm0/
├── FirstM0Application.java
├── controller/        # HTTP 接口
├── service/           # 业务编排
├── repository/        # JPA Repository
├── domain/            # 实体
├── dto/               # 请求/响应 record
├── llm/               # 大模型客户端
├── config/            # 配置属性
└── common/            # 全局异常、通用响应
```

新增文件按这个结构放，不要把所有类都堆在根包。

## 5. 运行与验证

```powershell
cd D:\IT\FirstM0
$env:DB_PASSWORD = "你的 MySQL 密码"   # 不要写进任何文件
$env:LLM_API_KEY = "sk-..."            # 只走环境变量，不要提交 Git
mvn test
mvn spring-boot:run
```

打开 `http://localhost:8080`。  
`mvn test` 使用 `src/test/resources/application.yml` 里的 H2 内存库，不需要本机 MySQL。  
每次改完代码必须运行 `mvn test`；如果失败，先修复，不要继续加功能。

## 6. 协作规则（最重要）

1. **用中文解释**，面向一个刚开始学 Spring Boot 的大三学生。
2. **一次只做一个里程碑或一个小任务**，不要顺手加别的功能。
3. 改代码前先给 3～6 行计划：本次目标、要改哪些文件、怎么验证。
4. 改完后说明：改了什么、为什么、怎么运行、你应该理解哪几个点。
5. 每个功能尽量配一个测试；至少保证 `mvn test` 通过。
6. 不要一次生成几百行代码；优先小步、可运行、可回滚。
7. 不要擅自升级 Spring Boot / Java / 依赖版本。
8. 不要改与当前任务无关的文件。
9. 不要编造性能数据、用户量、准确率；没有测就写“未测”。
10. 不要把 API Key、数据库密码写进代码或提交到 Git。
11. 遇到需求不明确，先问，不要猜着做。
12. 每完成一个里程碑，更新 `README.md` 和 `docs/PROJECT_PLAN.md` 的状态。

## 7. 代码规范

- 使用构造器注入，不用字段注入。
- DTO 用 `record`。
- 实体用普通类，不用 Lombok。
- 统一使用 `@RestControllerAdvice` 处理异常。
- 日志用 SLF4J，不用 `System.out.println`。
- 时间字段用 `LocalDateTime`，数据库用 `DATETIME`。
- 接口返回 JSON；错误响应至少包含 `code` 和 `message`。
- 数据库表名、字段名用 snake_case；Java 字段用 camelCase。
- 不要过度设计：没有第二个实现时，不要先写接口 + 工厂 + 策略。

## 8. 任务进度

**M1.3 已完成**（SSE 流式输出 + 虚拟线程 + 断线保留部分回复 + 35 个测试）。

**下一步任务：M1.4** —— Docker Compose，一条命令启动 app + MySQL。

要求见 `docs/PROJECT_PLAN.md` 的「M1.4 Docker」一节。

M1.4 范围提醒：

- 先写 `Dockerfile`（多阶段构建：Maven 打包 → JRE 运行）
- 再写 `docker-compose.yml`（app + mysql，带健康检查）
- 数据库密码、JWT 密钥、LLM Key 全部走环境变量
- 不在镜像里打包密钥
- 不引入 K8s、Redis

## 9. 相关文档

- `docs/PROJECT_PLAN.md`：完整目标、里程碑、数据模型、API、RAG、评测、学习路线
- `README.md`：启动方式和当前状态