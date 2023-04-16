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
        if (!commentText.startsWith("// devti://")) return null
        // remove //\s+ with regex
        val commentTextWithoutPrefix = commentText.replace(Regex("^//\\s+"), "")

        val storyConfig = DevtiAnnotator.matchByString(commentTextWithoutPrefix) ?: return null

        val state = CreateStoryConfigurationProducer().findConfig(listOf(element)) ?: return null
        state.storyConfig = storyConfig

        val actions = ExecutorAction.getActions(0)
        return Info(
            DevtiIcons.STORY,
            { state.configurationName },
            *actions
        )
    }
}
