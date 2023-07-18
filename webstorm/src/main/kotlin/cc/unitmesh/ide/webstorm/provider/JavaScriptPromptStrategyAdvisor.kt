package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.prompting.model.FinalCodePrompt
import cc.unitmesh.devti.provider.PromptStrategy
import com.intellij.psi.PsiElement

class JavaScriptPromptStrategyAdvisor : PromptStrategy {
    override fun advice(prefixCode: String, suffixCode: String): FinalCodePrompt {
        TODO()
    }

    override fun advice(psiFile: PsiElement, calleeName: String): FinalCodePrompt {
        TODO()
    }

    override fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): FinalCodePrompt {
        TODO()
    }
}
