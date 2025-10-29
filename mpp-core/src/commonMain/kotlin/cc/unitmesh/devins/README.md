# DevIns Language Parser for Kotlin Multiplatform

这是 DevIns 语言的 Kotlin Multiplatform 实现，支持在 JVM、JS 和 WASM 平台上运行。

## 架构设计

### 核心组件

1. **Token 定义** (`token/`)
   - `DevInsTokenType`: Token 类型枚举
   - `DevInsToken`: Token 数据类

2. **词法分析器** (`lexer/`)
   - `DevInsLexer`: 主要的词法分析器
   - `LexerState`: 词法分析器状态管理

3. **AST 节点** (`ast/`)
   - `DevInsNode`: AST 节点基类
   - `Expression`: 表达式节点
   - `Statement`: 语句节点
   - `FrontMatter`: 前置元数据节点

4. **语法分析器** (`parser/`)
   - `DevInsParser`: 递归下降解析器
   - `ParseResult`: 解析结果

5. **编译器** (`compiler/`)
   - `DevInsCompiler`: 编译器主类
   - `CompilerContext`: 编译上下文
   - `CompilerResult`: 编译结果

6. **测试框架** (`test/`)
   - `DevInsTestCase`: 测试用例基类
   - `ParserTestRunner`: 解析器测试运行器

## 设计原则

- **平台无关性**: 核心逻辑在 commonMain 中实现
- **类型安全**: 利用 Kotlin 类型系统确保正确性
- **可扩展性**: 支持新的语法特性和平台
- **性能优化**: 针对不同平台进行优化

## 与原实现的对应关系

| 原实现 | MPP 实现 |
|--------|----------|
| DevInLexer.flex | DevInsLexer.kt |
| DevInParser.bnf | DevInsParser.kt |
| DevInTypes.java | DevInsTokenType.kt + AST nodes |
| DevInsCompiler.kt | DevInsCompiler.kt |

## 使用示例

```kotlin
val source = """
---
variables:
  "test": /.*\.kt/ { cat }
---
Hello $test
"""

val lexer = DevInsLexer(source)
val parser = DevInsParser(lexer)
val ast = parser.parse()
val compiler = DevInsCompiler()
val result = compiler.compile(ast)
```
