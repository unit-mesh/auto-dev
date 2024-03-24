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

1. AI Agent return `DevIns` code-block, like:

    ```devin
    Explain code /file:src/main/java/com/example/Controller.java
    ``` 

2. Run DevIns command failed, will call llm to try to fix it. For example, if run program failed

    ```bash
   <DevInsError>: File not found: src/test/
    
    --------------------
    
    Process finished with exit code -1
    ```

3. Use DevIns comment to flag, will run next script, like:

    ```devin
    [flow]:script/flow.devin
    ```
   
