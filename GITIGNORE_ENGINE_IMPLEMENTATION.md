# 高性能双引擎 Gitignore 解析器实现

## 概述

本实现为 AutoDev 项目添加了一个高性能的双引擎 Gitignore 解析器，用于替换现有的 gitignore 相关逻辑。该解决方案提供了自定义高性能引擎和第三方库引擎的双重保障。

## 架构设计

### 核心组件

1. **IgnoreEngine** - 忽略引擎接口
2. **IgnoreRule** - 忽略规则接口
3. **GitIgnoreFlagWrapper** - 双引擎包装器
4. **HomeSpunIgnoreEngine** - 自定义高性能引擎
5. **BasjesIgnoreEngine** - 第三方库引擎
6. **IgnoreEngineFactory** - 工厂类

### 支持组件

- **PatternConverter** - 模式转换器（gitignore 模式到正则表达式）
- **IgnorePatternCache** - 模式缓存
- **ThreadSafeMatcher** - 线程安全匹配器
- **HomeSpunIgnoreRule** - 自定义规则实现
- **GitIgnoreUtil** - 工具类

## 功能特性

### 1. 双引擎架构
- **主引擎**: HomeSpunIgnoreEngine（自定义高性能实现）
- **备用引擎**: BasjesIgnoreEngine（基于 nl.basjes.gitignore 库）
- **动态切换**: 通过功能开关 `enableHomeSpunGitIgnore` 控制

### 2. 高性能优化
- **预编译正则表达式**: 避免重复编译
- **并发缓存**: 使用 ConcurrentHashMap 缓存编译后的模式
- **线程安全**: 所有组件都是线程安全的
- **错误恢复**: 主引擎失败时自动切换到备用引擎

### 3. 完整的 Gitignore 支持
- 基本通配符（`*`, `?`）
- 双星通配符（`**`）
- 目录模式（以 `/` 结尾）
- 否定模式（以 `!` 开头）
- 根路径模式（以 `/` 开头）
- 注释和空行处理

## 文件结构

```
core/src/main/kotlin/cc/unitmesh/devti/vcs/gitignore/
├── IgnoreEngine.kt                    # 核心接口
├── IgnoreRule.kt                      # 规则接口
├── GitIgnoreFlagWrapper.kt            # 双引擎包装器
├── HomeSpunIgnoreEngine.kt            # 自定义引擎
├── BasjesIgnoreEngine.kt              # 第三方库引擎
├── IgnoreEngineFactory.kt             # 工厂类
├── PatternConverter.kt                # 模式转换器
├── IgnorePatternCache.kt              # 模式缓存
├── ThreadSafeMatcher.kt               # 线程安全匹配器
├── HomeSpunIgnoreRule.kt              # 自定义规则实现
├── GitIgnoreUtil.kt                   # 工具类
└── InvalidGitIgnorePatternException.kt # 异常类
```

## 集成点

### 1. 设置配置
在 `AutoDevCoderSettingService` 中添加了功能开关：
```kotlin
var enableHomeSpunGitIgnore by property(true)
```

### 2. 现有代码更新
更新了以下文件以使用新的 gitignore 引擎：
- `ProjectFileUtil.kt`
- `DirInsCommand.kt`
- `WorkspaceFileSearchPopup.kt`
- `LocalSearchInsCommand.kt`

### 3. 依赖管理
添加了第三方库依赖：
```kotlin
implementation("nl.basjes.gitignore:gitignore-reader:1.6.0")
```

## 使用方法

### 基本使用
```kotlin
// 通过工厂创建引擎
val engine = IgnoreEngineFactory.createEngine(IgnoreEngineFactory.EngineType.HOMESPUN)

// 加载 gitignore 内容
engine.loadFromContent(gitIgnoreContent)

// 检查文件是否被忽略
val isIgnored = engine.isIgnored("path/to/file.txt")
```

### 项目集成使用
```kotlin
// 使用工具类（推荐）
val isIgnored = GitIgnoreUtil.isIgnored(project, virtualFile)

// 或者使用文件路径
val isIgnored = GitIgnoreUtil.isIgnored(project, "src/main/App.java")
```

## 测试

实现了全面的测试套件：
- `IgnoreEngineTest` - 引擎功能测试
- `PatternConverterTest` - 模式转换测试
- `GitIgnoreFlagWrapperTest` - 双引擎包装器测试

## 性能优势

1. **预编译模式**: 避免运行时重复编译正则表达式
2. **缓存机制**: 编译后的模式被缓存以供重用
3. **并发优化**: 使用线程安全的数据结构
4. **错误恢复**: 主引擎失败时的快速切换机制

## 配置选项

用户可以通过 AutoDev 设置面板控制：
- 启用/禁用自定义高性能引擎
- 查看引擎统计信息
- 重新加载 gitignore 规则

## 向后兼容性

- 完全向后兼容现有的 gitignore 功能
- 无需修改现有的 .gitignore 文件
- 平滑的功能切换，无需重启 IDE

## 故障排除

如果遇到问题：
1. 检查功能开关设置
2. 查看引擎统计信息
3. 尝试切换到备用引擎
4. 检查 gitignore 文件语法

## 未来扩展

- 支持更多的忽略文件格式
- 添加性能监控和分析
- 实现更高级的缓存策略
- 支持自定义忽略规则
