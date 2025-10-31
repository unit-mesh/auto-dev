@file:JsExport

package cc.unitmesh.devins.filesystem

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * JavaScript-friendly wrapper for ProjectFileSystem
 * Ensures the filesystem classes are properly exported to JavaScript
 */
@JsExport
class JsFileSystemFactory {
    companion object {
        /**
         * Create a new DefaultFileSystem instance
         * @param projectPath The root path of the project
         * @return A new DefaultFileSystem instance
         */
        @JsName("createFileSystem")
        fun createFileSystem(projectPath: String): ProjectFileSystem {
            return DefaultFileSystem(projectPath)
        }
        
        /**
         * Create an empty file system (for testing)
         * @return An empty file system instance
         */
        @JsName("createEmptyFileSystem")
        fun createEmptyFileSystem(): ProjectFileSystem {
            return EmptyFileSystem()
        }
    }
}

/**
 * Extension to export DefaultFileSystem with a simpler API
 */
@JsExport
@JsName("FileSystem")
class JsFileSystem(private val projectPath: String) {
    private val fs: ProjectFileSystem = DefaultFileSystem(projectPath)
    
    @JsName("getProjectPath")
    fun getProjectPath(): String? = fs.getProjectPath()
    
    @JsName("readFile")
    fun readFile(path: String): String? = fs.readFile(path)
    
    @JsName("writeFile")
    fun writeFile(path: String, content: String): Boolean = fs.writeFile(path, content)
    
    @JsName("exists")
    fun exists(path: String): Boolean = fs.exists(path)
    
    @JsName("isDirectory")
    fun isDirectory(path: String): Boolean = fs.isDirectory(path)
    
    @JsName("listFiles")
    fun listFiles(path: String, pattern: String? = null): Array<String> {
        return fs.listFiles(path, pattern).toTypedArray()
    }
    
    @JsName("searchFiles")
    fun searchFiles(pattern: String, maxDepth: Int = 10, maxResults: Int = 100): Array<String> {
        return fs.searchFiles(pattern, maxDepth, maxResults).toTypedArray()
    }
    
    @JsName("resolvePath")
    fun resolvePath(relativePath: String): String = fs.resolvePath(relativePath)
}
