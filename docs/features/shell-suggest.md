---
layout: default
title: Shell Suggest
parent: Basic Features
nav_order: 12
permalink: /features/refactoring
---

## Default context

```kotlin
data class ShellSuggestContext(
    val question: String,
    val shellPath: String,
    val cwd: String,
    // today's date like 20240322
    val today: String = SimpleDateFormat("yyyyMMdd").format(Date()),
    // operating system name
    val os: String = System.getProperty("os.name")
)
```

