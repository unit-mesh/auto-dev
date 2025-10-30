# 基于 Tool 系统的命令补全

## 概述

本文档描述了如何将现有的 `CommandCompletionProvider` 替换为基于 Tool 系统的新实现 `ToolBasedCommandCompletionProvider`。

## 背景

原有的 `CommandCompletionProvider` 使用硬编码的命令列表来提供补全功能。新的实现通过集成 Tool 系统，可以动态获取所有可用的工具，提供更灵活和可扩展的补全功能。

## 主要变更

### 1. 新增 `ToolBasedCommandCompletionProvider`

**位置**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/completion/CompletionProvider.kt`

**功能**:
- 从 `ToolRegistry` 动态获取所有可用工具
- 为每个工具生成补全项，包含名称、描述和图标
- 支持模糊匹配和排序
- 提供智能插入处理器

**特性**:
- 🔧 动态工具发现：自动发现所有注册的工具
- 🎯 智能匹配：支持前缀匹配、包含匹配和模糊匹配
- 🎨 图标支持：为不同类型的工具提供相应图标
- ⚡ 高性能：基于高效的工具注册表实现

### 2. 支持的工具类型

当前支持以下内置工具：

| 工具名称 | 图标 | 描述 |
|---------|------|------|
| `read-file` | 📄 | 读取文件内容，支持行范围 |
| `write-file` | ✏️ | 写入文件内容，支持目录创建 |
| `grep` | 🔍 | 文本搜索，支持正则表达式 |
| `glob` | 🌐 | 文件模式匹配，支持通配符 |
| `shell` | 💻 | 执行 Shell 命令（仅 JVM 平台） |

### 3. 使用示例

#### 基本用法

```kotlin
// 创建基于工具的补全提供者
val provider = ToolBasedCommandCompletionProvider()

// 创建补全上下文
val context = CompletionContext(
    fullText = "/read",
    cursorPosition = 5,
    triggerType = CompletionTriggerType.COMMAND,
    triggerOffset = 0,
    queryText = "read"
)

// 获取补全项
val completions = provider.getCompletions(context)
```

#### 集成到补全管理器

```kotlin
class CompletionManager {
    private val toolBasedCommandProvider = ToolBasedCommandCompletionProvider()
    
    private val providers = mapOf(
        CompletionTriggerType.COMMAND to toolBasedCommandProvider
    )
    
    fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val provider = providers[context.triggerType] ?: return emptyList()
        return provider.getCompletions(context)
    }
}
```

### 4. 补全触发器

新增了 `CompletionTrigger` 工具类，用于分析文本并确定补全触发类型：

```kotlin
val context = CompletionTrigger.analyzeTrigger("/read-file", 10)
// 返回: CompletionContext(triggerType=COMMAND, queryText="read-file")
```

支持的触发类型：
- `/` - 命令补全
- `@` - Agent 补全
- `$` - 变量补全
- `:` - 命令值补全（如 `/file:` 后的路径补全）

### 5. 跨平台支持

实现支持所有 Kotlin Multiplatform 目标：

- ✅ **JVM**: 完整功能，包括 Shell 工具
- ✅ **JavaScript**: 基础功能，不包括 Shell 工具
- ✅ **WebAssembly**: 基础功能，不包括 Shell 工具

### 6. 测试覆盖

新增了完整的测试套件：

**测试文件**:
- `ToolBasedCommandCompletionProviderTest.kt` - 核心功能测试
- `CompletionExampleTest.kt` - 集成和示例测试

**测试覆盖**:
- 工具发现和补全生成
- 查询匹配和排序
- 插入处理器功能
- 跨平台兼容性
- 边界条件处理

## 迁移指南

### 从旧的 CommandCompletionProvider 迁移

1. **替换提供者实例**:
   ```kotlin
   // 旧代码
   val provider = CommandCompletionProvider()
   
   // 新代码
   val provider = ToolBasedCommandCompletionProvider()
   ```

2. **更新依赖**:
   确保项目包含 Tool 系统的相关依赖。

3. **测试验证**:
   运行测试确保迁移成功：
   ```bash
   ./gradlew :mpp-core:allTests
   ```

### 扩展新工具

要添加新的工具到补全系统：

1. **实现工具接口**:
   ```kotlin
   class MyCustomTool : BaseExecutableTool<MyParams, ToolResult>() {
       override val name = "my-tool"
       override val description = "My custom tool description"
       // ... 实现其他方法
   }
   ```

2. **注册工具**:
   ```kotlin
   val registry = GlobalToolRegistry.getInstance()
   registry.registerTool(MyCustomTool())
   ```

3. **添加图标映射**:
   在 `ToolBasedCommandCompletionProvider.getToolIcon()` 中添加图标映射。

## 性能优化

- **延迟加载**: 工具注册表采用延迟初始化
- **缓存机制**: 补全结果基于工具注册表的缓存
- **高效匹配**: 使用优化的字符串匹配算法
- **内存优化**: 避免不必要的对象创建

## 未来扩展

计划中的功能增强：

1. **动态工具发现**: 支持运行时动态加载工具
2. **上下文感知补全**: 基于当前文件类型提供相关工具
3. **工具参数补全**: 为工具参数提供智能补全
4. **自定义图标**: 支持工具自定义图标
5. **补全缓存**: 实现更高级的缓存策略

## 总结

新的 `ToolBasedCommandCompletionProvider` 提供了：

- 🚀 **更好的可扩展性**: 基于工具注册表的动态发现
- 🎯 **更智能的匹配**: 支持多种匹配策略
- 🌐 **跨平台支持**: 在所有 KMP 目标上运行
- 🧪 **完整测试**: 全面的测试覆盖
- 📚 **清晰文档**: 详细的使用说明和示例

这个实现为未来的功能扩展奠定了坚实的基础，同时保持了与现有系统的兼容性。
