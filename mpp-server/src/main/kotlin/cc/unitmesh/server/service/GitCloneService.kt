package cc.unitmesh.server.service

import cc.unitmesh.agent.logging.AutoDevLogger
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
    
    private val logger = AutoDevLogger.getLogger("GitCloneService")
    private val logChannel = Channel<AgentEvent>(Channel.UNLIMITED)
    
    // Map to track projectId -> workspace path for persistence
    private val tempDirectoryMap = mutableMapOf<String, String>()
    
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
        logger.info { "Starting clone process for projectId: $projectId, gitUrl: $gitUrl, branch: ${branch ?: "default"}" }
        
        val workspaceDir = createWorkspaceDir(projectId)
        val projectPath = workspaceDir.pathString
        
        logger.info { "Created workspace directory at: $projectPath" }
        
        try {
            // Send initial progress
            emit(AgentEvent.CloneProgress("Preparing to clone repository", 0))
            
            val processedGitUrl = processGitUrl(gitUrl, username, password)
            
            // Check if already cloned
            val cloneSuccess = if (isGitRepository(workspaceDir.toFile())) {
                logger.info { "Repository already exists at $projectPath, pulling latest changes" }
                emit(AgentEvent.CloneLog("Repository already exists, pulling latest changes..."))
                emit(AgentEvent.CloneProgress("Updating repository", 50))
                
                val pullSuccess = gitPull(workspaceDir.toFile(), branch) { log ->
                    emit(log)
                }
                if (pullSuccess) {
                    logger.info { "Successfully pulled latest changes for $projectPath" }
                    emit(AgentEvent.CloneProgress("Repository updated", 100))
                    true
                } else {
                    logger.warn { "Pull failed, attempting fresh clone for $projectPath" }
                    emit(AgentEvent.CloneLog("Failed to pull, will try fresh clone", isError = true))
                    deleteDirectory(workspaceDir)
                    Files.createDirectories(workspaceDir)
                    gitClone(processedGitUrl, workspaceDir.toFile(), branch) { log ->
                        emit(log)
                    }
                }
            } else {
                // Fresh clone
                logger.info { "Starting fresh clone from $gitUrl to $projectPath" }
                emit(AgentEvent.CloneLog("Cloning repository from $gitUrl..."))
                emit(AgentEvent.CloneProgress("Cloning repository", 10))
                
                gitClone(processedGitUrl, workspaceDir.toFile(), branch) { log ->
                    emit(log)
                }
            }
            
            if (cloneSuccess) {
                logger.info { "✓ Clone completed successfully at: $projectPath" }
                emit(AgentEvent.CloneProgress("Clone completed successfully", 100))
                emit(AgentEvent.CloneLog("✓ Repository ready at: $projectPath"))
                // Store the path for agent to use later
                lastClonedPath = projectPath
                logger.info { "Stored lastClonedPath: $projectPath for projectId: $projectId" }
            } else {
                logger.error { "✗ Git clone failed for $gitUrl at $projectPath" }
                emit(AgentEvent.Error("Git clone failed"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during clone: ${e.message}" }
            emit(AgentEvent.CloneLog("Error during clone: ${e.message}", isError = true))
            emit(AgentEvent.Error("Clone failed: ${e.message}"))
        }
    }
    
    var lastClonedPath: String? = null
        private set
    
    private fun createWorkspaceDir(projectId: String): Path {
        val tempDir = Files.createTempDirectory("autodev-clone-")
        logger.info { "Created temporary directory: ${tempDir.pathString}" }
        
        val workspaceDir = tempDir.resolve(projectId)
        Files.createDirectories(workspaceDir)
        logger.info { "Created workspace directory: ${workspaceDir.pathString} for projectId: $projectId" }
        
        // Store temp directory mapping for reference
        tempDirectoryMap[projectId] = workspaceDir.pathString
        
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
        
        // Add depth for shallow clone (faster)
        cmd.addAll(listOf("--depth", "1"))
        
        // Add branch if specified
        if (!branch.isNullOrBlank()) {
            logger.info { "Cloning with specified branch: $branch" }
            cmd.addAll(listOf("-b", branch))
        } else {
            logger.info { "No branch specified, Git will use repository's default branch" }
        }
        
        cmd.add(gitUrl)
        cmd.add(".")
        
        val success = executeGitCommand(cmd, workspaceDir, emitLog)
        
        // If clone with specified branch failed, try without branch (use default)
        if (!success && !branch.isNullOrBlank()) {
            logger.warn { "Clone with branch '$branch' failed, retrying with default branch" }
            emitLog(AgentEvent.CloneLog("Branch '$branch' not found, trying repository's default branch..."))
            
            // Clear the directory and try again without branch
            deleteDirectory(workspaceDir.toPath())
            workspaceDir.mkdirs()
            
            val fallbackCmd = mutableListOf("git", "clone", "--depth", "1", gitUrl, ".")
            return executeGitCommand(fallbackCmd, workspaceDir, emitLog)
        }
        
        return success
    }
    
    private suspend fun gitPull(
        workspaceDir: File,
        branch: String?,
        emitLog: suspend (AgentEvent) -> Unit
    ): Boolean {
        val cmd = mutableListOf("git", "pull", "origin")
        
        if (!branch.isNullOrBlank()) {
            logger.info { "Pulling specified branch: $branch" }
            cmd.add(branch)
        } else {
            logger.info { "No branch specified for pull, Git will pull current/tracking branch" }
            // Don't specify branch - let git pull from the current tracking branch
        }
        
        val success = executeGitCommand(cmd, workspaceDir, emitLog)
        
        // If pull with specified branch failed, try without branch (current branch)
        if (!success && !branch.isNullOrBlank()) {
            logger.warn { "Pull with branch '$branch' failed, retrying with current branch" }
            emitLog(AgentEvent.CloneLog("Branch '$branch' not found, trying current branch..."))
            
            val fallbackCmd = mutableListOf("git", "pull", "origin")
            return executeGitCommand(fallbackCmd, workspaceDir, emitLog)
        }
        
        return success
    }
    
    private suspend fun executeGitCommand(
        cmd: List<String>,
        workingDir: File,
        emitLog: suspend (AgentEvent) -> Unit
    ): Boolean {
        try {
            val cmdString = cmd.joinToString(" ")
            logger.debug { "Executing git command: $cmdString in directory: ${workingDir.absolutePath}" }
            emitLog(AgentEvent.CloneLog("Executing: $cmdString"))
            
            val processBuilder = ProcessBuilder(cmd)
                .directory(workingDir)
                .redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            // Read output in real-time
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        logger.debug { "Git output: $it" }
                        emitLog(AgentEvent.CloneLog(it))
                    }
                }
            }
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                logger.info { "✓ Git command completed successfully: $cmdString" }
                emitLog(AgentEvent.CloneLog("✓ Git command completed successfully"))
                return true
            } else {
                logger.error { "✗ Git command failed with exit code $exitCode: $cmdString" }
                emitLog(AgentEvent.CloneLog("✗ Git command failed with exit code: $exitCode", isError = true))
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "✗ Error executing git command: ${cmd.joinToString(" ")}" }
            emitLog(AgentEvent.CloneLog("✗ Error executing git command: ${e.message}", isError = true))
            e.printStackTrace()
            return false
        }
    }
    
    private fun deleteDirectory(path: Path) {
        try {
            logger.info { "Deleting directory: ${path.pathString}" }
            path.toFile().deleteRecursively()
            logger.info { "Successfully deleted directory: ${path.pathString}" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to delete directory: ${path.pathString}" }
        }
    }
    
    /**
     * Get the workspace path for a given projectId
     */
    fun getWorkspacePath(projectId: String): String? {
        return tempDirectoryMap[projectId] ?: lastClonedPath
    }
    
    /**
     * Get all tracked workspace paths
     */
    fun getAllWorkspaces(): Map<String, String> {
        logger.info { "Retrieved all workspaces: ${tempDirectoryMap.size} entries" }
        return tempDirectoryMap.toMap()
    }
}

