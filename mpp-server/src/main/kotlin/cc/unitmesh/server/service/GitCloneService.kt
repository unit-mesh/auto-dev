package cc.unitmesh.server.service

import cc.unitmesh.server.model.AgentEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.pathString

class GitCloneService {
    
    private val logChannel = Channel<AgentEvent>(Channel.UNLIMITED)
    
    data class CloneResult(
        val success: Boolean,
        val projectPath: String,
        val error: String? = null
    )
    
    /**
     * Clone git repository and emit logs and result as SSE events
     */
    suspend fun cloneRepositoryWithLogs(
        gitUrl: String,
        branch: String? = null,
        username: String? = null,
        password: String? = null,
        projectId: String
    ): Flow<AgentEvent> = flow {
        val workspaceDir = createWorkspaceDir(projectId)
        val projectPath = workspaceDir.pathString
        
        try {
            // Send initial progress
            emit(AgentEvent.CloneProgress("Preparing to clone repository", 0))
            
            val processedGitUrl = processGitUrl(gitUrl, username, password)
            
            // Check if already cloned
            val cloneSuccess = if (isGitRepository(workspaceDir.toFile())) {
                emit(AgentEvent.CloneLog("Repository already exists, pulling latest changes..."))
                emit(AgentEvent.CloneProgress("Updating repository", 50))
                
                val pullSuccess = gitPull(workspaceDir.toFile(), branch) { log ->
                    emit(log)
                }
                if (pullSuccess) {
                    emit(AgentEvent.CloneProgress("Repository updated", 100))
                    true
                } else {
                    emit(AgentEvent.CloneLog("Failed to pull, will try fresh clone", isError = true))
                    deleteDirectory(workspaceDir)
                    Files.createDirectories(workspaceDir)
                    gitClone(processedGitUrl, workspaceDir.toFile(), branch) { log ->
                        emit(log)
                    }
                }
            } else {
                // Fresh clone
                emit(AgentEvent.CloneLog("Cloning repository from $gitUrl..."))
                emit(AgentEvent.CloneProgress("Cloning repository", 10))
                
                gitClone(processedGitUrl, workspaceDir.toFile(), branch) { log ->
                    emit(log)
                }
            }
            
            if (cloneSuccess) {
                emit(AgentEvent.CloneProgress("Clone completed successfully", 100))
                emit(AgentEvent.CloneLog("✓ Repository ready at: $projectPath"))
                // Store the path for agent to use later
                lastClonedPath = projectPath
            } else {
                emit(AgentEvent.Error("Git clone failed"))
            }
        } catch (e: Exception) {
            emit(AgentEvent.CloneLog("Error during clone: ${e.message}", isError = true))
            emit(AgentEvent.Error("Clone failed: ${e.message}"))
        }
    }
    
    var lastClonedPath: String? = null
        private set
    
    private fun createWorkspaceDir(projectId: String): Path {
        val tempDir = Files.createTempDirectory("autodev-clone-")
        val workspaceDir = tempDir.resolve(projectId)
        Files.createDirectories(workspaceDir)
        return workspaceDir
    }
    
    private fun isGitRepository(dir: File): Boolean {
        return File(dir, ".git").isDirectory
    }
    
    private fun processGitUrl(gitUrl: String, username: String?, password: String?): String {
        return if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            gitUrl.replace("//", "//${urlEncode(username)}:${urlEncode(password)}@")
        } else {
            gitUrl
        }
    }
    
    private fun urlEncode(msg: String): String {
        return URLEncoder.encode(msg, "UTF-8")
    }
    
    private suspend fun gitClone(
        gitUrl: String,
        workspaceDir: File,
        branch: String?,
        emitLog: suspend (AgentEvent) -> Unit
    ): Boolean {
        val cmd = mutableListOf("git", "clone")
        
        // Add branch if specified
        if (!branch.isNullOrBlank()) {
            cmd.addAll(listOf("-b", branch))
        }
        
        // Add depth for shallow clone (faster)
        cmd.addAll(listOf("--depth", "1"))
        
        cmd.add(gitUrl)
        cmd.add(".")
        
        return executeGitCommand(cmd, workspaceDir, emitLog)
    }
    
    private suspend fun gitPull(
        workspaceDir: File,
        branch: String?,
        emitLog: suspend (AgentEvent) -> Unit
    ): Boolean {
        val cmd = mutableListOf("git", "pull", "origin")
        
        if (!branch.isNullOrBlank()) {
            cmd.add(branch)
        } else {
            cmd.add("main") // default to main
        }
        
        return executeGitCommand(cmd, workspaceDir, emitLog)
    }
    
    private suspend fun executeGitCommand(
        cmd: List<String>,
        workingDir: File,
        emitLog: suspend (AgentEvent) -> Unit
    ): Boolean {
        try {
            emitLog(AgentEvent.CloneLog("Executing: ${cmd.joinToString(" ")}"))
            
            val processBuilder = ProcessBuilder(cmd)
                .directory(workingDir)
                .redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            // Read output in real-time
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        emitLog(AgentEvent.CloneLog(it))
                    }
                }
            }
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                emitLog(AgentEvent.CloneLog("✓ Git command completed successfully"))
                return true
            } else {
                emitLog(AgentEvent.CloneLog("✗ Git command failed with exit code: $exitCode", isError = true))
                return false
            }
        } catch (e: Exception) {
            emitLog(AgentEvent.CloneLog("✗ Error executing git command: ${e.message}", isError = true))
            e.printStackTrace()
            return false
        }
    }
    
    private fun deleteDirectory(path: Path) {
        try {
            path.toFile().deleteRecursively()
        } catch (e: Exception) {
            // Silently ignore
        }
    }
}

