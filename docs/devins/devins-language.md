---
layout: default
title: DevIns Language Spec
nav_order: 2
parent: AI Agent Language
---

AutoDev@1.7.2
{: .label .label-yellow }

# DevIn Language 

In issue [#101](https://github.com/unit-mesh/auto-dev/issues/101), to better interactive with LLM, and also 
handle `@`, `/`, `#`, `$` and `!` completion better, we introduce a new language: DevIn.

Code: [DevIns Language](https://github.com/unit-mesh/auto-dev/tree/master/exts/devin-lang)

Based on: [JetBrains' Markdown Util](https://github.com/JetBrains/intellij-community/tree/master/platform/markdown-utils)

## Design

- `/` Builtin Command, natural language command with IDE/editor, like read file, write file, etc.
- `@` Agent, natural language custom function / system function name, the handler or command, 
- `$` Variable, natural language variable name, like file name, file content, etc.
- `#` Third-party system API for traditional, like `#kanban`, `#issue`, `#github`, etc. 

## Language spec

```bnf
DevInFile ::= (used | code | TEXT_SEGMENT | NEWLINE)*

used ::= (
    AGENT_START AGENT_ID
    | COMMAND_START COMMAND_ID (COLON COMMAND_PROP?)?
    | VARIABLE_START VARIABLE_ID
    | SYSTEM_START SYSTEM_ID
)

code ::=  CODE_BLOCK_START LANGUAGE_ID? NEWLINE? code_contents? CODE_BLOCK_END?

code_contents ::= (NEWLINE | CODE_CONTENT)*
```

## Commands

- `/file`: read file content
- `/write`: write file content
- `/rev`: read git change by git revision
- `/run`: run code

### File Command

```devin
Explain code /file:src/main/java/com/example/Controller.java
```

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

```devin
Explain code /rev:HEAD~1
```

### Run Command

```devin
/run:src/main/java/com/example/Controller.java
```

## Case Design

### Introduce file

For example:

```devin
Explain code /file:src/main/java/com/example/Controller.java
```

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
