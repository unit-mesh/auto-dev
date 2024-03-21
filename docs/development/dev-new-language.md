---
layout: default
title: Dev New Language
nav_order: 1
parent: Development
---

If you want to develop a new language for AutoDev, you can follow this guide.

1. lookup the plugin in [JetBrains' Plugin Repository](https://plugins.jetbrains.com/)
2. create new language plugin module in AutoDev.
    - set dependencies in `build.gradle.kts`.
    - set dependencies in `settings.gradle.kts`.
    - create plugin module file under `newlang/src/main/resources/cc.unitmesh.<newlang>.xml`
    - declare plugin module in `plugin/src/main/plugin.xml`
3. implement the plugin module.

## AutoDev Extension Point

### CodeDataStructure Context Provider

> CodeDataStructure will provide the data structure for code, like file, class, method, variable, etc. Which will be
> used in Test Generation, Code Complete, Code Refactor, etc.

At beginning, we use [Chapi](https://github.com/phodal/chapi) to parse code data structure, but it's too slow.
And, we found that JetBrains' IDE already have a good data structure, so we use it. We follow JetBrains' code data
structure and design.

```xml
<extensions defaultExtensionNs="cc.unitmesh">
   <fileContextBuilder language="Rust"
                       implementationClass="cc.unitmesh.rust.context.RustFileContextBuilder"/>
   <classContextBuilder language="Rust"
                        implementationClass="cc.unitmesh.rust.context.RustClassContextBuilder"/>
   <methodContextBuilder language="Rust"
                         implementationClass="cc.unitmesh.rust.context.RustMethodContextBuilder"/>
   <variableContextBuilder language="Rust"
                           implementationClass="cc.unitmesh.rust.context.RustVariableContextBuilder"/>
</extensions>
```

### Chat Context Provider

> Chat Context Provider will provide the data structure for chat, like Language version, Compiler version, Framework
> information, etc.

Similar to CodeDataStructure Context Provider, we use JetBrains' design for Chat Context Provider. You can implement
multiple Chat Context Providers for same languages.

```xml
<chatContextProvider implementation="cc.unitmesh.rust.provider.RustVersionContextProvider"/>
<chatContextProvider implementation="cc.unitmesh.rust.provider.RustCompilerContextProvider"/>
```

### Test Context Provider

> Test Context will collect that context for test generation, and with CodeModifier to generate test code.

```xml
<testContextProvider language="Rust" implementation="cc.unitmesh.rust.provider.RustTestService"/>

<codeModifier language="Rust" implementationClass="cc.unitmesh.rust.provider.RustCodeModifier"/>
```

### Living Documentation

> Living Documentation will provide the living documentation for user, and also can generate the comments.

```xml
<livingDocumentationProvider language="Rust" implementation="cc.unitmesh.rust.provider.RustLivingDocumentationProvider"/>
```

### API TestDataBuilder

> API TestDataBuilder will provide the API test data for user, like API test data, API test code, etc.

```xml
<testDataBuilder language="kotlin"
             implementationClass="cc.unitmesh.kotlin.provider.KotlinTestDataBuilder"/>
```

### contextPrompter

> Context Prompter will provide the context prompt rules for user, like display and request prompts.

```xml
<contextPrompter
          language="kotlin"
          implementation="cc.unitmesh.kotlin.provider.KotlinContextPrompter"/>
```

### Custom Prompt Provider

> customPromptProvider will provide the custom prompt functions for user.

```xml
<customPromptProvider
        language="kotlin"
        implementationClass="cc.unitmesh.kotlin.provider.KotlinCustomPromptProvider"/>
```