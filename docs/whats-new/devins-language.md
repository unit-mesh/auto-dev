---
layout: default
title: Agent Language - DevIns
nav_order: 9
parent: What's New
---

在上一个版本中，我们构建了 AutoDev 的自定义 Agent 功能，即用户可以通过自定义能力来构建自己的智能体，以实现对于软件开发任务的智能辅助。
而在这个版本中，我们开始构建一个新的 AI Agent 语言：DevIns，即 Development Instruction。即 DevIns 可以让用户更快速描述软件开发任务，
同时，还可以自动化处理来自 AI Agent 返回的内容。

诸如于：`/write:README.md\n```\n# Hello, World!```\n"，AutoDev 将会翻译并执行这个指令，将 `# Hello, World!` 写入到 `README.md` 文件中。
除此，在这个版本的 DevIns 里，还可以执行补丁、提交代码、运行测试。

PS：其实原来是叫 DevIn，但是无奈 Devin AI 项目发布了 demo 视频，所以改名为 DevIns。

## Why DevIns？

在 AutoDev 项目中，我们通过**构建上下文感知**与**自定义能力**，来实现对于软件开发任务的智能辅助，如自动测试生成与运行、UI
自动生成等。而当
我们在 AutoDev 构建了越来越多的智能体之后，发现所有与模型的交互都是通过**指令文本**（instruction）。即用户通过指令文本来与智能体进行交互，
而智能体返回内容，并对编辑器或者 IDE 进行操作。

如在 AutoDev 的自定义 prompt 中，我们可以通过：`解释选中的代码：$selction` 来让 AI 为我们解释选中的代码，而这里的 `解释`
就可以看作是一个指令。

所以，我们开始思考，是否可以通过**自然语言**来与智能体进行交互？即用户可以通过自然语言来描述自己的需求，而模型可以回复对应的指令文本，以实现
对编辑器或者 IDE 的操作，进而实现对软件开发任务的自动化辅助。

## DevIns 语言是什么？

> DevIns 是一个界于自然语言与指令文本之间的交互语言，其中自然语言用于描述软件开发任务，而指令文本用于与智能体和 IDE 进行交互。

简单来说，DevIns 是一个可交互、可编译、可执行的文本语言。你可以通过 DevIns 来描述软件开发任务，诸如于：解析代码、生成代码、运行测试等等，而后
执行运行，DevIns 编译器将根据你调用的指令，生成对应的指令文本，并将其发送给智能体，智能体将返回对应的结果，并对编辑器或者 IDE
进行操作。

你可以将你的需求描述成自然语言：

```devin
解释代码 /file:src/main/java/com/example/Controller.java
```

而后，AutoDev 将会结合上下文，并将其编译成对应的指令文本，即读取 `src/main/java/com/example/Controller.java` 文件内容。

### DevIns Agent 指令

除了基本的读取文件内容、代码变更、自定义变量信息，DevIns 还支持更多的指令，诸如于：写入文件、运行测试、提交代码等。 根据预先设计的指令，
对应的操作可以是：

- `/write`，结合路径信息，对指令的代码进行操作
- `/run`，运行对应的测试
- `/patch`，根据 AI 返回的内容，执行对应的 patch
- `/commit`，提交代码

诸如于于生成内容，可以是：

    /write:src/main/java/com/example/Controller.java#L1-L5
    ```java
    public class Controller {
        public void method() {
            System.out.println("Hello, World!");
        }
    }
    ```

详细见：[https://ide.unitmesh.cc/devins](https://ide.unitmesh.cc/devins)

别担心指令的复杂度，我们在 IDE 开发上拥有丰富的经验，为此在 DevIns 构建了 “非常” 强大的交互能力 —— 智能补全与提示。

### 在 IDE 中使用 DevIns

在安装完 AutoDev 1.7.2  版本的插件后，新建一个 `hello.devins` 文件，就可以开始编写 DevIns 指令了，然后点击运行即可。如下图所示：

![AutoDev DevIns](https://unitmesh.cc/auto-dev/autodev-devins.png)

是不是非常简单。

## 为什么名为 DevIns？

几周前，当我们开始设计这个语言时，我们的名字意图是：AutoDev Input Language，即 AutoDev 的输入语言，我们称其为 DevIn ——
我们搜索了一下， 并没有发现类似的项目。 而当语言接近发布的时候， 在社交媒体上更火的 Devin AI 项目也刚好发布了 demo 视频。

WTF????

考虑到两者的相似性，我们决定将其重新命名为 DevIns，即 Development Instruction。 改名字并不是一件容易的事，有大量的代码需要修改，
还有大量的文档需要更新（虽然没有），除此还有 JetBrains 的插件市场、仓库等等。 而由于 JetBrains 的审核机制，
DevIns 的默认文件后缀依旧是 `.devin`，还没有改为 `.devins` —— 实在是改不动了。

## 下一步

在接下来的版本中，我们考虑：

1. 强化 DevIns 语言与智能体的交互方式（类似于 Jupyter Notebook？）
2. 结合 AutoDev 的自定义 Agent 能力，构建更多的智能体
3. 设计更丰富的 DevIns 指令，以让 AI 来实现更多的软件开发任务
4. 构建跨平台的 DevIns 编译器

如果大家有兴趣，欢迎加入我们的开发，或者提出你的建议。
