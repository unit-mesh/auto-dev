package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.runconfig.command.AiCopilotConfigurationProducer
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class FindBugLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiIdentifier) return null
        val parent = element.parent
        if (parent !is PsiMethod) return null

        val actions = ExecutorAction.getActions(0)
        val state = AiCopilotConfigurationProducer().findConfig(listOf(element)) ?: return null
        return Info(
            DevtiIcons.AI_COPILOT,
            { state.configurationName },
            *actions
        )
    }

}
