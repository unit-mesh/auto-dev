package cc.unitmesh.devti.context.modifier

import cc.unitmesh.devti.context.builder.CodeModifier
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension

class CodeModifierProvider {
    private val languageExtension = LanguageExtension<CodeModifier>("cc.unitmesh.codeModifier")
    private val providers: List<CodeModifier>

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull(languageExtension::forLanguage)
    }

    fun modifier(lang: Language): CodeModifier? = providers.find { it.isApplicable(lang) }
}
