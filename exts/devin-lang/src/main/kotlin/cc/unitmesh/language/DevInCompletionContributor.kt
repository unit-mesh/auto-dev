package cc.unitmesh.language

import cc.unitmesh.language.completion.VariableProvider
import cc.unitmesh.language.psi.DevInFile
import cc.unitmesh.language.psi.DevInTypes
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement

class DevInCompletionContributor : CompletionContributor() {
    private val INPUT_DUMMY_IDENTIFIER = "AutoDevDummy"

    init {
        extend(
            CompletionType.BASIC,
            psiElement(DevInTypes.VARIABLE_ID),
            VariableProvider()
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        if ((context.file is DevInFile) && context.dummyIdentifier === INPUT_DUMMY_IDENTIFIER) {
            context.dummyIdentifier = INPUT_DUMMY_IDENTIFIER
        }
    }
}
