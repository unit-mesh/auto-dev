package cc.unitmesh.devti.custom.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.serialization.Serializable

@Serializable
class CustomIntentionConfig {
    var title: String = ""
    var autoInvoke: Boolean = false
    var matchRegex: String = ""
    var template: String = ""
    val priority: Int = 0
    var selectedRegex: String = ""

    /**
     * batchFileRegex represents a regular expression used for matching files in batch mode.
     * When this regex is set, it will be used to determine which files should be included
     * in batch operations, such as file processing or mass operations.
     * The regex should be a valid regular expression string according to the regex
     * capabilities of the host environment.
     * It is expected that this variable will be set with an appropriate regex pattern
     * before batch operations are initiated.
     */
    var batchFileRegex: String = ""

    /// glob mode
    fun batchFiles(project: Project): List<VirtualFile> {
        val regex = Regex(batchFileRegex)
        return project.guessProjectDir()?.children?.filter { regex.matches(it.name) } ?: emptyList()
    }
}