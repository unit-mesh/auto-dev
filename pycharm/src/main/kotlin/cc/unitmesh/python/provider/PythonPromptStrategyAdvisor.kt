package cc.unitmesh.python.provider

import cc.unitmesh.devti.prompting.CodePromptText
import cc.unitmesh.devti.provider.PromptStrategy
import com.intellij.psi.PsiElement

class PythonPromptStrategyAdvisor : PromptStrategy() {
    override fun advice(prefixCode: String, suffixCode: String): CodePromptText {
        return CodePromptText("", "")
    }

    override fun advice(psiFile: PsiElement, calleeName: String): CodePromptText {
        return CodePromptText("", "")
    }

    override fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): CodePromptText {
        return CodePromptText("", "")
    }
}
