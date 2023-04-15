package cc.unitmesh.silvery.language

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NotNull
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl;

//import org.intellij.sdk.language.psi.DevtiProperty;


class AnalysiStoryMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        @NotNull element: PsiElement,
        @NotNull result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>
    ) {
        // This must be an element with a literal expression as a parent
        if (element !is PsiJavaTokenImpl || element.parent !is PsiLiteralExpression) {
            return
        }

        // The literal expression must start with the Devti language literal expression
        val literalExpression: PsiLiteralExpression = element.parent as PsiLiteralExpression
        val value: String? = if (literalExpression.value is String) literalExpression.value as String else null
        if (value == null ||
            !value.startsWith(DevtiAnnotator.DEVTI_PREFIX_STR + DevtiAnnotator.DEVTI_SEPARATOR_STR)
        ) {
            return
        }

        // Get the Devti language property usage
//        val project: Project = element.project
//        val possibleProperties: String = value.substring(
//            DevtiAnnotator.DEVTI_PREFIX_STR.length + DevtiAnnotator.DEVTI_SEPARATOR_STR.length
//        )
//        val properties: List<DevtiProperty> = DevtiUtil.findProperties(project, possibleProperties)
//        if (properties.size > 0) {
//            // Add the property to a collection of line marker info
//            val builder = NavigationGutterIconBuilder.create(DevtiIcons.FILE)
//                .setTargets(properties)
//                .setTooltipText("Navigate to Devti language property")
//            result.add(builder.createLineMarkerInfo(element))
//        }
    }
}