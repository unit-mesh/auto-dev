package cc.unitmesh.genius.actions.context

import cc.unitmesh.devti.template.DockerfileContext

data class DevOpsContext(
    val buildContext: String,
) {
    companion object {
        fun from(dockerContexts: List<DockerfileContext>): DevOpsContext {
            val string = dockerContexts.joinToString("\n") {
                val build = "- Build tool name: ${it.buildToolName}, Build tool version: ${it.buildToolVersion}\n"
                val language = "- Language name: ${it.languageName}, Language version: ${it.languageVersion}\n"
                val task = "- Build tool Task list: ${it.taskString}\n"
                build + language + task
            }
            return DevOpsContext(string)
        }
    }
}