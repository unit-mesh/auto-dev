package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.runconfig.command.BaseConfig

class FindBugConfigure(
    private val methodName: String,
) : BaseConfig() {
    override var configurationName = "Find Bug"

    init {
        configurationName += " $methodName"
    }
}