package cc.unitmesh.devti.language

import cc.unitmesh.devti.runconfig.DtRunConfiguration
import cc.unitmesh.devti.runconfig.config.DevtiCreateStoryConfigure
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import kotlinx.serialization.Serializable

class DevtiAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {

    }

    companion object {
        val DEVTI_REGEX = Regex("^//\\s+devti://story/(github)/(\\d+)(/.*)?$")

        fun isDevtiComment(comment: String): Boolean {
            return DEVTI_REGEX.matches(comment)
        }

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

@Serializable
class StoryConfig(
    val storyId: Int,
    val storySource: String,
    val acs: List<String> = listOf()
) {
    override fun toString(): String {
        return "StoryConfig(storyId=$storyId, storySource='$storySource', acs=$acs)"
    }
}
