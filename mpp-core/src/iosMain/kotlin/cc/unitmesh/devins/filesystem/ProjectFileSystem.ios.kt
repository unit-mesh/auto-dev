package cc.unitmesh.devins.filesystem

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import platform.Foundation.*
import platform.posix.getcwd

/**
 * iOS implementation of DefaultFileSystem
 * Uses Foundation framework for file operations
 */
@OptIn(ExperimentalForeignApi::class)
actual class DefaultFileSystem actual constructor(
    private val projectPath: String
) : ProjectFileSystem {

    actual override fun getProjectPath(): String? = projectPath

    actual override fun readFile(path: String): String? {
        val fullPath = resolvePath(path)
        return try {
            NSString.stringWithContentsOfFile(
                fullPath,
                encoding = NSUTF8StringEncoding,
                error = null
            ) as? String
        } catch (e: Exception) {
            null
        }
    }

    actual override fun readFileAsBytes(path: String): ByteArray? {
        val fullPath = resolvePath(path)
        return try {
            val data = NSData.dataWithContentsOfFile(fullPath) ?: return null
            val length = data.length.toInt()
            val bytes = data.bytes?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null
            ByteArray(length) { i ->
                bytes[i]
            }
        } catch (e: Exception) {
            null
        }
    }

    actual override fun writeFile(path: String, content: String): Boolean {
        val fullPath = resolvePath(path)
        return try {
            val nsString = content as NSString
            nsString.writeToFile(
                fullPath,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    actual override fun exists(path: String): Boolean {
        val fullPath = resolvePath(path)
        return NSFileManager.defaultManager.fileExistsAtPath(fullPath)
    }

    actual override fun isDirectory(path: String): Boolean {
        val fullPath = resolvePath(path)
        val fileManager = NSFileManager.defaultManager
        val attrs = fileManager.attributesOfItemAtPath(fullPath, error = null)
        return attrs?.get(NSFileType) == NSFileTypeDirectory
    }

    actual override fun listFiles(path: String, pattern: String?): List<String> {
        val fullPath = resolvePath(path)
        val fileManager = NSFileManager.defaultManager

        val contents = fileManager.contentsOfDirectoryAtPath(fullPath, error = null) as? List<*>
        val files = contents?.mapNotNull { it as? String } ?: emptyList()

        return if (pattern != null) {
            files.filter { it.contains(pattern) }
        } else {
            files
        }
    }

    actual override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
        val results = mutableListOf<String>()
        searchFilesRecursive(projectPath, pattern, 0, maxDepth, maxResults, results)
        return results
    }

    private fun searchFilesRecursive(
        currentPath: String,
        pattern: String,
        currentDepth: Int,
        maxDepth: Int,
        maxResults: Int,
        results: MutableList<String>
    ) {
        if (currentDepth > maxDepth || results.size >= maxResults) {
            return
        }

        val fileManager = NSFileManager.defaultManager
        val contents = fileManager.contentsOfDirectoryAtPath(currentPath, error = null) as? List<*>
        
        contents?.forEach { item ->
            val itemName = item as? String ?: return@forEach
            val itemPath = "$currentPath/$itemName"
            
            if (isDirectory(itemPath)) {
                searchFilesRecursive(itemPath, pattern, currentDepth + 1, maxDepth, maxResults, results)
            } else if (itemName.contains(pattern)) {
                results.add(itemPath.removePrefix("$projectPath/"))
                if (results.size >= maxResults) {
                    return
                }
            }
        }
    }

    actual override fun resolvePath(relativePath: String): String {
        return if (relativePath.startsWith("/")) {
            relativePath
        } else {
            "$projectPath/$relativePath"
        }
    }

    actual override fun createDirectory(path: String): Boolean {
        val fullPath = resolvePath(path)
        return try {
            NSFileManager.defaultManager.createDirectoryAtPath(
                fullPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

