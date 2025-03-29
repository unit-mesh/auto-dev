package cc.unitmesh.devti.sketch

import com.intellij.util.messages.Topic
import java.util.EventListener

@FunctionalInterface
interface AutoSketchModeListener : EventListener {
    fun start()

    fun done()

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<AutoSketchModeListener> =
            Topic(AutoSketchModeListener::class.java, Topic.BroadcastDirection.TO_DIRECT_CHILDREN)
    }
}
