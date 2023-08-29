package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.flow.model.StoryConfig
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class DevtiAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) = Unit

    companion object {
        val AutoCRUDRegex = Regex("^//\\s+devti://story/(github|gitlab)/(\\d+)(/.*)?$")

        fun isAutoCRUD(comment: String): Boolean {
            return AutoCRUDRegex.matches(comment)
        }

        fun matchByString(input: String): StoryConfig? {
            val matchResult = AutoCRUDRegex.find(input)
            if (matchResult != null) {
                val (storySource, storyIdStr, acs) = matchResult.destructured
                val storyId = storyIdStr.toIntOrNull()
                if (storyId != null) {
                    val acList = acs.split(",").filter { it.isNotEmpty() }
                    return StoryConfig(storyId, storySource, acList)
                }
            }
            return null
        }
    }
}

