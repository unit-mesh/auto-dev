package cc.unitmesh.devti.language.contributor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.language.DevtiAnnotator
import cc.unitmesh.devti.runconfig.command.FeatureConfigurationProducer
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class AutoDevFeatureMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiComment) return null

        val commentText = element.text
        if (!commentText.startsWith("// devti://")) return null

        if (!DevtiAnnotator.isAutoCRUD(commentText)) return null

        val state = FeatureConfigurationProducer().findConfig(listOf(element)) ?: return null

        val actions = ExecutorAction.getActions(0)
        return Info(
            AutoDevIcons.STORY,
            { state.configurationName },
            *actions
        )
    }
}
