# AGENTS.md — FirstM0 项目交接说明

> 这是给 AI 编程助手（TraeCode / Claude Code / Codex 等）的项目入口文档。
> **开始任何任务前，先读本文件，再读 `docs/PROJECT_PLAN.md`。**
> 如果文档与代码冲突，以代码、测试和 `pom.xml` 为准，并在回复中指出冲突。

## 1. 项目一句话

FirstM0 是一个用于求职简历的 **AI 智能客服** 项目：Java 后端负责业务与会话，MySQL 存数据，大模型负责回答，后续加知识库 RAG、转人工工单和评测。

目标岗位：**中小厂 Java 后端 / AI 应用开发**。  
项目定位：**简历项目 + 学习项目**，不是生产级 SaaS，不追求微服务和大厂架构。

## 2. 当前真实状态

已完成（M0 骨架 + M0.1）：

- Java 21
- Spring Boot 3.5.16
- Maven
- `GET /api/hello`
- `POST /api/chat`（固定回复 + `@Valid` 参数校验 + 全局异常处理）
- 静态聊天页 `src/main/resources/static/index.html`（调用 `/api/chat`）
- 5 个测试：上下文启动、`/api/hello`、chat 正常/空消息/超长消息
- README

尚未完成：

- MySQL
- 大模型调用
- 登录
- 多轮历史
- RAG 知识库
- 转人工工单
- 管理后台
- Android
- Docker

当前 Git 尚未初始化（机器没有配置 `user.name` / `user.email`）。

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
mvn test
mvn spring-boot:run
```

打开 `http://localhost:8080`。  
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

**M0.1 已完成**（`POST /api/chat` 固定回复 + 参数校验 + 全局异常处理 + 3 个测试）。

**下一步任务：M0.2** —— 接入 MySQL，把会话和消息存进数据库。

要求与验收标准见 `docs/PROJECT_PLAN.md` 的「M0.2 接入 MySQL」一节，此处不重复。

M0.2 范围提醒：

- 只新增 `spring-boot-starter-data-jpa`、`mysql-connector-j`，测试用 `com.h2database:h2`（test scope）
- 不接大模型、不做流式输出、不做登录
- 密码走环境变量，不写进代码、不提交 Git

## 9. 相关文档

- `docs/PROJECT_PLAN.md`：完整目标、里程碑、数据模型、API、RAG、评测、学习路线
- `README.md`：启动方式和当前状态