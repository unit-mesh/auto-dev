package cc.unitmesh.agent.platform

/**
 * iOS implementation of GitOperations
 * Git operations are not supported on iOS
 */
actual class GitOperations actual constructor(private val projectPath: String) {
    actual suspend fun getModifiedFiles(): List<String> {
        println("Git operations not supported on iOS")
        return emptyList()
    }

    actual suspend fun getFileDiff(filePath: String): String? {
        return null
    }

    actual fun isSupported(): Boolean = false
}

