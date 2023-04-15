package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.runconfig.command.CreateStoryConfigurationProducer
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class DisplayStoryLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiComment) return null

        val commentText = element.text
        val regex = DevtiAnnotator.DEVTI_REGEX
        val matchResult = regex.find(commentText) ?: return null

        val state = CreateStoryConfigurationProducer().findConfig(listOf(element)) ?: return null

        val actions = ExecutorAction.getActions(0)
        return Info(
            DevtiIcons.STORY,
            { state.configurationName },
            *actions
        )
    }
}
