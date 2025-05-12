package cc.unitmesh.devti.language.status

import cc.unitmesh.devti.language.run.runner.ShireConsoleView
import com.intellij.execution.process.ProcessEvent
import com.intellij.util.messages.Topic
import java.util.*

@FunctionalInterface
interface DevInsRunListener : EventListener {
    fun runFinish(string: String, llmOutput: String, event: ProcessEvent, scriptPath: String, consoleView: ShireConsoleView?)

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<DevInsRunListener> = Topic(
            DevInsRunListener::class.java, Topic.BroadcastDirection.TO_DIRECT_CHILDREN
        )
    }
}