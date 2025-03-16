package cc.unitmesh.devti.observer.plan

import com.intellij.util.messages.Topic
import java.util.*

@FunctionalInterface
interface PlanUpdateListener: EventListener {
    fun onPlanUpdate(items: MutableList<AgentTaskEntry>)

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<PlanUpdateListener> = Topic(
            PlanUpdateListener::class.java, Topic.BroadcastDirection.TO_DIRECT_CHILDREN
        )
    }
}