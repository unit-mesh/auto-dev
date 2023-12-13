---
layout: default
title: Custom Language
parent: Customize Features
nav_order: 11
permalink: /custom/language
---

# Custom Language

## improve language support

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
    <extensionPoint qualifiedName="cc.unitmesh.contextPrompter"
                    interface="cc.unitmesh.devti.provider.ContextPrompter"
                    dynamic="true"/>
  
    <extensionPoint qualifiedName="cc.unitmesh.promptStrategy"
                    interface="cc.unitmesh.devti.provider.PromptStrategy"
                    dynamic="true"/>
  
    <extensionPoint qualifiedName="cc.unitmesh.testContextProvider"
                    interface="cc.unitmesh.devti.provider.WriteTestService"
                    dynamic="true"/>
  
    <extensionPoint qualifiedName="cc.unitmesh.chatContextProvider"
                    interface="cc.unitmesh.devti.provider.context.ChatContextProvider"
                    dynamic="true"/>
</extensions>
```
