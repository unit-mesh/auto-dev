package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.sketch.MarkdownSketchRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme

/**
 * Markdown 渲染演示应用
 * 展示 MarkdownSketchRenderer 的各种渲染能力
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownDemoApp() {
    AutoDevTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("LLM 响应", "代码示例", "Markdown 完整", "纯文本")
            
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopAppBar(
                    title = { Text("Markdown Renderer Demo") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                // Content
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (selectedTab) {
                        0 -> LlmResponseDemo()
                        1 -> CodeExampleDemo()
                        2 -> FullMarkdownDemo()
                        3 -> PlainTextDemo()
                    }
                }
            }
        }
    }
}

@Composable
private fun LlmResponseDemo() {
    val llmResponse = """
# AI 助手回复示例

这是一个模拟的 LLM 响应，展示了混合内容的渲染能力。

## 代码示例

下面是一个 Kotlin 函数：

```kotlin
fun greet(name: String): String {
    return "Hello, ${'$'}name!"
}

fun main() {
    println(greet("World"))
}
```

## 功能说明

该函数具有以下特点：
- **简洁**：代码简单易懂
- **实用**：可直接使用
- *灵活*：可扩展更多功能

## Python 示例

```python
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)

# 输出前 10 个斐波那契数
for i in range(10):
    print(fibonacci(i))
```

希望这些示例对你有帮助！
""".trimIndent()
    
    MarkdownSketchRenderer.RenderResponse(
        content = llmResponse,
        isComplete = true,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CodeExampleDemo() {
    val codeContent = """
下面是一些不同语言的代码示例：

```javascript
// JavaScript 异步函数
async function fetchData(url) {
    try {
        const response = await fetch(url);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Error:', error);
    }
}
```

```rust
// Rust 结构体和实现
struct Person {
    name: String,
    age: u32,
}

impl Person {
    fn new(name: String, age: u32) -> Self {
        Person { name, age }
    }
    
    fn greet(&self) {
        println!("Hello, my name is {} and I'm {} years old", 
                 self.name, self.age);
    }
}
```

```diff
- old line to be removed
+ new line to be added
  unchanged line
- another old line
+ another new line
```
""".trimIndent()
    
    MarkdownSketchRenderer.RenderResponse(
        content = codeContent,
        isComplete = true,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun FullMarkdownDemo() {
    val markdown = """
# Markdown 完整功能展示

## 文本格式

这是**粗体**文本，这是*斜体*文本，这是~~删除线~~文本。

可以组合使用：***粗斜体***，或者 **_嵌套格式_**。

## 列表

### 无序列表

- 第一项
- 第二项
  - 子项 2.1
  - 子项 2.2
- 第三项

### 有序列表

1. 首先
2. 其次
3. 最后

## 引用

> 这是一个引用块
> 
> 可以包含多行内容
> > 甚至可以嵌套引用

## 链接和代码

访问 [GitHub](https://github.com) 获取更多信息。

内联代码：`const x = 10;`

## 代码块

```typescript
interface User {
    id: number;
    name: string;
    email: string;
}

class UserService {
    private users: User[] = [];
    
    addUser(user: User): void {
        this.users.push(user);
    }
    
    getUser(id: number): User | undefined {
        return this.users.find(u => u.id === id);
    }
}
```

## 表格（如果支持）

| 语言 | 类型 | 难度 |
|------|------|------|
| Kotlin | 静态类型 | 中等 |
| Python | 动态类型 | 简单 |
| Rust | 静态类型 | 困难 |

---

**完**
""".trimIndent()
    
    MarkdownSketchRenderer.RenderResponse(
        content = markdown,
        isComplete = true,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PlainTextDemo() {
    val plainText = "这是纯文本内容，不会被解析为 Markdown。\n\n" +
            "即使包含 **粗体** 或 *斜体* 标记，也会原样显示。\n\n" +
            "适合显示日志、错误信息等需要保持原始格式的内容。"
    
    MarkdownSketchRenderer.RenderPlainText(
        text = plainText,
        modifier = Modifier.fillMaxSize()
    )
}

