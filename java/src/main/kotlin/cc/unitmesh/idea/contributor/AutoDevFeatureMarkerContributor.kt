package cc.unitmesh.idea.contributor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.runconfig.DevtiAnnotator
import cc.unitmesh.devti.runconfig.command.AutoDevFeatureConfigurationProducer
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.lang.LanguageCommenters
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class AutoDevFeatureMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiComment) return null

        val commentText = element.text

        val commenter = LanguageCommenters.INSTANCE.forLanguage(element.language)
        val commentPrefix = commenter?.lineCommentPrefix

        if (!commentText.startsWith("$commentPrefix devti://")) return null

        if (!DevtiAnnotator.isAutoCRUD(commentText)) return null

        val state = AutoDevFeatureConfigurationProducer().findConfig(listOf(element)) ?: return null

        val actions = ExecutorAction.getActions(0)
        return Info(
            AutoDevIcons.STORY,
            { state.configurationName },
            *actions
        )
    }
}
