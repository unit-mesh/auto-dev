package cc.unitmesh.devti.language

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

public const val SHIRE_CHAT_BOX_FILE = "shire-chatbox.default.shire"
public const val SHIRE_TEMP_OUTPUT = ".shire-output"
public const val LLM_LOGGING_JSONL = "logging.jsonl"
public const val LLM_LOGGING = "logging.log"
public const val SHIRE_MKT_HOST = "https://shire.run/packages.json"

object ShireConstants {
    fun outputDir(project: Project): VirtualFile? {
        val baseDir = project.guessProjectDir() ?: throw IllegalStateException("Project directory not found")
        val virtualFile = baseDir.findFileByRelativePath(SHIRE_TEMP_OUTPUT)
        if (virtualFile == null) {
            baseDir.createChildDirectory(this, SHIRE_TEMP_OUTPUT)
        }

        return virtualFile
    }
}

