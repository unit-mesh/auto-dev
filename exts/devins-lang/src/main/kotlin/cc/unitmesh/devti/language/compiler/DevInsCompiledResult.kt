package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.agent.model.CustomAgentConfig

data class DevInsCompiledResult(
    /**
     * The origin DevIns content
     */
    var input: String = "",
    /**
     * Output String of a compile result
     */
    var output: String = "",
    var isLocalCommand: Boolean = false,
    var hasError: Boolean = false,
    var executeAgent: CustomAgentConfig? = null
) {
}
