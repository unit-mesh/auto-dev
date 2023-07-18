package cc.unitmesh.ide.pycharm.provider

import cc.unitmesh.devti.prompting.model.FinalCodePrompt
import cc.unitmesh.devti.provider.PromptStrategy
import com.intellij.psi.PsiElement

class PythonPromptStrategyAdvisor : PromptStrategy() {
    override fun advice(prefixCode: String, suffixCode: String): FinalCodePrompt {
        return FinalCodePrompt("", "")
    }

    override fun advice(psiFile: PsiElement, calleeName: String): FinalCodePrompt {
        return FinalCodePrompt("", "")
    }

    override fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): FinalCodePrompt {
        return FinalCodePrompt("", "")
    }
}
