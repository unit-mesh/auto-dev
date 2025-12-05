package cc.unitmesh.terminal.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.terminal.JBTerminalWidget
import com.pty4j.PtyProcess
import org.jetbrains.plugins.terminal.AbstractTerminalRunner
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner

@Service(Service.Level.PROJECT)
class TerminalRunnerService(private val project: Project) {
    private var terminalRunner: AbstractTerminalRunner<PtyProcess>? = null

    fun createTerminalRunner(): AbstractTerminalRunner<PtyProcess> {
        if (terminalRunner == null) {
            terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)
        }

        return terminalRunner!!
    }

    fun createTerminalWidget(
        parent: Disposable,
        startingDirectory: String?,
        deferSessionStartUntilUiShown: Boolean = true
    ): JBTerminalWidget {
        val terminalRunner = createTerminalRunner()
        return terminalRunner.createTerminalWidget(parent, startingDirectory, deferSessionStartUntilUiShown)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): TerminalRunnerService = project.service()
    }
}
