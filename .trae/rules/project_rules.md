---
description: FirstM0 项目通用规则（Java 21 + Spring Boot + AI 客服）
alwaysApply: true
---

# FirstM0 项目规则

1. 先读 `AGENTS.md` 和 `docs/PROJECT_PLAN.md`，再开始任务。
2. 用中文解释，面向初学者；每次只做一个里程碑。
3. 改代码前先给计划；改完必须运行 `mvn test` 并说明结果。
4. 技术栈固定：Java 21、Spring Boot 3.5.x、Maven、MySQL 8、Spring Data JPA。
5. 未到对应里程碑，不要引入 Redis、Docker、Spring AI、向量数据库、Android、微服务。
6. 不擅自升级依赖，不擅自改包名，不引入 Lombok。
7. 不编造指标；没测就写“未测”。
8. 不提交密钥；API Key 和数据库密码走环境变量。
9. 不要一次生成大量代码；小步修改，保持可运行。
10. 每次完成一个功能，更新 README 和 docs/PROJECT_PLAN.md。
11. 需求不明确时先问，不要猜。
12. 代码规范：构造器注入、DTO 用 record、SLF4J、`@RestControllerAdvice`、API 前缀 `/api`。