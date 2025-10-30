package cc.unitmesh.devins.filesystem

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.streams.toList

/**
 * JVM 平台的文件系统实现
 */
actual class DefaultFileSystem actual constructor(private val projectPath: String) : ProjectFileSystem {
    
    actual override fun getProjectPath(): String? = projectPath
    
    actual override fun readFile(path: String): String? {
        return try {
            val resolvedPath = resolvePathInternal(path)
            if (resolvedPath.exists() && resolvedPath.isRegularFile()) {
                resolvedPath.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual override fun exists(path: String): Boolean {
        return try {
            resolvePathInternal(path).exists()
        } catch (e: Exception) {
            false
        }
    }
    
    actual override fun listFiles(path: String, pattern: String?): List<String> {
        return try {
            val dirPath = resolvePathInternal(path)
            if (!dirPath.exists() || !dirPath.isDirectory()) {
                return emptyList()
            }
            
            val files = Files.list(dirPath).use { stream ->
                stream.filter { it.isRegularFile() }
                    .map { it.fileName.toString() }
                    .toList()
            }
            
            if (pattern != null) {
                val regex = pattern.replace("*", ".*").replace("?", ".").toRegex()
                files.filter { regex.matches(it) }
            } else {
                files
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    actual override fun resolvePath(relativePath: String): String {
        return resolvePathInternal(relativePath).toString()
    }
    
    private fun resolvePathInternal(path: String): Path {
        val p = Path.of(path)
        return if (p.isAbsolute) {
            p
        } else {
            Path.of(projectPath, path)
        }
    }
}

