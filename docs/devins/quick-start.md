---
layout: default
title: DevIns Quick Start
nav_order: 1
parent: AI Agent Language
---

AutoDev@1.7.2
{: .label .label-yellow }

## Quick Start

1. first create new file, like `sample.devin`, with content:

```devin
Explain code /file:src/main/java/com/example/Controller.java
```

2. Run file with `AutoDev` plugin, and you will see AI response result.

ScreenShot

![AutoDev DevIns](https://unitmesh.cc/auto-dev/autodev-devins.png)

## Basic Commands

- `/file`: read file content
- `/write`: write file content
- `/rev`: read git change by git revision
- `/run`: run code
- `/patch`: apply patches to file
- `/commit`: commit changes to git

### File Command

    Explain code /file:src/main/java/com/example/Controller.java

### Write Command


    /write:src/main/java/com/example/Controller.java#L1-L12
    ```java
    public class Controller {
        public void method() {
            System.out.println("Hello, World!");
        }
    }
    ```

### Rev Command

    Explain code /rev:HEAD~1

### Run Command

    /run:src/main/java/com/example/Controller.java

## Case Design

### Introduce file

For example:

    Explain code /file:src/main/java/com/example/Controller.java

will call LLM to explain the code in the file `src/main/java/com/example/Controller.java`.

### Edit file

If AI Agent returns a code snippet, AutoDev can edit the file with the code snippet.

For example:

    /write:src/main/java/com/example/Controller.java#L1-L12
    ```java
    public class Controller {
        public void method() {
            System.out.println("Hello, World!");
        }
    }
    ```

will edit the file `src/main/java/com/example/Controller.java` with the code snippet.
The `#L1-L12` is the line range to edit. The code snippet is in Java language.
