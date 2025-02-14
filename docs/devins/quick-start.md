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

## Basic Commands (BuiltinCommand.kt)

- `/file`: read file content, format: `/file:<file-path>`, example: `/file:src/main/java/com/example/Controller.java`.
- `/write`: write file content, format: `file#L1-L12`, example: `src/main/java/com/example/Controller.java#L1-L12`
- `/rev`: read git change by git revision.
- `/run`: run code, especially for test file, which is the best way to run code.
- `/patch`: apply patches to file.
- `/commit`: commit changes to git
- `/symbol`: get child by symbol, like get Class by package name, format: `java.lang.String#length`,
  example: `<package>.<class>#<method>`
- `/shell`: run shell command or shell script, like `ls`, `pwd`, etc.
- `/browse`: browse web page, like `https://ide.unitmesh.cc`
- `/refactor`: refactor code, like `rename`, `delete`, `move` etc. (since @1.8.6) (Java only)
- `/file-func`: read the name of a file, support for: regex, example: `/file-func:regex(".*\.txt")`
- `/structure`: get the structure of a file with AST/PSI, example: `/structure:cc.unitmesh.devti.language.psi`
- `/dir`: list files and directories in a tree-like structure, example: `/dir:src`
- `/database`: read the content of a database, example: `/database:query\n```sql\nSELECT * FROM table\n````
- `/localSearch`: search text in the scope (current only support project) will return 5 line before and after, example: `/localSearch:project\n```\nselect * from blog\n````
- `/related`: get related code by AST (abstract syntax tree) for the current file, example: `/related:cc.unitmesh.devti.language.psi`
- `/open`: open a file in the editor, example: `/open:.github/dependabot.yml`
- `/ripgrepSearch`: search text in the project with ripgrep, example: `/ripgrepSearch:.*AutoDev.*`

### File Command

based on [#143](https://github.com/unit-mesh/auto-dev/issues/143), we keep "/" as `File.separator` for macOS, Windows and Unix.

Read file content:

    Explain code /file:src/main/java/com/example/Controller.java

will call LLM to handle it.

### Write Command

write content to file:

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

will call LLM to handle it.

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

Get method will return code:

    /symbol:cc.unitmesh.untitled.demo.MathHelper.calculateInsurance

The output will be:

    ```java
    public static double calculateInsurance(double income) {
        if (income <= 10000) {
            return income * 0.365;
        } else {
            return income * 0.365 + 1000;
        }
    }
    ```

### Browse Command

Browse web page:

    /browse:https://ide.unitmesh.cc

It will be text inside the body from web page.

### Refactor Command

Refactor code:

    /refactor:rename /symbol:cc.unitmesh.untitled.demo.MathHelper.calculateInsurance to calculateInsuranceTax

It will handle in local.
