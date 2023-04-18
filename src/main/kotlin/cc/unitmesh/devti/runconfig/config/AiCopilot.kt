package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.runconfig.command.AiCopilotType
import cc.unitmesh.devti.runconfig.command.BaseConfig

class AiCopilot(
    methodName: String,
    copilotType: AiCopilotType,
) : BaseConfig() {
    override var configurationName = "Run $copilotType for $methodName"
}
