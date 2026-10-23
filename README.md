# FirstM0

M0 骨架：先把 Spring Boot 的前后端链路跑通，再一步一步扩展成 AI 智能客服项目。

当前版本刻意保持很小：

- 一个 Spring Boot 启动类
- 一个 `GET /api/hello` 接口
- 一个静态聊天页面
- 一个应用上下文测试和一个接口测试

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

页面里输入一句话，会调用 `/api/hello` 并返回 `Hello, 你的输入!`。

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

```http
GET /api/hello?name=M0
```

返回：

```json
{
  "message": "Hello, M0!"
}
```

## 项目结构

```text
src/main/java/com/example/firstm0/
├── FirstM0Application.java      # 启动类
├── controller/
│   └── HelloController.java     # /api/hello
└── dto/
    └── HelloResponse.java       # 响应对象（record）

src/main/resources/
├── application.yml              # 端口、应用名
└── static/index.html            # 聊天页面（当前调用 hello 占位接口）

src/test/java/com/example/firstm0/
├── FirstM0ApplicationTests.java # 上下文启动测试
└── controller/
    └── HelloControllerTest.java # 接口测试
```

## 建议先弄懂这 5 个点

1. `@SpringBootApplication` 做了什么？
2. `@RestController`、`@RequestMapping`、`@GetMapping` 分别是什么？
3. `@RequestParam` 怎么接收参数？
4. `record HelloResponse(String message)` 为什么能直接变成 JSON？
5. `MockMvc` 测试是怎么验证接口的？

## 下一步（按顺序）

- **M0.1**：新增 `POST /api/chat`，先返回固定字符串
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