package cc.unitmesh.agent.platform

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * JS 平台的 Git 操作实现 (Node.js)
 * 
 * 使用 Node.js 的 child_process 模块调用 git 命令
 * 移植自 TypeScript 版本的 ErrorRecoveryAgent
 */
actual class GitOperations actual constructor(private val projectPath: String) {
    
    private val isNodeJs: Boolean by lazy {
        try {
            js("typeof process !== 'undefined' && process.versions && process.versions.node") as Boolean
        } catch (e: Throwable) {
            false
        }
    }
    
    actual suspend fun getModifiedFiles(): List<String> {
        if (!isNodeJs) {
            println("   ⚠️  Git operations require Node.js environment")
            return emptyList()
        }
        
        return try {
            val output = execGitCommand("git diff --name-only")
            val files = output.trim().split("\n").filter { it.isNotBlank() }
            
            if (files.isNotEmpty()) {
                val fileNames = files.map { it.split("/").last() }.joinToString(", ")
                println("   📝 Modified: $fileNames")
            } else {
                println("   ✓ No modifications detected")
            }
            
            files
        } catch (e: Throwable) {
            println("   ⚠️  Git check failed: ${e.message}")
            emptyList()
        }
    }
    
    actual suspend fun getFileDiff(filePath: String): String? {
        if (!isNodeJs) {
            return null
        }
        
        return try {
            val output = execGitCommand("git diff -- \"$filePath\"")
            if (output.trim().isNotBlank()) {
                output
            } else {
                null
            }
        } catch (e: Throwable) {
            // Silently skip
            null
        }
    }
    
    actual fun isSupported(): Boolean = isNodeJs
    
    /**
     * 执行 git 命令
     * 使用 Node.js 的 child_process.exec
     */
    private suspend fun execGitCommand(command: String): String {
        return execAsync(command, projectPath).await()
    }
}

/**
 * 封装 Node.js 的 child_process.exec 为 Promise
 */
private fun execAsync(command: String, cwd: String): Promise<String> {
    val exec: dynamic = js("require('child_process').exec")
    
    return Promise { resolve, reject ->
        exec(command, js("{ cwd: cwd }")) { error: dynamic, stdout: dynamic, stderr: dynamic ->
            if (error != null) {
                reject(js("new Error(stderr || error.message)"))
            } else {
                resolve(stdout as String)
            }
        }
    }
}
