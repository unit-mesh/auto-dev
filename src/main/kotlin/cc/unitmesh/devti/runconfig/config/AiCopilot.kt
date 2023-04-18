package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.runconfig.command.BaseConfig

class AiCopilot(
    private val methodName: String,
) : BaseConfig() {
    override var configurationName = "Copilot for $methodName<"
}