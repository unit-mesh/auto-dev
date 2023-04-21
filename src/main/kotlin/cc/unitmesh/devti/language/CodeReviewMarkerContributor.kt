package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.actions.CodeReviewAction
import cc.unitmesh.devti.runconfig.AutoCRUDState
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class CodeReviewMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiIdentifier) return null
        val method = element.parent
        if (method !is PsiMethod) return null

        val methodName = method.name
        val runAction = CodeReviewAction(methodName, method)

        return Info(
            DevtiIcons.AI_COPILOT,
            { "Find Bug" },
            runAction
        )
    }
}
