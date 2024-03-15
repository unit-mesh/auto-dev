---
layout: default
title: DevIn Input Language
nav_order: 4
parent: Development
---

AutoDev@1.7.1
{: .label .label-yellow }

# DevIn Input Language 

In issue [#101](https://github.com/unit-mesh/auto-dev/issues/101),
in order to make `@`, `/`, `#`, `$` and `!` completion better, we introduce a new input language: DevIn.

Code: [DevIn Language](https://github.com/unit-mesh/auto-dev/tree/master/exts/devin-lang)

Based on: [JetBrains' Markdown Util](https://github.com/JetBrains/intellij-community/tree/master/platform/markdown-utils)

## Design

- Command, natural language command with IDE/editor, like read file, write file, etc.
- Agent, natural language custom function / system function name, the handler or command, 
- Variable, natural language variable name, like file name, file content, etc.

## Language spec

```bnf
DevInFile ::= (used | code | TEXT_SEGMENT | NEWLINE)*

used ::= (
    AGENT_START AGENT_ID (COLON PROPERTY_VALUE?)?
    | COMMAND_START COMMAND_ID
    | VARIABLE_START VARIABLE_ID
)

code ::=  CODE_BLOCK_START LANGUAGE_ID? NEWLINE? code_contents? CODE_BLOCK_END?

code_contents ::= (NEWLINE | CODE_CONTENT)*
```

