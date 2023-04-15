package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NotNull

class DisplayStoryMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        @NotNull element: PsiElement,
        @NotNull result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>
    ) {
        // a valid Devti property should be a comment, and the comment format should be like "devti://story/1102/{AC1,AC2}"
        if (element is PsiComment) {
            val commentText = element.text
            // use regex to match devti://story/1102/{AC1,AC2}
            val regex = DevtiAnnotator.DEVTI_REGEX
            val matchResult = regex.find(commentText)
            if (matchResult != null) {
                val builder = NavigationGutterIconBuilder.create(DevtiIcons.FILE).setTargets(element)
                    .setTooltipText("Display Story")
                result.add(builder.createLineMarkerInfo(element))
            }
        }
    }
}