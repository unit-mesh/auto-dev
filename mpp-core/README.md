# MPP-Core: Kotlin Multiplatform 核心模块

## 📋 概述

`mpp-core` 是一个 Kotlin Multiplatform 模块，提供跨平台的核心功能，支持 JVM、JavaScript 和 WebAssembly 平台。该模块将原本依赖 IntelliJ IDEA 平台的代码重构为平台无关的实现，为项目的跨平台扩展奠定基础。

## ✅ 当前成果

### 多平台支持
- **JVM**: Java 17，用于 IntelliJ IDEA 插件和服务器端
- **JavaScript**: Browser + Node.js，用于 Web 应用
- **WebAssembly**: 高性能 Web 应用支持

### 核心功能
- **工具接口**: 统一的 Tool 抽象接口
- **平台抽象**: expect/actual 机制处理平台差异
- **YAML 支持**: 基于 kaml 的多平台 YAML 处理
- **序列化**: kotlinx.serialization 支持
- **协程**: kotlinx.coroutines 支持
- **完整测试**: 所有平台的单元测试覆盖

## 🏗️ 项目结构

```
mpp-core/
├── build.gradle.kts                    # 多平台构建配置
└── src/
    ├── commonMain/kotlin/cc/unitmesh/   # 平台无关代码
    │   ├── agent/                       # 工具接口 ✅
    │   │   ├── Tool.kt                  # 核心抽象接口
    │   │   ├── Platform.kt              # 平台检测
    │   │   └── Example.kt               # 示例实现
    │   └── yaml/                        # YAML 工具 ✅
    │       └── YamlUtils.kt             # 多平台 YAML 处理
    ├── commonTest/kotlin/               # 平台无关测试
    │   ├── cc/unitmesh/agent/ToolTest.kt
    │   └── cc/unitmesh/yaml/YamlUtilsTest.kt
    ├── jvmMain/kotlin/                  # JVM 特定实现
    │   └── cc/unitmesh/agent/Platform.jvm.kt
    ├── jsMain/kotlin/                   # JS 特定实现
    │   └── cc/unitmesh/agent/Platform.js.kt
    └── wasmJsMain/kotlin/               # WASM 特定实现
        └── cc/unitmesh/agent/Platform.wasmJs.kt
```

## 🚀 快速开始

### 构建和测试
```bash
# 构建所有平台
./gradlew :mpp-core:build

# 运行所有平台测试
./gradlew :mpp-core:allTests

# 平台特定构建
./gradlew :mpp-core:jvmJar        # JVM 平台
./gradlew :mpp-core:jsJar         # JavaScript 平台
./gradlew :mpp-core:wasmJsJar     # WebAssembly 平台
```

### 依赖配置
```kotlin
// 在其他模块中使用 mpp-core
dependencies {
    implementation(project(":mpp-core"))
}
```

## 💡 设计原则

- **平台无关性**: 只包含不依赖特定平台 API 的代码
- **向后兼容**: 保持与现有模块的兼容性
- **渐进式迁移**: 分阶段迁移，确保稳定性
- **类型安全**: 利用 Kotlin 类型系统确保跨平台一致性

## 📋 后续迁移计划

### 优先级 1: 数据模型 (下一步)
**目标**: 迁移核心数据结构，为业务逻辑奠定基础

**候选模块**:
- Agent 配置模型 (`CustomAgentConfig`, `CustomAgentState`)
- 交互类型 (`ChatActionType`, `ChatRole`)
- 基础工具模型 (`McpTool`, `McpToolCall`)

**预期收益**: 统一数据模型，支持跨平台序列化

### 优先级 2: 命令系统 (中期)
**目标**: 迁移命令处理逻辑

**候选模块**:
- 命令数据提供者 (`BuiltinCommand`, `CustomCommand`)
- 文本处理工具 (`PostCodeProcessor`)
- 上下文数据结构 (`SimpleClassStructure`)

**技术挑战**: 需要抽象化文件系统依赖

### 优先级 3: 处理器接口 (长期)
**目标**: 抽象化业务逻辑接口

**候选模块**:
- 语言处理器接口 (`LanguageProcessor`)
- 配置管理 (`McpChatConfig`)

**技术挑战**: 需要设计平台抽象层

## 🔧 技术实现

### 构建配置
```kotlin
// mpp-core/build.gradle.kts
kotlin {
    jvm { jvmTarget = "17" }
    js(IR) { browser(); nodejs() }
    wasmJs { browser(); nodejs() }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            implementation("com.charleskorn.kaml:kaml:0.61.0")
        }
    }
}
```

### 平台抽象机制
```kotlin
// commonMain/kotlin
expect object Platform {
    val name: String
    val isJvm: Boolean
    val isJs: Boolean
    val isWasm: Boolean
}

// 平台特定实现
// jvmMain/kotlin, jsMain/kotlin, wasmJsMain/kotlin
actual object Platform { /* 平台特定实现 */ }
```


## 📖 使用示例

### 基本使用
```kotlin
// 创建工具实例
val tool = ExampleTool()

// 创建配置
val config = tool.createConfig()

// 序列化
val json = tool.serializeConfig(config)

// 平台检测
when {
    Platform.isJvm -> println("Running on JVM")
    Platform.isJs -> println("Running on JavaScript")
    Platform.isWasm -> println("Running on WebAssembly")
}
```

### YAML 处理
```kotlin
// 解析 YAML
val data = YamlUtils.load(yamlContent)

// 序列化为 YAML
val yamlString = YamlUtils.dump(config, ConfigSerializer)
```

## 🧪 平台特定测试

### JavaScript 平台测试
- **文件**: `src/jsTest/kotlin/cc/unitmesh/yaml/JsYamlTest.kt`
- **覆盖**: 浏览器和 Node.js 环境的 YAML 处理
- **特性**: 性能测试、错误处理、JavaScript 特定场景

### WebAssembly 平台测试
- **文件**: `src/wasmJsTest/kotlin/cc/unitmesh/yaml/WasmYamlTest.kt`
- **覆盖**: WebAssembly 环境的 YAML 处理
- **特性**: 内存效率、高性能处理、WASM 特定优化

### 跨平台兼容性测试
- **文件**: `src/commonTest/kotlin/cc/unitmesh/yaml/CrossPlatformYamlTest.kt`
- **覆盖**: 所有平台的一致性验证
- **特性**: 平台检测、数据类型兼容性、复杂结构处理

## 🔄 迁移策略

### 1. 数据类迁移
- ✅ 保持 `@Serializable` 注解
- ✅ 移除 IntelliJ 特定类型 (如 `Icon`, `VirtualFile`)
- ✅ 使用平台无关的替代方案

### 2. 接口抽象化
- ✅ 将平台相关的参数抽象为通用类型
- ✅ 使用 `expect/actual` 机制处理平台差异
- ✅ 保持向后兼容性

### 3. 依赖管理
- ✅ `core` 模块依赖 `mpp-core`
- ✅ 扩展模块根据需要添加 `mpp-core` 依赖
- 📋 逐步减少对 `core` 的直接依赖

### 4. 测试策略
- ✅ 为 `mpp-core` 编写平台无关的单元测试
- ✅ 确保迁移后功能完整性
- ✅ 验证序列化/反序列化正确性

## ✅ 验证结果

### 构建验证
- ✅ 所有平台构建成功
- ✅ 生成正确的构建产物
  - `mpp-core-jvm.jar` - JVM 平台库
  - `mpp-core-js.klib` - JavaScript 平台库
  - `mpp-core-wasm-js.klib` - WebAssembly 平台库
  - `mpp-core-metadata.jar` - 元数据库

### 测试验证
- ✅ 所有平台的单元测试通过
- ✅ 序列化/反序列化功能验证
- ✅ 平台检测功能验证
- ✅ 跨平台兼容性验证

### 集成验证
- ✅ `core` 模块正常依赖 `mpp-core`
- ✅ `exts:devins-lang` 模块正常编译
- ✅ `exts:ext-database` 模块正常编译
- ✅ `exts:ext-git` 模块正常编译

## 🎯 技术优势

1. **✅ 平台扩展性**: 支持 Kotlin Multiplatform，已扩展到 Web 平台
2. **✅ 代码复用**: 核心业务逻辑在所有平台间共享
3. **✅ 架构清晰**: 明确分离平台相关和平台无关代码
4. **✅ 测试改进**: 核心逻辑可独立测试，提高测试覆盖率
5. **✅ 维护性**: 减少平台耦合，提高代码可维护性
6. **✅ 类型安全**: 编译时保证跨平台类型一致性
7. **✅ 性能优化**: 各平台使用最优的运行时

## 🚀 未来扩展

### 计划支持的平台
- **Native**: Kotlin/Native 支持 (Linux, macOS, Windows)
- **iOS**: iOS 应用支持
- **Android**: Android 应用支持

### 依赖关系图
```
mpp-core (多平台) ✅
├── core (JVM) - 依赖 mpp-core ✅
├── exts:devins-lang (JVM) - 依赖 mpp-core + core ✅
├── exts:ext-database (JVM) - 依赖 mpp-core + core ✅
└── exts:ext-git (JVM) - 依赖 mpp-core + core ✅
```
