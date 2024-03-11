package cc.unitmesh.language

import cc.unitmesh.language.completion.VariableProvider
import cc.unitmesh.language.psi.DevInTypes
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement

class DevInCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, psiElement(DevInTypes.IDENTIFIER), VariableProvider())
    }
}
