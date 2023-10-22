---
layout: default
title: Variables
nav_order: 20
permalink: /variables
---

# Variables

- selection: 用于获取当前选中的文本。
- commentSymbol: 用于获取当前语言的注释符号，例如：`//`、`#`、`--`、`/* */` 等。
- beforeCursor: 用于获取当前光标前的文本。
- afterCursor: 用于获取当前光标后的文本。
- language: 用于获取当前文件的语言，例如：`kotlin`、`java`、`python`、`javascript` 等。
- fileName: 用于获取当前文件的文件名。
- filePath: 用于获取当前文件的文件路径。
- methodName: 用于获取当前方法的方法名。
- frameworkContext: 用于获取当前文件的框架上下文，例如：`spring`、`junit`、`mockito` 等。

# Methods

```
interface TeamContextProvider {
    /**
     * Retrieves the code of the target file associated with the given test name.
     *
     * @param fileName the name of the test
     * @return the code of the target file as a string
     */
    fun underTestFileCode(fileName: String): String

    /**
     * Retrieves the code of the target method associated with the given test name.
     *
     * @param testName the name of the test for which to retrieve the target method code
     * @return the code of the target method as a string
     */
    fun underTestMethodCode(testName: String): String

    /**
     * Returns a list of similar chunks.
     *
     * This method retrieves a list of similar chunks based on a certain criteria. The chunks are represented as strings.
     *
     * @return a list of similar chunks as strings
     */
    fun similarChunks(): String

    /**
     * Returns the related code for the given method.
     *
     * This method retrieves the related code that is associated with the current method. The related code
     * can be used to understand the context or dependencies of the method.
     *
     * @return The related code as a string.
     */
    fun relatedCode(): String
}
```
