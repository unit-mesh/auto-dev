package cc.unitmesh.agent.platform

import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * WebAssembly 平台的 Git 操作实现
 * 
 * 使用 wasm-git (libgit2 编译为 WASM) 提供 Git 功能
 * 参考: https://github.com/petersalomonsen/wasm-git
 */
actual class GitOperations actual constructor(private val projectPath: String) {
    
    private var lg2Module: LibGit2Module? = null
    private var isInitialized = false
    
    /**
     * Initialize wasm-git module
     */
    private suspend fun initialize() {
        if (isInitialized) return
        
        try {
            console.log("Initializing wasm-git...")
            
            // Load lg2 module
            val config = js("({ locateFile: (s) => 'https://unpkg.com/wasm-git@0.0.13/' + s })")
            val promise: Promise<LibGit2Module> = WasmGit.lg2(config.unsafeCast()).unsafeCast()
            lg2Module = promise.await()
            
            console.log("wasm-git initialized successfully")
            isInitialized = true
            
            // Try to change to project directory if it exists
            try {
                lg2Module?.FS?.chdir(projectPath)
            } catch (e: Throwable) {
                console.warn("Project directory not found: $projectPath")
            }
        } catch (e: Throwable) {
            console.error("Failed to initialize wasm-git: ${e.message ?: "Unknown error"}")
            throw e
        }
    }
    
    actual fun isSupported(): Boolean {
        return true // wasm-git provides git functionality
    }
    
    /**
     * Clone a git repository
     * @param repoUrl Repository URL (e.g., "https://github.com/user/repo.git")
     * @param targetDir Target directory name (optional, will be derived from URL if not provided)
     * @return true if successful
     */
    suspend fun clone(repoUrl: String, targetDir: String? = null): Boolean {
        initialize()
        
        val module = lg2Module ?: return false
        
        return try {
            val dir = targetDir ?: repoUrl.substringAfterLast('/').removeSuffix(".git")
            console.log("Cloning $repoUrl into $dir...")
            
            val exitCode = module.callMain(jsArrayOf("clone", repoUrl, dir))
            
            if (exitCode == 0) {
                console.log("Clone successful")
                // Change to cloned directory
                module.FS.chdir(dir)
                true
            } else {
                console.error("Clone failed with exit code: $exitCode")
                false
            }
        } catch (e: Throwable) {
            console.error("Clone error: ${e.message}")
            false
        }
    }
    
    actual suspend fun getModifiedFiles(): List<String> {
        initialize()
        
        val module = lg2Module ?: return emptyList()
        
        return try {
            // Run git status --porcelain to get modified files
            val exitCode = module.callMain(jsArrayOf("status", "--porcelain"))
            
            if (exitCode != 0) {
                console.warn("git status failed with exit code: $exitCode")
                return emptyList()
            }
            
            // Note: In the web worker implementation, stdout is captured via postMessage
            // For now, we'll return empty list as capturing stdout in WASM requires additional setup
            // TODO: Implement stdout capture mechanism
            emptyList()
        } catch (e: Throwable) {
            console.error("Failed to get modified files: ${e.message}")
            emptyList()
        }
    }
    
    actual suspend fun getFileDiff(filePath: String): String? {
        initialize()
        
        val module = lg2Module ?: return null
        
        return try {
            // Run git diff for specific file
            val exitCode = module.callMain(jsArrayOf("diff", filePath))
            
            if (exitCode != 0) {
                console.warn("git diff failed with exit code: $exitCode")
                return null
            }
            
            // TODO: Implement stdout capture to get actual diff content
            null
        } catch (e: Throwable) {
            console.error("Failed to get file diff: ${e.message}")
            null
        }
    }

    actual suspend fun getRecentCommits(count: Int): List<GitCommitInfo> {
        initialize()
        
        val module = lg2Module ?: return emptyList()
        
        return try {
            // Run git log
            val exitCode = module.callMain(jsArrayOf(
                "log", 
                "--pretty=format:%H|%an|%ae|%at|%s",
                "-n", count.toString()
            ))
            
            if (exitCode != 0) {
                console.warn("git log failed with exit code: $exitCode")
                return emptyList()
            }
            
            // TODO: Parse log output to create GitCommitInfo objects
            emptyList()
        } catch (e: Throwable) {
            console.error("Failed to get recent commits: ${e.message}")
            emptyList()
        }
    }

    actual suspend fun getTotalCommitCount(): Int? {
        initialize()
        
        val module = lg2Module ?: return null
        
        return try {
            // Run git rev-list --count HEAD
            val exitCode = module.callMain(jsArrayOf("rev-list", "--count", "HEAD"))
            
            if (exitCode != 0) {
                console.warn("git rev-list failed with exit code: $exitCode")
                return null
            }
            
            // TODO: Parse output to get count
            null
        } catch (e: Throwable) {
            console.error("Failed to get commit count: ${e.message}")
            null
        }
    }

    actual suspend fun getCommitDiff(commitHash: String): GitDiffInfo? {
        initialize()
        
        val module = lg2Module ?: return null
        
        return try {
            // Run git show for specific commit
            val exitCode = module.callMain(jsArrayOf("show", commitHash))
            
            if (exitCode != 0) {
                console.warn("git show failed with exit code: $exitCode")
                return null
            }
            
            // TODO: Parse diff output
            null
        } catch (e: Throwable) {
            console.error("Failed to get commit diff: ${e.message}")
            null
        }
    }

    actual suspend fun getDiff(base: String, target: String): GitDiffInfo? {
        initialize()
        
        val module = lg2Module ?: return null
        
        return try {
            // Run git diff between two refs
            val exitCode = module.callMain(jsArrayOf("diff", base, target))
            
            if (exitCode != 0) {
                console.warn("git diff failed with exit code: $exitCode")
                return null
            }
            
            // TODO: Parse diff output
            null
        } catch (e: Throwable) {
            console.error("Failed to get diff: ${e.message}")
            null
        }
    }
    
    /**
     * Initialize a new git repository
     */
    suspend fun init(): Boolean {
        initialize()
        
        val module = lg2Module ?: return false
        
        return try {
            console.log("Initializing git repository...")
            val exitCode = module.callMain(jsArrayOf("init"))
            exitCode == 0
        } catch (e: Throwable) {
            console.error("Failed to init repository: ${e.message}")
            false
        }
    }
    
    /**
     * Add files to staging area
     */
    suspend fun add(vararg files: String): Boolean {
        initialize()
        
        val module = lg2Module ?: return false
        
        return try {
            val args = jsArrayOf("add", *files)
            val exitCode = module.callMain(args)
            exitCode == 0
        } catch (e: Throwable) {
            console.error("Failed to add files: ${e.message}")
            false
        }
    }
    
    /**
     * Commit changes
     */
    suspend fun commit(message: String): Boolean {
        initialize()
        
        val module = lg2Module ?: return false
        
        return try {
            val exitCode = module.callMain(jsArrayOf("commit", "-m", message))
            exitCode == 0
        } catch (e: Throwable) {
            console.error("Failed to commit: ${e.message}")
            false
        }
    }
    
    /**
     * Push changes to remote
     */
    suspend fun push(remote: String = "origin", branch: String = "master"): Boolean {
        initialize()
        
        val module = lg2Module ?: return false
        
        return try {
            val exitCode = module.callMain(jsArrayOf("push", remote, branch))
            exitCode == 0
        } catch (e: Throwable) {
            console.error("Failed to push: ${e.message}")
            false
        }
    }
    
    /**
     * Pull changes from remote
     */
    suspend fun pull(remote: String = "origin", branch: String = "master"): Boolean {
        initialize()
        
        val module = lg2Module ?: return false
        
        return try {
            var exitCode = module.callMain(jsArrayOf("fetch", remote))
            if (exitCode != 0) return false
            
            exitCode = module.callMain(jsArrayOf("merge", "$remote/$branch"))
            exitCode == 0
        } catch (e: Throwable) {
            console.error("Failed to pull: ${e.message}")
            false
        }
    }
    
    /**
     * Read file content from repository
     */
    suspend fun readFile(filePath: String): String? {
        initialize()
        
        val module = lg2Module ?: return null
        
        return try {
            val options = createReadFileOptions("utf8")
            module.FS.readFile(filePath, options)
        } catch (e: Throwable) {
            console.error("Failed to read file: ${e.message}")
            null
        }
    }
    
    /**
     * Write file to repository
     */
    suspend fun writeFile(filePath: String, content: String): Boolean {
        initialize()
        
        val module = lg2Module ?: return false
        
        return try {
            module.FS.writeFile(filePath, content)
            true
        } catch (e: Throwable) {
            console.error("Failed to write file: ${e.message}")
            false
        }
    }
}
