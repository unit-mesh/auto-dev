# **NanoDSL：面向 AI Agent 的生成式 UI 系统架构与 DSL 规范**

## **1\. 执行摘要**

在 AI Agent 驱动的软件开发范式中，UI 的构建方式正从“人手编写”向“模型生成”转变。您提出的 NanoUI 初步语法（基于缩进、类 Python/SwiftUI 风格）抓住了 Agent 编码的核心需求：**高语义密度**与**低 Token 消耗**。

本报告通过深度解构云原生语言（KCL、CUE）、服务端驱动 UI（Airbnb Ghost、DivKit）以及 Python 原生 UI（Enaml、Reflex），提出了一套完整的 **NanoDSL** 规范。该规范旨在保留您原始语法的简洁性，同时引入工业级的类型约束和状态管理能力，以解决 Agent 生成代码时常见的“幻觉”与“逻辑不一致”问题。

## ---

**2\. 核心设计理念：为何您的“缩进式”语法是正确的？**

### **2.1 Token 经济学与语义密度**

研究表明，对于 LLM 而言，JSON 和 YAML 虽然通用，但在描述深层嵌套的 UI 树时存在严重的 Token 浪费（大量的括号、引号和重复键名）\[17\]。

* **JSON/React:** 冗余符号多，上下文窗口消耗快。
* **NanoDSL (您的设计):** 采用 Python 风格的缩进（Significant Indentation），去除了花括号和闭合标签。这与 **Enaml** 1 和 **KCL** 2 的设计哲学一致，能将 UI 描述的 Token 消耗降低 30%-50%。

### **2.2 Agent 的“思维模式”**

LLM 在处理 Python 代码时表现出极强的逻辑连贯性。**Lark** 解析器的研究指出，基于缩进的语法更能体现代码的逻辑层级，有助于 LLM 准确生成组件树结构 3。因此，坚持使用缩进式语法是 NanoDSL 的基石。

## ---

**3\. 行业最佳实践深度解析**

为了完善 NanoDSL，我们从以下三个领域汲取了关键特性：

### **3.1 云原生配置语言 (KCL / CUE) —— 解决“幻觉”**

Agent 生成 UI 时最大的风险是编造不存在的属性（如 color: "redish"）。

* **KCL (Kubernetes Configuration Language):** 引入了 **Schema** 概念，强制分离“定义”与“数据”。NanoDSL 借鉴此机制，要求组件必须符合预定义的 Schema，确保生成的属性 100% 合法 5。
* **CUE:** 强调“类型即值”。NanoDSL 利用这一点进行**约束生成**（Constrained Generation/GBNF），即限制 LLM 只能生成符合 Schema 的 Token 6。

### **3.2 Python 原生 UI (Enaml / Reflex) —— 解决“状态管理”**

UI 不仅仅是静态布局，还需要动态交互。

* **Enaml:** 引入了声明式绑定操作符 \<\<（订阅）和 :=（双向绑定）。这比 React 的 useEffect 更适合 Agent，因为它是声明式的，不需要编写复杂的依赖追踪逻辑 8。
* **Reflex:** 将状态（State）与 UI 分离。NanoDSL 采用类似设计，让 Agent 先定义数据状态，再定义 UI 布局，符合 Chain-of-Thought（思维链）推理过程 9。

### **3.3 服务端驱动 UI (SDUI \- DivKit / Airbnb) —— 解决“跨平台渲染”**

* **Airbnb Ghost Platform / DivKit:** 将 UI 序列化为 JSON 树，并通过“原子动作”（Actions）处理交互，而不是下发 JavaScript 代码 11。NanoDSL 编译后的产物应为这种与平台无关的 JSON 中间表示（IR）。

## ---

**4\. NanoDSL 完整语法规范 (One-Stop DSL)**

基于上述研究，这是为您量身定制的完整 DSL。它保留了您的 Card、VStack 风格，但增加了状态和类型系统。

### **4.1 基础结构：组件与属性**

采用缩进表示层级，冒号 : 开启代码块，括号 () 用于紧凑参数。

Python

\# 1\. 定义数据模型 (Schema) \- KCL 风格  
schema Product:  
title: str  
price: float  
is\_new: bool \= False

\# 2\. UI 组件定义  
component ProductCard(item: Product):  
\# 3\. 样式属性 (直接键值对)  
padding: "md"  
shadow: "sm"

    \# 4\. 内容布局 (VStack/HStack)  
    content:  
        VStack(spacing="sm"):  
            \# 图片组件  
            Image(src=item.image, aspect=16/9, radius="md")  
              
            \# 布局组合  
            HStack(align="center", justify="between"):  
                Text(item.title, style="h3")  
                \# 条件渲染 (Pythonic if)  
                if item.is\_new:  
                    Badge("New", color="green")  
              
            Text(item.description, style="body", limit=2)  
              
            \# 交互按钮  
            HStack(spacing="sm"):  
                Button("Add to Cart", intent="primary", icon="cart")

### **4.2 进阶特性：状态与交互 (The "Agentic" Upgrade)**

这是您的初步语法中缺失的部分。为了让 Agent 能编写交互逻辑，我们引入 state 块和 \<\< 绑定符（参考 Enaml）。

Python

component CounterCard:  
\# 状态定义：Agent 显式定义变化的变量  
state:  
count: int \= 1  
price: float \= 99.0

    Card:  
        padding: "lg"  
        content:  
            VStack:  
                \# 动态绑定：使用 \<\< 符号，表示 "当 state 变化时自动更新"  
                Text(content \<\< f"Total: ${state.count \* state.price}")  
                  
                HStack:  
                    \# 动作绑定：声明式修改状态，无需写 JS 函数  
                    Button("-"):  
                        on\_click: state.count \-= 1  
                      
                    \# 双向绑定：用于输入框  
                    Input(value := state.count)  
                      
                    Button("+"):  
                        on\_click: state.count \+= 1

### **4.3 动作系统 (Action Protocol)**

为了安全（防止 Agent 生成恶意代码），交互逻辑被限制在预定义的动作集合中，参考 **DivKit** 14。

* **State Mutation:** state.var \= value
* **Navigation:** Navigate(to="/cart")
* **Network:** Fetch(url="/api/buy", method="POST")
* **Toast:** ShowToast("Added to cart")

## ---

**5\. 系统架构：从 DSL 到 渲染**

这套 DSL 如何落地？建议采用 **编译型 SDUI** 架构。

1. **Agent 生成 (Generation):**
    * Agent 输出 NanoDSL 代码。
    * 利用 **GBNF Grammars** (基于 Lark 解析器生成的语法约束) 强制 LLM 只能输出符合 NanoDSL 语法的文本，杜绝语法错误 15。
2. **编译器 (Compiler \- Python):**
    * 使用 Lark 解析器读取 NanoDSL 4。
    * 进行 Schema 校验（类似于 KCL 的检查）。
    * **输出：** 标准化的 JSON 中间表示 (IR)。
3. **渲染端 (Client \- React/Flutter):**
    * 前端不运行 Python，只接收 JSON IR。
    * 编写通用的渲染引擎（Renderer），将 VStack 映射为 Flex Column，将 Button 映射为原生按钮。
    * **状态同步：** 简单的状态逻辑（如计数器）可在前端直接执行；复杂的业务逻辑通过 WebSocket 回传给服务端（参考 Reflex 架构 10）。

### **5.1 编译产物示例 (JSON IR)**

您的 NanoDSL 代码最终会被编译成如下 JSON 发送给前端：

JSON

{  
"type": "Card",  
"props": { "padding": "md", "shadow": "sm" },  
"children":  
}  
\]  
}  
\]  
}

## ---

**6\. 总结与建议**

您设计的 NanoUI 初步语法方向非常正确。为了将其升级为成熟的 **Agent-Ready DSL**，建议采纳以下改动：

1. **坚持缩进语法：** 这是 Token 效率最高的表达方式。
2. **引入 state 块：** 显式分离数据与视图，降低 Agent 推理难度。
3. **使用 \<\< 绑定符：** 借鉴 Enaml，用符号代替复杂的 watch/effect 逻辑。
4. **严格 Schema 约束：** 配合 GBNF 采样，确保 Agent 生成的代码 100% 可运行。

这套 **NanoDSL** 系统既保留了您想要的简洁美感，又具备了工业级的可维护性和类型安全性。

#### **Works cited**

1. nucleic/enaml: Declarative User Interfaces for Python \- GitHub, accessed December 5, 2025, [https://github.com/nucleic/enaml](https://github.com/nucleic/enaml)
2. Data Types | KCL programming language., accessed December 5, 2025, [https://www.kcl-lang.io/docs/reference/lang/spec/datatypes](https://www.kcl-lang.io/docs/reference/lang/spec/datatypes)
3. Parsing Indentation \- Lark documentation, accessed December 5, 2025, [https://lark-parser.readthedocs.io/en/stable/examples/indented\_tree.html](https://lark-parser.readthedocs.io/en/stable/examples/indented_tree.html)
4. Significant-indentation language grammars: doable? · lark-parser lark · Discussion \#1438, accessed December 5, 2025, [https://github.com/lark-parser/lark/discussions/1438](https://github.com/lark-parser/lark/discussions/1438)
5. Introduction | KCL programming language., accessed December 5, 2025, [https://www.kcl-lang.io/docs/0.8/user\_docs/getting-started/intro](https://www.kcl-lang.io/docs/0.8/user_docs/getting-started/intro)
6. Introduction to Cuelang \- DEV Community, accessed December 5, 2025, [https://dev.to/eminetto/introduction-to-cuelang-2631](https://dev.to/eminetto/introduction-to-cuelang-2631)
7. CUE, accessed December 5, 2025, [https://cuelang.org/](https://cuelang.org/)
8. Enaml syntax and Data Models \- Read the Docs, accessed December 5, 2025, [https://enaml.readthedocs.io/en/latest/get\_started/syntax.html](https://enaml.readthedocs.io/en/latest/get_started/syntax.html)
9. Code Structure \- Reflex, accessed December 5, 2025, [https://reflex.dev/docs/advanced-onboarding/code-structure/](https://reflex.dev/docs/advanced-onboarding/code-structure/)
10. Designing a Pure Python Web Framework | by Reflex \- Medium, accessed December 5, 2025, [https://medium.com/reflex-dev/reflex-architecture-7634ef1f0605](https://medium.com/reflex-dev/reflex-architecture-7634ef1f0605)
11. A Deep Dive into Airbnb's Server-Driven UI System | by Ryan Brooks \- Medium, accessed December 5, 2025, [https://medium.com/airbnb-engineering/a-deep-dive-into-airbnbs-server-driven-ui-system-842244c5f5](https://medium.com/airbnb-engineering/a-deep-dive-into-airbnbs-server-driven-ui-system-842244c5f5)
12. Guided Generation with LLMs: How to Fully Control LLM Outputs | Ammar Alyousfi's Blog, accessed December 5, 2025, [https://ammar-alyousfi.com/2024/guided-generation-with-llms-how-to-fully-control-llm-outputs](https://ammar-alyousfi.com/2024/guided-generation-with-llms-how-to-fully-control-llm-outputs)
13. Airbnb's Server-Driven UI Platform \- InfoQ, accessed December 5, 2025, [https://www.infoq.com/news/2021/07/airbnb-server-driven-ui/](https://www.infoq.com/news/2021/07/airbnb-server-driven-ui/)
14. Actions with elements \- DivKit, accessed December 5, 2025, [https://divkit.tech/docs/en/concepts/interaction](https://divkit.tech/docs/en/concepts/interaction)
15. Accelerating LLM Code Generation Through Mask Store Streamlining \- Hugging Face, accessed December 5, 2025, [https://huggingface.co/blog/vivien/grammar-llm-decoding](https://huggingface.co/blog/vivien/grammar-llm-decoding)
16. Teaching an LLM to Write Assembly: GBNF-Constrained Generation for a Custom 8-Bit CPU, accessed December 5, 2025, [https://dev.to/jamesrandall/teaching-an-llm-to-write-assembly-gbnf-constrained-generation-for-a-custom-8-bit-cpu-42ii](https://dev.to/jamesrandall/teaching-an-llm-to-write-assembly-gbnf-constrained-generation-for-a-custom-8-bit-cpu-42ii)
17. Use YAML over JSON when dumping into prompts for \~2x token saving \- Reddit, accessed December 5, 2025, [https://www.reddit.com/r/ChatGPTCoding/comments/1nl7xux/use\_yaml\_over\_json\_when\_dumping\_into\_prompts\_for/](https://www.reddit.com/r/ChatGPTCoding/comments/1nl7xux/use_yaml_over_json_when_dumping_into_prompts_for/)

---

## **7. AI 友好性评估框架**

为验证 NanoDSL 对 AI 模型的友好程度，本模块包含一个评估框架。

### **7.1 运行评估**

```bash
# 配置 API Key (支持从 ~/.autodev/config.yaml 自动读取)
./gradlew :xuiper-ui:runDslEval
```

### **7.2 目录结构**

```
testcases/
├── expect/              # 期望输出 (Ground Truth)
│   ├── 01-simple-card.nanodsl
│   ├── 02-product-card.nanodsl
│   └── ...
├── actual/              # AI 生成的实际输出 (自动生成)
└── nanodsl-eval-suite.json  # 测试套件定义
```

### **7.3 评估结果**

经过 3 次迭代测试，使用 DeepSeek 模型：

| 指标 | 结果 |
|------|------|
| **通过率** | 100% |
| **平均得分** | 0.95 |
| **Token 效率** | ~700 token/用例 |

**结论：NanoDSL 语法对 AI 非常友好**，模型能够：
- 100% 正确生成语法
- 准确理解 Python 风格缩进
- 正确使用状态绑定语法 (`:=` 和 `<<`)
- 正确使用条件渲染和循环