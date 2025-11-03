package cc.unitmesh.agent.tool.gitignore

/**
 * JavaScript platform GitIgnore parser implementation
 */
actual class GitIgnoreParser actual constructor(private val projectRoot: String) {
    private val loader = JsGitIgnoreLoader()
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
 * JavaScript implementation of GitIgnoreLoader using Node.js fs module
 */
class JsGitIgnoreLoader : GitIgnoreLoader {
    private val isNodeJs: Boolean = js("typeof process !== 'undefined' && process.versions && process.versions.node") as Boolean
    private val fs = if (isNodeJs) js("require('fs')") else null
    private val path = if (isNodeJs) js("require('path')") else null
    
    override fun loadGitIgnoreFile(dirPath: String): String? {
        if (!isNodeJs || fs == null || path == null) {
            return null
        }
        
        return try {
            val gitignorePath = js("path.join(dirPath, '.gitignore')") as String
            val exists = js("fs.existsSync(gitignorePath)") as Boolean
            
            if (exists) {
                val stats = js("fs.statSync(gitignorePath)")
                val isFile = js("stats.isFile()") as Boolean
                
                if (isFile) {
                    js("fs.readFileSync(gitignorePath, 'utf-8')") as String
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isDirectory(path: String): Boolean {
        if (!isNodeJs || fs == null) {
            return false
        }
        
        return try {
            val exists = js("fs.existsSync(path)") as Boolean
            if (exists) {
                val stats = js("fs.statSync(path)")
                js("stats.isDirectory()") as Boolean
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override fun listDirectories(path: String): List<String> {
        if (!isNodeJs || fs == null || this.path == null) {
            return emptyList()
        }
        
        return try {
            val exists = js("fs.existsSync(path)") as Boolean
            if (!exists) {
                return emptyList()
            }
            
            val stats = js("fs.statSync(path)")
            val isDir = js("stats.isDirectory()") as Boolean
            if (!isDir) {
                return emptyList()
            }
            
            val entries = js("fs.readdirSync(path)") as Array<String>
            val directories = mutableListOf<String>()
            
            for (entry in entries) {
                val fullPath = js("this.path.join(path, entry)") as String
                if (isDirectory(fullPath)) {
                    directories.add(fullPath)
                }
            }
            
            directories
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun joinPath(vararg components: String): String {
        if (!isNodeJs || path == null) {
            return components.joinToString("/")
        }
        
        return try {
            val args = components
            js("path.join.apply(path, args)") as String
        } catch (e: Exception) {
            components.joinToString("/")
        }
    }
    
    override fun getRelativePath(base: String, target: String): String {
        if (!isNodeJs || path == null) {
            // Simple fallback implementation
            return if (target.startsWith(base)) {
                target.removePrefix(base).removePrefix("/")
            } else {
                target
            }
        }
        
        return try {
            js("path.relative(base, target)") as String
        } catch (e: Exception) {
            target
        }
    }
}

