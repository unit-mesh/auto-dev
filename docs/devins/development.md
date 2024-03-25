---
layout: default
title: Development DevIns
nav_order: 99
parent: AI Agent Language
---

## Implementation `/run` command in different languages

Since we need to verify AI generated unit which is code, test is the best way to verify it. So most of the time, we
use `AutoTestService` to run the test.

In different language, the test runner is different, like:

- C/C++ => CppAutoTestService (CMake + Catch2)
- Go => GoAutoTestService
- Java => JavaAutoTestService (Gradle)
- Python => PythonAutoTestService

### Resources

[JetBrains Academy plugin](https://github.com/JetBrains/educational-plugin) shows a very good sample on how to organize 
different types of tasks in a single plugin.

- check: `EduTaskCheckerBase.kt` for task running
