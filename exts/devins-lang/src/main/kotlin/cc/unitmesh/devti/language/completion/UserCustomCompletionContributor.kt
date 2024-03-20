package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.completion.provider.CustomCommandCompletion
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns

class UserCustomCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.COMMAND_ID), CustomCommandCompletion())
    }
}
