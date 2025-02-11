package cc.unitmesh.devti.sketch.lint

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType

data class SketchInspectionError(
    val lineNumber: Int,
    val description: String,
    val highlightType: ProblemHighlightType,
) {
    companion object {
        fun from(problemDescriptor: ProblemDescriptor): SketchInspectionError {
            return SketchInspectionError(
                problemDescriptor.lineNumber,
                problemDescriptor.descriptionTemplate,
                problemDescriptor.highlightType
            )
        }
    }
}