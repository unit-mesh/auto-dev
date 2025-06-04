package cc.unitmesh.devti.language.run.runner

import cc.unitmesh.devti.language.console.DevInConsoleViewBase
import cc.unitmesh.devti.language.run.DevInsConfiguration
import cc.unitmesh.devti.language.run.ShireProcessHandler
import com.intellij.build.BuildView
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.BuildEvent
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfigurationViewManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.panels.NonOpaquePanel
import java.awt.BorderLayout
import javax.swing.JComponent

class ShireConsoleView(private val executionConsole: ShireExecutionConsole) : DevInConsoleViewBase(executionConsole) {
    override fun getComponent(): JComponent = myPanel

    private var myPanel: NonOpaquePanel = NonOpaquePanel(BorderLayout())

    private var shireRunner: ShireRunner? = null
    private val id = ProjectSystemId("DevIn")
    private fun createTaskId() =
        ExternalSystemTaskId.create(id, ExternalSystemTaskType.RESOLVE_PROJECT, executionConsole.project)

    private val scriptPath = executionConsole.configuration.getScriptPath()

    val task = createTaskId()
    val buildDescriptor: DefaultBuildDescriptor =
        DefaultBuildDescriptor(task.id, "DevIn", scriptPath, System.currentTimeMillis())

    val viewManager: ExternalSystemRunConfigurationViewManager =
        executionConsole.project.getService(ExternalSystemRunConfigurationViewManager::class.java)

    private val buildView: BuildView = object : BuildView(
        executionConsole.project,
        executionConsole,
        buildDescriptor,
        "build.toolwindow.run.selection.state",
        viewManager
    ) {
        override fun onEvent(buildId: Any, event: BuildEvent) {
            super.onEvent(buildId, event)
            viewManager.onEvent(buildId, event)
        }
    }

    init {
        val baseComponent = buildView.component
        myPanel.add(baseComponent, BorderLayout.EAST)

        executionConsole.getProcessHandler()?.let {
            buildView.attachToProcess(it)
        }

        myPanel.add(delegate.component, BorderLayout.CENTER)
    }

    fun output(clearAndStop: Boolean = true) = executionConsole.getOutput(clearAndStop)

    override fun cancelCallback(callback: (String) -> Unit) {
        shireRunner?.addCancelListener(callback)
    }

    fun getEditor(): Editor? {
        return executionConsole.editor
    }

    override fun isCanceled(): Boolean = shireRunner?.isCanceled() ?: super.isCanceled()

    fun bindShireRunner(runner: ShireRunner) {
        shireRunner = runner
    }

    override fun dispose() {
        super.dispose()
        executionConsole.dispose()
    }
}

class ShireExecutionConsole(
    project: Project,
    viewer: Boolean,
    private var isStopped: Boolean = false,
    val configuration: DevInsConfiguration,
) : ConsoleViewImpl(project, viewer) {
    private val outputBuilder = StringBuilder()
    private var processHandler: ShireProcessHandler? = null

    fun getProcessHandler(): ShireProcessHandler? {
        return processHandler
    }

    override fun attachToProcess(processHandler: ProcessHandler) {
        super.attachToProcess(processHandler)
        this.processHandler = processHandler as ShireProcessHandler
    }

    override fun print(text: String, contentType: ConsoleViewContentType) {
        super.print(text, contentType)
        if (!isStopped) outputBuilder.append(text)
    }

    fun getOutput(clearAndStop: Boolean): String {
        val output = outputBuilder.toString()
        if (clearAndStop) {
            isStopped = true
            outputBuilder.clear()
        }

        return output
    }
}