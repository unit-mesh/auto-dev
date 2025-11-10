package cc.unitmesh.server.command

import cc.unitmesh.agent.logging.AutoDevLogger
import java.io.File
import java.lang.String.format

// align to GoCD for better git clone
// https://github.com/gocd/gocd/blob/master/domain/src/main/java/com/thoughtworks/go/domain/materials/git/GitCommand.java
class GitCommand(
    val workingDir: File,
    val branch: String,
    val isSubmodule: Boolean,
    val secrets: List<String> = listOf(),
    val logStream: StreamConsumer
) {
    private val logger = AutoDevLogger.getLogger("GitCommand")
    
    private fun cloneCommand(): CommandLine {
        return git().withArg("clone")
    }

    fun clone(url: String, depth: Int, branch: String): Int {
        logger.info { "Cloning repository from $url to ${workingDir.absolutePath} with branch: $branch, depth: $depth" }
        
        val gitClone = cloneCommand()
            .`when`(depth < Int.MAX_VALUE) { git -> git.withArg(String.format("--depth=%s", depth)) }
            .withArg(url).withArg(workingDir.absolutePath)
            .withArgs("--branch", branch)

        val result = run(gitClone, logStream)
        
        if (result == 0) {
            logger.info { "✓ Successfully cloned repository to ${workingDir.absolutePath}" }
        } else {
            logger.error { "✗ Failed to clone repository, exit code: $result" }
        }
        
        return result
    }

    // todo: collection logs for frontend
    private fun run(cmd: CommandLine, console: StreamConsumer): Int {
        logger.debug { "Executing command: ${cmd.getCommandLine().joinToString(" ")}" }
        val processBuilder = ProcessBuilder(cmd.getCommandLine())
        val result = Processor.executeWithLogs(processBuilder, workingDir, logStream)
        logger.debug { "Command completed with exit code: $result" }
        return result
    }

    private fun git(): CommandLine {
        val git: CommandLine = CommandLine.createCommandLine("git").withEncoding("UTF-8")
        return git.withNonArgSecrets(secrets)
    }

    private fun gitWd(): CommandLine {
        return git().withWorkingDir(workingDir)
    }

    fun fetch(): Int {
        logger.info { "Fetching from origin for ${workingDir.absolutePath}" }
        val gitFetch: CommandLine = gitWd().withArgs("fetch", "origin", "--prune", "--recurse-submodules=no")
        val result: Int = run(gitFetch, logStream)
        if (result != 0) {
            logger.error { "✗ Git fetch failed for ${workingDir.absolutePath}" }
            throw RuntimeException(format("git fetch failed for [%s]", workingDir))
        }
        logger.info { "✓ Successfully fetched from origin" }
        return result
    }

    fun pullCode(): Int {
        logger.info { "Pulling code for ${workingDir.absolutePath} on branch: $branch" }
        val result = runCascade(
            logStream,
            gitWd().withArgs("reset", "--hard"),
            // clean files
            gitWd().withArgs("clean", "-dff"),
            git_C().withArgs("config", "--replace-all", "remote.origin.fetch", "+" + expandRefSpec()),
            git_C().withArgs("pull", "--rebase"),
        )
        
        if (result == 0) {
            logger.info { "✓ Successfully pulled code" }
        } else {
            logger.error { "✗ Failed to pull code, exit code: $result" }
        }
        
        return result
    }

    private fun git_C(): CommandLine {
        return git().withArgs("-C", workingDir.absolutePath)
    }

    fun localBranch(): String {
        return RefSpecHelper.localBranch(branch)
    }

    fun remoteBranch(): String {
        return RefSpecHelper.remoteBranch(RefSpecHelper.expandRefSpec(branch))
    }

    fun fullUpstreamRef(): String {
        return RefSpecHelper.fullUpstreamRef(branch)
    }

    fun expandRefSpec(): String {
        return RefSpecHelper.expandRefSpec(branch)
    }

    /**
     * Conveniently runs commands sequentially on a given console, aborting on the first failure.
     *
     * @param console  collects console output
     * @param commands the set of sequential commands
     * @return the exit status of the last executed command
     */
    protected fun runCascade(console: StreamConsumer, vararg commands: CommandLine?): Int {
        var code = 0

        for (cmd in commands) {
            code = run(cmd!!, console)
            if (0 != code) {
                break
            }
        }
        return code
    }
}
