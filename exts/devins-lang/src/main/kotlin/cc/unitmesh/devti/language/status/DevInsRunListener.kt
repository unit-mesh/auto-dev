package cc.unitmesh.devti.language.status

import com.intellij.execution.process.ProcessEvent
import com.intellij.util.messages.Topic
import java.util.*

@FunctionalInterface
interface DevInsRunListener : EventListener {
    fun runFinish(string: String, event: ProcessEvent, scriptPath: String)

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<DevInsRunListener> = Topic(
            DevInsRunListener::class.java, Topic.BroadcastDirection.TO_DIRECT_CHILDREN
        )
    }
}