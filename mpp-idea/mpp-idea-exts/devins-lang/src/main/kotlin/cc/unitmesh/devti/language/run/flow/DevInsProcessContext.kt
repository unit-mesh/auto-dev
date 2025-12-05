package cc.unitmesh.devti.language.run.flow

import cc.unitmesh.devti.language.compiler.DevInsCompiledResult

data class DevInsProcessContext(
    val scriptPath: String,
    val compiledResult: DevInsCompiledResult,
    val llmResponse: String,
    val ideOutput: String,
    val messages: MutableList<cc.unitmesh.devti.llms.custom.Message> = mutableListOf(),
    var hadReRun: Boolean = false
) {
    // update messages when has Error or Warning
}