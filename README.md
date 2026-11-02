# FirstM0

M0.1 骨架：前后端链路已打通，聊天接口先返回固定回复，下一步再逐步接数据库和大模型。

当前版本刻意保持很小：

- 一个 Spring Boot 启动类
- 一个 `GET /api/hello`（骨架验证用）
- 一个 `POST /api/chat`（固定回复 + 参数校验）
- 一个静态聊天页面
- 5 个测试（上下文启动、hello 接口、chat 正常/空消息/超长消息）

**现在还没有**：数据库、大模型、登录、RAG、Android。这些都在后面的 M0.x / M1 里加。

## AI 接手入口

- [AGENTS.md](AGENTS.md)：给 TraeCode / 其他 AI 的项目交接说明
- [docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md)：目标、里程碑、验收标准、学习路线
- [.trae/rules/project_rules.md](.trae/rules/project_rules.md)：TraeCode 项目规则

TraeCode 打开项目后，建议在 `设置 → 规则 → 导入设置` 中开启“将 AGENTS.md 包含在上下文中”，然后新开对话，让它先读 `AGENTS.md` 和 `docs/PROJECT_PLAN.md`。

## 环境

- JDK 21
- Maven 3.9+

## 启动

在 `D:\IT\FirstM0` 下执行：

```powershell
mvn spring-boot:run
```

然后打开：

```text
http://localhost:8080
```

页面里输入一句话，会调用 `POST /api/chat` 并返回 `我收到了：你的输入`。

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

### 2. 聊天（M0.1）

```http
POST /api/chat
Content-Type: application/json
```

请求体：

```json
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

参数校验：

- `message` 不能为空，最长 2000 字符
- 校验失败返回 `400`，格式统一为：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "消息不能为空"
}
```

> M0.1 的 `conversationId` 目前原样回传（请求是 null，响应也是 null）。
> M0.2 接入 MySQL 后会返回真实会话 id。

## 项目结构

```text
src/main/java/com/example/firstm0/
├── FirstM0Application.java      # 启动类
├── controller/
│   ├── HelloController.java     # /api/hello（骨架验证）
│   └── ChatController.java      # /api/chat（M0.1）
├── service/
│   └── ChatService.java         # 聊天业务逻辑（当前返回固定回复）
├── dto/
│   ├── HelloResponse.java       # hello 响应对象（record）
│   ├── ChatRequest.java         # chat 请求体（含参数校验）
│   └── ChatResponse.java        # chat 响应体
└── common/
    ├── ErrorResponse.java       # 统一错误响应 { code, message }
    └── GlobalExceptionHandler.java  # 全局异常处理（400）

src/main/resources/
├── application.yml              # 端口、应用名
└── static/index.html            # 聊天页面（调用 POST /api/chat）

src/test/java/com/example/firstm0/
├── FirstM0ApplicationTests.java # 上下文启动测试
└── controller/
    ├── HelloControllerTest.java # hello 接口测试
    └── ChatControllerTest.java  # chat 接口测试（正常 / 空消息 / 超长）
```

## 建议先弄懂这 6 个点

1. `@SpringBootApplication` 做了什么？
2. `@RestController`、`@RequestMapping`、`@GetMapping` / `@PostMapping` 分别是什么？
3. `@RequestBody` 怎么把 JSON 变成 record？
4. `@Valid` + `@NotBlank` + `@Size` 是怎么触发校验的？
5. `@RestControllerAdvice` 为什么能把异常统一成 `{ code, message }`？
6. Controller → Service → DTO 的调用链是怎么走的？

## 下一步（按顺序）

- **M0.1**：`POST /api/chat` 固定回复 —— ✅ 已完成
- **M0.2**：接 MySQL，保存会话和消息
- **M0.3**：接大模型 API，把固定字符串换成真实回复
- **M0.4**：加多轮历史
- **M0.5**：把静态页改成真正的聊天页
- **M1**：SSE 流式输出、登录、会话列表、Docker
- **M2**：知识库 RAG、引用、拒答、转人工
- **M3**：管理后台、工单、评测、部署、演示视频

## 常用命令

```powershell
mvn test          # 跑测试
mvn spring-boot:run
mvn clean package # 打包
java -jar target/first-m0-0.0.1-SNAPSHOT.jar
```