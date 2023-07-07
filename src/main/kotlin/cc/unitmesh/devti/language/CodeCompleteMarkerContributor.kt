package cc.unitmesh.devti.language

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.actions.CodeCompleteAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class CodeCompleteMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        // should be search from leaf element
        if (element !is PsiIdentifier) return null
        val method = element.parent
        if (method !is PsiMethod) return null

        val methodName = method.name

        val runAction = CodeCompleteAction(methodName, method)

        return Info(
            AutoDevIcons.AI_COPILOT,
            { "Code Complete " },
            runAction
        )
    }
}

