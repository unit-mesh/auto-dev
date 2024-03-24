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

Example:

    ```devin
    Explain code /file:src/main/java/com/example/Controller.java
    ```

consider use `$command` to get $command example, and let LLM handle it.

### Method: Auto Handle DevIns error

Run DevIns command failed, will call llm to try to fix it. For example, if run program failed

    ```bash
    <DevInsError>: File not found: src/test/
    
    --------------------
    
    Process finished with exit code -1
    ```

In this case, the AutoDev will handle rest in ChatPanel.

### Method: DevIns Comment Flag

Experimental Feature
{: .label .label-yellow }

Use DevIns comment to flag, will run the next script, like:

    ```devin
    [flow]:script/flow.devin
    ```

still in experimental, and may change in the future. 
