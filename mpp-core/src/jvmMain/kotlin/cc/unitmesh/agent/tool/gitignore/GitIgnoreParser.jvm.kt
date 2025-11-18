package cc.unitmesh.agent.tool.gitignore

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

/**
 * JVM platform GitIgnore parser implementation
 */
actual class GitIgnoreParser actual constructor(private val projectRoot: String) {
    private val loader = JvmGitIgnoreLoader()
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
 * JVM implementation of GitIgnoreLoader using Java NIO
 */
class JvmGitIgnoreLoader : GitIgnoreLoader {
    override fun loadGitIgnoreFile(dirPath: String): String? {
        return try {
            val gitignorePath = Paths.get(dirPath, ".gitignore")
            if (gitignorePath.exists() && !gitignorePath.isDirectory()) {
                gitignorePath.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isDirectory(path: String): Boolean {
        return try {
            val p = Paths.get(path)
            p.exists() && p.isDirectory()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun listDirectories(path: String): List<String> {
        return try {
            val p = Paths.get(path)
            if (!p.exists() || !p.isDirectory()) {
                return emptyList()
            }
            
            Files.list(p).use { stream ->
                stream
                    .filter { it.isDirectory() }
                    .map { it.toString() }
                    .toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun joinPath(vararg components: String): String {
        return Paths.get("", *components).toString()
    }
    
    override fun getRelativePath(base: String, target: String): String {
        return try {
            val basePath = Paths.get(base)
            val targetPath = Paths.get(target)
            basePath.relativize(targetPath).toString()
        } catch (e: Exception) {
            target
        }
    }
}

