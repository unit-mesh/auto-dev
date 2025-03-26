package cc.unitmesh.devti.sketch.rule

import cc.unitmesh.devti.bridge.knowledge.lookupFile
import cc.unitmesh.devti.sketch.ui.patch.readText
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile

@Service(Service.Level.PROJECT)
class ProjectRule(private val project: Project) {
    companion object {
        const val RULE_PATH = "prompts/rules"
    }

    /**
     * Get rule content by filename
     */
    fun getRuleContent(filename: String): String? {
        val fullname = "$RULE_PATH/$filename.md"
        val file = project.lookupFile(fullname)

        if (file != null) {
            val content = file.readText()
            return "<user-rule>\n$content\n</user-rule>"
        }

        return file
    }

    fun hasRule(filename: String): Boolean {
        val fullname = "$RULE_PATH/$filename.md"
        val file = project.lookupFile(fullname)
        return file != null
    }

    /**
     * Get all available rules
     */
    fun getAllRules(): List<VirtualFile> {
        val ruleDir = project.guessProjectDir()?.findFileByRelativePath(RULE_PATH) ?: return emptyList()
        return ruleDir.children.filter { it.isFile && (it.extension == "md") }
    }
}