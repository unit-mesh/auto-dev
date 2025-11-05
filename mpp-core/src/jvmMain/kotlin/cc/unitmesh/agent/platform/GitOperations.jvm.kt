package cc.unitmesh.agent.platform

import cc.unitmesh.agent.logging.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * JVM 平台的 Git 操作实现
 * 
 * 使用 ProcessBuilder 调用系统 git 命令
 */
actual class GitOperations actual constructor(private val projectPath: String) {

    private val logger = getLogger("GitOperations")

    actual suspend fun getModifiedFiles(): List<String> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("git", "diff", "--name-only")
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val files = output.trim().split("\n").filter { it.isNotBlank() }

            if (files.isNotEmpty()) {
                logger.info { "Modified: ${files.map { it.split("/").last() }.joinToString(", ")}" }
            } else {
                logger.debug { "No modifications detected" }
            }

            files
        } catch (e: Exception) {
            logger.warn(e) { "Git check failed: ${e.message}" }
            emptyList()
        }
    }
    
    actual suspend fun getFileDiff(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("git", "diff", "--", filePath)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (output.isNotBlank()) {
                output
            } else {
                null
            }
        } catch (e: Exception) {
            // Silently skip
            null
        }
    }
    
    actual fun isSupported(): Boolean = true
}
