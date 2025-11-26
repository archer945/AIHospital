# Repository Guidelines

## 项目结构与模块
- `prescription-service`：Spring Boot 3.2（Java 17）微服务，使用 Spring AI 与 MyBatis-Plus；控制器/服务/Mapper 分层清晰；核心配置在 `src/main/resources/application.yaml`。
- `gateway`：Spring Cloud Gateway + Nacos 注册发现，继承根 POM 统一依赖版本。
- `frontend`：Vite + Vue 3，路由在 `src/router`，视图在 `src/views`，通用样式在 `src/assets`。
- 根 `pom.xml` 仅做依赖管理；在根执行 Maven 时使用 `-pl <module>` 选择模块。

## 构建、测试与开发命令
- 打包后端：`mvn -pl prescription-service clean package`（产物位于 `prescription-service/target`）。
- 运行后端：`mvn -pl prescription-service spring-boot:run` 或 `java -jar prescription-service/target/*.jar`。
- 启动网关：`mvn -pl gateway spring-boot:run`，依赖 Nacos 可用。
- 前端开发：`npm --prefix frontend install` 后 `npm --prefix frontend run dev`；生产构建 `npm --prefix frontend run build`。
- 后端测试（需补充用例后执行）：`mvn -pl prescription-service test`。

## 代码风格与命名
- Java：4 空格缩进，Java 17，尽量构造器注入；命名遵循 `Controller → Service → Mapper`，接口/实现用 `*Service`/`*ServiceImpl`，DTO 以 `DTO` 结尾，实体置于 `entity` 包。公共 API 逻辑可加简短 Javadoc。
- MyBatis-Plus Mapper 为接口，定制 SQL 保持注解或 XML 风格一致。
- Vue：推荐 `<script setup>`，组件文件 PascalCase，路由命名与业务一致，公共样式集中在 `src/assets/main.css`。

## 测试指南
- 后端测试放在 `prescription-service/src/test/java`，类名用 `*Tests`，使用 JUnit 5 与 Spring Boot 测试切片；外部 AI/Nacos 调用需 Mock，避免联网依赖。
- 新接口需覆盖成功与校验失败路径，包含 Mapper 交互的集成测试。
- 前端目前未配置测试，如增加，优先 `vitest` 并就近放置用例。

## 提交与 Pull Request
- Git 历史有简短中文与 `feat(scope): ...` 混用，建议遵循 Conventional Commits（例：`feat(prescription): add conversation paging API`）且单一关注点。
- 提交应同步相关文档/配置；PR 描述变更、运行/验证方式（`mvn ...`、`npm ...`）、关联需求，UI 改动附截图或 GIF。
- 对端口、Nacos、数据库、API Key 等配置改动需在 PR 中明确说明。

## 安全与配置
- 切勿提交真实密钥；通过环境变量或本地 profile 覆盖 `application.yaml` 中的 DB/Nacos/OpenAI 配置，并在 PR 备注所需变量。
- 分享日志/截图时注意脱敏患者/医生标识与令牌。
