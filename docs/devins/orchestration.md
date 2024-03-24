---
layout: default
title: Orchestration
nav_order: 3
parent: AI Agent Language
---

# Orchestration

## Auto response for DevIns

see
in: [DevInsProcessProcessor.kt](https://github.com/unit-mesh/auto-dev/blob/master/exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/run/flow/DevInsProcessProcessor.kt)

In the following cases, AutoDev will auto execute:

### Method: AI Agent return `devin` code-block

example:

    ```devin
    Explain code /file:src/main/java/com/example/Controller.java
    ```
### Method: Auto Handle DevIns error

Run DevIns command failed, will call llm to try to fix it. For example, if run program failed

    ```bash
    <DevInsError>: File not found: src/test/
    
    --------------------
    
    Process finished with exit code -1
    ```

### Method: DevIns Comment Flag

Use DevIns comment to flag, will run next script, like:

    ```devin
    [flow]:script/flow.devin
    ```
       
