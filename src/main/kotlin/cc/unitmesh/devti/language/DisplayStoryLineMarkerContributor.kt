package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class DisplayStoryLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        // a valid Devti property should be a comment, and the comment format should be like "devti://story/1102/{AC1,AC2}"
        if (element !is PsiComment) return null

        val commentText = element.text
        // use regex to match devti://story/1102/{AC1,AC2}
        val regex = DevtiAnnotator.DEVTI_REGEX
        val matchResult = regex.find(commentText) ?: return null

        val actions = ExecutorAction.getActions(0)
        return Info(
            DevtiIcons.STORY,
            { "Display Story" },
            *actions
        )
    }
}
