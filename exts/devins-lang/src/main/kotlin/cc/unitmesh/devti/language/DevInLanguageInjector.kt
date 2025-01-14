// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.language

import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguage
import cc.unitmesh.devti.language.parser.CodeBlockElement
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.elementType

class DevInLanguageInjector : LanguageInjector {
    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, registrar: InjectedLanguagePlaces) {
        if (host !is CodeBlockElement || !host.isValidHost()) return

        val hasCodeContents = host.children.any { it.elementType == DevInTypes.CODE_CONTENTS }
        if (!hasCodeContents) return

        val text = host.getLanguageId()?.text ?: return
        val language = findLanguage(text)

        val contentList = CodeBlockElement.obtainFenceContent(host) ?: return
        if (contentList.isEmpty()) return

        injectAsOnePlace(host, language, registrar)
    }

    private fun injectAsOnePlace(host: CodeBlockElement, language: Language, registrar: InjectedLanguagePlaces) {
        val elements = CodeBlockElement.obtainFenceContent(host) ?: return

        val first = elements.first()
        val last = elements.last()

        try {
            val textRange = TextRange.create(first.startOffsetInParent, last.startOffsetInParent + last.textLength)
            registrar.addPlace(language, textRange, null, null)
        } catch (e: Exception) {
            logger<DevInLanguageInjector>().warn("Failed to inject language", e)
        }
    }
}
