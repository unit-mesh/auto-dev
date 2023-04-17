package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.runconfig.command.AutoCRUDConfigurationProducer
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class AutoCRUDLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiComment) return null

        val commentText = element.text
        if (!commentText.startsWith("// devti://")) return null

        if (!DevtiAnnotator.isAutoCRUD(commentText)) return null

        val state = AutoCRUDConfigurationProducer().findConfig(listOf(element)) ?: return null

        val actions = ExecutorAction.getActions(0)
        return Info(
            DevtiIcons.STORY,
            { state.configurationName },
            *actions
        )
    }
}
