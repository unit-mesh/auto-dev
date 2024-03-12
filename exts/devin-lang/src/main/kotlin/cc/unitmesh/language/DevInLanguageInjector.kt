package cc.unitmesh.language

import cc.unitmesh.language.parser.CodeBlockElement
import cc.unitmesh.language.psi.DevInTypes
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.elementType
import org.intellij.plugins.markdown.injection.aliases.CodeFenceLanguageGuesser

class DevInLanguageInjector : LanguageInjector {
    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, registrar: InjectedLanguagePlaces) {
        if (host !is CodeBlockElement || !host.isValidHost()) {
            return
        }

        val hasContentsElement = host.children.any { it.elementType == DevInTypes.CODE_CONTENTS }
        if (!hasContentsElement) {
            return
        }

        val languageIdentifier = host.getLanguageId()
        val text = languageIdentifier?.text ?: return
        val language = CodeFenceLanguageGuesser.guessLanguageForInjection(text) ?: return

        val elements = host.getContents()
        if (elements.size < 2) {
            return
        }

        injectAsOnePlace(host, language, registrar)
    }

    private fun injectAsOnePlace(host: CodeBlockElement, language: Language, registrar: InjectedLanguagePlaces) {
        val elements = CodeBlockElement.obtainFenceContent(host, withWhitespaces = true) ?: return

        val first = elements.first()
        val last = elements.last()

        val textRange = TextRange.create(first.startOffsetInParent, last.startOffsetInParent + last.textLength)
        registrar.addPlace(language, textRange, null, null)
    }
}
