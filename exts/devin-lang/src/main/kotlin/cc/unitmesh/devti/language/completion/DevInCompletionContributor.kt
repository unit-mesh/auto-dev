package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.*
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ProcessingContext

class DevInCompletionContributor : CompletionContributor() {
    private val INPUT_DUMMY_IDENTIFIER = "DevInDummy"

    init {
        extend(CompletionType.BASIC, declarationPattern(), CodeLanguageProvider())
        extend(CompletionType.BASIC, psiElement(DevInTypes.VARIABLE_ID), CustomVariableProvider())
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        if ((context.file is DevInFile) && context.dummyIdentifier === INPUT_DUMMY_IDENTIFIER) {
            context.dummyIdentifier = INPUT_DUMMY_IDENTIFIER
        }
    }

    fun declarationPattern(): PsiElementPattern.Capture<PsiElement> =
        psiElement()
            .and(psiElement(DevInTypes.LANGUAGE_ID))
}
