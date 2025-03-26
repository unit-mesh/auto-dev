package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.sketch.ui.patch.readText
import com.intellij.openapi.project.Project

class RuleInsCommand(val myProject: Project, private val filename: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.OPEN

    override suspend fun execute(): String? {
        val fullname = "prompts/rule/$filename.md"
        val file = myProject.lookupFile(fullname)

        if (file != null) {
            return file.readText()
        }

        return "$DEVINS_ERROR rule file not found: $fullname"
    }
}
