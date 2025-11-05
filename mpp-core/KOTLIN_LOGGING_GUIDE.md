# Kotlin Logging 使用指南

本文档介绍如何在 mpp-core 中使用 kotlin-logging 替代 println 进行日志记录。

## 概述

我们已经成功引入了 kotlin-logging v7.0.13 来替换项目中的 println 调用，提供了更专业的日志管理功能。

## 快速开始

### 1. 获取 Logger

```kotlin
import cc.unitmesh.agent.logging.getLogger

class MyClass {
    private val logger = getLogger("MyClass")
    
    fun doSomething() {
        logger.info { "开始执行操作" }
        logger.debug { "详细调试信息" }
        logger.warn { "警告信息" }
        logger.error { "错误信息" }
    }
}
```

### 2. 使用扩展函数

```kotlin
import cc.unitmesh.agent.logging.getLogger

class MyClass {
    private val logger = this.getLogger() // 自动使用类名
    
    fun doSomething() {
        logger.info { "使用扩展函数获取 logger" }
    }
}
```

## 配置

### 1. 日志级别配置

在 ConfigManager.ts 中配置全局日志级别：

```typescript
// 在配置文件中添加
{
  "globalLogLevel": "DEBUG", // TRACE, DEBUG, INFO, WARN, ERROR
  "configs": [
    {
      "name": "default",
      "logLevel": "INFO", // 优先级高于全局设置
      // ... 其他配置
    }
  ]
}
```

### 2. 程序化配置

```kotlin
import cc.unitmesh.agent.logging.*
import io.github.oshai.kotlinlogging.Level

// 初始化日志系统
LoggingInitializer.initialize(LoggingConfig.debug())

// 动态更改日志级别
GlobalLoggingConfig.updateLogLevel(Level.WARN)

// 获取当前配置
val config = GlobalLoggingConfig.config
println("当前日志级别: ${config.logLevel}")
```

## 日志输出

### 文件输出

日志文件自动输出到：`~/.autodev/logs/${platform}-${date}.log`

- **JVM**: `~/.autodev/logs/jvm-2025-11-05.log`
- **JS**: `~/.autodev/logs/js-2025-11-05.log`
- **Android**: `/data/data/.autodev/logs/android-2025-11-05.log`

### 控制台输出

所有平台都支持控制台输出，可通过配置控制开关。

## 最佳实践

### 1. 使用懒加载消息

```kotlin
// ✅ 推荐：使用 lambda 表达式
logger.debug { "用户 ID: $userId, 操作: $operation" }

// ❌ 不推荐：直接字符串拼接
logger.debug("用户 ID: $userId, 操作: $operation")
```

### 2. 异常日志记录

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    logger.error(e) { "操作失败: ${e.message}" }
}
```

### 3. 结构化日志

```kotlin
logger.info { 
    "API调用完成 - " +
    "endpoint: $endpoint, " +
    "status: $status, " +
    "duration: ${duration}ms"
}
```

## 迁移指南

### 替换 println

```kotlin
// 旧代码
println("调试信息: $value")
println("错误: ${error.message}")

// 新代码
private val logger = getLogger("ClassName")

logger.debug { "调试信息: $value" }
logger.error { "错误: ${error.message}" }
```

### CompilerLogger 已更新

CompilerLogger 已经自动使用 kotlin-logging，无需手动修改。

## 示例

查看 `LoggingExample.kt` 了解完整的使用示例：

```kotlin
// 演示不同日志级别
LoggingExample.demonstrateLogging()

// 演示配置变更
LoggingExample.demonstrateConfigurationChanges()
```

## 测试

运行日志相关测试：

```bash
./gradlew :mpp-core:jvmTest --tests "*LoggingTest*"
```

## 注意事项

1. **性能**: 使用 lambda 表达式 `{ }` 可以避免不必要的字符串拼接
2. **线程安全**: kotlin-logging 是线程安全的
3. **平台差异**: 不同平台的日志后端可能有所不同，但 API 保持一致
4. **文件权限**: 确保应用有权限写入日志目录

## 故障排除

### 日志不显示

1. 检查日志级别设置
2. 确认 LoggingInitializer 已初始化
3. 验证平台特定的日志后端配置

### 文件写入失败

1. 检查目录权限
2. 确认磁盘空间充足
3. 验证路径格式正确

## 更多信息

- [kotlin-logging 官方文档](https://github.com/oshai/kotlin-logging)
- [多平台支持文档](https://github.com/oshai/kotlin-logging/wiki/Multiplatform-support)
