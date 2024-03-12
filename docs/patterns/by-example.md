---
layout: default
title: By Example
nav_order: 1
parent: Design Patterns
---

根据当前的用户习惯，寻找待生成的内容作为示例，而后作为生成式 AI 的输入。

## 提交信息示例

步骤：

1. 获取当前项目的版本控制系统（VCS）日志提供者。
2. 获取当前分支和用户。
3. 根据用户或分支过滤日志。
4. 收集示例提交信息。

代码示例：

```kotlin
private fun findExampleCommitMessages(project: Project): String? {
    val logProviders = VcsProjectLog.getLogProviders(project)
    val entry = logProviders.entries.firstOrNull() ?: return null

    val logProvider = entry.value
    val branch = logProvider.getCurrentBranch(entry.key) ?: return null
    val user = logProvider.getCurrentUser(entry.key)

    val logFilter = if (user != null) {
        VcsLogFilterObject.collection(VcsLogFilterObject.fromUser(user, setOf()))
    } else {
        VcsLogFilterObject.collection(VcsLogFilterObject.fromBranch(branch))
    }

    return collectExamples(logProvider, entry.key, logFilter)
}
```

## 测试代码示例

参考语言实现：`JavaTestContextProvider`、`KotlinTestContextProvider`。

步骤：

1. 获取当前项目的被测试代码。
2. 根据被测试代码寻找模板：`Controller`、`Service`、默认测试等。
    - Kotlin：`ControllerTest.kt`, `ServiceTest.kt`, `Test.kt`
    - Java: `ControllerTest.java`, `ServiceTest.java`, `Test.java`
3. 根据模板生成测试代码。



## 文档示例

DOC TODO

## 代码示例

DOC TODO
