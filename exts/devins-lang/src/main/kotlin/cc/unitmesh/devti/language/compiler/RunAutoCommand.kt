package cc.unitmesh.devti.language.compiler

import com.intellij.openapi.project.Project

class RunAutoCommand(val myProject: Project, val prop: String) : AutoCommand {
    override fun execute(): String? {
        return null
    }
}
