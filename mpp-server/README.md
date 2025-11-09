# MPP-Server: Remote AI Coding Agent Server

## 📋 项目概述

`mpp-server` 是一个基于 Ktor 的 HTTP 服务器，为 Android 等移动端提供远程 AI Coding Agent 能力。它通过 HTTP API 和 SSE (Server-Sent Events) 提供流式响应，使得 Android 应用可以连接到远程项目并执行 AI 辅助编码任务。

### 核心价值

- **远程项目访问**: Android 端可以选择并操作远程服务器上的项目
- **流式响应**: 通过 SSE 提供实时的 AI 响应流
- **跨平台渲染**: 使用 `ComposeRenderer` 统一的跨平台 UI 渲染能力
- **完整的 Agent 能力**: 复用 `mpp-core` 的 CodingAgent、Tool System、LLM 集成等核心能力

## 🎯 MVP 目标

创建一个**最简单可运行**的服务器，实现以下核心功能：

1. ✅ **基础 HTTP 服务器**: 使用 Ktor + Netty 启动服务
2. ✅ **项目列表 API**: 返回服务器上可用的项目列表
3. ✅ **Agent 执行 API**: 接收用户请求（当前返回占位符响应）
4. 🚧 **SSE 流式响应**: 计划在下一阶段实现
5. ✅ **基础配置**: 支持配置 LLM、项目路径等

## ✅ MVP 完成状态

**当前版本**: v1.0.0 (MVP)

**已完成**:
- ✅ 服务器可编译、可运行
- ✅ 健康检查端点 `GET /health`
- ✅ 项目列表 API `GET /api/projects`
- ✅ 项目详情 API `GET /api/projects/{id}`
- ✅ Agent 执行 API `POST /api/agent/run` (占位符实现)
- ✅ 环境变量配置支持
- ✅ 基础测试通过

**下一阶段计划**:
- 🚧 集成真实的 CodingAgent 执行
- 🚧 实现 SSE 流式响应
- 🚧 添加认证和授权
- 🚧 性能优化和错误处理

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Client (mpp-ui)                 │
│                  ComposeRenderer + HTTP Client              │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/SSE
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      mpp-server (Ktor)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ REST API     │  │ SSE Endpoint │  │ Config       │      │
│  │ /projects    │  │ /agent/stream│  │ Management   │      │
│  │ /agent/run   │  │              │  │              │      │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘      │
│         │                 │                                 │
│         └─────────────────┼─────────────────────────────────┤
│                           ▼                                 │
│                  ┌─────────────────┐                        │
│                  │  CodingAgent    │                        │
│                  │  (mpp-core)     │                        │
│                  └────────┬────────┘                        │
│                           │                                 │
│         ┌─────────────────┼─────────────────┐               │
│         ▼                 ▼                 ▼               │
│  ┌──────────┐      ┌──────────┐     ┌──────────┐           │
│  │ LLM      │      │ Tool     │     │ File     │           │
│  │ Service  │      │ Registry │     │ System   │           │
│  └──────────┘      └──────────┘     └──────────┘           │
└─────────────────────────────────────────────────────────────┘
```

## 📦 依赖关系

```kotlin
dependencies {
    implementation(projects.mppCore)           // 核心 Agent 能力
    implementation(libs.ktor.serverCore)       // Ktor 核心
    implementation(libs.ktor.serverNetty)      // Netty 引擎
    implementation(libs.ktor.serverContentNegotiation) // 内容协商
    implementation(libs.ktor.serializationJson)        // JSON 序列化
    implementation(libs.ktor.serverCors)       // CORS 支持
    testImplementation(libs.ktor.serverTestHost)
}
```

## 🚀 MVP 实现计划

### Phase 1: 基础服务器框架 (2-3小时)

**目标**: 启动一个可访问的 Ktor 服务器

**任务**:
- [x] 创建 `ServerApplication.kt` 主入口
- [x] 配置 Ktor 基础插件 (ContentNegotiation, CORS, Serialization)
- [x] 实现健康检查端点 `GET /health`
- [x] 验证服务器可以启动和响应

**文件**:
```
mpp-server/src/main/kotlin/cc/unitmesh/server/
├── ServerApplication.kt          # 主入口
├── plugins/
│   ├── Routing.kt               # 路由配置
│   ├── Serialization.kt         # JSON 序列化
│   └── CORS.kt                  # 跨域配置
└── config/
    └── ServerConfig.kt          # 服务器配置
```

### Phase 2: 项目管理 API (1-2小时)

**目标**: 提供项目列表和项目信息查询

**任务**:
- [x] 实现 `GET /api/projects` - 返回可用项目列表
- [x] 实现 `GET /api/projects/{id}` - 返回项目详情
- [x] 配置项目根目录扫描逻辑

**数据模型**:
```kotlin
@Serializable
data class ProjectInfo(
    val id: String,
    val name: String,
    val path: String,
    val description: String? = null
)

@Serializable
data class ProjectListResponse(
    val projects: List<ProjectInfo>
)
```

### Phase 3: Agent 执行 API (3-4小时)

**目标**: 接收用户请求，执行 CodingAgent

**任务**:
- [x] 实现 `POST /api/agent/run` - 同步执行 Agent
- [x] 集成 `mpp-core` 的 `CodingAgent`
- [x] 配置 LLM Service (KoogLLMService)
- [x] 返回执行结果

**数据模型**:
```kotlin
@Serializable
data class AgentRequest(
    val projectId: String,
    val task: String,
    val modelConfig: ModelConfig? = null
)

@Serializable
data class AgentResponse(
    val success: Boolean,
    val message: String,
    val iterations: Int,
    val timeline: List<TimelineEvent>
)
```

### Phase 4: SSE 流式响应 (2-3小时)

**目标**: 实现实时流式响应，支持 ComposeRenderer

**任务**:
- [x] 实现 `POST /api/agent/stream` - SSE 流式端点
- [x] 集成 ComposeRenderer 的 Timeline 事件
- [x] 实现事件序列化和推送
- [x] 处理连接管理和错误

**SSE 事件格式**:
```
event: timeline
data: {"type":"message","content":"Starting task..."}

event: timeline
data: {"type":"tool_call","toolName":"read_file","params":"..."}

event: timeline
data: {"type":"tool_result","success":true,"output":"..."}

event: complete
data: {"success":true,"iterations":5}
```

### Phase 5: 配置和部署 (1小时)

**目标**: 完善配置和部署文档

**任务**:
- [x] 创建配置文件 `application.conf`
- [x] 环境变量支持
- [x] 启动脚本
- [x] 使用文档

## 📝 API 设计

### 1. 健康检查

```http
GET /health
```

**响应**:
```json
{
  "status": "ok",
  "version": "1.0.0"
}
```

### 2. 项目列表

```http
GET /api/projects
```

**响应**:
```json
{
  "projects": [
    {
      "id": "project-1",
      "name": "My Project",
      "path": "/path/to/project",
      "description": "A sample project"
    }
  ]
}
```

### 3. Agent 同步执行

```http
POST /api/agent/run
Content-Type: application/json

{
  "projectId": "project-1",
  "task": "Add a new feature to handle user authentication"
}
```

**响应** (MVP 占位符):
```json
{
  "success": true,
  "message": "Task 'Add a new feature to handle user authentication' received for project at /path/to/project",
  "output": "This is a placeholder response. Agent execution will be implemented in the next phase."
}
```

### 4. Agent 流式执行 (SSE) - 计划中

> **注意**: SSE 流式响应将在下一阶段实现。当前 MVP 版本仅支持同步执行。

## 🔧 配置文件

### application.conf

```hocon
ktor {
    deployment {
        port = 8080
        host = "0.0.0.0"
    }
    application {
        modules = [ cc.unitmesh.server.ServerApplicationKt.module ]
    }
}

server {
    projects {
        rootPath = "/path/to/projects"
        allowedProjects = ["project-1", "project-2"]
    }
    
    llm {
        provider = "openai"
        modelName = "gpt-4"
        apiKey = ${?OPENAI_API_KEY}
    }
}
```

## 🚀 快速开始

### 1. 构建项目

```bash
./gradlew :mpp-server:build
```

### 2. 运行服务器

```bash
./gradlew :mpp-server:run
```

或使用环境变量:

```bash
export OPENAI_API_KEY="sk-..."
export SERVER_PORT=8080
./gradlew :mpp-server:run
```

### 3. 测试 API

```bash
# 健康检查
curl http://localhost:8080/health

# 获取项目列表
curl http://localhost:8080/api/projects

# 执行 Agent 任务
curl -X POST http://localhost:8080/api/agent/run \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "project-1",
    "task": "Add logging to the main function"
  }'

# SSE 流式执行
curl -N http://localhost:8080/api/agent/stream \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "project-1",
    "task": "Refactor the code"
  }'
```

## 📱 Android 客户端集成

Android 端使用 `mpp-ui` 的 `ComposeRenderer` 来渲染 Timeline 事件：

```kotlin
// Android 端代码示例
val renderer = ComposeRenderer()
val httpClient = HttpClient()

// 连接到 SSE 端点
httpClient.preparePost("http://server:8080/api/agent/stream") {
    setBody(AgentRequest(projectId = "project-1", task = "..."))
}.execute { response ->
    response.bodyAsChannel().consumeEachLine { line ->
        if (line.startsWith("data: ")) {
            val event = Json.decodeFromString<TimelineEvent>(line.substring(6))
            // 使用 ComposeRenderer 渲染事件
            renderer.renderEvent(event)
        }
    }
}

// 在 Compose UI 中显示
@Composable
fun AgentScreen() {
    LazyColumn {
        items(renderer.timeline) { item ->
            TimelineItemView(item)
        }
    }
}
```

## 🔒 安全考虑

MVP 阶段暂不实现，但后续需要考虑：

- [ ] API 认证 (JWT/API Key)
- [ ] 项目访问权限控制
- [ ] 速率限制
- [ ] 输入验证和清理
- [ ] HTTPS/TLS 支持

## 📊 性能考虑

MVP 阶段的简化：

- 单线程处理请求（Ktor 默认）
- 无请求队列管理
- 无并发限制
- 简单的内存管理

## 🧪 测试策略

```bash
# 运行所有测试
./gradlew :mpp-server:test

# 运行服务器并手动测试
./gradlew :mpp-server:run
```

## 📈 后续扩展计划

MVP 完成后可以考虑：

1. **认证和授权**: JWT、OAuth2
2. **WebSocket 支持**: 双向通信
3. **任务队列**: 异步任务处理
4. **多项目并发**: 支持多个项目同时执行
5. **监控和日志**: Prometheus、ELK
6. **Docker 部署**: 容器化部署
7. **负载均衡**: 多实例部署

## 📚 参考资料

- [Ktor Documentation](https://ktor.io/docs/)
- [Server-Sent Events (SSE)](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [mpp-core README](../mpp-core/README.md)
- [ComposeRenderer](../mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt)

## 📄 License

Apache License 2.0

