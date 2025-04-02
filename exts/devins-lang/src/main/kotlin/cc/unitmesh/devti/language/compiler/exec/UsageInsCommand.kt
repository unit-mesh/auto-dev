package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.devti.provider.devins.DevInsSymbolProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

class UsageInsCommand(val myProject: Project, private val symbol: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.RELATED

    override suspend fun execute(): String? {
        val elements = DevInsSymbolProvider.all().map {
            runReadAction { it.resolveElement(myProject, symbol) }
        }.flatten()

        if (elements.isEmpty()) return "$DEVINS_ERROR: No symbol found for $symbol"

        val psiElements = elements.mapNotNull {
            RelatedClassesProvider.provide(it.language)?.lookupCaller(myProject, it)
        }.flatten()

        if (psiElements.isEmpty()) return "$DEVINS_ERROR: No usage found for $symbol"

        return "Here is related to $symbol usage" + psiElements.joinToString("\n\n") {
            runReadAction { it.text }
        }
    }
}
