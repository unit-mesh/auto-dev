package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.completion.provider.CustomCommandCompletion
import cc.unitmesh.devti.language.completion.provider.ToolchainCommandCompletion
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

class UserCustomCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.COMMAND_ID), CustomCommandCompletion())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.COMMAND_ID), ToolchainCommandCompletion())

        extend(CompletionType.BASIC, identifierAfter(DevInTypes.COMMAND_START), CustomCommandCompletion())
        extend(CompletionType.BASIC, identifierAfter(DevInTypes.COMMAND_START), ToolchainCommandCompletion())
    }

    private fun identifierAfter(type: IElementType): ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement(DevInTypes.IDENTIFIER)
            .afterLeaf(PlatformPatterns.psiElement().withElementType(type))
}
