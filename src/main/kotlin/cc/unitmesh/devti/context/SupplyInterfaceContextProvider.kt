package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.builder.SupplyInterfaceContextBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project

class SupplyInterfaceContextProvider {
    private val languageExtension: LanguageExtension<SupplyInterfaceContextBuilder> =
        LanguageExtension("cc.unitmesh.externalInterfaceBuilder")

    private val providers: List<SupplyInterfaceContextBuilder>

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull(languageExtension::forLanguage)
    }

    fun from(project: Project): SupplyInterfaceContext? {
        for (provider in providers) {
            val supplyInterfaceContext = provider.getSupplyInterfaceContext(project)
            if (supplyInterfaceContext != null) {
                return supplyInterfaceContext
            }
        }

        return null
    }
}