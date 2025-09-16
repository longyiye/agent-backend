## agent-backend

一个基于 Spring Boot 3 的 AI Agent 后端示例工程，集成了 Spring AI（DashScope、Ollama）、Knife4j/Swagger、SSE 流式输出、向量存储（PGVector，可选）与 MCP（Model Context Protocol）客户端/服务端地图服务

### 功能特性
- **对话服务**: `LoveApp` 应用的同步与 SSE 流式对话接口
- **SSE 多种形态**: Flux、ServerSentEvent、SseEmitter 三种方式
- **AI 模型集成**: DashScope（通义千问）与本地 Ollama（可选）
- **接口文档**: 集成 Springdoc + Knife4j，开箱即用
- **向量存储**: PGVector（可自定义/自动装配二选一）
- **MCP**: 
  - 客户端：通过 `mcp-servers.json` 配置外部 MCP Server
  - 服务端：子模块 `image-search-mcp-server` 提供图片搜索 MCP Server 示例

### 运行环境
- JDK 21+
- Maven 3.9+
- 可选：Docker / Docker Compose、PostgreSQL（使用 PGVector 时）

### 快速开始
1) 安装依赖并打包
```bash
mvn clean package -DskipTests
```

2) 运行（本地 profile 默认为 `local`）
```bash
java -jar target/agent-backend-0.0.1-SNAPSHOT.jar
```

3) 访问接口文档
- Swagger/Knife4j: `http://localhost:8123/api/swagger-ui.html`

### 配置说明
核心配置参见：`src/main/resources/application.yml`、`application-local.yml`、`application-prod.yml`

- 应用与服务
  - 端口：`8123`
  - 上下文路径：`/api`
  - Swagger 文档：`/swagger-ui.html`（完整路径见上）

- AI 相关（建议以环境变量或外部配置覆盖，不要在仓库中明文暴露）
  - DashScope Key: `spring.ai.dashscope.api-key`
  - DashScope 模型：`spring.ai.dashscope.chat.options.model`（示例 `qwen-plus`）
  - Ollama：`spring.ai.ollama.base-url` 和 `spring.ai.ollama.chat.model`

- 数据源（使用 PGVector 时）
  - `spring.datasource.url`
  - `spring.datasource.username`
  - `spring.datasource.password`
  - PGVector 参数：`spring.ai.vectorstore.pgvector.*`

- 其他示例
  - `search-api.api-key`（自定义第三方搜索 API）

切换运行环境：
```bash
# Windows PowerShell 示例
$env:SPRING_PROFILES_ACTIVE = "prod"; java -jar target/agent-backend-0.0.1-SNAPSHOT.jar
```

### 对外接口
Controller 位于 `com.yiye.controller`

- `GET /api/ai/love_app/chat/sync`
  - 参数：`message`、`chatId`
  - 描述：`LoveApp` 同步调用

- `GET /api/ai/love_app/chat/sse`（`text/event-stream`）
  - 参数：`message`、`chatId`
  - 描述：`LoveApp` SSE 流式输出（Flux<String>）

- `GET /api/ai/love_app/chat/server_sent_event`
  - 参数：`message`、`chatId`
  - 描述：SSE 流式输出（`ServerSentEvent<String>`）

- `GET /api/ai/love_app/chat/sse/emitter`
  - 参数：`message`、`chatId`
  - 描述：SSE 流式输出（`SseEmitter`），可更灵活地控制发送/完成/错误

- `GET /api/ai/manus/chat`
  - 参数：`message`
  - 描述：流式调用 `Manus` 超级智能体

说明：实际返回结构与行为依赖业务实现（`com.yiye.app.LoveApp`、`com.yiye.agent.Manus` 等），可在接口文档页调试

### Swagger/Knife4j
- 分组/扫描包在 `application.yml` 中配置：
  - `springdoc.group-configs[0].packages-to-scan: com.yiye.controller`
- 访问路径：`/api/swagger-ui.html`

### MCP（Model Context Protocol）
本项目演示了 MCP 客户端与服务端：

1) 客户端配置
- 文件：`src/main/resources/mcp-servers.json`
- 示例包含：
  - `amap-maps`：通过 `npx` 启动的高德地图 MCP Server（需要 `Node.js`）
  - `image-search-mcp-server`：本仓库子模块产物的 JAR 直连
- 你可以通过外部化配置或环境变量，替换/隐藏密钥，如 `AMAP_MAPS_API_KEY`

2) 服务端子模块 `image-search-mcp-server`
- 路径：`image-search-mcp-server`
- 打包运行：
```bash
cd image-search-mcp-server
mvn clean package -DskipTests
java -Dspring.ai.mcp.server.stdio=true -Dspring.main.web-application-type=none -jar target/image-search-mcp-server-0.0.1-SNAPSHOT.jar
```
- 也可通过主工程的 `mcp-servers.json` 以 stdio 方式联动

### Docker 运行
项目根目录提供 `Dockerfile`（主应用）。示例：
```bash
# 构建镜像
mvn -q -DskipTests package
docker build -t agent-backend:latest .

# 运行容器（按需传入环境变量覆盖敏感配置）
docker run -p 8123:8123 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_AI_DASHSCOPE_API_KEY=your_key \
  agent-backend:latest
```

### 开发提示
- 代码入口：`com.yiye.AgentBackendApplication`
- 默认排除了 `PgVectorStoreAutoConfiguration`，如需自动装配请调整依赖与注解
- CORS 等跨域配置可见 `com.yiye.config`

### 常见问题
- 文档页访问不到？请确认应用已启动，端口为 8123，路径含 `/api`
- SSE 接口在浏览器中仅显示流式文本，建议使用 cURL 或前端 EventSource 进行调试
- 密钥与连接串请使用环境变量或外部化配置，不要提交到版本库

### 许可
本项目仅供学习与演示使用，商业使用请自查依赖与数据合规性