package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement

class DevInCompletionContributor : CompletionContributor() {
    private val INPUT_DUMMY_IDENTIFIER = "DevInDummy"

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.LANGUAGE_ID), CodeFenceLanguageProvider())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.VARIABLE_ID), CustomVariableProvider())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.AGENT_ID), BuiltinAgentProvider())
        extend(CompletionType.BASIC, filePattern(), CodeFenceLanguageProvider())
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        if ((context.file is DevInFile) && context.dummyIdentifier === INPUT_DUMMY_IDENTIFIER) {
            context.dummyIdentifier = INPUT_DUMMY_IDENTIFIER
        }
    }

    private inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
        return PlatformPatterns.psiElement(I::class.java)
    }

    private fun baseUsedPattern(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .inside(psiElement<DevInUsed>())

    private fun filePattern(): PsiElementPattern.Capture<PsiElement> =
        baseUsedPattern()
            .withElementType(DevInTypes.COLON)

}
