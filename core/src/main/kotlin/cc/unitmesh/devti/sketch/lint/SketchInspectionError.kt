package cc.unitmesh.devti.sketch.lint

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

data class SketchInspectionError(
    val lineNumber: Int,
    val description: String,
    val highlightType: ProblemHighlightType,
    val toolId: String?,
) {
    companion object {
        fun from(problemDescriptor: ProblemDescriptor, toolwrapper: LocalInspectionToolWrapper): SketchInspectionError {
            return SketchInspectionError(
                problemDescriptor.lineNumber,
                problemDescriptor.descriptionTemplate,
                problemDescriptor.highlightType,
                toolwrapper.id
            )
        }
    }
}