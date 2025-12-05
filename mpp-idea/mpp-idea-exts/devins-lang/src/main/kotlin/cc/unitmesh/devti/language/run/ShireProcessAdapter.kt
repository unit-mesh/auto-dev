package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.run.runner.ShireConsoleView
import cc.unitmesh.devti.language.status.DevInsRunListener
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key

/**
 * Adapter for the process of the Shire run configuration.
 * Will be used to get the result of the process and to notify the listeners when the process is terminated.
 */
class ShireProcessAdapter(val configuration: DevInsConfiguration, val consoleView: ShireConsoleView?) :
    ProcessAdapter() {
    var result = ""
    private var llmOutput: String = ""

    override fun processTerminated(event: ProcessEvent) {
        super.processTerminated(event)
        ApplicationManager.getApplication().messageBus
            .syncPublisher(DevInsRunListener.TOPIC)
            .runFinish(result, llmOutput, event, configuration.getScriptPath(), consoleView)
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        super.onTextAvailable(event, outputType)
        result = consoleView?.output().toString()
    }

    /**
     * When the process is terminated, will use the given llmOutput to set the llmOutput of the adapter.
     */
    fun setLlmOutput(llmOutput: String?) {
        if (llmOutput != null) {
            this.llmOutput = llmOutput
        }
    }
}