package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.prompting.CodePromptText
import cc.unitmesh.devti.provider.PromptStrategy
import com.intellij.psi.PsiElement

class JavaScriptPromptStrategyAdvisor : PromptStrategy() {
    override fun advice(prefixCode: String, suffixCode: String): CodePromptText {
        TODO()
    }

    override fun advice(psiFile: PsiElement, calleeName: String): CodePromptText {
        TODO()
    }

    override fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): CodePromptText {
        TODO()
    }
}
