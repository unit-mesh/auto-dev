package cc.unitmesh.devti.language.service

import cc.unitmesh.devti.language.run.runner.ShireExecutionConsole
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ConsoleService(private val project: Project) {
    private var activeConsole: ShireExecutionConsole? = null

    fun setActiveConsole(console: ShireExecutionConsole) {
        activeConsole = console
    }

    fun getActiveConsole(): ShireExecutionConsole? = activeConsole

    fun print(text: String, contentType: ConsoleViewContentType = ConsoleViewContentType.NORMAL_OUTPUT) {
        activeConsole?.print(text, contentType)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ConsoleService = project.service<ConsoleService>()
    }
}
