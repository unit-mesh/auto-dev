---
layout: default
title: Shell Suggest
parent: Basic Features
nav_order: 12
permalink: /features/shell-suggest
---

AutoDev@1.8.0
{: .label .label-yellow }

## Shell Suggest

In Terminal, we provide a feature to generate shell script by LLM.

![Shell Suggest](https://unitmesh.cc/auto-dev/autodev-shell-suggest.png)


## Customize

### Default context

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

### Prompt output example

