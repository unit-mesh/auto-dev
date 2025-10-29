# DevIn Language Parser for JavaScript

这是 DevIn 语言在 JavaScript 平台上的解析器实现，基于 Kotlin Multiplatform 项目。

## 概述

DevIn 语言是一种用于 AI 代理交互的领域特定语言（DSL），支持：

- **Front Matter**：YAML 风格的元数据
- **Agent 块**：`@agent` 语法
- **Command 块**：`/command` 语法  
- **Variable 块**：`$variable` 语法
- **Code 块**：```language 代码块
- **Expression 块**：`#expression` 语法
- **模板语法**：条件语句和循环

## 架构

### 核心组件

1. **DevInLexer**：词法分析器，基于 Moo.js
2. **DevInParser**：语法分析器，使用简化的递归下降解析
3. **DevInLanguage**：高级 API 接口
4. **DevInUtils**：实用工具函数

### 技术栈

- **Kotlin Multiplatform**：跨平台代码共享
- **Moo.js**：高性能词法分析器
- **JavaScript/TypeScript**：运行时环境

## 使用方法

### 基本解析

```kotlin
// 创建 DevIn 语言实例
val devIn = DevInLanguage.create()

// 解析 DevIn 源码
val source = """
    ---
    name: example
    agent: myAgent
    ---
    
    Hello @myAgent, please /read: file.txt
"""

val result = devIn.parse(source)
if (result.errors.isEmpty()) {
    println("解析成功！")
    println("AST 类型: ${result.ast?.type}")
} else {
    println("解析错误:")
    result.errors.forEach { error ->
        println("- ${error.message} at line ${error.line}")
    }
}
```

### 词法分析

```kotlin
val devIn = DevInLanguage.create()
val tokens = devIn.tokenize("@agent /command \$variable")

tokens.forEach { token ->
    println("${token.type}: '${token.value}' at line ${token.line}")
}
```

### 语法验证

```kotlin
val devIn = DevInLanguage.create()
val isValid = devIn.isValid("@agent /command")
val errors = devIn.validate("@agent /command")

if (isValid) {
    println("语法正确")
} else {
    println("语法错误: $errors")
}
```

### 高级处理

```kotlin
val config = DevInConfig(
    enableFrontMatter = true,
    enableCodeBlocks = true,
    enableVariables = true,
    enableAgents = true,
    strictMode = false
)

val processor = DevInProcessor(config)
val result = processor.process(source)

println("Front Matter: ${result.frontMatter}")
println("Code Blocks: ${result.codeBlocks}")
println("Variables: ${result.variables}")
println("Agents: ${result.agents}")
```

## DevIn 语法示例

### Front Matter

```yaml
---
name: example
version: 1.0
agent: myAgent
enabled: true
functions:
  - process
  - analyze
---
```

### Agent 块

```
@myAgent
@"agent with spaces"
```

### Command 块

```
/read: file.txt
/write: output.txt #L1-L10
/execute: script.sh
```

### Variable 块

```
$data
$config
${complex.variable}
```

### Code 块

```javascript
console.log("Hello World");
const result = process(data);
```

```python
print("Hello World")
result = process(data)
```

### Expression 块

```
#if (condition == true)
    Process data
#else
    Handle error
#end

#for item in items
    Process ${item}
#end
```

### 复合示例

```
---
name: complex-example
agent: dataProcessor
version: 1.0
---

# Data Processing Workflow

Hello @dataProcessor, please analyze the following data:

```json
{
  "users": [
    {"id": 1, "name": "Alice"},
    {"id": 2, "name": "Bob"}
  ]
}
```

Then /read: config.json and process $data with $config.

#if (data.users.length > 0)
    /execute: process_users.py
    
    For each user:
    ```python
    for user in data['users']:
        print(f"Processing user: {user['name']}")
    ```
#else
    /log: "No users to process"
#end

// Processing complete
[Status] All users processed successfully
```

## AST 结构

解析器生成的 AST 包含以下节点类型：

- `FILE`：根节点
- `FRONT_MATTER_HEADER`：Front Matter 块
- `AGENT_BLOCK`：Agent 声明
- `COMMAND_BLOCK`：Command 声明
- `VARIABLE_BLOCK`：Variable 声明
- `CODE_BLOCK`：代码块
- `EXPRESSION_BLOCK`：表达式块
- `TEXT_SEGMENT`：文本内容
- `COMMENT`：注释

每个节点包含：
- `type`：节点类型
- `children`：子节点列表
- `startOffset`、`endOffset`：位置信息
- `line`、`column`：行列信息

## 错误处理

解析器提供详细的错误信息：

```kotlin
data class DevInParseError(
    val message: String,
    val line: Int,
    val column: Int,
    val offset: Int,
    val token: DevInToken?
)
```

## 性能特性

- **高性能词法分析**：基于 Moo.js，使用正则表达式优化
- **增量解析**：支持部分内容解析
- **内存效率**：按需创建 AST 节点
- **错误恢复**：解析错误时尝试继续解析

## 扩展性

### 自定义 Token 类型

可以通过修改 `devins-lexer.js` 添加新的 token 类型：

```javascript
// 在相应的状态中添加新的 token
newTokenType: /pattern/,
```

### 自定义语法规则

可以通过修改 `devins-simple-parser.js` 添加新的语法规则：

```javascript
parseNewRule() {
    // 实现新的解析规则
}
```

## 测试

运行测试：

```bash
./gradlew jsTest
```

测试覆盖：
- 词法分析器测试
- 语法分析器测试
- 集成测试
- 错误处理测试

## 依赖

- `moo`: 0.5.2 - 词法分析器
- `chevrotain`: 11.0.3 - 语法分析器（可选）
- Kotlin/JS 标准库

## 限制

1. **简化的表达式解析**：当前实现使用简化的表达式解析
2. **有限的错误恢复**：复杂错误情况下可能无法完全恢复
3. **JavaScript 依赖**：需要 Moo.js 库支持

## 未来改进

1. **完整的 Chevrotain 集成**：使用完整的 LR 解析器
2. **更好的错误恢复**：改进错误处理和恢复机制
3. **语义分析**：添加类型检查和语义验证
4. **IDE 支持**：语法高亮、自动完成等
5. **性能优化**：进一步优化解析性能

## 贡献

欢迎贡献代码！请确保：

1. 添加适当的测试
2. 更新文档
3. 遵循代码风格
4. 通过所有测试

## 许可证

[项目许可证信息]
