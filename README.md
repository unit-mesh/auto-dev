# AutoDev

<p align="center">
  <img src="plugin/src/main/resources/META-INF/pluginIcon.svg" width="64px" height="64px" />
</p>

<p align="center">
  <a href="https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml">
    <img src="https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml/badge.svg" alt="Build">
  </a>
  <a href="https://plugins.jetbrains.com/plugin/21520-autodev">
    <img src="https://img.shields.io/jetbrains/plugin/v/21520-autodev.svg" alt="Version">
  </a>
  <a href="https://plugins.jetbrains.com/plugin/21520-autodev">
    <img src="https://img.shields.io/jetbrains/plugin/d/21520-autodev.svg" alt="Downloads">
  </a>
</p>

> AutoDev 是一款高度自动化的 AI 辅助编程工具。AutoDev 能够与您的需求管理系统（例如 Jira、Trello、Github Issue 等）直接对接。在
> IDE 中，您只需简单点击，AutoDev 会根据您的需求自动为您生成代码。您所需做的，仅仅是对生成的代码进行质量检查。

features:

- languages support: Java, Kotlin, Python, JavaScript, or others...
- Auto development mode. With DevTi Protocol (like `devti://story/github/1102`) will auto generate
  Model-Controller-Service-Repository code.
- Smart code completion.
    - Pattern specific.Based on your code context like (Controller, Service `import`), AutoDev will suggest you the
      best code.
    - Related code. Based on recently file changes, AutoDev will call calculate similar chunk to generate best code. 
- AI assistant. AutoDev will help you find bug, explain code, trace exception, generate commits, and more.
- Custom prompt. You can customize your prompt in `Settings` -> `Tools` -> `AutoDev`
- Custom LLM Server. You can customize your LLM Server in `Settings` -> `Tools` -> `AutoDev`
- [ ] Smart architecture. With ArchGuard Co-mate DSL, AutoDev will help you design your architecture.

## Usage

1. Install from JetBrains Plugin Repository: [AutoDev](https://plugins.jetbrains.com/plugin/21520-autodev)
2. Configure GitHub Token (optional) and OpenAI config in `Settings` -> `Tools` -> `AutoDev`

### CodeCompletion mode

You can:

- Right-click on the code editor, select `AutoDev` -> `CodeCompletion` -> `CodeComplete`
- or use `Alt + Enter` to open `Intention Actions` menu, select `AutoDev` -> `CodeCompletion`

![Code completion](https://unitmesh.cc/auto-dev/completion-mode.png)

### Custom prompt

```json
{
  "auto_complete": {
    "instruction": "",
    "input": ""
  },
  "auto_comment": {
    "instruction": "",
    "input": ""
  },
  "code_review": {
    "instruction": "",
    "input": ""
  },
  "refactor": {
    "instruction": "",
    "input": ""
  },
  "write_test": {
    "instruction": "",
    "input": ""
  },
  "spec": {
    "controller": "- 在 Controller 中使用 BeanUtils.copyProperties 进行 DTO 转换 Entity\n- 禁止使用 Autowired\n-使用 Swagger Annotation 表明 API 含义\n-Controller 方法应该捕获并处理业务异常，不应该抛出系统异常。",
    "service": "- Service 层应该使用构造函数注入或者 setter 注入，不要使用 @Autowired 注解注入。",
    "entity": "- Entity 类应该使用 JPA 注解进行数据库映射\n- 实体类名应该与对应的数据库表名相同。实体类应该使用注解标记主键和表名，例如：@Id、@GeneratedValue、@Table 等。",
    "repository": "- Repository 接口应该继承 JpaRepository 接口，以获得基本的 CRUD 操作",
    "ddl": "-  字段应该使用 NOT NULL 约束，确保数据的完整性"
  }
}
```

### AutoCRUD mode

1. add `// devti://story/github/1` comments in your code.
2. configure GitHub repository for Run Configuration.
3. click `AutoDev` button in the comments' left.

Run Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/init-instruction.png)

Output Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/blog-controller.png)

## Development

1. `git clone https://github.com/unit-mesh/AutoDev.git`
2. open in IntelliJ IDEA
3. `./gradlew runIde`

Key Concepts:

- Workflow flow design: [DevFlowProvider](src/main/kotlin/cc/unitmesh/devti/provider/DevFlowProvider.kt)
- Prompt Strategy
  design: [PromptStrategyAdvisor](src/main/kotlin/cc/unitmesh/devti/java/prompt/PromptStrategyAdvisor.kt)

### Release

1. change `pluginVersion` in [gradle.properties](gradle.properties)
2. git tag `version`
3. `./gradlew publishPlugin`

## improve language support for some language

We referenced the multi-language support implementation of JetBrains AI Assistant and combined it with the design
principles of AutoDev to design a series of extension points.

We referenced the multi-target support implementation of Intellij Rust plugin and combined it with the design.

For a new language, you need to implement:

1. create a new module in `settings.gradle.kts`, like: `webstorm`, `pycharm` ...,
2. config in  `build.gradle.kts` for new module, like:

```kotlin
project(":pycharm") {
    intellij {
        version.set(pycharmVersion)
        plugins.set(pycharmPlugins)
    }
    dependencies {
        implementation(project(":"))
    }
}
```

3. sync Gradle in Intellij IDEA
4. create xml file in `resources/META-INF` like `cc.unitmesh.pycharm.xml`, and import
   to `plugin/src/main/resources/META-INF/plugin.xml`
5. create extension points

### Extension Points

JetBrains AI Assistant Extension Points:

```xml

<extensionPoints>
    <extensionPoint qualifiedName="cc.unitmesh.fileContextBuilder"
                    beanClass="com.intellij.lang.LanguageExtensionPoint" dynamic="true">
        <with attribute="implementationClass"
              implements="cc.unitmesh.devti.context.builder.FileContextBuilder"/>
    </extensionPoint>

    <extensionPoint qualifiedName="cc.unitmesh.classContextBuilder"
                    beanClass="com.intellij.lang.LanguageExtensionPoint" dynamic="true">
        <with attribute="implementationClass"
              implements="cc.unitmesh.devti.context.builder.ClassContextBuilder"/>
    </extensionPoint>

    <extensionPoint qualifiedName="cc.unitmesh.methodContextBuilder"
                    beanClass="com.intellij.lang.LanguageExtensionPoint" dynamic="true">
        <with attribute="implementationClass"
              implements="cc.unitmesh.devti.context.builder.MethodContextBuilder"/>
    </extensionPoint>

    <extensionPoint qualifiedName="cc.unitmesh.variableContextBuilder"
                    beanClass="com.intellij.lang.LanguageExtensionPoint" dynamic="true">
        <with attribute="implementationClass"
              implements="cc.unitmesh.devti.context.builder.VariableContextBuilder"/>
    </extensionPoint>
</extensionPoints>
```

AutoDev Extension Points:

```xml

<extensionPoints>
    <!-- AutoCRUD flow -->
    <extensionPoint qualifiedName="cc.unitmesh.devFlowProvider"
                    interface="cc.unitmesh.devti.provider.DevFlowProvider"
                    dynamic="true"/>

    <!-- get tech stacks from dep, like build.gradle, package.json ... -->
    <extensionPoint qualifiedName="cc.unitmesh.techStackProvider"
                    interface="cc.unitmesh.devti.provider.TechStackProvider"
                    dynamic="true"/>

    <!-- custom context strategy for Auto CRUD -->
    <extensionPoint qualifiedName="cc.unitmesh.contextPrompter"
                    interface="cc.unitmesh.devti.provider.ContextPrompter"
                    dynamic="true"/>

    <!-- Others strategy, like token count -->
    <extensionPoint qualifiedName="cc.unitmesh.promptStrategy"
                    interface="cc.unitmesh.devti.provider.PromptStrategy"
                    dynamic="true"/>
</extensionPoints>
```

#### Java/IDEA Example

```xml

<extensions defaultExtensionNs="cc.unitmesh">
    <!-- Language support   -->
    <classContextBuilder language="JAVA"
                         implementationClass="cc.unitmesh.ide.idea.context.JavaClassContextBuilder"/>

    <methodContextBuilder language="JAVA"
                          implementationClass="cc.unitmesh.ide.idea.context.JavaMethodContextBuilder"/>

    <fileContextBuilder language="JAVA"
                        implementationClass="cc.unitmesh.ide.idea.context.JavaFileContextBuilder"/>

    <variableContextBuilder language="JAVA"
                            implementationClass="cc.unitmesh.ide.idea.context.JavaVariableContextBuilder"/>

    <!-- TechStack Binding -->
    <contextPrompter
            language="JAVA"
            implementation="cc.unitmesh.ide.idea.provider.JavaContextPrompter"/>
    <techStackProvider
            language="JAVA"
            implementation="cc.unitmesh.ide.idea.provider.JavaTechStackService"/>
    <devFlowProvider
            language="JAVA"
            implementation="cc.unitmesh.ide.idea.provider.JavaAutoDevFlow"/>
    <promptStrategy
            language="JAVA"
            implementation="cc.unitmesh.ide.idea.provider.PromptStrategyAdvisor"/>
</extensions>
```

## Prompt Strategy

JetBrains LLM and GitHub Copilot try to implmentation like this:

```javascript
defaultPriorities.json = [
    "BeforeCursor",
    "SimilarFile",
    "ImportedFile",
    "PathMarker",
    "LanguageMarker"
]
```

We currently support:

- [x] BeforeCursor
- [ ] SimilarFile
    - [x] JaccardSimilarity Path and Chunks by JetBrains
    - [ ] Cosine Similarity Chunk by MethodName 
- [ ] ImportedFile
    - [x] Java CRUD
    - [ ] all cases
- [x] PathMarker
- [x] LanguageMarker

## License

- ChatUI based
  on: [https://github.com/Cspeisman/chatgpt-intellij-plugin](https://github.com/Cspeisman/chatgpt-intellij-plugin)
- Multiple target inspired
  by: [https://github.com/intellij-rust/intellij-rust](https://github.com/intellij-rust/intellij-rust)
- SimilarFile inspired by: JetBrains and GitHub Copilot

This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
