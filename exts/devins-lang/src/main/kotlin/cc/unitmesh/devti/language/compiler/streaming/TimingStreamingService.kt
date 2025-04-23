package cc.unitmesh.devti.language.compiler.streaming

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.console.DevInConsoleViewBase
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project

/**
 * Logging start time and end time for each lifecycle
 */
class TimingStreamingService : StreamingServiceProvider {
    override var name: String = "timing"

    private var time: Long = 0
    private var console: DevInConsoleViewBase? = null

    override fun onCreated(console: DevInConsoleViewBase?) {
        this.console = console
        val currentTime = System.currentTimeMillis()
        time = currentTime
        // new line
        console?.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        console?.print("Start timing: $currentTime \n", ConsoleViewContentType.SYSTEM_OUTPUT)
    }

    override fun afterStreamingDone(project: Project) {
        val currentTime = System.currentTimeMillis()

        console?.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        console?.print("End timing: $currentTime \n", ConsoleViewContentType.SYSTEM_OUTPUT)

        AutoDevNotifications.warn(project, "Timing: ${currentTime - time}ms")
    }
}