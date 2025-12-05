package cc.unitmesh.devti.sketch.rule

import cc.unitmesh.devti.bridge.knowledge.lookupFile
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.sketch.ui.patch.readText
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class ProjectRule(private val project: Project) {
    val RULE_PATH = project.coderSetting.state.teamPromptsDir + "/" + "rules"

    /**
     * Get rule content by filename
     */
    fun getRuleContent(contextFileName: String): String? {
        val fullname = "$RULE_PATH/$contextFileName.md"
        val file = project.lookupFile(fullname)

        if (file != null) {
            val content = file.readText()
            return "<user-rule>\n$content\n</user-rule>"
        }

        val devinFile = project.lookupFile("$RULE_PATH/$contextFileName.devin")
        if (devinFile != null) {
            val content = devinFile.readText()
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
        return ruleDir.children.filter { it.extension == "md" || it.extension == "devin" }
    }
}