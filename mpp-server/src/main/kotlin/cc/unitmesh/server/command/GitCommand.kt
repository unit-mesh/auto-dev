package cc.unitmesh.server.command

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
    private fun cloneCommand(): CommandLine {
        return git().withArg("clone")
    }

    fun clone(url: String, depth: Int, branch: String): Int {
        val gitClone = cloneCommand()
            .`when`(depth < Int.MAX_VALUE) { git -> git.withArg(String.format("--depth=%s", depth)) }
            .withArg(url).withArg(workingDir.absolutePath)
            .withArgs("--branch", branch)

        return run(gitClone, logStream)
    }

    // todo: collection logs for frontend
    private fun run(cmd: CommandLine, console: StreamConsumer): Int {
        val processBuilder = ProcessBuilder(cmd.getCommandLine())
        return Processor.executeWithLogs(processBuilder, workingDir, logStream)
    }

    private fun git(): CommandLine {
        val git: CommandLine = CommandLine.createCommandLine("git").withEncoding("UTF-8")
        return git.withNonArgSecrets(secrets)
    }

    private fun gitWd(): CommandLine {
        return git().withWorkingDir(workingDir)
    }

    fun fetch(): Int {
        val gitFetch: CommandLine = gitWd().withArgs("fetch", "origin", "--prune", "--recurse-submodules=no")
        val result: Int = run(gitFetch, logStream)
        if (result != 0) {
            throw RuntimeException(format("git fetch failed for [%s]", workingDir))
        }

        return result
    }

    fun pullCode(): Int {
        return runCascade(
            logStream,
            gitWd().withArgs("reset", "--hard"),
            // clean files
            gitWd().withArgs("clean", "-dff"),
            git_C().withArgs("config", "--replace-all", "remote.origin.fetch", "+" + expandRefSpec()),
            git_C().withArgs("pull", "--rebase"),
        )
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
