package cc.unitmesh.devti.language.compiler.streaming

import cc.unitmesh.devti.language.console.DevInConsoleViewBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * all - Returns a list of all registered StreamingServiceProvider implementations.
 *
 * @return A list of StreamingServiceProvider instances.
 */
interface StreamingServiceProvider : Disposable {
    var name: String

    /**
     * When create the service, you can do some initialization here, like start timer, etc.
     */
    fun onCreated(console: DevInConsoleViewBase?) {
        /// do nothing
    }

    /**
     * For the start of the LLM streaming, you can do some initialization here, for example, you can create a file to log the data
     */
    fun onBeforeStreaming(project: Project, userPrompt: String, console: DevInConsoleViewBase?) {
        /// do nothing
    }

    /**
     * For the streaming data, you can do some processing here, for example, you can log the data to a file
     */
    fun onStreaming(project: Project, flow: String, args: List<Any>) {
        /// do nothing
    }

    /**
     * For the end of the streaming, for example, you can do some cleanup here, or show some notification
     */
    fun afterStreamingDone(project: Project) {
        /// do nothing
    }

    override fun dispose() {
        /// do nothing
    }

    companion object {
        val EP_NAME =
            ExtensionPointName.create<StreamingServiceProvider>("com.phodal.shireStreamingService")

        fun getStreamingService(name: String): StreamingServiceProvider? {
            return EP_NAME.extensions.firstOrNull { it.name == name }
        }

        fun all(): List<StreamingServiceProvider> {
            return EP_NAME.extensions.toList()
        }
    }
}