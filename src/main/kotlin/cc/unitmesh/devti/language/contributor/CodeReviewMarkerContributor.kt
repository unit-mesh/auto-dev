package cc.unitmesh.devti.language.contributor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.actions.CodeSuggestionAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class CodeReviewMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiIdentifier) return null
        val method = element.parent
        if (method !is PsiMethod) return null

        val methodName = method.name
        val runAction = CodeSuggestionAction(methodName, method)

        return Info(
            AutoDevIcons.AI_COPILOT,
            { "Find Bug" },
            runAction
        )
    }
}
