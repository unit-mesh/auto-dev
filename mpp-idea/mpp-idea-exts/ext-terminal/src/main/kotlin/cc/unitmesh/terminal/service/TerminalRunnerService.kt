package cc.unitmesh.terminal.service

import cc.unitmesh.devti.sketch.run.ProcessExecutor
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.terminal.JBTerminalWidget
import com.pty4j.PtyProcess
import org.jetbrains.plugins.terminal.AbstractTerminalRunner
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions

@Service(Service.Level.PROJECT)
class TerminalRunnerService(private val project: Project) {
    private var terminalRunner: AbstractTerminalRunner<PtyProcess>? = null

    fun createTerminalRunner(): AbstractTerminalRunner<PtyProcess> {
        return terminalRunner ?: initializeTerminalRunner().also { terminalRunner = it }
    }

    fun initializeTerminalRunner(): AbstractTerminalRunner<PtyProcess> {
        return LocalTerminalDirectRunner.createTerminalRunner(project)
    }

    private fun createStartupOptions(): ShellStartupOptions? {
        val jdkVersion = try {
            ProcessExecutor.getJdkVersion(project)
        } catch (e: Exception) {
            null
        }

        return jdkVersion?.let { javaHomePath ->
            val environmentVariables = mapOf("JAVA_HOME" to javaHomePath)
            val startupOptions = ShellStartupOptions.Builder()
                .envVariables(environmentVariables)
                .build()

            return@let startupOptions
        }
    }

    fun createTerminalWidget(
        parent: Disposable,
        startingDirectory: String?,
        deferSessionStartUntilUiShown: Boolean = true
    ): JBTerminalWidget {
        val terminalRunner = createTerminalRunner()

        createStartupOptions()?.also {
            val terminalWidget = terminalRunner.startShellTerminalWidget(parent, it, deferSessionStartUntilUiShown)
            val jediTermWidget = JBTerminalWidget.asJediTermWidget(terminalWidget)
            if (jediTermWidget != null) {
                return jediTermWidget
            }
        }

        return terminalRunner.createTerminalWidget(parent, startingDirectory, deferSessionStartUntilUiShown)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): TerminalRunnerService = project.service()
    }
}
