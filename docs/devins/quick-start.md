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
- `/write`: write file content, format: `file#L1-L12`, example: `src/main/java/com/example/Controller.java#L1-L12`
- `/rev`: read git change by git revision
- `/run`: run code
- `/patch`: apply patches to file
- `/commit`: commit changes to git
- `/symbol`: get child by symbol, like get Class by package name, format: `java.lang.String#length`, example: `<package>.<class>#<method>`

### File Command

Read file content:

    Explain code /file:src/main/java/com/example/Controller.java

### Write Command

write file content:

    /write:src/main/java/com/example/Controller.java#L1-L12
    ```java
    public class Controller {
        public void method() {
            System.out.println("Hello, World!");
        }
    }
    ```

### Rev Command

Read git change by git revision:

    Explain code /rev:HEAD~1

### Run Command

Run file:

    /run:src/main/java/com/example/Controller.java

PS: current only support for TestFile, since UnitTest is the best way to run code.

### Symbol Command

Get child elements by symbol, like get Class by package name.

    /symbol:cc.unitmesh.untitled.demo

The output will be:
    
    ```java
    cc.unitmesh.untitled.demo.MathHelper
    cc.unitmesh.untitled.demo.DemoApplication
    cc.unitmesh.untitled.demo.MathHelperTest
    cc.unitmesh.untitled.demo.DemoApplicationTests
    ```
