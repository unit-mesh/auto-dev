package cc.unitmesh.devti.language.compiler.exec

import com.intellij.openapi.project.Project

class RefactorInsCommand(val myProject: Project, private val argument: String) : InsCommand {
    override suspend fun execute(): String? {
        TODO("Not yet implemented")
    }
}