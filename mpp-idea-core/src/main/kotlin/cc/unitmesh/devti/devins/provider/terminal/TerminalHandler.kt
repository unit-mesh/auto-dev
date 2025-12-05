package cc.unitmesh.devti.devins.provider.terminal

import com.intellij.openapi.project.Project

class TerminalHandler(
    val userInput: String,
    val project: Project,
    val onChunk: (str: String) -> Any?,
    val onFinish: ((str: String?) -> Any?)?,
)
