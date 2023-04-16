package cc.unitmesh.devti.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class DevtiAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {

    }

    companion object {
        val DEVTI_REGEX = Regex("^//\\sdevti://story/(github)/(\\d+)(/.*)?$")

        // examples: // devti://story/github/1234
        fun matchByString(input: String): StoryConfig? {
            val matchResult = DEVTI_REGEX.find(input)
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

class StoryConfig(
    val storyId: Int,
    val storySource: String,
    val acs: List<String> = listOf()
)
