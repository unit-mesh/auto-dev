package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.agent.model.CustomAgentConfig

data class CompileResult(
    var output: String = "",
    var isLocalCommand: Boolean = false,
    var hasError: Boolean = false,
    var workingAgent: CustomAgentConfig? = null
)
