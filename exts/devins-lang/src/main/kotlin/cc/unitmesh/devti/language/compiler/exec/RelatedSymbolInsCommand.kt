package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.devti.provider.devins.DevInsSymbolProvider
import com.intellij.openapi.project.Project

class RelatedSymbolInsCommand(val myProject: Project, private val symbol: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.RELATED

    override suspend fun execute(): String? {
        val elements = DevInsSymbolProvider.all().map {
            it.resolveElement(myProject, symbol)
        }.flatten()

        if (elements.isEmpty()) return null

        val psiElements = elements.mapNotNull {
            RelatedClassesProvider.provide(it.language)?.lookup(it)
        }.flatten()

        if (psiElements.isEmpty()) return null

        return psiElements.joinToString("\n") { it.text }
    }
}
