package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.provider.devins.DevInsSymbolProvider
import com.intellij.openapi.project.Project

class SymbolInsCommand(val myProject: Project, val prop: String) :
    InsCommand {
    override fun execute(): String {
        val result = DevInsSymbolProvider.all().mapNotNull {
            val found = it.resolveSymbol(myProject, prop)
            if (found.isEmpty()) return@mapNotNull null
            "```${it.language}\n${found.joinToString("\n")}\n```\n"
        }

        if (result.isEmpty()) {
            return "<DevInsError> No symbol found: $prop"
        }

        return result.joinToString("\n")
    }
}