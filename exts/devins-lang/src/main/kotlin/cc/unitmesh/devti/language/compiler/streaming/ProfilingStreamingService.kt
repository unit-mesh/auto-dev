package cc.unitmesh.devti.language.compiler.streaming

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.console.DevInConsoleViewBase
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.util.io.IOUtil


/**
 * The ProfilingStreamingService class is a concrete implementation of the StreamingServiceProvider interface.
 * It provides profiling capabilities during the streaming process, outputting memory usage information to the console.
 */
class ProfilingStreamingService : StreamingServiceProvider {
    override var name: String = "profiling"
    private var console: DevInConsoleViewBase? = null

    override fun onBeforeStreaming(project: Project, userPrompt: String, console: DevInConsoleViewBase?) {
        this.console = console
        console?.print("Start profiling: ${getMemory()}", ConsoleViewContentType.SYSTEM_OUTPUT)

    }

    override fun afterStreamingDone(project: Project) {
        console?.print("End profiling: ${getMemory()}", ConsoleViewContentType.SYSTEM_OUTPUT)
        AutoDevNotifications.warn(project, "Memory: ${getMemory()}MB")
    }

    private fun getMemory(): Long {
        val runtime = Runtime.getRuntime()
        val allocatedMem = runtime.totalMemory()
        val usedMem = allocatedMem - runtime.freeMemory()
        return toMb(usedMem)
    }

    private fun toMb(value: Long): Long {
        return value / 1048576
    }
}
