package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.runconfig.command.FindBugConfigurationProducer
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class FindBugLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiMethod) return null
        val actions = ExecutorAction.getActions(0)
        val state = FindBugConfigurationProducer().findConfig(listOf(element)) ?: return null
        return Info(
            DevtiIcons.STORY,
            { state.configurationName },
            *actions
        )
    }

}
