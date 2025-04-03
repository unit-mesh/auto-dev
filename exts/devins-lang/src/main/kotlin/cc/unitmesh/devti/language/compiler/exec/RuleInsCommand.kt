package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.sketch.rule.ProjectRule
import com.intellij.openapi.project.Project

class RuleInsCommand(val myProject: Project, private val filename: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.OPEN

    override suspend fun execute(): String? {
        val projectRule = ProjectRule(myProject)
        return projectRule.getRuleContent(filename) ?: "$DEVINS_ERROR rule file not found: $filename"
    }
}
