# Workspace 系统设计与实现

## 概述

本文档描述了在 `@mpp-core` 中引入的 Workspace 概念，它类似于 IntelliJ IDEA 中的 Project 或 Gemini CLI 中的 workspaceContext，用于统一管理项目路径和相关服务。

## 设计目标

1. **全局状态管理**: 提供单例模式管理当前工作空间
2. **服务集成**: 集成 ProjectFileSystem、CompletionManager 等服务
3. **状态通知**: 支持工作空间状态变化的响应式通知
4. **跨平台支持**: 在所有 KMP 目标平台上工作
5. **向后兼容**: 保持与现有代码的兼容性

## 核心组件

### 1. Workspace 接口

**位置**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/workspace/Workspace.kt`

```kotlin
interface Workspace {
    val name: String                    // 工作空间名称
    val rootPath: String?              // 工作空间根路径
    val fileSystem: ProjectFileSystem  // 文件系统服务
    val completionManager: CompletionManager // 补全管理器
    val stateFlow: StateFlow<WorkspaceState> // 状态流
    
    fun isInitialized(): Boolean       // 检查是否已初始化
    suspend fun refresh()              // 刷新工作空间
    suspend fun close()                // 关闭工作空间
}
```

### 2. WorkspaceManager 单例

全局工作空间管理器，提供以下功能：

- **工作空间切换**: `openWorkspace(name, rootPath)`
- **空工作空间**: `openEmptyWorkspace(name)`
- **状态监听**: `workspaceFlow: StateFlow<Workspace?>`
- **便捷访问**: `getCurrentOrEmpty(): Workspace`

### 3. WorkspaceState 状态管理

```kotlin
data class WorkspaceState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshTime: Long = 0L
)
```

## 主要特性

### 🔄 响应式状态管理

```kotlin
// 监听工作空间变化
val workspaceState by WorkspaceManager.workspaceFlow.collectAsState()

// 监听工作空间内部状态
val internalState by workspace.stateFlow.collectAsState()
```

### 🛠️ 服务集成

每个工作空间自动提供：
- **ProjectFileSystem**: 文件系统操作
- **CompletionManager**: 智能补全服务
- **SpecKit 命令**: 项目特定的命令

### 🌐 跨平台兼容

- ✅ **JVM**: 完整功能支持
- ✅ **JavaScript**: 基础功能支持
- ✅ **WebAssembly**: 基础功能支持

### 📁 智能文件补全

FilePathCompletionProvider 现在支持：
- **静态路径**: 常用项目路径（src/main/kotlin/ 等）
- **动态搜索**: 基于工作空间的实时文件搜索
- **边输入边搜索**: 支持模糊匹配和实时过滤

## 使用示例

### 基本用法

```kotlin
// 打开工作空间
val workspace = WorkspaceManager.openWorkspace("My Project", "/path/to/project")

// 获取服务
val fileSystem = workspace.fileSystem
val completionManager = workspace.completionManager

// 监听状态变化
workspace.stateFlow.collect { state ->
    if (state.isInitialized) {
        println("工作空间已就绪")
    }
}
```

### UI 集成

```kotlin
@Composable
fun MyComponent() {
    // 获取当前工作空间
    var currentWorkspace by remember { mutableStateOf(WorkspaceManager.getCurrentOrEmpty()) }
    
    // 监听工作空间变化
    val workspaceState by WorkspaceManager.workspaceFlow.collectAsState()
    
    LaunchedEffect(workspaceState) {
        workspaceState?.let { workspace ->
            currentWorkspace = workspace
        }
    }
    
    // 使用工作空间服务
    DevInEditorInput(
        completionManager = currentWorkspace.completionManager,
        // ... 其他参数
    )
}
```

### 工作空间切换

```kotlin
// 选择项目目录
fun selectProjectDirectory() {
    val fileChooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    }
    
    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val selectedPath = fileChooser.selectedFile.absolutePath
        val projectName = File(selectedPath).name
        
        scope.launch {
            WorkspaceManager.openWorkspace(projectName, selectedPath)
        }
    }
}
```

## 迁移指南

### 从直接使用 ProjectFileSystem 迁移

**之前**:
```kotlin
var projectPath by remember { mutableStateOf<String?>("/path") }
var fileSystem by remember { mutableStateOf(DefaultFileSystem(projectPath)) }
var completionManager by remember { mutableStateOf(CompletionManager(fileSystem)) }
```

**现在**:
```kotlin
var currentWorkspace by remember { mutableStateOf(WorkspaceManager.getCurrentOrEmpty()) }
val workspaceState by WorkspaceManager.workspaceFlow.collectAsState()

LaunchedEffect(workspaceState) {
    workspaceState?.let { workspace ->
        currentWorkspace = workspace
    }
}

// 使用
val fileSystem = currentWorkspace.fileSystem
val completionManager = currentWorkspace.completionManager
```

### 更新补全提供者

FilePathCompletionProvider 现在自动使用 WorkspaceManager：

```kotlin
class FilePathCompletionProvider : CompletionProvider {
    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val workspace = WorkspaceManager.getCurrentOrEmpty()
        // 自动获取当前工作空间的文件系统
        return getDynamicCompletions(context.queryText, workspace)
    }
}
```

## 测试覆盖

### 测试文件
- `WorkspaceTest.kt` - 核心功能测试
- 涵盖所有主要功能和边界条件
- 跨平台兼容性测试

### 测试场景
- ✅ 工作空间创建和初始化
- ✅ 状态管理和通知
- ✅ 服务集成
- ✅ 工作空间切换
- ✅ 错误处理
- ✅ 跨平台兼容性

## 性能优化

### 延迟初始化
- 服务按需创建，避免不必要的资源消耗
- 文件系统操作采用异步模式

### 状态缓存
- 工作空间状态缓存，减少重复计算
- 补全结果基于工作空间缓存

### 内存管理
- 工作空间关闭时自动清理资源
- 避免内存泄漏

## 扩展点

### 自定义工作空间类型
```kotlin
class CustomWorkspace(name: String, rootPath: String?) : Workspace {
    // 自定义实现
}
```

### 工作空间插件
```kotlin
interface WorkspacePlugin {
    fun onWorkspaceOpened(workspace: Workspace)
    fun onWorkspaceClosed(workspace: Workspace)
}
```

### 自定义服务
```kotlin
class MyWorkspaceService(private val workspace: Workspace) {
    // 自定义服务实现
}
```

## 未来规划

1. **工作空间模板**: 支持不同类型的项目模板
2. **多工作空间**: 同时管理多个工作空间
3. **工作空间配置**: 持久化工作空间设置
4. **插件系统**: 支持第三方工作空间扩展
5. **远程工作空间**: 支持远程项目访问

## 总结

Workspace 系统提供了：

- 🏗️ **统一架构**: 集中管理项目相关的所有服务
- 🔄 **响应式设计**: 支持状态变化的实时响应
- 🌐 **跨平台支持**: 在所有 KMP 目标上一致工作
- 🧪 **完整测试**: 全面的测试覆盖
- 📚 **清晰文档**: 详细的使用指南和示例

这个设计为项目的未来扩展奠定了坚实的基础，同时保持了与现有代码的完全兼容性。
