package cc.unitmesh.terminal.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.terminal.JBTerminalWidget
import com.pty4j.PtyProcess
import org.jetbrains.plugins.terminal.AbstractTerminalRunner
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions

@Service(Service.Level.PROJECT)
class TerminalRunnerService(private val project: Project) {
    private var terminalRunner: AbstractTerminalRunner<PtyProcess>? = null

    fun createTerminalRunner(): AbstractTerminalRunner<PtyProcess> {
        if (terminalRunner == null) {
            terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)
        }

        val homepath: String? = getJdkVersion()

        if (homepath != null) {
            val env = mutableMapOf<String, String>()
            env["JAVA_HOME"] = homepath

            val baseOptions = ShellStartupOptions.Builder()
                .envVariables(env)
                .build()

            terminalRunner?.configureStartupOptions(baseOptions)
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

    private fun getJdkVersion() : String? {
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk
        if (projectSdk != null && projectSdk.sdkType is JavaSdk) {
            return projectSdk.homePath
        }

        val projectJdkTable = ProjectJdkTable.getInstance()
        if (projectJdkTable.allJdks.isNotEmpty()) {
            for (jdk in projectJdkTable.allJdks) {
                if (jdk.sdkType is JavaSdk) {
                    return jdk.homePath
                }
            }
        }

        val javaHome = System.getenv("JAVA_HOME")
        if (javaHome != null && javaHome.isNotEmpty()) {
            return javaHome
        }

        val javaHomeSdk: Sdk? = ExternalSystemJdkUtil.resolveJdkName(null, "#JAVA_HOME")
        if (javaHomeSdk != null) {
            return javaHomeSdk.homePath
        }

        return null
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): TerminalRunnerService = project.service()
    }
}
