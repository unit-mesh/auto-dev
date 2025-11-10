package cc.unitmesh.agent.tool.gitignore

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

/**
 * iOS implementation of GitIgnoreParser
 */
actual class GitIgnoreParser actual constructor(private val projectRoot: String) {
    private val loader = IosGitIgnoreLoader()
    private val parser = BaseGitIgnoreParser(projectRoot, loader)

    actual fun isIgnored(filePath: String): Boolean {
        return parser.isIgnored(filePath)
    }

    actual fun reload() {
        parser.reload()
    }

    actual fun getPatterns(): List<String> {
        return parser.getPatterns()
    }
}

/**
 * iOS implementation of GitIgnoreLoader using Foundation framework
 */
@OptIn(ExperimentalForeignApi::class)
class IosGitIgnoreLoader : GitIgnoreLoader {
    override fun loadGitIgnoreFile(dirPath: String): String? {
        return try {
            val gitignorePath = "$dirPath/.gitignore"
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(gitignorePath)) {
                NSString.stringWithContentsOfFile(
                    gitignorePath,
                    encoding = NSUTF8StringEncoding,
                    error = null
                ) as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun isDirectory(path: String): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager
            val attrs = fileManager.attributesOfItemAtPath(path, error = null)
            attrs?.get(NSFileType) == NSFileTypeDirectory
        } catch (e: Exception) {
            false
        }
    }

    override fun listDirectories(path: String): List<String> {
        return try {
            val fileManager = NSFileManager.defaultManager
            if (!isDirectory(path)) {
                return emptyList()
            }

            val contents = fileManager.contentsOfDirectoryAtPath(path, error = null) as? List<*>
            contents?.mapNotNull { item ->
                val itemName = item as? String ?: return@mapNotNull null
                val itemPath = "$path/$itemName"
                if (isDirectory(itemPath)) itemPath else null
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun joinPath(vararg components: String): String {
        return components.joinToString("/")
    }

    override fun getRelativePath(base: String, target: String): String {
        return try {
            if (target.startsWith(base)) {
                target.removePrefix(base).removePrefix("/")
            } else {
                target
            }
        } catch (e: Exception) {
            target
        }
    }
}

